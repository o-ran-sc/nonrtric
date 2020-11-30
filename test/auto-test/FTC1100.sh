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


TC_ONELINE_DESCR="ECS full intefaces walkthrough"

#App names to include in the test, space separated list
INCLUDED_IMAGES="ECS PRODSTUB CR RICSIM CP"

#SUPPORTED TEST ENV FILE
SUPPORTED_PROFILES="ONAP-MASTER ORAN-CHERRY"

. ../common/testcase_common.sh  $@
. ../common/ecs_api_functions.sh
. ../common/prodstub_api_functions.sh
. ../common/cr_api_functions.sh

#### TEST BEGIN ####

FLAT_A1_EI="1"

clean_containers

use_ecs_rest_https

use_prod_stub_https

use_simulator_https

use_cr_https

start_ecs

start_prod_stub

set_ecs_trace

start_control_panel

if [ "$PMS_VERSION" == "V2" ]; then
    start_ric_simulators ricsim_g3 4  STD_2.0.0
fi

start_cr

CB_JOB="$PROD_STUB_HTTPX://$PROD_STUB_APP_NAME:$PROD_STUB_PORT/callbacks/job"
CB_SV="$PROD_STUB_HTTPX://$PROD_STUB_APP_NAME:$PROD_STUB_PORT/callbacks/supervision"
TARGET1="$RIC_SIM_HTTPX://ricsim_g3_1:$RIC_SIM_PORT/datadelivery"
TARGET2="$RIC_SIM_HTTPX://ricsim_g3_2:$RIC_SIM_PORT/datadelivery"
TARGET3="$RIC_SIM_HTTPX://ricsim_g3_3:$RIC_SIM_PORT/datadelivery"
TARGET8="$RIC_SIM_HTTPX://ricsim_g3_4:$RIC_SIM_PORT/datadelivery"
TARGET10="$RIC_SIM_HTTPX://ricsim_g3_4:$RIC_SIM_PORT/datadelivery"

STATUS1="$CR_HTTPX://$CR_APP_NAME:$CR_PORT/callbacks/job1-status"
STATUS2="$CR_HTTPX://$CR_APP_NAME:$CR_PORT/callbacks/job2-status"
STATUS3="$CR_HTTPX://$CR_APP_NAME:$CR_PORT/callbacks/job3-status"
STATUS8="$CR_HTTPX://$CR_APP_NAME:$CR_PORT/callbacks/job8-status"
STATUS10="$CR_HTTPX://$CR_APP_NAME:$CR_PORT/callbacks/job10-status"

### Setup prodstub sim to accept calls for producers, types and jobs
## prod-a type1
## prod-b type1 and type2
## prod-c no-type
## prod-d type4
## prod-e type6
## prod-f type6

## job1 -> prod-a
## job2 -> prod-a
## job3 -> prod-b
## job4 -> prod-a
## job6 -> prod-b
## job8 -> prod-d
## job10 -> prod-e and prod-f

prodstub_arm_producer 200 prod-a
prodstub_arm_producer 200 prod-b
prodstub_arm_producer 200 prod-c
prodstub_arm_producer 200 prod-d
prodstub_arm_producer 200 prod-e
prodstub_arm_producer 200 prod-f

prodstub_arm_type 200 prod-a type1
prodstub_arm_type 200 prod-b type2
prodstub_arm_type 200 prod-b type3
prodstub_arm_type 200 prod-d type4
prodstub_arm_type 200 prod-e type6
prodstub_arm_type 200 prod-f type6

prodstub_disarm_type 200 prod-b type3
prodstub_arm_type 200 prod-b type1
prodstub_disarm_type 200 prod-b type1


prodstub_arm_job_create 200 prod-a job1
prodstub_arm_job_create 200 prod-a job2
prodstub_arm_job_create 200 prod-b job3

prodstub_arm_job_delete 200 prod-a job1
prodstub_arm_job_delete 200 prod-a job2
prodstub_arm_job_delete 200 prod-b job3

prodstub_arm_job_create 200 prod-b job4
prodstub_arm_job_create 200 prod-a job4

prodstub_arm_job_create 200 prod-b job6

prodstub_arm_job_create 200 prod-d job8

prodstub_arm_job_create 200 prod-e job10
prodstub_arm_job_create 200 prod-f job10

### ecs status
ecs_api_service_status 200

cr_equal received_callbacks 0

### Initial tests - no config made
### GET: type ids, types, producer ids, producers, job ids, jobs
### DELETE: jobs
ecs_api_a1_get_type_ids 200 EMPTY
ecs_api_a1_get_type 404 test-type

