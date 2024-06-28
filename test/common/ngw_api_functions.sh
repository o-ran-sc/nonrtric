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

# This is a script that contains container/service management functions
# for NonRTRIC Gateway

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__NGW_imagesetup() {
	__check_and_create_image_var NGW "NRT_GATEWAY_IMAGE" "NRT_GATEWAY_IMAGE_BASE" "NRT_GATEWAY_IMAGE_TAG" $1 "$NRT_GATEWAY_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__NGW_imagepull() {
	__check_and_pull_image $1 "$NRT_GATEWAY_DISPLAY_NAME" $NRT_GATEWAY_APP_NAME NRT_GATEWAY_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__NGW_imagebuild() {
	echo -e $RED"Image for app NGW shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__NGW_image_data() {
	echo -e "$NRT_GATEWAY_DISPLAY_NAME\t$(docker images --format $1 $NRT_GATEWAY_IMAGE)" >>   $2
	if [ ! -z "$NRT_GATEWAY_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $NRT_GATEWAY_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__NGW_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest NGW
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__NGW_kube_scale_zero_and_wait() {
	echo -e " NGW replicas kept as is"
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__NGW_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest NGW
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__NGW_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=NGW" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 > $1$2_gateway.log 2>&1
	else
		docker logs $NRT_GATEWAY_APP_NAME > $1$2_gateway.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__NGW_initial_setup() {
	use_gateway_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__NGW_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "NGW $NRT_GATEWAY_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
	else
		echo "NGW $NRT_GATEWAY_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__NGW_test_requirements() {
	:
}

#######################################################





# Set http as the protocol to use for all communication to the nonrtric gateway
# args: -
# (Function for test scripts)
use_gateway_http() {
	__gateway_set_protocoll "http" $NRT_GATEWAY_INTERNAL_PORT $NRT_GATEWAY_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the nonrtric gateway
# args: -
# (Function for test scripts)
use_gateway_https() {
	__gateway_set_protocoll "https" $NRT_GATEWAY_INTERNAL_SECURE_PORT $NRT_GATEWAY_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__gateway_set_protocoll() {
	echo -e $BOLD"$NRT_GATEWAY_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $NRT_GATEWAY_DISPLAY_NAME"

	## Access to nonrtric gateway

	NRT_GATEWAY_SERVICE_PATH=$1"://"$NRT_GATEWAY_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		NRT_GATEWAY_SERVICE_PATH=$1"://"$NRT_GATEWAY_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	# NRT_GATEWAY_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
	NRT_GATEWAY_ADAPTER_TYPE="REST"
	NRT_GATEWAY_ADAPTER=$DMAAP_ADP_SERVICE_PATH

	echo ""
}

# Turn on debug level tracing in the gateway
# args: -
# (Function for test scripts)
set_gateway_debug() {
	echo -e $BOLD"Setting gateway debug logging"$EBOLD
	curlString="$NRT_GATEWAY_SERVICE_PATH$NRT_GATEWAY_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "could not set debug mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Turn on trace level tracing in the gateway
# args: -
# (Function for test scripts)
set_gateway_trace() {
	echo -e $BOLD"Setting gateway trace logging"$EBOLD
	curlString="$NRT_GATEWAY_SERVICE_PATH$NRT_GATEWAY_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "could not set trace mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Export env vars for config files, docker compose and kube resources
# args: -
__gateway_export_vars() {

	export NRT_GATEWAY_APP_NAME
	export NRT_GATEWAY_DISPLAY_NAME

	export KUBE_NONRTRIC_NAMESPACE
	export DOCKER_SIM_NWNAME

	export NRT_GATEWAY_IMAGE
	export NRT_GATEWAY_INTERNAL_PORT
	export NRT_GATEWAY_INTERNAL_SECURE_PORT
	export NRT_GATEWAY_EXTERNAL_PORT
	export NRT_GATEWAY_EXTERNAL_SECURE_PORT
	export NRT_GATEWAY_CONFIG_MOUNT_PATH
	export NRT_GATEWAY_CONFIG_FILE
	export NGW_CONFIG_CONFIGMAP_NAME=$NRT_GATEWAY_APP_NAME"-config"
	export NRT_GATEWAY_HOST_MNT_DIR
	export NRT_GATEWAY_COMPOSE_DIR

	if [ $RUNMODE == "KUBE" ]; then
		export A1PMS_EXTERNAL_SECURE_PORT
		export ICS_EXTERNAL_SECURE_PORT
		export A1PMS_DOMAIN_NAME=$A1PMS_APP_NAME.$KUBE_NONRTRIC_NAMESPACE
		export ICS_DOMAIN_NAME=$ICS_APP_NAME.$KUBE_NONRTRIC_NAMESPACE
	else
		export A1PMS_DOMAIN_NAME=$A1PMS_APP_NAME
		export ICS_DOMAIN_NAME=$ICS_APP_NAME
	fi
}

# Start the Gateway container
# args: -
# (Function for test scripts)
start_gateway() {

	echo -e $BOLD"Starting $NRT_GATEWAY_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "NGW"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "NGW"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $NRT_GATEWAY_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $NRT_GATEWAY_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $NRT_GATEWAY_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $NRT_GATEWAY_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		__check_prestarted_image "NGW"
		if [ $? -eq 0 ]; then
			echo -e " Using existing $NRT_GATEWAY_APP_NAME deployment and service"
			echo " Setting NGW replicas=1"
			__kube_scale deployment $NRT_GATEWAY_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $NRT_GATEWAY_APP_NAME app and expose service"

			__gateway_export_vars

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			# Create config map for config
			datafile=$PWD/tmp/$NRT_GATEWAY_CONFIG_FILE
			#Add config to properties file
			envsubst < $1 > $datafile
			output_yaml=$PWD/tmp/ngw_cfc.yaml
			__kube_create_configmap $NGW_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest NGW $datafile $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$NRT_GATEWAY_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/ngw_svc.yaml
			__kube_create_instance service $NRT_GATEWAY_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$NRT_GATEWAY_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/ngw_app.yaml
			__kube_create_instance app $NRT_GATEWAY_APP_NAME $input_yaml $output_yaml

		fi

		__check_service_start $NRT_GATEWAY_APP_NAME $NRT_GATEWAY_SERVICE_PATH$NRT_GATEWAY_ALIVE_URL

	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'NGW'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Gateway app is not included in this test script"$ERED
			echo -e $RED"The Gateway will not be started"$ERED
			exit
		fi

		__gateway_export_vars

		dest_file=$SIM_GROUP/$NRT_GATEWAY_COMPOSE_DIR/$NRT_GATEWAY_HOST_MNT_DIR/$NRT_GATEWAY_CONFIG_FILE

		envsubst < $1 > $dest_file

		__start_container $NRT_GATEWAY_COMPOSE_DIR "" NODOCKERARGS 1 $NRT_GATEWAY_APP_NAME

		__check_service_start $NRT_GATEWAY_APP_NAME $NRT_GATEWAY_SERVICE_PATH$NRT_GATEWAY_ALIVE_URL
	fi
	echo ""
}


# API Test function: V2 GET /status towards A1PMS
# args: <response-code>
# (Function for test scripts)
gateway_a1pms_get_status() {
	__log_test_start $@
    if [ $# -ne 1 ]; then
		__print_err "<response-code>" $@
		return 1
	fi
	query=$A1PMS_API_PREFIX"/v2/status"
    res="$(__do_curl_to_api NGW GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eitypes towards ICS
# Note: This is just to test service response
# args: <response-code>
# (Function for test scripts)
gateway_ics_get_types() {
	__log_test_start $@
    if [ $# -ne 1 ]; then
		__print_err "<response-code>" $@
		return 1
	fi
	query="/ei-producer/v1/eitypes"
    res="$(__do_curl_to_api NGW GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}