#
# ============LICENSE_START=======================================================
#  Copyright (C) 2020 Nordix Foundation.
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

WORKDIR /opt/app/enrichment-coordinator-service
RUN mkdir -p /var/log/enrichment-coordinator-service
RUN mkdir -p /opt/app/enrichment-coordinator-service/etc/cert/
RUN mkdir -p /var/enrichment-coordinator-service
RUN chmod -R 777 /var/enrichment-coordinator-service

EXPOSE 8083 8434

ADD /config/application.yaml /opt/app/enrichment-coordinator-service/config/application.yaml
ADD target/${JAR} /opt/app/enrichment-coordinator-service/enrichment-coordinator-service.jar
ADD /config/keystore.jks /opt/app/enrichment-coordinator-service/etc/cert/keystore.jks
ADD /config/truststore.jks /opt/app/enrichment-coordinator-service/etc/cert/truststore.jks


RUN chmod -R 777 /opt/app/enrichment-coordinator-service/config/

CMD ["java", "-jar", "/opt/app/enrichment-coordinator-service/enrichment-coordinator-service.jar"]




