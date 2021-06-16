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
helm uninstall dmaap-mr message-generator oru-app sdnr-simulator -n nonrtric

# be careful if other stopped containers are on the same system
docker stop $(docker ps -aq)
docker system prune -f

# build o-ru application image
cd ${SHELL_FOLDER}/../app/
docker build -t oru-app .

# build simulator image of sdnr
cd ${SHELL_FOLDER}/../simulators/
docker build -f Dockerfile-sdnr-sim -t sdnr-simulator .

# build message generator image
docker build -f Dockerfile-message-generator -t message-generator .

# build dmaap-mr sim
cd ${SHELL_FOLDER}/../../../../mrstub/
docker build -t mrstub .

# build dmapp-mr helm chart & install chart
cd ${SHELL_FOLDER}/dmaap-mr/
helm package .
helm install dmaap-mr dmaap-mr-0.1.0.tgz --namespace nonrtric --create-namespace --wait

# build dmapp-mr helm chart & install chart
cd ${SHELL_FOLDER}/message-generator/
helm package .
helm install message-generator message-generator-0.1.0.tgz --namespace nonrtric --create-namespace --wait

# build dmapp-mr helm chart & install chart
cd ${SHELL_FOLDER}/oru-app/
helm package .
helm install oru-app oru-app-0.1.0.tgz --namespace nonrtric --create-namespace --wait

# build dmapp-mr helm chart & install chart
cd ${SHELL_FOLDER}/sdnr-simulator/
helm package .
helm install sdnr-simulator sdnr-simulator-0.1.0.tgz --namespace nonrtric --create-namespace --wait