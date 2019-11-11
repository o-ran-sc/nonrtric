
FROM openjdk:11-jre-slim

ARG JAR_FILE

WORKDIR /opt/app/nearricsimulator

COPY ${JAR_FILE} nearric-simulator.jar

ENTRYPOINT ["java", "-jar", "/opt/app/nearricsimulator/nearric-simulator.jar"]