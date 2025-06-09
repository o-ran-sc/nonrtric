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

TC_ONELINE_DESCR="App test DMAAP Meditor and DMAAP Adapter with 100 jobs,types and topics"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="ICS DMAAPMED DMAAPADP KUBEPROXY MR DMAAPMR CR KAFKAPC HTTPPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" ICS DMAAPMED DMAAPADP KUBEPROXY MR DMAAPMR CR KAFKAPC HTTPPROXY"

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

NUM_CR=1 # Number of callback receivers, max 1
## Note: The number jobs must be a multiple of the number of CRs in order to calculate the number of expected event in each CR
NUM_JOBS=100  # Mediator and adapter gets same number of jobs for every type
if [ $NUM_CR -gt 1 ]; then
    __log_conf_fail_general "Max number of callback receivers is one in this test"
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

start_mr

start_kafkapc

for ((i=1; i<=$NUM_JOBS; i++))
do
    kafkapc_api_create_topic 201 "unauthenticated.dmaapadp_kafka.text$i" "text/plain"

    kafkapc_api_start_sending 200 "unauthenticated.dmaapadp_kafka.text$i"
done

adp_med_type_list=""
adp_config_data='{"types": ['
for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $i -ne 1 ]; then
        adp_config_data=$adp_config_data','
    fi
    adp_config_data=$adp_config_data'{"id": "ADPKafkaType'$i'","kafkaInputTopic": "unauthenticated.dmaapadp_kafka.text'$i'","useHttpProxy": false}'
    adp_med_type_list="$adp_med_type_list ADPKafkaType$i "
done
adp_config_data=$adp_config_data']}'
echo $adp_config_data > tmp/adp_config_data.json

start_dmaapadp NOPROXY $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_CONFIG_FILE_TEMPLATE tmp/adp_config_data.json

set_dmaapadp_trace

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        kafkapc_api_create_topic 201 "unauthenticated.dmaapmed_kafka.text$i" "text/plain"

        kafkapc_api_start_sending 200 "unauthenticated.dmaapmed_kafka.text$i"
    done
fi

med_config_data='{"types": ['
for ((i=1; i<=$NUM_JOBS; i++))
do
    if [ $i -ne 1 ]; then
        med_config_data=$med_config_data','
    fi
    med_config_data=$med_config_data'{"id": "MEDKafkaType'$i'","kafkaInputTopic": "unauthenticated.dmaapmed_kafka.text'$i'"}'
    adp_med_type_list="$adp_med_type_list MEDKafkaType$i "
done
med_config_data=$med_config_data']}'
echo $med_config_data > tmp/med_config_data.json

start_dmaapmed NOPROXY tmp/med_config_data.json

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

ics_api_idc_get_type_ids 200 $adp_med_type_list


# Create jobs for adapter kafka - CR stores data as MD5 hash
start_timer "Create adapter (kafka) jobs: $NUM_JOBS"
for ((i=1; i<=$NUM_JOBS; i++))
do
    # Max buffer timeout for is about 160 sec for Adapter jobs"
    adp_timeout=$(($i*1000))
    if [[ "$DMAAP_ADP_FEATURE_LEVEL" == *"FILTERSPEC"* ]]; then
        deviation "It is possible to give filter without filtertype without error indication"
        if [[ "$DMAAP_ADP_FEATURE_LEVEL" == *"FILTERSCHEMA"* ]]; then
            adp_config_data='{"filterType": "regexp", "filter":"Message*","maxConcurrency": 1,"bufferTimeout": {"maxSize": 100,"maxTimeMilliseconds": '$adp_timeout'}}'
        else
            adp_config_data='{"filterType": "regexp", "filter":"Message*","maxConcurrency": 1,"bufferTimeout": {"maxSize": 100,"maxTimeMiliseconds": '$adp_timeout'}}'
        fi
    else
        if [[ "$DMAAP_ADP_FEATURE_LEVEL" == *"FILTERSCHEMA"* ]]; then
            adp_config_data='{"filter":"Message*","maxConcurrency": 1,"bufferTimeout": {"maxSize": 100,"maxTimeMilliseconds": '$adp_timeout'}}'
        else
            adp_config_data='{"filter":"Message*","maxConcurrency": 1,"bufferTimeout": {"maxSize": 100,"maxTimeMiliseconds": '$adp_timeout'}}'
        fi
    fi
    echo $adp_config_data > tmp/adp_config_data.json

    cr_index=$(($i%$NUM_CR))
    service_text="CR_SERVICE_TEXT_PATH_"$cr_index
    service_app="CR_SERVICE_APP_PATH_"$cr_index
    ics_api_idc_put_job 201 job-adp-kafka-$i "ADPKafkaType$i" ${!service_text}/job-adp-kafka-data$i"?storeas=md5" info-owner-adp-kafka-$i ${!service_app}/callbacks-null tmp/adp_config_data.json

