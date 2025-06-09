#!/bin/bash
#  ============LICENSE_START===============================================
#  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

TC_ONELINE_DESCR="Sanity test, create service and then create, update and delete a policy using http and A1PMS REST and REST+SDNC"

# App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR A1PMS RICSIM RICMEDIATORSIM NGW KUBEPROXY SDNC"

# App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR A1PMS RICSIM RICMEDIATORSIM NGW KUBEPROXY SDNC"
# Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""
# Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if the image is not configured in the supplied env_file
# Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW "

# Supported test environment profiles
SUPPORTED_PROFILES="ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
# Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@
setup_testenvironment

#### TEST BEGIN ####
sim_generate_policy_uuid

# Tested variants of REST config
TESTED_VARIANTS="REST REST+SDNC"

# Test a1pms and mediator protocol versions
TESTED_PROTOCOLS="HTTP"

for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do
        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing a1pms: $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        clean_environment

        start_kube_proxy

        use_a1pms_rest_http
        use_ricmediator_simulator_http

        start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE
        set_a1pms_trace

        a1pms_api_put_service 201 "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"

        use_cr_http
        use_ricmediator_simulator_http
        if [[ $interface = *"SDNC"* ]]; then
            use_sdnc_http
        fi
        use_a1pms_rest_http

        start_ricmediator_simulators ricsim_g4 1 NONE
        start_cr 1
        start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

        if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
            start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
        fi

        if [ -z "$A1PMS_SDNC_ADAPTER_CLASS" ] || [ -z "$A1PMS_NOSDNC_ADAPTER_CLASS" ]; then
            echo -e $RED"Env vars A1PMS_SDNC_ADAPTER_CLASS and A1PMS_NOSDNC_ADAPTER_CLASS must be set with override file"$ERED
            exit 1
        fi

        if [[ $interface = *"SDNC"* ]]; then
            start_sdnc
            prepare_a1pms_config SDNC ".a1pms_config.json" ricsim-g4 $A1PMS_SDNC_ADAPTER_CLASS
        else
            prepare_a1pms_config NOSDNC ".a1pms_config.json" ricsim-g4 $A1PMS_NOSDNC_ADAPTER_CLASS
        fi

        if [ $RUNMODE == "KUBE" ]; then
            a1pms_load_config ".a1pms_config.json"
        else
            a1pms_api_put_configuration 200 ".a1pms_config.json"
        fi

        ricmediatorsim_put_policy_type 201 ricsim_g4_1 $A1PMS_ADAPTER_POLICY_TYPE testdata/OSC/sim_1.json

        a1pms_equal json:rics 1 300
        a1pms_equal json:policy-types 1 120
        a1pms_equal json:policies 0
        a1pms_equal json:policy-instances 0
        cr_equal 0 received_callbacks 1 120
        cr_api_check_all_sync_events 200 0 ric-registration ricsim_g4_1

        echo "############################################"
        echo "############## Health check ################"
        echo "############################################"
        sleep_wait 120 "Let A1PMS configuration take effect"
        a1pms_api_get_status 200

        echo "############################################"
        echo "##### Service registry and supervision #####"
        echo "############################################"
        a1pms_api_put_service 201 "serv1" 1000 "$CR_SERVICE_APP_PATH_0/1"
        a1pms_api_get_service_ids 200 "serv1" "ric-registration"
        a1pms_api_put_services_keepalive 200 "serv1"

        echo "############################################"
        echo "############## RIC Repository ##############"
        echo "############################################"
        a1pms_api_get_rics 200 NOTYPE "ricsim_g4_1:me1_ricsim_g4_1,me2_ricsim_g4_1:$A1PMS_ADAPTER_POLICY_TYPE:AVAILABLE"

        echo "############################################"
        echo "########### A1 Policy Management ###########"
        echo "############################################"
        notificationurl=$CR_SERVICE_APP_PATH_0"/test"
        a1pms_api_put_policy 201 "serv1" ricsim_g4_1 $A1PMS_ADAPTER_POLICY_TYPE 5300 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json
        a1pms_api_put_policy 200 "serv1" ricsim_g4_1 $A1PMS_ADAPTER_POLICY_TYPE 5300 NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json

        a1pms_equal json:policies 1
        a1pms_api_delete_policy 204 5300
        a1pms_equal json:policies 0
        a1pms_equal json:policy-instances 0
        cr_equal 0 received_callbacks 1

        check_a1pms_logs
    done
done

#### TEST COMPLETE ####
print_result
auto_clean_environment