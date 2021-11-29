#!/usr/bin/env bash

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

TC_ONELINE_DESCR="Testing southbound proxy for PMS and ICS"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CBS CONSUL CP CR MR PA RICSIM ICS PRODSTUB HTTPPROXY NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" MR CR PA PRODSTUB RICSIM CP ICS HTTPPROXY KUBEPROXY NGW"
#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-HONOLULU ONAP-ISTANBUL ORAN-CHERRY ORAN-D-RELEASE ORAN-E-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

use_cr_https
use_agent_rest_https
use_simulator_https
use_ics_rest_https
use_prod_stub_https

if [ "$PMS_VERSION" == "V2" ]; then
    notificationurl=$CR_SERVICE_APP_PATH_0"/test"
else
   echo "PMS VERSION 2 (V2) is required"
   exit 1
fi

clean_environment

start_kube_proxy

STD_NUM_RICS=2

start_http_proxy

start_ric_simulators $RIC_SIM_PREFIX"_g3" $STD_NUM_RICS STD_2.0.0

start_mr #Just to prevent errors in the agent log...

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_policy_agent PROXY $SIM_GROUP/$POLICY_AGENT_COMPOSE_DIR/$POLICY_AGENT_CONFIG_FILE

if [ $RUNMODE == "DOCKER" ]; then
    start_consul_cbs
fi

prepare_consul_config      NOSDNC  ".consul_config.json"

if [ $RUNMODE == "KUBE" ]; then
    agent_load_config                       ".consul_config.json"
else
    consul_config_app                      ".consul_config.json"
fi

start_cr 1

start_prod_stub

start_ics PROXY $SIM_GROUP/$ICS_COMPOSE_DIR/$ICS_CONFIG_FILE

set_agent_trace

set_ics_debug

api_get_status 200

# Print the A1 version for STD 2.X
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_print $RIC_SIM_PREFIX"_g3_"$i interface
done
# Load the polictypes in std
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 demo-testdata/STD2/sim_qos.json
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS2_0.1.0 demo-testdata/STD2/sim_qos2.json
done

#Check the number of schemas and the individual schemas in STD
api_equal json:policy-types 2 120

for ((i=1; i<=$STD_NUM_RICS; i++))
do
    api_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g3_"$i 2 120
done

# Check the schemas in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    api_get_policy_type 200 STD_QOS_0_2_0 demo-testdata/STD2/qos-agent-modified.json
    api_get_policy_type 200 'STD_QOS2_0.1.0' demo-testdata/STD2/qos2-agent-modified.json
done

#Check the number of types
api_equal json:policy-types 2 300

api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_APP_PATH_0/1"

# Create policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    generate_policy_uuid
    api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 $((2300+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
    generate_policy_uuid
    api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i 'STD_QOS2_0.1.0' $((2400+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
done


# Check the number of policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_equal $RIC_SIM_PREFIX"_g3_"$i num_instances 2
done

# Print calling hosts STD 2.X
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_print $RIC_SIM_PREFIX"_g3_"$i remote_hosts
    sim_contains_str $RIC_SIM_PREFIX"_g3_"$i remote_hosts proxy
done

FLAT_A1_EI="1"

CB_JOB="$PROD_STUB_SERVICE_PATH$PROD_STUB_JOB_CALLBACK"
CB_SV="$PROD_STUB_SERVICE_PATH$PROD_STUB_SUPERVISION_CALLBACK"
RIC_G1_1=$RIC_SIM_PREFIX"_g3_1"
RIC_G1_2=$RIC_SIM_PREFIX"_g3_2"
if [ $RUNMODE == "KUBE" ]; then
    RIC_G1_1=$(get_kube_sim_host $RIC_G1_1)
    RIC_G1_2=$(get_kube_sim_host $RIC_G1_2)
fi
TARGET1="$RIC_SIM_HTTPX://$RIC_G1_1:$RIC_SIM_PORT/datadelivery"
TARGET2="$RIC_SIM_HTTPX://$RIC_G1_1:$RIC_SIM_PORT/datadelivery"

STATUS1="$CR_SERVICE_APP_PATH_0/job1-status"
STATUS2="$CR_SERVICE_APP_PATH_0/job2-status"

prodstub_arm_producer 200 prod-a
prodstub_arm_type 200 prod-a type1
prodstub_arm_job_create 200 prod-a job1
prodstub_arm_job_create 200 prod-a job2

### ics status
ics_api_service_status 200

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    #Type registration status callbacks
    TYPESTATUS1="$CR_SERVICE_APP_PATH_0/type-status1"

    ics_api_idc_put_subscription 201 subscription-id-1 owner1 $TYPESTATUS1

    ics_api_idc_get_subscription_ids 200 owner1 subscription-id-1
fi

## Setup prod-a
if [ $ICS_VERSION == "V1-1" ]; then
    ics_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ics/ei-type-1.json

    ics_api_edp_get_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ics/ei-type-1.json
else
    ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json

    ics_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

    ics_api_edp_get_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1
fi

ics_api_edp_get_producer_status 200 prod-a ENABLED


## Create a job for prod-a
## job1 - prod-a
if [  -z "$FLAT_A1_EI" ]; then
    ics_api_a1_put_job 201 type1 job1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json
else
    ics_api_a1_put_job 201 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ics/job-template.json
fi

# Check the job data in the producer
if [ $ICS_VERSION == "V1-1" ]; then
    prodstub_check_jobdata 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json
else
    if [[ "$ICS_FEATURE_LEVEL" != *"INFO-TYPES"* ]]; then
        prodstub_check_jobdata_2 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json
    else
        prodstub_check_jobdata_3 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json
    fi
fi


## Create a second job for prod-a
## job2 - prod-a
if [  -z "$FLAT_A1_EI" ]; then
    ics_api_a1_put_job 201 type1 job2 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
else
    ics_api_a1_put_job 201 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ics/job-template.json
fi

# Check the job data in the producer
if [ $ICS_VERSION == "V1-1" ]; then
    prodstub_check_jobdata 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
else
    if [[ "$ICS_FEATURE_LEVEL" != *"INFO-TYPES"* ]]; then
        prodstub_check_jobdata_2 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
    else
        prodstub_check_jobdata_3 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
    fi
fi

# Arm producer prod-a for supervision failure
prodstub_arm_producer 200 prod-a 400

# Wait for producer prod-a to go disabled
ics_api_edp_get_producer_status 200 prod-a DISABLED 360

if [[ "$ICS_FEATURE_LEVEL" == *"INFO-TYPES"* ]]; then
    ics_equal json:data-producer/v1/info-producers 0 1000
else
    ics_equal json:ei-producer/v1/eiproducers 0 1000
fi

if [[ "$ICS_FEATURE_LEVEL" == *"TYPE-SUBSCRIPTIONS"* ]]; then
    cr_equal 0 received_callbacks 3 30
    cr_api_check_all_ics_subscription_events 200 0 type-status1 type1 testdata/ics/ei-type-1.json REGISTERED
    cr_api_check_all_ics_events 200 0 job1-status DISABLED
    cr_api_check_all_ics_events 200 0 job2-status DISABLED
else
    cr_equal 0 received_callbacks 2 30
    cr_api_check_all_ics_events 200 0 job1-status DISABLED
    cr_api_check_all_ics_events 200 0 job2-status DISABLED
fi

cr_contains_str 0 remote_hosts $HTTP_PROXY_APP_NAME

check_policy_agent_logs
check_ics_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment