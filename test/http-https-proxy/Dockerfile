#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
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

FROM ${NEXUS_PROXY_REPO}alpine:3.17.3

RUN apk add --update --no-cache nodejs

WORKDIR /app

COPY cert/*.crt cert/
COPY cert/pass cert/

COPY http_proxy.js .

CMD [ "node", "http_proxy.js" ]
