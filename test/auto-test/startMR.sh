#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
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

TC_ONELINE_DESCR="Starts DMAAP MR"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="MR DMAAPMR KUBEPROXY KAFKAPC"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="MR DMAAPMR KUBEPROXY KAFKAPC"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-G-RELEASE ORAN-H-RELEASE ORAN-I-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

clean_environment
start_kube_proxy
start_mr    "$MR_READ_TOPIC"  "/events" "users/policy-agent" \
            "$MR_WRITE_TOPIC" "/events" "users/mr-stub"
            #\
            #"unauthenticated.dmaapadp.json" "/events" "dmaapadapterproducer/msgs" \
            #"unauthenticated.dmaapmed.json" "/events" "maapmediatorproducer/STD_Fault_Messages"

start_kafkapc

kafkapc_api_reset 200

kafkapc_api_create_topic 201 "unauthenticated.dmaapadp.json" "application/json"

kafkapc_api_create_topic 201 "unauthenticated.dmaapmed.json" "application/json"

dmaap_api_print_topics

if [ $RUNMODE == "KUBE" ]; then
    :
else
    docker kill $MR_STUB_APP_NAME
fi



