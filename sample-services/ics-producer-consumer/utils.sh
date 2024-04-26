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

#!/bin/bash

checkJava() {
    if ! command -v java >/dev/null 2>&1; then
        echo "Java is not installed. Please install Java."
        echo "Suggested fix for ubuntu:"
        echo "sudo apt install default-jdk"
        exit 1
    else
        echo "Java is installed."
    fi
}

checkMaven() {
    if mvn -v >/dev/null 2>&1; then
        echo "Maven is installed."
    else
        echo "Maven is not installed. Please install Maven."
        echo "Suggested fix for ubuntu:"
        echo "sudo apt install maven"
        exit 1
    fi
}

checkDocker() {
    if ! docker -v > /dev/null 2>&1; then
        echo "Docker is not installed. Please install Docker."
        echo "Suggested fix for ubuntu:"
        echo "sudo apt-get update"
        echo "sudo apt-get install -y apt-transport-https ca-certificates curl gnupg lsb-release"
        echo "curl -fsSL https://download.docker.com/linux/ubuntu/gpg | sudo gpg --dearmor -o /usr/share/keyrings/docker-archive-keyring.gpg"
        echo "echo \"deb [arch=\$(dpkg --print-architecture) signed-by=/usr/share/keyrings/docker-archive-keyring.gpg] https://download.docker.com/linux/ubuntu \$(lsb_release -cs) stable\" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null"
        echo "sudo apt-get update"
        echo "sudo apt-get install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin"
        echo "sudo usermod -aG docker \$USER"
		echo "newgrp docker"
        exit 1
    else
        echo "Docker is installed."
    fi
}

checkDockerCompose() {
    if ! docker-compose -v > /dev/null 2>&1; then
        echo "docker-compose is not installed. Please install docker-compose"
        echo "Suggested fix for ubuntu:"
        echo "sudo apt-get install docker-compose-plugin"
        exit 1
    else
        echo "docker-compose is installed."
    fi
}

# Function to wait for a Docker container to be running and log a specific string with a maximum timeout of 20 minutes
wait_for_container() {
    local container_name="$1"
    local log_string="$2"
    local timeout=1200  # Timeout set to 20 minutes (20 minutes * 60 seconds)

    local start_time=$(date +%s)
    local end_time=$((start_time + timeout))

    while ! docker inspect "$container_name" &>/dev/null; do
        echo "Waiting for container '$container_name' to be created..."
        sleep 5
        if [ "$(date +%s)" -ge "$end_time" ]; then
            echo "Timeout: Container creation exceeded 20 minutes."
            exit 1
        fi
    done

    while [ "$(docker inspect -f '{{.State.Status}}' "$container_name")" != "running" ]; do
        echo "Waiting for container '$container_name' to be running..."
        sleep 5
        if [ "$(date +%s)" -ge "$end_time" ]; then
            echo "Timeout: Container start exceeded 20 minutes."
            exit 1
        fi
    done

    # Check container logs for the specified string
    while ! docker logs "$container_name" 2>&1 | grep "$log_string"; do
        echo "Waiting for '$log_string' in container logs of '$container_name'..."
        sleep 5
        if [ "$(date +%s)" -ge "$end_time" ]; then
            echo "Timeout: Log string not found within 20 minutes."
            exit 1
        fi
    done
}


space() {
    echo ""
    echo "++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo ""
}
