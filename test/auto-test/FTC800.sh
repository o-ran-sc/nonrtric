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

TC_ONELINE_DESCR="Create 10000 policies in sequence using http/https and a1pms REST with/without SDNC controller"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM SDNC KUBEPROXY NGW"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS ORAN-I-RELEASE ORAN-J-RELEASE ORAN-K-RELEASE ORAN-L-RELEASE"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

sim_generate_policy_uuid

#Local vars in test script
##########################
# Number of policies in each sequence
NUM_POLICIES=10000

# Tested variants of SDNC config
TESTED_VARIANTS="NOSDNC   SDNC"

#Test a1pms and simulator protocol versions (others are http only)
TESTED_PROTOCOLS="HTTP HTTPS"

for __httpx in $TESTED_PROTOCOLS ; do
    for interface in $TESTED_VARIANTS ; do

        echo "#####################################################################"
        echo "#####################################################################"
        echo "### Testing a1pms via $interface using $__httpx"
        echo "#####################################################################"
        echo "#####################################################################"

        if [ $__httpx == "HTTPS" ]; then
            use_cr_https
            use_simulator_https
            use_mr_https
            use_a1pms_rest_https
        else
            use_cr_http
            use_simulator_http
            use_mr_http
            use_a1pms_rest_http
        fi

        # Policy instance start id
        START_ID=1

        clean_environment

        start_kube_proxy

        start_ric_simulators ricsim_g1 1 OSC_2.1.0
        start_ric_simulators ricsim_g2 1 STD_1.1.3
        start_ric_simulators ricsim_g3 1  STD_2.0.0

        if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
            :
        else
            start_mr
        fi

        start_cr 1

        start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

        if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
            start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
        fi

        start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

        set_a1pms_debug

        if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
            :
        else
            mr_equal requests_submitted 0
        fi

        sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
        sim_put_policy_type 201 ricsim_g3_1 STD_QOS2_0.1.0 testdata/STD2/sim_qos2.json


        if [[ $interface == "SDNC" ]]; then
            start_sdnc
            controller_api_wait_for_status_ok 200 ricsim_g1_1
            prepare_a1pms_config      SDNC    ".a1pms_config.json"
        else
            prepare_a1pms_config      NOSDNC  ".a1pms_config.json"
        fi

        if [ $RUNMODE == "KUBE" ]; then
            a1pms_load_config                       ".a1pms_config.json"
        else
            a1pms_api_put_configuration 200 ".a1pms_config.json"
        fi

        sleep_wait 120 "Let A1PMS configuration take effect"

        a1pms_api_get_status 200

        sim_print ricsim_g1_1 interface
        sim_print ricsim_g2_1 interface
        sim_print ricsim_g3_1 interface

        a1pms_equal json:policy-types 3 300  #Wait for the a1pms to refresh types from the simulators

        a1pms_api_put_service 201 "serv1" 3600 "$CR_SERVICE_APP_PATH_0/1"

        notificationurl=$CR_SERVICE_APP_PATH_0"/test"

        start_timer "Create polices in OSC via a1pms REST and $interface using "$__httpx
        a1pms_api_put_policy 201 "serv1" ricsim_g1_1 1 $START_ID NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in OSC via a1pms REST and $interface using "$__httpx

        sim_equal ricsim_g1_1 num_instances $NUM_POLICIES

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD via a1pms REST and $interface using "$__httpx
        a1pms_api_put_policy 201 "serv1" ricsim_g2_1 NOTYPE $START_ID NOTRANSIENT $notificationurl testdata/STD/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in STD via a1pms REST and $interface using "$__httpx

        sim_equal ricsim_g2_1 num_instances $NUM_POLICIES



        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD 2 via a1pms REST and $interface using "$__httpx
        a1pms_api_put_policy 201 "serv1" ricsim_g3_1 STD_QOS2_0.1.0 $START_ID NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json $NUM_POLICIES
        print_timer "Create polices in STD via a1pms REST and $interface using "$__httpx

        sim_equal ricsim_g3_1 num_instances $NUM_POLICIES


        INTERFACE_VARIANT=
        if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
            INTERFACE_VARIANT=REST
            if [ $__httpx == "HTTPS" ]; then
                use_a1pms_rest_https
            else
                use_a1pms_rest_http
            fi
        else
            INTERFACE_VARIANT=DMAAP
            if [ $__httpx == "HTTPS" ]; then
                echo "Using secure ports towards dmaap"
                use_a1pms_dmaap_https
            else
                echo "Using non-secure ports towards dmaap"
                use_a1pms_dmaap_http
            fi
        fi

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in OSC via a1pms $INTERFACE_VARIANT, one by one, and $interface using "$__httpx
        a1pms_api_put_policy 201 "serv1" ricsim_g1_1 1 $START_ID NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in OSC via a1pms $INTERFACE_VARIANT, one by one, and $interface using "$__httpx

        sim_equal ricsim_g1_1 num_instances $((2*$NUM_POLICIES))

        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD via a1pms $INTERFACE_VARIANT, one by one, and $interface using "$__httpx
        a1pms_api_put_policy 201 "serv1" ricsim_g2_1 NOTYPE $START_ID NOTRANSIENT $notificationurl testdata/STD/pi1_template.json $NUM_POLICIES
        print_timer "Create polices in STD via a1pms $INTERFACE_VARIANT, one by one, and $interface using "$__httpx

        sim_equal ricsim_g2_1 num_instances $((2*$NUM_POLICIES))


        START_ID=$(($START_ID+$NUM_POLICIES))

        start_timer "Create polices in STD 2 via a1pms $INTERFACE_VARIANT, one by one, and $interface using "$__httpx
        a1pms_api_put_policy 201 "serv1" ricsim_g3_1 STD_QOS2_0.1.0 $START_ID NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json $NUM_POLICIES
        print_timer "Create polices in STD via a1pms $INTERFACE_VARIANT, one by one, and $interface using "$__httpx

        sim_equal ricsim_g3_1 num_instances $((2*$NUM_POLICIES))

        if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
            :
        else
            START_ID=$(($START_ID+$NUM_POLICIES))
            start_timer "Create polices in OSC via a1pms $INTERFACE_VARIANT in batch and $interface using "$__httpx
            a1pms_api_put_policy_batch 201 "serv1" ricsim_g1_1 1 $START_ID NOTRANSIENT $notificationurl testdata/OSC/pi1_template.json $NUM_POLICIES
            print_timer "Create polices in OSC via a1pms $INTERFACE_VARIANT in batch and $interface using "$__httpx

            sim_equal ricsim_g1_1 num_instances $((3*$NUM_POLICIES))

            START_ID=$(($START_ID+$NUM_POLICIES))

            start_timer "Create polices in STD via a1pms $INTERFACE_VARIANT in batch and $interface using "$__httpx
            a1pms_api_put_policy_batch 201 "serv1" ricsim_g2_1 NOTYPE $START_ID NOTRANSIENT $notificationurl testdata/STD/pi1_template.json $NUM_POLICIES
            print_timer "Create polices in STD via a1pms $INTERFACE_VARIANT in batch and $interface using "$__httpx

            sim_equal ricsim_g2_1 num_instances $((3*$NUM_POLICIES))


            START_ID=$(($START_ID+$NUM_POLICIES))

            start_timer "Create polices in STD via a1pms $INTERFACE_VARIANT in batch and $interface using "$__httpx
            a1pms_api_put_policy_batch 201 "serv1" ricsim_g3_1 STD_QOS2_0.1.0 $START_ID NOTRANSIENT $notificationurl testdata/STD2/pi_qos2_template.json $NUM_POLICIES
            print_timer "Create polices in STD via a1pms $INTERFACE_VARIANT in batch and $interface using "$__httpx

            sim_equal ricsim_g3_1 num_instances $((3*$NUM_POLICIES))
        fi

        if [ $interface == "SDNC" ]; then
            sim_contains_str ricsim_g1_1 remote_hosts $SDNC_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $SDNC_APP_NAME
            sim_contains_str ricsim_g3_1 remote_hosts $SDNC_APP_NAME
        else
            sim_contains_str ricsim_g1_1 remote_hosts $A1PMS_APP_NAME
            sim_contains_str ricsim_g2_1 remote_hosts $A1PMS_APP_NAME
            sim_contains_str ricsim_g3_1 remote_hosts $A1PMS_APP_NAME
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