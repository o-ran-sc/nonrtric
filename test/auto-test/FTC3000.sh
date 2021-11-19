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

TC_ONELINE_DESCR="App test DMAAP Meditor and DMAAP Adapter"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="ECS DMAAPMED DMAAPADP KUBEPROXY MR DMAAPMR CR"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" ECS DMAAPMED DMAAPADP KUBEPROXY MR DMAAPMR CR"

#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-E-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

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
. ../common/consul_cbs_functions.sh
. ../common/http_proxy_api_functions.sh
. ../common/kube_proxy_api_functions.sh
. ../common/gateway_api_functions.sh
. ../common/dmaapmed_api_functions.sh
. ../common/dmaapadp_api_functions.sh

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################
FLAT_A1_EI="1"
NUM_JOBS=100  # Mediator and adapter gets same number of jobs

clean_environment

#use_cr_https
use_cr_http
use_ecs_rest_https
use_mr_https
use_dmaapadp_https
use_dmaapmed_https

start_kube_proxy

start_cr

start_ecs NOPROXY $SIM_GROUP/$ECS_COMPOSE_DIR/$ECS_CONFIG_FILE

set_ecs_trace

start_mr    "unauthenticated.dmaapmed.json" "/events" "dmaapmediatorproducer/STD_Fault_Messages" \
            "unauthenticated.dmaapadp.json" "/events" "dmaapadapterproducer/msgs" \
            "unauthenticated.dmaapadp_kafka.text" "/events" "dmaapadapterproducer/msgs"

start_dmaapadp NOPROXY $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_CONFIG_FILE $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_DATA_FILE

set_dmaapadp_trace

start_dmaapmed NOPROXY $SIM_GROUP/$DMAAP_MED_COMPOSE_DIR/$DMAAP_MED_DATA_FILE

ecs_equal json:data-producer/v1/info-producers 2 60

# Check producers
ecs_api_idc_get_job_ids 200 NOTYPE NOWNER EMPTY
ecs_api_idc_get_type_ids 200 ExampleInformationType STD_Fault_Messages ExampleInformationTypeKafka
ecs_api_edp_get_producer_ids_2 200 NOTYPE DmaapGenericInfoProducer DMaaP_Mediator_Producer


# Create jobs for adapter - CR stores data as MD5 hash
start_timer "Create adapter jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    ecs_api_idc_put_job 201 job-adp-$i ExampleInformationType $CR_SERVICE_MR_PATH/job-adp-data$i"?storeas=md5" info-owner-adp-$i $CR_SERVICE_APP_PATH/job_status_info-owner-adp-$i testdata/dmaap-adapter/job-template.json

done
print_timer "Create adapter jobs: $NUM_JOBS"

# Create jobs for adapter kafka - CR stores data as MD5 hash
start_timer "Create adapter (kafka) jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    ecs_api_idc_put_job 201 job-adp-kafka-$i ExampleInformationTypeKafka $CR_SERVICE_TEXT_PATH/job-adp-kafka-data$i"?storeas=md5" info-owner-adp-kafka-$i $CR_SERVICE_APP_PATH/job_status_info-owner-adp-kafka-$i testdata/dmaap-adapter/job-template-1-kafka.json

done
print_timer "Create adapter (kafka) jobs: $NUM_JOBS"

# Create jobs for mediator - CR stores data as MD5 hash
start_timer "Create mediator jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    ecs_api_idc_put_job 201 job-med-$i STD_Fault_Messages $CR_SERVICE_MR_PATH/job-med-data$i"?storeas=md5" info-owner-med-$i $CR_SERVICE_APP_PATH/job_status_info-owner-med-$i testdata/dmaap-adapter/job-template.json
done
print_timer "Create mediator jobs: $NUM_JOBS"

# Check job status
for ((i=1; i<=$NUM_JOBS; i++))
do
    ecs_api_a1_get_job_status 200 job-med-$i ENABLED 30
    ecs_api_a1_get_job_status 200 job-adp-$i ENABLED 30
    ecs_api_a1_get_job_status 200 job-adp-kafka-$i ENABLED 30
done


EXPECTED_DATA_DELIV=0

mr_api_generate_json_payload_file 1 ./tmp/data_for_dmaap_test.json
mr_api_generate_text_payload_file 1 ./tmp/data_for_dmaap_test.txt

## Send json file via message-router to adapter

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))

mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

# Check received data callbacks from adapter
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_event_md5_file 200 job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-adp-data$i ./tmp/data_for_dmaap_test.json
done


## Send text file via message-router to adapter kafka

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))

mr_api_send_text_file "/events/unauthenticated.dmaapadp_kafka.text" ./tmp/data_for_dmaap_test.txt
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_text_file "/events/unauthenticated.dmaapadp_kafka.text" ./tmp/data_for_dmaap_test.txt
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_text_file "/events/unauthenticated.dmaapadp_kafka.text" ./tmp/data_for_dmaap_test.txt
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_text_file "/events/unauthenticated.dmaapadp_kafka.text" ./tmp/data_for_dmaap_test.txt
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_text_file "/events/unauthenticated.dmaapadp_kafka.text" ./tmp/data_for_dmaap_test.txt
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

