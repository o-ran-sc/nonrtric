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

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CBS CONSUL CP CR MR PA RICSIM NGW"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES=" MR CR PA RICSIM CP KUBEPROXY NGW"
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

. ../common/testcase_common.sh $@
. ../common/agent_api_functions.sh
. ../common/consul_cbs_functions.sh
. ../common/control_panel_api_functions.sh
. ../common/controller_api_functions.sh
. ../common/cr_api_functions.sh
. ../common/mr_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/kube_proxy_api_functions.sh
. ../common/gateway_api_functions.sh

setup_testenvironment

#### TEST BEGIN ####

generate_policy_uuid

use_simulator_http
use_mr_http
use_agent_rest_http


clean_environment

if [ $RUNMODE == "KUBE" ]; then
    start_kube_proxy
fi

start_ric_simulators  ricsim_g1 3 OSC_2.1.0

start_ric_simulators  ricsim_g2 5 STD_1.1.3

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

set_agent_debug

use_agent_rest_http

prepare_consul_config      NOSDNC  ".consul_config.json"

if [ $RUNMODE == "KUBE" ]; then
    agent_load_config                       ".consul_config.json"
else
    consul_config_app                      ".consul_config.json"
fi

api_get_status 200

sim_print ricsim_g1_1 interface

sim_print ricsim_g2_1 interface

if [ "$PMS_VERSION" == "V2" ]; then
    sim_print ricsim_g3_1 interface
fi

sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

if [ "$PMS_VERSION" == "V2" ]; then
    sim_put_policy_type 201 ricsim_g3_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json
    api_equal json:policy-types 3 300
else
    api_equal json:policy_types 2 300
fi

# Create policies

if [ "$PMS_VERSION" == "V2" ]; then
    notificationurl=$CR_SERVICE_PATH"/test"
else
    notificationurl=""
fi

use_agent_rest_http

api_put_service 201 "service1" 3600 "$CR_SERVICE_PATH/1"

api_put_policy 201 "service1" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 1


use_agent_dmaap_http

api_put_policy 201 "service1" ricsim_g1_1 1 3000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 2


use_agent_rest_http

api_put_policy 201 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 1


use_agent_dmaap_http

api_put_policy 201 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 2

if [ "$PMS_VERSION" == "V2" ]; then
    use_agent_rest_http

    api_put_policy 201 "service1" ricsim_g3_1 STD_QOS_0_2_0 2200 true $notificationurl testdata/STD2/pi_qos_template.json 1

    sim_equal ricsim_g3_1 num_instances 1

    use_agent_dmaap_http

    api_put_policy 201 "service1" ricsim_g3_1 STD_QOS_0_2_0 3200 NOTRANSIENT $notificationurl testdata/STD2/pi_qos_template.json 1

    sim_equal ricsim_g3_1 num_instances 2

fi

#Update policies
use_agent_rest_http

api_put_service 200 "service1" 3600 "$CR_SERVICE_PATH/1"

api_put_policy 200 "service1" ricsim_g1_1 1 2000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 2


use_agent_dmaap_http

api_put_policy 200 "service1" ricsim_g1_1 1 3000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json 1

sim_equal ricsim_g1_1 num_instances 2


use_agent_rest_http


api_put_policy 200 "service1" ricsim_g2_1 NOTYPE 2100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 2


use_agent_dmaap_http

api_put_policy 200 "service1" ricsim_g2_1 NOTYPE 3100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1

sim_equal ricsim_g2_1 num_instances 2

if [ "$PMS_VERSION" == "V2" ]; then
    use_agent_rest_http

    api_put_policy 200 "service1" ricsim_g3_1 STD_QOS_0_2_0 2200 true $notificationurl testdata/STD2/pi_qos_template.json 1

    sim_equal ricsim_g3_1 num_instances 2


    use_agent_dmaap_http

    api_put_policy 200 "service1" ricsim_g3_1 STD_QOS_0_2_0 3200 true $notificationurl testdata/STD2/pi_qos_template.json 1

    sim_equal ricsim_g3_1 num_instances 2
fi

# Check policies
if [ "$PMS_VERSION" == "V2" ]; then
    api_get_policy 200 2000 testdata/OSC/pi1_template.json "service1" ricsim_g1_1 1 false $notificationurl
    api_get_policy 200 3000 testdata/OSC/pi1_template.json "service1" ricsim_g1_1 1 false $notificationurl
    api_get_policy 200 2100 testdata/STD/pi1_template.json "service1" ricsim_g2_1 NOTYPE false $notificationurl
    api_get_policy 200 3100 testdata/STD/pi1_template.json "service1" ricsim_g2_1 NOTYPE false $notificationurl
    api_get_policy 200 2200 testdata/STD2/pi_qos_template.json "service1" ricsim_g3_1 STD_QOS_0_2_0 true $notificationurl
    api_get_policy 200 3200 testdata/STD2/pi_qos_template.json "service1" ricsim_g3_1 STD_QOS_0_2_0 true $notificationurl
else
    api_get_policy 200 2000 testdata/OSC/pi1_template.json
    api_get_policy 200 3000 testdata/OSC/pi1_template.json
    api_get_policy 200 2100 testdata/STD/pi1_template.json
    api_get_policy 200 3100 testdata/STD/pi1_template.json
fi

sim_equal ricsim_g1_1 num_instances 2
sim_equal ricsim_g2_1 num_instances 2

if [ "$PMS_VERSION" == "V2" ]; then
    sim_equal ricsim_g3_1 num_instances 2
fi

# Remove policies

use_agent_dmaap_http
api_delete_policy 204 2000
use_agent_rest_http
api_delete_policy 204 3000
use_agent_dmaap_http
api_delete_policy 204 2100
use_agent_rest_http
api_delete_policy 204 3100
if [ "$PMS_VERSION" == "V2" ]; then
    use_agent_dmaap_http
    api_delete_policy 204 2200
    use_agent_rest_http
    api_delete_policy 204 3200
fi

sim_equal ricsim_g1_1 num_instances 0
sim_equal ricsim_g2_1 num_instances 0

if [ "$PMS_VERSION" == "V2" ]; then
    sim_equal ricsim_g3_1 num_instances 0
fi

# Check remote host access to simulator

sim_contains_str ricsim_g1_1 remote_hosts $POLICY_AGENT_APP_NAME
sim_contains_str ricsim_g2_1 remote_hosts $POLICY_AGENT_APP_NAME
if [ "$PMS_VERSION" == "V2" ]; then
    sim_contains_str ricsim_g3_1 remote_hosts $POLICY_AGENT_APP_NAME
fi

# Check policy removal
use_agent_rest_http
api_get_policy 404 2000
api_get_policy 404 3000
api_get_policy 404 2100
api_get_policy 404 3100

if [ "$PMS_VERSION" == "V2" ]; then
    api_get_policy 404 2200
    api_get_policy 404 3200
fi

# Remove the service
use_agent_dmaap_http
api_delete_services 204 "service1"

api_get_services 404 "service1"

check_policy_agent_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment