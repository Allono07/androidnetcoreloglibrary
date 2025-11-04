
plugins {
    id("com.android.library") version "8.9.1"
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("maven-publish")

}

// SDK version
val sdkVersion = "0.0.beta"
val sdkGroupId = "com.android.netcoresdkcapturer"
val sdkArtifactId = "netcore-sdk-capturer"

android {
    namespace = "com.android.netcoresdkcapturer"
    compileSdk = 36

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    
    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    
    // Room Database
    implementation("androidx.room:room-runtime:2.8.3")
    implementation("androidx.room:room-ktx:2.8.3")
    ksp("androidx.room:room-compiler:2.8.3")
    
    // Lifecycle
    implementation("androidx.lifecycle:lifecycle-process:2.9.4")
    
    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.11.0")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

// Maven publishing configuration
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = sdkGroupId
            artifactId = sdkArtifactId
            version = sdkVersion

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Netcore SDK Capturer")
                description.set("Android SDK for capturing and uploading Netcore Smartech event logs")
                url.set("https://github.com/Allono07/androidnetcoreloglibrary.git")
                
                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }
                
                developers {
                    developer {
                        id.set("allono07")
                        name.set("Allen Thomson")
                        email.set("allono.at@gmail.com") // Update this
                    }
                }
                
                scm {
                    connection.set("scm:git:git://github.com/Allono07/androidnetcoreloglibrary.git")
                    developerConnection.set("scm:git:ssh://github.com/Allono07/androidnetcoreloglibrary.git")
                    url.set("https://github.com/Allono07/androidnetcoreloglibrary")
                }
            }
        }
    }

    repositories {
//        maven {
//            name = "sonatype"
//            val releasesRepoUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
//            val snapshotsRepoUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
//            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl
//
//            credentials {
//                username = project.findProperty("ossrhUsername") as String? ?: System.getenv("OSSRH_USERNAME")
//                password = project.findProperty("ossrhPassword") as String? ?: System.getenv("OSSRH_PASSWORD")
//            }

        maven{
            name ="GithubPackages"
            url =uri("https://maven.pkg.github.com/Allono07/androidnetcoreloglibrary")
            credentials{
                username = "Allono07"
                password ="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"
            }
        }
    }
}

// Signing configuration (required for Maven Central)
//signing {
//    sign(publishing.publications["release"])
//}