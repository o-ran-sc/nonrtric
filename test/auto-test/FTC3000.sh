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

TC_ONELINE_DESCR="App test DMAAP Meditor and DMAAP Adapter"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="ICS DMAAPMED DMAAPADP KUBEPROXY MR DMAAPMR CR KAFKAPC"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" ICS DMAAPMED DMAAPADP KUBEPROXY MR DMAAPMR CR KAFKAPC"

#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-G-RELEASE ORAN-H-RELEASE ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

NUM_CR=10 # Number of callback receivers, divide all callbacks to this number of servers - for load sharing
## Note: The number jobs must be a multiple of the number of CRs in order to calculate the number of expected event in each CR
NUM_JOBS=200  # Mediator and adapter gets same number of jobs for every type
if [ $NUM_JOBS -lt $NUM_CR ]; then
    __log_conf_fail_general "Number of jobs: $NUM_JOBS must be greater then the number of CRs: $NUM_CR"
fi

clean_environment

#use_cr_https
use_cr_http
use_ics_rest_https
use_mr_https
use_dmaapadp_https
use_dmaapmed_https

start_kube_proxy

start_cr $NUM_CR

start_ics NOPROXY $SIM_GROUP/$ICS_COMPOSE_DIR/$ICS_CONFIG_FILE

set_ics_trace

start_mr    "unauthenticated.dmaapmed.json" "/events" "dmaapmediatorproducer/STD_Fault_Messages" \
            "unauthenticated.dmaapadp.json" "/events" "dmaapadapterproducer/msgs"

start_kafkapc

kafkapc_api_create_topic 201 "unauthenticated.dmaapadp_kafka.text" "text/plain"

kafkapc_api_start_sending 200 "unauthenticated.dmaapadp_kafka.text"

start_dmaapadp NOPROXY $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_CONFIG_FILE_TEMPLATE $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_DATA_FILE

set_dmaapadp_trace

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    kafkapc_api_create_topic 201 "unauthenticated.dmaapmed_kafka.text" "text/plain"

    kafkapc_api_start_sending 200 "unauthenticated.dmaapmed_kafka.text"
fi

start_dmaapmed NOPROXY $SIM_GROUP/$DMAAP_MED_COMPOSE_DIR/$DMAAP_MED_HOST_DATA_FILE

ics_equal json:data-producer/v1/info-producers 2 60

# Check producers
ics_api_idc_get_job_ids 200 NOTYPE NOWNER EMPTY
if [[ "$DMAAP_ADP_FEATURE_LEVEL" == *"GENERATED_PROD_NAME"* ]]; then
    if [ $RUNMODE == "KUBE" ]; then
        __NAME="https:__$DMAAP_ADP_APP_NAME.$KUBE_NONRTRIC_NAMESPACE:$DMAAP_ADP_EXTERNAL_SECURE_PORT"
    else
        __NAME="https:__$DMAAP_ADP_APP_NAME:$DMAAP_ADP_INTERNAL_SECURE_PORT"
    fi
    ics_api_edp_get_producer_ids_2 200 NOTYPE $__NAME DMaaP_Mediator_Producer
else
    ics_api_edp_get_producer_ids_2 200 NOTYPE DmaapGenericInfoProducer DMaaP_Mediator_Producer
fi

if [[ "$DMAAP_MED_FEATURE_LEVEL" != *"KAFKATYPES"* ]]; then
    ics_api_idc_get_type_ids 200 ExampleInformationType STD_Fault_Messages ExampleInformationTypeKafka
else
    ics_api_idc_get_type_ids 200 ExampleInformationType STD_Fault_Messages ExampleInformationTypeKafka Kafka_TestTopic
fi


# Create jobs for adapter - CR stores data as MD5 hash
start_timer "Create adapter jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    service_mr="CR_SERVICE_MR_PATH_"$cr_index
    service_app="CR_SERVICE_APP_PATH_"$cr_index
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"FILTERSCHEMA"* ]]; then
        ics_api_idc_put_job 201 job-adp-$i ExampleInformationType ${!service_mr}/job-adp-data$i"?storeas=md5" info-owner-adp-$i ${!service_app}/job_status_info-owner-adp-$i testdata/dmaap-adapter/job-template1.1.json
    else
        ics_api_idc_put_job 201 job-adp-$i ExampleInformationType ${!service_mr}/job-adp-data$i"?storeas=md5" info-owner-adp-$i ${!service_app}/job_status_info-owner-adp-$i testdata/dmaap-adapter/job-template1.json
    fi

done
print_timer

