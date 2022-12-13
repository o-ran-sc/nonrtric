#!/bin/bash
#
# ============LICENSE_START=======================================================
#  Copyright (C) 2022 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#
INGRESS_HOST=$(minikube ip)
INGRESS_PORT=$(kubectl -n istio-system get service istio-ingressgateway -o jsonpath='{.spec.ports[?(@.name=="http2")].nodePort}')
TESTS=0
PASSED=0
FAILED=0
TEST_TS=$(date +%F-%T)
TOKEN=""
ACCESS_TOKEN=""
REFRESH_TOKEN=""

function get_token
{
    local prefix="${1}"
    url="http://192.168.49.2:31560/auth/realms"
         TOKEN=$(curl -s -X POST $url/provider/protocol/openid-connect/token -H \
		 "Content-Type: application/x-www-form-urlencoded" -d client_secret=OwTCeahULA21G5TfEVMLG1iMloGiyH3i \
		 -d 'grant_type=client_credentials' -d client_id=provider-cli)
	echo "TOKEN: $TOKEN"
        ACCESS_TOKEN=$(echo $TOKEN | jq -r '.access_token')
        REFRESH_TOKEN=$(echo $TOKEN | jq -r '.refresh_token')
    echo $ACCESS_TOKEN
}

function run_test
{
    local prefix="${1}" type=${2} msg="${3}" data=${4}
    TESTS=$((TESTS+1))
    echo "Test ${TESTS}: Testing $type /${prefix}"
    get_token $prefix
    url=$INGRESS_HOST:$INGRESS_PORT"/"$prefix
    #echo $url
    result=$(curl -s -X ${type} -H "Content-type: application/json" -H "Authorization: Bearer $ACCESS_TOKEN" $url)
    echo $result
    if [ "$result" != "$msg" ]; then
            echo "FAIL"
            FAILED=$((FAILED+1))
    else
            echo "PASS"
            PASSED=$((PASSED+1))
    fi
    echo ""
}


run_test "rapp-provider" "GET" "Hello World!" ""

echo
echo "-----------------------------------------------------------------------"
echo "Number of Tests: $TESTS, Tests Passed: $PASSED, Tests Failed: $FAILED"
echo "Date: $TEST_TS"
echo "-----------------------------------------------------------------------"
