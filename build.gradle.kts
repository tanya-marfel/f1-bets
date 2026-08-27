plugins {
    java
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.lombok)
    alias(libs.plugins.spotless)
    jacoco
}

group = "com.sporty"
version = "0.0.1-SNAPSHOT"
description = "f1-bets"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

repositories {
    mavenCentral()
}

// The freefair plugin wires Lombok into every source set; this is the only
// place its version needs to be declared.
lombok {
    version = libs.versions.lombok
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.springdoc.openapi.starter.webmvc.ui)
    implementation(libs.spring.boot.flyway)
    implementation(libs.flyway.core)
    implementation(libs.flyway.database.postgresql)
    runtimeOnly(libs.postgresql)
    developmentOnly(libs.spring.boot.docker.compose)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
    testImplementation(libs.wiremock.standalone)
    testRuntimeOnly(libs.junit.platform.launcher)
}

spotless {
    java {
        target("src/**/*.java")
        targetExclude("**/build/**", "**/generated/**")
        palantirJavaFormat(libs.versions.palantir.java.format.get())
        removeUnusedImports()
        formatAnnotations()
        trimTrailingWhitespace()
        endWithNewline()
    }
}

// Never format generated OpenAPI sources
tasks.named("spotlessJava") {
    mustRunAfter(tasks.named("openApiGenerate"))
}

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

tasks.test {
    useJUnitPlatform { includeTags("small") }
}

tasks.register("smallTest") {
    group = "verification"
    description = "Runs Small (unit) tests."
    dependsOn(tasks.test)
}

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
    toolVersion = libs.versions.jacoco.get()
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

