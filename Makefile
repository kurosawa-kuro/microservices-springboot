# Kubernetes-Native Microservices Makefile
# Provides common commands for building, testing, and deploying microservices

# Variables
SERVICES = accounts-service cards-service loans-service message-service
DOCKER_REGISTRY = kurobytes
IMAGE_TAG = s20
MAVEN = ./mvnw
KUBECTL = kubectl
HELM = helm

# Colors for output
GREEN = \033[0;32m
YELLOW = \033[0;33m
RED = \033[0;31m
NC = \033[0m # No Color

.PHONY: help
help: ## Show this help message
	@echo 'Usage: make [target]'
	@echo ''
	@echo 'Targets:'
	@awk 'BEGIN {FS = ":.*?## "} /^[a-zA-Z_-]+:.*?## / {printf "  $(GREEN)%-20s$(NC) %s\n", $$1, $$2}' $(MAKEFILE_LIST)

# Build targets
.PHONY: build
build: ## Build all services (JAR files)
	@echo "$(YELLOW)Building all services...$(NC)"
	mvn clean install -DskipTests

.PHONY: build-with-tests
build-with-tests: ## Build all services with tests
	@echo "$(YELLOW)Building all services with tests...$(NC)"
	mvn clean install

.PHONY: build-service
build-service: ## Build specific service (usage: make build-service SERVICE=accounts-service)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Building $(SERVICE) service...$(NC)"
	cd $(SERVICE) && $(MAVEN) clean package

# Docker targets
.PHONY: docker-build
docker-build: ## Build Docker images for all services using Jib
	@echo "$(YELLOW)Building Docker images for all services...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(GREEN)Building $$service...$(NC)"; \
		cd $$service && $(MAVEN) compile jib:dockerBuild && cd ..; \
	done

.PHONY: docker-build-service
docker-build-service: ## Build Docker image for specific service (usage: make docker-build-service SERVICE=accounts-service)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Building Docker image for $(SERVICE)...$(NC)"
	cd $(SERVICE) && $(MAVEN) compile jib:dockerBuild

.PHONY: docker-push
docker-push: ## Push Docker images to registry
	@echo "$(YELLOW)Pushing Docker images...$(NC)"
	@for service in $(SERVICES); do \
		echo "$(GREEN)Pushing $$service...$(NC)"; \
		docker push $(DOCKER_REGISTRY)/$$service:$(IMAGE_TAG); \
	done

# Run targets
.PHONY: run-local
run-local: ## Run specific service locally (usage: make run-local SERVICE=accounts-service)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Running $(SERVICE) locally...$(NC)"
	cd $(SERVICE) && $(MAVEN) spring-boot:run

.PHONY: docker-compose-up
docker-compose-up: ## Start all services with Docker Compose
	@echo "$(YELLOW)Starting services with Docker Compose...$(NC)"
	docker compose up -d

.PHONY: docker-compose-down
docker-compose-down: ## Stop all services with Docker Compose
	@echo "$(YELLOW)Stopping services with Docker Compose...$(NC)"
	docker compose down

# Kubernetes/Helm targets
.PHONY: helm-deploy
helm-deploy: ## Deploy all services to Kubernetes using Helm
	@echo "$(YELLOW)Deploying services to Kubernetes...$(NC)"
	@for service in $(SERVICES); do \
		if [ -d "charts/$$service" ]; then \
			echo "$(GREEN)Deploying $$service...$(NC)"; \
			$(HELM) upgrade --install $$service charts/$$service/; \
		fi; \
	done
	@echo "$(GREEN)Deploying Gateway API...$(NC)"
	$(HELM) upgrade --install gateway-api charts/gateway-api/

.PHONY: helm-deploy-service
helm-deploy-service: ## Deploy specific service to Kubernetes (usage: make helm-deploy-service SERVICE=accounts)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Deploying $(SERVICE) to Kubernetes...$(NC)"
	$(HELM) upgrade --install $(SERVICE) charts/$(SERVICE)/

.PHONY: helm-deploy-istio
helm-deploy-istio: ## Deploy Istio service mesh policies
	@echo "$(YELLOW)Deploying Istio policies...$(NC)"
	$(HELM) upgrade --install istio charts/istio/

.PHONY: helm-deploy-secrets
helm-deploy-secrets: ## Deploy External Secrets Operator configuration
	@echo "$(YELLOW)Deploying External Secrets configuration...$(NC)"
	$(HELM) upgrade --install external-secrets charts/external-secrets/

.PHONY: helm-uninstall
helm-uninstall: ## Uninstall all services from Kubernetes
	@echo "$(YELLOW)Uninstalling services from Kubernetes...$(NC)"
	@for service in $(SERVICES) gateway-api istio external-secrets; do \
		if $(HELM) list | grep -q $$service; then \
			echo "$(RED)Uninstalling $$service...$(NC)"; \
			$(HELM) uninstall $$service; \
		fi; \
	done

# Kubernetes utilities
.PHONY: k8s-status
k8s-status: ## Show status of all Kubernetes resources
	@echo "$(YELLOW)Kubernetes resource status:$(NC)"
	$(KUBECTL) get pods
	@echo ""
	$(KUBECTL) get services
	@echo ""
	$(KUBECTL) get gateway,httproute

.PHONY: k8s-logs
k8s-logs: ## Show logs for specific service (usage: make k8s-logs SERVICE=accounts)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	$(KUBECTL) logs -l app=$(SERVICE) --tail=100 -f

.PHONY: k8s-port-forward
k8s-port-forward: ## Port forward to specific service (usage: make k8s-port-forward SERVICE=accounts PORT=8080)
	@if [ -z "$(SERVICE)" ] || [ -z "$(PORT)" ]; then echo "$(RED)Please specify SERVICE=<service-name> PORT=<port>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Port forwarding $(SERVICE) to localhost:$(PORT)...$(NC)"
	$(KUBECTL) port-forward service/$(SERVICE) $(PORT):$(PORT)

# Testing targets
.PHONY: test
test: ## Run tests for all services
	@echo "$(YELLOW)Running tests for all services...$(NC)"
	mvn test

.PHONY: test-service
test-service: ## Run tests for specific service (usage: make test-service SERVICE=accounts)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Running tests for $(SERVICE)...$(NC)"
	cd src/$(SERVICE) && $(MAVEN) test

.PHONY: integration-test
integration-test: ## Run integration tests
	@echo "$(YELLOW)Running integration tests...$(NC)"
	mvn verify

# Development utilities
.PHONY: clean
clean: ## Clean all build artifacts
	@echo "$(YELLOW)Cleaning build artifacts...$(NC)"
	mvn clean
	@echo "$(GREEN)Clean complete!$(NC)"

.PHONY: swagger
swagger: ## Open Swagger UI for all services (requires port-forwarding)
	@echo "$(YELLOW)Opening Swagger UI...$(NC)"
	@echo "Accounts: http://localhost:8080/swagger-ui.html"
	@echo "Cards: http://localhost:9000/swagger-ui.html"
	@echo "Loans: http://localhost:8090/swagger-ui.html"
	@echo ""
	@echo "$(YELLOW)Note: Ensure services are running or use port-forwarding$(NC)"

.PHONY: db-shell
db-shell: ## Connect to SQLite database for a service (usage: make db-shell SERVICE=accounts)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Connecting to $(SERVICE) database...$(NC)"
	$(KUBECTL) exec -it deployment/$(SERVICE)-deployment -- sqlite3 /data/app.db

# Load testing
.PHONY: load-test
load-test: ## Run load test against service (usage: make load-test URL=http://localhost:8080/api/endpoint)
	@if [ -z "$(URL)" ]; then echo "$(RED)Please specify URL=<endpoint-url>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Running load test against $(URL)...$(NC)"
	ab -n 100 -c 10 -v 3 $(URL)

# Monitoring
.PHONY: metrics
metrics: ## Show metrics endpoints for all services
	@echo "$(YELLOW)Metrics endpoints:$(NC)"
	@echo "Accounts: http://localhost:8080/actuator/prometheus"
	@echo "Cards: http://localhost:9000/actuator/prometheus"
	@echo "Loans: http://localhost:8090/actuator/prometheus"

# Complete workflow targets
.PHONY: full-build
full-build: clean build docker-build ## Clean, build JARs, and build Docker images

.PHONY: full-deploy
full-deploy: docker-build helm-deploy ## Build Docker images and deploy to Kubernetes

.PHONY: restart-service
restart-service: ## Restart specific service in Kubernetes (usage: make restart-service SERVICE=accounts)
	@if [ -z "$(SERVICE)" ]; then echo "$(RED)Please specify SERVICE=<service-name>$(NC)"; exit 1; fi
	@echo "$(YELLOW)Restarting $(SERVICE)...$(NC)"
	$(KUBECTL) rollout restart deployment/$(SERVICE)-deployment

# Validation
.PHONY: validate
validate: ## Validate Maven project structure
	@echo "$(YELLOW)Validating project structure...$(NC)"
	mvn validate
	@echo "$(GREEN)Validation complete!$(NC)"

# Version information
.PHONY: version
version: ## Show version information
	@echo "$(YELLOW)Version Information:$(NC)"
	@echo "Java: $$(java -version 2>&1 | head -n 1)"
	@echo "Maven: $$(mvn -version | head -n 1)"
	@echo "Docker: $$(docker --version)"
	@echo "Kubernetes: $$(kubectl version --client --short 2>/dev/null || kubectl version --client)"
	@echo "Helm: $$(helm version --short)"