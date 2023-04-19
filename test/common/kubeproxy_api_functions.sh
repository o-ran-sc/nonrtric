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

# This is a script that contains container/service management functions for Kube Http Proxy
# This http proxy is to provide full access for the test script to all addressable kube object in a cluster

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KUBEPROXY_imagesetup() {
	__check_and_create_image_var KUBEPROXY "KUBE_PROXY_IMAGE" "KUBE_PROXY_IMAGE_BASE" "KUBE_PROXY_IMAGE_TAG" LOCAL "$KUBE_PROXY_DISPLAY_NAME" $IMAGE_TARGET_PLATFORM_IMG_TAG
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__KUBEPROXY_imagepull() {
	echo -e $RED"Image for app KUBEPROXY shall never be pulled from remote repo"$ERED
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__KUBEPROXY_imagebuild() {
	cd ../http-https-proxy
	echo " Building KUBEPROXY - $KUBE_PROXY_DISPLAY_NAME - image: $KUBE_PROXY_IMAGE"
	docker build  $IMAGE_TARGET_PLATFORM_CMD_PARAM --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $KUBE_PROXY_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN"  Build Ok"$EGREEN
		__retag_and_push_image KUBE_PROXY_IMAGE
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
__KUBEPROXY_image_data() {
	echo -e "$KUBE_PROXY_DISPLAY_NAME\t$(docker images --format $1 $KUBE_PROXY_IMAGE)" >>   $2
	if [ ! -z "$KUBE_PROXY_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $KUBE_PROXY_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__KUBEPROXY_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest KUBEPROXY
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__KUBEPROXY_kube_scale_zero_and_wait() {
	echo -e $RED" KUBEPROXY app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__KUBEPROXY_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest KUBEPROXY
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__KUBEPROXY_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=KUBEPROXY" -n $KUBE_SIM_NAMESPACE --tail=-1 > $1$2_kubeproxy.log 2>&1
	else
		docker logs $KUBE_PROXY_APP_NAME > $1$2_kubeproxy.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__KUBEPROXY_initial_setup() {
	use_kube_proxy_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__KUBEPROXY_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "KUBEPROXXY $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE"
	else
		echo "KUBEPROXXY $KUBE_PROXY_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__KUBEPROXY_test_requirements() {
	:
}

#######################################################

## Access to Kube http proxy
# Direct access
KUBE_PROXY_HTTPX="http"


# Set http as the protocol to use for all communication to the Kube http proxy
# args: -
# (Function for test scripts)
use_kube_proxy_http() {
	echo -e $BOLD"$KUBE_PROXY_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards Kube http proxy"

	KUBE_PROXY_HTTPX="http"

	echo -e $YELLOW" This setting cannot be changed once the kube proxy is started"$EYELLOW
	echo ""
}

# Set https as the protocol to use for all communication to the Kube http proxy
# args: -
# (Function for test scripts)
use_kube_proxy_https() {
	echo -e $BOLD"$KUBE_PROXY_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards Kube http proxy"

	KUBE_PROXY_HTTPX="https"

	echo -e $YELLOW" This setting cannot be changed once the kube proxy is started"$EYELLOW
	echo ""
}

#########################
### Kube Http Proxy functions
#########################

# Export env vars for config files, docker compose and kube resources
# args: -
__kube_proxy_vars() {

	export KUBE_PROXY_WEB_EXTERNAL_PORT
	export KUBE_PROXY_WEB_INTERNAL_PORT
	export KUBE_PROXY_EXTERNAL_PORT
	export KUBE_PROXY_INTERNAL_PORT

	export KUBE_PROXY_WEB_EXTERNAL_SECURE_PORT
	export KUBE_PROXY_WEB_INTERNAL_SECURE_PORT
	export KUBE_PROXY_EXTERNAL_SECURE_PORT
	export KUBE_PROXY_INTERNAL_SECURE_PORT

	export KUBE_SIM_NAMESPACE
	export KUBE_PROXY_IMAGE

	export KUBE_PROXY_APP_NAME
	export KUBE_PROXY_DOCKER_EXTERNAL_PORT
	export KUBE_PROXY_DOCKER_EXTERNAL_SECURE_PORT
	export KUBE_PROXY_WEB_DOCKER_EXTERNAL_PORT
	export KUBE_PROXY_WEB_DOCKER_EXTERNAL_SECURE_PORT
	export DOCKER_SIM_NWNAME

	export KUBE_PROXY_DISPLAY_NAME
}

# Start the Kube Http Proxy in the simulator group
# args: -
# (Function for test scripts)
start_kube_proxy() {

	echo -e $BOLD"Starting $KUBE_PROXY_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "KUBEPROXY"
		retcode_i=$?

		# Check if app shall only be used by the test script
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

			__kube_proxy_vars

			export KUBE_PROXY_APP_NAME

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

		# #Keeping this old code for reference
		# #Finding host of the proxy
		# echo "  Trying to find svc hostname..."
		# CLUSTER_KUBE_PROXY_HOST=$(__kube_cmd_with_timeout "kubectl $KUBECONF get svc $KUBE_PROXY_APP_NAME -n $KUBE_SIM_NAMESPACE  -o jsonpath={.status.loadBalancer.ingress[0].hostname}")


		# if [ "$CLUSTER_KUBE_PROXY_HOST" == "localhost" ]; then
		# 	#Local host found
		# 	echo -e $YELLOW" The test environment svc $KUBE_PROXY_APP_NAME host is: $CLUSTER_KUBE_PROXY_HOST"$EYELLOW
		# 	CLUSTER_KUBE_PROXY_HOST="127.0.0.1"
		# else
		# 	if [ -z "$CLUSTER_KUBE_PROXY_HOST" ]; then
		# 		#Host of proxy not found, trying to find the ip....
		# 		echo "  Trying to find svc ip..."
		# 		CLUSTER_KUBE_PROXY_HOST=$(__kube_cmd_with_timeout "kubectl $KUBECONF get svc $KUBE_PROXY_APP_NAME -n $KUBE_SIM_NAMESPACE  -o jsonpath={.status.loadBalancer.ingress[0].ip}")
		# 		if [ ! -z "$CLUSTER_KUBE_PROXY_HOST" ]; then
		# 			#Host ip found
		# 			echo -e $YELLOW" The test environment svc $KUBE_PROXY_APP_NAME ip is: $CLUSTER_KUBE_PROXY_HOST."$EYELLOW
		# 		fi
		# 	else
		# 		#Host or ip of proxy found
		# 		echo -e $YELLOW" The test environment host/ip is: $CLUSTER_KUBE_PROXY_HOST."$EYELLOW
		# 	fi
		# fi

		# PORT_KEY_PREFIX=""
		# if [ $KUBE_PROXY_HTTPX == "https" ]; then
		# 	PORT_KEY_PREFIX="s"  #add suffix to port key name to get https ports
		# fi
		# if [ -z "$CLUSTER_KUBE_PROXY_HOST" ]; then
		# 	#Host/ip of proxy not found, try to use the cluster and the nodeports of the proxy
		# 	CLUSTER_KUBE_PROXY_HOST=$(kubectl $KUBECONF config view -o jsonpath={.clusters[0].cluster.server} | awk -F[/:] '{print $4}')
		# 	echo -e $YELLOW" The test environment cluster ip is: $CLUSTER_KUBE_PROXY_HOST."$EYELLOW
		# 	CLUSTER_KUBE_PROXY_PORT=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http$PORT_KEY_PREFIX")  # port for proxy access
		# 	KUBE_PROXY_WEB_NODEPORT=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "web$PORT_KEY_PREFIX")  # web port, only for alive test
		# 	echo " Cluster ip/host, cluster http$PORT_KEY_PREFIX nodeport, cluster web$PORT_KEY_PREFIX nodeport: $CLUSTER_KUBE_PROXY_HOST $CLUSTER_KUBE_PROXY_PORT $KUBE_PROXY_WEB_NODEPORT"
		# else
		# 	#Find the service ports of the proxy
		# 	CLUSTER_KUBE_PROXY_PORT=$(__kube_get_service_port $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http$PORT_KEY_PREFIX")  # port for proxy access
		# 	KUBE_PROXY_WEB_NODEPORT=$(__kube_get_service_port $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "web$PORT_KEY_PREFIX")  # web port, only for alive test
		# 	echo " Proxy ip/host, proxy http$PORT_KEY_PREFIX port, proxy web$PORT_KEY_PREFIX port: $CLUSTER_KUBE_PROXY_HOST $CLUSTER_KUBE_PROXY_PORT $KUBE_PROXY_WEB_NODEPORT"
		# fi
		# #End of old code

		########### New method to find host/ip to cluster/proxy
		# Basic principle if the ip/host of the svc for the proxy is found - use the proxy service ports towards that ip/host of the proxy.
		# If proxy ip/host is not found then find the cluster ip/host and use the proxy nodeports towards that ip/host of the cluster

		#Finding host/ip of the proxy
		echo "  Trying to find svc hostname..."
		CLUSTER_KUBE_PROXY_HOST=$(__kube_cmd_with_timeout "kubectl $KUBECONF get svc $KUBE_PROXY_APP_NAME -n $KUBE_SIM_NAMESPACE  -o jsonpath={.status.loadBalancer.ingress[0].hostname}")
		if [ -z "$CLUSTER_KUBE_PROXY_HOST" ]; then
			#Host of proxy not found, trying to find the ip....
			echo "  Svc hostname not found, trying to find svc ip..."
			CLUSTER_KUBE_PROXY_HOST=$(__kube_cmd_with_timeout "kubectl $KUBECONF get svc $KUBE_PROXY_APP_NAME -n $KUBE_SIM_NAMESPACE  -o jsonpath={.status.loadBalancer.ingress[0].ip}")
		fi
		PORT_KEY_PREFIX=""
		if [ $KUBE_PROXY_HTTPX == "https" ]; then
			PORT_KEY_PREFIX="s"  #add suffix to port key name to get https ports
		fi

		if [ -z "$CLUSTER_KUBE_PROXY_HOST" ]; then
			#Finding host/ip of the cluster
			echo "  Nor svc hostname or ip found, trying to find cluster host/ip from context..."
			__current_context=$(kubectl $KUBECONF config current-context)
			__cluster_name=$(kubectl $KUBECONF config view -o "jsonpath={.contexts[?(@.name=='"$__current_context"')].context.cluster}")
			__cluster_server=$(kubectl $KUBECONF config view -o "jsonpath={.clusters[?(@.name=='"$__cluster_name"')].cluster.server}")
			CLUSTER_KUBE_PROXY_HOST=$(echo $__cluster_server | awk -F[/:] '{print $4}')

			echo -e $YELLOW" The test environment cluster: $CLUSTER_KUBE_PROXY_HOST."$EYELLOW
			CLUSTER_KUBE_PROXY_PORT=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http$PORT_KEY_PREFIX")  # port for proxy access
			KUBE_PROXY_WEB_NODEPORT=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "web$PORT_KEY_PREFIX")  # web port, only for alive test
			echo " Cluster ip/host, cluster http$PORT_KEY_PREFIX nodeport, cluster web$PORT_KEY_PREFIX nodeport: $CLUSTER_KUBE_PROXY_HOST $CLUSTER_KUBE_PROXY_PORT $KUBE_PROXY_WEB_NODEPORT"
		else
			echo -e $YELLOW" The test environment proxy: $CLUSTER_KUBE_PROXY_HOST."$EYELLOW
			CLUSTER_KUBE_PROXY_PORT=$(__kube_get_service_port $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http$PORT_KEY_PREFIX")  # port for proxy access
			KUBE_PROXY_WEB_NODEPORT=$(__kube_get_service_port $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "web$PORT_KEY_PREFIX")  # web port, only for alive test
			echo " Proxy ip/host, proxy http$PORT_KEY_PREFIX port, proxy web$PORT_KEY_PREFIX port: $CLUSTER_KUBE_PROXY_HOST $CLUSTER_KUBE_PROXY_PORT $KUBE_PROXY_WEB_NODEPORT"
		fi
		########### End of new method

		KUBE_PROXY_WEB_PATH=$KUBE_PROXY_HTTPX"://"$CLUSTER_KUBE_PROXY_HOST":"$KUBE_PROXY_WEB_NODEPORT

		export KUBE_PROXY_PATH=  # Make sure proxy is empty when checking the proxy itself
		__check_service_start $KUBE_PROXY_APP_NAME $KUBE_PROXY_WEB_PATH$KUBE_PROXY_ALIVE_URL

		# Set proxy for all subsequent calls for all services etc
		export KUBE_PROXY_PATH=$KUBE_PROXY_HTTPX"://"$CLUSTER_KUBE_PROXY_HOST":"$CLUSTER_KUBE_PROXY_PORT
		export KUBE_PROXY_HTTPX

		KP_PORT1=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "http")
		KP_PORT2=$(__kube_get_service_nodeport $KUBE_PROXY_APP_NAME $KUBE_SIM_NAMESPACE "https")

		echo " $KUBE_PROXY_DISPLAY_NAME node ports (http/https): $KP_PORT1 $KP_PORT2"

	else
		# Check if docker app shall be fully managed by the test script
		__check_included_image 'KUBEPROXY'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Kube Proxy app is not included in this test script"$ERED
			echo -e $RED"The Kube Proxy will not be started"$ERED
			exit
		fi

		__kube_proxy_vars

		__start_container $KUBE_PROXY_COMPOSE_DIR "" NODOCKERARGS 1 $KUBE_PROXY_APP_NAME

		if [ $KUBE_PROXY_HTTPX == "http" ]; then
			export KUBE_PROXY_WEB_PATH=$KUBE_PROXY_HTTPX"://"$LOCALHOST_NAME":"$KUBE_PROXY_WEB_DOCKER_EXTERNAL_PORT
		else
			export KUBE_PROXY_WEB_PATH=$KUBE_PROXY_HTTPX"://"$LOCALHOST_NAME":"$KUBE_PROXY_WEB_DOCKER_EXTERNAL_SECURE_PORT
		fi

		export KUBE_PROXY_PATH=  # Make sure proxy is empty when checking the proxy itself
        __check_service_start $KUBE_PROXY_APP_NAME $KUBE_PROXY_WEB_PATH$KUBE_PROXY_ALIVE_URL

		if [ $KUBE_PROXY_HTTPX == "http" ]; then
			export KUBE_PROXY_PATH=$KUBE_PROXY_HTTPX"://"$LOCALHOST_NAME":"$KUBE_PROXY_DOCKER_EXTERNAL_PORT
		else
			export KUBE_PROXY_PATH=$KUBE_PROXY_HTTPX"://"$LOCALHOST_NAME":"$KUBE_PROXY_DOCKER_EXTERNAL_SECURE_PORT
		fi

		echo " $KUBE_PROXY_DISPLAY_NAME localhost ports (http/https): $KUBE_PROXY_DOCKER_EXTERNAL_PORT $KUBE_PROXY_DOCKER_EXTERNAL_SECURE_PORT"


	fi
	echo ""

}

# Turn on debug logging in kubeproxy
# args: -
# (Function for test scripts)
set_kubeproxy_debug() {
	echo -e $BOLD"Setting kubeproxy debug logging"$EBOLD
	curlString="$KUBE_PROXY_WEB_PATH/debug -X PUT"
	result=$(__do_curl_no_proxy "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "could not set debug logging" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

