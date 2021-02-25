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

# This is a script that contains container/service management functions and test functions for Consul/CBS

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for that exist with staging, snapshot,release tags
__CONSUL_imagesetup() {
	__check_and_create_image_var CONSUL "CONSUL_IMAGE" "CONSUL_IMAGE_BASE" "CONSUL_IMAGE_TAG" REMOTE_PROXY "$CONSUL_DISPLAY_NAME"

}

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CBS_imagesetup() {
	__check_and_create_image_var  CBS "CBS_IMAGE" "CBS_IMAGE_BASE" "CBS_IMAGE_TAG" REMOTE_RELEASE_ONAP "$CBS_DISPLAY_NAME"

}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CONSUL_imagepull() {
	__check_and_pull_image $2 "$CONSUL_DISPLAY_NAME" $CONSUL_APP_NAME $CONSUL_IMAGE
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CBS_imagepull() {
	__check_and_pull_image $2 "$CBS_DISPLAY_NAME" $CBS_APP_NAME $CBS_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CONSUL_imagebuild() {
	echo -e $RED" Image for app CONSUL shall never be built"$ERED
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CBS_imagebuild() {
	echo -e $RED" Image for app CBS shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# arg: <docker-images-format-string> <file-to-append>
__CONSUL_image_data() {
	echo -e "$CONSUL_DISPLAY_NAME\t$(docker images --format $1 $CONSUL_IMAGE)" >>   $2
}

# Generate a string for each included image using the app display name and a docker images format string
# arg: <docker-images-format-string> <file-to-append>
__CBS_image_data() {
	echo -e "$CBS_DISPLAY_NAME\t$(docker images --format $1 $CBS_IMAGE)" >>   $2
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CONSUL_kube_scale_zero() {
	echo -e $RED" Image for app CONSUL is not used in kube"$ERED
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CBS_kube_scale_zero() {
	echo -e $RED" Image for app CBS is not used in kube"$ERED
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__CONSUL_kube_scale_zero_and_wait() {
	echo -e $RED" CONSUL app is not used in kube"$ERED
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__CBS_kube_scale_zero_and_wait() {
	echo -e $RED" CBS app is not used in kube"$ERED
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__CONSUL_kube_delete_all() {
	echo -e $RED" CONSUL app is not used in kube"$ERED
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__CBS_kube_delete_all() {
	echo -e $RED" CBS app is not used in kube"$ERED
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__CONSUL_store_docker_logs() {
	docker logs $CONSUL_APP_NAME > $1/$2_consul.log 2>&1
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__CBS_store_docker_logs() {
	docker logs $CBS_APP_NAME > $1$2_cbs.log 2>&1
	body="$(__do_curl $LOCALHOST_HTTP:$CBS_EXTERNAL_PORT/service_component_all/$POLICY_AGENT_APP_NAME)"
	echo "$body" > $1$2_consul_config.json 2>&1
}

#######################################################

CONSUL_PATH="http://$LOCALHOST:$CONSUL_EXTERNAL_PORT"

####################
### Consul functions
####################

# Function to load config from a file into consul for the Policy Agent
# arg: <json-config-file>
# (Function for test scripts)
consul_config_app() {

	echo -e $BOLD"Configuring Consul"$EBOLD

	if [ $# -ne 1 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need one arg,  <json-config-file>" $@
		exit 1
	fi

	echo " Loading config for "$POLICY_AGENT_APP_NAME" from "$1

	curlString="$LOCALHOST_HTTP:${CONSUL_EXTERNAL_PORT}/v1/kv/${POLICY_AGENT_CONFIG_KEY}?dc=dc1 -X PUT -H Accept:application/json -H Content-Type:application/json -H X-Requested-With:XMLHttpRequest --data-binary @"$1
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		echo -e $RED" FAIL - json config could not be loaded to consul" $ERED
		((RES_CONF_FAIL++))
		return 1
	fi
	body="$(__do_curl $LOCALHOST_HTTP:$CBS_EXTERNAL_PORT/service_component_all/$POLICY_AGENT_CONFIG_KEY)"
	echo $body > "./tmp/.output"$1

	if [ $? -ne 0 ]; then
		echo -e $RED" FAIL - json config could not be loaded from consul/cbs, contents cannot be checked." $ERED
		((RES_CONF_FAIL++))
		return 1
	else
		targetJson=$(< $1)
		targetJson="{\"config\":"$targetJson"}"
		echo "TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")
		if [ $res -ne 0 ]; then
			echo -e $RED" FAIL - policy json config read from consul/cbs is not equal to the intended json config...." $ERED
			((RES_CONF_FAIL++))
			return 1
		else
			echo -e $GREEN" Config loaded ok to consul"$EGREEN
		fi
	fi

	echo ""

}

# Function to perpare the consul configuration according to the current simulator configuration
# args: SDNC|NOSDNC <output-file>
# (Function for test scripts)
prepare_consul_config() {
  	echo -e $BOLD"Prepare Consul config"$EBOLD

	echo " Writing consul config for "$POLICY_AGENT_APP_NAME" to file: "$2

	if [ $# != 2 ];  then
		((RES_CONF_FAIL++))
    	__print_err "need two args,  SDNC|NOSDNC <output-file>" $@
		exit 1
	fi

	if [ $1 == "SDNC" ]; then
		echo -e " Config$BOLD including SDNC$EBOLD configuration"
	elif [ $1 == "NOSDNC" ];  then
		echo -e " Config$BOLD excluding SDNC$EBOLD configuration"
	else
		((RES_CONF_FAIL++))
    	__print_err "need two args,  SDNC|NOSDNC <output-file>" $@
		exit 1
	fi

	config_json="\n            {"
	if [ $1 == "SDNC" ]; then
		config_json=$config_json"\n   \"controller\": ["
		config_json=$config_json"\n                     {"
		config_json=$config_json"\n                       \"name\": \"$SDNC_APP_NAME\","
		config_json=$config_json"\n                       \"baseUrl\": \"$SDNC_SERVICE_PATH\","
		config_json=$config_json"\n                       \"userName\": \"$SDNC_USER\","
		config_json=$config_json"\n                       \"password\": \"$SDNC_PWD\""
		config_json=$config_json"\n                     }"
		config_json=$config_json"\n   ],"
	fi

	config_json=$config_json"\n   \"streams_publishes\": {"
	config_json=$config_json"\n                            \"dmaap_publisher\": {"
	config_json=$config_json"\n                              \"type\": \"message-router\","
	config_json=$config_json"\n                              \"dmaap_info\": {"
	config_json=$config_json"\n                                \"topic_url\": \"$MR_SERVICE_PATH$MR_WRITE_URL\""
	config_json=$config_json"\n                              }"
	config_json=$config_json"\n                            }"
	config_json=$config_json"\n   },"
	config_json=$config_json"\n   \"streams_subscribes\": {"
	config_json=$config_json"\n                             \"dmaap_subscriber\": {"
	config_json=$config_json"\n                               \"type\": \"message-router\","
	config_json=$config_json"\n                               \"dmaap_info\": {"
	config_json=$config_json"\n                                   \"topic_url\": \"$MR_SERVICE_PATH$MR_READ_URL\""
	config_json=$config_json"\n                                 }"
	config_json=$config_json"\n                               }"
	config_json=$config_json"\n   },"

	config_json=$config_json"\n   \"ric\": ["

	if [ $RUNMODE == "KUBE" ]; then
		result=$(kubectl get pods -n $KUBE_NONRTRIC_NAMESPACE -o jsonpath='{.items[?(@.metadata.labels.autotest=="RICSIM")].metadata.name}')
		rics=""
		ric_cntr=0
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			for im in $result; do
				if [[ $im != *"-0" ]]; then
					ric_subdomain=$(kubectl get pod $im -n $KUBE_NONRTRIC_NAMESPACE -o jsonpath='{.spec.subdomain}')
					rics=$rics" "$im"."$ric_subdomain".nonrtric"
					let ric_cntr=ric_cntr+1
				fi
			done
		fi
		if [ $ric_cntr -eq 0 ]; then
			echo $YELLOW"Warning: No rics found for the configuration"$EYELLOW
		fi
	else
		rics=$(docker ps --filter "name=$RIC_SIM_PREFIX" --filter "network=$DOCKER_SIM_NWNAME" --filter "status=running" --format {{.Names}})
		if [ $? -ne 0 ] || [ -z "$rics" ]; then
			echo -e $RED" FAIL - the names of the running RIC Simulator cannot be retrieved." $ERED
			((RES_CONF_FAIL++))
			return 1
		fi
	fi
	cntr=0
	for ric in $rics; do
		if [ $cntr -gt 0 ]; then
			config_json=$config_json"\n          ,"
		fi
		config_json=$config_json"\n          {"
		if [ $RUNMODE == "KUBE" ]; then
			ric_id=${ric%.*.*} #extract pod id from full hosthame
			ric_id=$(echo "$ric_id" | tr '-' '_')
		else
			ric_id=$ric
		fi
		echo " Found a1 sim: "$ric_id
		config_json=$config_json"\n            \"name\": \"$ric_id\","
		config_json=$config_json"\n            \"baseUrl\": \"$RIC_SIM_HTTPX://$ric:$RIC_SIM_PORT\","
		if [ $1 == "SDNC" ]; then
			config_json=$config_json"\n            \"controller\": \"$SDNC_APP_NAME\","
		fi
		config_json=$config_json"\n            \"managedElementIds\": ["
		config_json=$config_json"\n              \"me1_$ric_id\","
		config_json=$config_json"\n              \"me2_$ric_id\""
		config_json=$config_json"\n            ]"
		config_json=$config_json"\n          }"
		let cntr=cntr+1
	done

	config_json=$config_json"\n           ]"
	config_json=$config_json"\n}"

	if [ $RUNMODE == "KUBE" ]; then
		config_json="{\"config\":"$config_json"}"
	fi

	printf "$config_json">$2

	echo ""
}

# Start Consul and CBS
# args: -
# (Function for test scripts)
start_consul_cbs() {

	echo -e $BOLD"Starting $CONSUL_DISPLAY_NAME and $CBS_DISPLAY_NAME"$EBOLD
	__check_included_image 'CONSUL'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Consul image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"Consul will not be started"$ERED
		exit
	fi
	export CONSUL_APP_NAME
	export CONSUL_INTERNAL_PORT
	export CONSUL_EXTERNAL_PORT
	export CBS_APP_NAME
	export CBS_INTERNAL_PORT
	export CBS_EXTERNAL_PORT
	export CONSUL_HOST
	export CONSUL_DISPLAY_NAME
	export CBS_DISPLAY_NAME

	__start_container $CONSUL_CBS_COMPOSE_DIR "" NODOCKERARGS 2 $CONSUL_APP_NAME $CBS_APP_NAME

	__check_service_start $CONSUL_APP_NAME "http://"$LOCALHOST_NAME":"$CONSUL_EXTERNAL_PORT$CONSUL_ALIVE_URL
	__check_service_start $CBS_APP_NAME "http://"$LOCALHOST_NAME":"$CBS_EXTERNAL_PORT$CBS_ALIVE_URL

	echo ""
}

