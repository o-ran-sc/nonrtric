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

# This is a script that contains management and test functions for Policy Agent


## Access to Policy agent
# Host name may be changed if app started by kube
# Direct access from script
PA_HTTPX="http"
PA_HOST_NAME=$LOCALHOST_NAME
PA_PATH=$PA_HTTPX"://"$PA_HOST_NAME":"$POLICY_AGENT_EXTERNAL_PORT

# PA_ADAPTER used for switch between REST and DMAAP
PA_ADAPTER_TYPE="REST"
PA_ADAPTER=$PA_PATH

# Make curl retries towards the agent for http response codes set in this env var, space separated list of codes
AGENT_RETRY_CODES=""

###########################
### Policy Agents functions
###########################

# All calls to the agent will be directed to the agent REST interface from now on
# args: -
# (Function for test scripts)
use_agent_rest_http() {
	echo -e $BOLD"Agent protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD and $BOLD REST $EBOLD towards the agent"
	PA_HTTPX="http"
	PA_PATH=$PA_HTTPX"://"$PA_HOST_NAME":"$POLICY_AGENT_EXTERNAL_PORT

	PA_ADAPTER_TYPE="REST"
	PA_ADAPTER=$PA_PATH
	echo ""
}

# All calls to the agent will be directed to the agent REST interface from now on
# args: -
# (Function for test scripts)
use_agent_rest_https() {
	echo -e $BOLD"Agent protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD and $BOLD REST $EBOLD towards the agent"
	PA_HTTPX="https"
	PA_PATH=$PA_HTTPX"://"$PA_HOST_NAME":"$POLICY_AGENT_EXTERNAL_SECURE_PORT

	PA_ADAPTER_TYPE="REST"
	PA_ADAPTER=$PA_PATH
	echo ""
}

# All calls to the agent will be directed to the agent dmaap interface over http from now on
# args: -
# (Function for test scripts)
use_agent_dmaap_http() {
	echo -e $BOLD"Agent dmaap protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD and $BOLD DMAAP $EBOLD towards the agent"
	PA_ADAPTER_TYPE="MR-HTTP"
	echo ""
}

# All calls to the agent will be directed to the agent dmaap interface over https from now on
# args: -
# (Function for test scripts)
use_agent_dmaap_https() {
	echo -e $BOLD"Agent dmaap protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD and $BOLD DMAAP $EBOLD towards the agent"
	echo -e $YELLOW" Setting http instead of https - MR only uses http"$EYELLOW
	PA_ADAPTER_TYPE="MR-HTTPS"
	echo ""
}

