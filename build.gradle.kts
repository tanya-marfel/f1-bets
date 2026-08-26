plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.openapi.generator") version "7.14.0"
    id("io.freefair.lombok") version "9.5.0"
    jacoco
}

group = "com.sporty"
version = "0.0.1-SNAPSHOT"
description = "f1-bets"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.0")
    implementation("org.springframework.boot:spring-boot-flyway")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")
    developmentOnly("org.springframework.boot:spring-boot-docker-compose")

    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    testCompileOnly("org.projectlombok:lombok:1.18.46")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.46")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter:1.20.6")
    testImplementation("org.testcontainers:postgresql:1.20.6")
    testImplementation("org.wiremock:wiremock-standalone:3.9.2")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// --- Contract-first: generate DTOs + API interfaces from the OpenAPI spec ---

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$projectDir/src/main/resources/openapi/f1-bets.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/openapi").get().asFile.path)
    apiPackage.set("com.sporty.f1bets.generated.api")
    modelPackage.set("com.sporty.f1bets.generated.model")
    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot3" to "true",
            "useJakartaEe" to "true",
            "useTags" to "true",
            "useResponseEntity" to "true",
            "useBeanValidation" to "true",
            "documentationProvider" to "springdoc",
            "openApiNullable" to "false",
            "hideGenerationTimestamp" to "true",
        )
    )
}

sourceSets["main"].java.srcDir(layout.buildDirectory.dir("generated/openapi/src/main/java"))

tasks.named("compileJava") {
    dependsOn(tasks.named("openApiGenerate"))
}

// --- Test sizes (Google model) selected by JUnit tags over the single test source set ---

// Default `test` runs Small tests only: fast, no Docker required.
tasks.test {
    useJUnitPlatform { includeTags("small") }
}

// Explicit alias so both size tasks exist by name (mirrors `test`).
tasks.register("smallTest") {
    group = "verification"
    description = "Runs Small (unit) tests."
    dependsOn(tasks.test)
}

// Medium tests: Spring + Testcontainers + WireMock (requires Docker).
val mediumTest = tasks.register<Test>("mediumTest") {
    group = "verification"
    description = "Runs Medium (integration) tests."
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("medium") }
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(mediumTest)
    dependsOn("jacocoTestCoverageVerification")
}

jacoco {
    toolVersion = "0.8.13"
}

val coverageExclusions = listOf(
    "**/F1BetsApplication*",
    "**/config/**",
    "**/generated/**",
    "**/*Request*",
    "**/*Response*",
    "**/error/**",
    "**/*Properties*",
    "**/openf1/OpenF1*Dto*",
)

tasks.named<JacocoReport>("jacocoTestReport") {
    dependsOn(tasks.test, mediumTest)
    executionData(
        fileTree(layout.buildDirectory).include("jacoco/test.exec", "jacoco/mediumTest.exec")
    )
    reports {
        xml.required = true
        html.required = true
    }
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) { exclude(coverageExclusions) }
    }))
}

tasks.named<JacocoCoverageVerification>("jacocoTestCoverageVerification") {
    dependsOn(tasks.test, mediumTest)
    executionData(
        fileTree(layout.buildDirectory).include("jacoco/test.exec", "jacoco/mediumTest.exec")
    )
    classDirectories.setFrom(files(classDirectories.files.map {
        fileTree(it) { exclude(coverageExclusions) }
    }))
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}

