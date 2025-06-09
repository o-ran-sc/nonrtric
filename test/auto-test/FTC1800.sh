#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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


TC_ONELINE_DESCR="ICS Create 10000 jobs (ei and info) restart, test job persistency"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="ICS PRODSTUB CR CP NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="ICS PRODSTUB CP CR KUBEPROXY NGW"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

clean_environment

start_kube_proxy

use_ics_rest_http

use_prod_stub_http

start_ics NOPROXY $SIM_GROUP/$ICS_COMPOSE_DIR/$ICS_CONFIG_FILE

start_prod_stub

set_ics_trace

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_cr 1

CB_JOB="$PROD_STUB_SERVICE_PATH$PROD_STUB_JOB_CALLBACK"
CB_SV="$PROD_STUB_SERVICE_PATH$PROD_STUB_SUPERVISION_CALLBACK"
TARGET="http://localhost:80/target"  # Dummy target

NUM_JOBS=5000 # 5K ei jobs and 5K info jobs

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    #Type registration status callbacks
    TYPESTATUS1="$CR_SERVICE_APP_PATH_0/type-status1"
    TYPESTATUS2="$CR_SERVICE_APP_PATH_0/type-status2"

    ics_api_idc_put_subscription 201 subscription-id-1 owner1 $TYPESTATUS1

    ics_api_idc_get_subscription_ids 200 owner1 subscription-id-1

    ics_api_idc_put_subscription 201 subscription-id-2 owner2 $TYPESTATUS2

    ics_api_idc_get_subscription_ids 200 owner2 subscription-id-2

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



ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json
ics_api_edp_put_type_2 201 type2 testdata/ics/ei-type-2.json
ics_api_edp_put_type_2 201 type3 testdata/ics/ei-type-3.json
ics_api_edp_put_type_2 201 type4 testdata/ics/ei-type-4.json
ics_api_edp_put_type_2 201 type5 testdata/ics/ei-type-5.json

ics_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

ics_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2

ics_api_edp_put_producer_2 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3

ics_api_edp_put_producer_2 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5

ics_api_edp_put_type_2 201 type101 testdata/ics/info-type-1.json
ics_api_edp_put_type_2 201 type102 testdata/ics/info-type-2.json
ics_api_edp_put_type_2 201 type103 testdata/ics/info-type-3.json
ics_api_edp_put_type_2 201 type104 testdata/ics/info-type-4.json
ics_api_edp_put_type_2 201 type105 testdata/ics/info-type-5.json



if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 20 30
    cr_equal 0 received_callbacks?id=type-status1 10
    cr_equal 0 received_callbacks?id=type-status2 10

    cr_api_check_all_ics_subscription_events 200 0 type-status1 \
        type1 testdata/ics/ei-type-1.json REGISTERED \
        type2 testdata/ics/ei-type-2.json REGISTERED \
        type3 testdata/ics/ei-type-3.json REGISTERED \
        type4 testdata/ics/ei-type-4.json REGISTERED \
        type5 testdata/ics/ei-type-5.json REGISTERED \
        type101 testdata/ics/info-type-1.json REGISTERED \
        type102 testdata/ics/info-type-2.json REGISTERED \
        type103 testdata/ics/info-type-3.json REGISTERED \
        type104 testdata/ics/info-type-4.json REGISTERED \
        type105 testdata/ics/info-type-5.json REGISTERED

    cr_api_check_all_ics_subscription_events 200 0 type-status2 \
        type1 testdata/ics/ei-type-1.json REGISTERED \
        type2 testdata/ics/ei-type-2.json REGISTERED \
        type3 testdata/ics/ei-type-3.json REGISTERED \
        type4 testdata/ics/ei-type-4.json REGISTERED \
        type5 testdata/ics/ei-type-5.json REGISTERED \
        type101 testdata/ics/info-type-1.json REGISTERED \
        type102 testdata/ics/info-type-2.json REGISTERED \
        type103 testdata/ics/info-type-3.json REGISTERED \
        type104 testdata/ics/info-type-4.json REGISTERED \
        type105 testdata/ics/info-type-5.json REGISTERED

fi

ics_api_edp_put_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 type101

ics_api_edp_put_producer_2 200 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2 type101 type102

ics_api_edp_put_producer_2 200 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3 type101 type102 type103

ics_api_edp_put_producer_2 200 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5 type104 type105


ics_equal json:data-producer/v1/info-producers 4