# Create jobs for adapter kafka - CR stores data as MD5 hash
start_timer "Create adapter (kafka) jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    service_text="CR_SERVICE_TEXT_PATH_"$cr_index
    service_app="CR_SERVICE_APP_PATH_"$cr_index
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"FILTERSCHEMA"* ]]; then
        ics_api_idc_put_job 201 job-adp-kafka-$i ExampleInformationTypeKafka ${!service_text}/job-adp-kafka-data$i"?storeas=md5" info-owner-adp-kafka-$i ${!service_app}/job_status_info-owner-adp-kafka-$i testdata/dmaap-adapter/job-template-1.1-kafka.json
    else
        ics_api_idc_put_job 201 job-adp-kafka-$i ExampleInformationTypeKafka ${!service_text}/job-adp-kafka-data$i"?storeas=md5" info-owner-adp-kafka-$i ${!service_app}/job_status_info-owner-adp-kafka-$i testdata/dmaap-adapter/job-template-1-kafka.json
    fi
done
print_timer

# Create jobs for mediator - CR stores data as MD5 hash
start_timer "Create mediator jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    service_mr="CR_SERVICE_MR_PATH_"$cr_index
    service_app="CR_SERVICE_APP_PATH_"$cr_index
    ics_api_idc_put_job 201 job-med-$i STD_Fault_Messages ${!service_mr}/job-med-data$i"?storeas=md5" info-owner-med-$i ${!service_app}/job_status_info-owner-med-$i testdata/dmaap-mediator/job-template.json
done
print_timer

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    # Create jobs for mediator kafka - CR stores data as MD5 hash
    start_timer "Create mediator (kafka) jobs: $NUM_JOBS"
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        cr_index=$(($i%$NUM_CR))
        service_text="CR_SERVICE_TEXT_PATH_"$cr_index
        service_app="CR_SERVICE_APP_PATH_"$cr_index
        if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"FILTERSCHEMA"* ]]; then
            ics_api_idc_put_job 201 job-med-kafka-$i Kafka_TestTopic ${!service_text}/job-med-kafka-data$i"?storeas=md5" info-owner-med-kafka-$i ${!service_app}/job_status_info-owner-med-kafka-$i testdata/dmaap-mediator/job-template-1.1-kafka.json
        else
            ics_api_idc_put_job 201 job-med-kafka-$i Kafka_TestTopic ${!service_text}/job-med-kafka-data$i"?storeas=md5" info-owner-med-kafka-$i ${!service_app}/job_status_info-owner-med-kafka-$i testdata/dmaap-mediator/job-template-1-kafka.json
        fi
    done
    print_timer
fi

# Check job status
for ((i=1; i<=$NUM_JOBS; i++))
do
    ics_api_a1_get_job_status 200 job-med-$i ENABLED 30
    ics_api_a1_get_job_status 200 job-adp-$i ENABLED 30
    ics_api_a1_get_job_status 200 job-adp-kafka-$i ENABLED 30
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
        ics_api_a1_get_job_status 200 job-med-kafka-$i ENABLED 30
    fi
done


EXPECTED_DATA_DELIV=0 #Total delivered msg per CR
EXPECTED_BATCHES_DELIV=0 #Total delivered batches per CR
DATA_DELIV_JOBS=0 #Total delivered msg per job per CR

mr_api_generate_json_payload_file 1 ./tmp/data_for_dmaap_test.json
kafkapc_api_generate_text_payload_file 1 ./tmp/data_for_dmaap_test.txt

## Send json file via message-router to adapter
DATA_DELIV_JOBS=5 #Each job will eventuall get 2 msgs
EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapadp.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

# Check received data callbacks from adapter
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-data$i ./tmp/data_for_dmaap_test.json
done


## Send text file via message-router to adapter kafka

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 1 30
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 2 30
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 3 30
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 4 30
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 5 30
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

# Check received data callbacks from adapter kafka
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-adp-kafka-data$i ./tmp/data_for_dmaap_test.txt
done

## Send json file via message-router to mediator

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
mr_api_send_json_file "/events/unauthenticated.dmaapmed.json" ./tmp/data_for_dmaap_test.json
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
done

# Check received data callbacks from mediator
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-data$i ./tmp/data_for_dmaap_test.json
    cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-data$i ./tmp/data_for_dmaap_test.json
done

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    ## Send text file via message-router to mediator kafka

    EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
    EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
    kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 1 30
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
        cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
    done

    EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
    EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
    kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 2 30
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
        cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
    done

    EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
    EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
    kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 3 30
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
        cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
    done

    EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
    EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
    kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 4 30
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
        cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
    done

    EXPECTED_DATA_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_DATA_DELIV))
    EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))
    kafkapc_api_post_msg_from_file 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" ./tmp/data_for_dmaap_test.txt
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 5 30
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
        cr_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV 60
    done

    # Check received data callbacks from adapter kafka
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        cr_index=$(($i%$NUM_CR))
        cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-kafka-data$i ./tmp/data_for_dmaap_test.txt
        cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-kafka-data$i ./tmp/data_for_dmaap_test.txt
        cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-kafka-data$i ./tmp/data_for_dmaap_test.txt
        cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-kafka-data$i ./tmp/data_for_dmaap_test.txt
        cr_api_check_single_generic_event_md5_file 200 $cr_index job-med-kafka-data$i ./tmp/data_for_dmaap_test.txt
    done
