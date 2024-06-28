#!/bin/bash

# -
#   ========================LICENSE_START=================================
#   O-RAN-SC
#   %%
#   Copyright (C) 2023-2024: OpenInfra Foundation Europe.
#   %%
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#        http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#   ========================LICENSE_END===================================
#

NAME="hello-world"
IMAGE_NAME="o-ran-sc/nonrtric-sample-helloworld"

docker build -t $IMAGE_NAME:latest .

docker run --rm -d -p 8080:8080 --name $NAME $IMAGE_NAME

sleep 10

echo "Make an HTTP request to the Hello World endpoint and display the response"
response=$(curl -s http://localhost:8080/helloworld/v1)
echo "Response from the /helloworld/v1 endpoint: "
echo "$response"
