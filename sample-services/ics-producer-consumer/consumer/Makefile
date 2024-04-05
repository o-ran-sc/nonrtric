# Define variables
IMAGE_NAME = kafka-consumer
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
