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


TC_ONELINE_DESCR="Resync of RIC via changes in the pushed config"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM KUBEPROXY"

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES=""

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

sim_generate_policy_uuid

# Clean container and start all needed containers #
clean_environment

start_kube_proxy

start_a1pms NOPROXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

set_a1pms_trace

# Create service to be able to receive events when rics becomes available
# Must use rest towards the a1pms since dmaap is not configured yet
a1pms_api_put_service 201 "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"

# Start one RIC of each type
start_ric_simulators ricsim_g1 1  OSC_2.1.0
start_ric_simulators ricsim_g2 1  STD_1.1.3
start_ric_simulators ricsim_g3 1  STD_2.0.0

if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
    :
else
    start_mr
fi

start_cr 1

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE




prepare_a1pms_config      NOSDNC  ".a1pms_config.json"

a1pms_api_put_configuration 200 ".a1pms_config.json"
a1pms_api_get_configuration 200 ".a1pms_config.json"

a1pms_equal json:rics 3 300

cr_equal 0 received_callbacks 3 120

cr_api_check_all_sync_events 200 0 ric-registration ricsim_g1_1 ricsim_g2_1 ricsim_g3_1

# Add an STD RIC and check

start_ric_simulators ricsim_g2 2  STD_1.1.3

prepare_a1pms_config      NOSDNC  ".a1pms_config.json"
a1pms_api_put_configuration 200 ".a1pms_config.json"
a1pms_api_get_configuration 200 ".a1pms_config.json"

a1pms_equal json:rics 4 120

cr_equal 0 received_callbacks 4 120

cr_api_check_all_sync_events 200 0 ric-registration ricsim_g2_2

check_a1pms_logs

# Remove one RIC and check
container_id=$(docker rm -f $(docker ps -aq --filter "name=ricsim-g2-2"))
echo "Removing Container: ricsim-g2-2, ID: $container_id"
start_ric_simulators ricsim_g2 1  STD_1.1.3

prepare_a1pms_config      NOSDNC  ".a1pms_config.json"
a1pms_api_put_configuration 200 ".a1pms_config.json"
a1pms_api_get_configuration 200 ".a1pms_config.json"

a1pms_equal json:rics 3 120

cr_equal 0 received_callbacks 4 120

if [ "$A1PMS_VERSION" == "V2" ]; then
    a1pms_api_get_configuration 200 ".a1pms_config.json"
fi

check_a1pms_logs

store_logs          END_$consul_conf



#### TEST COMPLETE ####


print_result

auto_clean_environment
