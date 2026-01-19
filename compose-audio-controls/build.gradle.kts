@file:OptIn(ExperimentalKotlinGradlePluginApi::class)

import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

buildscript {
    repositories {
        maven("https://plugins.gradle.org/m2/")
    }
    dependencies {
    }
}

plugins {
    id("maven-publish")
    id("signing")
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.dokkaPlugin)
    alias(libs.plugins.binaryCompatibilityValidatorPlugin)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrainsComposePlugin)
    alias(libs.plugins.vanniktech.maven.publish)
}

group = "org.androidaudioplugin"
version = libs.versions.compose.audio.controls.get()

kotlin {
    jvmToolchain(17)

    jvm {
        compilerOptions.jvmTarget.set(JvmTarget.JVM_11)
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    androidTarget {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs {
        browser {}
    }
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).onEach {
        it.binaries {
            framework { baseName = "ComposeAudioControls" }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.ui)

                implementation(libs.kotlinx.datetime)
                implementation(libs.ktmidi)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.coroutines.test)
            }
        }
        val jvmMain by getting
        val androidMain by getting {
            dependencies {
                implementation(libs.core.ktx)
            }
        }
        val androidInstrumentedTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.junit)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(libs.junit)
            }
        }
    }
}

android {
    namespace = "org.androidaudioplugin.composeaudiocontrols"
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["test"].assets.srcDir("src/commonTest/resources") // kind of hack...
    defaultConfig {
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
    buildTypes {
        val debug by getting
        val release by getting
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

val gitProjectName = "compose-audio-controls"
val packageName = project.name
val packageDescription = "Collection of Audio Controls for Jetpack Compose and Compose for Multiplatform"
// my common settings
val packageUrl = "https://github.com/atsushieno/$gitProjectName"
val licenseName = "MIT"
val licenseUrl = "https://github.com/atsushieno/$gitProjectName/blob/main/LICENSE"
val devId = "atsushieno"
val devName = "Atsushi Eno"
val devEmail = "atsushieno@gmail.com"

// Common copy-pasted
mavenPublishing {
    publishToMavenCentral()
    if (project.hasProperty("mavenCentralUsername") || System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername") != null)
        signAllPublications()
    coordinates(group.toString(), project.name, version.toString())
    pom {
        name.set(packageName)
        description.set(packageDescription)
        url.set(packageUrl)
        scm { url.set(packageUrl) }
        licenses { license { name.set(licenseName); url.set(licenseUrl) } }
        developers { developer { id.set(devId); name.set(devName); email.set(devEmail) } }
    }
}
