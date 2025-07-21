# Migration to Kubernetes OSS Native - Spring Cloud to K8s Migration

## Overview

This document describes the migration of the KuroBank microservices from Spring Cloud to Kubernetes OSS native solutions. The migration removes Spring Cloud dependencies and replaces them with Kubernetes-native alternatives.

## Changes Made

### 1. Spring Boot Version Update
- Updated Spring Boot from 3.2.x to 3.4.x in `src/kuro-bom/pom.xml`
- Removed Spring Cloud BOM dependency

### 2. Database Migration to SQLite
- **Replaced H2 with SQLite** for all microservices (accounts, cards, loans)
- Added `sqlite-jdbc:3.46.0.0` dependency to BOM
- Created custom `SQLiteDialect` class in `src/kuro-bom/common/src/main/java/com/kurobytes/common/dialect/SQLiteDialect.java`
- Updated JDBC URLs from `jdbc:h2:mem:testdb` to `jdbc:sqlite:/data/app.db`
- Added `VOLUME /data` to Dockerfiles for SQLite persistence

### 3. Spring Cloud Component Removal

| Component | Replacement | Status |
|-----------|-------------|---------|
| Config Server | K8s ConfigMap/Secret + External Secrets Operator | ✅ Removed |
| Eureka Discovery | K8s ClusterDNS | ✅ Removed |
| Spring Cloud Gateway | Gateway API + Envoy Gateway | ✅ Replaced |
| Resilience4j CircuitBreaker | Istio TrafficPolicy | ✅ Removed |
| OpenFeign | RestTemplate HTTP clients | ✅ Replaced |

### 4. Service Changes

#### Accounts Service
- Removed `@EnableFeignClients` annotation
- Replaced Feign clients with RestTemplate-based HTTP clients
- Added `RestTemplateConfig` for RestTemplate bean
- Removed Spring Cloud dependencies from pom.xml

#### Cards Service
- Removed Spring Cloud config and Eureka dependencies
- Updated database configuration to SQLite

#### Loans Service
- Removed Spring Cloud config and Eureka dependencies
- Updated database configuration to SQLite

#### Gateway Server
- Removed all Spring Cloud Gateway routing configuration
- Simplified to basic Spring Boot application
- Will be replaced by Gateway API + Envoy Gateway

#### Message Service
- Removed Spring Cloud Stream dependencies
- Simplified to basic Spring Boot application

### 5. Kubernetes Native Solutions

#### Helm Charts Created
- `charts/accounts/` - Accounts microservice with SQLite PVC
- `charts/cards/` - Cards microservice with SQLite PVC
- `charts/loans/` - Loans microservice with SQLite PVC
- `charts/gatewayserver/` - Gateway server (legacy)
- `charts/message/` - Message service
- `charts/gateway-api/` - Gateway API configuration
- `charts/istio/` - Istio traffic management
- `charts/external-secrets/` - External Secrets Operator

#### Gateway API Configuration
- Replaces Spring Cloud Gateway
- Uses Envoy Gateway as implementation
- HTTPRoute resources for path-based routing
- Supports `/kurobank/accounts`, `/kurobank/cards`, `/kurobank/loans` paths

#### Istio Integration
- Traffic management policies
- Circuit breaker configuration
- Retry policies
- Timeout configuration

#### External Secrets Operator
- Replaces Spring Cloud Config Server
- Manages secrets from external sources
- Kubernetes-native secret management

### 6. Docker Configuration
- Added Dockerfiles for all services
- SQLite services include `VOLUME /data` for persistence
- Uses OpenJDK 17 slim base image

## SQLite Configuration Details

### Database URL
```yaml
spring:
  datasource:
    url: jdbc:sqlite:/data/app.db
    driverClassName: org.sqlite.JDBC
    username: ''
    password: ''
  jpa:
    database-platform: com.kurobytes.common.dialect.SQLiteDialect
```

### Custom SQLite Dialect
The custom `SQLiteDialect` class handles SQLite-specific SQL generation and type mappings for Hibernate JPA.

### Persistence
- SQLite database files are stored in `/data/app.db`
- Kubernetes PersistentVolumeClaims provide storage
- Each service has its own SQLite database

## Deployment

### Prerequisites
- Kubernetes cluster with Gateway API CRDs installed
- Envoy Gateway installed
- Istio installed (optional, for traffic management)
- External Secrets Operator installed (optional, for secret management)

### Installation
```bash
# Install microservices
helm install accounts charts/accounts/
helm install cards charts/cards/
helm install loans charts/loans/

# Install Gateway API configuration
helm install gateway-api charts/gateway-api/

# Optional: Install Istio traffic policies
helm install istio charts/istio/

# Optional: Install External Secrets
helm install external-secrets charts/external-secrets/
```

### Verification
```bash
# Check pods are running
kubectl get pods

# Check services
kubectl get services

# Check Gateway API resources
kubectl get gateway,httproute

# Test endpoints
curl http://<gateway-ip>/kurobank/accounts/api/health
curl http://<gateway-ip>/kurobank/cards/api/health
curl http://<gateway-ip>/kurobank/loans/api/health
```

## Benefits

1. **Kubernetes Native**: Uses standard Kubernetes resources and patterns
2. **Vendor Neutral**: No dependency on Spring Cloud ecosystem
3. **Simplified Architecture**: Fewer moving parts and dependencies
4. **Better Observability**: Leverages Kubernetes and Istio observability
5. **Improved Security**: Uses Kubernetes RBAC and network policies
6. **Persistent Storage**: SQLite with PVC provides data persistence

## Limitations

1. **SQLite Limitations**: Single-writer database, not suitable for high-concurrency scenarios
2. **No Distributed Transactions**: Each service has its own database
3. **Manual Service Discovery**: Services use hardcoded URLs instead of dynamic discovery

## Future Improvements

1. Consider PostgreSQL for production workloads requiring higher concurrency
2. Implement distributed tracing with Jaeger/Zipkin
3. Add monitoring with Prometheus and Grafana
4. Implement GitOps with ArgoCD
5. Add security scanning and policies

## Migration Checklist

- [x] Update Spring Boot version to 3.4.x
- [x] Remove Spring Cloud dependencies
- [x] Migrate to SQLite database
- [x] Replace Feign clients with RestTemplate
- [x] Create Dockerfiles with SQLite volumes
- [x] Create Helm charts for all services
- [x] Configure Gateway API routing
- [x] Set up Istio traffic management
- [x] Configure External Secrets Operator
- [x] Document migration process
- [ ] Test deployment on kind cluster
- [ ] Update CI/CD pipelines
- [ ] Performance testing
- [ ] Security review
