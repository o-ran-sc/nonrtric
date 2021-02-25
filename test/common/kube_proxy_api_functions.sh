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

# This is a script that contains container/service managemnt functions for Kube Http Proxy
# This http proxy is to provide full access for the test script to all adressable kube object in a clister

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KUBEPROXY_imagesetup() {
	__check_and_create_image_var KUBEPROXY "KUBE_PROXY_IMAGE" "KUBE_PROXY_IMAGE_BASE" "KUBE_PROXY_IMAGE_TAG" REMOTE_PROXY "$KUBE_PROXY_DISPLAY_NAME"
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__KUBEPROXY_imagepull() {
	__check_and_pull_image $2 "$KUBE_PROXY_DISPLAY_NAME" $KUBE_PROXY_APP_NAME $KUBE_PROXY_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KUBEPROXY_imagebuild() {
	echo -e $RED"Image for app KUBEPROXY shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# arg: <docker-images-format-string> <file-to-append>
__KUBEPROXY_image_data() {
	echo -e "$KUBE_PROXY_DISPLAY_NAME\t$(docker images --format $1 $KUBE_PROXY_IMAGE)" >>   $2
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__KUBEPROXY_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest KUBEPROXY
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__KUBEPROXY_kube_scale_zero_and_wait() {
	echo -e $RED" Http proxy replicas kept as is"$ERED
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__KUBEPROXY_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest KUBEPROXY
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__KUBEPROXY_store_docker_logs() {
	docker logs $KUBE_PROXY_APP_NAME > $1$2_kubeproxy.log 2>&1
}

#######################################################


## Access to Kube Http Proxy
# Host name may be changed if app started by kube
# Direct access from script
KUBE_PROXY_HTTPX="http"
KUBE_PROXY_HOST_NAME=$LOCALHOST_NAME
KUBE_PROXY_PATH=$KUBE_PROXY_HTTPX"://"$KUBE_PROXY_HOST_NAME":"$KUBE_PROXY_WEB_EXTERNAL_PORT

#########################
### Http Proxy functions
#########################

# Start the Kube Http Proxy in the simulator group
# args: -
# (Function for test scripts)
start_kube_proxy() {

	echo -e $BOLD"Starting $KUBE_PROXY_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "KUBEPROXY"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "KUBEPROXY"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $KUBE_PROXY_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $KUBE_PROXY_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $KUBE_PROXY_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $KUBE_PROXY_APP_NAME will not be started"$ERED
			exit
		fi

		# Check if app shall be used - not managed - by the test script
		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $KUBE_PROXY_APP_NAME deployment and service"
			echo " Setting KUBEPROXY replicas=1"
			__kube_scale deployment $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $KUBE_PROXY_APP_NAME deployment and service"
			export KUBE_PROXY_APP_NAME
			export KUBE_PROXY_WEB_EXTERNAL_PORT
			export KUBE_PROXY_WEB_INTERNAL_PORT
			export KUBE_PROXY_EXTERNAL_PORT
			export KUBE_PROXY_INTERNAL_PORT
			export KUBE_SIM_NAMESPACE
			export KUBE_PROXY_IMAGE

			__kube_create_namespace $KUBE_SIM_NAMESPACE

			# Create service
			input_yaml=$SIM_GROUP"/"$KUBE_PROXY_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/proxy_svc.yaml
			__kube_create_instance service $KUBE_PROXY_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$KUBE_PROXY_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/proxy_app.yaml
			__kube_create_instance app $KUBE_PROXY_APP_NAME $input_yaml $output_yaml

		fi

		echo " Retrieving host and ports for service..."
		KUBE_PROXY_HOST_NAME=$(__kube_get_service_host $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE)
		KUBE_PROXY_WEB_EXTERNAL_PORT=$(__kube_get_service_port $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "web")
		KUBE_PROXY_EXTERNAL_PORT=$(__kube_get_service_port $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http")


		minikube status > /dev/null
		if [ $? -eq 0 ]; then
			echo -e $GREEN" Running minikube inside a kubernetes cluster. No proxy needed for the test script to access services"$EGREEN
			export CLUSTER_KUBE_PROXY_NODEPORT=""
		else
			echo -e $YELLOW" Running outside the kubernetes cluster. Proxy is setup to access services from the test script"$EYELLOW
			export CLUSTER_KUBE_PROXY_NODEPORT=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http")
		fi

		KUBE_PROXY_PATH=$KUBE_PROXY_HTTPX"://"$KUBE_PROXY_HOST_NAME":"$KUBE_PROXY_WEB_EXTERNAL_PORT
		KUBE_PROXY_CONFIG_PORT=$KUBE_PROXY_EXTERNAL_PORT
		KUBE_PROXY_CONFIG_HOST_NAME=$KUBE_PROXY_APP_NAME"."$KUBE_SIM_NAMESPACE

		echo " Host IP, http port, cluster http nodeport (may be empty): $KUBE_PROXY_HOST_NAME $KUBE_PROXY_WEB_EXTERNAL_PORT $CLUSTER_KUBE_PROXY_NODEPORT"

		__check_service_start $KUBE_PROXY_APP_NAME $KUBE_PROXY_PATH$KUBE_PROXY_ALIVE_URL

	else
		echo $YELLOW" Kube http proxy not needed in docker test. App not started"
	fi
	echo ""
}