# Check received data callbacks from adapter kafka
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_event_md5_file 200 job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_genric_event_md5_file 200 job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_genric_event_md5_file 200 job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_genric_event_md5_file 200 job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_genric_event_md5_file 200 job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
done

## Send json file via message-router to mediator

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))

mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

EXPECTED_DATA_DELIV=$(($NUM_JOBS+$EXPECTED_DATA_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
cr_equal received_callbacks $EXPECTED_DATA_DELIV 200

# Check received data callbacks from mediator
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_event_md5_file 200 job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_genric_event_md5_file 200 job-med-data$i ./tmp/data_for_dmaap_test.json
done


# Send small json via message-router to adapter
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-1"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-3"}'

# Wait for data recetption, adapter
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter, 2 json per job"
cr_equal received_callbacks $EXPECTED_DATA_DELIV 100
print_timer "Data delivery adapter, 2 json per job"

# Send small text via message-routere to adapter
mr_api_send_text "/events/unauthenticated.dmaapadp_kafka.text" 'Message-------1'
mr_api_send_text "/events/unauthenticated.dmaapadp_kafka.text" 'Message-------3'

# Wait for data recetption, adapter kafka
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapte kafkar, 2 strings per job"
cr_equal received_callbacks $EXPECTED_DATA_DELIV 100
print_timer "Data delivery adapte kafkar, 2 strings per job"

# Send small json via message-router to mediator
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-0"}'
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-2"}'

# Wait for data reception, mediator
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2+$EXPECTED_DATA_DELIV))
start_timer "Data delivery mediator, 2 json per job"
cr_equal received_callbacks $EXPECTED_DATA_DELIV 100
print_timer "Data delivery mediator, 2 json per job"

# Check received number of messages for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_equal received_callbacks?id=job-med-data$i 7
    cr_equal received_callbacks?id=job-adp-data$i 7
    cr_equal received_callbacks?id=job-adp-kafka-data$i 7
done

# Check received data and order for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_event_md5 200 job-med-data$i '{"msg":"msg-0"}'
    cr_api_check_single_genric_event_md5 200 job-med-data$i '{"msg":"msg-2"}'
    cr_api_check_single_genric_event_md5 200 job-adp-data$i '{"msg":"msg-1"}'
    cr_api_check_single_genric_event_md5 200 job-adp-data$i '{"msg":"msg-3"}'
    cr_api_check_single_genric_event_md5 200 job-adp-kafka-data$i 'Message-------1'
    cr_api_check_single_genric_event_md5 200 job-adp-kafka-data$i 'Message-------3'
done

# Set delay in the callback receiver to slow down callbacks
SEC_DELAY=2
cr_delay_callback 200 $SEC_DELAY

# Send small json via message-router to adapter
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-5"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-7"}'

# Wait for data recetption, adapter
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter with $SEC_DELAY seconds delay in consumer, 2 json per job"
cr_equal received_callbacks $EXPECTED_DATA_DELIV $(($NUM_JOBS+300))
print_timer "Data delivery adapter with $SEC_DELAY seconds delay in consumer, 2 json per job"


# Send small text via message-router to adapter kafka
mr_api_send_text "/events/unauthenticated.dmaapadp_kafka.text" 'Message-------5'
mr_api_send_text "/events/unauthenticated.dmaapadp_kafka.text" 'Message-------7'

# Wait for data recetption, adapter kafka
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter kafka with $SEC_DELAY seconds delay in consumer, 2 strings per job"
cr_equal received_callbacks $EXPECTED_DATA_DELIV $(($NUM_JOBS+300))
print_timer "Data delivery adapter with kafka $SEC_DELAY seconds delay in consumer, 2 strings per job"


# Send small json via message-router to mediator
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-4"}'
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-6"}'

# Wait for data reception, mediator
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2+$EXPECTED_DATA_DELIV))
start_timer "Data delivery mediator with $SEC_DELAY seconds delay in consumer, 2 json per job"
cr_equal received_callbacks $EXPECTED_DATA_DELIV 1000
print_timer "Data delivery mediator with $SEC_DELAY seconds delay in consumer, 2 json per job"

# Check received number of messages for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_equal received_callbacks?id=job-med-data$i 9
    cr_equal received_callbacks?id=job-adp-data$i 9
    cr_equal received_callbacks?id=job-adp-kafka-data$i 9
done

# Check received data and order for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_event_md5 200 job-med-data$i '{"msg":"msg-4"}'
    cr_api_check_single_genric_event_md5 200 job-med-data$i '{"msg":"msg-6"}'
    cr_api_check_single_genric_event_md5 200 job-adp-data$i '{"msg":"msg-5"}'
    cr_api_check_single_genric_event_md5 200 job-adp-data$i '{"msg":"msg-7"}'
    cr_api_check_single_genric_event_md5 200 job-adp-kafka-data$i 'Message-------5'
    cr_api_check_single_genric_event_md5 200 job-adp-kafka-data$i 'Message-------7'
done

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment
