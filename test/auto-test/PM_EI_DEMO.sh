#!/usr/bin/env bash

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

TC_ONELINE_DESCR="Preparation demo setup  - policy management and information information"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC ICS PRODSTUB RC HTTPPROXY KUBEPROXY NGW"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" MR CR A1PMS RC PRODSTUB RICSIM CP ICS SDNC HTTPPROXY KUBEPROXY NGW"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

use_cr_https
use_a1pms_rest_https
if [[ "$SDNC_FEATURE_LEVEL" == *"NO_NB_HTTPS"* ]]; then
    deviation "SDNC does not support NB https"
    use_sdnc_http
else
    use_sdnc_https
fi
use_simulator_https
use_ics_rest_https
use_prod_stub_https
use_rapp_catalogue_https


notificationurl=$CR_SERVICE_APP_PATH_0"/test"

clean_environment

start_kube_proxy

STD_NUM_RICS=2

#start_http_proxy  #Remove the comment on this line, comment out 'start_sdnc' and change SDNC to NOSDNC a few lines below to run with proxy

start_ric_simulators $RIC_SIM_PREFIX"_g3" $STD_NUM_RICS STD_2.0.0

if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
    :
else
    start_mr #Just to prevent errors in the a1pms log...
fi

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_sdnc    # Comment this line to run A1PMS with proxy
controller_api_wait_for_status_ok 200 ricsim_g3_1 # Comment this line to run A1PMS with proxy

start_a1pms PROXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE



prepare_a1pms_config      SDNC  ".a1pms_config.json"  #Change to NOSDNC if running A1PMS with  proxy

if [ $RUNMODE == "KUBE" ]; then
    a1pms_load_config                       ".a1pms_config.json"
else
    a1pms_api_put_configuration 200 ".a1pms_config.json"
fi

start_cr 1

start_prod_stub

start_ics PROXY $SIM_GROUP/$ICS_COMPOSE_DIR/$ICS_CONFIG_FILE

start_rapp_catalogue

set_a1pms_trace

set_ics_trace

rapp_cat_api_get_services 200 EMPTY

rapp_cat_api_put_service 201 "Emergency-response-app" v1 "Emergency-response-app" "Emergency-response-app"

rapp_cat_api_get_services 200 "Emergency-response-app" v1 "Emergency-response-app" "Emergency-response-app"

sleep_wait 120 "Let A1PMS configuration take effect"

a1pms_api_get_status 200

# Print the A1 version for STD 2.X
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_print $RIC_SIM_PREFIX"_g3_"$i interface
done
# Load the policytypes in std
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 demo-testdata/STD2/sim_qos.json
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS2_0.1.0 demo-testdata/STD2/sim_qos2.json
done

#Check the number of schemas and the individual schemas in STD
a1pms_equal json:policy-types 2 120

for ((i=1; i<=$STD_NUM_RICS; i++))
do
    a1pms_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g3_"$i 2 120
done

# Check the schemas in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    a1pms_api_get_policy_type 200 STD_QOS_0_2_0 demo-testdata/STD2/qos-a1pms-modified.json
    a1pms_api_get_policy_type 200 'STD_QOS2_0.1.0' demo-testdata/STD2/qos2-a1pms-modified.json
done

#Check the number of types
a1pms_equal json:policy-types 2 300

a1pms_api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_APP_PATH_0/1"

# Create policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 $((2300+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i 'STD_QOS2_0.1.0' $((2400+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
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
done

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

STATUS1="$CR_SERVICE_APP_PATH_0/callbacks/job1-status"
STATUS2="$CR_SERVICE_APP_PATH_0/callbacks/job2-status"

prodstub_arm_producer 200 prod-a
prodstub_arm_type 200 prod-a type1
prodstub_arm_job_create 200 prod-a job1
prodstub_arm_job_create 200 prod-a job2

### ics status
ics_api_service_status 200

## Setup prod-a
ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json

ics_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

ics_api_edp_get_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1

ics_api_edp_get_producer_status 200 prod-a ENABLED


## Create a job for prod-a
## job1 - prod-a
ics_api_a1_put_job 201 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ics/job-template.json

# Check the job data in the producer
 prodstub_check_jobdata_3 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json

## Create a second job for prod-a
## job2 - prod-a
ics_api_a1_put_job 201 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ics/job-template.json

# Check the job data in the producer
prodstub_check_jobdata_3 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json

check_a1pms_logs
check_ics_logs
check_sdnc_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment