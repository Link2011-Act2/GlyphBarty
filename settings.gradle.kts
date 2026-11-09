pluginManagement {
    val trickuildVersion = "2926f49060e58e2701f97de3c92322be26ed9811"
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
        maven("https://jitpack.io") {
            content {
                includeModule("com.github.Link2011-Act2.Trickuild", "trickuild-gradle-plugin")
                includeModule("com.github.Link2011-Act2.Trickuild", "trickuild-core")
                includeModule("com.github.Link2011-Act2.Trickuild", "trickuild-android-adapter")
            }
            metadataSources { mavenPom(); ignoreGradleMetadataRedirection() }
        }
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "dev.trickuild") {
                useModule("com.github.Link2011-Act2.Trickuild:trickuild-gradle-plugin:" + (requested.version ?: trickuildVersion))
            }
        }
    }
    plugins { id("dev.trickuild") version trickuildVersion }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://jitpack.io")
            content { includeGroup("com.github.Link2011-Act2"); includeGroup("com.github.Link2011-Act2.Goldy") }
        }
    }
}

rootProject.name = "GlyphBarty"
include(":app")
 