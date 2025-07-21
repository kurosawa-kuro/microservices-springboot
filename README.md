# Kubernetes-Native Microservices with Spring Boot

A modern microservices architecture built with Spring Boot and Kubernetes-native solutions, migrated from Spring Cloud components.

Based on the Udemy course [Master Microservices with Spring Boot, Docker, Kubernetes](https://www.udemy.com/course/master-microservices-with-spring-docker-kubernetes/?referralCode=9365DB9B7EE637F629A9) by KuroBytes.

---

## Project Overview

This project is a multi-module Maven monorepo implementing microservices with Java 21, Spring Boot 3.4.1, and SQLite. It has been migrated from Spring Cloud to Kubernetes-native solutions (ConfigMaps, DNS, Gateway API, Istio, etc.).

### Key Migration Points
- **Spring Cloud Config Server → Kubernetes ConfigMaps & Secrets**
- **Eureka Service Discovery → Kubernetes DNS**
- **Spring Cloud Gateway → Kubernetes Gateway API**
- **Resilience4j → Istio Traffic Management**
- **OpenFeign → RestTemplate**
- **H2 Database → SQLite (with Persistent Volumes)**

---

## Microservices Structure

```
├── accounts-service/        # Account management (port 8080)
├── cards-service/           # Credit card management (port 9000)
├── loans-service/           # Loan management (port 9020)
├── message-service/         # Event-driven messaging (port 9010)
├── gatewayserver-service/   # API Gateway (port 8072, legacy)
├── common/                  # Shared library (SQLite dialect, DTOs)
├── charts/                  # Helm charts for K8s deployment
├── docker-compose.yml       # Local multi-service orchestration
└── Makefile, pom.xml, etc.
```

- Each service is a standalone Spring Boot app with its own SQLite DB at `/data/app.db` (persistent volume in K8s)
- `common/` provides a custom SQLite dialect (`SQLiteDialect.java`) and shared DTOs (`ErrorResponseDto.java`)

---

## Technology Stack

- **Java 21** / **Spring Boot 3.4.1**
- **SQLite** (per-service, persistent)
- **Maven** (multi-module, BOM)
- **Docker** (Google Jib, no Dockerfile needed)
- **Kubernetes** (Helm, Gateway API, Istio optional)
- **Observability**: OpenTelemetry, Micrometer, Prometheus

---

## Prerequisites

- Java 21+
- Maven 3.8+
- Docker Desktop (with Kubernetes enabled)
- Helm 3.x
- kubectl CLI
- (Optional) Istio for service mesh

---

## Build & Run

### Local Development

```bash
# Build all services (from project root)
mvn clean install

# Build without tests
mvn clean install -Dmaven.test.skip=true

# Build only common module first (recommended for fresh clone)
cd common && ./mvnw clean install && cd ..

# Run a service locally (from service dir)
cd accounts-service
./mvnw spring-boot:run

# Build Docker image (from service dir)
./mvnw compile jib:dockerBuild

# Run all services with Docker Compose (from root)
docker compose up
```

### Kubernetes Deployment

```bash
# Deploy core services
helm install accounts charts/accounts/
helm install cards charts/cards/
helm install loans charts/loans/
helm install message charts/message/

# Deploy Gateway API
helm install gateway-api charts/gateway-api/

# (Optional) Deploy Istio
helm install istio charts/istio/

# Check deployments
kubectl get pods
kubectl get services
kubectl get gateway,httproute

# Scale deployments
kubectl scale deployment accounts-deployment --replicas=3
```

---

## Accessing Services

- **Via Gateway**: http://localhost/kurobank/{service}/api/*
- **Direct Access** (port-forward):
  - Accounts: http://localhost:8080/swagger-ui.html
  - Cards: http://localhost:9000/swagger-ui.html
  - Loans: http://localhost:8090/swagger-ui.html
  - Message: http://localhost:9010/swagger-ui.html

- **API Docs**: `/swagger-ui.html` on each service
- **Health/metrics**: `/actuator/*` on each service
- **Service APIs**: `/api/*` on each service

---

## Inter-Service Communication

- Uses `RestTemplate` (not OpenFeign)
- Service discovery via Kubernetes DNS (e.g. `http://accounts-service:8080/api/fetch`)
- No Eureka or registry required

---

## Database & Persistence

- Each service uses SQLite at `/data/app.db`
- Persistent Volume Claims (PVC) in Kubernetes
- Custom SQLite dialect in `common/SQLiteDialect.java`
- Ensure `/data` directory exists (Docker handles this)

---

## Common Module (`common/`)

- **Custom SQLite Dialect**: `common/src/main/java/com/kurobytes/common/dialect/SQLiteDialect.java`
- **Shared DTOs**: `common/src/main/java/com/kurobytes/common/dto/ErrorResponseDto.java`
- Build `common` first if you encounter dependency errors

---

## Development Tips

- Use Maven Wrapper (`./mvnw`) in each service directory
- Swagger UI is available on each service port
- For inter-service calls, follow existing RestTemplate usage
- Check DTOs in `common` for API contracts
- If DB errors occur, check `/data` directory and volume mounts
- Gateway API resources must be deployed for external access

---

## Troubleshooting

- If SQLite DB is not created, ensure `/data` exists and is writable
- If services cannot reach each other, check Kubernetes DNS and service names
- If you see dependency errors, build `common` module first

---

## Useful Commands

### Maven
| Command | Description |
|---------|-------------|
| `mvn clean install -Dmaven.test.skip=true` | Build all modules, skip tests |
| `./mvnw spring-boot:run` | Run a service locally |
| `./mvnw compile jib:dockerBuild` | Build Docker image with Jib |

### Docker
| Command | Description |
|---------|-------------|
| `docker compose up` | Start all services locally |
| `docker compose down` | Stop and remove all containers |
| `docker images` | List Docker images |
| `docker run -p 8080:8080 kurobytes/accounts:s20` | Run a service container |

### Kubernetes
| Command | Description |
|---------|-------------|
| `helm install accounts charts/accounts/` | Deploy accounts service |
| `kubectl get pods` | List pods |
| `kubectl get services` | List services |
| `kubectl scale deployment accounts-deployment --replicas=3` | Scale deployment |

---

## References & Links

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Spring Doc](https://springdoc.org/)
- [Open API](https://www.openapis.org/)
- [Docker](https://www.docker.com)
- [Google Jib](https://github.com/GoogleContainerTools/jib)
- [Kubernetes](https://kubernetes.io/)
- [Helm](https://helm.sh)
- [Istio](https://istio.io)
- [OpenTelemetry](https://opentelemetry.io/)
- [Prometheus](https://prometheus.io/)
- [Grafana](https://grafana.com)

---

## Notes
- This project is based on the KuroBytes Udemy course, but has been significantly modernized for Kubernetes-native operation.
- For any issues, check the CLAUDE.md for additional troubleshooting and context.
