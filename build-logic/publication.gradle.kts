
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("mavenCentralUsername") || System.getenv("ORG_GRADLE_PROJECT_mavenCentralUsername") != null)
        signAllPublications()
    coordinates(group.toString(), project.name, version.toString())

    pom {
        name.set(pomName)
        description.set(pomDescription)
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
