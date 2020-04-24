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


TC_ONELINE_DESCR="Testing of service registration timeouts and keepalive"

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

use_agent_rest

#Verify no callbacks or dmaap messages has been sent
cr_equal received_callbacks 0
mr_equal requests_submitted 0

#Check agent alive
api_get_status 200

#Print simulator interface version
sim_print ricsim_g1_1 interface

api_put_service 201 "rapp1" 15 "$CR_PATH/rapp1"

api_get_services 200 "rapp1" 15 "$CR_PATH/rapp1"

api_put_service 201 "rapp2" 120 "$CR_PATH/rapp2"

api_get_services 200 "rapp2" 120 "$CR_PATH/rapp2"

api_put_service 200 "rapp1" 50 "$CR_PATH/rapp1"
api_put_service 200 "rapp2" 180 "$CR_PATH/rapp2"

api_get_services 200 "rapp1" 50 "$CR_PATH/rapp1"
api_get_services 200 "rapp2" 180 "$CR_PATH/rapp2"

api_get_service_ids 200 "rapp1" "rapp2"

sleep_wait 30 "Waiting for keep alive timeout"

api_get_services 200 "rapp1" 50 "$CR_PATH/rapp1"
api_get_services 200 "rapp2" 180 "$CR_PATH/rapp2"

sleep_wait 100 "Waiting for keep alive timeout"

api_get_services 404 "rapp1"
api_get_services 200 "rapp2" 180 "$CR_PATH/rapp2"

api_delete_services 204 "rapp2"

api_get_services 404 "rapp1"
api_get_services 404 "rapp2"

api_put_service 201 "rapp3" 60 "$CR_PATH/rapp3"

api_get_services 200 "rapp3" 60 "$CR_PATH/rapp3"

sleep_wait 30 "Waiting for keep alive timeout"

api_put_service 200 "rapp3" 60 "$CR_PATH/rapp3"

sleep_wait 100 "Waiting for keep alive timeout"

api_get_services 404 "rapp3"

api_put_service 201 "rapp4" 120 "$CR_PATH/rapp4"

sleep_wait 60 "Waiting for keep alive timeout"

api_get_services 200 "rapp4" 120 "$CR_PATH/rapp4"

api_put_services_keepalive 200 "rapp4"

sleep_wait 90 "Waiting for keep alive timeout"

api_get_services 200 "rapp4" 120 "$CR_PATH/rapp4"

api_delete_services 204 "rapp4"

api_get_services 404 "rapp4"

api_get_services 404 "rapp1"
api_get_services 404 "rapp2"
api_get_services 404 "rapp3"

api_get_service_ids 200

api_delete_services 404 "rapp1"
api_delete_services 404 "rapp2"
api_delete_services 404 "rapp3"
api_delete_services 404 "rapp4"

api_put_services_keepalive 404 "rapp1"
api_put_services_keepalive 404 "rapp2"
api_put_services_keepalive 404 "rapp3"
api_put_services_keepalive 404 "rapp4"

api_get_service_ids 200



cr_equal received_callbacks 0
mr_equal requests_submitted 0

check_policy_agent_logs
check_control_panel_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_containers