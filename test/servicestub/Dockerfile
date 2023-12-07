#==================================================================================
#   Copyright (C) 2023: OpenInfra Foundation Europe
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


# Use Maven image with OpenJDK 17 for the build stage
FROM maven:3.8.5-openjdk-17 AS maven_build

# Copy Maven project files
COPY pom.xml /tmp/
COPY src /tmp/src/

# Set working directory
WORKDIR /tmp/

# Build the Maven project
RUN mvn package

# Use a separate image with OpenJDK 17 for the runtime stage
FROM openjdk:17-jdk-slim

# Expose port 8080
EXPOSE 8080

# Set the working directory
WORKDIR /app

# Copy the JAR file from the maven_build stage to the runtime stage
COPY --from=maven_build /tmp/target/hello-world-0.1.0.jar /app/hello-world-0.1.0.jar

# Command to run the application
CMD ["java", "-jar", "hello-world-0.1.0.jar"]