done
print_timer

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
    # Create jobs for mediator kafka - CR stores data as MD5 hash
    start_timer "Create mediator (kafka) jobs: $NUM_JOBS"
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        med_timeout=$(($i*5000))
        if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"FILTERSCHEMA"* ]]; then
            med_config_data='{"bufferTimeout": {"maxSize": 100,"maxTimeMilliseconds": '$med_timeout'}}'
        else
            med_config_data='{"bufferTimeout": {"maxSize": 100,"maxTimeMiliseconds": '$med_timeout'}}'
        fi
        echo $med_config_data > tmp/med_config_data.json
        cr_index=$(($i%$NUM_CR))
        service_text="CR_SERVICE_TEXT_PATH_"$cr_index
        service_app="CR_SERVICE_APP_PATH_"$cr_index
        ics_api_idc_put_job 201 job-med-kafka-$i "MEDKafkaType$i" ${!service_text}/job-med-kafka-data$i"?storeas=md5" info-owner-med-kafka-$i ${!service_app}/callbacks-null     tmp/med_config_data.json
    done
    print_timer
fi

# Check job status
for ((i=1; i<=$NUM_JOBS; i++))
do
    ics_api_a1_get_job_status 200 job-adp-kafka-$i ENABLED 30
    if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then
        ics_api_a1_get_job_status 200 job-med-kafka-$i ENABLED 30
    fi
done


EXPECTED_DATA_DELIV=0 #Total delivered msg per CR
EXPECTED_BATCHES_DELIV=0 #Total delivered batches per CR
DATA_DELIV_JOBS=0 #Total delivered msg per job per CR

sleep_wait 60

start_timer "Data delivery adapter kafka, 2 strings per job (short buffer timeouts)"
# Send small text via message-router to adapter
for ((i=1; i<=$NUM_JOBS; i++))
do
    kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text"$i "text/plain" 'Message-------1'$i
    kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text"$i "text/plain" 'Discard-------3'$i #Should be filtered out
    kafkapc_api_post_msg 200 "unauthenticated.dmaapadp_kafka.text"$i "text/plain" 'Message-------3'$i
done
for ((i=1; i<=$NUM_JOBS; i++))
do
    kafkapc_equal topics/unauthenticated.dmaapadp_kafka.text$i/counters/sent 3 30
done

# Wait for data reception, adapter kafka
EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$EXPECTED_DATA_DELIV))
EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$EXPECTED_BATCHES_DELIV))

adp_timeout=$(($NUM_JOBS*1*2+60))  #NUM_JOBS*MIN_BUFFERTIMEOUT*2+60_SEC_DELAY
for ((i=0; i<$NUM_CR; i++))
do
    cr_equal $i received_callbacks $EXPECTED_DATA_DELIV $adp_timeout
    cr_greater_or_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV
done
print_timer

# Check received data callbacks from adapter
for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_index=$(($i%$NUM_CR))
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-kafka-data$i  'Message-------1'$i
    cr_api_check_single_generic_event_md5 200 $cr_index job-adp-kafka-data$i  'Message-------3'$i
done

if [[ "$DMAAP_MED_FEATURE_LEVEL" == *"KAFKATYPES"* ]]; then

    PREV_DATA_DELIV=$(cr_read 0 received_callbacks)
    PREV_BATCHES_DELIV=$(cr_read 0 received_callback_batches)
    start_timer "Data delivery mediator kafka, 2 strings per job (long buffer timeouts)"
    # Send small text via message-router to mediator
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        kafkapc_api_post_msg 200 "unauthenticated.dmaapmed_kafka.text$i" "text/plain" 'Message-------0'$i
        kafkapc_api_post_msg 200 "unauthenticated.dmaapmed_kafka.text$i" "text/plain" 'Message-------2'$i
    done
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        kafkapc_equal topics/unauthenticated.dmaapmed_kafka.text$i/counters/sent 2 30
    done

    # Wait for data reception, adapter kafka

    EXPECTED_DATA_DELIV=$(($NUM_JOBS*2/$NUM_CR+$PREV_DATA_DELIV))
    EXPECTED_BATCHES_DELIV=$(($NUM_JOBS/$NUM_CR+$PREV_BATCHES_DELIV))

    med_timeout=$(($NUM_JOBS*5*2+60)) #NUM_JOBS*MIN_BUFFERTIMEOUT*2+60_SEC_DELAY
    for ((i=0; i<$NUM_CR; i++))
    do
        cr_equal $i received_callbacks $EXPECTED_DATA_DELIV $med_timeout
        cr_greater_or_equal $i received_callback_batches $EXPECTED_BATCHES_DELIV
    done

    print_timer

    # Check received data callbacks from mediator
    for ((i=1; i<=$NUM_JOBS; i++))
    do
        cr_index=$(($i%$NUM_CR))
        cr_api_check_single_generic_event_md5 200 $cr_index job-med-kafka-data$i  'Message-------0'$i
        cr_api_check_single_generic_event_md5 200 $cr_index job-med-kafka-data$i  'Message-------2'$i
    done
fi

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment
