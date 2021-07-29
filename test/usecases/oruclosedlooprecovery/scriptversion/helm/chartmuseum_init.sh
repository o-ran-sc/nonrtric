#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)
cd ${SHELL_FOLDER}

# build dmaap-mr helm chart & push to chartmuseum
cd ${SHELL_FOLDER}/dmaap-mr/
helm package .
curl --data-binary "@dmaap-mr-0.1.0.tgz" http://chartmuseum:8080/api/charts

# build message-generator helm chart & push to chartmuseum
cd ${SHELL_FOLDER}/message-generator/
helm package .
curl --data-binary "@message-generator-0.1.0.tgz" http://chartmuseum:8080/api/charts

# build oru-app helm chart & push to chartmuseum
cd ${SHELL_FOLDER}/oru-app/
helm package .
curl --data-binary "@oru-app-0.1.0.tgz" http://chartmuseum:8080/api/charts

# build sdnr-simulator helm chart & push to chartmuseum
cd ${SHELL_FOLDER}/sdnr-simulator/
helm package .
curl --data-binary "@sdnr-simulator-0.1.0.tgz" http://chartmuseum:8080/api/charts

# add chartmuseum repo to helm
helm repo add chartmuseum http://chartmuseum:8080