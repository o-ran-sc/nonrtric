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

WORKDIR /opt/app/r-app-catalogue
RUN mkdir -p /var/log/r-app-catalogue
RUN mkdir -p /opt/app/r-app-catalogue/etc/cert/

EXPOSE 8680 8633

ADD /config/application.yaml /opt/app/r-app-catalogue/config/application.yaml
ADD /config/r-app-catalogue-keystore.jks /opt/app/r-app-catalogue/etc/cert/keystore.jks
ADD target/${JAR} /opt/app/r-app-catalogue/r-app-catalogue.jar

ARG user=nonrtric
ARG group=nonrtric

RUN groupadd $user && \
    useradd -r -g $group $user
RUN chown -R $user:$group /opt/app/r-app-catalogue
RUN chown -R $user:$group /var/log/r-app-catalogue

USER ${user}

CMD ["java", "-jar", "/opt/app/r-app-catalogue/r-app-catalogue.jar"]




