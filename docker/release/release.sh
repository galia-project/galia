#!/bin/sh
#
# Builds a release Docker image.
#
# `docker buildx create --use --bootstrap` before running this for the first
# time.
#

if [ $# -eq 0 ]; then
    echo "Usage: build.sh <version>"
    exit 1
fi

VERSION=$1

mvn clean package -DskipTests

docker buildx build \
    --push \
    --platform linux/arm64,linux/amd64 \
    -t galia:latest -t galia:$VERSION \
    --build-arg galia_version=$VERSION \
    -f docker/release/Dockerfile .
