#!/bin/bash

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

TC_ONELINE_DESCR="Basic use case, register service, create/update policy, delete policy, de-register service using both STD and OSC interface while mixing REST and Dmaap"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDNC SDNC_ONAP"

. ../common/testcase_common.sh $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

generate_uuid

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"

clean_containers

start_ric_simulators  ricsim_g1 3 OSC_2.1.0

start_ric_simulators  ricsim_g2 5 STD_1.1.3

start_mr

start_cr

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_control_panel

start_policy_agent

set_agent_debug

use_agent_rest_http

api_get_status 200

sim_print ricsim_g1_1 interface

sim_print ricsim_g2_1 interface

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

api_equal json:policy_types 2 60


# Create policies
use_agent_rest_http

api_put_service 201 "service1" 3600 "$CR_PATH/1"

api_put_policy 201 "service1" ricsim_g1_1 1 2000 NOTRANSIENT testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 1


use_agent_dmaap

api_put_policy 201 "service1" ricsim_g1_1 1 3000 NOTRANSIENT testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 2


use_agent_rest_http

api_put_policy 201 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 1


use_agent_dmaap

api_put_policy 201 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 2


#Update policies
use_agent_rest_http

api_put_service 200 "service1" 3600 "$CR_PATH/1"

api_put_policy 200 "service1" ricsim_g1_1 1 2000 NOTRANSIENT testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 2


use_agent_dmaap

api_put_policy 200 "service1" ricsim_g1_1 1 3000 NOTRANSIENT testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 2


use_agent_rest_http


api_put_policy 200 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 2


use_agent_dmaap

api_put_policy 200 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 2

# Check policies
api_get_policy 200 2000 testdata/OSC/pi1_template.json
api_get_policy 200 3000 testdata/OSC/pi1_template.json
api_get_policy 200 2100 testdata/STD/pi1_template.json
api_get_policy 200 3100 testdata/STD/pi1_template.json

# Remove policies

use_agent_dmaap
api_delete_policy 204 2000
use_agent_rest_http
api_delete_policy 204 3000
use_agent_dmaap
api_delete_policy 204 2100
use_agent_rest_http
api_delete_policy 204 3100

sim_equal ricsim_g1_1 num_instances 0
sim_equal ricsim_g2_1 num_instances 0

# Check remote host access to simulator

sim_contains_str ricsim_g1_1 hosts "policy-agent"
sim_contains_str ricsim_g2_1 hosts "policy-agent"

# Check policy removal
use_agent_rest_http
api_get_policy 404 2000
api_get_policy 404 3000
api_get_policy 404 2100
api_get_policy 404 3100

# Remove the service
use_agent_dmaap
api_delete_services 204 "service1"

api_get_services 404 "service1"



check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_containers