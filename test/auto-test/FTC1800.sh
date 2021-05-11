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


TC_ONELINE_DESCR="ECS Create 10000 jobs (ei and info) restart, test job persistency"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="ECS PRODSTUB CR CP NGW"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="ECS PRODSTUB CP CR KUBEPROXY NGW"
#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-HONOLULU ONAP-ISTANBUL ORAN-CHERRY ORAN-D-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh  $@
. ../common/ecs_api_functions.sh
. ../common/prodstub_api_functions.sh
. ../common/control_panel_api_functions.sh
. ../common/controller_api_functions.sh
. ../common/cr_api_functions.sh
. ../common/kube_proxy_api_functions.sh
. ../common/gateway_api_functions.sh

setup_testenvironment

#### TEST BEGIN ####

FLAT_A1_EI="1"

clean_environment

if [ $RUNMODE == "KUBE" ]; then
    start_kube_proxy
fi

use_ecs_rest_http

use_prod_stub_http

start_ecs NOPROXY $SIM_GROUP/$ECS_COMPOSE_DIR/$ECS_CONFIG_FILE

start_prod_stub

set_ecs_trace

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_cr

CB_JOB="$PROD_STUB_SERVICE_PATH$PROD_STUB_JOB_CALLBACK"
CB_SV="$PROD_STUB_SERVICE_PATH$PROD_STUB_SUPERVISION_CALLBACK"
TARGET="http://localhost:80/target"  # Dummy target

NUM_JOBS=10000

use_info_jobs=false  #Set flag if interface supporting info-types is used
if [[ "$ECS_FEATURE_LEVEL" == *"INFO-TYPES"* ]]; then
    use_info_jobs=true
    NUM_JOBS=5000 # 5K ei jobs and 5K info jobs
fi

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

if [ $use_info_jobs ]; then
    prodstub_arm_producer 200 prod-a
    prodstub_arm_producer 200 prod-b
    prodstub_arm_producer 200 prod-c
    prodstub_arm_producer 200 prod-d

    prodstub_arm_type 200 prod-a type101

    prodstub_arm_type 200 prod-b type101
    prodstub_arm_type 200 prod-b type102

    prodstub_arm_type 200 prod-c type101
    prodstub_arm_type 200 prod-c type102
    prodstub_arm_type 200 prod-c type103

    prodstub_arm_type 200 prod-d type104
    prodstub_arm_type 200 prod-d type105

    for ((i=1; i<=$NUM_JOBS; i++))
    do
        if [ $(($i%5)) -eq 0 ]; then
            prodstub_arm_job_create 200 prod-a job$(($i+$NUM_JOBS))
            prodstub_arm_job_create 200 prod-b job$(($i+$NUM_JOBS))
            prodstub_arm_job_create 200 prod-c job$(($i+$NUM_JOBS))
        fi
        if [ $(($i%5)) -eq 1 ]; then
            prodstub_arm_job_create 200 prod-b job$(($i+$NUM_JOBS))
            prodstub_arm_job_create 200 prod-c job$(($i+$NUM_JOBS))
        fi
        if [ $(($i%5)) -eq 2 ]; then
            prodstub_arm_job_create 200 prod-c job$(($i+$NUM_JOBS))
        fi
        if [ $(($i%5)) -eq 3 ]; then
            prodstub_arm_job_create 200 prod-d job$(($i+$NUM_JOBS))
        fi
        if [ $(($i%5)) -eq 4 ]; then
            prodstub_arm_job_create 200 prod-d job$(($i+$NUM_JOBS))
        fi
    done
fi


if [ $ECS_VERSION == "V1-1" ]; then

    ecs_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

    ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json

    ecs_api_edp_put_producer 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json type3 testdata/ecs/ei-type-3.json

    ecs_api_edp_put_producer 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 testdata/ecs/ei-type-4.json type5 testdata/ecs/ei-type-5.json

