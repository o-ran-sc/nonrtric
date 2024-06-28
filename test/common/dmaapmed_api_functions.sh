#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions test functions for the Dmaap Mediator


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__DMAAPMED_imagesetup() {
	__check_and_create_image_var DMAAPMED "DMAAP_MED_IMAGE" "DMAAP_MED_IMAGE_BASE" "DMAAP_MED_IMAGE_TAG" $1 "$DMAAP_MED_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__DMAAPMED_imagepull() {
	__check_and_pull_image $1 "$DMAAP_MED_DISPLAY_NAME" $DMAAP_MED_APP_NAME DMAAP_MED_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__DMAAPMED_imagebuild() {
	echo -e $RED" Image for app DMAAPMED shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__DMAAPMED_image_data() {
	echo -e "$DMAAP_MED_DISPLAY_NAME\t$(docker images --format $1 $DMAAP_MED_IMAGE)" >>   $2
	if [ ! -z "$DMAAP_MED_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $DMAAP_MED_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__DMAAPMED_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest DMAAPMED
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__DMAAPMED_kube_scale_zero_and_wait() {
	__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app "$KUBE_NONRTRIC_NAMESPACE"-dmaapmediatorservice
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__DMAAPMED_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest DMAAPMED
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__DMAAPMED_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=DMAAPMED" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 > $1$2_dmaapmediator.log 2>&1
	else
		docker logs $DMAAP_MED_APP_NAME > $1$2_dmaapmediator.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__DMAAPMED_initial_setup() {
	use_dmaapmed_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__DMAAPMED_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "DMAAPMED $DMAAP_MED_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
	else
		echo "DMAAPMED $DMAAP_MED_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__DMAAPMED_test_requirements() {
	:
}

#######################################################

# Set http as the protocol to use for all communication to the Dmaap mediator
# args: -
# (Function for test scripts)
use_dmaapmed_http() {
	__dmaapmed_set_protocoll "http" $DMAAP_MED_INTERNAL_PORT $DMAAP_MED_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Dmaap mediator
# args: -
# (Function for test scripts)
use_dmaapmed_https() {
	__dmaapmed_set_protocoll "https" $DMAAP_MED_INTERNAL_SECURE_PORT $DMAAP_MED_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__dmaapmed_set_protocoll() {
	echo -e $BOLD"$DMAAP_MED_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $DMAAP_MED_DISPLAY_NAME"

	## Access to Dmaap mediator

	DMAAP_MED_SERVICE_PATH=$1"://"$DMAAP_MED_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		DMAAP_MED_SERVICE_PATH=$1"://"$DMAAP_MED_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	# DMAAP_MED_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
	DMAAP_MED_ADAPTER_TYPE="REST"
	DMAAP_MED_ADAPTER=$DMAAP_MED_SERVICE_PATH

	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args: PROXY|NOPROXY
__dmaapmed_export_vars() {

	export DMAAP_MED_APP_NAME
	export DMAAP_MED_DISPLAY_NAME

	export KUBE_NONRTRIC_NAMESPACE
	export DOCKER_SIM_NWNAME

	export DMAAP_MED_IMAGE

	export DMAAP_MED_INTERNAL_PORT
	export DMAAP_MED_INTERNAL_SECURE_PORT
	export DMAAP_MED_EXTERNAL_PORT
	export DMAAP_MED_EXTERNAL_SECURE_PORT

	export DMAAP_MED_DATA_MOUNT_PATH
	export DMAAP_MED_HOST_MNT_DIR
	export DMAAP_MED_CONTR_DATA_FILE
	export DMAAP_MED_DATA_CONFIGMAP_NAME=$DMAAP_MED_APP_NAME"-data"

	if [ $1 == "PROXY" ]; then
		export DMAAP_MED_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT  #Set if proxy is started
		export DMAAP_MED_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
		if [ $DMAAP_MED_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$DMAAP_MED_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
			echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
		else
			echo " Configured with http proxy"
		fi
	else
		export DMAAP_MED_HTTP_PROXY_CONFIG_PORT=0
		export DMAAP_MED_HTTP_PROXY_CONFIG_HOST_NAME=""
		echo " Configured without http proxy"
	fi

	# paths to other components
	export ICS_SERVICE_PATH

	export DMAAP_MED_CONF_SELF_HOST=$(echo $DMAAP_MED_SERVICE_PATH | cut -d: -f1-2)
	export DMAAP_MED_CONF_SELF_PORT=$(echo $DMAAP_MED_SERVICE_PATH | cut -d: -f3)
	export MR_SERVICE_PATH
	export MR_KAFKA_SERVICE_PATH

}

# Start the Dmaap mediator
# args: -
# (Function for test scripts)
start_dmaapmed() {

	echo -e $BOLD"Starting $DMAAP_MED_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "DMAAPMED"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "DMAAPMED"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $DMAAP_MED_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $DMAAP_MED_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $DMAAP_MED_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $DMAAP_MED_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $DMAAP_MED_APP_NAME deployment and service"
			echo " Setting DMAAPMED replicas=1"
			__kube_scale statefulset $DMAAP_MED_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $DMAAP_MED_APP_NAME deployment and service"

			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			__dmaapmed_export_vars $1

			# Create config map for data
			data_json=$PWD/tmp/$DMAAP_MED_CONTR_DATA_FILE
			if [ $# -lt 2 ]; then
				#create empty dummy file
				echo "{}" > $data_json
			else
				cp $2 $data_json
			fi
			output_yaml=$PWD/tmp/dmaapmed_cfd.yaml
			__kube_create_configmap $DMAAP_MED_DATA_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest DMAAPMED $data_json $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$DMAAP_MED_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/dmaapmed_svc.yaml
			__kube_create_instance service $DMAAP_MED_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$DMAAP_MED_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/dmaapmed_app.yaml
			__kube_create_instance app $DMAAP_MED_APP_NAME $input_yaml $output_yaml

		fi

		__check_service_start $DMAAP_MED_APP_NAME $DMAAP_MED_SERVICE_PATH$DMAAP_MED_ALIVE_URL

	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'DMAAPMED'
		if [ $? -eq 1 ]; then
			echo -e $RED"The $DMAAP_MED_DISPLAY_NAME app is not included in this test script"$ERED
			echo -e $RED"The $DMAAP_MED_DISPLAY_NAME will not be started"$ERED
			exit
		fi

		__dmaapmed_export_vars $1

		dest_file=$SIM_GROUP/$DMAAP_MED_COMPOSE_DIR/$DMAAP_MED_HOST_MNT_DIR/$DMAAP_MED_CONTR_DATA_FILE

		envsubst < $2 > $dest_file

		__start_container $DMAAP_MED_COMPOSE_DIR "" NODOCKERARGS 1 $DMAAP_MED_APP_NAME

		__check_service_start $DMAAP_MED_APP_NAME $DMAAP_MED_SERVICE_PATH$DMAAP_MED_ALIVE_URL
	fi
	echo ""
}