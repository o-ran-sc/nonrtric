#!/bin/bash

# Automated test script for mrstub container container

# mr-stub port
export PORT=3905

# source function to do curl and check result
. do_curl.sh

echo "=== Stub hello world ==="
RESULT="OK"
do_curl GET / 200

echo "=== Stub reset ==="
RESULT="OK"
do_curl GET /reset 200

echo "=== Send a request ==="
RESULT="*"
do_curl POST '/send-request?operation=GET&url=test' 200
#Save id for later
CORRID=$body

echo "=== Fetch a response, shall be empty ==="
RESULT=""
do_curl GET '/receive-response?correlationId='$CORRID 204

echo "=== Fetch a request ==="
# Cannot check the responded json since it contains variable data....test rely on response code only
RESULT="*"
do_curl GET '/events/A1-POLICY-AGENT-READ/users/policy-agent' 200

echo "=== Send a response ==="
# Create minimal accepted response message
echo "[{\"correlationId\": \""$CORRID"\", \"message\": \"test\", \"status\": \"200\"}]" > .tmp.json
RESULT="OK"
do_curl POST /events/A1-POLICY-AGENT-WRITE 200 .tmp.json

echo "=== Fetch a response ==="
RESULT="test200"
do_curl GET '/receive-response?correlationId='$CORRID 200


echo "********************"
echo "*** All tests ok ***"
echo "********************"
