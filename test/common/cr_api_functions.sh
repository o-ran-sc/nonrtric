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

# This is a script that contains container/service management functions test functions for the Callback Receiver


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CR_imagesetup() {
	__check_and_create_image_var CR "CR_IMAGE" "CR_IMAGE_BASE" "CR_IMAGE_TAG" LOCAL "$CR_DISPLAY_NAME" $IMAGE_TARGET_PLATFORM_IMG_TAG
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CR_imagepull() {
	echo -e $RED" Image for app CR shall never be pulled from remote repo"$ERED
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CR_imagebuild() {
	cd ../cr
	echo " Building CR - $CR_DISPLAY_NAME - image: $CR_IMAGE"
	docker build $IMAGE_TARGET_PLATFORM_CMD_PARAM --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $CR_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN"  Build Ok"$EGREEN
		__retag_and_push_image CR_IMAGE
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
__CR_image_data() {
	echo -e "$CR_DISPLAY_NAME\t$(docker images --format $1 $CR_IMAGE)" >>   $2
	if [ ! -z "$CR_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $CR_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CR_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest CR
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__CR_kube_scale_zero_and_wait() {
	echo -e $RED" CR app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__CR_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest CR
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__CR_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		for podname in $(kubectl $KUBECONF get pods -n $KUBE_SIM_NAMESPACE -l "autotest=CR" -o custom-columns=":metadata.name"); do
			kubectl $KUBECONF logs -n $KUBE_SIM_NAMESPACE $podname --tail=-1 > $1$2_$podname.log 2>&1
		done
	else
		crs=$(docker ps --filter "name=$CR_APP_NAME" --filter "network=$DOCKER_SIM_NWNAME" --filter "status=running" --format {{.Names}})
		for crid in $crs; do
			docker logs $crid > $1$2_$crid.log 2>&1
		done
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__CR_initial_setup() {
	use_cr_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__CR_statistics_setup() {
	for ((CR_INSTANCE=MAX_CR_APP_COUNT; CR_INSTANCE>0; CR_INSTANCE-- )); do
		if [ $RUNMODE == "KUBE" ]; then
			CR_INSTANCE_KUBE=$(($CR_INSTANCE-1))
			echo -n " CR-$CR_INSTANCE_KUBE $CR_APP_NAME-$CR_INSTANCE_KUBE $KUBE_SIM_NAMESPACE "
		else
			echo -n " CR_$CR_INSTANCE ${CR_APP_NAME}-cr-$CR_INSTANCE "
		fi
	done
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__CR_test_requirements() {
	:
}

#######################################################

################
### CR functions
################

#Var to hold the current number of CR instances
CR_APP_COUNT=1
MAX_CR_APP_COUNT=10

# Set http as the protocol to use for all communication to the Dmaap adapter
# args: -
# (Function for test scripts)
use_cr_http() {
	__cr_set_protocoll "http" $CR_INTERNAL_PORT $CR_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Dmaap adapter
# args: -
# (Function for test scripts)
use_cr_https() {
	__cr_set_protocoll "https" $CR_INTERNAL_SECURE_PORT $CR_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__cr_set_protocoll() {

	echo -e $BOLD"$CR_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $CR_DISPLAY_NAME"
	## Access to Dmaap adapter
	for ((CR_INSTANCE=0; CR_INSTANCE<$MAX_CR_APP_COUNT; CR_INSTANCE++ )); do
		CR_DOCKER_INSTANCE=$(($CR_INSTANCE+1))
		# CR_SERVICE_PATH is the base path to cr
		__CR_SERVICE_PATH=$1"://"$CR_APP_NAME"-cr-"${CR_DOCKER_INSTANCE}":"$2  # docker access, container->container and script->container via proxy
		if [ $RUNMODE == "KUBE" ]; then
			__CR_SERVICE_PATH=$1"://"$CR_APP_NAME"-"$CR_INSTANCE.$CR_APP_NAME"."$KUBE_SIM_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
		fi
		export CR_SERVICE_PATH"_"${CR_INSTANCE}=$__CR_SERVICE_PATH
		# Service paths are used in test script to provide callbacck urls to app
		export CR_SERVICE_MR_PATH"_"${CR_INSTANCE}=$__CR_SERVICE_PATH$CR_APP_CALLBACK_MR  #Only for messages from dmaap adapter/mediator
		export CR_SERVICE_TEXT_PATH"_"${CR_INSTANCE}=$__CR_SERVICE_PATH$CR_APP_CALLBACK_TEXT  #Callbacks for text payload
		export CR_SERVICE_APP_PATH"_"${CR_INSTANCE}=$__CR_SERVICE_PATH$CR_APP_CALLBACK    #For general callbacks from apps

		if [ $CR_INSTANCE -eq 0 ]; then
			# CR_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
			# CR_ADDAPTER need to be set before each call to CR....only set for instance 0 here
			CR_ADAPTER_TYPE="REST"
			CR_ADAPTER=$__CR_SERVICE_PATH
		fi
	done
	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args: <proxy-flag>
__cr_export_vars() {
	export CR_APP_NAME
	export CR_DISPLAY_NAME

	export KUBE_SIM_NAMESPACE
	export DOCKER_SIM_NWNAME

	export CR_IMAGE

	export CR_INTERNAL_PORT
	export CR_INTERNAL_SECURE_PORT
	export CR_EXTERNAL_PORT
	export CR_EXTERNAL_SECURE_PORT

	export CR_APP_COUNT
}

# Start the Callback receiver in the simulator group
# args: <app-count>
# (Function for test scripts)
start_cr() {

	echo -e $BOLD"Starting $CR_DISPLAY_NAME"$EBOLD

	if [ $# -ne 1 ]; then
		echo -e $RED" Number of CR instances missing, usage: start_cr <app-count>"$ERED
		exit 1
	fi
	if [ $1 -lt 1 ] || [ $1 -gt 10 ]; then
		echo -e $RED" Number of CR shall be 1...10, usage: start_cr <app-count>"$ERED
		exit 1
	fi
	export CR_APP_COUNT=$1

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "CR"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "CR"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $CR_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $CR_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $CR_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $CR_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $CR_APP_NAME deployment and service"
			echo " Setting CR replicas=1"
			__kube_scale deployment $CR_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $CR_APP_NAME deployment and service"

			__cr_export_vars

			__kube_create_namespace $KUBE_SIM_NAMESPACE

			# Create service
			input_yaml=$SIM_GROUP"/"$CR_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/cr_svc.yaml
			__kube_create_instance service $CR_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$CR_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/cr_app.yaml
			__kube_create_instance app $CR_APP_NAME $input_yaml $output_yaml

		fi

		for ((CR_INSTANCE=0; CR_INSTANCE<$CR_APP_COUNT; CR_INSTANCE++ )); do
			__dynvar="CR_SERVICE_PATH_"$CR_INSTANCE
			__cr_app_name=$CR_APP_NAME"-"$CR_INSTANCE
			__check_service_start $__cr_app_name ${!__dynvar}$CR_ALIVE_URL
			result=$(__do_curl ${!__dynvar}/reset)
		done

	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'CR'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Callback Receiver app is not included in this test script"$ERED
			echo -e $RED"The Callback Receiver will not be started"$ERED
			exit
		fi

		__cr_export_vars

		app_data=""
		cntr=1
		while [ $cntr -le $CR_APP_COUNT ]; do
			app=$CR_APP_NAME"-cr-"$cntr
			app_data="$app_data $app"
			let cntr=cntr+1
		done

		echo "COMPOSE_PROJECT_NAME="$CR_APP_NAME > $SIM_GROUP/$CR_COMPOSE_DIR/.env

		__start_container $CR_COMPOSE_DIR "" NODOCKERARGS $CR_APP_COUNT $app_data

		cntr=1   #Counter for docker instance, starts on 1
		cntr2=0  #Couter for env var name, starts with 0 to be compablible with kube
		while [ $cntr -le $CR_APP_COUNT ]; do
			app=$CR_APP_NAME"-cr-"$cntr
			__dynvar="CR_SERVICE_PATH_"$cntr2
			__check_service_start $app ${!__dynvar}$CR_ALIVE_URL
			let cntr=cntr+1
			let cntr2=cntr2+1
		done
	fi
	echo ""
}

#Convert a cr path id to the value of the environment var holding the url
# arg: <cr-path-id>
# returns: <base-url-to-the-app>
__cr_get_service_path(){
	if [ $# -ne 1 ]; then
		echo "DUMMY"
		return 1
	fi
	if [ $1 -lt 0 ] || [ $1 -ge $MAX_CR_APP_COUNT ]; then
		echo "DUMMY"
		return 1
	fi
	__dynvar="CR_SERVICE_PATH_"$1
	echo ${!__dynvar}
	return 0
}

# Tests if a variable value in the CR is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <cr-path-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
cr_equal() {
	if [ $# -eq 3 ] || [ $# -eq 4 ]; then
		CR_SERVICE_PATH=$(__cr_get_service_path $1)
		CR_ADAPTER=$CR_SERVICE_PATH
		if [ $? -ne 0 ]; then
			__print_err "<cr-path-id> missing or incorrect" $@
			return 1
		fi
		__var_test "CR" "$CR_SERVICE_PATH/counter/" $2 "=" $3 $4
	else
		__print_err "Wrong args to cr_equal, needs three or four args: <cr-path-id>  <variable-name> <target-value> [ timeout ]" $@
	fi
}

# Tests if a variable value in the CR is equal to or greater than the target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <cr-path-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to or greater than the target
# value or not.
# (Function for test scripts)
cr_greater_or_equal() {
	if [ $# -eq 3 ] || [ $# -eq 4 ]; then
		CR_SERVICE_PATH=$(__cr_get_service_path $1)
		CR_ADAPTER=$CR_SERVICE_PATH
		if [ $? -ne 0 ]; then
			__print_err "<cr-path-id> missing or incorrect" $@
			return 1
		fi
		__var_test "CR" "$CR_SERVICE_PATH/counter/" $2 ">=" $3 $4
	else
		__print_err "Wrong args to cr_equal, needs three or four args: <cr-path-id>  <variable-name> <target-value> [ timeout ]" $@
	fi
}

# Tests if a variable value in the CR contains the target string and and optional timeout
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable contains
# the target or not.
# Arg: <cr-path-id> <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value contains the target
# value or not.
# (Function for test scripts)
cr_contains_str() {

	if [ $# -eq 3 ] || [ $# -eq 4 ]; then
		CR_SERVICE_PATH=$(__cr_get_service_path $1)
		CR_ADAPTER=$CR_SERVICE_PATH
		if [ $? -ne 0 ]; then
			__print_err "<cr-path-id> missing or incorrect" $@
			return 1
		fi
		__var_test "CR" "$CR_SERVICE_PATH/counter/" $2 "contain_str" $3 $4
		return 0
	else
		__print_err "needs two or three args: <cr-path-id> <variable-name> <target-value> [ timeout ]"
		return 1
	fi
}

# Read a variable value from CR sim and send to stdout. Arg: <cr-path-id> <variable-name>
cr_read() {
	CR_SERVICE_PATH=$(__cr_get_service_path $1)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return  1
	fi
	echo "$(__do_curl $CR_SERVICE_PATH/counter/$2)"
}

# Function to configure write delay on callbacks
# Delay given in seconds.
# arg <response-code> <cr-path-id>  <delay-in-sec>
# (Function for test scripts)
cr_delay_callback() {
	__log_conf_start $@

	if [ $# -ne 3 ]; then
        __print_err "<response-code> <cr-path-id> <delay-in-sec>]" $@
        return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	res="$(__do_curl_to_api CR POST /forcedelay?delay=$3)"
	status=${res:${#res}-3}

	if [ $status -ne 200 ]; then
		__log_conf_fail_status_code $1 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# CR API: Check the contents of all current ric sync events for one id from A1PMS
# <response-code> <cr-path-id> <id> [ EMPTY | ( <ric-id> )+ ]
# (Function for test scripts)
cr_api_check_all_sync_events() {
	__log_test_start $@

	if [ "$A1PMS_VERSION" != "V2" ] && [ "$A1PMS_VERSION" != "V3" ]; then
		__log_test_fail_not_supported
		return 1
	fi

    if [ $# -lt 3 ]; then
        __print_err "<response-code> <cr-path-id> <id> [ EMPTY | ( <ric-id> )+ ]" $@
        return 1
    fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-all-events/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}
		if [ $# -eq 4 ] && [ $4 == "EMPTY" ]; then
			targetJson="["
		else
			targetJson="["
			arr=(${@:4})

			for ((i=0; i<$(($#-3)); i=i+1)); do

				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"{\"ric_id\":\"${arr[$i]}\",\"event_type\":\"AVAILABLE\"}"
			done
		fi

		targetJson=$targetJson"]"
		echo "TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi
	__log_test_pass
	return 0
}

# CR API: Check the contents of all current status events for one id from ICS
# <response-code> <cr-path-id> <id> [ EMPTY | ( <status> )+ ]
# (Function for test scripts)
cr_api_check_all_ics_events() {
	__log_test_start $@

    if [ $# -lt 3 ]; then
        __print_err "<response-code> <cr-path-id> <id> [ EMPTY | ( <status> )+ ]" $@
        return 1
    fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-all-events/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}
		if [ $# -eq 4 ] && [ $4 == "EMPTY" ]; then
			targetJson="["
		else
			targetJson="["
			arr=(${@:4})

			for ((i=0; i<$(($#-3)); i=i+1)); do

				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"{\"eiJobStatus\":\"${arr[$i]}\"}"
			done
		fi

		targetJson=$targetJson"]"
		echo "TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi
	__log_test_pass
	return 0
}

# CR API: Check the contents of all current type subscription events for one id from ICS
# <response-code> <cr-path-id> <id> [ EMPTY | ( <type-id> <schema> <registration-status> )+ ]
# (Function for test scripts)
cr_api_check_all_ics_subscription_events() {
	__log_test_start $@

	#Valid number of parameter 3,4,8,12
	paramError=1
	if [ $# -eq 3 ]; then
		paramError=0
	fi
	if [ $# -eq 4 ] && [ "$4" == "EMPTY" ]; then
		paramError=0
	fi
	variablecount=$(($#-3))
	if [ $# -gt 4 ] && [ $(($variablecount%3)) -eq 0 ]; then
		paramError=0
	fi
	if [ $paramError -eq 1 ]; then
		__print_err "<response-code> <cr-path-id> <id> [ EMPTY | ( <type-id> <schema> <registration-status> )+ ]" $@
		return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-all-events/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 4 ]; then
			arr=(${@:4})
			for ((i=0; i<$(($#-4)); i=i+3)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				if [ -f ${arr[$i+1]} ]; then
					jobfile=$(cat ${arr[$i+1]})
				else
					__log_test_fail_general "Job schema file "${arr[$i+1]}", does not exist"
					return 1
				fi
				targetJson=$targetJson"{\"info_type_id\":\"${arr[$i]}\",\"job_data_schema\":$jobfile,\"status\":\"${arr[$i+2]}\"}"
			done
		fi
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


# CR API: Reset all events and counters
# Arg: <cr-path-id>
# (Function for test scripts)
cr_api_reset() {
	__log_conf_start $@

	if [ $# -ne 1 ]; then
		__print_err "<cr-path-id>" $@
		return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $1)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	res="$(__do_curl_to_api CR GET /reset)"
	status=${res:${#res}-3}

	if [ $status -ne 200 ]; then
		__log_conf_fail_status_code $1 $status
		return 1
	fi

	__log_conf_ok
	return 0
}


# CR API: Check the contents of all json events for path
# <response-code> <cr-path-id> <topic-url> (EMPTY | <json-msg>+ )
# (Function for test scripts)
cr_api_check_all_generic_json_events() {
	__log_test_start $@

	if [ $# -lt 4 ]; then
		__print_err "<response-code> <cr-path-id>  <topic-url> (EMPTY | <json-msg>+ )" $@
		return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-all-events/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	body=${res:0:${#res}-3}
	targetJson="["

	if [ $4 != "EMPTY" ]; then
		shift
		shift
		shift
		while [ $# -gt 0 ]; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			targetJson=$targetJson$1
			shift
		done
	fi
	targetJson=$targetJson"]"

	echo " TARGET JSON: $targetJson" >> $HTTPLOG
	res=$(python3 ../common/compare_json.py "$targetJson" "$body")

	if [ $res -ne 0 ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}


# CR API: Check a single (oldest) json event (or none if empty) for path
# <response-code> <cr-path-id> <topic-url> (EMPTY | <json-msg> )
# (Function for test scripts)
cr_api_check_single_generic_json_event() {
	__log_test_start $@

	if [ $# -ne 4 ]; then
		__print_err "<response-code> <cr-path-id>  <topic-url> (EMPTY | <json-msg> )" $@
		return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-event/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	body=${res:0:${#res}-3}
	targetJson=$4

	if [ $targetJson == "EMPTY" ] && [ ${#body} -ne 0 ]; then
		__log_test_fail_body
		return 1
	fi
	echo " TARGET JSON: $targetJson" >> $HTTPLOG
	res=$(python3 ../common/compare_json.py "$targetJson" "$body")

	if [ $res -ne 0 ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}

# CR API: Check a single (oldest) json in md5 format (or none if empty) for path.
# Note that if a json message is given, it shall be compact, no ws except inside string.
# The MD5 will generate different hash if ws is present or not in otherwise equivalent json
# arg: <response-code> <cr-path-id> <topic-url> (EMPTY | <data-msg> )
# (Function for test scripts)
cr_api_check_single_generic_event_md5() {
	__log_test_start $@

	if [ $# -ne 4 ]; then
		__print_err "<response-code> <cr-path-id> <topic-url> (EMPTY | <data-msg> )" $@
		return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-event/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	body=${res:0:${#res}-3}
	if [ $4 == "EMPTY" ]; then
		if [ ${#body} -ne 0 ]; then
			__log_test_fail_body
			return 1
		else
			__log_test_pass
			return 0
		fi
	fi
	command -v md5 > /dev/null # Mac
	if [ $? -eq 0 ]; then
		targetMd5=$(echo -n "$4" | md5)
	else
		command -v md5sum > /dev/null # Linux
		if [ $? -eq 0 ]; then
			targetMd5=$(echo -n "$4" | md5sum | cut -d' ' -f 1)  # Need to cut additional info printed by cmd
		else
			__log_test_fail_general "Command md5 nor md5sum is available"
			return 1
		fi
	fi
	targetMd5="\""$targetMd5"\"" #Quotes needed

	echo " TARGET MD5 hash: $targetMd5" >> $HTTPLOG

	if [ "$body" != "$targetMd5" ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}

# CR API: Check a single (oldest) event in md5 format (or none if empty) for path.
# Note that if a file with json message is given, the json shall be compact, no ws except inside string and not newlines.
# The MD5 will generate different hash if ws/newlines is present or not in otherwise equivalent json
# arg: <response-code> <cr-path-id> <topic-url> (EMPTY | <data-file> )
# (Function for test scripts)
cr_api_check_single_generic_event_md5_file() {
	__log_test_start $@

	if [ $# -ne 4 ]; then
		__print_err "<response-code> <cr-path-id> <topic-url> (EMPTY | <data-file> )" $@
		return 1
	fi

	CR_SERVICE_PATH=$(__cr_get_service_path $2)
	CR_ADAPTER=$CR_SERVICE_PATH
	if [ $? -ne 0 ]; then
		__print_err "<cr-path-id> missing or incorrect" $@
		return 1
	fi

	query="/get-event/"$3
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	body=${res:0:${#res}-3}
	if [ $4 == "EMPTY" ]; then
		if [ ${#body} -ne 0 ]; then
			__log_test_fail_body
			return 1
		else
			__log_test_pass
			return 0
		fi
	fi

	if [ ! -f $4 ]; then
		__log_test_fail_general "File $3 does not exist"
		return 1
	fi

	filedata=$(cat $4)

	command -v md5 > /dev/null # Mac
	if [ $? -eq 0 ]; then
		targetMd5=$(echo -n "$filedata" | md5)
	else
		command -v md5sum > /dev/null # Linux
		if [ $? -eq 0 ]; then
			targetMd5=$(echo -n "$filedata" | md5sum | cut -d' ' -f 1)  # Need to cut additional info printed by cmd
		else
			__log_test_fail_general "Command md5 nor md5sum is available"
			return 1
		fi
	fi
	targetMd5="\""$targetMd5"\""   #Quotes needed

	echo " TARGET MD5 hash: $targetMd5" >> $HTTPLOG

	if [ "$body" != "$targetMd5" ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}