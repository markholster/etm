#!/bin/bash
VERSION=$(./gradlew properties | grep ^version: | sed 's/^.*: //')
if [[ "$VERSION" == *SNAPSHOT ]]
then
  echo "Version is still a SNAPSHOT"
  exit 1
fi
echo "Releasing Enterprise Telemetry Monitor $VERSION"

echo "Compiling the project"
./gradlew clean build
if [ $? -ne 0 ]; then
  echo "Build failed"
  exit 1
fi

echo "Building the documentation"
./gradlew -p etm-documentation clean asciidoctor
if [ $? -ne 0 ]; then
  echo "Generating documentation failed"
  exit 1
fi

echo "Publish the distribution to www.jecstar.com"
scp etm-distribution/build/distributions/etm-$VERSION.* mark@www.jecstar.com:/home/mark
if [ $? -ne 0 ]; then
  echo "Publishing distribution failed"
  exit 1
fi

echo "Now make the distribution available on the website"
read -n1 -r -p "Press any key when the distibution is available for download..." key

echo "Generating docker image"
cd etm-docker/build/docker
docker image build -t www.jecstar.com/etm:$VERSION .
if [ $? -ne 0 ]; then
    echo "Generating docker image failed"
    exit 1
fi
#docker push www.jecstar.com/etm:3.0.0

