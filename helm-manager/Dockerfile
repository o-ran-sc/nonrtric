FROM openjdk:11-jre-slim

ARG JAR

#Install curl
RUN apt-get update
RUN apt-get install -y curl

#Install helm
RUN curl -fsSL -o get_helm.sh https://raw.githubusercontent.com/helm/helm/master/scripts/get-helm-3
RUN chmod 700 get_helm.sh
RUN ./get_helm.sh

#Install kubectl and configure
RUN curl -LO https://dl.k8s.io/release/v1.20.2/bin/linux/amd64/kubectl

RUN chmod +x ./kubectl

RUN mv ./kubectl /usr/local/bin/kubectl

# Copy app config and app jar
WORKDIR /opt/app/helm-manager/src/main/resources/config

COPY config/KubernetesParticipantConfig.json .
COPY config/application.yaml .

WORKDIR /opt/app/helm-manager
COPY target/app.jar app.jar

CMD [ "java", "-jar", "app.jar" ]
