# Hello World Service Stub

This repository contains a Spring Boot application serving a Hello World endpoint. The application can be built and 
run using the provided script - ``service-stub-build-start.sh``.

## Prerequisites

- Docker installed on your machine.

## Building and Running the Application
Run the script:

```bash
  ./service-stub-build-start.sh
```

The script will build a Docker image and run a container with the Hello World service. After the container starts, 
wait for a few seconds to ensure the Spring Boot application is fully initialized. Next, it will make an HTTP request to the 
Hello World endpoint and display the response:

```bash
  response=$(curl -s http://localhost:8080/helloworld/v1/sme)
  echo "Response from the Hello World SME endpoint:"
  echo "$response"
```

To stop and remove the Docker container:

```bash
  docker stop service-stub-hello-world-test
  docker rm service-stub-hello-world-test
```

## Additional Information

- The Hello World SME endpoint is available at http://localhost:8080/helloworld/v1/sme.