else

    ecs_api_edp_put_type_2 201 type1 testdata/ecs/ei-type-1.json
    ecs_api_edp_put_type_2 201 type2 testdata/ecs/ei-type-2.json
    ecs_api_edp_put_type_2 201 type3 testdata/ecs/ei-type-3.json
    ecs_api_edp_put_type_2 201 type4 testdata/ecs/ei-type-4.json
    ecs_api_edp_put_type_2 201 type5 testdata/ecs/ei-type-5.json

    ecs_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

    ecs_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2

    ecs_api_edp_put_producer_2 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3

    ecs_api_edp_put_producer_2 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5

    if [ $use_info_jobs ]; then
        ecs_api_edp_put_type_2 201 type101 testdata/ecs/info-type-1.json
        ecs_api_edp_put_type_2 201 type102 testdata/ecs/info-type-2.json
        ecs_api_edp_put_type_2 201 type103 testdata/ecs/info-type-3.json
        ecs_api_edp_put_type_2 201 type104 testdata/ecs/info-type-4.json
        ecs_api_edp_put_type_2 201 type105 testdata/ecs/info-type-5.json

        ecs_api_edp_put_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 type101

        ecs_api_edp_put_producer_2 200 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2 type101 type102

        ecs_api_edp_put_producer_2 200 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3 type101 type102 type103

        ecs_api_edp_put_producer_2 200 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5 type104 type105
    fi
fi

if [ $use_info_jobs ]; then
    ecs_equal json:data-producer/v1/info-producers 4
else
    ecs_equal json:ei-producer/v1/eiproducers 4
fi

ecs_api_edp_get_producer_status 200 prod-a ENABLED
ecs_api_edp_get_producer_status 200 prod-b ENABLED
ecs_api_edp_get_producer_status 200 prod-c ENABLED
ecs_api_edp_get_producer_status 200 prod-d ENABLED

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ecs_api_a1_put_job 201 job$i type1 $TARGET ric1 $CR_SERVICE_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type1 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type101 $TARGET info-owner $CR_SERVICE_PATH/job_status_info-owner testdata/ecs/job-template.json VALIDATE
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ecs_api_a1_put_job 201 job$i type2 $TARGET ric1 $CR_SERVICE_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type2 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type102 $TARGET info-owner $CR_SERVICE_PATH/job_status_info-owner testdata/ecs/job-template.json VALIDATE
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ecs_api_a1_put_job 201 job$i type3 $TARGET ric1 $CR_SERVICE_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type3 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type103 $TARGET info-owner $CR_SERVICE_PATH/job_status_info-owner testdata/ecs/job-template.json VALIDATE
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ecs_api_a1_put_job 201 job$i type4 $TARGET ric1 $CR_SERVICE_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type4 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type104 $TARGET info-owner $CR_SERVICE_PATH/job_status_info-owner testdata/ecs/job-template.json VALIDATE
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ecs_api_a1_put_job 201 job$i type5 $TARGET ric1 $CR_SERVICE_PATH/job_status_ric1 testdata/ecs/job-template.json
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type5 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type105 $TARGET info-owner $CR_SERVICE_PATH/job_status_info-owner testdata/ecs/job-template.json VALIDATE
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
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
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type2 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type3 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type4 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type5 $(($NUM_JOBS/5))
fi
if [ $use_info_jobs ]; then
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type101 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type102 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type103 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type104 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type105 $(($NUM_JOBS/5))
fi

stop_ecs

start_stopped_ecs

set_ecs_trace

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        prodstub_delete_jobdata 204 prod-a job$i
        prodstub_delete_jobdata 204 prod-b job$i
        prodstub_delete_jobdata 204 prod-c job$i
        if [ $use_info_jobs ]; then
            prodstub_delete_jobdata 204 prod-a job$(($i+$NUM_JOBS))
            prodstub_delete_jobdata 204 prod-b job$(($i+$NUM_JOBS))
            prodstub_delete_jobdata 204 prod-c job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        prodstub_delete_jobdata 204 prod-b job$i
        prodstub_delete_jobdata 204 prod-c job$i
        if [ $use_info_jobs ]; then
            prodstub_delete_jobdata 204 prod-b job$(($i+$NUM_JOBS))
            prodstub_delete_jobdata 204 prod-c job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        prodstub_delete_jobdata 204 prod-c job$i
        if [ $use_info_jobs ]; then
            prodstub_delete_jobdata 204 prod-c job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        prodstub_delete_jobdata 204 prod-d job$i
        if [ $use_info_jobs ]; then
            prodstub_delete_jobdata 204 prod-d job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        prodstub_delete_jobdata 204 prod-d job$i
        if [ $use_info_jobs ]; then
            prodstub_delete_jobdata 204 prod-d job$(($i+$NUM_JOBS))
        fi
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
            ecs_api_a1_get_job_status 200 job$i DISABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) DISABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type2 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) DISABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type3 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) DISABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type4 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) DISABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type5 job$i DISABLED
        else
            ecs_api_a1_get_job_status 200 job$i DISABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) DISABLED 120
        fi
    fi
