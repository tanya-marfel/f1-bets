.DEFAULT_GOAL := help
COMPOSE := docker compose --profile app

JAVA_VERSION := $(shell awk -F'"' '/^java = /{print $$2}' gradle/libs.versions.toml)
DOCKERFILE_JAVA_VERSION := $(shell awk -F'=' '/^ARG JAVA_VERSION/{print $$2}' Dockerfile)
GRADLE_VERSION := $(shell sed -n 's/.*gradle-\(.*\)-bin\.zip/\1/p' gradle/wrapper/gradle-wrapper.properties)
DOCKERFILE_GRADLE_VERSION := $(shell awk -F'=' '/^ARG GRADLE_VERSION/{print $$2}' Dockerfile)

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

.PHONY: check-versions
check-versions: ## Verify Dockerfile JDK/Gradle match the catalog and wrapper
	@test "$(JAVA_VERSION)" = "$(DOCKERFILE_JAVA_VERSION)" || { \
		echo "JDK drift: Dockerfile ARG JAVA_VERSION=$(DOCKERFILE_JAVA_VERSION), catalog java=$(JAVA_VERSION)"; \
		exit 1; }
	@test "$(GRADLE_VERSION)" = "$(DOCKERFILE_GRADLE_VERSION)" || { \
		echo "Gradle drift: Dockerfile ARG GRADLE_VERSION=$(DOCKERFILE_GRADLE_VERSION), wrapper=$(GRADLE_VERSION)"; \
		exit 1; }

.PHONY: up
up: check-versions ## Build the image from source, then run app + Postgres in Docker
	$(COMPOSE) up --build

.PHONY: up-detached
up-detached: check-versions ## Same as `up` but detached
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

