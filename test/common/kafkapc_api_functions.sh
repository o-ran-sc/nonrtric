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

# This is a script that contains container/service management functions for Kafka producer/consumer

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KAFKAPC_imagesetup() {
	__check_and_create_image_var KAFKAPC "KAFKAPC_IMAGE" "KAFKAPC_IMAGE_BASE" "KAFKAPC_IMAGE_TAG" LOCAL "$KAFKAPC_DISPLAY_NAME" $IMAGE_TARGET_PLATFORM_IMG_TAG
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__KAFKAPC_imagepull() {
	__check_and_pull_image $2 "$KAFKAPC_DISPLAY_NAME" $KAFKAPC_APP_NAME KAFKAPC_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KAFKAPC_imagebuild() {

	cd ../$KAFKAPC_BUILD_DIR
	echo " Building KAFKAPC - $KAFKAPC_DISPLAY_NAME - image: $KAFKAPC_IMAGE"
	docker build  $IMAGE_TARGET_PLATFORM_CMD_PARAM --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $KAFKAPC_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN"  Build Ok"$EGREEN
		__retag_and_push_image KAFKAPC_IMAGE
		if [ $? -ne 0 ]; then
			exit 1
		fi
	else
		echo -e $RED"  Build Failed"$ERED
		((RES_CONF_FAIL++))
		cat .dockererr
		echo -e $RED"Exiting...."$ERED
		exit 1
	fi
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__KAFKAPC_image_data() {
	echo -e "$KAFKAPC_DISPLAY_NAME\t$(docker images --format $1 $KAFKAPC_IMAGE)" >>   $2
	if [ ! -z "$KAFKAPC_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $KAFKAPC_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__KAFKAPC_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest KAFKAPC
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__KAFKAPC_kube_scale_zero_and_wait() {
	echo -e $RED" KAFKAPC app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__KAFKAPC_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest KAFKAPC
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__KAFKAPC_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=KAFKAPC" -n $KUBE_SIM_NAMESPACE --tail=-1 > $1$2_kafkapc.log 2>&1
	else
		docker logs $KAFKAPC_APP_NAME > $1$2_kafkapc.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__KAFKAPC_initial_setup() {
	use_kafkapc_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__KAFKAPC_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "KAFKAPC $KAFKAPC_APP_NAME $KUBE_SIM_NAMESPACE"
	else
		echo "KAFKAPC $KAFKAPC_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__KAFKAPC_test_requirements() {
	:
}

#######################################################

#######################################################

# Set http as the protocol to use for all communication to the Kafka procon
# args: -
# (Function for test scripts)
use_kafkapc_http() {
	__kafkapc_set_protocoll "http" $KAFKAPC_INTERNAL_PORT $KAFKAPC_EXTERNAL_PORT
}

# Set httpS as the protocol to use for all communication to the Kafka procon
# args: -
# (Function for test scripts)
use_kafkapc_https() {
	__kafkapc_set_protocoll "https" $KAFKAPC_INTERNAL_SECURE_PORT $KAFKAPC_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__kafkapc_set_protocoll() {
	echo -e $BOLD"$KAFKAPC_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $KAFKAPC_DISPLAY_NAME"

	## Access to Kafka procon

	KAFKAPC_SERVICE_PATH=$1"://"$KAFKAPC_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		KAFKAPC_SERVICE_PATH=$1"://"$KAFKAPC_APP_NAME.$KUBE_SIM_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	KAFKAPC_ADAPTER_TYPE="REST"
	KAFKAPC_ADAPTER=$KAFKAPC_SERVICE_PATH

	echo ""
}

### Admin API functions Kafka procon

###########################
### Kafka Procon functions
###########################

# Export env vars for config files, docker compose and kube resources
# args:
__kafkapc_export_vars() {
	export KAFKAPC_APP_NAME
	export KAFKAPC_DISPLAY_NAME

	export DOCKER_SIM_NWNAME
	export KUBE_SIM_NAMESPACE

	export KAFKAPC_IMAGE
	export KAFKAPC_INTERNAL_PORT
	export KAFKAPC_INTERNAL_SECURE_PORT
	export KAFKAPC_EXTERNAL_PORT
	export KAFKAPC_EXTERNAL_SECURE_PORT

	export MR_KAFKA_SERVICE_PATH
}


# Start the Kafka procon in the simulator group
# args: -
# (Function for test scripts)
start_kafkapc() {

	echo -e $BOLD"Starting $KAFKAPC_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "KAFKAPC"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "KAFKAPC"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $ICS_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $ICS_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $ICS_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $ICS_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $KAFKAPC_APP_NAME deployment and service"
			echo " Setting RC replicas=1"
			__kube_scale deployment $KAFKAPC_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $KAFKAPC_APP_NAME deployment and service"

            __kube_create_namespace $KUBE_SIM_NAMESPACE

			__kafkapc_export_vars

			# Create service
			input_yaml=$SIM_GROUP"/"$KAFKAPC_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/kafkapc_svc.yaml
			__kube_create_instance service $KAFKAPC_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$KAFKAPC_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/kafkapc_app.yaml
			__kube_create_instance app $KAFKAPC_APP_NAME $input_yaml $output_yaml
		fi

		__check_service_start $KAFKAPC_APP_NAME $KAFKAPC_SERVICE_PATH$KAFKAPC_ALIVE_URL

	else

		# Check if docker app shall be fully managed by the test script
		__check_included_image 'KAFKAPC'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Kafka procon app is not included as managed in this test script"$ERED
			echo -e $RED"The Kafka procon will not be started"$ERED
			exit
		fi

		__kafkapc_export_vars

		__start_container $KAFKAPC_COMPOSE_DIR "" NODOCKERARGS 1 $KAFKAPC_APP_NAME

        __check_service_start $KAFKAPC_APP_NAME $KAFKAPC_SERVICE_PATH$KAFKAPC_ALIVE_URL
	fi
    echo ""
    return 0
}

# Tests if a variable value in the KAFPAPC is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
kafkapc_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test KAFPAPC "$KAFKAPC_SERVICE_PATH/" $1 "=" $2 $3
	else
		__print_err "Wrong args to kafkapc_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}

# KAFKA PC API: Reset all, POST /reset
# Arg: <response-code>
# (Function for test scripts)
kafkapc_api_reset() {
	__log_conf_start $@

	if [ $# -ne 1 ]; then
		__print_err "<response-code>" $@
		return 1
	fi

	res="$(__do_curl_to_api KAFKAPC POST /reset)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_conf_fail_status_code $1 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# KAFKA PC API: Create a topic of a data-type, PUT /topics/<topic>
# Arg: <response-code> <topic-name>  <mime-type>
# (Function for test scripts)
kafkapc_api_create_topic() {
	__log_conf_start $@

    if [ $# -ne 3 ]; then
        __print_err "<response-code> <topic-name>  <mime-type>" $@
        return 1
	fi

	res="$(__do_curl_to_api KAFKAPC PUT /topics/$2?type=$3)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_conf_fail_status_code $1 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# KAFKA PC API: Get topics, GET /topics
# args: <response-code> [ EMPTY | [<topic>]+ ]
# (Function for test scripts)
kafkapc_api_get_topics() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> EMPTY | [<policy-type-id>]*" $@
		return 1
	fi

    res="$(__do_curl_to_api KAFKAPC GET /topics)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:2} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "EMPTY" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done
		targetJson=$targetJson"]"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi
	__log_test_pass
	return 0
}

# KAFKA PC API: Get a topic, GET /topic/<topic>
# args: <response-code> <topic> <mime-type>
# (Function for test scripts)
kafkapc_api_get_topic() {
	__log_test_start $@

    if [ $# -ne 3 ]; then
		__print_err "<response-code> <topic> <mime-type>" $@
		return 1
	fi

    res="$(__do_curl_to_api KAFKAPC GET /topics/$2)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	body=${res:0:${#res}-3}
	if [ "$body" != $3 ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Start sending on a topic, POST /topic/<topic>/startsend
# args: <response-code> <topic>
# (Function for test scripts)
kafkapc_api_start_sending() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <topic>" $@
		return 1
	fi

    res="$(__do_curl_to_api KAFKAPC POST /topics/$2/startsend)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Start receiving on a topic, POST /topic/<topic>/startreceive
# args: <response-code> <topic>
# (Function for test scripts)
kafkapc_api_start_receiving() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <topic>" $@
		return 1
	fi

    res="$(__do_curl_to_api KAFKAPC POST /topics/$2/startreceive)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Stop sending on a topic, POST /topic/<topic>/stopsend
# args: <response-code> <topic>
# (Function for test scripts)
kafkapc_api_stop_sending() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <topic>" $@
		return 1
	fi

    res="$(__do_curl_to_api KAFKAPC POST /topics/$2/stopsend)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Stop receiving on a topic, POST /topic/<topic>/stopreceive
# args: <response-code> <topic>
# (Function for test scripts)
kafkapc_api_stop_receiving() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <topic>" $@
		return 1
	fi

    res="$(__do_curl_to_api KAFKAPC POST /topics/$2/stopreceive)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Send a message on a topic, POST /topic/<topic>/msg
# args: <response-code> <topic> <mime-type> <msg>
# (Function for test scripts)
kafkapc_api_post_msg() {
	__log_test_start $@

    if [ $# -ne 4 ]; then
		__print_err "<response-code> <topic> <mime-type> <msg>" $@
		return 1
	fi
	payload="tmp/.kafkapayload"
	echo -n $4 > $payload     #-n prevent a newline to be added...
    res="$(__do_curl_to_api KAFKAPC POST /topics/$2/msg $payload $3)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}


# KAFKA PC API: Get a msg on a topic, GET /topic/<topic>/msg
# args: <response-code> <topic>  ([ <mime-type>  <msg> ] | NOMSG )
# (Function for test scripts)
kafkapc_api_get_msg() {
	__log_test_start $@

    if [ $# -lt 3 ]; then
		__print_err "<response-code> <topic>  ([ <mime-type>  <msg> ] | NOMSG )" $@
		return 1
	fi
	mime_type="text/plain"
	if [ ! -z "$3" ]; then
		mime_type=$3
	fi
    res="$(__do_curl_to_api KAFKAPC GET /topics/$2/msg)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -eq 4 ]; then
		body=${res:0:${#res}-3}
		if [ "$body" != "$4" ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Send a message from a file on a topic, POST /topic/<topic>/msg
# args: <response-code> <topic> <mime-type> <file>
# (Function for test scripts)
kafkapc_api_post_msg_from_file() {
	__log_test_start $@

    if [ $# -ne 4 ]; then
		__print_err "<response-code> <topic> <mime-type> <file>" $@
		return 1
	fi
    res="$(__do_curl_to_api KAFKAPC POST /topics/$2/msg $4 $3)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# KAFKA PC API: Get a msg on a topic and compare with file, GET /topic/<topic>/msg
# args: <response-code> <topic>  <mime-type>  <file>
# (Function for test scripts)
kafkapc_api_get_msg_from_file() {
	__log_test_start $@

    if [ $# -ne 4 ]; then
		__print_err "<response-code> <topic>  <mime-type>  <file> " $@
		return 1
	fi

	if [ -f $4 ]; then
		msgfile=$(cat $4)
	else
		__log_test_fail_general "Message file "$4", does not exist"
		return 1
	fi

	mime_type="text/plain"

    res="$(__do_curl_to_api KAFKAPC GET /topics/$2/msg)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	body=${res:0:${#res}-3}
	if [ "$body" != "$msgfile" ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}


# Create json file for payload
# arg: <size-in-kb> <filename>
kafkapc_api_generate_json_payload_file() {
	__log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<topic-url> <json-file>" $@
        return 1
    fi
	if [ $1 -lt 1 ] || [ $1 -gt 10000 ]; then
		__log_conf_fail_general "Only size between 1k and 10000k supported"
		return 1
	fi
	echo -n "{\"abcdefghijklmno\":[" > $2
	LEN=$(($1*100-2))
	echo -n "\""ABCDEFG"\"" >> $2
	for ((idx=1; idx<$LEN; idx++))
	do
		echo -n ",\"ABCDEFG\"" >> $2
	done
	echo -n "]}" >> $2

	__log_conf_ok
	return 0
}

# Create text file for payload
# arg: <size-in-kb> <filename>
kafkapc_api_generate_text_payload_file() {
	__log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<topic-url> <text-file>" $@
        return 1
    fi
	if [ $1 -lt 1 ] || [ $1 -gt 10000 ]; then
		__log_conf_fail_general "Only size between 1k and 10000k supported"
		return 1
	fi
	echo -n "" > $2
	LEN=$(($1*100))
	for ((idx=0; idx<$LEN; idx++))
	do
		echo -n "ABCDEFGHIJ" >> $2
	done

	__log_conf_ok
	return 0
}