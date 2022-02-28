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

# Automated test script for producer stub container

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
    PORT=8992
    # Set http protocol
    HTTPX="http"
else
    #Default https port for the simulator
    PORT=8993
    # Set https protocol
    HTTPX="https"
fi

# source function to do curl and check result
. ../common/do_curl_function.sh
RESP_CONTENT="*"

echo "=== hello world ==="
RESULT="OK"
do_curl GET / 200

echo "=== reset ==="
RESULT=""
do_curl GET /reset 200

echo "=== status ==="
RESULT="json:{}"
do_curl GET /status 200

# Basic admin

echo "===  check supervision counter ==="
RESULT="-1"
do_curl GET /counter/supervision/prod-x 200

echo "===  check create counter ==="
RESULT="-1"
do_curl GET /counter/create/prod-x/job-x 200

echo "===  check delete counter ==="
RESULT="-1"
do_curl GET /counter/delete/prod-x/job-x 200

## Create/update a producer

echo "===  create producer ==="
RESULT="Unknown query parameter(s)"
do_curl PUT /arm/supervision/prod-x?test=201 400

echo "===  create producer ==="
RESULT=""
do_curl PUT /arm/supervision/prod-x?response=201 200

echo "===  update producer ==="
RESULT=""
do_curl PUT /arm/supervision/prod-x?response=400 200

echo "===  update producer ==="
RESULT=""
do_curl PUT /arm/supervision/prod-x 200

## Add types to a producere

echo "===  add type 10 ==="
RESULT=""
do_curl PUT /arm/type/prod-x/10 200

echo "===  add type 15 ==="
RESULT=""
do_curl PUT /arm/type/prod-x/15 200

## check the db

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 200, \"supervision_counter\": 0, \"types\": [\"10\", \"15\"]}}"
do_curl GET /status 200

## Add type
echo "===  add type 20 ==="
RESULT=""
do_curl PUT /arm/type/prod-x/20 200

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 200, \"supervision_counter\": 0, \"types\": [\"10\", \"15\", \"20\"]}}"
do_curl GET /status 200

## remove type
echo "===  remove type 20 ==="
RESULT=""
do_curl DELETE /arm/type/prod-x/20 200

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 200, \"supervision_counter\": 0, \"types\": [\"10\", \"15\"]}}"
do_curl GET /status 200

## producer supervision
echo "===  check supervision counter ==="
RESULT="0"
do_curl GET /counter/supervision/prod-x 200

echo "===  supervision producer ==="
RESULT=""
do_curl GET /callbacks/supervision/prod-x 200

echo "===  update producer ==="
RESULT=""
do_curl PUT /arm/supervision/prod-x?response=400 200

echo "===  callback supervision producer ==="
RESULT="returning configured response code"
do_curl GET /callbacks/supervision/prod-x 400

## check the db

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 400, \"supervision_counter\": 2, \"types\": [\"10\", \"15\"]}}"
do_curl GET /status 200

## create/update job

echo "===  add job ==="
RESULT=""
do_curl PUT /arm/create/prod-x/job-y 200

echo "===  update job ==="
RESULT=""
do_curl PUT /arm/create/prod-x/job-y?response=405 200

## check the db

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 400, \"supervision_counter\": 2, \"types\": [\"10\", \"15\"], \"job-y\": {\"create_response\": 405, \"delete_response\": 404, \"json\": null, \"create_counter\": 0, \"delete_counter\": 0, \"delivering\": \"stopped\", \"delivery_attempts\": 0}}}"
do_curl GET /status 200

## add delete response for job

echo "===  update job ==="
RESULT=""
do_curl PUT /arm/delete/prod-x/job-y?response=407 200

## check the db

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 400, \"supervision_counter\": 2, \"types\": [\"10\", \"15\"], \"job-y\": {\"create_response\": 405, \"delete_response\": 407, \"json\": null, \"create_counter\": 0, \"delete_counter\": 0, \"delivering\": \"stopped\", \"delivery_attempts\": 0}}}"
do_curl GET /status 200

## Get jobdata
echo "=== job data ==="
RESULT=""
do_curl GET /jobdata/prod-x/job-y 204

