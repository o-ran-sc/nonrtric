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

# Automated test script for Kafka procon container

# NOTE: Need a running instance of kafka


export PORT=8096
export HTTPX="http"
export REQ_CONTENT=""
export RESP_CONTENT="text/plain"

# source function to do curl and check result
. ../common/do_curl_function.sh

echo "Requires a running kafka"

payload=".payload"

echo "=== hello world ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="OK"
do_curl GET / 200

echo "=== reset ==="
REQ_CONTENT=""
RESP_CONTENT=""
RESULT="*"
do_curl POST /reset 200

echo "=== get topics ==="
REQ_CONTENT=""
RESP_CONTENT="application/json"
RESULT="json:[]"
do_curl GET /topics 200

echo "=== get global counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /counters/sent 200

echo "=== get global counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /counters/received 200

echo "=== get topic ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/test-topic 404

echo "=== get topic counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/test-topic/counters/sent 404

echo "=== get topic counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/test-topic/counters/received 404

echo "=== create a topic ==="
REQ_CONTENT=""
RESP_CONTENT=""
RESULT="*"
do_curl PUT /topics/test-topic 405

echo "=== start to send on a topic ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/test-topic/startsend 404

echo "=== start to receive from a  topic ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/test-topic/startreceive 404

echo "=== send a msg on a  topic ==="
echo "TEST1" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/test-topic/msg 404 $payload

echo "=== receive a msg  from a  topic ==="
echo "TEST1" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/test-topic/msg 404 $payload

echo "=== stop to send on a  topic ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/test-topic/stopsend 404

echo "=== stop to receive from a  topic ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/test-topic/stopreceive 404

# Create 4 topics

echo "=== create topic1 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl PUT /topics/topic1?type=text/plain 201

echo "=== get topics ==="
REQ_CONTENT=""
RESP_CONTENT="application/json"
RESULT="json:[\"topic1\"]"
do_curl GET /topics 200

echo "=== create topic2 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl PUT /topics/topic2?type=text/plain 201

echo "=== get topics ==="
REQ_CONTENT=""
RESP_CONTENT="application/json"
RESULT="json:[\"topic1\",\"topic2\"]"
do_curl GET /topics 200

echo "=== create topic3 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl PUT /topics/topic3?type=application/json 201

echo "=== get topics ==="
REQ_CONTENT=""
RESP_CONTENT="application/json"
RESULT="json:[\"topic1\",\"topic2\",\"topic3\"]"
do_curl GET /topics 200

echo "=== create topic4 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl PUT /topics/topic4?type=application/json 201

echo "=== get topics ==="
REQ_CONTENT=""
RESP_CONTENT="application/json"
RESULT="json:[\"topic1\",\"topic2\",\"topic3\",\"topic4\"]"
do_curl GET /topics 200

echo "=== get topic1 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="text/plain"
do_curl GET /topics/topic1 200

echo "=== get topic2 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="text/plain"
do_curl GET /topics/topic2 200

echo "=== get topic3 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="application/json"
do_curl GET /topics/topic3 200

echo "=== get topic4 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="application/json"
do_curl GET /topics/topic4 200

echo "=== send a msg on topic1 ==="
echo "TEST11" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/msg 400  $payload

echo "=== receive a msg  from topic1 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/topic1/msg 400

echo "=== send a msg on topic2 ==="
echo "TEST22" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic2/msg 400 $payload

echo "=== receive a msg  from topic2 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/topic2/msg 400



echo "=== send a msg on topic3 ==="
echo "{\"test\":\"33\"}" > $payload
REQ_CONTENT="application/json"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic3/msg 400 $payload

echo "=== receive a msg  from topic3 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/topic3/msg 400

echo "=== send a msg on topic4 ==="
echo "{\"test\":\"44\"}" > $payload
REQ_CONTENT="application/json"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic4/msg 400 $payload

echo "=== receive a msg  from topic4 ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/topic2/msg 400


echo "=== get global counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /counters/sent 200

echo "=== get global counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /counters/received 200

echo "=== get topic1 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic1/counters/sent 200

echo "=== get topic1 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic1/counters/received 200

echo "=== get topic2 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic2/counters/sent 200

echo "=== get topic2 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic2/counters/received 200

echo "=== get topic3 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic3/counters/sent 200

echo "=== get topic3 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic3/counters/received 200

echo "=== get topic4 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic4/counters/sent 200

echo "=== get topic4 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic4/counters/received 200

# Begins send and receive

echo "=== set topic1 start sending ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/startsend 200

echo "=== send a msg on topic1 ==="
echo "TEST11" > $payload
REQ_CONTENT="application/json"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/msg 400  $payload

echo "=== send a msg on topic1 ==="
echo "TEST11" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/msg 200  $payload

echo "sleep 2  to allow sending the msg to kafka"
sleep 2

echo "=== receive a msg  from topic1 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl GET /topics/topic1/msg 400

echo "=== get topic1 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="1"
do_curl GET /topics/topic1/counters/sent 200

echo "=== get topic1 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="0"
do_curl GET /topics/topic1/counters/received 200

echo "=== set topic1 start receiving ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/startreceive 200

echo "sleep 60 to allow kafka to process the msg, unclear why first message takes a long time..."
sleep 60

