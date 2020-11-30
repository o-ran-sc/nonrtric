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


TC_ONELINE_DESCR="ECS Create 10000 jobs and restart, test job persisency"

#App names to include in the test, space separated list
INCLUDED_IMAGES="ECS PRODSTUB CR CP"

#SUPPORTED TEST ENV FILE
SUPPORTED_PROFILES="ONAP-MASTER ORAN-CHERRY"

. ../common/testcase_common.sh  $@
. ../common/ecs_api_functions.sh
. ../common/prodstub_api_functions.sh

#### TEST BEGIN ####

FLAT_A1_EI="1"

clean_containers

use_ecs_rest_http

use_prod_stub_http

start_ecs

start_prod_stub

set_ecs_trace

start_control_panel

start_cr

CB_JOB="http://$PROD_STUB_APP_NAME:$PROD_STUB_PORT/callbacks/job"
CB_SV="http://$PROD_STUB_APP_NAME:$PROD_STUB_PORT/callbacks/supervision"
TARGET="http://localhost:80/target"  # Dummy target

NUM_JOBS=10000

# Setup prodstub sim to accept calls for producers, types and jobs
prodstub_arm_producer 200 prod-a
prodstub_arm_producer 200 prod-b
prodstub_arm_producer 200 prod-c
prodstub_arm_producer 200 prod-d

prodstub_arm_type 200 prod-a type1

prodstub_arm_type 200 prod-b type1
prodstub_arm_type 200 prod-b type2

prodstub_arm_type 200 prod-c type1
prodstub_arm_type 200 prod-c type2
prodstub_arm_type 200 prod-c type3

prodstub_arm_type 200 prod-d type4
prodstub_arm_type 200 prod-d type5

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        prodstub_arm_job_create 200 prod-a job$i
        prodstub_arm_job_create 200 prod-b job$i
        prodstub_arm_job_create 200 prod-c job$i
    fi
    if [ $(($i%5)) -eq 1 ]; then
        prodstub_arm_job_create 200 prod-b job$i
        prodstub_arm_job_create 200 prod-c job$i
    fi
    if [ $(($i%5)) -eq 2 ]; then
        prodstub_arm_job_create 200 prod-c job$i
    fi
    if [ $(($i%5)) -eq 3 ]; then
        prodstub_arm_job_create 200 prod-d job$i
    fi
    if [ $(($i%5)) -eq 4 ]; then
        prodstub_arm_job_create 200 prod-d job$i
    fi
done

ecs_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json

ecs_api_edp_put_producer 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json type3 testdata/ecs/ei-type-3.json

ecs_api_edp_put_producer 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 testdata/ecs/ei-type-4.json type5 testdata/ecs/ei-type-5.json

ecs_equal json:ei-producer/v1/eiproducers 4

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ecs_api_a1_put_job 201 job$i type1 $TARGET ric1 $CR_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type1 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ecs_api_a1_put_job 201 job$i type2 $TARGET ric1 $CR_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type2 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ecs_api_a1_put_job 201 job$i type3 $TARGET ric1 $CR_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type3 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ecs_api_a1_put_job 201 job$i type4 $TARGET ric1 $CR_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type4 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ecs_api_a1_put_job 201 job$i type5 $TARGET ric1 $CR_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type5 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
done

if [  -z "$FLAT_A1_EI" ]; then
    ecs_equal json:A1-EI/v1/eitypes/type1/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type2/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type3/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type4/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type5/eijobs $(($NUM_JOBS/5))
else
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
fi

restart_ecs

set_ecs_trace

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        prodstub_delete_jobdata 204 prod-a job$i
        prodstub_delete_jobdata 204 prod-b job$i
        prodstub_delete_jobdata 204 prod-c job$i
    fi
    if [ $(($i%5)) -eq 1 ]; then
        prodstub_delete_jobdata 204 prod-b job$i
        prodstub_delete_jobdata 204 prod-c job$i
    fi
    if [ $(($i%5)) -eq 2 ]; then
        prodstub_delete_jobdata 204 prod-c job$i
    fi
    if [ $(($i%5)) -eq 3 ]; then
        prodstub_delete_jobdata 204 prod-d job$i
    fi
    if [ $(($i%5)) -eq 4 ]; then
        prodstub_delete_jobdata 204 prod-d job$i
    fi
done

ecs_api_edp_get_producer_status 404 prod-a
ecs_api_edp_get_producer_status 404 prod-b
ecs_api_edp_get_producer_status 404 prod-c
ecs_api_edp_get_producer_status 404 prod-d

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type1 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type2 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type3 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type4 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type5 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED
        fi
    fi
done


ecs_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json

ecs_api_edp_put_producer 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json type3 testdata/ecs/ei-type-3.json

ecs_api_edp_put_producer 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 testdata/ecs/ei-type-4.json type5 testdata/ecs/ei-type-5.json

ecs_equal json:ei-producer/v1/eiproducers 4

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type1 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type2 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type3 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type4 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type5 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED
        fi
    fi
done


if [  -z "$FLAT_A1_EI" ]; then
    ecs_equal json:A1-EI/v1/eitypes/type1/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type2/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type3/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type4/eijobs $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eitypes/type5/eijobs $(($NUM_JOBS/5))
else
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
fi

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        prodstub_check_jobdata 200 prod-a job$i type1 $TARGET ric1 testdata/ecs/job-template.json
        prodstub_check_jobdata 200 prod-b job$i type1 $TARGET ric1 testdata/ecs/job-template.json
        prodstub_check_jobdata 200 prod-c job$i type1 $TARGET ric1 testdata/ecs/job-template.json
    fi
    if [ $(($i%5)) -eq 1 ]; then
        prodstub_check_jobdata 200 prod-b job$i type2 $TARGET ric1 testdata/ecs/job-template.json
        prodstub_check_jobdata 200 prod-c job$i type2 $TARGET ric1 testdata/ecs/job-template.json
    fi
    if [ $(($i%5)) -eq 2 ]; then
        prodstub_check_jobdata 200 prod-c job$i type3 $TARGET ric1 testdata/ecs/job-template.json
    fi
    if [ $(($i%5)) -eq 3 ]; then
        prodstub_check_jobdata 200 prod-d job$i type4 $TARGET ric1 testdata/ecs/job-template.json
    fi
    if [ $(($i%5)) -eq 4 ]; then
        prodstub_check_jobdata 200 prod-d job$i type5 $TARGET ric1 testdata/ecs/job-template.json
    fi
done


for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ecs_api_a1_delete_job 204 job$i
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ecs_api_a1_delete_job 204 job$i
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ecs_api_a1_delete_job 204 job$i
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ecs_api_a1_delete_job 204 job$i
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ecs_api_a1_delete_job 204 job$i
    fi
done

ecs_equal json:ei-producer/v1/eiproducers 4

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED

if [  -z "$FLAT_A1_EI" ]; then
    ecs_equal json:A1-EI/v1/eitypes/type1/eijobs 0
    ecs_equal json:A1-EI/v1/eitypes/type2/eijobs 0
    ecs_equal json:A1-EI/v1/eitypes/type3/eijobs 0
    ecs_equal json:A1-EI/v1/eitypes/type4/eijobs 0
    ecs_equal json:A1-EI/v1/eitypes/type5/eijobs 0
else
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type1 0
fi

check_ecs_logs

store_logs END

#### TEST COMPLETE ####


print_result

auto_clean_containers
