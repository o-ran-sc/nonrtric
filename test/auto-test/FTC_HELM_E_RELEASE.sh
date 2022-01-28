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

TC_ONELINE_DESCR="Sanity test of Non-RT RIC Helm chats - all components - E-RELEASE"
# This script requires the helm charts for nonrtric, a1simulator and a1controller are installed
# There should be 2 simulator of each A1 interface version started

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="" # Not used -  KUBE only test script

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" MR DMAAPMR CR  PRODSTUB KUBEPROXY KAFKAPC"
#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=" PA RICSIM CP ICS RC SDNC DMAAPMED DMAAPADP"

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-E-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

use_mr_https
use_cr_https
use_agent_rest_https
use_sdnc_https
use_simulator_https
use_ics_rest_https
use_prod_stub_https
use_dmaapmed_https

if [ $ICS_VERSION == "V1-1" ]; then
    use_rapp_catalogue_http # https not yet supported
else
    ########################################use_rapp_catalogue_https
    use_rapp_catalogue_http
fi

echo -e "$RED CHECK WHY RC HTTPS DOES NOT WORK $ERED"

###############################use_control_panel_https
use_control_panel_http

if [ "$PMS_VERSION" == "V1" ]; then
   echo "PMS VERSION 2 (V2) is required"
   exit 1
fi

clean_environment

ics_kube_pvc_reset

pms_kube_pvc_reset

start_kube_proxy

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

start_mr    "$MR_READ_TOPIC"  "/events" "users/policy-agent" \
            "$MR_WRITE_TOPIC" "/events" "users/mr-stub" \
            "unauthenticated.dmaapmed.json" "/events" "dmaapmediatorproducer/STD_Fault_Messages" \
            "unauthenticated.dmaapadp.json" "/events" "dmaapadapterproducer/msgs"

start_kafkapc

kafkapc_api_create_topic 201 "unauthenticated.dmaapadp_kafka.text" "text/plain"

kafkapc_api_start_sending 200 "unauthenticated.dmaapadp_kafka.text"

start_control_panel

start_sdnc

start_policy_agent

start_cr 1

start_prod_stub

start_ics NOPROXY

set_ics_trace

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

# Check the number of policies in STD and STD2
for ((i=0; i<$STD_NUM_RICS; i++))
do
    sim_equal "a1-sim-std-"$i num_instances 0
    sim_equal "a1-sim-std2-"$i num_instances 0
done

# Check the number of policies in OSC
for ((i=0; i<$STD_NUM_RICS; i++))
do
    sim_equal "a1-sim-osc-"$i num_instances 0
done

#Check the number of schemas
api_equal json:policy-types 1

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

if [ "$PMS_VERSION" == "V2" ]; then

    api_equal json:policy-types 5 120

    api_equal json:policies 0

    api_equal json:policy-instances 0
else

    api_equal json:policy_schemas 5 120

    api_equal json:policy_types 5

    api_equal json:policies 0

    api_equal json:policy_ids 0
fi

api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_APP_PATH_0/ER-app"

# Create policies in STD
for ((i=0; i<$STD_NUM_RICS; i++))
do
    ricid=$((3+$i))
    generate_policy_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid NOTYPE $((1100+$i)) NOTRANSIENT $CR_SERVICE_APP_PATH_0/"std2" testdata/STD/pi1_template.json 1
    generate_policy_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid NOTYPE $((1200+$i)) NOTRANSIENT $CR_SERVICE_APP_PATH_0/"std2" testdata/STD/pi1_template.json 1
done

#Create policies in STD 2
for ((i=0; i<$STD_NUM_RICS; i++))
do
   ricid=$((5+$i))
   generate_policy_uuid
   api_put_policy 201 "Emergency-response-app" ric$ricid STD_QOS_0_2_0 $((2100+$i)) NOTRANSIENT $CR_SERVICE_APP_PATH_0/"std2" testdata/STD2/pi_qos_template.json 1
   generate_policy_uuid
   api_put_policy 201 "Emergency-response-app" ric$ricid STD_QOS2_0.1.0 $((2200+$i)) NOTRANSIENT $CR_SERVICE_APP_PATH_0/"std2" testdata/STD2/pi_qos2_template.json 1
done

# Create policies in OSC
for ((i=0; i<$OSC_NUM_RICS; i++))
do
    ricid=$((1+$i))
    generate_policy_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid 1 $((3100+$i)) NOTRANSIENT $CR_SERVICE_APP_PATH_0/"osc" testdata/OSC/pi1_template.json 1
    generate_policy_uuid
    api_put_policy 201 "Emergency-response-app" ric$ricid 2 $((3200+$i)) NOTRANSIENT $CR_SERVICE_APP_PATH_0/"osc" testdata/OSC/pi2_template.json 1
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

stop_policy_agent

start_stopped_policy_agent

# Check PMS state after restart

sleep_wait 200

if [ "$PMS_VERSION" == "V2" ]; then

    api_equal json:policy-types 5 120

    api_equal json:policies 12

    api_equal json:policy-instances 12
else

    api_equal json:policy_schemas 5 120

    api_equal json:policy_types 5

    api_equal json:policies 12

    api_equal json:policy_ids 12
fi

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

cr_api_reset 0   # Reset CR to count new events

echo "ADD EVENT/STATUS CHECK"
echo "ADD MR CHECK"

FLAT_A1_EI="1"

ics_api_admin_reset

