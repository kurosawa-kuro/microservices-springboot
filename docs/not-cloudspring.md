# kurobytes/microservices — Kubernetes OSS Adaptation Guide (SQLite Edition)

> **Scope** – This guide explains how to migrate the Udemy sample repository **kurobytes/microservices** from Spring Cloud–centric architecture to a **Kubernetes‑native, OSS‑only stack**, while switching each service’s datastore to **SQLite**.
> Target runtime is **EKS** (production) & **kind** (local).  All manifests are Helm‑templated and deployed via **Argo CD**.

---

## 1. High‑level Architecture

```text
┌──────────────────────────────┐
│   External (Internet)        │
│   ────────────────           │
│   LB → Gateway API (Envoy)   │
└─────────────┬────────────────┘
              │ HTTP / gRPC
┌─────────────┴───────────────┐
│   Istio Service Mesh        │← circuit‑break / retry / mTLS / OTEL
└──────┬────┬────┬────┬──────┘
       │    │    │    │       (K8s DNS)
   order  product payment user …   
       │    │    │    │
       └─ RabbitMQ / Kafka (Strimzi)
```

* **Config** → K8s **ConfigMap / Secret** (optionally synced by **External Secrets Operator**).
* **Discovery** → Kubernetes built‑in **ClusterDNS**.
* **Gateway** → **Gateway API** (Envoy Gateway).
* **Resilience / Observability** → **Istio** + OpenTelemetry → Jaeger / Grafana Tempo.
* **Database** → **SQLite** (file‑based) per service, backed by PVC for persistence.

---

## 2. Service‑level Changes

| Item                                       | Action                                                                                             |
| ------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| **Build gradle / pom.xml**                 | • Remove all `spring-cloud-*` starters                                                             |
| • Add `org.xerial:sqlite-jdbc:3.46.0.0`    |                                                                                                    |
| • Keep `spring-boot-starter-data-jpa`      |                                                                                                    |
| **JPA Dialect**                            | `spring.jpa.database-platform: org.hibernate.dialect.SQLiteDialect` *(use a custom dialect class)* |
| **application.yaml**                       | Replace JDBC URL with `jdbc:sqlite:/data/app.db`                                                   |
| **@EnableDiscoveryClient / @LoadBalanced** | **Delete** (not required)                                                                          |
| **Feign URL**                              | Call target by K8s DNS: `http://product-service:8080`                                              |
| **Resilience4j annotations**               | Optional: keep or move to Istio policies                                                           |

### SQLite Dialect (Kotlin example)

```kotlin
class SQLiteDialect : Dialect() {
  init {
    registerColumnType( Types.BLOB, "BLOB" )
    // …
  }
  override fun supportsIdentityColumns() = true
  // … other overrides …
}
```

---

## 3. Configuration & Secrets

### ConfigMap pattern

```bash
kubectl create configmap orders --from-file=orders-service.yaml
```

### External Secrets Operator (AWS SSM)

```yaml
apiVersion: external-secrets.io/v1beta1
kind: ExternalSecret
metadata:
  name: orders-ssm
spec:
  refreshInterval: 1h
  secretStoreRef:
    name: aws-ssm
    kind: ClusterSecretStore
  target:
    name: orders-secret
  data:
    - secretKey: jwt-secret
      remoteRef:
        key: /prod/orders/jwt
```

*Add `io.awspring.cloud:spring-cloud-aws-starter-parameter-store` **only if you need Spring lookup at runtime**; otherwise mount via `SPRING_JWT_SECRET` env.*

---

## 4. API Gateway (Gateway API + Envoy Gateway)

Install CRDs & controller:

```bash
kubectl apply -f https://github.com/kubernetes-sigs/gateway-api/releases/download/v1.2.0/standard-install.yaml
helm repo add envoy-gateway https://gateway.envoyproxy.io
helm install eg envoy-gateway/eg --set service.type=LoadBalancer
```

Sample Gateway & Route:

```yaml
apiVersion: gateway.networking.k8s.io/v1
kind: Gateway
metadata:
  name: eg
spec:
  gatewayClassName: envoy-gateway
  listeners:
    - name: http
      port: 80
      protocol: HTTP
---
apiVersion: gateway.networking.k8s.io/v1
kind: HTTPRoute
metadata:
  name: orders-route
spec:
  parentRefs:
    - name: eg
  rules:
    - matches:
        - path:
            type: PathPrefix
            value: /orders
      backendRefs:
        - name: orders-service
          port: 8080
```

