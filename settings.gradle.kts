rootProject.name = "f1-bets"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.openapi.generator") {
                useModule("org.openapitools:openapi-generator-gradle-plugin:${requested.version}")
            }
        }
    }
}

