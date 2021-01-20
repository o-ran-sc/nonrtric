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

## Access to Message Router
# Host name may be changed if app started by kube
# Direct access from script
MR_HTTPX="http"
MR_STUB_HOST_NAME=$LOCALHOST_NAME
MR_DMAAP_HOST_NAME=$LOCALHOST_NAME
MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_HOST_NAME":"$MR_STUB_LOCALHOST_PORT
MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_HOST_NAME":"$MR_DMAAP_LOCALHOST_PORT
#Docker/Kube internal path
if [ $RUNMODE == "KUBE" ]; then
	MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_PORT
	__check_included_image "DMAAPMR"
	if [ $? -eq 0 ]; then
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_PORT
	fi
	__check_prestarted_image "DMAAPMR"
	if [ $? -eq 0 ]; then
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_PORT
	fi
else
	MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME":"$MR_INTERNAL_PORT
	__check_included_image "DMAAPMR"
	if [ $? -eq 0 ]; then
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME":"$MR_INTERNAL_PORT
	fi
fi
MR_ADAPTER_HTTP="http://"$MR_STUB_HOST_NAME":"$MR_STUB_LOCALHOST_PORT
MR_ADAPTER_HTTPS="https://"$MR_STUB_HOST_NAME":"$MR_STUB_LOCALHOST_SECURE_PORT


#####################
### MR stub functions
#####################

use_mr_http() {
	echo -e $BOLD"MR protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD towards MR"
	MR_HTTPX="http"
	MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_HOST_NAME":"$MR_STUB_LOCALHOST_PORT
	MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_HOST_NAME":"$MR_DMAAP_LOCALHOST_PORT
	#Docker/Kube internal path
	if [ $RUNMODE == "KUBE" ]; then
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_PORT
		__check_included_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_PORT
		fi
		__check_prestarted_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_PORT
		fi
	else
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME":"$MR_INTERNAL_PORT
		__check_included_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME":"$MR_INTERNAL_PORT
		fi
	fi
	echo ""
}

use_mr_https() {
	echo -e $BOLD"MR protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD towards MR"
	MR_HTTPX="https"
	MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_HOST_NAME":"$MR_STUB_LOCALHOST_SECURE_PORT
	MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_HOST_NAME":"$MR_DMAAP_LOCALHOST_SECURE_PORT
	#Docker/Kube internal path
	if [ $RUNMODE == "KUBE" ]; then
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_SECURE_PORT
		__check_included_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_SECURE_PORT
		fi
		__check_prestarted_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXTERNAL_SECURE_PORT
		fi
	else
		MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME":"$MR_INTERNAL_SECURE_PORT
		__check_included_image "DMAAPMR"
		if [ $? -eq 0 ]; then
			MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME":"$MR_INTERNAL_SECURE_PORT
		fi
	fi
	echo ""
}

# Create a dmaap mr topic
# args: <topic name> <topic-description>
__create_topic() {
	echo -ne " Creating read topic: $1"$SAMELINE

	json_topic="{\"topicName\":\"$1\",\"partitionCount\":\"2\", \"replicationCount\":\"3\", \"transactionEnabled\":\"false\",\"topicDescription\":\"$2\"}"
	echo $json_topic > ./tmp/$1.json

	curlString="$MR_DMAAP_PATH/topics/create -X POST  -H Content-Type:application/json -d@./tmp/$1.json"
	topic_retries=5
	while [ $topic_retries -gt 0 ]; do
		let topic_retries=topic_retries-1
		result=$(__do_curl "$curlString")
		if [ $? -eq 0 ]; then
			topic_retries=0
			echo -e " Creating read topic: $1 $GREEN OK $EGREEN"
		fi
		if [ $? -ne 0 ]; then
			if [ $topic_retries -eq 0 ]; then
				echo -e " Creating read topic: $1 $RED Failed $ERED"
				((RES_CONF_FAIL++))
				return 1
			else
				sleep 1
			fi
		fi
	done
	return 0
}

