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

# This is a script that contains container/service managemnt functions test functions for the Callback Reciver


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CR_imagesetup() {
	__check_and_create_image_var CR "CR_IMAGE" "CR_IMAGE_BASE" "CR_IMAGE_TAG" LOCAL "$CR_DISPLAY_NAME"
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CR_imagepull() {
	echo -e $RED" Image for app CR shall never be pulled from remove repo"$ERED
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CR_imagebuild() {
	cd ../cr
	echo " Building CR - $CR_DISPLAY_NAME - image: $CR_IMAGE"
	docker build  --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $CR_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN" Build Ok"$EGREEN
	else
		echo -e $RED" Build Failed"$ERED
		((RES_CONF_FAIL++))
		cat .dockererr
		echo -e $RED"Exiting...."$ERED
		exit 1
	fi
}

# Generate a string for each included image using the app display name and a docker images format string
# arg: <docker-images-format-string> <file-to-append>
__CR_image_data() {
	echo -e "$CR_DISPLAY_NAME\t$(docker images --format $1 $CR_IMAGE)" >>   $2
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CR_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest CR
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__CR_kube_scale_zero_and_wait() {
	echo -e $RED" CR app is not scaled in this state"$ERED
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__CR_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest CR
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__CR_store_docker_logs() {
	docker logs $CR_APP_NAME > $1$2_cr.log 2>&1
}

#######################################################


## Access to Callback Receiver
# Host name may be changed if app started by kube
# Direct access from script
CR_HTTPX="http"
CR_HOST_NAME=$LOCALHOST_NAME
CR_PATH=$CR_HTTPX"://"$CR_HOST_NAME":"$CR_EXTERNAL_PORT
#Docker/Kube internal path
if [ $RUNMODE == "KUBE" ]; then
	CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME"."$KUBE_SIM_NAMESPACE":"$CR_EXTERNAL_PORT$CR_APP_CALLBACK
else
	CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME":"$CR_INTERNAL_PORT$CR_APP_CALLBACK
fi
# CR_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
CR_ADAPTER_TYPE="REST"
CR_ADAPTER=$CR_PATH

################
### CR functions
################

# Set http as the protocol to use for all communication to the Callback Receiver
# args: -
# (Function for test scripts)
use_cr_http() {
	echo -e $BOLD"CR protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards CR"

	CR_HTTPX="http"
	CR_PATH=$CR_HTTPX"://"$CR_HOST_NAME":"$CR_EXTERNAL_PORT

	#Docker/Kube internal path
	if [ $RUNMODE == "KUBE" ]; then
		CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME"."$KUBE_SIM_NAMESPACE":"$CR_EXTERNAL_PORT$CR_APP_CALLBACK
	else
		CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME":"$CR_INTERNAL_PORT$CR_APP_CALLBACK
	fi
	CR_ADAPTER_TYPE="REST"
	CR_ADAPTER=$CR_PATH
	echo ""
}

# Set https as the protocol to use for all communication to the Callback Receiver
# args: -
# (Function for test scripts)
use_cr_https() {
	echo -e $BOLD"CR protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards CR"

	CR_HTTPX="https"
	CR_PATH=$CR_HTTPX"://"$CR_HOST_NAME":"$CR_EXTERNAL_SECURE_PORT

	if [ $RUNMODE == "KUBE" ]; then
		CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME"."$KUBE_SIM_NAMESPACE":"$CR_EXTERNAL_SECURE_PORT$CR_APP_CALLBACK
	else
		CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME":"$CR_INTERNAL_SECURE_PORT$CR_APP_CALLBACK
	fi

	CR_ADAPTER_TYPE="REST"
	CR_ADAPTER=$CR_PATH
	echo ""
}

# Start the Callback reciver in the simulator group
# args: -
# (Function for test scripts)
start_cr() {

	echo -e $BOLD"Starting $CR_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "CR"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
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
			export CR_APP_NAME
			export KUBE_SIM_NAMESPACE
			export CR_IMAGE
			export CR_INTERNAL_PORT
			export CR_INTERNAL_SECURE_PORT
			export CR_EXTERNAL_PORT
			export CR_EXTERNAL_SECURE_PORT

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

		echo " Retrieving host and ports for service..."
		CR_HOST_NAME=$(__kube_get_service_host $CR_APP_NAME $KUBE_SIM_NAMESPACE)

		CR_EXTERNAL_PORT=$(__kube_get_service_port $CR_APP_NAME $KUBE_SIM_NAMESPACE "http")
		CR_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $CR_APP_NAME $KUBE_SIM_NAMESPACE "https")

		echo " Host IP, http port, https port: $CR_HOST_NAME $CR_EXTERNAL_PORT $CR_EXTERNAL_SECURE_PORT"
		if [ $CR_HTTPX == "http" ]; then
			CR_PATH=$CR_HTTPX"://"$CR_HOST_NAME":"$CR_EXTERNAL_PORT
			CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME"."$KUBE_SIM_NAMESPACE":"$CR_EXTERNAL_PORT$CR_APP_CALLBACK
		else
			CR_PATH=$CR_HTTPX"://"$CR_HOST_NAME":"$CR_EXTERNAL_SECURE_PORT
			CR_SERVICE_PATH=$CR_HTTPX"://"$CR_APP_NAME"."$KUBE_SIM_NAMESPACE":"$CR_EXTERNAL_SECURE_PORT$CR_APP_CALLBACK
		fi
		if [ $CR_ADAPTER_TYPE == "REST" ]; then
			CR_ADAPTER=$CR_PATH
		fi

		__check_service_start $CR_APP_NAME $CR_PATH$CR_ALIVE_URL

		echo -ne " Service $CR_APP_NAME - reset  "$SAMELINE
		result=$(__do_curl $CR_APP_NAME $CR_PATH/reset)
		if [ $? -ne 0 ]; then
			echo -e " Service $CR_APP_NAME - reset  $RED Failed $ERED - will continue"
		else
			echo -e " Service $CR_APP_NAME - reset  $GREEN OK $EGREEN"
		fi
	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'CR'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Callback Receiver app is not included in this test script"$ERED
			echo -e $RED"The Callback Receiver will not be started"$ERED
			exit
		fi

		export CR_APP_NAME
		export CR_INTERNAL_PORT
		export CR_EXTERNAL_PORT
		export CR_INTERNAL_SECURE_PORT
		export CR_EXTERNAL_SECURE_PORT
		export DOCKER_SIM_NWNAME
		export CR_DISPLAY_NAME

		__start_container $CR_COMPOSE_DIR "" NODOCKERARGS 1 $CR_APP_NAME

        __check_service_start $CR_APP_NAME $CR_PATH$CR_ALIVE_URL
	fi
	echo ""
}


# Tests if a variable value in the CR is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
cr_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test "CR" "$CR_PATH/counter/" $1 "=" $2 $3
	else
		__print_err "Wrong args to cr_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}

# CR API: Check the contents of all current ric sync events for one id from PMS
# <response-code> <id> [ EMPTY | ( <ric-id> )+ ]
# (Function for test scripts)
cr_api_check_all_sync_events() {
	__log_test_start $@

	if [ "$PMS_VERSION" != "V2" ]; then
		__log_test_fail_not_supported
		return 1
	fi

    if [ $# -lt 2 ]; then
        __print_err "<response-code> <id> [ EMPTY | ( <ric-id> )+ ]" $@
        return 1
    fi

	query="/get-all-events/"$2
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		if [ $# -eq 3 ] && [ $3 == "EMPTY" ]; then
			targetJson="["
		else
			targetJson="["
			arr=(${@:3})

			for ((i=0; i<$(($#-2)); i=i+1)); do

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

# CR API: Check the contents of all current status events for one id from ECS
# <response-code> <id> [ EMPTY | ( <status> )+ ]
# (Function for test scripts)
cr_api_check_all_ecs_events() {
	__log_test_start $@

    if [ $# -lt 2 ]; then
        __print_err "<response-code> <id> [ EMPTY | ( <status> )+ ]" $@
        return 1
    fi

	query="/get-all-events/"$2
	res="$(__do_curl_to_api CR GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		if [ $# -eq 3 ] && [ $3 == "EMPTY" ]; then
			targetJson="["
		else
			targetJson="["
			arr=(${@:3})

			for ((i=0; i<$(($#-2)); i=i+1)); do

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