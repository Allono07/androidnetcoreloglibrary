pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
  // Pin plugin versions centrally so applied plugins (Kotlin, KSP, AGP)
  // use a consistent, compatible set. This prevents cases where KSP
  // cannot find Kotlin compiler classes at plugin initialization time.
  plugins {
    id("com.android.library") version "8.1.4"
    id("org.jetbrains.kotlin.android") version "2.2.20"
    id("com.google.devtools.ksp") version "2.3.1"
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