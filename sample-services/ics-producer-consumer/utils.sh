#!/bin/bash

checkJava() {
    if ! command -v java >/dev/null 2>&1; then
        echo "Java is not installed. Please install Java."
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
        echo "sudo apt install maven"
        exit 1
    fi
}

checkDocker() {
    if ! docker -v > /dev/null 2>&1; then
        echo "Docker is not installed. Please install Docker."
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
        echo "sudo apt-get install docker-compose-plugin"
        exit 1
    else
        echo "docker-compose is installed."
    fi
}

# Function to wait for a Docker container to be running and log a specific string
wait_for_container() {
    local container_name="$1"
    local log_string="$2"

    while ! docker inspect "$container_name" &>/dev/null; do
        echo "Waiting for container '$container_name' to be created..."
        sleep 5
    done

    while [ "$(docker inspect -f '{{.State.Status}}' "$container_name")" != "running" ]; do
        echo "Waiting for container '$container_name' to be running..."
        sleep 5
    done

    # Check container logs for the specified string
    while ! docker logs "$container_name" 2>&1 | grep "$log_string"; do
        echo "Waiting for '$log_string' in container logs of '$container_name'..."
        sleep 5
    done
}

space() {
    echo ""
    echo "++++++++++++++++++++++++++++++++++++++++++++++++++++"
    echo ""
}
