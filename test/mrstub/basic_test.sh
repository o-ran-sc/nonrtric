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

# Run the build_and_start with the same arg as this script
if [ $# -ne 1 ]; then
    echo "Usage: ./basic_test nonsecure|secure"
    exit 1
fi
if [ "$1" != "nonsecure" ] && [ "$1" != "secure" ]; then
    echo "Usage: ./basic_test nonsecure|secure"
    exit 1
fi

if [ $1 == "nonsecure" ]; then
    #Default http port for the simulator
    PORT=3904
    # Set http protocol
    HTTPX="http"
else
    #Default https port for the mr-stub
    PORT=3905
    # Set https protocol
    HTTPX="https"
fi

# source function to do curl and check result
. ../common/do_curl_function.sh

RESP_CONTENT='*' #Dont check resp content type

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
# Create minimal accepted response message, array
echo "[{\"correlationId\": \""$CORRID"\", \"message\": {\"test\":\"testresponse\"}, \"status\": \"200\"}]" > .tmp.json
RESULT="{}"
do_curl POST /events/A1-POLICY-AGENT-WRITE 200 .tmp.json

echo "=== Fetch a response ==="
RESULT="{\"test\": \"testresponse\"}200"
do_curl GET '/receive-response?correlationid='$CORRID 200

echo "=== Send a json response ==="
# Create minimal accepted response message, single message - no array
echo "{\"correlationId\": \""$CORRID"\", \"message\": {\"test\":\"testresponse2\"}, \"status\": \"200\"}" > .tmp.json
RESULT="{}"
do_curl POST /events/A1-POLICY-AGENT-WRITE 200 .tmp.json

echo "=== Fetch a response ==="
RESULT="{\"test\": \"testresponse2\"}200"
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

echo "=== Fetch a request, empty. Shall delay 10 seconds ==="
T1=$SECONDS
RESULT="json:[]"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent' 200
T2=$SECONDS
if [ $(($T2-$T1)) -lt 10 ] || [ $(($T2-$T1)) -gt 15 ]; then
    echo "Delay to short or too long"$(($T2-$T1))". Should be default 10 sec"
    exit 1
else
    echo "  Delay ok:"$(($T2-$T1))
fi

echo "=== Fetch a request, empty. Shall delay 5 seconds ==="
T1=$SECONDS
RESULT="json:[]"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=5000' 200
T2=$SECONDS
if [ $(($T2-$T1)) -lt 5 ] || [ $(($T2-$T1)) -gt 7 ]; then
    echo "Delay too short or too long"$(($T2-$T1))". Should be 10 sec"
    exit 1
else
    echo "  Delay ok:"$(($T2-$T1))
fi

echo "=== Fetch a request with limit 25, shall be empty.  ==="
RESULT="json-array-size:0"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=1000&limit=25' 200

echo "=== Send 5 request to test limit on MR GET==="
RESULT="*"
for i in {1..5}
do
    do_curl POST '/send-request?operation=GET&url=/test2' 200
done

echo "=== Fetch a request with limit 3.  ==="
RESULT="json-array-size:3"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=1000&limit=3' 200

echo "=== Fetch a request with limit 3, shall return 2.  ==="
RESULT="json-array-size:2"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=1000&limit=3' 200

echo "=== Fetch a request with limit 3, shall return 0.  ==="
RESULT="json-array-size:0"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=1000&limit=3' 200

echo "=== Send a json response ==="
# Create minimal accepted response message
echo "[{\"correlationId\": \""$CORRID"\", \"message\": \"test2-response\", \"status\": \"200\"}]" > .tmp.json
RESULT="{}"
do_curl POST /events/A1-POLICY-AGENT-WRITE 200 .tmp.json

echo "=== Fetch a response ==="
RESULT="test2-response200"
do_curl GET '/receive-response?correlationid='$CORRID 200


echo "=== Send a json response ==="
# Create minimal accepted response message, array
echo "{\"correlationId\": \""$CORRID"\", \"message\": {\"test\":\"testresponse\"}, \"status\": \"200\"}" > .tmp.json
RESULT="{}"
do_curl POST /events/generic-path 200 .tmp.json

echo "=== Fetch a request ==="
RESULT="json:[\"{\\\"correlationId\\\": \\\""$CORRID"\\\", \\\"message\\\": {\\\"test\\\": \\\"testresponse\\\"}, \\\"status\\\": \\\"200\\\"}\"]"
do_curl GET '/events/generic-path' 200

echo "********************"
echo "*** All tests ok ***"
echo "********************"