fi

# Send small json via message-router to adapter
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-1"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-3"}'

#########################################################
#Checking number of message batches is not reliable when
#sending several messages at the same time
#Two messages may be send separately or to together
#in one batch
#########################################################

DATA_DELIV_JOBS=7 #Each job will eventually get 5+2 msgs

# Wait for data reception, adapter
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter, 2 json per job"
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
done
print_timer

# Send small text via message-router to adapter
kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" 'Message-------1'
kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" 'Message-------3'
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 7 30

# Wait for data reception, adapter kafka
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter kafka, 2 strings per job"
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
done
print_timer

# Send small json via message-router to mediator
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-0"}'
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-2"}'

# Wait for data reception, mediator
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
start_timer "Data delivery mediator, 2 json per job"
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 100
done
print_timer

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    # Send small text via message-router to mediator
    kafkapc_api_post_msg 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" 'Message-------0'
    kafkapc_api_post_msg 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" 'Message-------2'
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 7 30

    # Wait for data reception, adapter kafka
    EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
    start_timer "Data delivery mediator kafka, 2 strings per job"
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 60
    done
    print_timer
fi

# Check received number of messages for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_equal $cr_index received_callbacks?id=job-med-data$i $DATA_DELIV_JOBS
    cr_equal $cr_index received_callbacks?id=job-adp-data$i $DATA_DELIV_JOBS
    cr_equal $cr_index received_callbacks?id=job-adp-kafka-data$i $DATA_DELIV_JOBS
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
        cr_equal $cr_index received_callbacks?id=job-med-kafka-data$i $DATA_DELIV_JOBS
    fi
done

# Check received data and order for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_api_check_single_generic_event_md5 200 $cr_index job-med-data$i '{"msg":"msg-0"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-med-data$i '{"msg":"msg-2"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-data$i '{"msg":"msg-1"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-data$i '{"msg":"msg-3"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-kafka-data$i 'Message-------1'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-kafka-data$i 'Message-------3'
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
        cr_api_check_single_generic_event_md5 200 $cr_index job-med-kafka-data$i 'Message-------0'
        cr_api_check_single_generic_event_md5 200 $cr_index job-med-kafka-data$i 'Message-------2'
    fi
done

# Set delay in the callback receiver to slow down callbacks
SEC_DELAY=2
for ((i=0; i<$NUM_CR; i++))
do
    cr_delay_callback 200 $i $SEC_DELAY
done

# Send small json via message-router to adapter
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-5"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-7"}'

# Wait for data reception, adapter
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter with $SEC_DELAY seconds delay in consumer, 2 json per job"
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 100
done
print_timer


# Send small text via message-router to adapter kafka
kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" 'Message-------5'
kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text" "text/plain" 'Message-------7'
kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text/counters/sent 9 30

# Wait for data reception, adapter kafka
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
start_timer "Data delivery adapter kafka with $SEC_DELAY seconds delay in consumer, 2 strings per job"
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 100
done
print_timer


# Send small json via message-router to mediator
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-4"}'
mr_api_send_json "/events/unauthenticated.dmaapmed.json" '{"msg":"msg-6"}'

# Wait for data reception, mediator
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
start_timer "Data delivery mediator with $SEC_DELAY seconds delay in consumer, 2 json per job"
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 100
done
print_timer

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    # Send small text via message-router to mediator kafka
    kafkapc_api_post_msg 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" 'Message-------4'
    kafkapc_api_post_msg 200 "unauthenticated.dmaapmed_kafka.text" "text/plain" 'Message-------6'
    kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text/counters/sent 9 30

    # Wait for data reception, mediator kafka
    EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
    start_timer "Data delivery mediator kafka with $SEC_DELAY seconds delay in consumer, 2 strings per job"
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV 100
    done
    print_timer
fi

# Check received number of messages for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_equal $cr_index received_callbacks?id=job-med-data$i 9
    cr_equal $cr_index received_callbacks?id=job-adp-data$i 9
    cr_equal $cr_index received_callbacks?id=job-adp-kafka-data$i 9
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
        cr_equal $cr_index received_callbacks?id=job-med-kafka-data$i 9
    fi
done

# Check received data and order for mediator and adapter callbacks
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_api_check_single_generic_event_md5 200 $cr_index job-med-data$i '{"msg":"msg-4"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-med-data$i '{"msg":"msg-6"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-data$i '{"msg":"msg-5"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-data$i '{"msg":"msg-7"}'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-kafka-data$i 'Message-------5'
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-kafka-data$i 'Message-------7'
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
        cr_api_check_single_generic_event_md5 200 $cr_index job-med-kafka-data$i 'Message-------4'
        cr_api_check_single_generic_event_md5 200 $cr_index job-med-kafka-data$i 'Message-------6'
    fi
done

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment
