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
}

group = "org.androidaudioplugin"
version = libs.versions.compose.audio.controls.get()

kotlin {
    jvmToolchain(17)

    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
        testRuns["test"].executionTask.configure {
            useJUnit()
        }
    }
    androidTarget {
        publishLibraryVariantsGroupedByFlavor = true
        publishLibraryVariants("debug", "release")
    }
    wasmJs {
        browser {}
    }
    listOf(
        iosArm64(),
        iosX64(),
        iosSimulatorArm64()
    ).onEach {
        it.binaries {
            framework { baseName = "ktmidi" }
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

afterEvaluate {
    val javadocJar by tasks.registering(Jar::class) {
        archiveClassifier.set("javadoc")
    }

    publishing {
        publications.withType<MavenPublication> {
            artifact(javadocJar)
            pom {
                name.set("compose-audio-controls")
                description.set("Collection of Audio Controls (WIP) for Jetpack Compose and Compose for Multiplatform")
                url.set("https://github.com/atsushieno/compose-audio-controls")
                scm {
                    url.set("https://github.com/atsushieno/compose-audio-controls")
                }
                licenses {
                    license {
                        name.set("the MIT License")
                        url.set("https://github.com/atsushieno/compose-audio-controls/blob/main/LICENSE")
                    }
                }
                developers {
                    developer {
                        id.set("atsushieno")
                        name.set("Atsushi Eno")
                        email.set("atsushieno@gmail.com")
                    }
                }
            }
        }

        repositories {
            maven {
                name = "OSSRH"
                url = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
                credentials {
                    username = System.getenv("OSSRH_USERNAME")
                    password = System.getenv("OSSRH_PASSWORD")
                }
            }
        }
    }

    // keep it as is. It is replaced by CI release builds
    signing {}
}

// https://kotlinlang.slack.com/archives/C0F4UNJET/p1685393101873549?thread_ts=1685392725.401269&cid=C0F4UNJET
tasks.withType<AbstractPublishToMaven>().configureEach {
    dependsOn(tasks.withType<Sign>())
}
