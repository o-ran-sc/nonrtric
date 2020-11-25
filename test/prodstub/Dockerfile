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

ARG NEXUS_PROXY_REPO

FROM ${NEXUS_PROXY_REPO}python:3.8-slim-buster

COPY app/ /usr/src/app/
COPY cert/ /usr/src/app/cert/

WORKDIR /usr/src/app

RUN pip install -r requirements.txt

RUN chmod +x start.sh

#install nginx
RUN apt-get update
RUN apt-get install -y nginx=1.14.*

CMD [ "./start.sh" ]
