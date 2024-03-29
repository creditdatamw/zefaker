#!/bin/sh
# .github/workflows/ci.yml

set -xe

JDK_VERSION=$1
OPTS="--no-daemon --no-build-cache --quiet"

# This runs with a modern build JDK for building because that is what we do
# anyway. Runs the tests with the matrix JDKs because that is what the users do.
#
SAVED_PATH=${PATH}
#
# build with a known good JDK and then only run
# the tests with the requested JDK
#
JAVA_HOME=${BASELINE_JAVA_HOME}
PATH=${BASELINE_JAVA_HOME}/bin:$PATH
export JAVA_HOME PATH
./gradlew ${OPTS} clean build

# switch back to the requested JDK
PATH=${SAVED_PATH}
JAVA_HOME=${BUILD_JAVA_HOME}
export JAVA_HOME PATH
./gradlew ${OPTS} clean build