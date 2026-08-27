rootProject.name = "f1-bets"

pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
    resolutionStrategy {
        eachPlugin {

            when (requested.id.id) {
                "org.openapi.generator" ->
                    useModule("org.openapitools:openapi-generator-gradle-plugin:${requested.version}")

                "com.diffplug.spotless" ->
                    useModule("com.diffplug.spotless:spotless-plugin-gradle:${requested.version}")
            }
        }
    }
}

