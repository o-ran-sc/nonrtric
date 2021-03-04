#
# ============LICENSE_START=======================================================
#  Copyright (C) 2021 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#
FROM openjdk:11-jre-slim

ARG JAR

WORKDIR /opt/app/rapp-manager
RUN mkdir -p /var/log/rapp-manager

RUN apt-get update && apt install -y wget
RUN wget https://get.helm.sh/helm-v3.5.2-linux-amd64.tar.gz && tar xvf helm-v3.5.2-linux-amd64.tar.gz && mv linux-amd64/helm /usr/local/bin
RUN rm -rf linux-amd64 && rm helm-v3.5.2-linux-amd64.tar.gz

EXPOSE 8080
ADD /config/application.yaml /opt/app/rapp-manager/config/application.yaml
ADD target/${JAR} /opt/app/rapp-manager/rapp-manager.jar

CMD ["java", "-jar", "/opt/app/rapp-manager/rapp-manager.jar"]