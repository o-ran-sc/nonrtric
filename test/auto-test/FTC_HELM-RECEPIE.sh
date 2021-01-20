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

TC_ONELINE_DESCR="Sanity test of Non-RT RIC Helm recepie - all components"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="" # Not used -  KUBE only test script

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" MR CR  PRODSTUB"
#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=" PA RICSIM CP ECS RC SDNC"

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-CHERRY ORAN-DAWN"
#Supported run modes
SUPPORTED_RUNMODES="KUBE"

. ../common/testcase_common.sh $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/ecs_api_functions.sh
. ../common/prodstub_api_functions.sh
. ../common/cr_api_functions.sh
. ../common/rapp_catalogue_api_functions.sh
. ../common/mr_api_functions.sh
. ../common/control_panel_api_functions.sh
. ../common/controller_api_functions.sh

#### TEST BEGIN ####

use_mr_http       #MR only supports http?
use_cr_https
use_agent_rest_https
use_sdnc_https
use_simulator_https
use_ecs_rest_https
use_prod_stub_https
if [ $ECS_VERSION == "V1-1" ]; then
    use_rapp_catalogue_http # https not yet supported
else
    use_rapp_catalogue_https
fi

echo -e "$RED CHECK WHY RC HTTPS DOES NOT WORK $ERED"

use_control_panel_https

if [ "$PMS_VERSION" == "V1" ]; then
   echo "PMS VERSION 2 (V2) is required"
   exit 1
fi

clean_environment

STD_NUM_RICS=2
OSC_NUM_RICS=2

start_ric_simulators a1-sim-osc $STD_NUM_RICS OSC_2.1.0
echo " RIC MAPPING a1-sim-osc-0 : ric1"
echo " RIC MAPPING a1-sim-osc-1 : ric2"

start_ric_simulators a1-sim-std $STD_NUM_RICS STD_1.1.3
echo " RIC MAPPING a1-sim-std-0 : ric3"
echo " RIC MAPPING a1-sim-std-1 : ric4"

start_ric_simulators a1-sim-std2 $STD_NUM_RICS STD_2.0.0
echo " RIC MAPPING a1-sim-std2-0 : ric5"
echo " RIC MAPPING a1-sim-std2-1 : ric6"

start_mr

start_control_panel

start_sdnc

start_policy_agent

start_cr

start_prod_stub

start_ecs NOPROXY

set_ecs_trace

start_rapp_catalogue

set_agent_trace

#### Test RAPP Catalogue ####

rapp_cat_api_get_services 200 EMPTY

rapp_cat_api_put_service 201 "Emergency-response-app" v1 "Emergency-response-app" "Emergency-response-app"

rapp_cat_api_get_services 200 "Emergency-response-app" v1 "Emergency-response-app" "Emergency-response-app"

#Check the number of services
rc_equal json:services 1

api_get_status 200

#### Test Policy Management Service ####

# Print the A1 version for STD 1.1.X
for ((i=0; i<$STD_NUM_RICS; i++))
do
    sim_print "a1-sim-std-"$i interface
done

# Print the A1 version for STD 2.0.X
for ((i=0; i<$STD_NUM_RICS; i++))
do
   sim_print "a1-sim-std2-"$i interface
done

# Print the A1 version for OSC 2.1.X
for ((i=0; i<$OSC_NUM_RICS; i++))
do
    sim_print "a1-sim-osc-"$i interface
done


# Load the polictypes in STD 2
for ((i=0; i<$STD_NUM_RICS; i++))
do
   sim_put_policy_type 201 "a1-sim-std2-"$i STD_QOS_0_2_0 testdata/STD2/sim_qos.json
   sim_put_policy_type 201 "a1-sim-std2-"$i STD_QOS2_0.1.0 testdata/STD2/sim_qos2.json
done

# Load the polictypes in OSC
for ((i=0; i<$OSC_NUM_RICS; i++))
do
    sim_put_policy_type 201 "a1-sim-osc-"$i 1 testdata/OSC/sim_1.json
    sim_put_policy_type 201 "a1-sim-osc-"$i 2 testdata/OSC/sim_2.json
done

# Check that all rics are synced in
api_equal json:rics 6 300

#Check the number of schemas and the individual schemas
api_equal json:policy-types 5 300

for ((i=0; i<$STD_NUM_RICS; i++))
do
    ricid=$((3+$i))
    api_equal json:policy-types?ric_id=ric$ricid 1 120
done

for ((i=0; i<$STD_NUM_RICS; i++))
do
   ricid=$((5+$i))
   api_equal json:policy-types?ric_id=ric$ricid 2 120
done

for ((i=0; i<$OSC_NUM_RICS; i++))
do
    ricid=$((1+$i))
    api_equal json:policy-types?ric_id=ric$ricid 2 120
done

