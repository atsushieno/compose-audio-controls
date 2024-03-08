import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.jetbrainsComposePlugin)
}

dependencies.platform(libs.compose.bom)

kotlin {
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "demoapp.js"
            }
        }
        //nodejs {}
        binaries.executable()
    }

    androidTarget {
        compilations.all {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    jvm("desktop")

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeAudioControlsDemoApp"
            isStatic = true
        }
    }

    sourceSets {
        val desktopMain by getting

        androidMain.dependencies {
            implementation(libs.core.ktx)
            implementation(libs.activity.compose)
        }
        commonMain.dependencies {
            implementation(project(":compose-audio-controls"))
            implementation(project(":compose-audio-controls-midi"))

            implementation(libs.ktmidi)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.ktmidi.jvm.desktop)
        }

        val iosMain by creating { dependsOn(commonMain.get()) }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain)
        }
    }
}

android {
    namespace = "org.androidaudioplugin.composeaudiocontrols.demoapp"
    compileSdk = 34

    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")

    defaultConfig {
        applicationId = "org.androidaudioplugin.composeaudiocontrols.demoapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    dependencies {
        debugImplementation(libs.compose.ui.tooling)
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "dev.atsushieno.ktmidi.citool"
            packageVersion = "1.0.0"
        }
    }
}

compose.experimental {
    web.application {}
}
