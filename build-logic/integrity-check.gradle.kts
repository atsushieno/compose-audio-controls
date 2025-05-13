
val thisKts = file("build.gradle.kts").readText()
val forewordsFrag = file("../build-logic/forewords.gradle.kts").readText()
val thisFrag = file("build.gradle.kts.in").readText()
val pubFrag = file("../build-logic/publication.gradle.kts").readText()
val integrityFrag = file("../build-logic/integrity-check.gradle.kts").readText()
if (forewordsFrag + thisFrag + pubFrag + integrityFrag != thisKts)
    throw InvalidPluginException("ERROR: build script is not up to date. Run `bash generate-build-script.sh`.")
