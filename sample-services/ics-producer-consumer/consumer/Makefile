#  ========================LICENSE_START=================================
#  O-RAN-SC
#
#  Copyright (C) 2024: OpenInfra Foundation Europe
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

# Define variables
IMAGE_NAME = "o-ran-sc/nonrtric-sample-icsconsumer"
DOCKERFILE = Dockerfile

# Default target
.PHONY: all
all: build run

# Target to build the Maven JAR
.PHONY: jar
jar:
	mvn clean package

# Target to build the Docker image
.PHONY: build
build:
	docker build -t $(IMAGE_NAME) -f $(DOCKERFILE) .

# Target to run the Docker container
.PHONY: run
run:
	docker run -p 8081:8081 $(IMAGE_NAME)

# Target to stop and remove the Docker container
.PHONY: stop
stop:
	docker stop $(IMAGE_NAME) || true
	docker rm $(IMAGE_NAME) || true

# Target to clean up
.PHONY: clean
clean: stop
	docker rmi $(IMAGE_NAME) || true
