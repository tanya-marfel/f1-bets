.DEFAULT_GOAL := help
COMPOSE := docker compose --profile app

.PHONY: help
help: ## Show available targets
	@grep -E '^[a-zA-Z0-9_-]+:.*?## ' $(MAKEFILE_LIST) | awk 'BEGIN{FS=":.*?## "}{printf "  \033[36m%-16s\033[0m %s\n", $$1, $$2}'

.PHONY: build
build: ## Build the executable boot jar
	./gradlew bootJar

.PHONY: test
test: ## Run all tests + 80% coverage gate (needs Docker)
	./gradlew check

.PHONY: run
run: ## Run locally with bootRun (Compose auto-starts Postgres)
	./gradlew bootRun

.PHONY: up
up: build ## Build the jar, then run app + Postgres in Docker
	$(COMPOSE) up --build

.PHONY: up-detached
up-detached: build ## Same as `up` but detached
	$(COMPOSE) up --build -d

.PHONY: down
down: ## Stop and remove the Docker stack
	$(COMPOSE) down

.PHONY: logs
logs: ## Tail the app container logs
	$(COMPOSE) logs -f app

.PHONY: clean
clean: ## Gradle clean and remove the Docker stack + volume
	./gradlew clean
	$(COMPOSE) down -v

