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

# Automated test script for mrstub container

# mr-stub port
export PORT=3905

# source function to do curl and check result
. ../common/do_curl_function.sh

echo "=== Stub hello world ==="
RESULT="OK"
do_curl GET / 200

echo "=== Stub reset ==="
RESULT="OK"
do_curl GET /reset 200

## Test with json response

echo "=== Send a request ==="
RESULT="*"
#create payload
echo "{\"data\": \"data-value\"}" > .tmp.json

do_curl POST '/send-request?operation=PUT&url=/test' 200 .tmp.json
#Save id for later
CORRID=$body

echo "=== Fetch a response, shall be empty ==="
RESULT=""
do_curl GET '/receive-response?correlationid='$CORRID 204

echo "=== Fetch a request ==="
RESULT="json:[{\"apiVersion\":\"1.0\",\"operation\":\"PUT\",\"correlationId\":\""$CORRID"\",\"originatorId\": \"849e6c6b420\",\"payload\":{\"data\": \"data-value\"},\"requestId\":\"23343221\", \"target\":\"policy-agent\", \"timestamp\":\"????\", \"type\":\"request\",\"url\":\"/test\"}]"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent' 200

echo "=== Send a json response ==="
# Create minimal accepted response message
echo "[{\"correlationId\": \""$CORRID"\", \"message\": {\"test\":\"testresponse\"}, \"status\": \"200\"}]" > .tmp.json
RESULT="OK"
do_curl POST /events/A1-POLICY-AGENT-WRITE 200 .tmp.json

echo "=== Fetch a response ==="
RESULT="{\"test\": \"testresponse\"}200"
do_curl GET '/receive-response?correlationid='$CORRID 200

### Test with plain text response

echo "=== Send a request ==="
RESULT="*"
do_curl POST '/send-request?operation=GET&url=/test2' 200
#Save id for later
CORRID=$body

echo "=== Fetch a response, shall be empty ==="
RESULT=""
do_curl GET '/receive-response?correlationid='$CORRID 204

echo "=== Fetch a request ==="
RESULT="json:[{\"apiVersion\":\"1.0\",\"operation\":\"GET\",\"correlationId\":\""$CORRID"\",\"originatorId\": \"849e6c6b420\",\"payload\":{},\"requestId\":\"23343221\", \"target\":\"policy-agent\", \"timestamp\":\"????\", \"type\":\"request\",\"url\":\"/test2\"}]"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent' 200

echo "=== Send a json response ==="
# Create minimal accepted response message
echo "[{\"correlationId\": \""$CORRID"\", \"message\": \"test2-response\", \"status\": \"200\"}]" > .tmp.json
RESULT="OK"
do_curl POST /events/A1-POLICY-AGENT-WRITE 200 .tmp.json

echo "=== Fetch a response ==="
RESULT="test2-response200"
do_curl GET '/receive-response?correlationid='$CORRID 200

echo "********************"
echo "*** All tests ok ***"
echo "********************"
