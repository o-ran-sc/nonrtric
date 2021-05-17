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

# start up dmaap message router
cd ${SHELL_FOLDER}/../../../../docker-compose/
docker-compose -f docker-compose.yaml -f mr/docker-compose.yml up -d

# wait until mr up & running
for i in {1..60}; do
    res=$(curl -o /dev/null -sw %{http_code} http://localhost:3904/topics)
    echo "$res"
    expect="200"
    if [ "$res" == "$expect" ]; then
        echo -e "dmaap-mr is alive!\n"
        break;
    else
        sleep $i
    fi
done

# start up oru, message-generator and sdnr-simulator
cd ${SHELL_FOLDER}
docker-compose up -d