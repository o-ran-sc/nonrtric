#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

FROM openjdk:11-jre-slim

ARG JAR

#Install apt-transport-https, curl and wget
#apt-transport-https is required to support packages installation over https
RUN apt-get update && apt-get install -y --no-install-recommends \
    apt-transport-https \
    wget \
    curl \
    && rm -rf /var/lib/apt/lists/*;

#Install helm
RUN wget -O helm.tar.gz https://get.helm.sh/helm-v3.6.1-linux-amd64.tar.gz
RUN tar -zxvf helm.tar.gz
RUN mv linux-amd64/helm /usr/local/bin/helm

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
