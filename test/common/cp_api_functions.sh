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

# This is a script that contains container/service management function
# and test functions for Control Panel

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CP_imagesetup() {
	__check_and_create_image_var CP "CONTROL_PANEL_IMAGE" "CONTROL_PANEL_IMAGE_BASE" "CONTROL_PANEL_IMAGE_TAG" $1 "$CONTROL_PANEL_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CP_imagepull() {
	__check_and_pull_image $1 "$CONTROL_PANEL_DISPLAY_NAME" $CONTROL_PANEL_APP_NAME CONTROL_PANEL_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CP_imagebuild() {
	echo -e $RED" Image for app CP shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__CP_image_data() {
	echo -e "$CONTROL_PANEL_DISPLAY_NAME\t$(docker images --format $1 $CONTROL_PANEL_IMAGE)" >>   $2
	if [ ! -z "$CONTROL_PANEL_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $CONTROL_PANEL_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CP_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest CP
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__CP_kube_scale_zero_and_wait() {
	echo -e " CP replicas kept as is"
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__CP_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest CP
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__CP_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=CP" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 > $1$2_control-panel.log 2>&1
	else
		docker logs $CONTROL_PANEL_APP_NAME > $1$2_control-panel.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__CP_initial_setup() {
	use_control_panel_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__CP_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "CP $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
	else
		echo "CP $CONTROL_PANEL_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__CP_test_requirements() {
	:
}

#######################################################


###########################
### Control Panel functions
###########################

# Set http as the protocol to use for all communication to the Control Panel
# args: -
# (Function for test scripts)
use_control_panel_http() {
	__control_panel_set_protocoll "http" $CONTROL_PANEL_INTERNAL_PORT $CONTROL_PANEL_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Control Panel
# args: -
# (Function for test scripts)
use_control_panel_https() {
	__control_panel_set_protocoll "https" $CONTROL_PANEL_INTERNAL_SECURE_PORT $CONTROL_PANEL_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__control_panel_set_protocoll() {
	echo -e $BOLD"$CONTROL_PANEL_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $CONTROL_PANEL_DISPLAY_NAME"

	CP_SERVICE_PATH=$1"://"$CONTROL_PANEL_APP_NAME":"$2
	if [ $RUNMODE == "KUBE" ]; then
		CP_SERVICE_PATH=$1"://"$CONTROL_PANEL_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3
	fi
	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args: -
__control_panel_export_vars() {
	#Export all vars needed for service and deployment
	export CONTROL_PANEL_APP_NAME
	export CONTROL_PANEL_DISPLAY_NAME
	export KUBE_NONRTRIC_NAMESPACE
	export DOCKER_SIM_NWNAME

	export CONTROL_PANEL_IMAGE
	export CONTROL_PANEL_INTERNAL_PORT
	export CONTROL_PANEL_INTERNAL_SECURE_PORT
	export CONTROL_PANEL_EXTERNAL_PORT
	export CONTROL_PANEL_EXTERNAL_SECURE_PORT
	export CONTROL_PANEL_CONFIG_MOUNT_PATH
	export CONTROL_PANEL_CONFIG_FILE
	export CONTROL_PANEL_HOST_MNT_DIR

	export CP_CONFIG_CONFIGMAP_NAME=$CONTROL_PANEL_APP_NAME"-config"
	export CP_PROXY_CONFIGMAP_NAME=$CONTROL_PANEL_APP_NAME"-proxy"

	export CONTROL_PANEL_PATH_POLICY_PREFIX
	export CONTROL_PANEL_PATH_ICS_PREFIX
	export CONTROL_PANEL_PATH_ICS_PREFIX2

	export NRT_GATEWAY_APP_NAME
	export NRT_GATEWAY_EXTERNAL_PORT

	export A1PMS_EXTERNAL_SECURE_PORT
	export ICS_EXTERNAL_SECURE_PORT

	if [ $RUNMODE == "KUBE" ]; then
		export NGW_DOMAIN_NAME=$NRT_GATEWAY_APP_NAME.$KUBE_NONRTRIC_NAMESPACE.svc.cluster.local  # suffix needed for nginx name resolution
		export CP_NGINX_RESOLVER=$CONTROL_PANEL_NGINX_KUBE_RESOLVER
	else
		export A1PMS_DOMAIN_NAME=$A1PMS_APP_NAME
		export ICS_DOMAIN_NAME=$ICS_APP_NAME

		export NGW_DOMAIN_NAME=$NRT_GATEWAY_APP_NAME
		export CP_NGINX_RESOLVER=$CONTROL_PANEL_NGINX_DOCKER_RESOLVER
	fi
}

# Start the Control Panel container
# args: -
# (Function for test scripts)
start_control_panel() {

	echo -e $BOLD"Starting $CONTROL_PANEL_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "CP"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "CP"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $CONTROL_PANEL_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $CONTROL_PANEL_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $CONTROL_PANEL_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $CONTROL_PANEL_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		__check_prestarted_image "CP"
		if [ $? -eq 0 ]; then
			echo -e " Using existing $CONTROL_PANEL_APP_NAME deployment and service"
			echo " Setting CP replicas=1"
			__kube_scale deployment $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $CONTROL_PANEL_APP_NAME app and expose service"

			__control_panel_export_vars

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			# Create config map for config
			datafile=$PWD/tmp/$CONTROL_PANEL_CONFIG_FILE
			#Add config to properties file

			#Trick to prevent these two vars to be replace with space in the config file by cmd envsubst
			export upstream='$upstream'
			export uri='$uri'

			envsubst < $1 > $datafile

			output_yaml=$PWD/tmp/cp_cfc.yaml
			__kube_create_configmap $CP_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest CP $datafile $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$CONTROL_PANEL_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/cp_svc.yaml
			__kube_create_instance service $CONTROL_PANEL_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$CONTROL_PANEL_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/cp_app.yaml
			__kube_create_instance app $CONTROL_PANEL_APP_NAME $input_yaml $output_yaml

		fi

		__check_service_start $CONTROL_PANEL_APP_NAME $CP_SERVICE_PATH$CONTROL_PANEL_ALIVE_URL

		CP_PORT1=$(__kube_get_service_nodeport $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE "http")
		CP_PORT2=$(__kube_get_service_nodeport $CONTROL_PANEL_APP_NAME $KUBE_NONRTRIC_NAMESPACE "https")

		echo " $CONTROL_PANEL_DISPLAY_NAME node ports (http/https): $CP_PORT1 $CP_PORT2"
	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'CP'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Control Panel app is not included in this test script"$ERED
			echo -e $RED"The Control Panel will not be started"$ERED
			exit
		fi

		__control_panel_export_vars

		dest_file=$SIM_GROUP/$CONTROL_PANEL_COMPOSE_DIR/$CONTROL_PANEL_HOST_MNT_DIR/$CONTROL_PANEL_CONFIG_FILE

		envsubst '${NGW_DOMAIN_NAME},${CP_NGINX_RESOLVER},${NRT_GATEWAY_EXTERNAL_PORT},${A1PMS_EXTERNAL_SECURE_PORT},${ICS_EXTERNAL_SECURE_PORT},${A1PMS_DOMAIN_NAME},${ICS_DOMAIN_NAME},${CONTROL_PANEL_PATH_POLICY_PREFIX},${CONTROL_PANEL_PATH_ICS_PREFIX} ,${CONTROL_PANEL_PATH_ICS_PREFIX2}' < $1 > $dest_file

		__start_container $CONTROL_PANEL_COMPOSE_DIR "" NODOCKERARGS 1 $CONTROL_PANEL_APP_NAME

		__check_service_start $CONTROL_PANEL_APP_NAME $CP_SERVICE_PATH$CONTROL_PANEL_ALIVE_URL

		echo " $CONTROL_PANEL_DISPLAY_NAME locahost ports (http/https): $CONTROL_PANEL_EXTERNAL_PORT $CONTROL_PANEL_EXTERNAL_SECURE_PORT"
	fi
	echo ""
}
