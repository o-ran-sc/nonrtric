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

FROM curlimages/curl:7.78.0 AS build

#Get helm
RUN curl -Lo /tmp/helm.tar.gz  https://get.helm.sh/helm-v3.6.1-linux-amd64.tar.gz

#Get kubectl
RUN curl -Lo /tmp/kubectl  https://dl.k8s.io/release/v1.20.2/bin/linux/amd64/kubectl


FROM openjdk:11-jre-slim

#Install helm
COPY --from=build /tmp/helm.tar.gz .

RUN tar -zxvf helm.tar.gz

RUN mv linux-amd64/helm /usr/local/bin/helm

#Install kubectl
COPY --from=build /tmp/kubectl .

RUN chmod +x ./kubectl

RUN mv ./kubectl /usr/local/bin/kubectl

WORKDIR /etc/app/helm-manager
COPY config/application.yaml .

WORKDIR /opt/app/helm-manager
COPY target/app.jar app.jar

ARG user=nonrtric
ARG group=nonrtric

RUN groupadd $group && \
    useradd -r -g $group $user
RUN chown -R $user:$group /opt/app/helm-manager
RUN chown -R $user:$group /etc/app/helm-manager

RUN mkdir /var/helm-manager-service
RUN chown -R $user:$group /var/helm-manager-service

RUN mkdir /home/$user
RUN chown -R $user:$group /home/$user

USER $user

CMD [ "java", "-jar", "app.jar", "--spring.config.location=optional:file:/etc/app/helm-manager/"]
