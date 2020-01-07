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

WORKDIR /opt/app/policy-agent
RUN mkdir -p /var/log/policy-agent
RUN mkdir -p /opt/app/policy-agent/etc/cert/

EXPOSE 8081

ADD /config/application.yaml /opt/app/policy-agent/config/
ADD target/policy-agent-1.0.0-SNAPSHOT.jar /opt/app/policy-agent/


RUN chmod -R 777 /opt/app/policy-agent/config/

CMD ["java", "-jar", "/opt/app/policy-agent/policy-agent-1.0.0-SNAPSHOT.jar"]