done

if [ $ECS_VERSION == "V1-1" ]; then

    ecs_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

    ecs_api_edp_put_producer 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json

    ecs_api_edp_put_producer 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 testdata/ecs/ei-type-1.json type2 testdata/ecs/ei-type-2.json type3 testdata/ecs/ei-type-3.json

    ecs_api_edp_put_producer 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 testdata/ecs/ei-type-4.json type5 testdata/ecs/ei-type-5.json

else
    if [ $use_info_jobs ]; then
        ecs_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1  type101

        ecs_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2  type101 type102

        ecs_api_edp_put_producer_2 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3  type101 type102 type103

        ecs_api_edp_put_producer_2 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5  type104 type105
    else
        ecs_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

        ecs_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2

        ecs_api_edp_put_producer_2 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3

        ecs_api_edp_put_producer_2 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5
    fi

fi

if [ $use_info_jobs ]; then
    ecs_equal json:data-producer/v1/info-producers 4
else
    ecs_equal json:ei-producer/v1/eiproducers 4
fi

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
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type2 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type3 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type4 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        if [  -z "$FLAT_A1_EI" ]; then
            ecs_api_a1_get_job_status 200 type5 job$i ENABLED
        else
            ecs_api_a1_get_job_status 200 job$i ENABLED 120
        fi
        if [ $use_info_jobs ]; then
            ecs_api_idc_get_job_status 200 job$(($i+$NUM_JOBS)) ENABLED 120
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
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type2 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type3 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type4 $(($NUM_JOBS/5))
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type5 $(($NUM_JOBS/5))
fi

