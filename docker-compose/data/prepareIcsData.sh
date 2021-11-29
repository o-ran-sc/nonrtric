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
# one EiProducer in ICS
# one EiType in ICS
# one EiJob in ICS

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

# Create EiType
echo "Create EiType:"
curl -X PUT -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-types/type1 -H accept:application/json -H Content-Type:application/json --data-binary @${SHELL_FOLDER}/testdata/ICS/EiType.json
echo -e "\n"

# Get EiTypes
echo "Get EiTypes:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-types -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual EiType
echo "Get Individual EiType:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-types/type1 -H Content-Type:application/json | jq
echo -e "\n"

# Create EiProducer
echo "Create EiProducer:"
curl -X PUT -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers/1 -H Content-Type:application/json --data-binary @${SHELL_FOLDER}/testdata/ICS/EiProducer.json
echo -e "\n"

# Get EiProducers
echo "Get EiProducers:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual EiProducer
echo "Get Individual EiProducer:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers/1 -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual EiProducer Status
echo "Get Individual EiProducer:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/data-producer/v1/info-producers/1/status -H Content-Type:application/json | jq
echo -e "\n"

# Create EiJob
echo "Create EiJob Of A Certain Type type1:"
curl -X PUT -skw %{http_code} $httpx://localhost:$ics_port/A1-EI/v1/eijobs/job1 -H Content-Type:application/json --data-binary @${SHELL_FOLDER}/testdata/ICS/EiJob.json
echo -e "\n"

# Get EiJobs
echo "Get EiJobs:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/A1-EI/v1/eijobs -H Content-Type:application/json | jq
echo -e "\n"

# Get Individual EiJob:
echo "Get Individual EiJob:"
curl -X GET -skw %{http_code} $httpx://localhost:$ics_port/A1-EI/v1/eijobs/job1 -H Content-Type:application/json | jq
echo -e "\n"