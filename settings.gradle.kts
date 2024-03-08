pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }
}

//enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
rootProject.name = "ComposeAudioControls"
include(":app")
include(":compose-audio-controls")
include(":compose-audio-controls-midi")