if [ $use_info_jobs ]; then
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type101 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type102 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type103 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type104 $(($NUM_JOBS/5))
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type105 $(($NUM_JOBS/5))
fi

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        if [ $ECS_VERSION == "V1-1" ]; then
            prodstub_check_jobdata 200 prod-a job$i type1 $TARGET ric1 testdata/ecs/job-template.json
            prodstub_check_jobdata 200 prod-b job$i type1 $TARGET ric1 testdata/ecs/job-template.json
            prodstub_check_jobdata 200 prod-c job$i type1 $TARGET ric1 testdata/ecs/job-template.json
        else
            if [ $use_info_jobs ]; then
                prodstub_check_jobdata_3 200 prod-a job$i type1 $TARGET ric1 testdata/ecs/job-template.json
                prodstub_check_jobdata_3 200 prod-b job$i type1 $TARGET ric1 testdata/ecs/job-template.json
                prodstub_check_jobdata_3 200 prod-c job$i type1 $TARGET ric1 testdata/ecs/job-template.json
            else
                prodstub_check_jobdata_2 200 prod-a job$i type1 $TARGET ric1 testdata/ecs/job-template.json
                prodstub_check_jobdata_2 200 prod-b job$i type1 $TARGET ric1 testdata/ecs/job-template.json
                prodstub_check_jobdata_2 200 prod-c job$i type1 $TARGET ric1 testdata/ecs/job-template.json
            fi
        fi
        if [ $use_info_jobs ]; then
            prodstub_check_jobdata_3 200 prod-a job$(($i+$NUM_JOBS)) type101 $TARGET info-owner testdata/ecs/job-template.json
            prodstub_check_jobdata_3 200 prod-b job$(($i+$NUM_JOBS)) type101 $TARGET info-owner testdata/ecs/job-template.json
            prodstub_check_jobdata_3 200 prod-c job$(($i+$NUM_JOBS)) type101 $TARGET info-owner testdata/ecs/job-template.json
        fi

    fi
    if [ $(($i%5)) -eq 1 ]; then
        if [ $ECS_VERSION == "V1-1" ]; then
            prodstub_check_jobdata 200 prod-b job$i type2 $TARGET ric1 testdata/ecs/job-template.json
            prodstub_check_jobdata 200 prod-c job$i type2 $TARGET ric1 testdata/ecs/job-template.json
        else
            if [ $use_info_jobs ]; then
                prodstub_check_jobdata_3 200 prod-b job$i type2 $TARGET ric1 testdata/ecs/job-template.json
                prodstub_check_jobdata_3 200 prod-c job$i type2 $TARGET ric1 testdata/ecs/job-template.json
            else
                prodstub_check_jobdata_2 200 prod-b job$i type2 $TARGET ric1 testdata/ecs/job-template.json
                prodstub_check_jobdata_2 200 prod-c job$i type2 $TARGET ric1 testdata/ecs/job-template.json
            fi
        fi
        if [ $use_info_jobs ]; then
            prodstub_check_jobdata_3 200 prod-b job$(($i+$NUM_JOBS)) type102 $TARGET info-owner testdata/ecs/job-template.json
            prodstub_check_jobdata_3 200 prod-c job$(($i+$NUM_JOBS)) type102 $TARGET info-owner testdata/ecs/job-template.json
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        if [ $ECS_VERSION == "V1-1" ]; then
            prodstub_check_jobdata 200 prod-c job$i type3 $TARGET ric1 testdata/ecs/job-template.json
        else
            if [ $use_info_jobs ]; then
                prodstub_check_jobdata_3 200 prod-c job$i type3 $TARGET ric1 testdata/ecs/job-template.json
            else
                prodstub_check_jobdata_2 200 prod-c job$i type3 $TARGET ric1 testdata/ecs/job-template.json
            fi
        fi
        if [ $use_info_jobs ]; then
            prodstub_check_jobdata_3 200 prod-c job$(($i+$NUM_JOBS)) type103 $TARGET info-owner testdata/ecs/job-template.json
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        if [ $ECS_VERSION == "V1-1" ]; then
            prodstub_check_jobdata 200 prod-d job$i type4 $TARGET ric1 testdata/ecs/job-template.json
        else
            if [ $use_info_jobs ]; then
                prodstub_check_jobdata_3 200 prod-d job$i type4 $TARGET ric1 testdata/ecs/job-template.json
            else
                prodstub_check_jobdata_2 200 prod-d job$i type4 $TARGET ric1 testdata/ecs/job-template.json
            fi
        fi
        if [ $use_info_jobs ]; then
            prodstub_check_jobdata_3 200 prod-d job$(($i+$NUM_JOBS)) type104 $TARGET info-owner testdata/ecs/job-template.json
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        if [ $ECS_VERSION == "V1-1" ]; then
            prodstub_check_jobdata 200 prod-d job$i type5 $TARGET ric1 testdata/ecs/job-template.json
        else
            if [ $use_info_jobs ]; then
                prodstub_check_jobdata_3 200 prod-d job$i type5 $TARGET ric1 testdata/ecs/job-template.json
            else
                prodstub_check_jobdata_2 200 prod-d job$i type5 $TARGET ric1 testdata/ecs/job-template.json
            fi
        fi
        if [ $use_info_jobs ]; then
            prodstub_check_jobdata_3 200 prod-d job$(($i+$NUM_JOBS)) type105 $TARGET info-owner testdata/ecs/job-template.json
        fi
    fi
done


for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ecs_api_a1_delete_job 204 job$i
        if [ $use_info_jobs ]; then
            ecs_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ecs_api_a1_delete_job 204 job$i
        if [ $use_info_jobs ]; then
            ecs_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ecs_api_a1_delete_job 204 job$i
        if [ $use_info_jobs ]; then
            ecs_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ecs_api_a1_delete_job 204 job$i
        if [ $use_info_jobs ]; then
            ecs_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
        fi
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ecs_api_a1_delete_job 204 job$i
        if [ $use_info_jobs ]; then
            ecs_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
        fi
    fi
done

if [ $use_info_jobs ]; then
    ecs_equal json:data-producer/v1/info-producers 4
else
    ecs_equal json:ei-producer/v1/eiproducers 4
fi

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
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type2 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type3 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type4 0
    ecs_equal json:A1-EI/v1/eijobs?eiTypeId=type5 0
fi

if [ $use_info_jobs ]; then
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type101 0
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type102 0
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type103 0
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type104 0
    ecs_equal json:data-consumer/v1/info-jobs?infoTypeId=type105 0
fi

check_ecs_logs

store_logs END

#### TEST COMPLETE ####


print_result

auto_clean_environment