# Start the policy agent
# args: (docker) PROXY|NOPROXY <config-file>
# args: (kube) PROXY|NOPROXY <config-file> [ <data-file>]
# (Function for test scripts)
start_policy_agent() {
	echo -e $BOLD"Starting $POLICY_AGENT_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "PA"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "PA"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $POLICY_AGENT_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $POLICY_AGENT_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $POLICY_AGENT_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $POLICY_AGENT_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $POLICY_AGENT_APP_NAME deployment and service"
			echo " Setting $POLICY_AGENT_APP_NAME replicas=1"
			__kube_scale deployment $POLICY_AGENT_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $POLICY_AGENT_APP_NAME app and expose service"

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			#Export all vars needed for service and deployment
			export POLICY_AGENT_APP_NAME
			export KUBE_NONRTRIC_NAMESPACE
			export POLICY_AGENT_IMAGE
			export POLICY_AGENT_INTERNAL_PORT
			export POLICY_AGENT_INTERNAL_SECURE_PORT
			export POLICY_AGENT_EXTERNAL_PORT
			export POLICY_AGENT_EXTERNAL_SECURE_PORT
			export POLICY_AGENT_CONFIG_MOUNT_PATH
			export POLICY_AGENT_DATA_MOUNT_PATH
			export POLICY_AGENT_CONFIG_CONFIGMAP_NAME=$POLICY_AGENT_APP_NAME"-config"
			export POLICY_AGENT_DATA_CONFIGMAP_NAME=$POLICY_AGENT_APP_NAME"-data"
			export POLICY_AGENT_PKG_NAME
			if [ $1 == "PROXY" ]; then
				AGENT_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT  #Set if proxy is started
				AGENT_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
				if [ $AGENT_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$AGENT_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
					echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
				else
					echo " Configured with http proxy"
				fi
			else
				AGENT_HTTP_PROXY_CONFIG_PORT=0
				AGENT_HTTP_PROXY_CONFIG_HOST_NAME=""
				echo " Configured without http proxy"
			fi
			export AGENT_HTTP_PROXY_CONFIG_PORT
			export AGENT_HTTP_PROXY_CONFIG_HOST_NAME


			# Create config map for config
			configfile=$PWD/tmp/$POLICY_AGENT_CONFIG_FILE
			cp $2 $configfile
			output_yaml=$PWD/tmp/pa_cfc.yaml
			__kube_create_configmap $POLICY_AGENT_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest PA $configfile $output_yaml

			# Create config map for data
			data_json=$PWD/tmp/$POLICY_AGENT_DATA_FILE
			if [ $# -lt 3 ]; then
				#create empty dummy file
				echo "{}" > $data_json
			else
				cp $3 $data_json
			fi
			output_yaml=$PWD/tmp/pa_cfd.yaml
			__kube_create_configmap $POLICY_AGENT_DATA_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest PA $data_json $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$POLICY_AGENT_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/pa_svc.yaml
			__kube_create_instance service $POLICY_AGENT_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$POLICY_AGENT_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/pa_app.yaml
			__kube_create_instance app $POLICY_AGENT_APP_NAME $input_yaml $output_yaml

		fi

		echo " Retrieving host and ports for service..."
		PA_HOST_NAME=$(__kube_get_service_host $POLICY_AGENT_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
		POLICY_AGENT_EXTERNAL_PORT=$(__kube_get_service_port $POLICY_AGENT_APP_NAME $KUBE_NONRTRIC_NAMESPACE "http")
		POLICY_AGENT_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $POLICY_AGENT_APP_NAME $KUBE_NONRTRIC_NAMESPACE "https")

		echo " Host IP, http port, https port: $PA_HOST_NAME $POLICY_AGENT_EXTERNAL_PORT $POLICY_AGENT_EXTERNAL_SECURE_PORT"

		if [ $PA_HTTPX == "http" ]; then
			PA_PATH=$PA_HTTPX"://"$PA_HOST_NAME":"$POLICY_AGENT_EXTERNAL_PORT
		else
			PA_PATH=$PA_HTTPX"://"$PA_HOST_NAME":"$POLICY_AGENT_EXTERNAL_SECURE_PORT
		fi
		__check_service_start $POLICY_AGENT_APP_NAME $PA_PATH$POLICY_AGENT_ALIVE_URL

		if [ $PA_ADAPTER_TYPE == "REST" ]; then
			PA_ADAPTER=$PA_PATH
		fi
	else
		__check_included_image 'PA'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Policy Agent app is not included in this test script"$ERED
			echo -e $RED"The Policy Agent will not be started"$ERED
			exit
		fi

		#Export all vars needed for docker-compose
		export POLICY_AGENT_APP_NAME
		export POLICY_AGENT_APP_NAME_ALIAS
		export POLICY_AGENT_INTERNAL_PORT
		export POLICY_AGENT_EXTERNAL_PORT
		export POLICY_AGENT_INTERNAL_SECURE_PORT
		export POLICY_AGENT_EXTERNAL_SECURE_PORT
		export CONSUL_HOST
		export CONSUL_INTERNAL_PORT
		export CONFIG_BINDING_SERVICE
		export POLICY_AGENT_CONFIG_KEY
		export DOCKER_SIM_NWNAME
		export POLICY_AGENT_HOST_MNT_DIR
		export POLICY_AGENT_CONFIG_MOUNT_PATH
		export POLICY_AGENT_CONFIG_FILE
		export POLICY_AGENT_PKG_NAME

		if [ $1 == "PROXY" ]; then
			AGENT_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT  #Set if proxy is started
			AGENT_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
			if [ $AGENT_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$AGENT_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
				echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
			else
				echo " Configured with http proxy"
			fi
		else
			AGENT_HTTP_PROXY_CONFIG_PORT=0
			AGENT_HTTP_PROXY_CONFIG_HOST_NAME=""
			echo " Configured without http proxy"
		fi
		export AGENT_HTTP_PROXY_CONFIG_PORT
		export AGENT_HTTP_PROXY_CONFIG_HOST_NAME

		dest_file=$SIM_GROUP/$POLICY_AGENT_COMPOSE_DIR/$POLICY_AGENT_HOST_MNT_DIR/application.yaml

		envsubst < $2 > $dest_file

		__start_container $POLICY_AGENT_COMPOSE_DIR NODOCKERARGS 1 $POLICY_AGENT_APP_NAME

		__check_service_start $POLICY_AGENT_APP_NAME $PA_PATH$POLICY_AGENT_ALIVE_URL
	fi
	echo ""
	return 0
}

# Load the the appl config for the agent into a config map
agent_load_config() {
	echo -e $BOLD"Agent - load config from "$EBOLD$1
	data_json=$PWD/tmp/$POLICY_AGENT_DATA_FILE
	cp $1 $data_json
	output_yaml=$PWD/tmp/pa_cfd.yaml
	__kube_create_configmap $POLICY_AGENT_APP_NAME"-data" $KUBE_NONRTRIC_NAMESPACE autotest PA $data_json $output_yaml
}


# Turn on debug level tracing in the agent
# args: -
# (Function for test scripts)
set_agent_debug() {
	echo -e $BOLD"Setting agent debug logging"$EBOLD
	curlString="$PA_PATH$POLICY_AGENT_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "could not set debug mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Turn on trace level tracing in the agent
# args: -
# (Function for test scripts)
set_agent_trace() {
	echo -e $BOLD"Setting agent trace logging"$EBOLD
	curlString="$PA_PATH$POLICY_AGENT_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "could not set trace mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Perform curl retries when making direct call to the agent for the specified http response codes
# Speace separated list of http response codes
# args: [<response-code>]*
use_agent_retries() {
	echo -e $BOLD"Do curl retries to the agent REST inteface for these response codes:$@"$EBOLD
	AGENT_RETRY_CODES=$@
	echo ""
	return
}

#########################################################
#### Test case functions A1 Policy management service
#########################################################

# This function compare the size, towards a target value, of a json array returned from <url> of the Policy Agent.
# This is done immediately by setting PASS or FAIL or wait up to and optional timeout before setting PASS or FAIL
# args: json:<url> <target-value> [<timeout-in-seconds]
# (Function for test scripts)
api_equal() {
    echo "(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >> $HTTPLOG
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		if [[ $1 == "json:"* ]]; then
			if [ "$PMS_VERSION" == "V2" ]; then
				__var_test "Policy Agent" $PA_PATH$PMS_API_PREFIX"/v2/" $1 "=" $2 $3
			else
				__var_test "Policy Agent" $PA_PATH"/" $1 "=" $2 $3
			fi
			return 0
		fi
	fi
	__print_err "needs two or three args: json:<json-array-param> <target-value> [ timeout ]" $@
	return 1
}

# API Test function: GET /policies and V2 GET /v2/policy-instances
# args: <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <template-file>]*]
# args(V2): <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <transient> <notification-url> <template-file>]*]
# (Function for test scripts)
api_get_policies() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		paramError=0
		variableParams=$(($#-4))
		if [ $# -lt 4 ]; then
			paramError=1
		elif [ $# -eq 5 ] && [ $5 != "NOID" ]; then
			paramError=1
		elif [ $# -gt 5 ] && [ $(($variableParams%7)) -ne 0 ]; then
			paramError=1
		fi

		if [ $paramError -ne 0 ]; then
			__print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <transient> <notification-url> <template-file>]*]" $@
			return 1
		fi
	else
		paramError=0
		variableParams=$(($#-4))
		if [ $# -lt 4 ]; then
			paramError=1
		elif [ $# -eq 5 ] && [ $5 != "NOID" ]; then
			paramError=1
		elif [ $# -gt 5 ] && [ $(($variableParams%5)) -ne 0 ]; then
			paramError=1
		fi

		if [ $paramError -ne 0 ]; then
			__print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <template-file>]*]" $@
			return 1
		fi
	fi

	queryparams=""
	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $2 != "NORIC" ]; then
			queryparams="?ric_id="$2
		fi
		if [ $3 != "NOSERVICE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?service_id="$3
			else
				queryparams=$queryparams"&service_id="$3
			fi
		fi
		if [ $4 != "NOTYPE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?policytype_id="$4
			else
				queryparams=$queryparams"&policytype_id="$4
			fi
		fi

		query="/v2/policy-instances"$queryparams
		res="$(__do_curl_to_api PA GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi

		if [ $# -gt 4 ]; then
			body=${res:0:${#res}-3}
			if [ $# -eq 5 ] && [ $5 == "NOID" ]; then
				targetJson="["
			else
				targetJson="["
				arr=(${@:5})

				for ((i=0; i<$(($#-4)); i=i+7)); do

					if [ "$targetJson" != "[" ]; then
						targetJson=$targetJson","
					fi
					targetJson=$targetJson"{\"policy_id\":\"$UUID${arr[$i]}\",\"ric_id\":\"${arr[$i+1]}\",\"service_id\":\"${arr[$i+2]}\",\"policytype_id\":"
					if [ "${arr[$i+3]}" == "EMPTY" ]; then
						targetJson=$targetJson"\"\","
					else
						targetJson=$targetJson"\"${arr[$i+3]}\","
					fi
					targetJson=$targetJson"\"transient\":${arr[$i+4]},\"status_notification_uri\":\"${arr[$i+5]}\","
					file="./tmp/.p.json"
					sed 's/XXX/'${arr[$i]}'/g' ${arr[$i+6]} > $file
					json=$(cat $file)
					targetJson=$targetJson"\"policy_data\":"$json"}"
				done
			fi

			targetJson=$targetJson"]"
			targetJson="{\"policies\": $targetJson}"
			echo "TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		if [ $2 != "NORIC" ]; then
			queryparams="?ric="$2
		fi
		if [ $3 != "NOSERVICE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?service="$3
			else
				queryparams=$queryparams"&service="$3
			fi
		fi
		if [ $4 != "NOTYPE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?type="$4
			else
				queryparams=$queryparams"&type="$4
			fi
		fi

		query="/policies"$queryparams
		res="$(__do_curl_to_api PA GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi

		if [ $# -gt 4 ]; then
			if [ $# -eq 5 ] && [ $5 == "NOID" ]; then
				targetJson="["
			else
				body=${res:0:${#res}-3}
				targetJson="["
				arr=(${@:5})

				for ((i=0; i<$(($#-4)); i=i+5)); do

					if [ "$targetJson" != "[" ]; then
						targetJson=$targetJson","
					fi
					targetJson=$targetJson"{\"id\":\"$UUID${arr[$i]}\",\"lastModified\":\"????\",\"ric\":\"${arr[$i+1]}\",\"service\":\"${arr[$i+2]}\",\"type\":"
					if [ "${arr[$i+3]}" == "EMPTY" ]; then
						targetJson=$targetJson"\"\","
					else
						targetJson=$targetJson"\"${arr[$i+3]}\","
					fi
					file="./tmp/.p.json"
					sed 's/XXX/'${arr[$i]}'/g' ${arr[$i+4]} > $file
					json=$(cat $file)
					targetJson=$targetJson"\"json\":"$json"}"
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
	fi

	__log_test_pass
	return 0

}


# API Test function: GET /policy and V2 GET /v2/policies/{policy_id}
# args: <response-code>  <policy-id> [<template-file>]
# args(V2): <response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>|NOTYPE <transient> <notification-url>|NOURL ]

# (Function for test scripts)
api_get_policy() {
	__log_test_start $@


	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -ne 2 ] && [ $# -ne 8 ]; then
			__print_err "<response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>|NOTYPE <transient> <notification-url>|NOURL ]" $@
			return 1
		fi
		query="/v2/policies/$UUID$2"
	else
		if [ $# -lt 2 ] || [ $# -gt 3 ]; then
			__print_err "<response-code>  <policy-id> [<template-file>] " $@
			return 1
		fi
		query="/policy?id=$UUID$2"
	fi
	res="$(__do_curl_to_api PA GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -eq 8 ]; then

			#Create a policy json to compare with
			body=${res:0:${#res}-3}

			targetJson="\"ric_id\":\"$5\",\"policy_id\":\"$UUID$2\",\"service_id\":\"$4\""
			if [ $7 != "NOTRANSIENT" ]; then
				targetJson=$targetJson", \"transient\":$7"
			fi
			if [ $6 != "NOTYPE" ]; then
				targetJson=$targetJson", \"policytype_id\":\"$6\""
			else
				targetJson=$targetJson", \"policytype_id\":\"\""
			fi
			if [ $8 != "NOURL" ]; then
				targetJson=$targetJson", \"status_notification_uri\":\"$8\""
			fi

			data=$(sed 's/XXX/'${2}'/g' $3)
			targetJson=$targetJson", \"policy_data\":$data"
			targetJson="{$targetJson}"

			echo "TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")
			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		if [ $# -eq 3 ]; then
			#Create a policy json to compare with
			body=${res:0:${#res}-3}
			file="./tmp/.p.json"
			sed 's/XXX/'${2}'/g' $3 > $file
			targetJson=$(< $file)
			echo "TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")
			if [ $res -ne 0 ]; then
				__log_test_fail_body
			fi
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT /policy and V2 PUT /policies
# args: <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <template-file> [<count>]
# args(V2): <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <notification-url>|NOURL <template-file> [<count>]
# (Function for test scripts)
api_put_policy() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -lt 8 ] || [ $# -gt 9 ]; then
			__print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <notification-url>|NOURL <template-file> [<count>]" $@
			return 1
		fi
	else
		if [ $# -lt 7 ] || [ $# -gt 8 ]; then
			__print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <template-file> [<count>]" $@
			return 1
		fi
	fi

	count=0
	max=1
	serv=$2
	ric=$3
	pt=$4
	pid=$5
	trans=$6

	if [ "$PMS_VERSION" == "V2" ]; then
		noti=$7
		temp=$8
		if [ $# -eq 9 ]; then
			max=$9
		fi
	else
		temp=$7
		if [ $# -eq 8 ]; then
			max=$8
		fi
	fi

	while [ $count -lt $max ]; do
		if [ "$PMS_VERSION" == "V2" ]; then

			query="/v2/policies"

			inputJson="\"ric_id\":\"$ric\",\"policy_id\":\"$UUID$pid\",\"service_id\":\"$serv\""
			if [ $trans != "NOTRANSIENT" ]; then
				inputJson=$inputJson", \"transient\":$trans"
			fi
			if [ $pt != "NOTYPE" ]; then
				inputJson=$inputJson", \"policytype_id\":\"$pt\""
			else
				inputJson=$inputJson", \"policytype_id\":\"\""
			fi
			if [ $noti != "NOURL" ]; then
				inputJson=$inputJson", \"status_notification_uri\":\"$noti\""
			fi
			file="./tmp/.p.json"
			data=$(sed 's/XXX/'${pid}'/g' $temp)
			inputJson=$inputJson", \"policy_data\":$data"
			inputJson="{$inputJson}"
			echo $inputJson > $file
		else
			query="/policy?id=$UUID$pid&ric=$ric&service=$serv"

			if [ $pt != "NOTYPE" ]; then
				query=$query"&type=$pt"
			fi

			if [ $trans != NOTRANSIENT ]; then
				query=$query"&transient=$trans"
			fi

			file="./tmp/.p.json"
			sed 's/XXX/'${pid}'/g' $temp > $file
		fi
    	res="$(__do_curl_to_api PA PUT $query $file)"
    	status=${res:${#res}-3}
		echo -ne " Executing "$count"("$max")${SAMELINE}"
		if [ $status -ne $1 ]; then
			echo " Executed "$count"?("$max")"
			__log_test_fail_status_code $1 $status
			return 1
		fi

		let pid=$pid+1
		let count=$count+1
		echo -ne " Executed  "$count"("$max")${SAMELINE}"
	done
	echo ""

	__log_test_pass
	return 0
}

# API Test function: PUT /policy and V2 PUT /policies, to run in batch
# args: <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <template-file> [<count>]
# args(V2): <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <notification-url>|NOURL <template-file> [<count>]
# (Function for test scripts)

api_put_policy_batch() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -lt 8 ] || [ $# -gt 9 ]; then
			__print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <notification-url>|NOURL <template-file> [<count>]" $@
			return 1
		fi
	else
		if [ $# -lt 7 ] || [ $# -gt 8 ]; then
			__print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <template-file> [<count>]" $@
			return 1
		fi
	fi

	count=0
	max=1
	serv=$2
	ric=$3
	pt=$4
	pid=$5
	trans=$6
	if [ "$PMS_VERSION" == "V2" ]; then
		noti=$7
		temp=$8
		if [ $# -eq 9 ]; then
			max=$9
		fi
	else
		temp=$7
		if [ $# -eq 8 ]; then
			max=$8
		fi
	fi

	ARR=""
	while [ $count -lt $max ]; do
		if [ "$PMS_VERSION" == "V2" ]; then
			query="/v2/policies"

			inputJson="\"ric_id\":\"$ric\",\"policy_id\":\"$UUID$pid\",\"service_id\":\"$serv\""
			if [ $trans != "NOTRANSIENT" ]; then
				inputJson=$inputJson", \"transient\":$trans"
			fi
			if [ $pt != "NOTYPE" ]; then
				inputJson=$inputJson", \"policytype_id\":\"$pt\""
			else
				inputJson=$inputJson", \"policytype_id\":\"\""
			fi
			if [ $noti != "NOURL" ]; then
				inputJson=$inputJson", \"status_notification_uri\":\"$noti\""
			fi
			file="./tmp/.p.json"
			data=$(sed 's/XXX/'${pid}'/g' $temp)
			inputJson=$inputJson", \"policy_data\":$data"
			inputJson="{$inputJson}"
			echo $inputJson > $file
		else
			query="/policy?id=$UUID$pid&ric=$ric&service=$serv"

			if [ $pt != "NOTYPE" ]; then
				query=$query"&type=$pt"
			fi

			if [ $trans != NOTRANSIENT ]; then
				query=$query"&transient=$trans"
			fi
			file="./tmp/.p.json"
			sed 's/XXX/'${pid}'/g' $temp > $file
		fi
    	res="$(__do_curl_to_api PA PUT_BATCH $query $file)"
    	status=${res:${#res}-3}
		echo -ne " Requesting(batch) "$count"("$max")${SAMELINE}"

		if [ $status -ne 200 ]; then
			echo " Requested(batch) "$count"?("$max")"
			__log_test_fail_status_code 200 $status
			return 1
		fi
		cid=${res:0:${#res}-3}
		ARR=$ARR" "$cid
		let pid=$pid+1
		let count=$count+1
		echo -ne " Requested(batch)  "$count"("$max")${SAMELINE}"
	done

	echo ""
	count=0
	for cid in $ARR; do

    	res="$(__do_curl_to_api PA RESPONSE $cid)"
    	status=${res:${#res}-3}
		echo -ne " Accepting(batch) "$count"("$max")${SAMELINE}"

		if [ $status -ne $1 ]; then
			echo " Accepted(batch) "$count"?("$max")"
			__log_test_fail_status_code $1 $status
			return 1
		fi

		let count=$count+1
		echo -ne " Accepted(batch)  "$count"("$max")${SAMELINE}"
	done

	echo ""

	__log_test_pass
	return 0
}

# API Test function: PUT /policy and V2 PUT /policies, to run in i parallel for a number of rics
# args: <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>
# args(V2): <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>
# (Function for test scripts)
api_put_policy_parallel() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -ne 11 ]; then
			__print_err "<response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>" $@
			return 1
		fi
	else
		if [ $# -ne 10 ]; then
			__print_err " <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>" $@
			return 1
		fi
	fi
	resp_code=$1; shift;
	serv=$1; shift
	ric_base=$1; shift;
	num_rics=$1; shift;
	type=$1; shift;
	start_id=$1; shift;
	transient=$1; shift;
	if [ "$PMS_VERSION" == "V2" ]; then
		noti=$1; shift;
	else
		noti=""
	fi
	template=$1; shift;
	count=$1; shift;
	pids=$1; shift;

	#if [ $PA_ADAPTER != $RESTBASE ] && [ $PA_ADAPTER != $RESTBASE_SECURE ]; then
	if [ $PA_ADAPTER_TYPE != "REST" ]; then
		echo " Info - api_put_policy_parallel uses only the agent REST interface - create over dmaap in parallel is not supported"
		echo " Info - will execute over agent REST"
	fi
	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $serv == "NOSERVICE" ]; then
			serv=""
		fi
		query="$PMS_API_PREFIX/v2/policies"
	else
		if [ $serv == "NOSERVICE" ]; then
			serv=""
		fi
		query="/policy?service=$serv"

		if [ $type != "NOTYPE" ]; then
			query=$query"&type=$type"
		fi

		if [ $transient != NOTRANSIENT ]; then
			query=$query"&transient=$transient"
		fi
	fi

	urlbase=${PA_ADAPTER}${query}

	for ((i=1; i<=$pids; i++))
	do
		uuid=$UUID
		if [ -z "$uuid" ]; then
			uuid="NOUUID"
		fi
		echo "" > "./tmp/.pid${i}.res.txt"
		if [ "$PMS_VERSION" == "V2" ]; then
			echo $resp_code $urlbase $ric_base $num_rics $uuid $start_id $serv $type $transient $noti $template $count $pids $i > "./tmp/.pid${i}.txt"
		else
			echo $resp_code $urlbase $ric_base $num_rics $uuid $start_id $template $count $pids $i > "./tmp/.pid${i}.txt"
		fi
		echo $i
	done  | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.txt")
		python3 ../common/create_policies_process.py $tmp > ./tmp/.pid${arg}.res.txt
	}'
	msg=""
	for ((i=1; i<=$pids; i++))
	do
		file="./tmp/.pid${i}.res.txt"
		tmp=$(< $file)
		if [ -z "$tmp" ]; then
			echo " Process $i : unknown result (result file empty"
			msg="failed"
		else
			res=${tmp:0:1}
			if [ $res == "0" ]; then
				echo " Process $i : OK - "${tmp:1}
			else
				echo " Process $i : failed - "${tmp:1}
				msg="failed"
			fi
		fi
	done
	if [ -z $msg ]; then
		__log_test_pass " $(($count*$num_rics)) policy request(s) executed"
		return 0
	fi

	__log_test_fail_general "One of more processes failed to execute"
	return 1
}

# API Test function: DELETE /policy and V2 DELETE /v2/policies/{policy_id}
# args: <response-code> <policy-id> [count]
# (Function for test scripts)
api_delete_policy() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
        __print_err "<response-code> <policy-id> [count]" $@
        return 1
    fi

	count=0
	max=1

	if [ $# -eq 3 ]; then
		max=$3
	fi

	pid=$2

	while [ $count -lt $max ]; do
		if [ "$PMS_VERSION" == "V2" ]; then
			query="/v2/policies/"$UUID$pid
		else
			query="/policy?id="$UUID$pid
		fi
		res="$(__do_curl_to_api PA DELETE $query)"
		status=${res:${#res}-3}
		echo -ne " Executing "$count"("$max")${SAMELINE}"

		if [ $status -ne $1 ]; then
			echo " Executed "$count"?("$max")"
			__log_test_fail_status_code $1 $status
			return 1
		fi
		let pid=$pid+1
		let count=$count+1
		echo -ne " Executed  "$count"("$max")${SAMELINE}"
	done
	echo ""

	__log_test_pass
	return 0
}

# API Test function: DELETE /policy and V2 DELETE /v2/policies/{policy_id}, to run in batch
# args: <response-code> <policy-id> [count]
# (Function for test scripts)
api_delete_policy_batch() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
        __print_err "<response-code> <policy-id> [count]" $@
        return 1
    fi

	count=0
	max=1

	if [ $# -eq 3 ]; then
		max=$3
	fi

	pid=$2
	ARR=""
	while [ $count -lt $max ]; do
		if [ "$PMS_VERSION" == "V2" ]; then
			query="/v2/policies/"$UUID$pid
		else
			query="/policy?id="$UUID$pid
		fi
		res="$(__do_curl_to_api PA DELETE_BATCH $query)"
		status=${res:${#res}-3}
		echo -ne " Requesting(batch) "$count"("$max")${SAMELINE}"

		if [ $status -ne 200 ]; then
			echo " Requested(batch) "$count"?("$max")"
			__log_test_fail_status_code 200 $status
			return 1
		fi
		cid=${res:0:${#res}-3}
		ARR=$ARR" "$cid
		let pid=$pid+1
		let count=$count+1
		echo -ne " Requested(batch)  "$count"("$max")${SAMELINE}"
	done

	echo ""

	count=0
	for cid in $ARR; do

    	res="$(__do_curl_to_api PA RESPONSE $cid)"
    	status=${res:${#res}-3}
		echo -ne " Deleting(batch) "$count"("$max")${SAMELINE}"

		if [ $status -ne $1 ]; then
			echo " Deleted(batch) "$count"?("$max")"
			__log_test_fail_status_code $1 $status
			return 1
		fi

		let count=$count+1
		echo -ne " Deleted(batch)  "$count"("$max")${SAMELINE}"
	done

	echo ""

	__log_test_pass
	return 0
}

# API Test function: DELETE /policy and V2 DELETE /v2/policies/{policy_id}, to run in i parallel for a number of rics
# args: <response-code> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>
# (Function for test scripts)
api_delete_policy_parallel() {
	__log_test_start $@

    if [ $# -ne 5 ]; then
        __print_err " <response-code> <ric-id-base> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>" $@
        return 1
    fi
	resp_code=$1; shift;
	num_rics=$1; shift;
	start_id=$1; shift;
	count=$1; shift;
	pids=$1; shift;

	#if [ $PA_ADAPTER != $RESTBASE ] && [ $PA_ADAPTER != $RESTBASE_SECURE ]; then
	if [ $PA_ADAPTER_TYPE != "REST" ]; then
		echo " Info - api_delete_policy_parallel uses only the agent REST interface - create over dmaap in parallel is not supported"
		echo " Info - will execute over agent REST"
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
		query="$PMS_API_PREFIX/v2/policies/"
	else
		query="/policy"
	fi

	urlbase=${PA_ADAPTER}${query}

	for ((i=1; i<=$pids; i++))
	do
		uuid=$UUID
		if [ -z "$uuid" ]; then
			uuid="NOUUID"
		fi
		echo "" > "./tmp/.pid${i}.del.res.txt"
		echo $resp_code $urlbase $num_rics $uuid $start_id $count $pids $i > "./tmp/.pid${i}.del.txt"
		echo $i
	done  | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.del.txt")
		python3 ../common/delete_policies_process.py $tmp > ./tmp/.pid${arg}.del.res.txt
	}'
	msg=""
	for ((i=1; i<=$pids; i++))
	do
		file="./tmp/.pid${i}.del.res.txt"
		tmp=$(< $file)
		if [ -z "$tmp" ]; then
			echo " Process $i : unknown result (result file empty"
			msg="failed"
		else
			res=${tmp:0:1}
			if [ $res == "0" ]; then
				echo " Process $i : OK - "${tmp:1}
			else
				echo " Process $i : failed - "${tmp:1}
				msg="failed"
			fi
		fi
	done
	if [ -z $msg ]; then
		__log_test_pass " $(($count*$num_rics)) policy request(s) executed"
		return 0
	fi

	__log_test_fail_general "One of more processes failed to execute"
	return 1
}

# API Test function: GET /policy_ids and V2 GET /v2/policies
# args: <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)
# (Function for test scripts)
api_get_policy_ids() {
	__log_test_start $@

    if [ $# -lt 4 ]; then
		__print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)" $@
		return 1
	fi

	queryparams=""

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $2 != "NORIC" ]; then
			queryparams="?ric_id="$2
		fi

		if [ $3 != "NOSERVICE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?service_id="$3
			else
				queryparams=$queryparams"&service_id="$3
			fi
		fi
		if [ $4 != "NOTYPE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?policytype_id="$4
			else
				queryparams=$queryparams"&policytype_id="$4
			fi
		fi

		query="/v2/policies"$queryparams
	else
		if [ $2 != "NORIC" ]; then
			queryparams="?ric="$2
		fi

		if [ $3 != "NOSERVICE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?service="$3
			else
				queryparams=$queryparams"&service="$3
			fi
		fi
		if [ $4 != "NOTYPE" ]; then
			if [ -z $queryparams ]; then
				queryparams="?type="$4
			else
				queryparams=$queryparams"&type="$4
			fi
		fi

		query="/policy_ids"$queryparams
	fi

    res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 4 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:5} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "NOID" ]; then
				targetJson=$targetJson"\"$UUID$pid\""
			fi
		done

		targetJson=$targetJson"]"
		if [ "$PMS_VERSION" == "V2" ]; then
			targetJson="{\"policy_ids\": $targetJson}"
		fi
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

# API Test function: V2 GET /v2/policy-types/{policyTypeId}
# args(V2): <response-code> <policy-type-id> [<schema-file>]
# (Function for test scripts)
api_get_policy_type() {
	__log_test_start $@

	if [ "$PMS_VERSION" != "V2" ]; then
		__log_test_fail_not_supported
		return 1
	fi

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
        __print_err "<response-code> <policy-type-id> [<schema-file>]" $@
        return 1
    fi
	query="/v2/policy-types/$2"

	res="$(__do_curl_to_api PA GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 3 ]; then

		body=${res:0:${#res}-3}

		targetJson=$(< $3)
		targetJson="{\"policy_schema\":$targetJson}"
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

# API Test function: GET /policy_schema
# args: <response-code> <policy-type-id> [<schema-file>]
# (Function for test scripts)
api_get_policy_schema() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		__log_test_fail_not_supported
		return 1
	fi

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
        __print_err "<response-code> <policy-type-id> [<schema-file>]" $@
        return 1
    fi
	query="/policy_schema?id=$2"
	res="$(__do_curl_to_api PA GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 3 ]; then

		body=${res:0:${#res}-3}

		targetJson=$(< $3)

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

# API Test function: GET /policy_schemas
# args: <response-code>  <ric-id>|NORIC [<schema-file>|NOFILE]*
# args(V2): <response-code>
# (Function for test scripts)
api_get_policy_schemas() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -ne 1 ]; then
			__print_err "<response-code>" $@
			return 1
		fi
	else
		if [ $# -lt 2 ]; then
			__print_err "<response-code> <ric-id>|NORIC [<schema-file>|NOFILE]*" $@
			return 1
		fi
	fi
	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/policy-schemas"
	else
		query="/policy_schemas"
		if [ $2 != "NORIC" ]; then
			query=$query"?ric="$2
		fi
	fi

	res="$(__do_curl_to_api PA GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for file in ${@:3} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $file == "NOFILE" ]; then
				targetJson=$targetJson"{}"
			else
				targetJson=$targetJson$(< $file)
			fi
		done

		targetJson=$targetJson"]"
		if [ "$PMS_VERSION" == "V2" ]; then
			targetJson="{\"policy_schemas\": $targetJson }"
		fi
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

# API Test function: GET /policy_status and V2 GET /policies/{policy_id}/status
# arg: <response-code> <policy-id> (STD|STD2 <enforce-status>|EMPTY [<reason>|EMPTY])|(OSC <instance-status> <has-been-deleted>)
# (Function for test scripts)
api_get_policy_status() {
	__log_test_start $@

    if [ $# -lt 4 ] || [ $# -gt 5 ]; then
		__print_err "<response-code> <policy-id> (STD <enforce-status>|EMPTY [<reason>|EMPTY])|(OSC <instance-status> <has-been-deleted>)" $@
		return 1
	fi

	targetJson=""

	if [ $3 == "STD" ]; then
		targetJson="{\"enforceStatus\":\"$4\""
		if [ $# -eq 5 ]; then
			targetJson=$targetJson",\"reason\":\"$5\""
		fi
		targetJson=$targetJson"}"
	elif [ $3 == "STD2" ]; then
		if [ $4 == "EMPTY" ]; then
			targetJson="{\"enforceStatus\":\"\""
		else
			targetJson="{\"enforceStatus\":\"$4\""
		fi
		if [ $# -eq 5 ]; then
			if [ $5 == "EMPTY" ]; then
				targetJson=$targetJson",\"enforceReason\":\"\""
			else
				targetJson=$targetJson",\"enforceReason\":\"$5\""
			fi
		fi
		targetJson=$targetJson"}"
	elif [ $3 == "OSC" ]; then
		targetJson="{\"instance_status\":\"$4\""
		if [ $# -eq 5 ]; then
			targetJson=$targetJson",\"has_been_deleted\":\"$5\""
		fi
		targetJson=$targetJson",\"created_at\":\"????\"}"
	else
		__print_err "<response-code> (STD <enforce-status> [<reason>])|(OSC <instance-status> <has-been-deleted>)" $@
		return 1
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/policies/$UUID$2/status"
		targetJson="{\"last_modified\":\"????\",\"status\":$targetJson}"
	else
		query="/policy_status?id="$UUID$2
	fi

	res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	echo "TARGET JSON: $targetJson" >> $HTTPLOG
	body=${res:0:${#res}-3}
	res=$(python3 ../common/compare_json.py "$targetJson" "$body")

	if [ $res -ne 0 ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /policy_types and V2 GET /v2/policy-types
# args: <response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]
# (Function for test scripts)
api_get_policy_types() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]" $@
		return 1
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -eq 1 ]; then
			query="/v2/policy-types"
		elif [ $2 == "NORIC" ]; then
			query="/v2/policy-types"
		else
			query="/v2/policy-types?ric_id=$2"
		fi
	else
		if [ $# -eq 1 ]; then
			query="/policy_types"
		elif [ $2 == "NORIC" ]; then
			query="/policy_types"
		else
			query="/policy_types?ric=$2"
		fi
	fi

    res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:3} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid == "EMPTY" ]; then
				pid=""
			fi
			targetJson=$targetJson"\"$pid\""
		done

		targetJson=$targetJson"]"
		if [ "$PMS_VERSION" == "V2" ]; then
			targetJson="{\"policytype_ids\": $targetJson }"
		fi
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

#########################################################
#### Test case functions Health check
#########################################################

# API Test function: GET /status and V2 GET /status
# args: <response-code>
# (Function for test scripts)
api_get_status() {
	__log_test_start $@
    if [ $# -ne 1 ]; then
		__print_err "<response-code>" $@
		return 1
	fi
	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/status"
	else
		query="/status"
	fi
    res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

#########################################################
#### Test case functions RIC Repository
#########################################################

# API Test function: GET /ric and V2 GET /v2/rics/ric
# args: <reponse-code> <management-element-id> [<ric-id>]
# (V2) args: <reponse-code> <management-element-id>|NOME <ric-id>|<NORIC> [<string-of-ricinfo>]
# (V2) example of <string-of-ricinfo> = "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2,4"
# (V2) format of ric-info:  <ric-id>:<list-of-mes>:<list-of-policy-type-ids>


# (Function for test scripts)
api_get_ric() {
	__log_test_start $@

	if [ "$PMS_VERSION" == "V2" ]; then
		if [ $# -lt 3 ]; then
			__print_err "<reponse-code> <management-element-id>|NOME <ric-id>|<NORIC> [string-of-ricinfo>]" $@
			return 1
		fi
		search=""
		if [ $2 != "NOME" ]; then
			search="?managed_element_id="$2
		fi
		if [ $3 != "NORIC" ]; then
			if [ -z $search ]; then
				search="?ric_id="$3
			else
				search=$search"&ric_id="$3
			fi
		fi
		query="/v2/rics/ric"$search

		res="$(__do_curl_to_api PA GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi

		if [ $# -gt 3 ]; then
			body=${res:0:${#res}-3}
			res=$(python3 ../common/create_rics_json.py "./tmp/.tmp_rics.json" "V2" "$4" )
			if [ $res -ne 0 ]; then
				__log_test_fail_general "Could not create target ric info json"
				return 1
			fi

			targetJson=$(<./tmp/.tmp_rics.json)
			targetJson=${targetJson:1:${#targetJson}-2} #remove array brackets
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")
			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		if [ $# -lt 2 ] || [ $# -gt 3 ]; then
			__print_err "<reponse-code> <management-element-id> [<ric-id>]" $@
			return 1
		fi

		query="/ric?managedElementId="$2

		res="$(__do_curl_to_api PA GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi

		if [ $# -eq 3 ]; then
			body=${res:0:${#res}-3}
			if [ "$body" != "$3" ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	fi
	__log_test_pass
	return 0
}

# API test function: GET /rics and V2 GET /v2/rics
# args: <reponse-code> <policy-type-id>|NOTYPE [<space-separate-string-of-ricinfo>]
# example of <space-separate-string-of-ricinfo> = "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2,4 ricsim_g1_1:me2_........."
# format of ric-info:  <ric-id>:<list-of-mes>:<list-of-policy-type-ids>
# (Function for test scripts)
api_get_rics() {
	__log_test_start $@

    if [ $# -lt 2 ]; then
		__print_err "<reponse-code> <policy-type-id>|NOTYPE [<space-separate-string-of-ricinfo>]" $@
		return 1
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/rics"
		if [ $2 != "NOTYPE" ]; then
			query="/v2/rics?policytype_id="$2
		fi
	else
		query="/rics"
		if [ $2 != "NOTYPE" ]; then
			query="/rics?policyType="$2
		fi
	fi

    res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		if [ "$PMS_VERSION" == "V2" ]; then
			res=$(python3 ../common/create_rics_json.py "./tmp/.tmp_rics.json" "V2" "$3" )
		else
			res=$(python3 ../common/create_rics_json.py "./tmp/.tmp_rics.json" "V1" "$3" )
		fi
		if [ $res -ne 0 ]; then
			__log_test_fail_general "Could not create target ric info json"
			return 1
		fi

		targetJson=$(<./tmp/.tmp_rics.json)
		if [ "$PMS_VERSION" == "V2" ]; then
			targetJson="{\"rics\": $targetJson }"
		fi
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

##################################################################
#### API Test case functions Service registry and supervision ####
##################################################################

# API test function: PUT /service and V2 PUT /service
# args: <response-code>  <service-name> <keepalive-timeout> <callbackurl>
# (Function for test scripts)
api_put_service() {
	__log_test_start $@
    if [ $# -ne 4 ]; then
        __print_err "<response-code>  <service-name> <keepalive-timeout> <callbackurl>" $@
        return 1
    fi

	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/services"
		json="{\"callback_url\": \""$4"\",\"keep_alive_interval_seconds\": \""$3"\",\"service_id\": \""$2"\"}"
	else
		query="/service"
		json="{\"callbackUrl\": \""$4"\",\"keepAliveIntervalSeconds\": \""$3"\",\"serviceName\": \""$2"\"}"
	fi
    file="./tmp/.tmp.json"
	echo "$json" > $file

    res="$(__do_curl_to_api PA PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API test function: GET /services and V2 GET /v2/services
#args: <response-code> [ (<query-service-name> <target-service-name> <keepalive-timeout> <callbackurl>) | (NOSERVICE <target-service-name> <keepalive-timeout> <callbackurl> [<target-service-name> <keepalive-timeout> <callbackurl>]* )]
# (Function for test scripts)
api_get_services() {
	__log_test_start $@
	#Number of accepted parameters: 1, 2, 4, 7, 10, 13,...
	paramError=1
	if [ $# -eq 1 ]; then
		paramError=0
	elif [ $# -eq 2 ] && [ $2 != "NOSERVICE" ]; then
		paramError=0
	elif [ $# -eq 5 ]; then
		paramError=0
	elif [ $# -gt 5 ] && [ $2 == "NOSERVICE" ]; then
		argLen=$(($#-2))
		if [ $(($argLen%3)) -eq 0 ]; then
			paramError=0
		fi
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> [ (<query-service-name> <target-service-name> <keepalive-timeout> <callbackurl>) | (NOSERVICE <target-service-name> <keepalive-timeout> <callbackurl> [<target-service-name> <keepalive-timeout> <callbackurl>]* )]" $@
		return 1
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/services"

		if [ $# -gt 1 ] && [ $2 != "NOSERVICE" ]; then
			query="/v2/services?service_id="$2
		fi
	else
		query="/services"

		if [ $# -gt 1 ] && [ $2 != "NOSERVICE" ]; then
			query="/services?name="$2
		fi
	fi
    res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		variableArgCount=$(($#-2))
		body=${res:0:${#res}-3}
    	targetJson="["
		shift; shift;
		cntr=0
		while [ $cntr -lt $variableArgCount ]; do
			servicename=$1; shift;
			timeout=$1; shift;
			callback=$1; shift;
			if [ $cntr -gt 0 ]; then
				targetJson=$targetJson","
			fi
			# timeSinceLastActivitySeconds value cannot be checked since value varies
			if [ "$PMS_VERSION" == "V2" ]; then
				targetJson=$targetJson"{\"service_id\": \""$servicename"\",\"keep_alive_interval_seconds\": "$timeout",\"time_since_last_activity_seconds\":\"????\",\"callback_url\": \""$callback"\"}"
			else
				targetJson=$targetJson"{\"serviceName\": \""$servicename"\",\"keepAliveIntervalSeconds\": "$timeout",\"timeSinceLastActivitySeconds\":\"????\",\"callbackUrl\": \""$callback"\"}"
			fi
			let cntr=cntr+3
		done
		targetJson=$targetJson"]"
		if [ "$PMS_VERSION" == "V2" ]; then
			targetJson="{\"service_list\": $targetJson }"
		fi
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

# API test function: GET /services V2 GET /v2/services -  (only checking service names)
# args: <response-code> [<service-name>]*"
# (Function for test scripts)
api_get_service_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<service-name>]*" $@
		return 1
	fi

	if [ "$PMS_VERSION" == "V2" ]; then
	    query="/v2/services"
	else
    	query="/services"
	fi
    res="$(__do_curl_to_api PA GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	body=${res:0:${#res}-3}
	targetJson="["
	for rapp in ${@:2} ; do
		if [ "$targetJson" != "[" ]; then
			targetJson=$targetJson","
		fi
		if [ "$PMS_VERSION" == "V2" ]; then
			targetJson=$targetJson"{\"callback_url\":\"????\",\"keep_alive_interval_seconds\":\"????\",\"service_id\":\""$rapp"\",\"time_since_last_activity_seconds\":\"????\"}"
		else
			targetJson=$targetJson"{\"callbackUrl\":\"????\",\"keepAliveIntervalSeconds\":\"????\",\"serviceName\":\""$rapp"\",\"timeSinceLastActivitySeconds\":\"????\"}"
		fi
	done

	targetJson=$targetJson"]"
	if [ "$PMS_VERSION" == "V2" ]; then
		targetJson="{\"service_list\": $targetJson }"
	fi
	echo "TARGET JSON: $targetJson" >> $HTTPLOG
	res=$(python3 ../common/compare_json.py "$targetJson" "$body")

	if [ $res -ne 0 ]; then
		__log_test_fail_body
		return 1
	fi

	__log_test_pass
	return 0
}

# API test function: DELETE /services and V2 DELETE /v2/services/{serviceId}
# args: <response-code> <service-name>
# (Function for test scripts)
api_delete_services() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <service-name>" $@
		return 1
	fi
	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/services/"$2
	else
		query="/services?name="$2
	fi
    res="$(__do_curl_to_api PA DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API test function: PUT /services/keepalive and V2 PUT /v2/services/{service_id}/keepalive
# args: <response-code> <service-name>
# (Function for test scripts)
api_put_services_keepalive() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <service-name>" $@
		return 1
	fi
	if [ "$PMS_VERSION" == "V2" ]; then
		query="/v2/services/$2/keepalive"
	else
    	query="/services/keepalive?name="$2
	fi

    res="$(__do_curl_to_api PA PUT $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

##################################################################
#### API Test case functions Configuration                    ####
##################################################################

# API Test function: PUT /v2/configuration
# args: <response-code> <config-file>
# (Function for test scripts)
api_put_configuration() {
	__log_test_start $@

	if [ "$PMS_VERSION" != "V2" ]; then
		__log_test_fail_not_supported
		return 1
	fi

    if [ $# -ne 2 ]; then
        __print_err "<response-code> <config-file>" $@
        return 1
    fi
	if [ ! -f $2 ]; then
		_log_test_fail_general "Config file "$2", does not exist"
		return 1
	fi
	inputJson=$(< $2)
	inputJson="{\"config\":"$inputJson"}"
	file="./tmp/.config.json"
	echo $inputJson > $file
	query="/v2/configuration"
	res="$(__do_curl_to_api PA PUT $query $file)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /v2/configuration
# args: <response-code> [<config-file>]
# (Function for test scripts)
api_get_configuration() {
	__log_test_start $@

	if [ "$PMS_VERSION" != "V2" ]; then
		__log_test_fail_not_supported
		return 1
	fi

    if [ $# -lt 1 ] || [ $# -gt 2 ]; then
        __print_err "<response-code> [<config-file>]" $@
        return 1
    fi
	if [ ! -f $2 ]; then
		_log_test_fail_general "Config file "$2" for comparison, does not exist"
		return 1
	fi

	query="/v2/configuration"
	res="$(__do_curl_to_api PA GET $query)"
	status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 2 ]; then

		body=${res:0:${#res}-3}

		targetJson=$(< $2)
		targetJson="{\"config\":"$targetJson"}"
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