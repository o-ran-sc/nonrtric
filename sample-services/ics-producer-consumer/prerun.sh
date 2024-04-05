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
        echo "sudo apt-get install -y docker-ce docker-ce-cli containerd.io"
        echo "sudo usermod -aG docker \$USER"
		echo "newgrp docker"
        exit 1
    else
        echo "Docker is installed."
    fi
}

checkJava
checkMaven
checkDocker
