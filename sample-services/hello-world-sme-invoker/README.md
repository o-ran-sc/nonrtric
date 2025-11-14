# Hello World Sme Invoker Service  (Experimental O-RAN-SC Module)

![Status: Not for Production](https://img.shields.io/badge/status-not--for--production-red)
![Status: Experimental](https://img.shields.io/badge/CVE%20Support-none-lightgrey)

> [!WARNING]
> This repository is pre-spec and not intended for production use. No CVE remediation or production guarantees apply.

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

