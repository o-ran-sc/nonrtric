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

TC_ONELINE_DESCR="Preparation for test of the Control Panel and the Health Check app - populating a number of ric simulators with types and instances"

#App names to include in the test when running docker, space separated list
DOCKER_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM NGW KUBEPROXY"

#App names to include in the test when running kubernetes, space separated list
KUBE_INCLUDED_IMAGES="CP CR MR A1PMS RICSIM KUBEPROXY NGW"
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

clean_environment

start_kube_proxy

OSC_NUM_RICS=6
STD_NUM_RICS=5

start_ric_simulators  $RIC_SIM_PREFIX"_g1" $OSC_NUM_RICS OSC_2.1.0

start_ric_simulators  $RIC_SIM_PREFIX"_g2" $STD_NUM_RICS STD_1.1.3

start_ric_simulators  $RIC_SIM_PREFIX"_g3" $STD_NUM_RICS STD_2.0.0

if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
    :
else
    start_mr #Just to prevent errors in the a1pms log...
fi

start_control_panel $SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_CONFIG_FILE

if [ ! -z "$NRT_GATEWAY_APP_NAME" ]; then
    start_gateway $SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_CONFIG_FILE
fi

start_a1pms NORPOXY $SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_CONFIG_FILE

use_a1pms_rest_http



prepare_a1pms_config      NOSDNC  ".a1pms_config.json"

if [ $RUNMODE == "KUBE" ]; then
    a1pms_load_config                       ".a1pms_config.json"
else
    a1pms_api_put_configuration 200 ".a1pms_config.json"
fi

sleep_wait 120 "Let A1PMS configuration take effect"

a1pms_api_get_status 200

# Print the A1 version for OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_print $RIC_SIM_PREFIX"_g1_"$i interface
done


# Print the A1 version for STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_print $RIC_SIM_PREFIX"_g2_"$i interface
done

# Print the A1 version for STD 2.X
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_print $RIC_SIM_PREFIX"_g3_"$i interface
done

# Load the policytypes in osc
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 2 testdata/OSC/sim_hw.json
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 100 testdata/OSC/sim_qos.json
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g1_"$i 20008 testdata/OSC/sim_tsa.json
done


#Check the number of schemas and the individual schemas in OSC
a1pms_equal json:policy-types 4 300

for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    a1pms_equal json:policy-types?ric_id=$RIC_SIM_PREFIX"_g1_"$i 3 120
done

# Check the schemas in OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    a1pms_api_get_policy_type 200 2 testdata/OSC/hw-a1pms-modified.json
    a1pms_api_get_policy_type 200 100 testdata/OSC/qos-a1pms-modified.json
    a1pms_api_get_policy_type 200 20008 testdata/OSC/tsa-a1pms-modified.json
done

# Load the policytypes in std
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 demo-testdata/STD2/sim_qos.json
    sim_put_policy_type 201 $RIC_SIM_PREFIX"_g3_"$i STD_QOS2_0.1.0 demo-testdata/STD2/sim_qos2.json
done

#Check the number of schemas and the individual schemas in STD
a1pms_equal json:policy-types 6 120

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

# Create policies
use_a1pms_rest_http

a1pms_api_put_service 201 "Emergency-response-app" 0 "$CR_SERVICE_APP_PATH_0/1"

notificationurl=$CR_SERVICE_APP_PATH_0"/test"

# Create policies in OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 2 $((2000+$i)) NOTRANSIENT $notificationurl testdata/OSC/pihw_template.json 1
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 100 $((3000+$i)) NOTRANSIENT $notificationurl testdata/OSC/piqos_template.json 1
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g1_"$i 20008 $((4000+$i)) NOTRANSIENT $notificationurl testdata/OSC/pitsa_template.json 1
done


# Check the number of policies in OSC
for ((i=1; i<=$OSC_NUM_RICS; i++))
do
    sim_equal $RIC_SIM_PREFIX"_g1_"$i num_instances 3
done


# Create policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g2_"$i NOTYPE $((2100+$i)) NOTRANSIENT $notificationurl testdata/STD/pi1_template.json 1
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i STD_QOS_0_2_0 $((2300+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
    sim_generate_policy_uuid
    a1pms_api_put_policy 201 "Emergency-response-app" $RIC_SIM_PREFIX"_g3_"$i 'STD_QOS2_0.1.0' $((2400+$i)) NOTRANSIENT $notificationurl demo-testdata/STD2/pi1_template.json 1
done


# Check the number of policies in STD
for ((i=1; i<=$STD_NUM_RICS; i++))
do
    sim_equal $RIC_SIM_PREFIX"_g2_"$i num_instances 1
    sim_equal $RIC_SIM_PREFIX"_g3_"$i num_instances 2
done

check_a1pms_logs

#### TEST COMPLETE ####

store_logs          END

print_result

auto_clean_environment