# Do a pipeclean of a topic - to overcome dmaap mr bug...
# args: <topic> <post-url> <read-url>
__dmaap_pipeclean() {
	pipeclean_retries=50
	echo -ne " Doing dmaap-mr pipe cleaning on topic: $1"$SAMELINE
	while [ $pipeclean_retries -gt 0 ]; do
		echo "{\"pipeclean-$1\":$pipeclean_retries}" > ./tmp/pipeclean.json
		let pipeclean_retries=pipeclean_retries-1
		curlString="$MR_DMAAP_PATH$2 -X POST  -H Content-Type:application/json -d@./tmp/pipeclean.json"
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

# Start the Message Router stub interface in the simulator group
# args: -
# (Function for test scripts)
start_mr() {

	echo -e $BOLD"Starting $MR_DMAAP_DISPLAY_NAME and/or $MR_STUB_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

        # Table of possible combinations of included mr and included/prestarted dmaap-mr
		# mr can never be prestarted
		# mr can be used stand alone
		# if dmaapmr is included/prestarted, then mr is needed as well as frontend

        # Inverted logic - 0 mean true, 1 means false
		# mr prestarted      0 1 0 1 0 1 0 1 0 1 0 1 0 1 0 1
		# mr included        0 0 1 1 0 0 1 1 0 0 1 1 0 0 1 1
		# dmaap prestarted   0 0 0 0 1 1 1 1 0 0 0 0 1 1 1 1
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
				echo -e $RED"Both MR and DAAMPMR  - or - only MR - need to be included and/or prestarted"
				exit
		fi

		if [ $retcode_prestarted_dmaapmr -eq 0 ]; then
			echo -e " Using existing $MR_DMAAP_APP_NAME deployment and service"
			__kube_scale deployment $MR_DMAAP_APP_NAME $KUBE_ONAP_NAMESPACE 1
		fi

		if [ $retcode_included_dmaapmr -eq 0 ]; then
			#export MR_DMAAP_APP_NAME
			export MR_DMAAP_KUBE_APP_NAME=message-router
			MR_DMAAP_APP_NAME=$MR_DMAAP_KUBE_APP_NAME
			export KUBE_ONAP_NAMESPACE
			export MR_EXTERNAL_PORT
			export MR_INTERNAL_PORT
			export MR_EXTERNAL_SECURE_PORT
			export MR_INTERNAL_SECURE_PORT
			export ONAP_DMAAPMR_IMAGE

			export MR_KAFKA_BWDS_NAME=akfak-bwds
			export KUBE_ONAP_NAMESPACE

			export MR_ZOOKEEPER_APP_NAME
			export ONAP_ZOOKEEPER_IMAGE

			#Check if onap namespace exists, if not create it
			__kube_create_namespace $KUBE_ONAP_NAMESPACE

			# TODO - Fix domain name substitution in the prop file
			# Create config maps - dmaapmr app
			configfile=$PWD/tmp/MsgRtrApi.properties
			cp $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"mnt/mr/KUBE-MsgRtrApi.properties $configfile
			output_yaml=$PWD/tmp/dmaapmr_msgrtrapi_cfc.yaml
			__kube_create_configmap dmaapmr-msgrtrapi.properties $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			configfile=$PWD/tmp/logback.xml
			cp $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"mnt/mr/logback.xml $configfile
			output_yaml=$PWD/tmp/dmaapmr_logback_cfc.yaml
			__kube_create_configmap dmaapmr-logback.xml $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			configfile=$PWD/tmp/cadi.properties
			cp $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"mnt/mr/cadi.properties $configfile
			output_yaml=$PWD/tmp/dmaapmr_cadi_cfc.yaml
			__kube_create_configmap dmaapmr-cadi.properties $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			# Create config maps - kafka app
			configfile=$PWD/tmp/zk_client_jaas.conf
			cp $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"mnt/kafka/zk_client_jaas.conf $configfile
			output_yaml=$PWD/tmp/dmaapmr_zk_client_cfc.yaml
			__kube_create_configmap dmaapmr-zk-client-jaas.conf $KUBE_ONAP_NAMESPACE autotest DMAAPMR $configfile $output_yaml

			# Create config maps - zookeeper app
			configfile=$PWD/tmp/zk_server_jaas.conf
			cp $SIM_GROUP"/"$MR_DMAAP_COMPOSE_DIR"/"mnt/zk/zk_server_jaas.conf $configfile
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


			echo " Retrieving host and ports for service..."
			MR_DMAAP_HOST_NAME=$(__kube_get_service_host $MR_DMAAP_APP_NAME $KUBE_ONAP_NAMESPACE)

			MR_EXT_PORT=$(__kube_get_service_port $MR_DMAAP_APP_NAME $KUBE_ONAP_NAMESPACE "http")
			MR_EXT_SECURE_PORT=$(__kube_get_service_port $MR_DMAAP_APP_NAME $KUBE_ONAP_NAMESPACE "https")

			echo " Host IP, http port, https port: $MR_DMAAP_APP_NAME $MR_EXT_PORT $MR_EXT_SECURE_PORT"
			MR_SERVICE_PATH=""
			if [ $MR_HTTPX == "http" ]; then
				MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_HOST_NAME":"$MR_EXT_PORT
				MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXT_PORT
			else
				MR_DMAAP_PATH=$MR_HTTPX"://"$MR_DMAAP_HOST_NAME":"$MR_EXT_SECURE_PORT
				MR_SERVICE_PATH=$MR_HTTPX"://"$MR_DMAAP_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXT_SECURE_PORT
			fi

				__check_service_start $MR_DMAAP_APP_NAME $MR_DMAAP_PATH$MR_DMAAP_ALIVE_URL

		fi

		if [ $retcode_included_mr -eq 0 ]; then
			#exporting needed var for deployment
			export MR_STUB_APP_NAME
			export KUBE_ONAP_NAMESPACE
			export MRSTUB_IMAGE
			export MR_INTERNAL_PORT
			export MR_INTERNAL_SECURE_PORT
			export MR_EXTERNAL_PORT
			export MR_EXTERNAL_SECURE_PORT

			if [ $retcode_prestarted_dmaapmr -eq 0 ] || [ $retcode_included_dmaapmr -eq 0 ]; then  # Set topics for dmaap
				export TOPIC_READ="http://$MR_DMAAP_APP_NAME.$KUBE_ONAP_NAMESPACE:$MR_INTERNAL_PORT/events/$MR_READ_TOPIC"
				export TOPIC_WRITE="http://$MR_DMAAP_APP_NAME.$KUBE_ONAP_NAMESPACE:$MR_INTERNAL_PORT/events/$MR_WRITE_TOPIC/users/mr-stub?timeout=15000&limit=100"
			else
				export TOPIC_READ=""
				export TOPIC_WRITE=""
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


		echo " Retrieving host and ports for service..."
		MR_STUB_HOST_NAME=$(__kube_get_service_host $MR_STUB_APP_NAME $KUBE_ONAP_NAMESPACE)

		MR_EXT_PORT=$(__kube_get_service_port $MR_STUB_APP_NAME $KUBE_ONAP_NAMESPACE "http")
		MR_EXT_SECURE_PORT=$(__kube_get_service_port $MR_STUB_APP_NAME $KUBE_ONAP_NAMESPACE "https")

		echo " Host IP, http port, https port: $MR_STUB_APP_NAME $MR_EXT_PORT $MR_EXT_SECURE_PORT"
		if [ $MR_HTTPX == "http" ]; then
			MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_HOST_NAME":"$MR_EXT_PORT
			if [ -z "$MR_SERVICE_PATH" ]; then
				MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXT_PORT
			fi
		else
			MR_STUB_PATH=$MR_HTTPX"://"$MR_STUB_HOST_NAME":"$MR_EXT_SECURE_PORT
			if [ -z "$MR_SERVICE_PATH" ]; then
				MR_SERVICE_PATH=$MR_HTTPX"://"$MR_STUB_APP_NAME"."$KUBE_ONAP_NAMESPACE":"$MR_EXT_SECURE_PORT
			fi
		fi
		MR_ADAPTER_HTTP="http://"$MR_STUB_HOST_NAME":"$MR_EXT_PORT
		MR_ADAPTER_HTTPS="https://"$MR_STUB_HOST_NAME":"$MR_EXT_SECURE_PORT

		__check_service_start $MR_STUB_APP_NAME $MR_STUB_PATH$MR_STUB_ALIVE_URL

		echo -ne " Service $MR_STUB_APP_NAME - reset  "$SAMELINE
		result=$(__do_curl $MR_STUB_APP_NAME $MR_STUB_PATH/reset)
		if [ $? -ne 0 ]; then
			echo -e " Service $MR_STUB_APP_NAME - reset  $RED Failed $ERED - will continue"
		else
			echo -e " Service $MR_STUB_APP_NAME - reset  $GREEN OK $EGREEN"
		fi


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
		if [ $retcode_dmaapmr -eq 0 ]; then  # Set topics for dmaap
			export TOPIC_READ="http://$MR_DMAAP_APP_NAME:$MR_INTERNAL_PORT/events/$MR_READ_TOPIC"
			export TOPIC_WRITE="http://$MR_DMAAP_APP_NAME:$MR_INTERNAL_PORT/events/$MR_WRITE_TOPIC/users/mr-stub?timeout=15000&limit=100"
		fi

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

		if [ $retcode_dmaapmr -eq 0 ]; then
			__start_container $MR_DMAAP_COMPOSE_DIR NODOCKERARGS 1 $MR_DMAAP_APP_NAME

			__check_service_start $MR_DMAAP_APP_NAME $MR_DMAAP_PATH$MR_DMAAP_ALIVE_URL


			__create_topic $MR_READ_TOPIC "Topic for reading policy messages"

			__create_topic $MR_WRITE_TOPIC "Topic for writing policy messages"

			__dmaap_pipeclean $MR_READ_TOPIC "/events/A1-POLICY-AGENT-READ" "/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=1000&limit=100"

			__dmaap_pipeclean $MR_WRITE_TOPIC "/events/A1-POLICY-AGENT-WRITE" "/events/A1-POLICY-AGENT-WRITE/users/mr-stub?timeout=1000&limit=100"

			echo " Current topics:"
			curlString="$MR_DMAAP_PATH/topics"
			result=$(__do_curl "$curlString")
			echo $result | indent2
		fi

		export DOCKER_SIM_NWNAME
		export MR_STUB_APP_NAME
		export MRSTUB_IMAGE
		export MR_INTERNAL_PORT
		export MR_INTERNAL_SECURE_PORT
		export MR_STUB_LOCALHOST_PORT
		export MR_STUB_LOCALHOST_SECURE_PORT
		export MR_STUB_CERT_MOUNT_DIR

		if [ $retcode_mr -eq 0 ]; then
			__start_container $MR_STUB_COMPOSE_DIR NODOCKERARGS 1 $MR_STUB_APP_NAME

			__check_service_start $MR_STUB_APP_NAME $MR_STUB_PATH$MR_STUB_ALIVE_URL
		fi

	fi
	echo ""
	return 0
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