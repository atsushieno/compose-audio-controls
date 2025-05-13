#!/bin/bash

# Due to Gradle's incapability of dealing with simple includes as well as
# build-logic being incapable of handling non-default dependencies,
# we copy-paste the entire package publication in every module.
# To avoid making changes to one module without making changes to others, we check
# auto-generation integrity on each module (embodied in build.gradle.kts itself).
# Even developers from 1980s with Makefile handles things better than Gradle (Makefile.in)...

MODULES=(compose-audio-controls compose-audio-controls-midi)

for MODULE in "${MODULES[@]}"
do
    echo "generating $MODULE/build.gradle.kts ..."
    cat \
      build-logic/forewords.gradle.kts \
      $MODULE/build.gradle.kts.in \
      build-logic/publication.gradle.kts \
      build-logic/integrity-check.gradle.kts > $MODULE/build.gradle.kts
done
