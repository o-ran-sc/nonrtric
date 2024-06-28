#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2024: OpenInfra Foundation Europe.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

# Build image from Dockerfile with/without custom image tag
# Optionally push to external image repo

print_usage() {
    echo "Usage: build.sh no-push|<docker-hub-repo-name> [<image-tag>]"
    exit 1
}

if [ $# -ne 1 ] && [ $# -ne 2 ]; then
    print_usage
fi

IMAGE_NAME="o-ran-sc/nonrtric-sample-helloworld-sme-invoker"
IMAGE_TAG="latest"
REPO=""
if [ $1 == "no-push" ]; then
    echo "Only local image build"
else
    REPO=$1
    echo "Attempt to push built image to: "$REPO
fi

shift
while [ $# -ne 0 ]; do
    if [ $1 == "--tag" ]; then
        shift
        if [ -z "$1" ]; then
            print_usage
        fi
        IMAGE_TAG=$1
        echo "Setting image tag to: "$IMAGE_TAG
        shift
    else
        echo "Unknown parameter: $1"
        print_usage
    fi
done

IMAGE=$IMAGE_NAME:$IMAGE_TAG

export DOCKER_DEFAULT_PLATFORM=linux/amd64
CURRENT_PLATFORM=$(docker system info --format '{{.OSType}}/{{.Architecture}}')
if [ $CURRENT_PLATFORM != $DOCKER_DEFAULT_PLATFORM ]; then
    echo "Image may not work on the current platform: $CURRENT_PLATFORM, only platform $DOCKER_DEFAULT_PLATFORM supported"
fi

echo "Building image $IMAGE"
docker build -t $IMAGE_NAME:$IMAGE_TAG .
if [ $? -ne 0 ]; then
    echo "BUILD FAILED"
    exit 1
fi
echo "BUILD OK"

if [ "$REPO" != "" ]; then
    echo "Tagging image"
    NEW_IMAGE=$REPO/$IMAGE_NAME:$IMAGE_TAG
    docker tag $IMAGE $NEW_IMAGE
    if [ $? -ne 0 ]; then
        echo "RE-TAGGING FAILED"
        exit 1
    fi
    echo "RE-TAG OK"

    echo "Pushing image $NEW_IMAGE"
    docker push $NEW_IMAGE
    if [ $? -ne 0 ]; then
        echo "PUSHED FAILED"
        echo " Perhaps not logged into docker-hub repo $REPO?"
        exit 1
    fi
    IMAGE=$NEW_IMAGE
    echo "PUSH OK"
fi

echo "IMAGE OK: $IMAGE"
echo "DONE"
