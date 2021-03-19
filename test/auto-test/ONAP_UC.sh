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


TC_ONELINE_DESCR="ONAP Use case REQ-626"
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
SUPPORTED_PROFILES="ONAP-HONOLULU"
#Supported run modes
SUPPORTED_RUNMODES="DOCKER KUBE"

. ../common/testcase_common.sh $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh
. ../common/mr_api_functions.sh
. ../common/control_panel_api_functions.sh
. ../common/controller_api_functions.sh
. ../common/cr_api_functions.sh
. ../common/consul_cbs_functions.sh
. ../common/kube_proxy_api_functions.sh
. ../common/gateway_api_functions.sh

setup_testenvironment

#### TEST BEGIN ####

#Local vars in test script
##########################

use_cr_https
use_agent_rest_https
use_sdnc_https
use_simulator_https
use_mr_https

if [ "$PMS_VERSION" == "V2" ]; then
    notificationurl=$CR_SERVICE_PATH"/test"
else
    echo "Version V2 of PMS is needed, exiting..."
    exit 1
fi

generate_policy_uuid

# Tested variants of REST/DMAAP/SDNC config
TESTED_VARIANTS="REST   DMAAP   REST+SDNC   DMAAP+SDNC"
for interface in $TESTED_VARIANTS ; do

    echo "#####################################################################"
    echo "#####################################################################"
    echo "### Testing agent: $interface using https"
    echo "#####################################################################"
    echo "#####################################################################"

    clean_environment

    if [ $RUNMODE == "KUBE" ]; then
        start_kube_proxy
    fi

    if [[ $interface = *"DMAAP"* ]]; then
        use_agent_dmaap_https
    else
        use_agent_rest_https
    fi

    OSC_NUM_RICS=1
    STD_NUM_RICS=1

    start_ric_simulators  $RIC_SIM_PREFIX"_g1" $OSC_NUM_RICS OSC_2.1.0

    start_ric_simulators  $RIC_SIM_PREFIX"_g2" $STD_NUM_RICS STD_1.1.3

    start_ric_simulators $RIC_SIM_PREFIX"_g3" $STD_NUM_RICS STD_2.0.0

    start_mr

    start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

    if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
        start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
    fi

    if [[ $interface = *"SDNC"* ]]; then
        start_sdnc
        prepare_consul_config      SDNC    ".consul_config.json"
    else
        prepare_consul_config      NOSDNC  ".consul_config.json"
    fi

    start_policy_agent NORPOXY $SIM_GROUP/$POLICY_AGENT_COMPOSE_DIR/$POLICY_AGENT_CONFIG_FILE

    set_agent_trace

    if [ $RUNMODE == "DOCKER" ]; then
        start_consul_cbs
    fi

    if [ $RUNMODE == "KUBE" ]; then
        agent_load_config                       ".consul_config.json"
    else
        consul_config_app                      ".consul_config.json"
    fi

    # Check that all rics are synced in
    api_equal json:rics 3 300

    api_get_status 200

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

    # Load the polictypes in osc
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 100 demo-testdata/OSC/sim_qos.json
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 20008 demo-testdata/OSC/sim_tsa.json
    done

    #Check the number of schemas and the individual schemas in OSC
    api_equal json:policy-types 3 300

    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        api_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g1_"$i 2 120
    done

    # Check the schemas in OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        api_get_policy_type 200 100 demo-testdata/OSC/qos-agent-modified.json
        api_get_policy_type 200 20008 demo-testdata/OSC/tsa-agent-modified.json
    done


    # Load the polictypes in std
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 demo-testdata/STD2/sim_qos.json
        sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS2_0.1.0 demo-testdata/STD2/sim_qos2.json
    done

    #Check the number of schemas and the individual schemas in STD
    api_equal json:policy-types 5 120

    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        api_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g3_"$i 2 120
    done

    # Check the schemas in STD
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        api_get_policy_type 200 STD_QOS_0_2_0 demo-testdata/STD2/qos-agent-modified.json
        api_get_policy_type 200 'STD_QOS2_0.1.0' demo-testdata/STD2/qos2-agent-modified.json
    done

    #Check the number of types
    api_equal json:policy-types 5 120

    #################################################################
    ##  REQ: A1 Policy Type / Instance Operations
    #################################################################

    # Create policies
    use_agent_rest_http

    api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_PATH/1"

    # Create policies in OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 100 $((3000+$i)) NOTRANSIENT $notificationurl demo-testdata/OSC/piqos_template.json 1
        api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 20008 $((4000+$i)) NOTRANSIENT $notificationurl demo-testdata/OSC/pitsa_template.json 1
    done


    # Check the number of policies in OSC
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        sim_equal $RIC_SIM_PREFIX"_g1_"$i num_instances 2
    done


    # Create policies in STD
    for ((i=1; i<=$STD_NUM_RICS; i++))
    do
        api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g2_"$i NOTYPE $((2100+$i)) NOTRANSIENT $notificationurl demo-testdata/STD/pi1_template.json 1
        api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 $((2300+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
        api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i 'STD_QOS2_0.1.0' $((2400+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
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
        api_get_policy_status 200 $((2100+$i)) STD "UNDEFINED"
        api_get_policy_status 200 $((2300+$i)) STD2 EMPTY EMPTY
        api_get_policy_status 200 $((2400+$i)) STD2 EMPTY EMPTY
    done

    # Check status OSC
    VAL='NOT IN EFFECT'
    for ((i=1; i<=$OSC_NUM_RICS; i++))
    do
        api_get_policy_status 200 $((3000+$i)) OSC "$VAL" "false"
        api_get_policy_status 200 $((4000+$i)) OSC "$VAL" "false"
    done

    # Note: Status callback is not tested since this callback (http POST) is made from the
    # ricsim directly to the receiver of the status, i.e. the status does NOT
    # pass through PMS

    check_policy_agent_logs

    if [[ $interface = *"SDNC"* ]]; then
        check_sdnc_logs
    fi

    #### TEST COMPLETE ####

    store_logs          "https__${interface}"

done

print_result

auto_clean_environment