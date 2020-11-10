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
# ./fill_data.sh [policy-agent port] [a1-sim-OSC port] [a1-sim-STD port] [http/https]

policy_agent_port=${1:-8081}
a1_sim_OSC_port=${2:-30001}
a1_sim_STD_port=${3:-30005}
httpx=${4:-"http"}

echo "using policy_agent port: "$policy_agent_port
echo "using a1-sim-OSC port: "$a1_sim_OSC_port
echo "using a1-sim-STD port: "$a1_sim_STD_port
echo "using protocol: "$httpx
echo -e "\n"

echo "policy agent status:"
curl -skw " %{http_code}" $httpx://localhost:$policy_agent_port/status
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

echo "create policy type 2 to ric2:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_STD_port/policytype?id=2 -X PUT -H Accept:application/json -H Content-Type:application/json -H X-Requested-With:XMLHttpRequest --data-binary @testdata/v2/policy_type.json
echo -e "\n"

for i in {1..12}; do
	echo "policy types from policy agent:"
    curlString="curl -skw %{http_code} $httpx://localhost:$policy_agent_port/v2/policy-types"
    res=$($curlString)
    echo "$res"
    expect="{\"policytype_ids\":[\"1\",\"2\"]}200"
    if [ "$res" == "$expect" ]; then
        echo -e "\n"
        break;
    else
        sleep $i
    fi
done

echo "create service ric-registration to policy agent:"
curl -k -X PUT -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$policy_agent_port/v2/services" --data-binary @testdata/v2/service.json
echo -e "\n"

echo "create policy aa8feaa88d944d919ef0e83f2172a5000 to ric1 with type 1 and service ric-registration via policy agent:"
curl -k -X PUT -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$policy_agent_port/v2/policies" --data-binary @testdata/v2/policy_osc.json
echo -e "\n"

echo "policy numbers from ric1:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_OSC_port/counter/num_instances
echo -e "\n"

echo "create policy aa8feaa88d944d919ef0e83f2172a5100 to ric2 with service1 via policy agent, no type:"
curl -k -X PUT -sw " %{http_code}" -H accept:application/json -H Content-Type:application/json "$httpx://localhost:$policy_agent_port/v2/policies" --data-binary @testdata/v2/policy_std_v2.json
echo -e "\n"

echo "policy numbers from ric2:"
curl -skw " %{http_code}" $httpx://localhost:$a1_sim_STD_port/counter/num_instances
echo -e "\n"

echo "policy id aa8feaa88d944d919ef0e83f2172a5000 from policy agent:"
curl -k -X GET -sw " %{http_code}" $httpx://localhost:$policy_agent_port/v2/policies/aa8feaa88d944d919ef0e83f2172a5000
echo -e "\n"

echo "policy id aa8feaa88d944d919ef0e83f2172a5100 from policy agent:"
curl -k -X GET -sw " %{http_code}" $httpx://localhost:$policy_agent_port/v2/policies/aa8feaa88d944d919ef0e83f2172a5100
echo -e "\n"