
FROM openjdk:8-jre-alpine

ARG JAR_FILE

WORKDIR /opt/app/nearricsimulator

COPY ${JAR_FILE} nearric-simulator.jar

ENTRYPOINT ["/usr/bin/java", "-jar", "/opt/app/nearricsimulator/nearric-simulator.jar"]