ecs_api_edp_get_type_ids 200 EMPTY
ecs_api_edp_get_type 404 test-type

ecs_api_edp_get_producer_ids 200 EMPTY
ecs_api_edp_get_producer 404 test-prod

ecs_api_edp_get_producer_status 404 test-prod

ecs_api_edp_delete_producer 404 test-prod

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_ids 404 test-type NOWNER
    ecs_api_a1_get_job_ids 404 test-type test-owner

    ecs_api_a1_get_job 404 test-type test-job

    ecs_api_a1_get_job_status 404 test-type test-job
else
    ecs_api_a1_get_job_ids 200 test-type NOWNER EMPTY
    ecs_api_a1_get_job_ids 200 test-type test-owner EMPTY

    ecs_api_a1_get_job 404 test-job

    ecs_api_a1_get_job_status 404 test-job
fi

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_delete_job 404 test-type test-job
else
    ecs_api_a1_delete_job 404 test-job
fi

ecs_api_edp_get_producer_jobs 404 test-prod


### Setup of producer/job and testing apis ###

## Setup prod-a
ecs_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json
ecs_api_edp_put_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json


ecs_api_a1_get_type_ids 200 type1
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_type 200 type1 testdata/ecs/ei-type-1.json
else
    ecs_api_a1_get_type 200 type1 testdata/ecs/empty-type.json
fi

ecs_api_edp_get_type_ids 200 type1
ecs_api_edp_get_type 200 type1 testdata/ecs/ei-type-1.json prod-a

ecs_api_edp_get_producer_ids 200 prod-a

ecs_api_edp_get_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_edp_get_producer_status 200 prod-a ENABLED

ecs_api_a1_get_job_ids 200 type1 NOWNER EMPTY
ecs_api_a1_get_job_ids 200 type1 test-owner EMPTY

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job 404 type1 test-job

    ecs_api_a1_get_job_status 404 type1 test-job
else
    ecs_api_a1_get_job 404 test-job

    ecs_api_a1_get_job_status 404 test-job
fi

ecs_api_edp_get_producer_jobs 200 prod-a EMPTY

## Create a job for prod-a
## job1 - prod-a
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type1 job1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ecs/job-template.json
fi

# Check the job data in the producer
prodstub_check_jobdata 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json

ecs_api_a1_get_job_ids 200 type1 NOWNER job1
ecs_api_a1_get_job_ids 200 type1 ricsim_g3_1 job1

