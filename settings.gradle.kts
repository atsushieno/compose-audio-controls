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

rootProject.name = "ComposeAudioControls"
include(":app")
include(":compose-audio-controls")
include(":compose-audio-controls-midi")