ics_api_edp_get_producer_status 200 prod-a ENABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ics_api_a1_put_job 201 job$i type1 $TARGET ric1 $CR_SERVICE_APP_PATH_0/job_status_ric1_$(($i+$NUM_JOBS)) testdata/ics/job-template.json
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type101 $TARGET info-owner $CR_SERVICE_APP_PATH_0/job_status_info-owner$(($i+$NUM_JOBS)) testdata/ics/job-template.json VALIDATE
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 3 prod-a prod-b prod-c 120
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ics_api_a1_put_job 201 job$i type2 $TARGET ric1 $CR_SERVICE_APP_PATH_0/job_status_ric1_$(($i+$NUM_JOBS)) testdata/ics/job-template.json
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type102 $TARGET info-owner $CR_SERVICE_APP_PATH_0/job_status_info-owner$(($i+$NUM_JOBS)) testdata/ics/job-template.json VALIDATE
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 2 prod-b prod-c 120
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ics_api_a1_put_job 201 job$i type3 $TARGET ric1 $CR_SERVICE_APP_PATH_0/job_status_ric1_$(($i+$NUM_JOBS)) testdata/ics/job-template.json
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type103 $TARGET info-owner $CR_SERVICE_APP_PATH_0/job_status_info-owner$(($i+$NUM_JOBS)) testdata/ics/job-template.json VALIDATE
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 1 prod-c 120
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ics_api_a1_put_job 201 job$i type4 $TARGET ric1 $CR_SERVICE_APP_PATH_0/job_status_ric1_$(($i+$NUM_JOBS)) testdata/ics/job-template.json
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type104 $TARGET info-owner $CR_SERVICE_APP_PATH_0/job_status_info-owner$(($i+$NUM_JOBS)) testdata/ics/job-template.json VALIDATE
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 1 prod-d 120
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ics_api_a1_put_job 201 job$i type5 $TARGET ric1 $CR_SERVICE_APP_PATH_0/job_status_ric1_$(($i+$NUM_JOBS)) testdata/ics/job-template.json
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_put_job 201 job$(($i+$NUM_JOBS)) type105 $TARGET info-owner $CR_SERVICE_APP_PATH_0/job_status_info-owner$(($i+$NUM_JOBS)) testdata/ics/job-template.json VALIDATE
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 1 prod-d 120
    fi
done

ics_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type2 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type3 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type4 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type5 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type101 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type102 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type103 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type104 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type105 $(($NUM_JOBS/5))

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 20 30

else
    cr_equal 0 received_callbacks 0 30

fi


if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then

    ics_equal json:data-consumer/v1/info-type-subscription 2 200

    ics_api_idc_get_subscription_ids 200 owner1 subscription-id-1
    ics_api_idc_get_subscription_ids 200 owner2 subscription-id-2

    ics_equal json:data-producer/v1/info-types 10 1000

fi

stop_ics

cr_api_reset 0

start_stopped_ics

set_ics_trace

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then

    ics_equal json:data-consumer/v1/info-type-subscription 2 200

    ics_api_idc_get_subscription_ids 200 owner1 subscription-id-1
    ics_api_idc_get_subscription_ids 200 owner2 subscription-id-2

    ics_equal json:data-producer/v1/info-types 10 1000
fi

cr_equal 0 received_callbacks 0

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        prodstub_delete_jobdata 204 prod-a job$i
        prodstub_delete_jobdata 204 prod-b job$i
        prodstub_delete_jobdata 204 prod-c job$i
        prodstub_delete_jobdata 204 prod-a job$(($i+$NUM_JOBS))
        prodstub_delete_jobdata 204 prod-b job$(($i+$NUM_JOBS))
        prodstub_delete_jobdata 204 prod-c job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 1 ]; then
        prodstub_delete_jobdata 204 prod-b job$i
        prodstub_delete_jobdata 204 prod-c job$i
        prodstub_delete_jobdata 204 prod-b job$(($i+$NUM_JOBS))
        prodstub_delete_jobdata 204 prod-c job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 2 ]; then
        prodstub_delete_jobdata 204 prod-c job$i
        prodstub_delete_jobdata 204 prod-c job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 3 ]; then
        prodstub_delete_jobdata 204 prod-d job$i
        prodstub_delete_jobdata 204 prod-d job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 4 ]; then
        prodstub_delete_jobdata 204 prod-d job$i
        prodstub_delete_jobdata 204 prod-d job$(($i+$NUM_JOBS))
    fi
done

ics_api_edp_get_producer_status 404 prod-a
ics_api_edp_get_producer_status 404 prod-b
ics_api_edp_get_producer_status 404 prod-c
ics_api_edp_get_producer_status 404 prod-d

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ics_api_a1_get_job_status 200 job$i DISABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) DISABLED EMPTYPROD 120
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ics_api_a1_get_job_status 200 job$i DISABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) DISABLED EMPTYPROD 120
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ics_api_a1_get_job_status 200 job$i DISABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) DISABLED EMPTYPROD 120
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ics_api_a1_get_job_status 200 job$i DISABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) DISABLED EMPTYPROD 120
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ics_api_a1_get_job_status 200 job$i DISABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) DISABLED EMPTYPROD 120
    fi
done

ics_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1  type101

ics_api_edp_put_producer_2 201 prod-b $CB_JOB/prod-b $CB_SV/prod-b type1 type2  type101 type102

ics_api_edp_put_producer_2 201 prod-c $CB_JOB/prod-c $CB_SV/prod-c type1 type2 type3  type101 type102 type103

ics_api_edp_put_producer_2 201 prod-d $CB_JOB/prod-d $CB_SV/prod-d type4 type5  type104 type105

ics_equal json:data-producer/v1/info-producers 4

