#!/bin/bash
RED='\033[0;31m'
GREEN='\033[0;32m'
NC='\033[0m'
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
VERSION=$(./gradlew properties | grep ^version: | sed 's/^.*: //')
VERSION_WILDCARD_BUGFIX=$(echo $VERSION | sed 's/.$/x/')

function logError() {
  echo -e "[$RED ERROR $NC] - $1"
}

function logOk() {
  echo -e "[$GREEN OK $NC] - $1"
}

function checkVersionInDocumentation() {
  VERSION_COUNT=$(grep -o "$VERSION" "$1" | wc -l)
  if [[ "$VERSION_COUNT" != "$2" ]]
  then
    logError "'$1' seems to mention the old version because we expected mentioning version $VERSION $2 times but found it $VERSION_COUNT times."
    exit 1
  fi
  logOk "'$1' contains correct version."
}

if [[ "$VERSION" == *SNAPSHOT ]]
then
  logError "Version is still a SNAPSHOT."
  exit 1
fi
logOk "Version not a SNAPSHOT."

ETM_PUBLIC_VERSION=$(etm-public/gradlew properties -b etm-public/build.gradle | grep ^version: | sed 's/^.*: //')
if [[ "$VERSION" != "$ETM_PUBLIC_VERSION" ]]
then
  logError "etm-public has a different version: $ETM_PUBLIC_VERSION. Expected version $VERSION."
  exit 1
fi
logOk "etm-public has same version."

ES_VERSION=$(./gradlew properties | grep ^version_elasticsearch: | sed 's/^.*: //')
ES_PUBLIC_VERSION=$(etm-public/gradlew properties -b etm-public/build.gradle | grep ^version_elasticsearch: | sed 's/^.*: //')
if [[ "$ES_VERSION" != "$ES_PUBLIC_VERSION" ]]
then
  logError "etm-public has a different elasticsearch version: $ES_PUBLIC_VERSION. Expected version $ES_VERSION."
  exit 1
fi
logOk "etm-public has same elasticsearch version."

checkVersionInDocumentation "$SCRIPT_DIR"/etm-public/etm-documentation/docs/getting-started/installation.md 4
checkVersionInDocumentation "$SCRIPT_DIR"/etm-public/etm-documentation/docs/setup/installation-on-windows.md 2
checkVersionInDocumentation "$SCRIPT_DIR"/etm-public/etm-documentation/docs/setup/installation-with-tgz.md 4
checkVersionInDocumentation "$SCRIPT_DIR"/etm-public/etm-documentation/docs/setup/installation-with-zip.md 4
checkVersionInDocumentation "$SCRIPT_DIR"/etm-public/etm-documentation/docs/setup/installation-with-docker.md 2
checkVersionInDocumentation "$SCRIPT_DIR"/etm-public/etm-documentation/docs/setup/installation-with-kubernetes.md 1

RELEASE_DATE=$(cat "$SCRIPT_DIR"/etm-public/etm-documentation/docs/support-matrix/README.md | grep "ETM $VERSION_WILDCARD_BUGFIX" | cut -d'|' -f2)
if [[ -z "${RELEASE_DATE// }" ]]
then
  logError "Release date in support matrix documentation is empty."
  exit 1
fi
logOk "Release date fount in support matrix."

EOL_DATE=$(cat "$SCRIPT_DIR"/etm-public/etm-documentation/docs/support-matrix/README.md | grep "ETM $VERSION_WILDCARD_BUGFIX" | cut -d'|' -f6)
if [[ -z "${EOL_DATE// }" ]]
then
  logError "End of life date in support matrix documentation is empty."
  exit 1
fi
logOk "End of life date found in support matrix."

./gradlew clean build
if [ $? -ne 0 ]; then
  logError "Build failed."
  exit 1
fi
logOk "Build succeeded."

./gradlew :etm-distribution:linuxJreX64DistTar
if [ $? -ne 0 ]; then
  logError "Generating Linux x86_64 distribution failed."
  exit 1
fi
logOk "Linux x86_64 distribution generated."

scp -r "$SCRIPT_DIR"/etm-distribution/build/distributions/* www.jecstar.com:/home/mark/etm-dist
if [ $? -ne 0 ]; then
  logError "Failed to upload distribution to server."
  exit 1
fi
logOk "Distributions uploaded to server."
cd "$SCRIPT_DIR" || exit

cd "$SCRIPT_DIR"/etm-public/etm-documentation || exit
yarn docs:build
if [ $? -ne 0 ]; then
  logError "Generating documentation failed."
  exit 1
fi
logOk "Documentation generated."

echo "Uploading the documentation to www.jecstar.com"
scp -r docs/.vuepress/dist www.jecstar.com:/home/mark/etm-docs
if [ $? -ne 0 ]; then
  logError "Failed to upload documentation to server."
  exit 1
fi
logOk "Documentation uploaded to server."
cd "$SCRIPT_DIR" || exit

podman rmi docker.io/adoptopenjdk/openjdk11:alpine-slim
buildah unshare "$SCRIPT_DIR"/etm-public/etm-buildah/build-oci.sh "$VERSION"
if [ $? -ne 0 ]; then
    logError "Generating OCI image failed."
    exit 1
fi
logOk "OCI image created."

podman push eu.gcr.io/virtual-ellipse-208415/etm:"$VERSION"
if [ $? -ne 0 ]; then
    logError "Pushing OCI image failed."
    exit 1
fi
logOk "OCI image pushed."

#==== Push subtree
#git subtree push --prefix=etm-public git@github.com:jecstarinnovations/etm.git develop
#cd ../etm-public
#git checkout develop
#git pull
#git checkout master
#git merge develop
#git tag -a v$VERSION
#git push --tags