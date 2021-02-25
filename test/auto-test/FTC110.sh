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

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CBS CONSUL CP CR MR PA RICSIM NGW"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR PA RICSIM KUBEPROXY NGW"
#Prestarted app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-GUILIN ONAP-HONOLULU  ORAN-CHERRY ORAN-DAWN"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/consul_cbs_functions.sh
. ../common/control_panel_api_functions.sh
. ../common/cr_api_functions.sh
. ../common/mr_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/kube_proxy_api_functions.sh
. ../common/gateway_api_functions.sh

setup_testenvironment

#### TEST BEGIN ####

generate_policy_uuid

use_cr_http
use_simulator_http
use_mr_http
use_agent_rest_http

clean_environment

if [ $RUNMODE == "KUBE" ]; then
    start_kube_proxy
fi

start_ric_simulators ricsim_g1 1  OSC_2.1.0
start_ric_simulators ricsim_g2 1  STD_1.1.3
if [ "$PMS_VERSION" == "V2" ]; then
    start_ric_simulators ricsim_g3 1  STD_2.0.0
fi

start_mr

start_cr

if [ $RUNMODE == "DOCKER" ]; then
    start_consul_cbs
fi

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_policy_agent NORPOXY $SIM_GROUP/$POLICY_AGENT_COMPOSE_DIR/$POLICY_AGENT_CONFIG_FILE

prepare_consul_config      NOSDNC  ".consul_config.json"

if [ $RUNMODE == "KUBE" ]; then
    agent_load_config                       ".consul_config.json"
else
    consul_config_app                      ".consul_config.json"
fi

set_agent_debug

mr_equal requests_submitted 0

#Check agent alive
api_get_status 200

#Print simulator interface version
sim_print ricsim_g1_1 interface
sim_print ricsim_g2_1 interface
if [ "$PMS_VERSION" == "V2" ]; then
    sim_print ricsim_g3_1 interface
fi

api_put_service 201 "service1" 15 "$CR_SERVICE_PATH/service1"

api_get_services 200 "service1" "service1" 15 "$CR_SERVICE_PATH/service1"

api_put_service 201 "service2" 120 "$CR_SERVICE_PATH/service2"

api_get_services 200 "service2" "service2" 120 "$CR_SERVICE_PATH/service2"

api_put_service 200 "service1" 50 "$CR_SERVICE_PATH/service1"
api_put_service 200 "service2" 180 "$CR_SERVICE_PATH/service2"

api_get_services 200 "service1" "service1" 50 "$CR_SERVICE_PATH/service1"
api_get_services 200 "service2" "service2" 180 "$CR_SERVICE_PATH/service2"

api_get_service_ids 200 "service1" "service2"

sleep_wait 30 "Waiting for keep alive timeout"

api_get_services 200 "service1" "service1" 50 "$CR_SERVICE_PATH/service1"
api_get_services 200 "service2" "service2" 180 "$CR_SERVICE_PATH/service2"

sleep_wait 100 "Waiting for keep alive timeout"

api_get_services 404 "service1"
api_get_services 200 "service2" "service2" 180 "$CR_SERVICE_PATH/service2"

api_delete_services 204 "service2"

api_get_services 404 "service1"
api_get_services 404 "service2"

api_put_service 201 "service3" 60 "$CR_SERVICE_PATH/service3"

api_get_services 200 "service3" "service3" 60 "$CR_SERVICE_PATH/service3"

sleep_wait 30 "Waiting for keep alive timeout"

api_put_service 200 "service3" 60 "$CR_SERVICE_PATH/service3"

sleep_wait 100 "Waiting for keep alive timeout"

api_get_services 404 "service3"

api_put_service 201 "service4" 120 "$CR_SERVICE_PATH/service4"

sleep_wait 60 "Waiting for keep alive timeout"

api_get_services 200 "service4" "service4" 120 "$CR_SERVICE_PATH/service4"

api_put_services_keepalive 200 "service4"

sleep_wait 90 "Waiting for keep alive timeout"

api_get_services 200 "service4" "service4" 120 "$CR_SERVICE_PATH/service4"

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
api_put_service 201 "service10" 600 "$CR_SERVICE_PATH/service10"

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

if [ "$PMS_VERSION" == "V2" ]; then

    sim_put_policy_type 201 ricsim_g3_1 STD_QOS2_0.1.0 testdata/STD2/sim_qos2.json

    api_equal json:rics 3 300

    #api_equal json:policy_schemas 2 120

    api_equal json:policy-types 3 120

    api_equal json:policies 0
else
    api_equal json:rics 2 300

    api_equal json:policy_schemas 2 120

    api_equal json:policy_types 2

    api_equal json:policies 0
fi

if [ "$PMS_VERSION" == "V2" ]; then
    notificationurl=$CR_SERVICE_PATH"/test"
else
    notificationurl=""
fi

api_put_policy 201 "service10" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
api_put_policy 201 "service10" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json

if [ "$PMS_VERSION" == "V2" ]; then
    api_put_policy 201 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json
    api_equal json:policies 3
else
    api_equal json:policies 2
fi

sim_equal ricsim_g1_1 num_instances 1
sim_equal ricsim_g2_1 num_instances 1

api_put_policy 201 "service10" ricsim_g1_1 1 5001 true $notificationurl testdata/OSC/pi1_template.json
api_put_policy 201 "service10" ricsim_g2_1 NOTYPE 5101 true $notificationurl testdata/STD/pi1_template.json

if [ "$PMS_VERSION" == "V2" ]; then
    api_put_policy 201 "service10" ricsim_g3_1 STD_QOS2_0.1.0 5201 true $notificationurl testdata/STD2/pi_qos2_template.json
    api_equal json:policies 6
else
    api_equal json:policies 4
fi

sim_equal ricsim_g1_1 num_instances 2
sim_equal ricsim_g2_1 num_instances 2
if [ "$PMS_VERSION" == "V2" ]; then
    sim_equal ricsim_g3_1 num_instances 2
fi

sim_post_delete_instances 200 ricsim_g1_1
sim_post_delete_instances 200 ricsim_g2_1

if [ "$PMS_VERSION" == "V2" ]; then
    sim_post_delete_instances 200 ricsim_g3_1
fi

#Wait for recreate of non transient policy
if [ "$PMS_VERSION" == "V2" ]; then
    api_equal json:policies 3 180
else
    api_equal json:policies 2 180
fi

sim_equal ricsim_g1_1 num_instances 1
sim_equal ricsim_g2_1 num_instances 1
if [ "$PMS_VERSION" == "V2" ]; then
    sim_equal ricsim_g3_1 num_instances 1
fi

api_put_service 200 "service10" 10 "$CR_SERVICE_PATH/service10"

#Wait for service expiry
api_equal json:policies 0 120

sim_equal ricsim_g1_1 num_instances 0
sim_equal ricsim_g2_1 num_instances 0
if [ "$PMS_VERSION" == "V2" ]; then
    sim_equal ricsim_g3_1 num_instances 0
fi

api_get_service_ids 200

mr_equal requests_submitted 0

check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment