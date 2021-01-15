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
		rics=$(docker ps | grep $RIC_SIM_PREFIX | awk '{print $NF}')

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

	__start_container $CONSUL_CBS_COMPOSE_DIR NODOCKERARGS 2 $CONSUL_APP_NAME $CBS_APP_NAME

	__check_service_start $CONSUL_APP_NAME "http://"$LOCALHOST_NAME":"$CONSUL_EXTERNAL_PORT$CONSUL_ALIVE_URL
	__check_service_start $CBS_APP_NAME "http://"$LOCALHOST_NAME":"$CBS_EXTERNAL_PORT$CBS_ALIVE_URL

	echo ""
}

