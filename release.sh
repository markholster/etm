#!/bin/bash
VERSION=$(./gradlew properties | grep ^version: | sed 's/^.*: //')
if [[ "$VERSION" == *SNAPSHOT ]]
then
  echo "Version is still a SNAPSHOT"
  exit 1
fi
ETM_PUBLIC_VERSION=$(etm-public/gradlew properties -b etm-public/build.gradle | grep ^version: | sed 's/^.*: //')
if [[ "$VERSION" != "$ETM_PUBLIC_VERSION" ]]
then
  echo "etm-public has a different version: $ETM_PUBLIC_VERSION. Expected version $VERSION."
  exit 1
fi
ES_VERSION=$(./gradlew properties | grep ^version_elasticsearch: | sed 's/^.*: //')
ES_PUBLIC_VERSION=$(etm-public/gradlew properties -b etm-public/build.gradle | grep ^version_elasticsearch: | sed 's/^.*: //')
if [[ "$ES_VERSION" != "$ES_PUBLIC_VERSION" ]]
then
  echo "etm-public has a different elasticsearch version: $ES_PUBLIC_VERSION. Expected version $ES_VERSION."
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
scp etm-documentation/build/asciidoc/html5/etm.html mark@www.jecstar.com:/home/mark/etm-$VERSION.html
if [ $? -ne 0 ]; then
  echo "Publishing documentation failed"
  exit 1
fi


echo "Now make the distribution available on the website"
read -n1 -r -p "Press any key when the distibution is available for download..." key

echo "Generating docker image"
cd etm-public/etm-docker/build/docker
docker image build -t docker.jecstar.com/etm:$VERSION .
if [ $? -ne 0 ]; then
    echo "Generating docker image failed"
    exit 1
fi
#docker push docker.jecstar.com/etm:$VERSION


#==== Push subtree
#git subtree push --prefix=etm-public git@github.com:jecstarinnovations/etm.git develop
#cd ../etm-public
#git checkout develop
#git pull
#git checkout master
#git merge develop
#git tag -a v$VERSION
#git push --tags

