#
# ============LICENSE_START=======================================================
# O-RAN-SC
# ================================================================================
# Copyright (C) 2021 Nordix Foundation. All rights reserved.
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


FROM openjdk:11-jre-slim

EXPOSE 8084 8435

ARG JAR

WORKDIR /opt/app/dmaap-adaptor-service
RUN mkdir -p /var/log/dmaap-adaptor-service
RUN mkdir -p /opt/app/dmaap-adaptor-service/etc/cert/
RUN mkdir -p /var/dmaap-adaptor-service

ADD /config/application.yaml /opt/app/dmaap-adaptor-service/config/application.yaml
ADD /config/application_configuration.json /opt/app/dmaap-adaptor-service/data/application_configuration.json_example
ADD /config/keystore.jks /opt/app/dmaap-adaptor-service/etc/cert/keystore.jks
ADD /config/truststore.jks /opt/app/dmaap-adaptor-service/etc/cert/truststore.jks

ARG user=nonrtric
ARG group=nonrtric

RUN groupadd $user && \
    useradd -r -g $group $user
RUN chown -R $user:$group /opt/app/dmaap-adaptor-service
RUN chown -R $user:$group /var/log/dmaap-adaptor-service
RUN chown -R $user:$group /var/dmaap-adaptor-service

USER ${user}

ADD target/${JAR} /opt/app/dmaap-adaptor-service/dmaap-adaptor.jar
CMD ["java", "-jar", "/opt/app/dmaap-adaptor-service/dmaap-adaptor.jar"]
