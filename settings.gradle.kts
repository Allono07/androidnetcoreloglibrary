pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
      plugins {
    id("com.android.library") version "8.1.4"
    id("org.jetbrains.kotlin.android") version "2.1.0"
    id("com.google.devtools.ksp") version "1.9.10-1.0.13"
  }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "netcore-sdk-capturer"
include(":NetcoreSDKCapturer")