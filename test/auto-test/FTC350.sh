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

TC_ONELINE_DESCR="Change supported policy types and reconfigure rics"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDNC_ONAP"

. ../common/testcase_common.sh  $@
. ../common/agent_api_functions.sh
. ../common/ricsimulator_api_functions.sh

#### TEST BEGIN ####

generate_uuid

#Local vars in test script
##########################
# Path to callback receiver
CR_PATH="http://$CR_APP_NAME:$CR_EXTERNAL_PORT/callbacks"
use_cr_http

NUM_RICS=10
NUM_RICS_2=$(($NUM_RICS-2))

# Tested variants of REST/SDNC config
TESTED_VARIANTS="REST   REST+SDNC"

for interface in $TESTED_VARIANTS ; do

    echo "#####################################################################"
    echo "#####################################################################"
    echo "### Testing agent: "$interface
    echo "#####################################################################"
    echo "#####################################################################"


    # Clean container and start all needed containers #
    clean_containers

    #Start simulators and prepare two configs

    start_ric_simulators ricsim_g1 $NUM_RICS_2 OSC_2.1.0

    start_cr

    start_mr

    start_consul_cbs

    if [[ $interface = *"SDNC"* ]]; then
        start_sdnc
        prepare_consul_config      SDNC  ".consul_config_2.json"
    else
        prepare_consul_config      NOSDNC  ".consul_config_2.json"
    fi

    consul_config_app                  ".consul_config_2.json"


    # Create 2nd config and save for later
    start_ric_simulators ricsim_g1 $NUM_RICS OSC_2.1.0

    if [[ $interface = *"SDNC"* ]]; then
        start_sdnc
        prepare_consul_config      SDNC  ".consul_config_all.json"
    else
        prepare_consul_config      NOSDNC  ".consul_config_all.json"
    fi

    start_policy_agent

    set_agent_debug

    api_get_status 200

    for ((i=1; i<=${NUM_RICS}; i++))
    do
        sim_print ricsim_g1_$i interface
    done

    # All sims running but 2 are not configured in consul
    api_equal json:rics 8 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:NOTYPE:???? \
                             ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:NOTYPE:???? \
                             ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:NOTYPE:???? \
                             ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:NOTYPE:???? \
                             ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:NOTYPE:???? \
                             ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:NOTYPE:???? \
                             ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:NOTYPE:???? \
                             ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:NOTYPE:???? "


    sim_put_policy_type 201 ricsim_g1_1 1 testdata/OSC/sim_1.json
    sim_put_policy_type 201 ricsim_g1_2 1 testdata/OSC/sim_1.json
    sim_put_policy_type 201 ricsim_g1_3 1 testdata/OSC/sim_1.json
    sim_put_policy_type 201 ricsim_g1_4 1 testdata/OSC/sim_1.json
    sim_put_policy_type 201 ricsim_g1_5 1 testdata/OSC/sim_1.json

    sim_put_policy_type 201 ricsim_g1_2 2 testdata/OSC/sim_2.json
    sim_put_policy_type 201 ricsim_g1_3 2 testdata/OSC/sim_2.json
    sim_put_policy_type 201 ricsim_g1_4 2 testdata/OSC/sim_2.json
    sim_put_policy_type 201 ricsim_g1_5 2 testdata/OSC/sim_2.json
    sim_put_policy_type 201 ricsim_g1_6 2 testdata/OSC/sim_2.json

    sim_put_policy_type 201 ricsim_g1_3 3 testdata/OSC/sim_3.json
    sim_put_policy_type 201 ricsim_g1_4 3 testdata/OSC/sim_3.json
    sim_put_policy_type 201 ricsim_g1_5 3 testdata/OSC/sim_3.json
    sim_put_policy_type 201 ricsim_g1_6 3 testdata/OSC/sim_3.json
    sim_put_policy_type 201 ricsim_g1_7 3 testdata/OSC/sim_3.json

    sim_put_policy_type 201 ricsim_g1_4 4 testdata/OSC/sim_4.json
    sim_put_policy_type 201 ricsim_g1_5 4 testdata/OSC/sim_4.json
    sim_put_policy_type 201 ricsim_g1_6 4 testdata/OSC/sim_4.json
    sim_put_policy_type 201 ricsim_g1_7 4 testdata/OSC/sim_4.json
    sim_put_policy_type 201 ricsim_g1_8 4 testdata/OSC/sim_4.json

    sim_put_policy_type 201 ricsim_g1_5 5 testdata/OSC/sim_5.json
    sim_put_policy_type 201 ricsim_g1_6 5 testdata/OSC/sim_5.json
    sim_put_policy_type 201 ricsim_g1_7 5 testdata/OSC/sim_5.json
    sim_put_policy_type 201 ricsim_g1_8 5 testdata/OSC/sim_5.json
    sim_put_policy_type 201 ricsim_g1_9 5 testdata/OSC/sim_5.json

    api_equal json:policy_types 5 120

    echo "Check the number of types in the agent for each ric"
    api_equal json:policy_types?ric=ricsim_g1_1 1 120
    api_equal json:policy_types?ric=ricsim_g1_2 2 120
    api_equal json:policy_types?ric=ricsim_g1_3 3 120
    api_equal json:policy_types?ric=ricsim_g1_4 4 120
    api_equal json:policy_types?ric=ricsim_g1_5 5 120
    api_equal json:policy_types?ric=ricsim_g1_6 4 120
    api_equal json:policy_types?ric=ricsim_g1_7 3 120
    api_equal json:policy_types?ric=ricsim_g1_8 2 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:???? \
                             ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:1,2:???? \
                             ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:1,2,3:???? \
                             ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:1,2,3,4:???? \
                             ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:1,2,3,4,5:???? \
                             ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:2,3,4,5:???? \
                             ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:3,4,5:???? \
                             ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:4,5:???? "


    #Load config with all rics
    consul_config_app                  ".consul_config_all.json"

    api_equal json:rics 10 120

    echo "Check the number of types in the agent for each ric"
    api_equal json:policy_types?ric=ricsim_g1_1 1 120
    api_equal json:policy_types?ric=ricsim_g1_2 2 120
    api_equal json:policy_types?ric=ricsim_g1_3 3 120
    api_equal json:policy_types?ric=ricsim_g1_4 4 120
    api_equal json:policy_types?ric=ricsim_g1_5 5 120
    api_equal json:policy_types?ric=ricsim_g1_6 4 120
    api_equal json:policy_types?ric=ricsim_g1_7 3 120
    api_equal json:policy_types?ric=ricsim_g1_8 2 120
    api_equal json:policy_types?ric=ricsim_g1_9 1 120
    api_equal json:policy_types?ric=ricsim_g1_10 0 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:???? \
                             ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:1,2:???? \
                             ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:1,2,3:???? \
                             ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:1,2,3,4:???? \
                             ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:1,2,3,4,5:???? \
                             ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:2,3,4,5:???? \
                             ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:3,4,5:???? \
                             ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:4,5:???? \
                             ricsim_g1_9:me1_ricsim_g1_9,me2_ricsim_g1_9:5:???? \
                             ricsim_g1_10:me1_ricsim_g1_10,me2_ricsim_g1_10:NOTYPE:???? "


    #No policy type in sim #10

    api_equal json:policy_types 5

    api_put_service 201 "serv1" 3600 "$CR_PATH/serv1"

    api_put_policy 201 "serv1" ricsim_g1_9 5 2000 NOTRANSIENT testdata/OSC/pi5_template.json 1

    api_equal json:policy_ids 1

    sim_equal ricsim_g1_9 num_instances 1


    # Load config with reduced number of rics
    consul_config_app                  ".consul_config_2.json"

    api_equal json:rics 8 120

    echo "Check the number of types in the agent for each ric"
    api_equal json:policy_types?ric=ricsim_g1_1 1 120
    api_equal json:policy_types?ric=ricsim_g1_2 2 120
    api_equal json:policy_types?ric=ricsim_g1_3 3 120
    api_equal json:policy_types?ric=ricsim_g1_4 4 120
    api_equal json:policy_types?ric=ricsim_g1_5 5 120
    api_equal json:policy_types?ric=ricsim_g1_6 4 120
    api_equal json:policy_types?ric=ricsim_g1_7 3 120
    api_equal json:policy_types?ric=ricsim_g1_8 2 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:???? \
                             ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:1,2:???? \
                             ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:1,2,3:???? \
                             ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:1,2,3,4:???? \
                             ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:1,2,3,4,5:???? \
                             ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:2,3,4,5:???? \
                             ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:3,4,5:???? \
                             ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:4,5:???? "

    sleep 120

    api_equal json:policy_ids 0

    api_get_policy_types 404 ricsim_g1_9

    sim_equal ricsim_g1_9 num_instances 1

    api_delete_policy 404 2000

    # Load config with all rics
    consul_config_app                  ".consul_config_all.json"

    api_equal json:rics 10 120

    echo "Check the number of types in the agent for each ric"
    api_equal json:policy_types?ric=ricsim_g1_1 1 120
    api_equal json:policy_types?ric=ricsim_g1_2 2 120
    api_equal json:policy_types?ric=ricsim_g1_3 3 120
    api_equal json:policy_types?ric=ricsim_g1_4 4 120
    api_equal json:policy_types?ric=ricsim_g1_5 5 120
    api_equal json:policy_types?ric=ricsim_g1_6 4 120
    api_equal json:policy_types?ric=ricsim_g1_7 3 120
    api_equal json:policy_types?ric=ricsim_g1_8 2 120
    api_equal json:policy_types?ric=ricsim_g1_9 1 120
    api_equal json:policy_types?ric=ricsim_g1_10 0 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:???? \
                             ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:1,2:???? \
                             ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:1,2,3:???? \
                             ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:1,2,3,4:???? \
                             ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:1,2,3,4,5:???? \
                             ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:2,3,4,5:???? \
                             ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:3,4,5:???? \
                             ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:4,5:???? \
                             ricsim_g1_9:me1_ricsim_g1_9,me2_ricsim_g1_9:5:???? \
                             ricsim_g1_10:me1_ricsim_g1_10,me2_ricsim_g1_10:NOTYPE:???? "

    sleep 120

    api_equal json:policy_ids 0

    sim_equal ricsim_g1_9 num_instances 0


    sim_delete_policy_type 204 ricsim_g1_4 4
    sim_delete_policy_type 204 ricsim_g1_5 4
    sim_delete_policy_type 204 ricsim_g1_6 4
    sim_delete_policy_type 204 ricsim_g1_7 4

    sleep 120

    api_equal json:policy_types?ric=ricsim_g1_1 1 120
    api_equal json:policy_types?ric=ricsim_g1_2 2 120
    api_equal json:policy_types?ric=ricsim_g1_3 3 120
    api_equal json:policy_types?ric=ricsim_g1_4 3 120
    api_equal json:policy_types?ric=ricsim_g1_5 4 120
    api_equal json:policy_types?ric=ricsim_g1_6 3 120
    api_equal json:policy_types?ric=ricsim_g1_7 2 120
    api_equal json:policy_types?ric=ricsim_g1_8 2 120
    api_equal json:policy_types?ric=ricsim_g1_9 1 120
    api_equal json:policy_types?ric=ricsim_g1_10 0 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:???? \
                             ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:1,2:???? \
                             ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:1,2,3:???? \
                             ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:1,2,3:???? \
                             ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:1,2,3,5:???? \
                             ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:2,3,5:???? \
                             ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:3,5:???? \
                             ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:4,5:???? \
                             ricsim_g1_9:me1_ricsim_g1_9,me2_ricsim_g1_9:5:???? \
                             ricsim_g1_10:me1_ricsim_g1_10,me2_ricsim_g1_10:NOTYPE:???? "

    sim_delete_policy_type 204 ricsim_g1_8 4

    api_equal json:policy_types?ric=ricsim_g1_8 1 120

    api_get_rics 200 NOTYPE "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1:???? \
                            ricsim_g1_2:me1_ricsim_g1_2,me2_ricsim_g1_2:1,2:???? \
                            ricsim_g1_3:me1_ricsim_g1_3,me2_ricsim_g1_3:1,2,3:???? \
                            ricsim_g1_4:me1_ricsim_g1_4,me2_ricsim_g1_4:1,2,3:???? \
                            ricsim_g1_5:me1_ricsim_g1_5,me2_ricsim_g1_5:1,2,3,5:???? \
                            ricsim_g1_6:me1_ricsim_g1_6,me2_ricsim_g1_6:2,3,5:???? \
                            ricsim_g1_7:me1_ricsim_g1_7,me2_ricsim_g1_7:3,5:???? \
                            ricsim_g1_8:me1_ricsim_g1_8,me2_ricsim_g1_8:5:???? \
                            ricsim_g1_9:me1_ricsim_g1_9,me2_ricsim_g1_9:5:???? \
                            ricsim_g1_10:me1_ricsim_g1_10,me2_ricsim_g1_10:NOTYPE:???? "



    check_policy_agent_logs
    store_logs          ${interface}
done





#### TEST COMPLETE ####


print_result

auto_clean_containers