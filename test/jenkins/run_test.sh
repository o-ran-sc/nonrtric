#!/bin/bash

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

# This script is to set up test env in jenkins vm

echo "--> run_integration.sh"

# Install docker-compose
DOCKER_C_VERSION=$(curl --silent https://api.github.com/repos/docker/compose/releases/latest | jq .name -r)
curl -L "https://github.com/docker/compose/releases/download/${DOCKER_C_VERSION}/docker-compose-$(uname -s)-$(uname -m)" -o ./docker-compose
chmod +x docker-compose
export PATH=$PATH:`pwd`

clean_docker(){
    docker stop $(docker ps -aq)
    docker system prune -f
}
# Run auto-test scripts
cd ../auto-test/
clean_docker
bash FTC10.sh remote auto-clean --env-file ../common/test_env-oran-master.sh --use-local-image A1PMS SDNC

echo "--> run_integration.sh END"

FILE=.resultFTC10.txt
if [[ -f "$FILE" ]]; then
    res=$(cat .resultFTC10.txt)
    clean_docker
    exit $res
fi
clean_docker
exit 1
