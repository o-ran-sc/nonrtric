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
# and test functions for Message Router - mr stub

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__MR_imagesetup() {
	__check_and_create_image_var MR "MRSTUB_IMAGE" "MRSTUB_IMAGE_BASE" "MRSTUB_IMAGE_TAG" LOCAL "$MR_STUB_DISPLAY_NAME" $IMAGE_TARGET_PLATFORM_IMG_TAG
}

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__DMAAPMR_imagesetup() {
	__check_and_create_image_var DMAAPMR "ONAP_DMAAPMR_IMAGE"    "ONAP_DMAAPMR_IMAGE_BASE"  "ONAP_DMAAPMR_IMAGE_TAG"   REMOTE_RELEASE_ONAP "DMAAP Message Router" ""
	__check_and_create_image_var DMAAPMR "ONAP_ZOOKEEPER_IMAGE" "ONAP_ZOOKEEPER_IMAGE_BASE" "ONAP_ZOOKEEPER_IMAGE_TAG" REMOTE_RELEASE_ONAP "ZooKeeper" ""
	__check_and_create_image_var DMAAPMR "ONAP_KAFKA_IMAGE"     "ONAP_KAFKA_IMAGE_BASE"     "ONAP_KAFKA_IMAGE_TAG"     REMOTE_RELEASE_ONAP "Kafka" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__MR_imagepull() {
	echo -e $RED"Image for app MR shall never be pulled from remote repo"$ERED
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released (remote) images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__DMAAPMR_imagepull() {
	__check_and_pull_image $2 "DMAAP Message Router" $MR_DMAAP_APP_NAME ONAP_DMAAPMR_IMAGE
	__check_and_pull_image $2 "ZooKeeper" $MR_ZOOKEEPER_APP_NAME ONAP_ZOOKEEPER_IMAGE
	__check_and_pull_image $2 "Kafka" $MR_KAFKA_APP_NAME ONAP_KAFKA_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__MR_imagebuild() {
	cd ../mrstub
	echo " Building MR - $MR_STUB_DISPLAY_NAME - image: $MRSTUB_IMAGE"
	docker build  $IMAGE_TARGET_PLATFORM_CMD_PARAM --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $MRSTUB_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN"  Build Ok"$EGREEN
		__retag_and_push_image MRSTUB_IMAGE
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

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__DMAAPMR_imagebuild() {
	echo -e $RED"Image for app DMAAPMR shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__MR_image_data() {
	echo -e "$MR_STUB_DISPLAY_NAME\t$(docker images --format $1 $MRSTUB_IMAGE)" >>   $2
	if [ ! -z "$MRSTUB_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $MRSTUB_IMAGE_SOURCE)" >>   $2
	fi
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__DMAAPMR_image_data() {
	echo -e "DMAAP Message Router\t$(docker images --format $1 $ONAP_DMAAPMR_IMAGE)" >>   $2
	if [ ! -z "$ONAP_DMAAPMR_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $ONAP_DMAAPMR_IMAGE_SOURCE)" >>   $2
	fi
	echo -e "ZooKeeper\t$(docker images --format $1 $ONAP_ZOOKEEPER_IMAGE)" >>   $2
	if [ ! -z "$ONAP_ZOOKEEPER_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $ONAP_ZOOKEEPER_IMAGE_SOURCE)" >>   $2
	fi
	echo -e "Kafka\t$(docker images --format $1 $ONAP_KAFKA_IMAGE)" >>   $2
	if [ ! -z "$ONAP_KAFKA_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $ONAP_KAFKA_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__MR_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_ONAP_NAMESPACE autotest MR
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__DMAAPMR_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_ONAP_NAMESPACE autotest DMAAPMR
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__MR_kube_scale_zero_and_wait() {
	echo -e " MR replicas kept as is"
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__DMAAPMR_kube_scale_zero_and_wait() {
	echo -e " DMAAP replicas kept as is"
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__MR_kube_delete_all() {
	__kube_delete_all_resources $KUBE_ONAP_NAMESPACE autotest MR
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__DMAAPMR_kube_delete_all() {
	__kube_delete_all_resources $KUBE_ONAP_NAMESPACE autotest DMAAPMR
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__MR_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=MR" -n $KUBE_ONAP_NAMESPACE --tail=-1 > $1$2_mr_stub.log 2>&1
	else
		docker logs $MR_STUB_APP_NAME > $1$2_mr_stub.log 2>&1
	fi
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__DMAAPMR_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		for podname in $(kubectl $KUBECONF get pods -n $KUBE_ONAP_NAMESPACE -l "autotest=DMAAPMR" -o custom-columns=":metadata.name"); do
			kubectl $KUBECONF logs -n $KUBE_ONAP_NAMESPACE $podname --tail=-1 > $1$2_$podname.log 2>&1
		done
	else
		docker logs $MR_DMAAP_APP_NAME > $1$2_mr.log 2>&1
		docker logs $MR_KAFKA_APP_NAME > $1$2_mr_kafka.log 2>&1
		docker logs $MR_ZOOKEEPER_APP_NAME > $1$2_mr_zookeeper.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__MR_initial_setup() {
	use_mr_http
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__DMAAPMR_initial_setup() {
	:  # handle by __MR_initial_setup
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__MR_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "MR-STUB $MR_STUB_APP_NAME $KUBE_ONAP_NAMESPACE"
	else
		echo "MR-STUB $MR_STUB_APP_NAME"
	fi
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__DMAAPMR_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "KAFKA $MR_KAFKA_APP_NAME $KUBE_ONAP_NAMESPACE MESSAGE-ROUTER $MR_DMAAP_APP_NAME $KUBE_ONAP_NAMESPACE ZOOKEEPER $MR_ZOOKEEPER_APP_NAME $KUBE_ONAP_NAMESPACE"
	else
		echo "KAFKA $MR_KAFKA_APP_NAME MESSAGE-ROUTER $MR_DMAAP_APP_NAME ZOOKEEPER $MR_ZOOKEEPER_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__MR_test_requirements() {
	:
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__DMAAPMR_test_requirements() {
	:
}

#######################################################

# Description of port mappings when running MR-STUB only or MR-STUB + MESSAGE-ROUTER
#
# 'MR-STUB only' is started when only 'MR' is included in the test script. Both the test scripts and app will then use MR-STUB as a message-router simulator.
#
# 'MR-STUB + MESSAGE-ROUTER' is started when 'MR' and 'DMAAPMR' is included in the testscripts. DMAAPMR is the real message router including kafka and zookeeper.
# In this configuration, MR-STUB is used by the test-script as frontend to the message-router while app are using the real message-router.
#
# DOCKER                                                                      KUBE
# ---------------------------------------------------------------------------------------------------------------------------------------------------

#                             MR-STUB                                                             MR-STUB
#                             +++++++                                                             +++++++
# localhost                               container                           service                                 pod
# ==============================================================================================================================================
# 10 MR_STUB_LOCALHOST_PORT          ->   13 MR_INTERNAL_PORT                 15 MR_EXTERNAL_PORT                ->   17 MR_INTERNAL_PORT
# 12 MR_STUB_LOCALHOST_SECURE_PORT   ->   14 MR_INTERNAL_SECURE_PORT          16 MR_EXTERNAL_SECURE_PORT		 ->   18 MR_INTERNAL_SECURE_PORT



#                             MESSAGE-ROUTER                                                      MESSAGE-ROUTER
#                             ++++++++++++++                                                      ++++++++++++++
# localhost                               container                           service                                 pod
# ===================================================================================================================================================
# 20 MR_DMAAP_LOCALHOST_PORT         ->   23 MR_INTERNAL_PORT                 25 MR_EXTERNAL_PORT                ->   27 MR_INTERNAL_PORT
# 22 MR_DMAAP_LOCALHOST_SECURE_PORT  ->   24 MR_INTERNAL_SECURE_PORT          26 MR_EXTERNAL_SECURE_PORT   		 ->   28 MR_INTERNAL_SECURE_PORT


# Running only the MR-STUB - apps using MR-STUB
# DOCKER                                                                      KUBE
# localhost:          10 and 12                                                -
# via proxy (script): 13 and 14                                               via proxy (script): 15 and 16
# apps:               13 and 14                                               apps:               15 and 16

# Running MR-STUB (as frontend for test script) and MESSAGE-ROUTER - apps using MESSAGE-ROUTER
# DOCKER                                                                      KUBE
# localhost:          10 and 12                                                -
# via proxy (script): 13 and 14                                               via proxy (script): 15 and 16
# apps:               23 and 24                                               apps:               25 and 26
#



use_mr_http() {
	__mr_set_protocoll "http" $MR_INTERNAL_PORT $MR_EXTERNAL_PORT $MR_INTERNAL_SECURE_PORT $MR_EXTERNAL_SECURE_PORT
}

use_mr_https() {
	__mr_set_protocoll "https" $MR_INTERNAL_PORT $MR_EXTERNAL_PORT $MR_INTERNAL_SECURE_PORT $MR_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port> <internal-secure-port> <external-secure-port>
__mr_set_protocoll() {
	echo -e $BOLD"$MR_STUB_DISPLAY_NAME and $MR_DMAAP_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $MR_STUB_DISPLAY_NAME and $MR_DMAAP_DISPLAY_NAME"

	## Access to Dmaap mediator

	MR_HTTPX=$1

	if [ $MR_HTTPX == "http" ]; then
		INT_PORT=$2
		EXT_PORT=$3
	else
		INT_PORT=$4
		EXT_PORT=$5
	fi

	# Access via test script
	MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME":"$INT_PORT  # access from script via proxy, docker
	MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME":"$INT_PORT # access from script via proxy, docker
	MR_DMAAP_ADAPTER_HTTP="" # Access to dmaap mr via proxy - set only if app is included

	MR_SERVICE_PATH=$MR_STUB_PATH # access container->container, docker -  access pod->svc, kube
	MR_KAFKA_SERVICE_PATH=""
	MR_ZOOKEEPER_SERVICE_PATH=""
	__check_included_image "DMAAPMR"
	if [ $? -eq 0 ]; then
		MR_SERVICE_PATH=$MR_DMAAP_PATH # access container->container, docker -  access pod->svc, kube
		MR_DMAAP_ADAPTER_HTTP=$MR_DMAAP_PATH

		MR_KAFKA_SERVICE_PATH=$MR_KAFKA_APP_NAME":"$MR_KAFKA_PORT
		MR_ZOOKEEPER_SERVICE_PATH=$MR_ZOOKEEPER_APP_NAME":"$MR_ZOOKEEPER_PORT
	fi

	# For directing calls from script to e.g.A1PMS via message router
	# These cases shall always go though the  mr-stub
	MR_ADAPTER_HTTP="http://"$MR_STUB_APP_NAME":"$2
	MR_ADAPTER_HTTPS="https://"$MR_STUB_APP_NAME":"$4

	MR_DMAAP_ADAPTER_TYPE="REST"



	if [ $RUNMODE == "KUBE" ]; then
		MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME.$KUBE_ONAP_NAMESPACE":"$EXT_PORT # access from script via proxy, kube
		MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME.$KUBE_ONAP_NAMESPACE":"$EXT_PORT # access from script via proxy, kube

		MR_SERVICE_PATH=$MR_STUB_PATH
		__check_included_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_DMAAP_PATH
			MR_DMAAP_ADAPTER_HTTP=$MR_DMAAP_PATH
			MR_KAFKA_SERVICE_PATH=$MR_KAFKA_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_KAFKA_PORT
			MR_ZOOKEEPER_SERVICE_PATH=$MR_ZOOKEEPER_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_ZOOKEEPER_PORT
		fi
		__check_prestarted_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_DMAAP_PATH
			MR_DMAAP_ADAPTER_HTTP=$MR_DMAAP_PATH
			MR_KAFKA_SERVICE_PATH=$MR_KAFKA_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_KAFKA_PORT
			MR_ZOOKEEPER_SERVICE_PATH=$MR_ZOOKEEPER_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_ZOOKEEPER_PORT
		fi

		# For directing calls from script to e.g.A1PMS, via message router
		# These calls shall always go though the  mr-stub
		MR_ADAPTER_HTTP="http://"$MR_STUB_APP_NAME.$KUBE_ONAP_NAMESPACE":"$3
		MR_ADAPTER_HTTPS="https://"$MR_STUB_APP_NAME.$KUBE_ONAP_NAMESPACE":"$5
	fi

	# For calls from script to the mr-stub
	MR_STUB_ADAPTER=$MR_STUB_PATH
	MR_STUB_ADAPTER_TYPE="REST"

	echo ""

}

# Export env vars for config files, docker compose and kube resources
# args: -
__dmaapmr_export_vars() {
	#Docker only
	export DOCKER_SIM_NWNAME
	export ONAP_ZOOKEEPER_IMAGE
	export MR_ZOOKEEPER_APP_NAME
	export ONAP_KAFKA_IMAGE
	export MR_KAFKA_APP_NAME
	export ONAP_DMAAPMR_IMAGE
	export MR_DMAAP_APP_NAME
	export MR_DMAAP_LOCALHOST_PORT
	export MR_INTERNAL_PORT
	export MR_DMAAP_LOCALHOST_SECURE_PORT
	export MR_INTERNAL_SECURE_PORT
	export MR_DMAAP_HOST_MNT_DIR

	export KUBE_ONAP_NAMESPACE
	export MR_EXTERNAL_PORT
	export MR_EXTERNAL_SECURE_PORT
	export MR_KAFKA_PORT
	export MR_ZOOKEEPER_PORT

	export MR_KAFKA_SERVICE_PATH
	export MR_ZOOKEEPER_SERVICE_PATH

	export MR_KAFKA_KUBE_NODE_PORT
	export MR_KAFKA_DOCKER_LOCALHOST_PORT
}

# Export env vars for config files, docker compose and kube resources
# args: -
__mr_export_vars() {
	#Docker only
	export DOCKER_SIM_NWNAME
	export MR_STUB_APP_NAME
	export MRSTUB_IMAGE
	export MR_INTERNAL_PORT
	export MR_INTERNAL_SECURE_PORT
	export MR_EXTERNAL_PORT
	export MR_EXTERNAL_SECURE_PORT
	export MR_STUB_LOCALHOST_PORT
	export MR_STUB_LOCALHOST_SECURE_PORT
	export MR_STUB_CERT_MOUNT_DIR
	export MR_STUB_DISPLAY_NAME

	export KUBE_ONAP_NAMESPACE
	export MR_EXTERNAL_PORT

	export MR_KAFKA_SERVICE_PATH
	export MR_ZOOKEEPER_SERVICE_PATH
}


# Start the Message Router stub interface in the simulator group
# args: -
# (Function for test scripts)
start_mr() {

	echo -e $BOLD"Starting $MR_DMAAP_DISPLAY_NAME and/or $MR_STUB_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

        # Table of possible combinations of included mr and included/pre-started dmaap-mr
		# mr can never be pre-started
		# mr can be used stand alone
		# if dmaapmr is included/pre-started, then mr is needed as well as frontend

        # Inverted logic - 0 mean true, 1 means false
		# mr pre-started     0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1
		# mr included        0 0 1 1 0 0 1 1 0 0 1 1 0 0 1 1
		# dmaap pre-started  0 0 0 0 1 1 1 1 0 0 0 0 1 1 1 1
		# dmaap included     0 0 0 0 0 0 0 0 1 1 1 1 1 1 1 1
		# ==================================================
		# OK                 1 1 1 1 1 0 1 1 1 0 1 1 1 0 1 1

		__check_prestarted_image 'MR'
		retcode_prestarted_mr=$?
		__check_included_image 'MR'
		retcode_included_mr=$?

		__check_prestarted_image 'DMAAPMR'
		retcode_prestarted_dmaapmr=$?
		__check_included_image 'DMAAPMR'
		retcode_included_dmaapmr=$?

		paramerror=1

		if [ $retcode_prestarted_mr -ne 0 ] && [ $retcode_included_mr -eq 0 ]; then
			if [ $retcode_prestarted_dmaapmr -ne 0 ] && [ $retcode_included_dmaapmr -eq 0 ]; then
				paramerror=0
			fi
		fi

		if [ $retcode_prestarted_mr -ne 0 ] && [ $retcode_included_mr -eq 0 ]; then
			if [ $retcode_prestarted_dmaapmr -eq 0 ] && [ $retcode_included_dmaapmr -ne 0 ]; then
				paramerror=0
			fi
		fi

		if [ $retcode_prestarted_mr -ne 0 ] && [ $retcode_included_mr -eq 0 ]; then
			if [ $retcode_prestarted_dmaapmr -ne 0 ] && [ $retcode_included_dmaapmr -ne 0 ]; then
				paramerror=0
			fi
		fi

		if [ $paramerror -ne 0 ]; then
				echo -e $RED"The Message Router apps 'MR' and/or 'DMAAPMR' are not included in this test script"$ERED
				echo -e $RED"The Message Router will not be started"$ERED
				echo -e $RED"Both MR and DAAMPMR  - or - only MR - need to be included and/or prestarted"$ERED
				exit
		fi

		if [ $retcode_prestarted_dmaapmr -eq 0 ]; then
			echo -e " Using existing $MR_DMAAP_APP_NAME deployment and service"
			__kube_scale deployment $MR_DMAAP_APP_NAME $KUBE_ONAP_NAMESPACE 1
		fi

		if [ $retcode_included_dmaapmr -eq 0 ]; then

			__dmaapmr_export_vars

			#Check if onap namespace exists, if not create it
			__kube_create_namespace $KUBE_ONAP_NAMESPACE

			# copy config files
			MR_MNT_CONFIG_BASEPATH=$SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR$MR_DMAAP_HOST_MNT_DIR
			cp -r $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR$MR_DMAAP_HOST_CONFIG_DIR/*  $MR_MNT_CONFIG_BASEPATH

			# Create config maps - dmaapmr app
			configfile=$MR_MNT_CONFIG_BASEPATH/mr/MsgRtrApi.properties
			output_yaml=$PWD/tmp/dmaapmr_msgrtrapi_cfc.yaml
			__kube_create_configmap dmaapmr-msgrtrapi.properties $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			configfile=$MR_MNT_CONFIG_BASEPATH/mr/logback.xml
			output_yaml=$PWD/tmp/dmaapmr_logback_cfc.yaml
			__kube_create_configmap dmaapmr-logback.xml $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			configfile=$MR_MNT_CONFIG_BASEPATH/mr/cadi.properties
			output_yaml=$PWD/tmp/dmaapmr_cadi_cfc.yaml
			__kube_create_configmap dmaapmr-cadi.properties $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			# Create config maps - kafka app
			configfile=$MR_MNT_CONFIG_BASEPATH/kafka/zk_client_jaas.conf
			output_yaml=$PWD/tmp/dmaapmr_zk_client_cfc.yaml
			__kube_create_configmap dmaapmr-zk-client-jaas.conf $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			# Create config maps - zookeeper app
			configfile=$MR_MNT_CONFIG_BASEPATH/zk/zk_server_jaas.conf
			output_yaml=$PWD/tmp/dmaapmr_zk_server_cfc.yaml
			__kube_create_configmap dmaapmr-zk-server-jaas.conf $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/dmaapmr_svc.yaml
			__kube_create_instance service $MR_DMAAP_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/dmaapmr_app.yaml
			__kube_create_instance app $MR_DMAAP_APP_NAME $input_yaml $output_yaml


			__check_service_start $MR_DMAAP_APP_NAME $MR_DMAAP_PATH$MR_DMAAP_ALIVE_URL

			echo " Kafka TCP node port $MR_KAFKA_KUBE_NODE_PORT"


			if [ $# -gt 0 ]; then
				if [ $(($#%3)) -eq 0 ]; then
					while [ $# -gt 0 ]; do
						__dmaap_pipeclean "$1" "$2/$1" "$2/$1/$3?timeout=1000&limit=100"
						shift; shift; shift;
					done
				else
					echo -e $RED" args: start_mr [<topic-name> <base-url> <group-and-user-url>]*"$ERED
					echo -e $RED" Got: $@"$ERED
					exit 1
				fi
			fi

			echo " Current topics:"
			curlString="$MR_DMAAP_PATH/topics"
			result=$(__do_curl "$curlString")
			echo $result | indent2

		fi

		if [ $retcode_included_mr -eq 0 ]; then

			__mr_export_vars

			if [ $retcode_prestarted_dmaapmr -eq 0 ] || [ $retcode_included_dmaapmr -eq 0 ]; then  # Set topics for dmaap
				export TOPIC_READ="http://$MR_DMAAP_APP_NAME.$KUBE_ONAP_NAMESPACE:$MR_INTERNAL_PORT/events/$MR_READ_TOPIC"
				export TOPIC_WRITE="http://$MR_DMAAP_APP_NAME.$KUBE_ONAP_NAMESPACE:$MR_INTERNAL_PORT/events/$MR_WRITE_TOPIC/users/mr-stub?timeout=15000&limit=100"
				export GENERIC_TOPICS_UPLOAD_BASEURL="http://$MR_DMAAP_APP_NAME.$KUBE_ONAP_NAMESPACE:$MR_INTERNAL_PORT"
			else
				export TOPIC_READ=""
				export TOPIC_WRITE=""
				export GENERIC_TOPICS_UPLOAD_BASEURL=""
			fi

			#Check if onap namespace exists, if not create it
			__kube_create_namespace $KUBE_ONAP_NAMESPACE

			# Create service
			input_yaml=$SIM_GROUP"/"$MR_STUB_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/mr_svc.yaml
			__kube_create_instance service $MR_STUB_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$MR_STUB_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/mr_app.yaml
			__kube_create_instance app $MR_STUB_APP_NAME $input_yaml $output_yaml


		fi

		__check_service_start $MR_STUB_APP_NAME $MR_STUB_PATH$MR_STUB_ALIVE_URL

	else

		__check_included_image 'DMAAPMR'
		retcode_dmaapmr=$?
		__check_included_image 'MR'
		retcode_mr=$?

		if [ $retcode_dmaapmr -ne 0 ] && [ $retcode_mr -ne 0 ]; then
				echo -e $RED"The Message Router apps 'MR' and/or 'DMAAPMR' are not included in this test script"$ERED
				echo -e $RED"The Message Router will not be started"$ERED
				echo -e $RED"Both MR and DAAMPMR  - or - only MR - need to be included"
				exit
		fi

		if [ $retcode_dmaapmr -eq 0 ] && [ $retcode_mr -ne 0 ]; then
				echo -e $RED"The Message Router apps 'MR' and/or 'DMAAPMR' are not included in this test script"$ERED
				echo -e $RED"The Message Router will not be started"$ERED
				echo -e $RED"Both MR and DAAMPMR  - or - only MR - need to be included"
				exit
		fi

		export TOPIC_READ=""
        export TOPIC_WRITE=""
		export GENERIC_TOPICS_UPLOAD_BASEURL=""
		if [ $retcode_dmaapmr -eq 0 ]; then  # Set topics for dmaap
			export TOPIC_READ="http://$MR_DMAAP_APP_NAME:$MR_INTERNAL_PORT/events/$MR_READ_TOPIC"
			export TOPIC_WRITE="http://$MR_DMAAP_APP_NAME:$MR_INTERNAL_PORT/events/$MR_WRITE_TOPIC/users/mr-stub?timeout=15000&limit=100"
			export GENERIC_TOPICS_UPLOAD_BASEURL="http://$MR_DMAAP_APP_NAME:$MR_INTERNAL_PORT"
		fi

		__dmaapmr_export_vars

		if [ $retcode_dmaapmr -eq 0 ]; then

			# copy config files
			MR_MNT_CONFIG_BASEPATH=$SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR$MR_DMAAP_HOST_MNT_DIR
			cp -r $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR$MR_DMAAP_HOST_CONFIG_DIR/*  $MR_MNT_CONFIG_BASEPATH

			# substitute vars
			configfile=$MR_MNT_CONFIG_BASEPATH/mr/MsgRtrApi.properties
			cp $configfile $configfile"_tmp"
			envsubst < $configfile"_tmp" > $configfile

			__start_container $MR_DMAAP_COMPOSE_DIR "" NODOCKERARGS 1 $MR_DMAAP_APP_NAME

			__check_service_start $MR_DMAAP_APP_NAME $MR_DMAAP_PATH$MR_DMAAP_ALIVE_URL

			echo " Kafka TCP node port $MR_KAFKA_DOCKER_LOCALHOST_PORT"

			if [ $# -gt 0 ]; then
				if [ $(($#%3)) -eq 0 ]; then
					while [ $# -gt 0 ]; do
						__dmaap_pipeclean "$1" "$2/$1" "$2/$1/$3?timeout=1000&limit=100"
						shift; shift; shift;
					done
				else
					echo -e $RED" args: start_mr [<topic-name> <base-url> <group-and-user-url>]*"$ERED
					echo -e $RED" Got: $@"$ERED
					exit 1
				fi
			fi

			dmaap_api_print_topics
		fi

		__mr_export_vars

		if [ $retcode_mr -eq 0 ]; then
			__start_container $MR_STUB_COMPOSE_DIR "" NODOCKERARGS 1 $MR_STUB_APP_NAME

			__check_service_start $MR_STUB_APP_NAME $MR_STUB_PATH$MR_STUB_ALIVE_URL
		fi

	fi
	echo ""
	return 0
}

# Create a dmaap mr topic
# args: <topic name> <topic-description>
__create_topic() {
	echo -ne " Creating topic: $1"$SAMELINE

	json_topic="{\"topicName\":\"$1\",\"partitionCount\":\"2\", \"replicationCount\":\"3\", \"transactionEnabled\":\"false\",\"topicDescription\":\"$2\"}"
	fname="./tmp/$1.json"
	echo $json_topic > $fname

	query="/topics/create"
	topic_retries=10
	while [ $topic_retries -gt 0 ]; do
		let topic_retries=topic_retries-1
		res="$(__do_curl_to_api DMAAPMR POST $query $fname)"
		status=${res:${#res}-3}

		if [[ $status == "2"* ]]; then
			topic_retries=0
			echo -e " Creating topic: $1 $GREEN OK $EGREEN"
		else
			if [ $topic_retries -eq 0 ]; then
				echo -e " Creating topic: $1 $RED Failed $ERED"
				((RES_CONF_FAIL++))
				return 1
			else
				sleep 1
			fi
		fi
	done
	echo
	return 0
}

# Do a pipeclean of a topic - to overcome dmaap mr bug...
# args: <topic> <post-url> <read-url> [<num-retries>]
__dmaap_pipeclean() {
	pipeclean_retries=50
	if [ $# -eq 4 ]; then
		pipeclean_retries=$4
	fi
	echo -ne " Doing dmaap-mr pipe cleaning on topic: $1"$SAMELINE
	while [ $pipeclean_retries -gt 0 ]; do
		if [[ $1 == *".text" ]]; then
			echo "pipeclean-$1:$pipeclean_retries" > ./tmp/__dmaap_pipeclean.txt
			curlString="$MR_DMAAP_PATH$2 -X POST  -H Content-Type:text/plain -d@./tmp/__dmaap_pipeclean.txt"
		else
			echo "{\"pipeclean-$1\":$pipeclean_retries}" > ./tmp/__dmaap_pipeclean.json
			curlString="$MR_DMAAP_PATH$2 -X POST  -H Content-Type:application/json -d@./tmp/__dmaap_pipeclean.json"
		fi
		let pipeclean_retries=pipeclean_retries-1
		result=$(__do_curl "$curlString")
		if [ $? -ne 0 ]; then
			sleep 1
		else
			curlString="$MR_DMAAP_PATH$3"
			result=$(__do_curl "$curlString")
			if [ $? -eq 0 ]; then
				if [ $result != "[]" ]; then
					echo -e " Doing dmaap-mr pipe cleaning on topic: $1 $GREEN OK $EGREEN"
					return 0

				else
					sleep 1
				fi
			fi
		fi
	done
	echo -e "Doing dmaap-mr pipe cleaning on topic: $1 $RED Failed $ERED"
	return 1
}

# Helper function to list the current topics in DMAAP MR
# args: -
dmaap_api_print_topics() {
	echo " Current topics:"
	curlString="$MR_DMAAP_PATH/topics"
	result=$(__do_curl "$curlString")
	echo $result | indent2
}


### Generic test cases for varaible checking

# Tests if a variable value in the MR stub is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
mr_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test "MR" $MR_STUB_PATH/counter/ $1 "=" $2 $3
	else
		((RES_CONF_FAIL++))
		__print_err "Wrong args to mr_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}

# Tests if a variable value in the MR stub is greater than a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# greater than the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes greater than the target
# value or not.
# (Function for test scripts)
mr_greater() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test "MR" $MR_STUB_PATH/counter/ $1 ">" $2 $3
	else
		((RES_CONF_FAIL++))
		__print_err "Wrong args to mr_greater, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}

# Read a variable value from MR sim and send to stdout. Arg: <variable-name>
mr_read() {
	echo "$(__do_curl $MR_STUB_PATH/counter/$1)"
}

# Print a variable value from the MR stub.
# arg: <variable-name>
# (Function for test scripts)
mr_print() {
	if [ $# != 1 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need one arg, <mr-param>" $@
		exit 1
	fi
	echo -e $BOLD"INFO(${BASH_LINENO[0]}): mrstub, $1 = $(__do_curl $MR_STUB_PATH/counter/$1)"$EBOLD
}

# Send json to topic in mr-stub.
# arg: <topic-url> <json-msg>
# (Function for test scripts)
mr_api_send_json() {
	__log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<topic-url> <json-msg>" $@
        return 1
    fi
	query=$1
	fname=$PWD/tmp/json_payload_to_mr.json
	echo $2 > $fname
	res="$(__do_curl_to_api MRSTUB POST $query $fname)"

	status=${res:${#res}-3}
	if [ $status -ne 200 ]; then
		__log_conf_fail_status_code 200 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# Send text to topic in mr-stub.
# arg: <topic-url> <text-msg>
# (Function for test scripts)
mr_api_send_text() {
	__log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<topic-url> <text-msg>" $@
        return 1
    fi
	query=$1
	fname=$PWD/tmp/text_payload_to_mr.txt
	echo $2 > $fname
	res="$(__do_curl_to_api MRSTUB POST $query $fname text/plain)"

	status=${res:${#res}-3}
	if [ $status -ne 200 ]; then
		__log_conf_fail_status_code 200 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# Send json file to topic in mr-stub.
# arg: <topic-url> <json-file>
# (Function for test scripts)
mr_api_send_json_file() {
	__log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<topic-url> <json-file>" $@
        return 1
    fi
	query=$1
	if [ ! -f $2 ]; then
		__log_test_fail_general "File $2 does not exist"
		return 1
	fi
	#Create json array for mr
	datafile="tmp/mr_api_send_json_file.json"
	{ echo -n "[" ; cat $2 ; echo -n "]" ;} > $datafile

	res="$(__do_curl_to_api MRSTUB POST $query $datafile)"

	status=${res:${#res}-3}
	if [ $status -ne 200 ]; then
		__log_conf_fail_status_code 200 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# Send text file to topic in mr-stub.
# arg: <topic-url> <text-file>
# (Function for test scripts)
mr_api_send_text_file() {
	__log_conf_start $@
    if [ $# -ne 2 ]; then
        __print_err "<topic-url> <text-file>" $@
        return 1
    fi
	query=$1
	if [ ! -f $2 ]; then
		__log_test_fail_general "File $2 does not exist"
		return 1
	fi

	res="$(__do_curl_to_api MRSTUB POST $query $2 text/plain)"

	status=${res:${#res}-3}
	if [ $status -ne 200 ]; then
		__log_conf_fail_status_code 200 $status
		return 1
	fi

	__log_conf_ok
	return 0
}

# Create json file for payload
# arg: <size-in-kb> <filename>
mr_api_generate_json_payload_file() {
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
mr_api_generate_text_payload_file() {
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
