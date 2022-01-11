#
# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
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

EXPOSE 8081 8433


WORKDIR /opt/app/policy-agent
RUN mkdir -p /var/log/policy-agent
RUN mkdir -p /opt/app/policy-agent/etc/cert/
EXPOSE 8081 8433

ADD /config/application.yaml /opt/app/policy-agent/config/application.yaml
ADD /config/application_configuration.json /opt/app/policy-agent/data/application_configuration.json_example
ADD /config/keystore.jks /opt/app/policy-agent/etc/cert/keystore.jks
ADD /config/truststore.jks /opt/app/policy-agent/etc/cert/truststore.jks

ARG user=nonrtric
ARG group=nonrtric

RUN groupadd $user && \
    useradd -r -g $group $user
RUN chown -R $user:$group /opt/app/policy-agent
RUN chown -R $user:$group /var/log/policy-agent

USER ${user}

ADD target/${JAR} /opt/app/policy-agent/policy-agent.jar
CMD ["java", "-jar", "/opt/app/policy-agent/policy-agent.jar"]