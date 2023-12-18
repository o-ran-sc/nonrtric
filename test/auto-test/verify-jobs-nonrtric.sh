#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
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

TEST_DIRECTORY="test/auto-test"
TEST_SCRIPT="./Suite-Verify-jobs.sh"
DOCKER_COMPOSE_VERSION="v2.21.0"

# Check if jq is installed, and install it if not
if ! command -v jq &> /dev/null; then
    echo "Installing jq..."
    sudo apt-get update
    sudo apt-get install -y jq
fi

# Function to install Docker Compose version 2
install_docker_compose() {
    echo "Installing Docker Compose version 2..."
    sudo curl -L "https://github.com/docker/compose/releases/download/"$DOCKER_COMPOSE_VERSION"/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
    sudo chmod +x /usr/local/bin/docker-compose
    sudo ln -s /usr/local/bin/docker-compose /usr/libexec/docker/cli-plugins/docker-compose
}

# Function to remove Docker Compose
remove_docker_compose() {
    echo "Removing Docker Compose..."
    sudo rm /usr/local/bin/docker-compose
}

# Check if docker-compose is installed, and install it if not
if ! command -v docker-compose &> /dev/null; then
    install_docker_compose
fi

cd "$TEST_DIRECTORY"
sudo chmod 775 "$TEST_SCRIPT"
"$TEST_SCRIPT" remote-remove docker release --env-file ../common/test_env-oran-h-release.sh

# Remove docker-compose after tests are done
if command -v docker-compose &> /dev/null; then
    remove_docker_compose
fi

# Remove jq after tests are done
if command -v jq &> /dev/null; then
    echo "Removing jq..."
    sudo apt-get remove -y jq
fi