#Check the schemas in STD 2
for ((i=0; i<$OSC_NUM_RICS; i++))
do
   ricid=$((5+$i))
   api_get_policy_type 200 STD_QOS_0_2_0 testdata/STD2/qos-agent-modified.json
   api_get_policy_type 200 STD_QOS2_0.1.0 testdata/STD2/qos2-agent-modified.json
done

# Check the schemas in OSC
for ((i=0; i<$OSC_NUM_RICS; i++))
do
    api_get_policy_type 200 1 testdata/OSC/1-agent-modified.json
    api_get_policy_type 200 2 testdata/OSC/2-agent-modified.json
done

api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_PATH/ER-app"

# Create policies in STD
for ((i=0; i<$STD_NUM_RICS; i++))
do
    ricid=$((3+$i))
    generate_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid NOTYPE $((1100+$i)) NOTRANSIENT $CR_SERVICE_PATH/"std2" testdata/STD/pi1_template.json 1
    generate_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid NOTYPE $((1200+$i)) NOTRANSIENT $CR_SERVICE_PATH/"std2" testdata/STD/pi1_template.json 1
done

#Create policies in STD 2
for ((i=0; i<$STD_NUM_RICS; i++))
do
   ricid=$((5+$i))
   generate_uuid
   api_put_policy 201 "Emergency-response-app" ric$ricid STD_QOS_0_2_0 $((2100+$i)) NOTRANSIENT $CR_SERVICE_PATH/"std2" testdata/STD2/pi_qos_template.json 1
   generate_uuid
   api_put_policy 201 "Emergency-response-app" ric$ricid STD_QOS2_0.1.0 $((2200+$i)) NOTRANSIENT $CR_SERVICE_PATH/"std2" testdata/STD2/pi_qos2_template.json 1
done

# Create policies in OSC
for ((i=0; i<$OSC_NUM_RICS; i++))
do
    ricid=$((1+$i))
    generate_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid 1 $((3100+$i)) NOTRANSIENT $CR_SERVICE_PATH/"osc" testdata/OSC/pi1_template.json 1
    generate_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid 2 $((3200+$i)) NOTRANSIENT $CR_SERVICE_PATH/"osc" testdata/OSC/pi2_template.json 1
done


# Check the number of policies in STD and STD2
for ((i=0; i<$STD_NUM_RICS; i++))
do
    sim_equal "a1-sim-std-"$i num_instances 2
    sim_equal "a1-sim-std2-"$i num_instances 2
done

# Check the number of policies in OSC
for ((i=0; i<$STD_NUM_RICS; i++))
do
    sim_equal "a1-sim-osc-"$i num_instances 2
done

echo "ADD EVENT/STATUS CHECK"
echo "ADD MR CHECK"

FLAT_A1_EI="1"

ecs_api_admin_reset

CB_JOB="$PROD_STUB_SERVICE_PATH$PROD_STUB_JOB_CALLBACK"
CB_SV="$PROD_STUB_SERVICE_PATH$PROD_STUB_SUPERVISION_CALLBACK"
TARGET1="$RIC_SIM_HTTPX://a1-sim-std2-0.a1-sim:$RIC_SIM_PORT/datadelivery"
TARGET2="$RIC_SIM_HTTPX://a1-sim-std2-1.a1-sim:$RIC_SIM_PORT/datadelivery"

STATUS1="$CR_SERVICE_PATH/job1-status"
STATUS2="$CR_SERVICE_PATH/job2-status"

prodstub_arm_producer 200 prod-a
prodstub_arm_type 200 prod-a type1
prodstub_arm_job_create 200 prod-a job1
prodstub_arm_job_create 200 prod-a job2


### ecs status
ecs_api_service_status 200

## Setup prod-a
ecs_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_edp_get_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_edp_get_producer_status 200 prod-a ENABLED


## Create a job for prod-a
## job1 - prod-a
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type1 job1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job1 type1 $TARGET1 ricsim_g3_1 $STATUS1 testdata/ecs/job-template.json
fi

# Check the job data in the producer
if [ $ECS_VERSION == "V1-1" ]; then
    prodstub_check_jobdata 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json
else
    prodstub_check_jobdata_2 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ecs/job-template.json
fi

## Create a second job for prod-a
## job2 - prod-a
if [  -z "$FLAT_A1_EI" ]; then
    ecs_api_a1_put_job 201 type1 job2 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json
else
    ecs_api_a1_put_job 201 job2 type1 $TARGET2 ricsim_g3_2 $STATUS2 testdata/ecs/job-template.json
fi

# Check the job data in the producer
if [ $ECS_VERSION == "V1-1" ]; then
    prodstub_check_jobdata 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json
else
    prodstub_check_jobdata_2 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ecs/job-template.json
fi

echo "ADD EVENT/STATUS CHECK"

check_policy_agent_logs
check_ecs_logs
check_sdnc_logs

#### TEST COMPLETE ####

store_logs          END

print_result
