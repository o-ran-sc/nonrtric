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
# one policy type in a1-sim-OSC
# one service in policy agent
# one policy in a1-sim-OSC
# one policy in a1-sim-STD

# Run command:
# ./dmaap-msg.sh [dmaap-mr port] [a1-sim-OSC port] [a1-sim-STD port] [http/https]

dmaa_mr_port=${1:-3904}
a1_sim_OSC_port=${2:-30001}
a1_sim_STD_port=${3:-30003}
httpx=${4:-"http"}

echo "using dmaap-mr port: "$dmaa_mr_port
echo "using a1-sim-OSC port: "$a1_sim_OSC_port
echo "using a1-sim-STD port: "$a1_sim_STD_port
echo "using protocol: "$httpx
echo -e "\n"

echo "dmaap-mr topics:"
curl -skw " %{http_code}" $httpx://localhost:$dmaa_mr_port/topics/listAll
echo -e "\n"

echo "dmaap-mr create topic A1-POLICY-AGENT-READ:"
curl -skw " %{http_code}" -X POST "$httpx://localhost:$dmaa_mr_port/topics/create" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{  \"topicName\": \"A1-POLICY-AGENT-READ\",  \"topicDescription\": \"test topic\",  \"partitionCount\": 1,  \"replicationCount\": 1,  \"transactionEnabled\": \"false\"}"
echo -e "\n"

echo "dmaap-mr create topic A1-POLICY-AGENT-WRITE:"
curl -skw " %{http_code}" -X POST "$httpx://localhost:$dmaa_mr_port/topics/create" -H  "accept: application/json" -H  "Content-Type: application/json" -d "{  \"topicName\": \"A1-POLICY-AGENT-WRITE\",  \"topicDescription\": \"test topic\",  \"partitionCount\": 1,  \"replicationCount\": 1,  \"transactionEnabled\": \"false\"}"
echo -e "\n"

echo "dmaap-mr topics:"
curl -skw " %{http_code}" $httpx://localhost:$dmaa_mr_port/topics/listAll
echo -e "\n"

echo "ric1 version:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_OSC_port/counter/interface
echo -e "\n"

echo "ric2 version:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_STD_port/counter/interface
echo -e "\n"

echo "create policy type 1 to ric1:"
curl -X PUT -skw " %{http_code}" $httpx://localhost:$a1_sim_OSC_port/policytype?id=1 -H Content-Type:application/json --data-binary @testdata/OSC/policy_type.json
echo -e "\n"

for i in {1..12}; do
	echo "policy types from policy agent:"
    curlString="curl -skw %{http_code} $httpx://localhost:8081/policy_types"
    res=$($curlString)
    echo "$res"
    expect="[\"\",\"1\"]200"
    if [ "$res" == "$expect" ]; then
        echo -e "\n"
        break;
    else
        sleep $i
    fi
done

echo "create service 1 to policy agent:"
curl -k -X POST -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$dmaa_mr_port/events/A1-POLICY-AGENT-READ/" --data-binary @testdata/dmaap-msg-service.json
echo -e "\n"

echo "create policies to ric1 & ric2 with type1 and service1 via dmaa_mr:"
curl -k -X POST -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$dmaa_mr_port/events/A1-POLICY-AGENT-READ/" --data-binary @testdata/dmaap-msg-policy.json
echo -e "\n"

echo "policy numbers from ric1:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_OSC_port/counter/num_instances
echo -e "\n"

# echo "create policy 2100 to ric2 with service1 via dmaa_mr, no type:"
# curl -k -X PUT -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$dmaa_mr_port/events/A1-POLICY-AGENT-READ/" --data-binary @testdata/policy.json
# echo -e "\n"

echo "policy numbers from ric2:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_STD_port/counter/num_instances
echo -e "\n"

echo "get policy from policy agent:"
curl -k -X POST -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$dmaa_mr_port/events/A1-POLICY-AGENT-READ/" --data-binary @testdata/dmaap-msg-status.json
echo -e "\n"

# echo "policy id 2100 from policy agent:"
# curl -k -X GET -sw " %{http_code}" $httpx://localhost:$dmaa_mr_port/events/A1-POLICY-AGENT-READ/
# echo -e "\n"