##  callback create

echo "===  add job ==="
RESULT=""
do_curl PUT /arm/create/prod-x/job-1 200
RESULT=""
do_curl PUT /arm/delete/prod-x/job-1 200

echo "===  callback create job ==="
RESULT=""
echo "{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\",\"ei_type_identity\": \"10\"}" > .p.json
do_curl POST /callbacks/job/prod-x 201 .p.json

echo "===  callback create job -update ==="
RESULT=""
echo "{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\",\"ei_type_identity\": \"10\"}" > .p.json
do_curl POST /callbacks/job/prod-x 200 .p.json

## Get jobdata
echo "=== job data ==="
RESULT="json:{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\", \"ei_type_identity\": \"10\"}"
do_curl GET /jobdata/prod-x/job-1 200

## check the db

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 400, \"supervision_counter\": 2, \"types\": [\"10\", \"15\"], \"job-y\": {\"create_response\": 405, \"delete_response\": 407, \"json\": null, \"create_counter\": 0, \"delete_counter\": 0, \"delivering\": \"stopped\", \"delivery_attempts\": 0}, \"job-1\": {\"create_response\": 200, \"delete_response\": 204, \"json\": {\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\", \"ei_type_identity\": \"10\"}, \"create_counter\": 2, \"delete_counter\": 0, \"delivering\": \"delivering\", \"delivery_attempts\": 0}}}"
do_curl GET /status 200

# create and delete job tests
echo "===  set job create response ==="
RESULT=""
do_curl PUT /arm/create/prod-x/job-1?response=404 200

echo "===  callback create job -update ==="
RESULT="returning configured response code"
echo "{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\",\"ei_type_identity\": \"10\"}" > .p.json
do_curl POST /callbacks/job/prod-x 404 .p.json

echo "===  set job delete response ==="
RESULT=""
do_curl PUT /arm/delete/prod-x/job-1?response=404 200

echo "===  callback delete job==="
RESULT="returning configured response code"
echo "{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\",\"ei_type_identity\": \"10\"}" > .p.json
do_curl DELETE /callbacks/job/prod-x/job-1 404 .p.json

echo "===  set job delete response ==="
RESULT=""
do_curl PUT /arm/delete/prod-x/job-1 200

echo "===  callback delete job==="
RESULT=""
echo "{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\",\"ei_type_identity\": \"10\"}" > .p.json
do_curl DELETE /callbacks/job/prod-x/job-1 204 .p.json

## check the db

echo "=== status ==="
RESULT="json:{\"prod-x\": {\"supervision_response\": 400, \"supervision_counter\": 2, \"types\": [\"10\", \"15\"], \"job-y\": {\"create_response\": 405, \"delete_response\": 407, \"json\": null, \"create_counter\": 0, \"delete_counter\": 0, \"delivering\": \"stopped\", \"delivery_attempts\": 0}, \"job-1\": {\"create_response\": 404, \"delete_response\": 404, \"json\": null, \"create_counter\": 3, \"delete_counter\": 2, \"delivering\": \"stopped\", \"delivery_attempts\": 0}}}"
do_curl GET /status 200


##  data delivery

echo "===  update producer ==="
RESULT=""
do_curl PUT /arm/create/prod-x/job-1 200

echo "===  callback create job ==="
RESULT=""
echo "{\"ei_job_identity\": \"job-1\", \"ei_job_data\": {}, \"target_uri\": \"http://localhost:80\",\"ei_type_identity\": \"10\"}" > .p.json
do_curl POST /callbacks/job/prod-x 201 .p.json

echo "=== data delivery start ==="
RESULT="job not found"
do_curl POST /jobdata/prod-x/job-x?action=START 404

echo "=== data delivery start ==="
RESULT=""
do_curl POST /jobdata/prod-x/job-1?action=START 200

echo "sleep 5"
sleep 5

echo "=== data delivery stop ==="
RESULT=""
do_curl POST /jobdata/prod-x/job-1?action=STOP 200

echo "********************"
echo "*** All tests ok ***"
echo "********************"
