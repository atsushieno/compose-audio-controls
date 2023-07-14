pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven(uri("https://jitpack.io"))
    }
}

rootProject.name = "ComposeAudioControls"
include(":app")
include(":compose-audio-controls")
include(":aap-resident-midi-keyboard")
include(":compose-audio-controls-midi")
