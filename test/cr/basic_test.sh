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

RESP_CONTENT='*' #Dont check resp content type

echo "=== CR hello world ==="
RESULT="OK"
do_curl GET / 200

echo "=== Reset ==="
RESULT="*"
do_curl POST /reset 200

echo "=== Get counter - callbacks ==="
RESULT="0"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages 200

echo "=== Get counter - remote hosts ==="
RESULT="*"
do_curl GET /counter/remote_hosts 200

echo "=== Send a request non json ==="
RESULT="*"
#create payload
echo "DATA" > .tmp.json
do_curl POST '/callbacks/test' 200 .tmp.json

echo "=== Send a request ==="
RESULT="*"
#create payload
echo "{\"DATA-MSG\":\"msg\"}" > .tmp.json
do_curl POST '/callbacks/test' 200 .tmp.json


echo "=== Fetch an event, wrong id==="
RESULT="*"
do_curl GET '/get-event/wrongid' 204

# Test counters for all ids
echo "=== Get counter - callbacks ==="
RESULT="2"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="2"
do_curl GET /counter/current_messages 200

# Test counter for one id
echo "=== Get counter - callbacks ==="
RESULT="2"
do_curl GET /counter/received_callbacks?id=test 200

echo "=== Get counter - callback batches ==="
RESULT="1"
do_curl GET /counter/received_callback_batches?id=test 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks?id=test 200

echo "=== Get counter - current events ==="
RESULT="2"
do_curl GET /counter/current_messages?id=test 200

# Test counter for dummy id
echo "=== Get counter - callbacks ==="
RESULT="0"
do_curl GET /counter/received_callbacks?id=dummy 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches?id=dummy 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks?id=dummy 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages?id=dummy 200


echo "=== Fetch an event ==="
RESULT="json:{}"
do_curl GET '/get-event/test' 200

echo "=== Fetch an event ==="
RESULT="json:{\"DATA-MSG\":\"msg\"}"
do_curl GET '/get-event/test' 200

echo "=== Fetch an event again ==="
RESULT="*"
do_curl GET '/get-event/test' 204

echo "=== Get counter - callbacks ==="
RESULT="2"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="2"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages 200

# Test counter for one id
echo "=== Get counter - callbacks ==="
RESULT="2"
do_curl GET /counter/received_callbacks?id=test 200

echo "=== Get counter - callback batches ==="
RESULT="1"
do_curl GET /counter/received_callback_batches?id=test 200

echo "=== Get counter - fetched events ==="
RESULT="2"
do_curl GET /counter/fetched_callbacks?id=test 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages?id=test 200

echo "=== Send a request ==="
RESULT="*"
#create payload
echo "{\"DATA-MSG\":\"msg\"}" > .tmp.json
do_curl POST '/callbacks/test' 200 .tmp.json

echo "=== Send a request ==="
RESULT="*"
#create payload
echo "{\"DATA-MSG2\":\"msg2\"}" > .tmp.json
do_curl POST '/callbacks/test' 200 .tmp.json

echo "=== Send a request ==="
RESULT="*"
#create payload
echo "{\"DATA-MSG3\":\"msg3\"}" > .tmp.json
do_curl POST '/callbacks/test1' 200 .tmp.json

echo "=== Get counter - callbacks ==="
RESULT="5"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="2"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="3"
do_curl GET /counter/current_messages 200

# Test counter for one id, test1
echo "=== Get counter - callbacks ==="
RESULT="1"
do_curl GET /counter/received_callbacks?id=test1 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches?id=test1 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks?id=test1 200

echo "=== Get counter - current events ==="
RESULT="1"
do_curl GET /counter/current_messages?id=test1 200

echo "=== Fetch all events ==="
RESULT="json:[{\"DATA-MSG2\":\"msg2\"},{\"DATA-MSG\":\"msg\"}]"
do_curl GET '/get-all-events/test' 200

echo "=== Get counter - callbacks ==="
RESULT="5"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="4"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="1"
do_curl GET /counter/current_messages 200

echo "=== Send a request ==="
RESULT="*"
#create payload
echo "[{\"DATA-MSG\":\"msg\"},{\"DATA-MSG\":\"msg\"}]" > .tmp.json
do_curl POST '/callbacks-text/test' 200 .tmp.json

echo "=== Get counter - callbacks ==="
RESULT="7"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="1"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="4"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="3"
do_curl GET /counter/current_messages 200


echo "=== CR reset ==="
RESULT="OK"
do_curl GET /reset 200

echo "=== Get counter - callbacks ==="
RESULT="0"
do_curl GET /counter/received_callbacks 200

echo "=== Get counter - callback batches ==="
RESULT="0"
do_curl GET /counter/received_callback_batches 200

echo "=== Get counter - fetched events ==="
RESULT="0"
do_curl GET /counter/fetched_callbacks 200

echo "=== Get counter - current events ==="
RESULT="0"
do_curl GET /counter/current_messages 200


# Check delay

echo "=== Set delay 10 sec==="
RESULT="*"
do_curl POST /forcedelay?delay=10 200

TSECONDS=$SECONDS
echo "=== Send a request, dealyed ==="
RESULT="*"
#create payload
echo "{\"DATA-MSG\":\"msg-del1\"}" > .tmp.json
do_curl POST '/callbacks/test' 200 .tmp.json

if [ $(($SECONDS-$TSECONDS)) -lt 10 ]; then
    echo "  Delay failed $(($SECONDS-$TSECONDS))"
    echo "  Exiting...."
    exit 1
else
    echo "  Delay OK $(($SECONDS-$TSECONDS))"
fi


echo "=== Fetch an event ==="
RESULT="json:{\"DATA-MSG\":\"msg-del1\"}"
do_curl GET '/get-event/test' 200

echo "********************"
echo "*** All tests ok ***"
echo "********************"
