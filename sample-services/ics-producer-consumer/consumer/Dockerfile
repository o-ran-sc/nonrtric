# Use the official OpenJDK image as base
FROM openjdk:17-jdk-alpine

# Set the working directory in the container
WORKDIR /app

ARG KAFKA_SERVERS
ENV KAFKA_SERVERS=${KAFKA_SERVERS}

# Copy the packaged JAR file into the container
COPY target/consumer-1.0.jar /app/Consumer.jar

# Expose port 8081
EXPOSE 8081

# Command to run the Spring Boot application
CMD ["java", "-jar", "Consumer.jar"]
