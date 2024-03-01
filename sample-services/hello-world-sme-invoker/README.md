# Hello World Sme Invoker Service

This repository contains a Spring Boot application serving few Hello World SME endpoints. 
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
Hello World SME endpoint and display the response:

```bash
  response=$(curl -s http://localhost:8080/helloworld/v1/sme)
  echo "Response from the Hello World SME endpoint:"
  echo "$response"
```

To stop and remove the Docker container:

```bash
  docker stop hello-world-sme-invoker
  docker rm hello-world-sme-invoker
```

## Additional Information

- The Hello World SME endpoint is available at http://localhost:8080/helloworld/v1/sme.