ics_api_edp_get_producer_status 200 prod-a ENABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 3 prod-a prod-b prod-c 120
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 2 prod-b prod-c 120
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 1 prod-c 120
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 1 prod-d 120
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ics_api_a1_get_job_status 200 job$i ENABLED 120
        ics_api_idc_get_job_status2 200 job$(($i+$NUM_JOBS)) ENABLED 1 prod-d 120
    fi
done


ics_equal json:A1-EI/v1/eijobs?eiTypeId=type1 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type2 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type3 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type4 $(($NUM_JOBS/5))
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type5 $(($NUM_JOBS/5))

ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type101 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type102 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type103 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type104 $(($NUM_JOBS/5))
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type105 $(($NUM_JOBS/5))

for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        prodstub_check_jobdata_3 200 prod-a job$i type1 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-b job$i type1 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-c job$i type1 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-a job$(($i+$NUM_JOBS)) type101 $TARGET info-owner testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-b job$(($i+$NUM_JOBS)) type101 $TARGET info-owner testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-c job$(($i+$NUM_JOBS)) type101 $TARGET info-owner testdata/ics/job-template.json

    fi
    if [ $(($i%5)) -eq 1 ]; then
        prodstub_check_jobdata_3 200 prod-b job$i type2 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-c job$i type2 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-b job$(($i+$NUM_JOBS)) type102 $TARGET info-owner testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-c job$(($i+$NUM_JOBS)) type102 $TARGET info-owner testdata/ics/job-template.json
    fi
    if [ $(($i%5)) -eq 2 ]; then
        prodstub_check_jobdata_3 200 prod-c job$i type3 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-c job$(($i+$NUM_JOBS)) type103 $TARGET info-owner testdata/ics/job-template.json
    fi
    if [ $(($i%5)) -eq 3 ]; then
        prodstub_check_jobdata_3 200 prod-d job$i type4 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-d job$(($i+$NUM_JOBS)) type104 $TARGET info-owner testdata/ics/job-template.json
    fi
    if [ $(($i%5)) -eq 4 ]; then
        prodstub_check_jobdata_3 200 prod-d job$i type5 $TARGET ric1 testdata/ics/job-template.json
        prodstub_check_jobdata_3 200 prod-d job$(($i+$NUM_JOBS)) type105 $TARGET info-owner testdata/ics/job-template.json
    fi
done


for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $(($i%5)) -eq 0 ]; then
        ics_api_a1_delete_job 204 job$i
        ics_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 1 ]; then
        ics_api_a1_delete_job 204 job$i
        ics_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 2 ]; then
        ics_api_a1_delete_job 204 job$i
        ics_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 3 ]; then
        ics_api_a1_delete_job 204 job$i
        ics_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
    fi
    if [ $(($i%5)) -eq 4 ]; then
        ics_api_a1_delete_job 204 job$i
        ics_api_idc_delete_job 204 job$(($i+$NUM_JOBS))
    fi
done

ics_equal json:data-producer/v1/info-producers 4

ics_api_edp_get_producer_status 200 prod-a ENABLED
ics_api_edp_get_producer_status 200 prod-b ENABLED
ics_api_edp_get_producer_status 200 prod-c ENABLED
ics_api_edp_get_producer_status 200 prod-d ENABLED

ics_equal json:A1-EI/v1/eijobs?eiTypeId=type1 0
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type2 0
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type3 0
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type4 0
ics_equal json:A1-EI/v1/eijobs?eiTypeId=type5 0

ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type101 0
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type102 0
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type103 0
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type104 0
ics_equal json:data-consumer/v1/info-jobs?infoTypeId=type105 0

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    ics_api_edp_put_type_2 200 type101 testdata/ics/info-type-1.json
    ics_api_edp_put_type_2 200 type102 testdata/ics/info-type-2.json
    ics_api_edp_put_type_2 200 type103 testdata/ics/info-type-3.json
    ics_api_edp_put_type_2 200 type104 testdata/ics/info-type-4.json
    ics_api_edp_put_type_2 200 type105 testdata/ics/info-type-5.json
fi

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    deviation "Total number of job callbacks are not stable - there may be additional job status callbacks"
    cr_equal 0 received_callbacks 10 30    # 10 type status
    cr_equal 0 received_callbacks?id=type-status1 5
    cr_equal 0 received_callbacks?id=type-status2 5

    cr_api_check_all_ics_subscription_events 200 0 type-status1 \
        type101 testdata/ics/info-type-1.json REGISTERED \
        type102 testdata/ics/info-type-2.json REGISTERED \
        type103 testdata/ics/info-type-3.json REGISTERED \
        type104 testdata/ics/info-type-4.json REGISTERED \
        type105 testdata/ics/info-type-5.json REGISTERED

    cr_api_check_all_ics_subscription_events 200 0 type-status2 \
        type101 testdata/ics/info-type-1.json REGISTERED \
        type102 testdata/ics/info-type-2.json REGISTERED \
        type103 testdata/ics/info-type-3.json REGISTERED \
        type104 testdata/ics/info-type-4.json REGISTERED \
        type105 testdata/ics/info-type-5.json REGISTERED

else
    cr_equal 0 received_callbacks 0 30
fi

check_ics_logs

store_logs END

#### TEST COMPLETE ####


print_result

auto_clean_environment