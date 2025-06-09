#!/usr/bin/env bash

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


TC_ONELINE_DESCR="ONAP Use case REQ-626"
#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR DMAAPMR A1PMS RICSIM SDNC NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR DMAAPMR A1PMS RICSIM SDNC KUBEPROXY NGW"
#Pre-started app (not started by script) to include in the test when running kubernetes, space separated list
KUBE_PRESTARTED_IMAGES=""

#Ignore image in DOCKER_INCLUDED_IMAGES, KUBE_INCLUDED_IMAGES if
#the image is not configured in the supplied env_file
#Used for images not applicable to all supported profile
CONDITIONALLY_IGNORED_IMAGES="NGW"

#Supported test environment profiles
SUPPORTED_PROFILES="ONAP-MONTREAL ONAP-NEWDELHI ONAP-OSLO ONAP-PARIS"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

use_cr_https
use_a1pms_rest_https
if [[ "$SDNC_FEATURE_LEVEL" == *"NO_NB_HTTPS"* ]]; then
    deviation "SDNC does not support NB https"
    use_sdnc_http
else
    use_sdnc_https
fi
use_simulator_https
if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
    :
else
    use_mr_https
fi
__httpx="HTTPS"
notificationurl=$CR_SERVICE_APP_PATH_0"/test"

sim_generate_policy_uuid

# Tested variants of REST/DMAAP/SDNC config


if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
    TESTED_VARIANTS="REST   REST+SDNC"
else
    TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"
fi

for interface in $TESTED_VARIANTS ; do

    echo "#####################################################################"
    echo "#####################################################################"
    echo "### Testing a1pms: $interface using https"
    echo "#####################################################################"
    echo "#####################################################################"

    clean_environment

    start_kube_proxy

    if [[ $interface = *"DMAAP"* ]]; then
        use_a1pms_dmaap_https
    else
        use_a1pms_rest_https
    fi

    OSC_NUM_RICS=1
    STD_NUM_RICS=1

    start_ric_simulators  $RIC_SIM_PREFIX"_g1" $OSC_NUM_RICS OSC_2.1.0

    start_ric_simulators  $RIC_SIM_PREFIX"_g2" $STD_NUM_RICS STD_1.1.3

    start_ric_simulators $RIC_SIM_PREFIX"_g3" $STD_NUM_RICS STD_2.0.0

    if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
        :
    else
        start_mr    "$MR_READ_TOPIC"  "/events" "users/policy-agent" \
                    "$MR_WRITE_TOPIC" "/events" "users/mr-stub"
    fi

    start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

    if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
        start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
    fi



    if [[ $interface = *"SDNC"* ]]; then
        start_sdnc
        controller_api_wait_for_status_ok 200 ricsim_g1_1
        prepare_a1pms_config      SDNC    ".a1pms_config.json"
    else
        prepare_a1pms_config      NOSDNC  ".a1pms_config.json"
    fi

    start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

    set_a1pms_trace

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
            if [[ $interface = *"DMAAP"* ]]; then
                use_a1pms_dmaap_https
            else
                use_a1pms_rest_https
            fi
        else
            if [[ $interface = *"DMAAP"* ]]; then
                use_a1pms_dmaap_http
            else
                use_a1pms_rest_http
            fi
        fi
    fi

    # Check that all rics are synced in
    a1pms_equal json:rics 3 300

    a1pms_api_get_status 200

    # Print the A1 version for OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        sim_print $RIC_SIM_PREFIX"_g1_"$i interface
    done


    # Print the A1 version for STD 1.X
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        sim_print $RIC_SIM_PREFIX"_g2_"$i interface
    done

    # Print the A1 version for STD 2.X
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        sim_print $RIC_SIM_PREFIX"_g3_"$i interface
    done

    #################################################################
    ## REQ: Synchronize A1 Policy Information in RAN
    #################################################################

    # Load the policytypes in osc
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 100 demo-testdata/OSC/sim_qos.json
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 20008 demo-testdata/OSC/sim_tsa.json
    done

    #Check the number of schemas and the individual schemas in OSC
    a1pms_equal json:policy-types 3 300

    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        a1pms_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g1_"$i 2 120
    done

    # Check the schemas in OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        a1pms_api_get_policy_type 200 100 demo-testdata/OSC/qos-a1pms-modified.json
        a1pms_api_get_policy_type 200 20008 demo-testdata/OSC/tsa-a1pms-modified.json
    done


    # Load the policytypes in std
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 demo-testdata/STD2/sim_qos.json
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS2_0.1.0 demo-testdata/STD2/sim_qos2.json
    done

    #Check the number of schemas and the individual schemas in STD
    a1pms_equal json:policy-types 5 120

    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        a1pms_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g3_"$i 2 120
    done

    # Check the schemas in STD
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        a1pms_api_get_policy_type 200 STD_QOS_0_2_0 demo-testdata/STD2/qos-a1pms-modified.json
        a1pms_api_get_policy_type 200 'STD_QOS2_0.1.0' demo-testdata/STD2/qos2-a1pms-modified.json
    done

    #Check the number of types
    a1pms_equal json:policy-types 5 120

    #################################################################
    ##  REQ: A1 Policy Type / Instance Operations
    #################################################################

    # Create policies
    use_a1pms_rest_http

    a1pms_api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_APP_PATH_0/1"

    # Create policies in OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 100 $((3000+$i)) NOTRANSIENT $notificationurl demo-testdata/OSC/piqos_template.json 1
        a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 20008 $((4000+$i)) NOTRANSIENT $notificationurl demo-testdata/OSC/pitsa_template.json 1
    done


    # Check the number of policies in OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        sim_equal $RIC_SIM_PREFIX"_g1_"$i num_instances 2
    done


    # Create policies in STD
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g2_"$i NOTYPE $((2100+$i)) NOTRANSIENT $notificationurl demo-testdata/STD/pi1_template.json 1
        a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 $((2300+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
        a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i 'STD_QOS2_0.1.0' $((2400+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
    done


    # Check the number of policies in STD
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        sim_equal $RIC_SIM_PREFIX"_g2_"$i num_instances 1
        sim_equal $RIC_SIM_PREFIX"_g3_"$i num_instances 2
    done

    #################################################################
    ##  REQ: A1 Policy Instance Status Operations
    #################################################################

    # Check status STD
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        a1pms_api_get_policy_status 200 $((2100+$i)) STD "UNDEFINED"
        a1pms_api_get_policy_status 200 $((2300+$i)) STD2 EMPTY EMPTY
        a1pms_api_get_policy_status 200 $((2400+$i)) STD2 EMPTY EMPTY
    done

    # Check status OSC
    if [[ $TEST_ENV_PROFILE =~ ^ORAN-[A-H] ]] || [[ $TEST_ENV_PROFILE =~ ^ONAP-[A-L] ]]; then
      VAL='NOT IN EFFECT'
      VAL2="false"
    else
      VAL='NOT_ENFORCED'
      VAL2="OTHER_REASON"
    fi
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        a1pms_api_get_policy_status 200 $((3000+$i)) OSC "$VAL" "$VAL2"
        a1pms_api_get_policy_status 200 $((4000+$i)) OSC "$VAL" "$VAL2"
    done

    # Note: Status callback is not tested since this callback (http POST) is made from the
    # ricsim directly to the receiver of the status, i.e. the status does NOT
    # pass through A1PMS

    check_a1pms_logs

    if [[ $interface = *"SDNC"* ]]; then
        check_sdnc_logs
    fi

    #### TEST COMPLETE ####

    store_logs          "https__${interface}"

done

print_result

auto_clean_environment