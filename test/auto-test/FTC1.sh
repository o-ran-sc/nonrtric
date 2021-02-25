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


TC_ONELINE_DESCR="Sanity test, create service and then create,update and delete a policy using http/https and Agent REST/DMAAP with/without SDNC controller"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CBS CONSUL CP CR MR DMAAPMR PA RICSIM SDNC NGW"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR PA RICSIM SDNC KUBEPROXY NGW"
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
. ../common/controller_api_functions.sh
. ../common/cr_api_functions.sh
. ../common/mr_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/http_proxy_api_functions.sh
. ../common/kube_proxy_api_functions.sh
. ../common/gateway_api_functions.sh

setup_testenvironment

#### TEST BEGIN ####

generate_policy_uuid

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"
#Test agent and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"
for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing agent: $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        clean_environment

        if [ $RUNMODE == "KUBE" ]; then
            start_kube_proxy
        fi

        if [ $__httpx == "HTTPS" ]; then
            use_agent_rest_https
        else
            use_agent_rest_http
        fi

        start_policy_agent NORPOXY $SIM_GROUP/$POLICY_AGENT_COMPOSE_DIR/$POLICY_AGENT_CONFIG_FILE

        set_agent_trace

        # Create service to be able to receive events when rics becomes available
        # Must use rest towards the agent since dmaap is not configured yet
        api_put_service 201 "ric-registration" 0 "$CR_SERVICE_PATH/ric-registration"

        if [ $__httpx == "HTTPS" ]; then
            use_cr_https
            use_simulator_https
            use_mr_https
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_https
            fi
            if [[ $interface = *"DMAAP"* ]]; then
                use_agent_dmaap_https
            else
                use_agent_rest_https
            fi
        else
            use_cr_http
            use_simulator_http
            use_mr_http
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_http
            fi
            if [[ $interface = *"DMAAP"* ]]; then
                use_agent_dmaap_http
            else
                use_agent_rest_http
            fi
        fi

        start_ric_simulators ricsim_g1 1  OSC_2.1.0
        start_ric_simulators ricsim_g2 1  STD_1.1.3
        if [ "$PMS_VERSION" == "V2" ]; then
            start_ric_simulators ricsim_g3 1  STD_2.0.0
        fi

        start_mr

        start_cr

        start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

        if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
            start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
        fi

        if [ $RUNMODE == "DOCKER" ]; then
            start_consul_cbs
        fi

        if [[ $interface = *"SDNC"* ]]; then
            start_sdnc
            prepare_consul_config      SDNC    ".consul_config.json"
        else
            prepare_consul_config      NOSDNC  ".consul_config.json"
        fi

        if [ $RUNMODE == "KUBE" ]; then
            agent_load_config                       ".consul_config.json"
        else
            consul_config_app                      ".consul_config.json"
        fi

        mr_equal requests_submitted 0

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json

        if [ "$PMS_VERSION" == "V2" ]; then

            sim_put_policy_type 201 ricsim_g3_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json

            api_equal json:rics 3 300

            api_equal json:policy-types 3 120

            api_equal json:policies 0

            api_equal json:policy-instances 0

            cr_equal received_callbacks 3 120

            cr_api_check_all_sync_events 200 ric-registration ricsim_g1_1 ricsim_g2_1 ricsim_g3_1

        else
            api_equal json:rics 2 300

            api_equal json:policy_schemas 2 120

            api_equal json:policy_types 2

            api_equal json:policies 0

            api_equal json:policy_ids 0
        fi

        echo "############################################"
        echo "############## Health check ################"
        echo "############################################"

        api_get_status 200

        echo "############################################"
        echo "##### Service registry and supervision #####"
        echo "############################################"

        api_put_service 201 "serv1" 1000 "$CR_SERVICE_PATH/1"

        api_get_service_ids 200 "serv1" "ric-registration"

        api_put_services_keepalive 200 "serv1"

        echo "############################################"
        echo "############## RIC Repository ##############"
        echo "############################################"

        if [ "$PMS_VERSION" == "V2" ]; then
            api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE ricsim_g3_1:me1_ricsim_g3_1,me2_ricsim_g3_1:STD_QOS_0_2_0:AVAILABLE"
        else
            api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:AVAILABLE  ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:EMPTYTYPE:AVAILABLE"
        fi

        echo "############################################"
        echo "########### A1 Policy Management ###########"
        echo "############################################"

        if [ "$PMS_VERSION" == "V2" ]; then
            notificationurl=$CR_SERVICE_PATH"/test"
        else
            notificationurl=""
        fi
        api_put_policy 201 "serv1" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
        api_put_policy 200 "serv1" ricsim_g1_1 1 5000 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
        if [ "$PMS_VERSION" == "V2" ]; then
            api_put_policy 201 "serv1" ricsim_g3_1 STD_QOS_0_2_0 5200 true $notificationurl testdata/STD2/pi_qos_template.json
            api_put_policy 200 "serv1" ricsim_g3_1 STD_QOS_0_2_0 5200 true $notificationurl testdata/STD2/pi_qos_template.json
        fi

        api_put_policy 201 "serv1" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json
        api_put_policy 200 "serv1" ricsim_g2_1 NOTYPE 5100 NOTRANSIENT $notificationurl testdata/STD/pi1_template.json

        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:policies 3
        else
            api_equal json:policies 2
        fi

        api_delete_policy 204 5000

        api_delete_policy 204 5100

        if [ "$PMS_VERSION" == "V2" ]; then
            api_delete_policy 204 5200
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            api_equal json:policies 0

            api_equal json:policy-instances 0
        else
            api_equal json:policies 0

            api_equal json:policy_ids 0
        fi

        if [ "$PMS_VERSION" == "V2" ]; then
            cr_equal received_callbacks 3
        fi

        if [[ $interface = *"DMAAP"* ]]; then

            if [ "$PMS_VERSION" == "V2" ]; then
                VAL=14 # Number of Agent API calls over DMAAP
            else
                VAL=11 # Number of Agent API calls over DMAAP
            fi
            mr_equal requests_fetched $VAL
            mr_equal responses_submitted $VAL
            mr_equal responses_fetched $VAL
            mr_equal current_requests 0
            mr_equal current_responses 0
        else
            mr_equal requests_submitted 0
        fi

        if [[ $interface = *"SDNC"* ]]; then
            sim_contains_str ricsim_g1_1 remote_hosts $SDNC_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $SDNC_APP_NAME
            if [ "$PMS_VERSION" == "V2" ]; then
                sim_contains_str ricsim_g3_1 remote_hosts $SDNC_APP_NAME
            fi
        else
            sim_contains_str ricsim_g1_1 remote_hosts $POLICY_AGENT_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $POLICY_AGENT_APP_NAME
            if [ "$PMS_VERSION" == "V2" ]; then
                sim_contains_str ricsim_g3_1 remote_hosts $POLICY_AGENT_APP_NAME
            fi
        fi

        check_policy_agent_logs


        if [[ $interface = *"SDNC"* ]]; then
            check_sdnc_logs
        fi

        store_logs          "${__httpx}__${interface}"

    done

done

#### TEST COMPLETE ####


print_result

auto_clean_environment
