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

# Automated test script for callback receiver container

# callbackreciver port
# export PORT=8090
if [ $# -ne 1 ]; then
    echo "Usage: ./basic_test.sh nonsecure|secure"
    exit 1
fi
if [ "$1" != "nonsecure" ] && [ "$1" != "secure" ]; then
    echo "Usage: ./basic_test.sh nonsecure|secure"
    exit 1
fi

if [ $1 == "nonsecure" ]; then
    #Default http port for the simulator
    PORT=8090
    # Set http protocol
    HTTPX="http"
else
    #Default https port for the simulator
    PORT=8091
    # Set https protocol
    HTTPX="https"
fi

# source function to do curl and check result
. ../common/do_curl_function.sh

echo "=== CR hello world ==="
RESULT="OK"
do_curl GET / 200


echo "=== Get counter - callbacks ==="
RESULT="0"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages 200


echo "=== Send a request ==="
RESULT="*"
#create payload
echo "\"DATA-MSG\"" > .tmp.json
do_curl POST '/callbacks/test' 200 .tmp.json


echo "=== Fetch an event, wrong id==="
RESULT="*"
do_curl GET '/get-event/wrongid' 204


echo "=== Get counter - callbacks ==="
RESULT="1"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="1"
do_curl GET /counter/current_messages 200


echo "=== Fetch an event ==="
RESULT="DATA-MSG"
do_curl GET '/get-event/test' 200

echo "=== Fetch an event again ==="
RESULT="*"
do_curl GET '/get-event/test' 204

echo "=== Get counter - callbacks ==="
RESULT="1"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - fetched events ==="
RESULT="1"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages 200

echo "=== CR reset ==="
RESULT="OK"
do_curl GET /reset 200

echo "=== Get counter - callbacks ==="
RESULT="0"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages 200


echo "********************"
echo "*** All tests ok ***"
echo "********************"
