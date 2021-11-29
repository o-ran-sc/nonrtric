#!/bin/bash

#  ============LICENSE_START===============================================
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

# The scripts in data/ will generate some dummy data in the running system.
# It will send a dmaap msg of job to mediator:

# Run command:
# ./sendMsgToMediator.sh [dmaap-mr port] [http/https]

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)
bash ${SHELL_FOLDER}/prepareIcsData.sh

dmaa_mr_port=${1:-3904}
httpx=${2:-"http"}

echo "using dmaap-mr port: "$dmaa_mr_port
echo "using protocol: "$httpx
echo -e "\n"

echo "dmaap-mr topics:"
curl -skw %{http_code} $httpx://localhost:$dmaa_mr_port/topics/listAll
echo -e "\n"

echo "dmaap-mr create topic unauthenticated.VES_NOTIFICATION_OUTPUT:"
curl -skw %{http_code} -X POST "$httpx://localhost:$dmaa_mr_port/topics/create" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{  \"topicName\": \"unauthenticated.VES_NOTIFICATION_OUTPUT\",  \"topicDescription\": \"test topic\",  \"partitionCount\": 1,  \"replicationCount\": 1,  \"transactionEnabled\": \"false\"}"
echo -e "\n"

echo "dmaap-mr topics:"
curl -skw %{http_code} $httpx://localhost:$dmaa_mr_port/topics/listAll
echo -e "\n"

echo "send job msg to dmaap-mr:"
curl -k -X POST -sw %{http_code} -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$dmaa_mr_port/events/unauthenticated.VES_NOTIFICATION_OUTPUT/" --data-binary @${SHELL_FOLDER}/testdata/dmaap-mediator-java/job.json
echo -e "\n"