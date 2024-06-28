# Hello World Service

This repository contains a Spring Boot application serving a Hello World endpoint. The application can be built and 
run using the provided script - ``hello-world-build-start.sh``.

## Prerequisites

- Docker installed on your machine.

## Building and Running the Application
Run the script:

```bash
  ./hello-world-build-start.sh
```

The script will build a Docker image and run a container with the Hello World service. After the container starts, 
wait for a few seconds to ensure the Spring Boot application is fully initialized. Next, it will make an HTTP request to the 
Hello World endpoint and display the response:

```bash
  response=$(curl -s http://localhost:8080/helloworld/v1)
  echo "Response from the Hello World endpoint:"
  echo "$response"
```

To stop and remove the Docker container:

```bash
  docker stop hello-world
  docker rm hello-world
```

## Additional Information

- The Hello World endpoint is available at http://localhost:8080/helloworld/v1.