echo "=== get topic1 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="1"
do_curl GET /topics/topic1/counters/sent 200

echo "=== get topic1 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="1"
do_curl GET /topics/topic1/counters/received 200

echo "=== get global counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="1"
do_curl GET /counters/sent 200

echo "=== get global counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="1"
do_curl GET /counters/received 200

echo "=== receive a msg from topic1 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="TEST11"
do_curl GET /topics/topic1/msg 200

echo "=== receive a msg from topic1 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT=""
RESULT="*"
do_curl GET /topics/topic1/msg 204


echo "=== set topic1 start sending ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/startsend 200

echo "=== set topic2 start sending ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic2/startsend 200

echo "=== set topic3 start sending ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic3/startsend 200

echo "=== set topic4 start sending ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic4/startsend 200

echo "=== set topic1 start receiving ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/startreceive 200

echo "=== set topic2 start receiving ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic2/startreceive 200

echo "=== set topic3 start receiving ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic3/startreceive 200

echo "=== set topic4 start receiving ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic4/startreceive 200


# Send and receive on all topics

echo "=== send a msg on topic1 ==="
echo "TEST101" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic1/msg 200  $payload

echo "=== send two msg on topic2 ==="
echo "TEST201" > $payload
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic2/msg 200  $payload
echo "TEST202" > $payload
do_curl POST /topics/topic2/msg 200  $payload

echo "=== send three msg on topic3 ==="
echo "{\"a\":\"msg301\"}" > $payload
REQ_CONTENT="application/json"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic3/msg 200  $payload
echo "{\"a\":\"msg302\"}" > $payload
do_curl POST /topics/topic3/msg 200  $payload
echo "{\"a\":\"msg303\"}" > $payload
do_curl POST /topics/topic3/msg 200  $payload


echo "=== send four msg on topic4 ==="
echo "{\"a\":\"msg401\"}" > $payload
REQ_CONTENT="application/json"
RESP_CONTENT="text/plain"
RESULT="*"
do_curl POST /topics/topic4/msg 200  $payload
echo "{\"a\":\"msg402\"}" > $payload
do_curl POST /topics/topic4/msg 200  $payload
echo "{\"a\":\"msg403\"}" > $payload
do_curl POST /topics/topic4/msg 200  $payload
echo "{\"a\":\"msg404\"}" > $payload
do_curl POST /topics/topic4/msg 200  $payload

echo "sleep 10 to allow kafka to process msg"
sleep 10

echo "=== get global counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="11"
do_curl GET /counters/sent 200

echo "=== get global counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="11"
do_curl GET /counters/received 200


echo "=== get topic1 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="2"
do_curl GET /topics/topic1/counters/sent 200

echo "=== get topic1 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="2"
do_curl GET /topics/topic1/counters/received 200


echo "=== get topic2 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="2"
do_curl GET /topics/topic2/counters/sent 200

echo "=== get topic2 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="2"
do_curl GET /topics/topic2/counters/received 200


echo "=== get topic3 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="3"
do_curl GET /topics/topic3/counters/sent 200

echo "=== get topic3 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="3"
do_curl GET /topics/topic3/counters/received 200


echo "=== get topic4 counter sent ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="4"
do_curl GET /topics/topic4/counters/sent 200

echo "=== get topic4 counter received ==="
REQ_CONTENT=""
RESP_CONTENT="text/plain"
RESULT="4"
do_curl GET /topics/topic4/counters/received 200


echo "=== get a msg on topic1 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="TEST101"
do_curl GET /topics/topic1/msg 200


echo "=== attempt to receive a msg from topic1 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT=""
RESULT="*"
do_curl GET /topics/topic1/msg 204

echo "=== get a two msg on topic2 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="text/plain"
RESULT="TEST201"
do_curl GET /topics/topic2/msg 200
RESULT="TEST202"
do_curl GET /topics/topic2/msg 200


echo "=== attempt to receive a msg from topic2 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT=""
RESULT="*"
do_curl GET /topics/topic2/msg 204

echo "=== get three msg on topic3 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="application/json"
RESULT="json:{\"a\":\"msg301\"}"
do_curl GET /topics/topic3/msg 200
RESULT="json:{\"a\":\"msg302\"}"
do_curl GET /topics/topic3/msg 200
RESULT="json:{\"a\":\"msg303\"}"
do_curl GET /topics/topic3/msg 200

echo "=== attempt to receive a msg from topic3 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT=""
RESULT="*"
do_curl GET /topics/topic3/msg 204

echo "=== send four msg on topic4 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT="application/json"
RESULT="json:{\"a\":\"msg401\"}"
do_curl GET /topics/topic4/msg 200
RESULT="json:{\"a\":\"msg402\"}"
do_curl GET /topics/topic4/msg 200
RESULT="json:{\"a\":\"msg403\"}"
do_curl GET /topics/topic4/msg 200
RESULT="json:{\"a\":\"msg404\"}"
do_curl GET /topics/topic4/msg 200

echo "=== attempt to receive a msg from topic4 ==="
REQ_CONTENT="text/plain"
RESP_CONTENT=""
RESULT="*"
do_curl GET /topics/topic4/msg 204

echo "********************"
echo "*** All tests ok ***"
echo "********************"

