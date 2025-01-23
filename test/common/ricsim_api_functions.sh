#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions and test functions for RICSIM A1 simulators

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__RICSIM_imagesetup() {
    __check_and_create_image_var RICSIM "RIC_SIM_IMAGE" "RIC_SIM_IMAGE_BASE" "RIC_SIM_IMAGE_TAG" $1 "$RIC_SIM_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__RICSIM_imagepull() {
    __check_and_pull_image $1 "$RIC_SIM_DISPLAY_NAME" $RIC_SIM_PREFIX"_"$RIC_SIM_BASE RIC_SIM_IMAGE
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__RICSIM_image_data() {
    echo -e "$RIC_SIM_DISPLAY_NAME\t$(docker images --format $1 $RIC_SIM_IMAGE)" >>   $2
    if [ ! -z "$RIC_SIM_IMAGE_SOURCE" ]; then
        echo -e "-- source image --\t$(docker images --format $1 $RIC_SIM_IMAGE_SOURCE)" >>   $2
    fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__RICSIM_kube_scale_zero() {
    __kube_scale_all_resources $KUBE_A1SIM_NAMESPACE autotest RICSIM
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__RICSIM_kube_scale_zero_and_wait() {
    #__kube_scale_and_wait_all_resources $KUBE_A1SIM_NAMESPACE app $KUBE_A1SIM_NAMESPACE"-"$RIC_SIM_PREFIX
    __kube_scale_and_wait_all_resources $KUBE_A1SIM_NAMESPACE app $KUBE_A1SIM_NAMESPACE"-a1simulator"
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__RICSIM_kube_delete_all() {
    __kube_delete_all_resources $KUBE_A1SIM_NAMESPACE autotest RICSIM
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__RICSIM_store_docker_logs() {
    if [ $RUNMODE == "KUBE" ]; then
        for podname in $(kubectl $KUBECONF get pods -n $KUBE_A1SIM_NAMESPACE -l "autotest=RICSIM" -o custom-columns=":metadata.name"); do
            kubectl $KUBECONF logs -n $KUBE_A1SIM_NAMESPACE $podname --tail=-1 > $1$2_$podname.log 2>&1
        done
    else

        rics=$(docker ps --filter "name=$RIC_SIM_PREFIX" --filter "network=$DOCKER_SIM_NWNAME" --filter "status=running" --filter "label=a1sim" --format {{.Names}})
        for ric in $rics; do
            docker logs $ric > $1$2_$ric.log 2>&1
        done
    fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__RICSIM_initial_setup() {
    use_simulator_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__RICSIM_statistics_setup() {
    for ((RICSIM_INSTANCE=10; RICSIM_INSTANCE>0; RICSIM_INSTANCE-- )); do
        if [ $RUNMODE == "KUBE" ]; then
            RICSIM_INSTANCE_KUBE=$(($RICSIM_INSTANCE-1))
            echo -n " RICSIM_G1_$RICSIM_INSTANCE_KUBE ${RIC_SIM_PREFIX}-g1-$RICSIM_INSTANCE_KUBE $KUBE_A1SIM_NAMESPACE "
            echo -n " RICSIM_G2_$RICSIM_INSTANCE_KUBE ${RIC_SIM_PREFIX}-g2-$RICSIM_INSTANCE_KUBE $KUBE_A1SIM_NAMESPACE "
            echo -n " RICSIM_G3_$RICSIM_INSTANCE_KUBE ${RIC_SIM_PREFIX}-g3-$RICSIM_INSTANCE_KUBE $KUBE_A1SIM_NAMESPACE "
        else
            echo -n " RICSIM_G1_$RICSIM_INSTANCE ${RIC_SIM_PREFIX}-g1-$RICSIM_INSTANCE "
            echo -n " RICSIM_G2_$RICSIM_INSTANCE ${RIC_SIM_PREFIX}-g2-$RICSIM_INSTANCE "
            echo -n " RICSIM_G3_$RICSIM_INSTANCE ${RIC_SIM_PREFIX}-g3-$RICSIM_INSTANCE "
        fi
    done
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__RICSIM_test_requirements() {
    :
}

#######################################################


RIC_SIM_HTTPX="http"
RIC_SIM_PORT=$RIC_SIM_INTERNAL_PORT


#Vars for container count
G1_COUNT=0
G2_COUNT=0
G3_COUNT=0
G4_COUNT=0
G5_COUNT=0


###########################
### RIC Simulator functions
###########################

use_simulator_http() {
    echo -e $BOLD"RICSIM protocol setting"$EBOLD
    echo -e " Using $BOLD http $EBOLD towards the simulators"
    RIC_SIM_HTTPX="http"
    RIC_SIM_PORT=$RIC_SIM_INTERNAL_PORT
    echo ""
}

use_simulator_https() {
    echo -e $BOLD"RICSIM protocol setting"$EBOLD
    echo -e " Using $BOLD https $EBOLD towards the simulators"
    RIC_SIM_HTTPX="https"
    RIC_SIM_PORT=$RIC_SIM_INTERNAL_SECURE_PORT
    echo ""
}

# Start one group (ricsim_g1, ricsim_g2 .. ricsim_g5) with a number of RIC Simulators using a given A interface
# 'ricsim' may be set on command line to other prefix
# args:  ricsim_g1|ricsim_g2|ricsim_g3|ricsim_g4|ricsim_g5 <count> <interface-id>
# (Function for test scripts)
start_ric_simulators() {

    echo -e $BOLD"Starting $RIC_SIM_DISPLAY_NAME"$EBOLD

    if [ $RUNMODE == "KUBE" ]; then

        # Check if app shall be fully managed by the test script
        __check_included_image "RICSIM"
        retcode_i=$?

        # Check if app shall only be used by the test script
        __check_prestarted_image "RICSIM"
        retcode_p=$?

        if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
            echo -e $RED"The $1 app is not included as managed nor prestarted in this test script"$ERED
            echo -e $RED"The $1 will not be started"$ERED
            exit
        fi
        if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
            echo -e $RED"The $1 stub app is included both as managed and prestarted in this test script"$ERED
            echo -e $RED"The $1 will not be started"$ERED
            exit
        fi

        if [ $retcode_p -eq 0 ]; then
            echo -e " Using existing $1 statefulset and service"
            echo " Using existing simulator deployment and service for statefulset $1"
            echo " Setting $1 replicas=$2"
            __kube_scale statefulset $1 $KUBE_A1SIM_NAMESPACE $2
            echo ""
            return
        fi
    fi

    RIC1=$RIC_SIM_PREFIX"_g1"
    RIC2=$RIC_SIM_PREFIX"_g2"
    RIC3=$RIC_SIM_PREFIX"_g3"
    RIC4=$RIC_SIM_PREFIX"_g4"
    RIC5=$RIC_SIM_PREFIX"_g5"

    if [ $# != 3 ]; then
        ((RES_CONF_FAIL++))
        __print_err "need three args,  $RIC1|$RIC2|$RIC3|$RIC4|$RIC5 <count> <interface-id>" $@
        exit 1
    fi

    echo " $2 simulators using basename: $1 on interface: $3"
    #Set env var for simulator count and A1 interface version for the given group
    if [ $1 == "$RIC1" ]; then
        G1_COUNT=$2
    elif [ $1 == "$RIC2" ]; then
        G2_COUNT=$2
    elif [ $1 == "$RIC3" ]; then
        G3_COUNT=$2
    elif [ $1 == "$RIC4" ]; then
        G4_COUNT=$2
    elif [ $1 == "$RIC5" ]; then
        G5_COUNT=$2
    else
        ((RES_CONF_FAIL++))
        __print_err "need three args, $RIC1|$RIC2|$RIC3|$RIC4|$RIC5 <count> <interface-id>" $@
        exit 1
    fi

    if [ $RUNMODE == "KUBE" ]; then

        if [ $retcode_i -eq 0 ]; then

            #export needed env var for statefulset
            export RIC_SIM_SET_NAME=$(echo "$1" | tr '_' '-')  #kube does not accept underscore in names
            export KUBE_A1SIM_NAMESPACE
            export RIC_SIM_IMAGE
            #Adding 1 more instance, instance 0 is never used. This is done to keep test scripts compatible
            # with docker that starts instance index on 1.....
            export RIC_SIM_COUNT=$(($2+1))
            export A1_VERSION=$3
            export RIC_SIM_INTERNAL_PORT
            export RIC_SIM_INTERNAL_SECURE_PORT

            echo -e " Creating $RIC_SIM_PREFIX app and expose service"

            #Check if nonrtric namespace exists, if not create it
            __kube_create_namespace $KUBE_A1SIM_NAMESPACE

            # Create service
            input_yaml=$SIM_GROUP"/"$RIC_SIM_COMPOSE_DIR"/"svc.yaml
            output_yaml=$PWD/tmp/ric_${1}_svc.yaml
            __kube_create_instance service $RIC_SIM_SET_NAME $input_yaml $output_yaml

            # Create app
            input_yaml=$SIM_GROUP"/"$RIC_SIM_COMPOSE_DIR"/"app.yaml
            output_yaml=$PWD/tmp/ric_${1}_app.yaml
            __kube_create_instance app $RIC_SIM_SET_NAME $input_yaml $output_yaml

            #Using only instance from index 1 to keep compatability with docker
            for (( count=1; count<${RIC_SIM_COUNT}; count++ )); do
                host=$(__find_sim_host $RIC_SIM_SET_NAME"-"$count)
                __check_service_start $RIC_SIM_SET_NAME"-"$count $host"/"
            done
        fi
    else
        __check_included_image 'RICSIM'
        if [ $? -eq 1 ]; then
            echo -e $RED"The Near-RT RIC Simulator app is not included as managed in this test script"$ERED
            echo -e $RED"Near-RT RIC Simulator will not be started"$ERED
            exit 1
        fi

        # Create .env file to compose project, all ric container will get this prefix
        echo "COMPOSE_PROJECT_NAME="$RIC_SIM_PREFIX > $SIM_GROUP/$RIC_SIM_COMPOSE_DIR/.env

        #extract service name (group), g1, g2, g3, g4 or g5 from var $1
        #E.g. ricsim_g1 -> g1 is the service name
        TMP_GRP=$1
        RICSIM_COMPOSE_SERVICE_NAME=$(echo "${TMP_GRP##*_}")

        export RICSIM_COMPOSE_A1_VERSION=$3
        export RICSIM_COMPOSE_SERVICE_NAME
        export RIC_SIM_INTERNAL_PORT
        export RIC_SIM_INTERNAL_SECURE_PORT
        export RIC_SIM_CERT_MOUNT_DIR
        export DOCKER_SIM_NWNAME
        export RIC_SIM_DISPLAY_NAME

        echo -e $BOLD$YELLOW" Warning: Using docker compose --force-recreate "$EYELLOW$EBOLD
        docker_args="--force-recreate --scale $RICSIM_COMPOSE_SERVICE_NAME=$2"

        #Create a list of container names
        #Will be <ricsim-prefix>-<service-name>-<index>
        app_data=""
        cntr=1
        app_name_prefix=$RIC_SIM_PREFIX"-"$RICSIM_COMPOSE_SERVICE_NAME"-"
        while [ $cntr -le $2 ]; do
            app=$app_name_prefix$cntr
            app_data="$app_data $app"
            let cntr=cntr+1
        done

        __start_container $RIC_SIM_COMPOSE_DIR "" "$docker_args" $2 $app_data

        cntr=1
        while [ $cntr -le $2 ]; do
            app=$RIC_SIM_PREFIX"-"$RICSIM_COMPOSE_SERVICE_NAME"-"$cntr
            __check_service_start $app $RIC_SIM_HTTPX"://"$app:$RIC_SIM_PORT$RIC_SIM_ALIVE_URL
            let cntr=cntr+1
        done

    fi
    echo ""
    return 0
}

# Translate ric name to kube host name
# args: <ric-name>
# For test scripts
get_kube_sim_host() {
    name=$(echo "$1" | tr '_' '-')  #kube does not accept underscore in names
    #example gnb_1_2 -> gnb-1-2
    set_name=$(echo $name | rev | cut -d- -f2- | rev) # Cut index part of ric name to get the name of statefulset
    # example gnb-g1-2 -> gnb-g1 where gnb-g1-2 is the ric name and gnb-g1 is the set name
    echo $name"."$set_name"."$KUBE_A1SIM_NAMESPACE
}

# Helper function to get a the port and host name of a specific ric simulator
# args: <ric-id>
# (Not for test scripts)
__find_sim_host() {
    if [ $RUNMODE == "KUBE" ]; then
        ricname=$(echo "$1" | tr '_' '-') # Kube does not accept underscore in names as docker do
        if [ -z "$RIC_SIM_COMMON_SVC_NAME" ]; then
            ric_setname="${ricname%-*}"  #Extract the stateful set name
        else
            ric_setname=$RIC_SIM_COMMON_SVC_NAME # Use the common svc name in the host name of the sims
        fi
        echo $RIC_SIM_HTTPX"://"$ricname.$ric_setname.$KUBE_A1SIM_NAMESPACE":"$RIC_SIM_PORT
    else
        ricname=$(echo "$1" | tr '_' '-')
        echo $RIC_SIM_HTTPX"://"$ricname":"$RIC_SIM_PORT

    fi
}

# Generate a UUID to use as prefix for policy ids
sim_generate_policy_uuid() {
    UUID=$(python3 -c 'import sys,uuid; sys.stdout.write(uuid.uuid4().hex)')
    #Reduce length to make space for serial id, uses 'a' as marker where the serial id is added
    UUID=${UUID:0:${#UUID}-4}"a"
}

# Execute a curl cmd towards a ricsimulator and check the response code.
# args: <expected-response-code> <curl-cmd-string>
__execute_curl_to_sim() {
    echo ${FUNCNAME[1]} "line: "${BASH_LINENO[1]} >> $HTTPLOG
    proxyflag=""
    if [ ! -z "$KUBE_PROXY_PATH" ]; then
        if [ $KUBE_PROXY_HTTPX == "http" ]; then
            proxyflag=" --proxy $KUBE_PROXY_PATH"
        else
            proxyflag=" --proxy-insecure --proxy $KUBE_PROXY_PATH"
        fi
    fi
    if [ -z "$KUBE_PROXY_CURL_JWT" ]; then
        echo " CMD: $2 $proxyflag" >> $HTTPLOG
        res="$($2 $proxyflag)"
    else
        echo " CMD: $2 $proxyflag -H Authorization: Bearer $KUBE_PROXY_CURL_JWT" >> $HTTPLOG
        res=$($2 $proxyflag -H 'Authorization: Bearer '$KUBE_PROXY_CURL_JWT)
    fi

    echo " RESP: $res" >> $HTTPLOG
    retcode=$?
    if [ $retcode -ne 0 ]; then
        ((RES_CONF_FAIL++))
        echo " RETCODE: "$retcode
        echo -e $RED" FAIL - fatal error when executing curl."$ERED
        return 1
    fi
    status=${res:${#res}-3}
    if [ $status -eq $1 ]; then
        echo -e $GREEN" OK"$EGREEN
        return 0
    fi
    echo -e $RED" FAIL - expected http response: "$1" but got http response: "$status $ERED
    ((RES_CONF_FAIL++))
    return 1
}

# Tests if a variable value in the ricsimulator is equal to a target value and and optional timeout.
# Arg: <ric-id> <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <ric-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
sim_equal() {

    if [ $# -eq 3 ] || [ $# -eq 4 ]; then
        host=$(__find_sim_host $1)
        __var_test $1 "$host/counter/" $2 "=" $3 $4
        return 0
    else
        __print_err "needs three or four args: <ric-id> <sim-param> <target-value> [ timeout ]"
        return 1
    fi
}

# Print a variable value from the RIC sim.
# args: <ric-id> <variable-name>
# (Function for test scripts)
sim_print() {

    if [ $# != 2 ]; then
        __print_err "need two args, <ric-id> <sim-param>" $@
        exit 1
    fi
    host=$(__find_sim_host $1)
    echo -e $BOLD"INFO(${BASH_LINENO[0]}): $1, $2 = $(__do_curl $host/counter/$2)"$EBOLD
}

# Tests if a variable value in the RIC simulator contains the target string and and optional timeout
# Arg: <ric-id> <variable-name> <target-value> - This test set pass or fail depending on if the variable contains
# the target or not.
# Arg: <ric-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value contains the target
# value or not.
# (Function for test scripts)
sim_contains_str() {

    if [ $# -eq 3 ] || [ $# -eq 4 ]; then
        host=$(__find_sim_host $1)
        __var_test $1 "$host/counter/" $2 "contain_str" $3 $4
        return 0
    else
        __print_err "needs three or four args: <ric-id> <sim-param> <target-value> [ timeout ]"
        return 1
    fi
}

# Simulator API: Put a policy type in a ric
# args: <response-code> <ric-id> <policy-type-id> <policy-type-file>
# (Function for test scripts)
sim_put_policy_type() {
    __log_conf_start $@
    if [ $# -ne 4 ]; then
        __print_err "<response-code> <ric-id> <policy-type-id> <policy-type-file>" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    curlString="curl -X PUT -skw %{http_code} "$host"/policytype?id="$3" -H Content-Type:application/json --data-binary @"$4
    __execute_curl_to_sim $1 "$curlString"
    return $?
}

# Simulator API: Delete a policy type in a ric
# <response-code> <ric-id> <policy-type-id>
# (Function for test scripts)
sim_delete_policy_type() {
    __log_conf_start $@
    if [ $# -ne 3 ]; then
        __print_err "<response-code> <ric-id> <policy_type_id>" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    curlString="curl -X DELETE -skw %{http_code} "$host"/policytype?id="$3
    __execute_curl_to_sim $1 "$curlString"
    return $?
}

# Simulator API: Delete instances (and status), for one ric
# <response-code> <ric-id>
# (Function for test scripts)
sim_post_delete_instances() {
    __log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<response-code> <ric-id>" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    curlString="curl -X POST -skw %{http_code} "$host"/deleteinstances"
    __execute_curl_to_sim $1 "$curlString"
    return $?
}

# Simulator API: Delete all (instances/types/statuses/settings), for one ric
# <response-code> <ric-id>
# (Function for test scripts)
sim_post_delete_all() {
    __log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<response-code> <numericic-id>" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    curlString="curl -X POST -skw %{http_code} "$host"/deleteall"
    __execute_curl_to_sim $1 "$curlString"
    return $?
}

# Simulator API: Set (or reset) response code for next A1 message, for one ric
# <response-code> <ric-id> [<forced_response_code>]
# (Function for test scripts)
sim_post_forcedresponse() {
    __log_conf_start $@
    if [ $# -ne 3 ]; then
        __print_err "<response-code> <ric-id> <forced_response_code>" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    curlString="curl -X POST -skw %{http_code} "$host"/forceresponse"
    if [ $# -eq 3 ]; then
        curlString=$curlString"?code="$3
    fi
    __execute_curl_to_sim $1 "$curlString"
    return $?
}

# Simulator API: Set (or reset) A1 response delay, for one ric
# <response-code> <ric-id> [<delay-in-seconds>]
# (Function for test scripts)
sim_post_forcedelay() {
    __log_conf_start $@
    if [ $# -ne 3 ]; then
        __print_err "<response-code> <ric-id> [<delay-in-seconds>]" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    curlString="curl -X POST -skw %{http_code} $host/forcedelay"
    if [ $# -eq 3 ]; then
        curlString=$curlString"?delay="$3
    fi
    __execute_curl_to_sim $1 "$curlString"
    return $?
}

# Simulator API: Set status and reason for a policy, for one ric
# args: <response-code> <ric-id> <policy-id> <status> <reason>
# (Function for test scripts)
sim_put_policy_status() {
    __log_conf_start $@
    if [ $# -ne 5 ]; then
        __print_err "<response-code> <ric-id> <policy-id> <status> <reason>" $@
        return 1
    fi
    host=$(__find_sim_host $2)
    pid=$3
    curlString="curl -X PUT -skw %{http_code} $host/status?policyid=$UUID$pid&status=$4&reason=$5 -H Content-Type:application/json"
    __execute_curl_to_sim $1 "$curlString"
    return $?
}
