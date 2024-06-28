# Hello World Sme Invoker Service

This repository contains a Spring Boot application serving as Hello World SME invoker application.
The application can be built and run using the provided script - ``hello-world-sme-invoker-build-start.sh``.

## Prerequisites

- Docker installed on your machine.

## Building and Running the Application
Run the script:

```bash
  ./hello-world-sme-invoker-build-start.sh
```

The script will build a Docker image and run a container with the Hello World SME service. After the container starts,
wait for a few seconds to ensure the Spring Boot application is fully initialized. Next, it will make an HTTP request to the
Hello World SME endpoint on the interval of 5 seconds and displays the response:

To stop and remove the Docker container:

```bash
  docker stop hello-world-sme-invoker
  docker rm hello-world-sme-invoker
```

