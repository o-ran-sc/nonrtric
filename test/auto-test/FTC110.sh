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


TC_ONELINE_DESCR="Testing of service registration timeouts and keepalive"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDMC SDNC_ONAP"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh


#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"


#### TEST BEGIN ####

clean_containers

start_ric_simulators ricsim_g1 1  OSC_2.1.0

start_mr

start_cr

start_consul_cbs

prepare_consul_config      NOSDNC  ".consul_config.json"
consul_config_app                  ".consul_config.json"

start_control_panel

start_policy_agent

set_agent_debug

use_agent_rest_http

#Verify no callbacks or dmaap messages has been sent
cr_equal received_callbacks 0
mr_equal requests_submitted 0

#Check agent alive
api_get_status 200

#Print simulator interface version
sim_print ricsim_g1_1 interface

api_put_service 201 "service1" 15 "$CR_PATH/service1"

api_get_services 200 "service1" "service1" 15 "$CR_PATH/service1"

api_put_service 201 "service2" 120 "$CR_PATH/service2"

api_get_services 200 "service2" "service2" 120 "$CR_PATH/service2"

api_put_service 200 "service1" 50 "$CR_PATH/service1"
api_put_service 200 "service2" 180 "$CR_PATH/service2"

api_get_services 200 "service1" "service1" 50 "$CR_PATH/service1"
api_get_services 200 "service2" "service2" 180 "$CR_PATH/service2"

api_get_service_ids 200 "service1" "service2"

sleep_wait 30 "Waiting for keep alive timeout"

api_get_services 200 "service1" "service1" 50 "$CR_PATH/service1"
api_get_services 200 "service2" "service2" 180 "$CR_PATH/service2"

sleep_wait 100 "Waiting for keep alive timeout"

api_get_services 404 "service1"
api_get_services 200 "service2" "service2" 180 "$CR_PATH/service2"

api_delete_services 204 "service2"

api_get_services 404 "service1"
api_get_services 404 "service2"

api_put_service 201 "service3" 60 "$CR_PATH/service3"

api_get_services 200 "service3" "service3" 60 "$CR_PATH/service3"

sleep_wait 30 "Waiting for keep alive timeout"

api_put_service 200 "service3" 60 "$CR_PATH/service3"

sleep_wait 100 "Waiting for keep alive timeout"

api_get_services 404 "service3"

api_put_service 201 "service4" 120 "$CR_PATH/service4"

sleep_wait 60 "Waiting for keep alive timeout"

api_get_services 200 "service4" "service4" 120 "$CR_PATH/service4"

api_put_services_keepalive 200 "service4"

sleep_wait 90 "Waiting for keep alive timeout"

api_get_services 200 "service4" "service4" 120 "$CR_PATH/service4"

api_delete_services 204 "service4"

api_get_services 404 "service4"

api_get_services 404 "service1"
api_get_services 404 "service2"
api_get_services 404 "service3"

api_get_service_ids 200

api_delete_services 404 "service1"
api_delete_services 404 "service2"
api_delete_services 404 "service3"
api_delete_services 404 "service4"

api_put_services_keepalive 404 "service1"
api_put_services_keepalive 404 "service2"
api_put_services_keepalive 404 "service3"
api_put_services_keepalive 404 "service4"

# Policy delete after timeout
api_put_service 201 "service10" 600 "$CR_PATH/service10"

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

api_equal json:rics 1 60

api_equal json:policy_schemas 1 120

api_equal json:policy_types 1

api_equal json:policies 0

api_put_policy 201 "service10" ricsim_g1_1 1 5000 NOTRANSIENT testdata/OSC/pi1_template.json

api_equal json:policies 1

sim_equal ricsim_g1_1 num_instances 1

api_put_policy 201 "service10" ricsim_g1_1 1 5001 true testdata/OSC/pi1_template.json

api_equal json:policies 2

sim_equal ricsim_g1_1 num_instances 2

sim_post_delete_instances 200 ricsim_g1_1

#Wait for recreate of non transient policy
api_equal json:policies 1 180

sim_equal ricsim_g1_1 num_instances 1

api_put_service 200 "service10" 10 "$CR_PATH/service10"

#Wait for service expiry
api_equal json:policies 0 120

sim_equal ricsim_g1_1 num_instances 0


api_get_service_ids 200

deviation "TR18 Agents sends callback with empty body"
cr_equal received_callbacks 0
mr_equal requests_submitted 0

check_policy_agent_logs
check_control_panel_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_containers