if [ ! -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1
fi

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job 200 type1 job1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json

    ecs_api_a1_get_job_status 200 type1 job1 ENABLED
else
    ecs_api_a1_get_job 200 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ecs/job-template.json

    ecs_api_a1_get_job_status 200 job1 ENABLED
fi

prodstub_equal create/prod-a/job1 1

ecs_api_edp_get_producer_jobs 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json

## Create a second job for prod-a
## job2 - prod-a
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type1 job2 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ecs/job-template.json
fi

# Check the job data in the producer
prodstub_check_jobdata 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json

ecs_api_a1_get_job_ids 200 type1 NOWNER job1 job2
ecs_api_a1_get_job_ids 200 type1 ricsim_g3_1 job1
ecs_api_a1_get_job_ids 200 type1 ricsim_g3_2 job2
if [ ! -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2
fi

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job 200 type1 job2 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json

    ecs_api_a1_get_job_status 200 type1 job2 ENABLED
else
    ecs_api_a1_get_job 200 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ecs/job-template.json

    ecs_api_a1_get_job_status 200 job2 ENABLED
fi

prodstub_equal create/prod-a/job2 1

ecs_api_edp_get_producer_jobs 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json job2 type1 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json

## Setup prod-b
ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2 testdata/ecs/ei-type-2.json

ecs_api_a1_get_type_ids 200 type1 type2
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_type 200 type1 testdata/ecs/ei-type-1.json
    ecs_api_a1_get_type 200 type2 testdata/ecs/ei-type-2.json
else
    ecs_api_a1_get_type 200 type1 testdata/ecs/empty-type.json
    ecs_api_a1_get_type 200 type2 testdata/ecs/empty-type.json
fi

ecs_api_edp_get_type_ids 200 type1 type2
ecs_api_edp_get_type 200 type1 testdata/ecs/ei-type-1.json prod-a
ecs_api_edp_get_type 200 type2 testdata/ecs/ei-type-2.json prod-b

ecs_api_edp_get_producer_ids 200 prod-a prod-b

ecs_api_edp_get_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json
ecs_api_edp_get_producer 200 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2 testdata/ecs/ei-type-2.json


ecs_api_edp_get_producer_status 200 prod-b ENABLED

## Create job for prod-b
##  job3 - prod-b
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type2 job3 $TARGET3 ricsim_g3_3 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ecs/job-template.json
fi

prodstub_equal create/prod-b/job3 1

# Check the job data in the producer
prodstub_check_jobdata 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ecs/job-template.json

ecs_api_a1_get_job_ids 200 type1 NOWNER job1 job2
ecs_api_a1_get_job_ids 200 type2 NOWNER job3
ecs_api_a1_get_job_ids 200 type1 ricsim_g3_1 job1
ecs_api_a1_get_job_ids 200 type1 ricsim_g3_2 job2
ecs_api_a1_get_job_ids 200 type2 ricsim_g3_3 job3

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job 200 type2 job3 $TARGET3 ricsim_g3_3 testdata/ecs/job-template.json

    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
else
    ecs_api_a1_get_job 200 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ecs/job-template.json

    ecs_api_a1_get_job_status 200 job3 ENABLED
fi

ecs_api_edp_get_producer_jobs 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json job2 type1 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json
ecs_api_edp_get_producer_jobs 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ecs/job-template.json


## Setup prod-c (no types)
ecs_api_edp_put_producer 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c NOTYPE


ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c

ecs_api_edp_get_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json
ecs_api_edp_get_producer 200 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2 testdata/ecs/ei-type-2.json
ecs_api_edp_get_producer 200 prod-c $CB_JOB/prod-c $CB_SV/prod-c EMPTY

ecs_api_edp_get_producer_status 200 prod-c ENABLED


## Delete job3 and prod-b and re-create if different order

# Delete job then producer
ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3
ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_delete_job 204 type2 job3
else
    ecs_api_a1_delete_job 204 job3
fi

ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2
ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c

ecs_api_edp_delete_producer 204 prod-b

ecs_api_edp_get_producer_status 404 prod-b

ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2
ecs_api_edp_get_producer_ids 200 prod-a prod-c

prodstub_equal delete/prod-b/job3 1

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 404 type2 job3 $TARGET3 ricsim_g3_3 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 404 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ecs/job-template.json
fi

# Put producer then job
ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2 testdata/ecs/ei-type-2.json

ecs_api_edp_get_producer_status 200 prod-b ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type2 job3 $TARGET3 ricsim_g3_3 testdata/ecs/job-template2.json
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
else
    ecs_api_a1_put_job 201 job3 type2 $TARGET3 ricsim_g3_3 $STATUS3 testdata/ecs/job-template2.json
    ecs_api_a1_get_job_status 200 job3 ENABLED
fi

prodstub_check_jobdata 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ecs/job-template2.json

ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3
ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c

prodstub_equal create/prod-b/job3 2
prodstub_equal delete/prod-b/job3 1

# Delete only the producer
ecs_api_edp_delete_producer 204 prod-b

ecs_api_edp_get_producer_status 404 prod-b

ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3
ecs_api_edp_get_producer_ids 200 prod-a prod-c

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type2 job3 DISABLED
else
    ecs_api_a1_get_job_status 200 job3 DISABLED
fi

cr_equal received_callbacks 1 30
cr_equal received_callbacks?id=job3-status 1
cr_api_check_all_ecs_events 200 job3-status DISABLED

# Re-create the producer
ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type2 testdata/ecs/ei-type-2.json

ecs_api_edp_get_producer_status 200 prod-b ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
else
    ecs_api_a1_get_job_status 200 job3 ENABLED
fi

cr_equal received_callbacks 2 30
cr_equal received_callbacks?id=job3-status 2
cr_api_check_all_ecs_events 200 job3-status ENABLED

prodstub_check_jobdata 200 prod-b job3 type2 $TARGET3 ricsim_g3_3 testdata/ecs/job-template2.json


## Setup prod-d
ecs_api_edp_put_producer 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 testdata/ecs/ei-type-1.json

ecs_api_a1_get_job_ids 200 type4 NOWNER EMPTY

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type4 job8 $TARGET8 ricsim_g3_4 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job8 type4 $TARGET8 ricsim_g3_4 $STATUS8 testdata/ecs/job-template.json
fi

prodstub_check_jobdata 200 prod-d job8 type4 $TARGET8 ricsim_g3_4 testdata/ecs/job-template.json

prodstub_equal create/prod-d/job8 1
prodstub_equal delete/prod-d/job8 0

ecs_api_a1_get_job_ids 200 type4 NOWNER job8

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
else
    ecs_api_a1_get_job_status 200 job8 ENABLED
fi

# Re-PUT the producer with zero types
ecs_api_edp_put_producer 200 prod-d $CB_JOB/prod-d $CB_SV/prod-d NOTYPE

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_ids 404 type4 NOWNER
else
    ecs_api_a1_get_job_ids 200 type4 NOWNER job8
    ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3 job8
fi

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type4 job8 DISABLED
else
    ecs_api_a1_get_job_status 200 job8 DISABLED
fi

cr_equal received_callbacks 3 30
cr_equal received_callbacks?id=job8-status 1
cr_api_check_all_ecs_events 200 job8-status DISABLED

prodstub_equal create/prod-d/job8 1
prodstub_equal delete/prod-d/job8 0

## Re-setup prod-d
ecs_api_edp_put_producer 200 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 testdata/ecs/ei-type-1.json

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_ids 404 type4 NOWNER
else
    ecs_api_a1_get_job_ids 200 type4 NOWNER job8
    ecs_api_a1_get_job_ids 200 NOTYPE NOWNER job1 job2 job3 job8
fi

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
else
    ecs_api_a1_get_job_status 200 job8 ENABLED
fi

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED

cr_equal received_callbacks 4 30
cr_equal received_callbacks?id=job8-status 2
cr_api_check_all_ecs_events 200 job8-status ENABLED

prodstub_equal create/prod-d/job8 2
prodstub_equal delete/prod-d/job8 0


## Setup prod-e
ecs_api_edp_put_producer 201 prod-e $CB_JOB/prod-e $CB_SV/prod-e type6 testdata/ecs/ei-type-6.json

ecs_api_a1_get_job_ids 200 type6 NOWNER EMPTY

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type6 job10 $TARGET10 ricsim_g3_4 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job10 type6 $TARGET10 ricsim_g3_4 $STATUS10 testdata/ecs/job-template.json
fi

prodstub_check_jobdata 200 prod-e job10 type6 $TARGET10 ricsim_g3_4 testdata/ecs/job-template.json

prodstub_equal create/prod-e/job10 1
prodstub_equal delete/prod-e/job10 0

ecs_api_a1_get_job_ids 200 type6 NOWNER job10

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

## Setup prod-f
ecs_api_edp_put_producer 201 prod-f $CB_JOB/prod-f $CB_SV/prod-f type6 testdata/ecs/ei-type-6.json

ecs_api_a1_get_job_ids 200 type6 NOWNER job10

prodstub_check_jobdata 200 prod-f job10 type6 $TARGET10 ricsim_g3_4 testdata/ecs/job-template.json

prodstub_equal create/prod-f/job10 1
prodstub_equal delete/prod-f/job10 0

ecs_api_a1_get_job_ids 200 type6 NOWNER job10

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

## Status updates prod-a and jobs

ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c prod-d prod-e prod-f

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e ENABLED
ecs_api_edp_get_producer_status 200 prod-f ENABLED

# Arm producer prod-a for supervision failure
prodstub_arm_producer 200 prod-a 400

# Wait for producer prod-a to go disabled
ecs_api_edp_get_producer_status 200 prod-a DISABLED 360

ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c prod-d  prod-e prod-f

ecs_api_edp_get_producer_status 200 prod-a DISABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e ENABLED
ecs_api_edp_get_producer_status 200 prod-f ENABLED


if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type1 job1 ENABLED
    ecs_api_a1_get_job_status 200 type1 job2 ENABLED
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job1 ENABLED
    ecs_api_a1_get_job_status 200 job2 ENABLED
    ecs_api_a1_get_job_status 200 job3 ENABLED
    ecs_api_a1_get_job_status 200 job8 ENABLED
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

# Arm producer prod-a for supervision
prodstub_arm_producer 200 prod-a 200

# Wait for producer prod-a to go enabled
ecs_api_edp_get_producer_status 200 prod-a ENABLED 360

ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c prod-d prod-e prod-f

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e ENABLED
ecs_api_edp_get_producer_status 200 prod-f ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type1 job1 ENABLED
    ecs_api_a1_get_job_status 200 type1 job2 ENABLED
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job1 ENABLED
    ecs_api_a1_get_job_status 200 job2 ENABLED
    ecs_api_a1_get_job_status 200 job3 ENABLED
    ecs_api_a1_get_job_status 200 job8 ENABLED
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

# Arm producer prod-a for supervision failure
prodstub_arm_producer 200 prod-a 400

# Wait for producer prod-a to go disabled
ecs_api_edp_get_producer_status 200 prod-a DISABLED 360

ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c prod-d prod-e prod-f

ecs_api_edp_get_producer_status 200 prod-a DISABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e ENABLED
ecs_api_edp_get_producer_status 200 prod-f ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type1 job1 ENABLED
    ecs_api_a1_get_job_status 200 type1 job2 ENABLED
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job1 ENABLED
    ecs_api_a1_get_job_status 200 job2 ENABLED
    ecs_api_a1_get_job_status 200 job3 ENABLED
    ecs_api_a1_get_job_status 200 job8 ENABLED
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

# Wait for producer prod-a to be removed
ecs_equal json:ei-producer/v1/eiproducers 5 1000

ecs_api_edp_get_producer_ids 200 prod-b prod-c prod-d prod-e prod-f

ecs_api_edp_get_producer_status 404 prod-a
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e ENABLED
ecs_api_edp_get_producer_status 200 prod-f ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type1 job1 DISABLED
    ecs_api_a1_get_job_status 200 type1 job2 DISABLED
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job1 DISABLED
    ecs_api_a1_get_job_status 200 job2 DISABLED
    ecs_api_a1_get_job_status 200 job3 ENABLED
    ecs_api_a1_get_job_status 200 job8 ENABLED
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

cr_equal received_callbacks 6 30
cr_equal received_callbacks?id=job1-status 1
cr_equal received_callbacks?id=job2-status 1

cr_api_check_all_ecs_events 200 job1-status DISABLED
cr_api_check_all_ecs_events 200 job2-status DISABLED


# Arm producer prod-e for supervision failure
prodstub_arm_producer 200 prod-e 400

ecs_api_edp_get_producer_status 200 prod-e DISABLED 1000

ecs_api_edp_get_producer_ids 200 prod-b prod-c prod-d prod-e prod-f

ecs_api_edp_get_producer_status 404 prod-a
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e DISABLED
ecs_api_edp_get_producer_status 200 prod-f ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type1 job1 DISABLED
    ecs_api_a1_get_job_status 200 type1 job2 DISABLED
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job1 DISABLED
    ecs_api_a1_get_job_status 200 job2 DISABLED
    ecs_api_a1_get_job_status 200 job3 ENABLED
    ecs_api_a1_get_job_status 200 job8 ENABLED
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

#Disable create for job10 in prod-e
prodstub_arm_job_create 200 prod-e job10 400

#Update tjob 10 - only prod-f will be updated
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 200 type6 job10 $TARGET10 ricsim_g3_4 testdata/ecs/job-template2.json
else
    ecs_api_a1_put_job 200 job10 type6 $TARGET10 ricsim_g3_4 $STATUS10 testdata/ecs/job-template2.json
fi
#Reset producer and job responses
prodstub_arm_producer 200 prod-e 200
prodstub_arm_job_create 200 prod-e job10 200

ecs_api_edp_get_producer_status 200 prod-e ENABLED 360

ecs_api_edp_get_producer_ids 200 prod-b prod-c prod-d prod-e prod-f

#Job 10 should be updated when the producer goes enabled
deviation "Job 10 should be updated when the producer prod-e goes enabled"
prodstub_check_jobdata 200 prod-e job10 type6 $TARGET10 ricsim_g3_4 testdata/ecs/job-template2.json
prodstub_check_jobdata 200 prod-f job10 type6 $TARGET10 ricsim_g3_4 testdata/ecs/job-template2.json

prodstub_arm_producer 200 prod-f 400

ecs_api_edp_get_producer_status 200 prod-f DISABLED 360

ecs_equal json:ei-producer/v1/eiproducers 4 1000

ecs_api_edp_get_producer_ids 200 prod-b prod-c prod-d prod-e

ecs_api_edp_get_producer_status 404 prod-a
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED
ecs_api_edp_get_producer_status 200 prod-e ENABLED
ecs_api_edp_get_producer_status 404 prod-f

if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_get_job_status 200 type1 job1 DISABLED
    ecs_api_a1_get_job_status 200 type1 job2 DISABLED
    ecs_api_a1_get_job_status 200 type2 job3 ENABLED
    ecs_api_a1_get_job_status 200 type4 job8 ENABLED
    ecs_api_a1_get_job_status 200 type6 job10 ENABLED
else
    ecs_api_a1_get_job_status 200 job1 DISABLED
    ecs_api_a1_get_job_status 200 job2 DISABLED
    ecs_api_a1_get_job_status 200 job3 ENABLED
    ecs_api_a1_get_job_status 200 job8 ENABLED
    ecs_api_a1_get_job_status 200 job10 ENABLED
fi

cr_equal received_callbacks 6

check_ecs_logs

store_logs END

#### TEST COMPLETE ####

print_result

auto_clean_containers
