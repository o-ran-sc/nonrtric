#==================================================================================
#   Copyright (C) 2024: OpenInfra Foundation Europe
#
#   Licensed under the Apache License, Version 2.0 (the "License");
#   you may not use this file except in compliance with the License.
#   You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#   Unless required by applicable law or agreed to in writing, software
#   distributed under the License is distributed on an "AS IS" BASIS,
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#   See the License for the specific language governing permissions and
#   limitations under the License.
#
#   This source code is part of the near-RT RIC (RAN Intelligent Controller)
#   platform project (RICP).
#==================================================================================

# Use the official OpenJDK image as base
FROM openjdk:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

ARG KAFKA_SERVERS
ENV KAFKA_SERVERS=${KAFKA_SERVERS}

# Copy the packaged JAR file into the container
COPY target/producer-0.0.1.jar /app/producer-0.0.1.jar

# Expose port 8080
EXPOSE 8080

# Command to run the Spring Boot application
CMD ["java", "-jar", "producer-0.0.1.jar"]
