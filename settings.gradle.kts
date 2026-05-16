pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Dumitru_Estera_ActivitateDMC2026"
include(":laborator2")
include(":laborator3")
include(":laborator4_laborator5")
include(":laborator6_laborator7")
include(":laborator8")
include(":laborator9")
include(":dumitruestera")
include(":proiect")
include(":laborator10")
include(":laborator11")
