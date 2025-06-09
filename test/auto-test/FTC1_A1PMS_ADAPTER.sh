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


TC_ONELINE_DESCR="Sanity test, create service and then create,update and delete a policy with A1PMS adapter using http/https and A1PMS REST with/without SDNC controller"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR A1PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR A1PMS RICSIM SDNC NGW KUBEPROXY "
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW "

#Supported test environment profiles
SUPPORTED_PROFILES="ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

sim_generate_policy_uuid

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   REST+SDNC"

#Test a1pms and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"
for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing a1pms: $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        clean_environment

        start_kube_proxy

        if [ $__httpx == "HTTPS" ]; then
            use_a1pms_rest_https
        else
            use_a1pms_rest_http
        fi

        start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

        set_a1pms_trace

        # Create service to be able to receive events when rics becomes available
        # Must use rest towards the a1pms since dmaap is not configured yet
        a1pms_api_put_service 201 "ric-registration" 0 "$CR_SERVICE_APP_PATH_0/ric-registration"

        if [ $__httpx == "HTTPS" ]; then
            use_cr_https
            use_simulator_https
            if [[ $interface = *"SDNC"* ]]; then
                if [[ "$SDNC_FEATURE_LEVEL" == *"NO_NB_HTTPS"* ]]; then
                    deviation "SDNC does not support NB https"
                    use_sdnc_http
                else
                    use_sdnc_https
                fi
            fi
            use_a1pms_rest_https
        else
            use_cr_http
            use_simulator_http
            if [[ $interface = *"SDNC"* ]]; then
                use_sdnc_http
            fi
            use_a1pms_rest_http
        fi

        start_ric_simulators ricsim_g1 1  STD_2.0.0
        start_ric_simulators ricsim_g2 1  STD_2.0.0  #A1PMS will use adapter class towards this sim

        start_cr 1

        start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

        if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
            start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
        fi

        if [ -z "$A1PMS_ADAPTER_CLASS" ]; then
            echo -e $RED"Env var A1PMS_ADAPTER_CLASS must be set with override file"$ERED
            exit 1
        fi
        if [[ $interface = *"SDNC"* ]]; then
            start_sdnc
            controller_api_wait_for_status_ok 200 ricsim_g1_1
            prepare_a1pms_config      SDNC    ".a1pms_config.json" ricsim-g2 $A1PMS_ADAPTER_CLASS
        else
            prepare_a1pms_config      NOSDNC  ".a1pms_config.json" ricsim-g2 $A1PMS_ADAPTER_CLASS
        fi

        if [ $RUNMODE == "KUBE" ]; then
            a1pms_load_config                       ".a1pms_config.json"
        else
            #Temporary switch to http/https if dmaap use. Otherwise it is not possible to push config
            if [ $__httpx == "HTTPS" ]; then
                use_a1pms_rest_https
            else
                use_a1pms_rest_http
            fi
            a1pms_api_put_configuration 200 ".a1pms_config.json"
            if [ $__httpx == "HTTPS" ]; then
                use_a1pms_rest_https
            else
                use_a1pms_rest_http
            fi
        fi

        sim_put_policy_type 201 ricsim_g1_1 STD_QOS_0_2_0 testdata/STD2/sim_qos.json
        sim_put_policy_type 201 ricsim_g2_1 $A1PMS_ADAPTER_POLICY_TYPE testdata/STD2/sim_qos.json

        a1pms_equal json:rics 2 300

        a1pms_equal json:policy-types 2 120

        a1pms_equal json:policies 0

        a1pms_equal json:policy-instances 0

        cr_equal 0 received_callbacks 2 120

        cr_api_check_all_sync_events 200 0 ric-registration ricsim_g1_1 ricsim_g2_1

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

        a1pms_api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:STD_QOS_0_2_0:AVAILABLE ricsim_g2_1:me1_ricsim_g2_1,me2_ricsim_g2_1:$A1PMS_ADAPTER_POLICY_TYPE:AVAILABLE"

        echo "############################################"
        echo "########### A1 Policy Management ###########"
        echo "############################################"

        notificationurl=$CR_SERVICE_APP_PATH_0"/test"

        a1pms_api_put_policy 201 "serv1" ricsim_g1_1 STD_QOS_0_2_0 5000 true $notificationurl testdata/STD2/pi_qos_template.json
        a1pms_api_put_policy 200 "serv1" ricsim_g1_1 STD_QOS_0_2_0 5000 true $notificationurl testdata/STD2/pi_qos_template.json

        a1pms_api_put_policy 201 "serv1" ricsim_g2_1 $A1PMS_ADAPTER_POLICY_TYPE 5100 true $notificationurl testdata/STD2/pi_qos_template.json
        a1pms_api_put_policy 200 "serv1" ricsim_g2_1 $A1PMS_ADAPTER_POLICY_TYPE 5100 true $notificationurl testdata/STD2/pi_qos_template.json

        a1pms_equal json:policies 2

        a1pms_api_delete_policy 204 5000

        a1pms_api_delete_policy 204 5100

        a1pms_equal json:policies 0

        a1pms_equal json:policy-instances 0

        cr_equal 0 received_callbacks 2


        if [[ $interface = *"SDNC"* ]]; then
            sim_contains_str ricsim_g1_1 remote_hosts $SDNC_APP_NAME
            # Adapter does not go through SDNC
            sim_contains_str ricsim_g2_1 remote_hosts $A1PMS_APP_NAME
        else
            sim_contains_str ricsim_g1_1 remote_hosts $A1PMS_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $A1PMS_APP_NAME
        fi

        check_a1pms_logs


        if [[ $interface = *"SDNC"* ]]; then
            check_sdnc_logs
        fi

        store_logs          "${__httpx}__${interface}"

    done

done

#### TEST COMPLETE ####


print_result

auto_clean_environment