CB_JOB="$PROD_STUB_SERVICE_PATH$PROD_STUB_JOB_CALLBACK"
CB_SV="$PROD_STUB_SERVICE_PATH$PROD_STUB_SUPERVISION_CALLBACK"
TARGET1="$RIC_SIM_HTTPX://a1-sim-std2-0.a1-sim:$RIC_SIM_PORT/datadelivery"
TARGET2="$RIC_SIM_HTTPX://a1-sim-std2-1.a1-sim:$RIC_SIM_PORT/datadelivery"

STATUS1="$CR_SERVICE_APP_PATH_0/job1-status"
STATUS2="$CR_SERVICE_APP_PATH_0/job2-status"

prodstub_arm_producer 200 prod-a
prodstub_arm_type 200 prod-a type1
prodstub_arm_job_create 200 prod-a job1
prodstub_arm_job_create 200 prod-a job2


### ics status
ics_api_service_status 200

## Setup prod-a
if [ $ICS_VERSION == "V1-1" ]; then
    ics_api_edp_put_producer 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ics/ei-type-1.json

    ics_api_edp_get_producer 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1 testdata/ics/ei-type-1.json
else
    ics_api_edp_put_type_2 201 type1 testdata/ics/ei-type-1.json
    ics_api_edp_get_type_2 200 type1

    ics_api_edp_get_type_ids 200 type1

    ics_api_edp_put_producer_2 201 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1
    ics_api_edp_put_producer_2 200 prod-a $CB_JOB/prod-a $CB_SV/prod-a type1
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
    if [[ "$ICS_FEATURE3LEVEL" == *"INFO-TYPES"* ]]; then
        prodstub_check_jobdata_3 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json
    else
        prodstub_check_jobdata_2 200 prod-a job1 type1 $TARGET1 ricsim_g3_1 testdata/ics/job-template.json
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
    if [[ "$ICS_FEATURE_LEVEL" == *"INFO-TYPES"* ]]; then
        prodstub_check_jobdata_3 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
    else
        prodstub_check_jobdata_2 200 prod-a job2 type1 $TARGET2 ricsim_g3_2 testdata/ics/job-template.json
    fi
fi

# Dmaap mediator and adapter
start_dmaapadp NOPROXY $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_CONFIG_FILE $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_DATA_FILE

start_dmaapmed NOPROXY $SIM_GROUP/$DMAAP_MED_COMPOSE_DIR/$DMAAP_MED_HOST_DATA_FILE

ics_equal json:data-producer/v1/info-producers 3 120

ics_equal json:data-producer/v1/info-types 4 30

ics_api_idc_get_type_ids 200 ExampleInformationType ExampleInformationTypeKafka STD_Fault_Messages type1

ics_api_edp_get_producer_ids_2 200 NOTYPE prod-a DmaapGenericInfoProducer DMaaP_Mediator_Producer

NUM_JOBS=1

for ((i=1; i<=$NUM_JOBS; i++))
do
    ics_api_idc_put_job 201 jobx$i STD_Fault_Messages $CR_SERVICE_MR_PATH_0/jobx-data$i info-ownerx$i $CR_SERVICE_MR_PATH_0/job_status_info-ownerx$i testdata/dmaap-adapter/job-template.json
done

for ((i=1; i<=$NUM_JOBS; i++))
do
    ics_api_idc_put_job 201 joby$i ExampleInformationType $CR_SERVICE_MR_PATH_0/joby-data$i info-ownery$i $CR_SERVICE_MR_PATH_0/job_status_info-ownery$i testdata/dmaap-adapter/job-template.json
    ics_api_idc_put_job 201 jobz$i ExampleInformationTypeKafka $CR_SERVICE_MR_PATH_0/jobz-data$i info-ownerz$i $CR_SERVICE_MR_PATH_0/job_status_info-ownerz$i testdata/dmaap-adapter/job-template-1-kafka.json
done

for ((i=1; i<=$NUM_JOBS; i++))
do
    ics_api_a1_get_job_status 200 jobx$i ENABLED 30
    ics_api_a1_get_job_status 200 joby$i ENABLED 30
    ics_api_a1_get_job_status 200 jobz$i ENABLED 30
done

sleep_wait 30 # Wait for mediator to listening to kafka

mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-0"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-1"}'
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-2"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-3"}'
kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" 'Message-------4'
kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" 'Message-------6'


cr_equal 0 received_callbacks $(($NUM_JOBS*2*3)) 200
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_equal 0 received_callbacks?id=jobx-data$i 2
    cr_equal 0 received_callbacks?id=joby-data$i 2
    cr_equal 0 received_callbacks?id=jobz-data$i 2
done

for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_json_event 200 0 jobx-data$i '{"msg":"msg-0"}'
    cr_api_check_single_genric_json_event 200 0 jobx-data$i '{"msg":"msg-2"}'
    cr_api_check_single_genric_json_event 200 0 joby-data$i '{"msg":"msg-1"}'
    cr_api_check_single_genric_json_event 200 0 joby-data$i '{"msg":"msg-3"}'
    cr_api_check_single_genric_json_event 200 0 jobz-data$i 'Message-------4'
    cr_api_check_single_genric_json_event 200 0 jobz-data$i 'Message-------6'
done


stop_ics

start_stopped_ics

# Check ICS status after restart

if [  -z "$FLAT_A1_EI" ]; then
    ics_api_a1_get_job_status 200 type1 job1 DISABLED
    ics_api_a1_get_job_status 200 type1 job2 DISABLED
else
    ics_api_a1_get_job_status 200 job1 DISABLED
    ics_api_a1_get_job_status 200 job2 DISABLED
fi

check_policy_agent_logs
check_ics_logs
check_sdnc_logs

#### TEST COMPLETE ####

store_logs          END

print_result
