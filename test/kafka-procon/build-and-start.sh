#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

echo "This script requires running kafka instance in a docker private network"

# Script to build and start the container
if [ $# -ne 2 ]; then
    echo "usage: ./build-and-start.sh <docker-network> <kafka-boostrapserver-host>:<kafka-boostrapserver-port>"
    echo "example: ./build-and-start.sh nonrtric-docker-net message-router-kafka:9092"
    exit 1
fi
IMAGE="kafka-procon:latest"
#Build the image
docker build -t $IMAGE .

if [ $? -ne 0 ]; then
    echo "Build failed, exiting..."
    exit 1
fi

echo "Starting kafka-procon"
#Run the container in interactive mode o port 8090.
docker run --rm -it -p "8090:8090" --network $1 -e KAFKA_BOOTSTRAP_SERVER=$2 --name kafka-procon $IMAGE

