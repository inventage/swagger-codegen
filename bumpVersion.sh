#!/usr/bin/env bash
# Sets the version in all maven submodules to the given version
#
# $1 The version to set

set -eo pipefail

if [ "$#" -ne 1 ]
then
  echo "Usage: $0 1.1.12 Note: Don't append the SNAPSHOT, it will be appended automatically"
  exit 1
fi

readonly BASE_DIR=$(cd "$(dirname $0)"; pwd)
readonly NEW_VERSION=$1
readonly MVN=$(which mvn)

${MVN} org.codehaus.mojo:versions-maven-plugin:2.6:set -DnewVersion=${NEW_VERSION}-SNAPSHOT
