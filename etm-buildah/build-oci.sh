#!/bin/bash
if [[ $# -eq 0 ]] ; then
    echo 'Version not supplied'
    exit 1
fi
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
VERSION=$1
# SETUP ALPINE LINUX
container=$(buildah from adoptopenjdk/openjdk14:alpine-slim)
buildah config --created-by "Jecstar Innovation" $container

# INSTALL ETM
dir=$(buildah mount $container)
tar -zxvf $SCRIPT_DIR/../etm-distribution/build/distributions/etm-$VERSION.tgz -C $dir/opt
cp -f $SCRIPT_DIR/config/etm.yml $dir/opt/etm-$VERSION/config

## CONFIGURE ETM
buildah config --port 8080 --entrypoint "[\"/opt/etm-$VERSION/bin/etm\", \"console\"]" $container
buildah commit --format docker $container docker.jecstar.com/etm:$VERSION
buildah tag docker.jecstar.com/etm:$VERSION eu.gcr.io/virtual-ellipse-208415/etm:$VERSION
buildah unmount $container
