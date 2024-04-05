# Use the official OpenJDK image as base
FROM openjdk:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

ARG KAFKA_SERVERS
ENV KAFKA_SERVERS=${KAFKA_SERVERS}

# Copy the packaged JAR file into the container
COPY target/producer-1.0.jar /app/Producer.jar

# Expose port 8080
EXPOSE 8080

# Command to run the Spring Boot application
CMD ["java", "-jar", "Producer.jar"]
