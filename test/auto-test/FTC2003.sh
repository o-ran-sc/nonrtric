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

TC_ONELINE_DESCR="Testing southbound proxy for Dmaap Adaptor"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CR MR ECS HTTPPROXY KUBEPROXY DMAAPADP"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" CR MR ECS HTTPPROXY KUBEPROXY DMAAPADP"
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

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

FLAT_A1_EI="1"
NUM_JOBS=10

clean_environment

use_cr_https
use_ecs_rest_https
use_mr_https
use_dmaapadp_https

start_kube_proxy

start_http_proxy

start_cr 1

start_ecs NOPROXY $SIM_GROUP/$ECS_COMPOSE_DIR/$ECS_CONFIG_FILE

set_ecs_trace

start_mr

start_dmaapadp PROXY $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_CONFIG_FILE $SIM_GROUP/$DMAAP_ADP_COMPOSE_DIR/$DMAAP_ADP_DATA_FILE

set_dmaapadp_trace

if [[ "$ECS_FEATURE_LEVEL" == *"INFO-TYPES"* ]]; then
    ecs_equal json:data-producer/v1/info-producers 1 60
else
    ecs_equal json:ei-producer/v1/eiproducers 1 60
fi

ecs_api_idc_get_job_ids 200 NOTYPE NOWNER EMPTY
ecs_api_idc_get_type_ids 200 ExampleInformationType


ecs_api_edp_get_producer_ids_2 200 NOTYPE DmaapGenericInfoProducer

for ((i=1; i<=$NUM_JOBS; i++))
do
    ecs_api_idc_put_job 201 joby$i ExampleInformationType $CR_SERVICE_MR_PATH_0/joby-data$i info-ownery$i $CR_SERVICE_MR_PATH_0/job_status_info-ownery$i testdata/dmaap-adapter/job-template.json
done

for ((i=1; i<=$NUM_JOBS; i++))
do
    ecs_api_a1_get_job_status 200 joby$i ENABLED 30
done


# Adapter data
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-1"}'
mr_api_send_json "/events/unauthenticated.dmaapadp.json" '{"msg":"msg-3"}'

cr_equal 0 received_callbacks $(($NUM_JOBS*2)) 60

for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_equal 0 received_callbacks?id=joby-data$i 2
done

for ((i=1; i<=$NUM_JOBS; i++))
do
    cr_api_check_single_genric_json_event 200 0 joby-data$i '{"msg":"msg-1"}'
    cr_api_check_single_genric_json_event 200 0 joby-data$i '{"msg":"msg-3"}'
done

cr_contains_str 0 remote_hosts $HTTP_PROXY_APP_NAME

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment