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

# The scripts in data/ will generate some dummy data in the running system.
# It will create:
# one InfoProducer in ICS
# one InfoType in ICS
# one InfoJob in ICS

# Run command:
# ./prepareIcsData.sh [ICS port] [http/https]

ics_port=${1:-8083}
httpx=${4:-"http"}
SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)

echo "using ics port: "$ics_port
echo "using protocol: "$httpx
echo -e "\n"

echo "ICS status:"
curl -skw " %{http_code}" $httpx://localhost:$ics_port/status
echo -e "\n"

# Create InfoType
echo "Create InfoType:"
curl -X PUT -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-types/type1 -H accept:application/json -H Content-Type:application/json --data-binary @${SHELL_FOLDER}/testdata/ICS/InfoType.json
echo -e "\n"

# Get InfoTypes
echo "Get InfoTypes:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-types -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual InfoType
echo "Get Individual InfoType:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-types/type1 -H Content-Type:application/json | jq
echo -e "\n"

# Create InfoProducer
echo "Create InfoProducer:"
curl -X PUT -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers/1 -H Content-Type:application/json --data-binary @${SHELL_FOLDER}/testdata/ICS/InfoProducer.json
echo -e "\n"

# Get InfoProducers
echo "Get InfoProducers:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual InfoProducer
echo "Get Individual InfoProducer:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers/1 -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual InfoProducer Status
echo "Get Individual InfoProducer:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers/1/status -H Content-Type:application/json | jq
echo -e "\n"

# Create InfoJob
echo "Create InfoJob Of A Certain Type type1:"
curl -X PUT -skw %{http_code} $httpx://localhost:$ics_port/A1-EI/v1/eijobs/job1 -H Content-Type:application/json --data-binary @${SHELL_FOLDER}/testdata/ICS/InfoJob.json
echo -e "\n"

# Get InfoJobs
echo "Get InfoJobs:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/A1-EI/v1/eijobs -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual InfoJob:
echo "Get Individual InfoJob:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/A1-EI/v1/eijobs/job1 -H Content-Type:application/json | jq
echo -e "\n"