# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a Spring Boot microservices project that has been migrated from Spring Cloud to Kubernetes-native solutions. It's based on the Udemy course "Master Microservices with Spring Boot, Docker, Kubernetes" by KuroBytes.

### Key Migration Context
The project recently underwent a major migration (see commit history):
- **From**: Spring Cloud (Config Server, Eureka, Gateway, Resilience4j)
- **To**: Kubernetes native (ConfigMaps, DNS, Gateway API, Istio)
- **Database**: Migrated from H2 to SQLite with persistent volumes

## Essential Commands

### Building Services
```bash
# Build all services (from root directory)
mvn clean install

# Build without tests
mvn clean install -Dmaven.test.skip=true

# Build individual service (from service directory, e.g., /src/accounts/)
./mvnw clean package

# Build Docker images using Jib (from service directory)
./mvnw compile jib:dockerBuild
```

### Running Services Locally
```bash
# Run individual service (from service directory)
./mvnw spring-boot:run

# Run with Docker Compose (from root)
docker compose up

# Run specific service
docker run -p 8080:8080 kurobytes/accounts:s20
```

### Testing
```bash
# Run unit tests for a service (from service directory)
./mvnw test

# Run all tests including integration
./mvnw verify

# Load testing example
ab -n 10 -c 2 -v 3 http://localhost:8072/kurobank/cards/api/contact-info
```

### Kubernetes Deployment
```bash
# Deploy services using Helm
helm install accounts charts/accounts/
helm install cards charts/cards/
helm install loans charts/loans/
helm install gateway-api charts/gateway-api/

# Check deployments
kubectl get pods
kubectl get services
kubectl get gateway,httproute

# Scale deployments
kubectl scale deployment accounts-deployment --replicas=3
```

## Architecture Overview

### Microservices Structure
```
/src/
├── accounts/     # Account management service (port 8080)
├── cards/        # Credit card service (port 9000)
├── loans/        # Loan service (port 8090)
├── gatewayserver/# API Gateway (being phased out for K8s Gateway API)
├── message/      # Event handling service
└── parent-bom/   # 親POM（全サービスの依存関係とバージョンを一元管理）
    └── common/   # 共有ライブラリ（SQLiteカスタム方言、共通DTO）
```

### Technology Stack
- **Java 21** with **Spring Boot 3.4.1**
- **SQLite** for persistence (each service has its own DB at `/data/app.db`)
- **Maven** multi-module project with BOM pattern
- **Docker** with **Google Jib** for containerization
- **Kubernetes** with **Helm** for orchestration
- **Gateway API** for ingress routing (replacing Spring Cloud Gateway)
- **Istio** for service mesh capabilities (optional)

### Inter-Service Communication
Services communicate using RestTemplate (migrated from OpenFeign):
- Service discovery via Kubernetes DNS
- Example: `http://accounts:8080/api/fetch`
- No longer uses Eureka or service registry

### Configuration Management
- Environment variables for configuration (no Spring Cloud Config)
- Kubernetes ConfigMaps and Secrets
- External Secrets Operator for cloud secret management (optional)

### Key Endpoints
Each service exposes:
- `/swagger-ui.html` - API documentation
- `/actuator/*` - Health and metrics endpoints
- Service-specific APIs under `/api/*`

### Database Persistence
- SQLite databases mounted at `/data/app.db`
- Uses custom SQLite dialect from common module
- Persistent Volume Claims in Kubernetes

### Docker Image Naming
Images follow pattern: `kurobytes/{service-name}:s20`
- accounts → kurobytes/accounts:s20
- cards → kurobytes/cards:s20
- loans → kurobytes/loans:s20

## Development Tips

### Working with Individual Services
1. Navigate to service directory: `cd src/accounts/`
2. Use Maven wrapper: `./mvnw` instead of global `mvn`
3. SQLite DB is created automatically at `/data/app.db`

### API Testing
- Swagger UI available at service ports (8080, 9000, 8090)
- Use RestTemplate patterns from existing code for inter-service calls
- Check `ContactInfoDto` and similar DTOs for API contracts

### Observability
- OpenTelemetry configured for tracing
- Micrometer with Prometheus for metrics
- Actuator endpoints enabled for monitoring

### Common Issues
- Ensure `/data` directory exists for SQLite (Docker handles this)
- Services expect other services at Kubernetes DNS names
- Gateway API resources must be deployed for external access
- SQLite custom dialect is in parent-bom/common module - must be built first