---

## 5. Istio Resilience Policies

```yaml
apiVersion: networking.istio.io/v1beta1
kind: DestinationRule
metadata:
  name: orders-dr
spec:
  host: orders-service
  trafficPolicy:
    outlierDetection:
      consecutive5xxErrors: 3
      interval: 5s
      baseEjectionTime: 30s
    connectionPool:
      tcp:
        maxConnections: 10
```

Add retries/timeouts in a `VirtualService` if code annotations are removed.

---

## 6. SQLite Persistence

### Dockerfile snippet

```dockerfile
FROM eclipse-temurin:21-jdk
WORKDIR /app
COPY build/libs/orders.jar ./
VOLUME ["/data"]               # will hold app.db
ENV SPRING_DATASOURCE_URL=jdbc:sqlite:/data/app.db
CMD ["java","-jar","orders.jar"]
```

### PersistentVolumeClaim (EKS gp2)

```yaml
kind: PersistentVolumeClaim
apiVersion: v1
metadata:
  name: orders-pvc
spec:
  accessModes: [ "ReadWriteOnce" ]
  resources:
    requests:
      storage: 1Gi
```

Mount at `/data` in Deployment.

> *For **local kind**, use `hostPath` or emptyDir (ephemeral).*  SQLite fits single‑writer constraint per pod; scale vertically or migrate to PostgreSQL later.

---

## 7. Helm Chart Layout

```
charts/
  orders/
    templates/
      deployment.yaml
      service.yaml
      pvc.yaml
  gateway/
  istio-polices/
  external-secrets/
  messaging/               # RabbitMQ or Strimzi Kafka
```

Key `values.yaml` knobs per service:

```yaml
image:
  repository: ghcr.io/you/orders
  tag: 1.0.0
replicaCount: 1            # SQLite ⇒ single‑pod
persistence:
  enabled: true
  size: 1Gi
```

---

## 8. Argo CD GitOps

Application example:

```yaml
apiVersion: argoproj.io/v1alpha1
kind: Application
metadata:
  name: orders
spec:
  destination:
    namespace: orders
    server: https://kubernetes.default.svc
  source:
    repoURL: https://github.com/you/platform-helm
    targetRevision: main
    path: charts/orders
  syncPolicy:
    automated:
      prune: true
      selfHeal: true
```

*Use `ApplicationSet` with generators to handle per‑env overlays.*

---

## 9. Local Development Flow

1. `./gradlew bootRun` → auto‑recreates `app.db` under `./data`.
2. `docker compose up` (optional) for RabbitMQ / Kafka only.
3. `make kind-deploy` — wrapper that builds image, loads into kind, runs `helm upgrade --install`.

---

## 10. Production Checklist

| Area              | Checklist                                                             |
| ----------------- | --------------------------------------------------------------------- |
| **Persistence**   | Use EBS‑backed PVC; enable backup (Velero / EFS‑CSI Snapshots).       |
| **Security**      | Store JWT secret & DB encryption key in AWS SSM; sync via ESO.        |
| **Observability** | Install Istio telemetry add‑ons, scrape with OpenTelemetry Collector. |
| **Scaling**       | HorizontalPodAutoscaler disabled for SQLite pods (single writer).     |
| **Migrations**    | Flyway callback points to `/data/app.db`; run job on startup.         |

---

## 11. Appendix: Quick Reference

### SQLite JDBC URL Patterns

* `jdbc:sqlite:/path/to/app.db` – persistent file.
* `jdbc:sqlite::memory:` – tests only.

### Commands

```bash
# Deploy env
helm dependency update charts/orders
helm upgrade --install orders charts/orders -n orders --create-namespace

# View Gateway routes
kubectl get httproute -A

# Tail events
stern orders-service
```

---

### Next Steps

1. Migrate Product/Payment/User services following the same pattern.
2. Replace Resilience4j annotations with Istio traffic policies incrementally.
3. Plan future move from SQLite → PostgreSQL (Amazon RDS) once multi‑replica scaling is required.

---

© 2025  Your Name
