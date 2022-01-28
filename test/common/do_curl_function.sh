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



# Function to execute curl towards a container (or process) and compare + print result
# Intended use is for basic test scripts where testing is done with curl and the returned response and payload need to be checked.
# args: GET|PUT|POST|DELETE <url> <target-response-code> [<payload-file>]
# All calls made to 'localhost:'<port>.
# Expects env PORT set to intended port number
# Expects env RESULT to contain the target response body.
# Optional env HTTPX shall contain protocol 'http' or 'https'. If not set, 'http' is used. For 'https' all cert errors are ignored
#   RESULT="*" means that returned payload is not checked, may contain any text
#   RESULT="<text>" means that the returned payload has to match the <text> exactly
#   RESULT="json:<returned-payload>" means that the returned json payload is compared with the expected result (order of json keys and index is irrelevant)
#   RESULT="json-array-size:<integer-size>" means that the returned json payload shall contain the number of element given by the <integer-size>
# Env BODY contains the response body after the call
# Any error will stop script execution
# How to use in a test script:  source this file into your bash test script to the make the function available.
# The function may create a dir 'tmp' for temporary files.

do_curl() {
    echo -e $BOLD"TEST(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ $EBOLD
    if [ $# -lt 3 ]; then
        echo "Need 3 or more parameters, <http-operation> <url> <response-code> [file]: "$@
        echo "Exting test script....."
        exit 1
    fi

    if [ -z $HTTPX ]; then
        if [ "$HTTPX" != "http" ] && [ "$HTTPX" != "https" ]; then
            echo "Env var HTTPX shall be set to 'http' or 'https'"
            echo "Exting test script....."
            exit 1
        fi
        PROT="http"
    else
        PROT=$HTTPX
    fi

    req_content=""
    if [ -z "$REQ_CONTENT" ]; then
        if [ $# -gt 3 ]; then
            req_content="-H Content-Type:application/json" #Assuming json
        fi
    else
        req_content="-H Content-Type:$REQ_CONTENT"
    fi
    resp_content=""
    if [ -z "$RESP_CONTENT" ]; then
        if [[ "$RESULT" == "json"* ]]; then
            resp_content="application/json"
        elif [[ "$RESULT" == "*" ]]; then
            resp_content=""
        else
            resp_content="text/plain"
        fi
    else
        resp_content=$RESP_CONTENT
    fi
    curlstr="curl -X "$1" -skw :%{content_type}:%{http_code} ${PROT}://localhost:$PORT$2 -H accept:*/*"
    if [ $# -gt 3 ]; then
        curlstr=$curlstr" $req_content --data-binary @"$4
    fi
    echo "  CMD:"$curlstr
    res=$($curlstr)
    status=${res:${#res}-3}
    reminder=${res:0:${#res}-4}
    content_type="${reminder##*:}"
    body="${reminder%:*}"

    export body
    if [ $status -ne $3 ]; then
        echo "  Error status:"$status" Expected status: "$3
        echo "  Body: "$body
        echo "Exting test script....."
        exit 1
    else
        echo "  OK, code: "$status"     (Expected)"
        if [[ "$resp_content" == '*' ]]; then
            :
        elif [[ "$content_type" == *"$resp_content"* ]]; then
            echo "  Content type: "$content_type"     (Expected)"
        else
            echo "  Expected content type: "$resp_content
            echo "  Got: "$content_type
            echo "Exiting....."
            exit 1
        fi
        echo "  Body: "$body
        if [ "$RESULT" == "*" ]; then
            echo "  Body contents not checked"
        elif [[ "$RESULT" == "json:"* ]]; then
            result=${RESULT:5:${#RESULT}}
            #Find dir of the common dir
            DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
            res=$(python ${DIR}/compare_json.py "$result" "$body")
            if [ $res -eq 0 ]; then
                echo "  Body as expected"
            else
                echo "  Expected json body: "$result
                echo "Exiting....."
                exit 1
            fi
        elif [[ "$RESULT" == "json-array-size:"* ]]; then
            count=${RESULT:16:${#RESULT}}
            #Find dir of the common dir
            DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" >/dev/null 2>&1 && pwd )"
            mkdir -p tmp
            echo $body > ./tmp/.tmp.json
            res=$(python ${DIR}/count_json_elements.py ./tmp/.tmp.json)
            if [ $res -eq $count ]; then
                echo "  Body (array size) as expected"
            else
                echo "  Expected json array size: "$count
                echo "Exiting....."
                exit 1
            fi
        else
            body="$(echo $body | tr -d '\n' )"
            if [ "$RESULT" == "$body" ]; then
                echo "  Body as expected"
            else
                echo "  Expected body: "$RESULT
                echo "Exiting....."
                exit 1
            fi
        fi
    fi
}