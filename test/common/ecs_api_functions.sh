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

# This is a script that contains container/service management functions and test functions for ECS

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__ECS_imagesetup() {
	__check_and_create_image_var ECS "ECS_IMAGE" "ECS_IMAGE_BASE" "ECS_IMAGE_TAG" $1 "$ECS_DISPLAY_NAME"
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__ECS_imagepull() {
	__check_and_pull_image $1 "$ECS_DISPLAY_NAME" $ECS_APP_NAME ECS_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__ECS_imagebuild() {
	echo -e $RED" Image for app ECS shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__ECS_image_data() {
	echo -e "$ECS_DISPLAY_NAME\t$(docker images --format $1 $ECS_IMAGE)" >>   $2
	if [ ! -z "$ECS_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $ECS_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__ECS_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ECS
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for prestarted apps not managed by the test script.
__ECS_kube_scale_zero_and_wait() {
	__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app nonrtric-enrichmentservice
}

# Delete all kube resouces for the app
# This function is called for apps managed by the test script.
__ECS_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ECS
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prexix>
__ECS_store_docker_logs() {
	docker logs $ECS_APP_NAME > $1$2_ecs.log 2>&1
}
#######################################################


## Access to ECS
# Host name may be changed if app started by kube
# Direct access
ECS_HTTPX="http"
ECS_HOST_NAME=$LOCALHOST_NAME
ECS_PATH=$ECS_HTTPX"://"$ECS_HOST_NAME":"$ECS_EXTERNAL_PORT

# ECS_ADAPTER used for switch between REST and DMAAP (only REST supported currently)
ECS_ADAPTER_TYPE="REST"
ECS_ADAPTER=$ECS_PATH

# Make curl retries towards ECS for http response codes set in this env var, space separated list of codes
ECS_RETRY_CODES=""

#Save first worker node the pod is started on
__ECS_WORKER_NODE=""

###########################
### ECS functions
###########################

# All calls to ECS will be directed to the ECS REST interface from now on
# args: -
# (Function for test scripts)
use_ecs_rest_http() {
	echo -e $BOLD"ECS protocol setting"$EBOLD
	echo -e " Using $BOLD http $EBOLD and $BOLD REST $EBOLD towards ECS"
	ECS_HTTPX="http"
	ECS_PATH=$ECS_HTTPX"://"$ECS_HOST_NAME":"$ECS_EXTERNAL_PORT

	ECS_ADAPTER_TYPE="REST"
	ECS_ADAPTER=$ECS_PATH
	echo ""
}

# All calls to ECS will be directed to the ECS REST interface from now on
# args: -
# (Function for test scripts)
use_ecs_rest_https() {
	echo -e $BOLD"ECS protocol setting"$EBOLD
	echo -e " Using $BOLD https $EBOLD and $BOLD REST $EBOLD towards ECS"
	ECS_HTTPX="https"
	ECS_PATH=$ECS_HTTPX"://"$ECS_HOST_NAME":"$ECS_EXTERNAL_SECURE_PORT

	ECS_ADAPTER_TYPE="REST"
	ECS_ADAPTER=$ECS_PATH
	echo ""
}

# All calls to ECS will be directed to the ECS dmaap interface over http from now on
# args: -
# (Function for test scripts)
use_ecs_dmaap_http() {
	echo -e $BOLD"ECS dmaap protocol setting"$EBOLD
	echo -e $RED" - NOT SUPPORTED - "$ERED
	echo -e " Using $BOLD http $EBOLD and $BOLD DMAAP $EBOLD towards ECS"
	ECS_ADAPTER_TYPE="MR-HTTP"
	echo ""
}

# All calls to ECS will be directed to the ECS dmaap interface over https from now on
# args: -
# (Function for test scripts)
use_ecs_dmaap_https() {
	echo -e $BOLD"RICSIM protocol setting"$EBOLD
	echo -e $RED" - NOT SUPPORTED - "$ERED
	echo -e " Using $BOLD https $EBOLD and $BOLD REST $EBOLD towards ECS"
	ECS_ADAPTER_TYPE="MR-HTTPS"
	echo ""
}

# Start the ECS
# args: PROXY|NOPROXY <config-file>
# (Function for test scripts)
start_ecs() {

	echo -e $BOLD"Starting $ECS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "ECS"
		retcode_i=$?

		# Check if app shall only be used by the testscipt
		__check_prestarted_image "ECS"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $ECS_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $ECS_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $ECS_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $ECS_APP_NAME will not be started"$ERED
			exit
		fi


		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $ECS_APP_NAME deployment and service"
			echo " Setting ECS replicas=1"
			__kube_scale deployment $ECS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		# Check if app shall be fully managed by the test script
		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $ECS_APP_NAME app and expose service"

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			export ECS_APP_NAME
			export KUBE_NONRTRIC_NAMESPACE
			export ECS_IMAGE
			export ECS_INTERNAL_PORT
			export ECS_INTERNAL_SECURE_PORT
			export ECS_EXTERNAL_PORT
			export ECS_EXTERNAL_SECURE_PORT
			export ECS_CONFIG_MOUNT_PATH
			export ECS_CONFIG_CONFIGMAP_NAME=$ECS_APP_NAME"-config"
			export ECS_DATA_CONFIGMAP_NAME=$ECS_APP_NAME"-data"
			export ECS_CONTAINER_MNT_DIR

			export ECS_DATA_PV_NAME=$ECS_APP_NAME"-pv"
			export ECS_DATA_PVC_NAME=$ECS_APP_NAME"-pvc"
			#Create a unique path for the pv each time to prevent a previous volume to be reused
			export ECS_PV_PATH="ecsdata-"$(date +%s)

			if [ $1 == "PROXY" ]; then
				ECS_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT  #Set if proxy is started
				ECS_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
				if [ $ECS_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$ECS_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
					echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
				else
					echo " Configured with http proxy"
				fi
			else
				ECS_HTTP_PROXY_CONFIG_PORT=0
				ECS_HTTP_PROXY_CONFIG_HOST_NAME=""
				echo " Configured without http proxy"
			fi
			export ECS_HTTP_PROXY_CONFIG_PORT
			export ECS_HTTP_PROXY_CONFIG_HOST_NAME

			# Create config map for config
			datafile=$PWD/tmp/$ECS_CONFIG_FILE
			cp $2 $datafile
			output_yaml=$PWD/tmp/ecs_cfc.yaml
			__kube_create_configmap $ECS_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest ECS $datafile $output_yaml

			# Create pv
			input_yaml=$SIM_GROUP"/"$ECS_COMPOSE_DIR"/"pv.yaml
			output_yaml=$PWD/tmp/ecs_pv.yaml
			__kube_create_instance pv $ECS_APP_NAME $input_yaml $output_yaml

			# Create pvc
			input_yaml=$SIM_GROUP"/"$ECS_COMPOSE_DIR"/"pvc.yaml
			output_yaml=$PWD/tmp/ecs_pvc.yaml
			__kube_create_instance pvc $ECS_APP_NAME $input_yaml $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$ECS_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/ecs_svc.yaml
			__kube_create_instance service $ECS_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$ECS_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/ecs_app.yaml
			__kube_create_instance app $ECS_APP_NAME $input_yaml $output_yaml
		fi

		# Tie the ECS to a worker node so that ECS will always be scheduled to the same worker node if the ECS pod is restarted
		# A PVC of type hostPath is mounted to ECS, for persistent storage, so the ECS must always be on the node which mounted the volume

		# Keep the initial worker node in case the pod need to be "restarted" - must be made to the same node due to a volume mounted on the host
		__ECS_WORKER_NODE=$(kubectl get pod -l "autotest=ECS" -n $KUBE_NONRTRIC_NAMESPACE -o jsonpath='{.items[*].spec.nodeName}')
		if [ -z "$__ECS_WORKER_NODE" ]; then
			echo -e $YELLOW" Cannot find worker node for pod for $ECS_APP_NAME, persistency may not work"$EYELLOW
		fi

		echo " Retrieving host and ports for service..."
		ECS_HOST_NAME=$(__kube_get_service_host $ECS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
		ECS_EXTERNAL_PORT=$(__kube_get_service_port $ECS_APP_NAME $KUBE_NONRTRIC_NAMESPACE "http")
		ECS_EXTERNAL_SECURE_PORT=$(__kube_get_service_port $ECS_APP_NAME $KUBE_NONRTRIC_NAMESPACE "https")

		echo " Host IP, http port, https port: $ECS_HOST_NAME $ECS_EXTERNAL_PORT $ECS_EXTERNAL_SECURE_PORT"

		if [ $ECS_HTTPX == "http" ]; then
			ECS_PATH=$ECS_HTTPX"://"$ECS_HOST_NAME":"$ECS_EXTERNAL_PORT
		else
			ECS_PATH=$ECS_HTTPX"://"$ECS_HOST_NAME":"$ECS_EXTERNAL_SECURE_PORT
		fi

		__check_service_start $ECS_APP_NAME $ECS_PATH$ECS_ALIVE_URL

		if [ $ECS_ADAPTER_TYPE == "REST" ]; then
			ECS_ADAPTER=$ECS_PATH
		fi
	else
		__check_included_image 'ECS'
		if [ $? -eq 1 ]; then
			echo -e $RED"The ECS app is not included in this test script"$ERED
			echo -e $RED"ECS will not be started"$ERED
			exit 1
		fi

		curdir=$PWD
		cd $SIM_GROUP
		cd ecs
		cd $ECS_HOST_MNT_DIR
		#cd ..
		if [ -d db ]; then
			if [ "$(ls -A $DIR)" ]; then
				echo -e $BOLD" Cleaning files in mounted dir: $PWD/db"$EBOLD
				rm -rf db/*  &> /dev/null
				if [ $? -ne 0 ]; then
					echo -e $RED" Cannot remove database files in: $PWD"$ERED
					exit 1
				fi
			fi
		else
			echo " No files in mounted dir or dir does not exists"
		fi
		cd $curdir

		export ECS_APP_NAME
		export ECS_APP_NAME_ALIAS
		export ECS_HOST_MNT_DIR
		export ECS_CONTAINER_MNT_DIR
		export ECS_CONFIG_MOUNT_PATH
		export ECS_CONFIG_FILE
		export ECS_INTERNAL_PORT
		export ECS_EXTERNAL_PORT
		export ECS_INTERNAL_SECURE_PORT
		export ECS_EXTERNAL_SECURE_PORT
		export DOCKER_SIM_NWNAME
		export ECS_DISPLAY_NAME

		if [ $1 == "PROXY" ]; then
			ECS_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT  #Set if proxy is started
			ECS_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
			if [ $ECS_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$ECS_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
				echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
			else
				echo " Configured with http proxy"
			fi
		else
			ECS_HTTP_PROXY_CONFIG_PORT=0
			ECS_HTTP_PROXY_CONFIG_HOST_NAME=""
			echo " Configured without http proxy"
		fi
		export ECS_HTTP_PROXY_CONFIG_PORT
		export ECS_HTTP_PROXY_CONFIG_HOST_NAME

		dest_file=$SIM_GROUP/$ECS_COMPOSE_DIR/$ECS_HOST_MNT_DIR/$ECS_CONFIG_FILE

		envsubst < $2 > $dest_file

		__start_container $ECS_COMPOSE_DIR "" NODOCKERARGS 1 $ECS_APP_NAME

		__check_service_start $ECS_APP_NAME $ECS_PATH$ECS_ALIVE_URL
	fi
	echo ""
	return 0
}

# Stop the ecs
# args: -
# args: -
# (Function for test scripts)
stop_ecs() {
	echo -e $BOLD"Stopping $ECS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ECS
		echo "  Deleting the replica set - a new will be started when the app is started"
		tmp=$(kubectl delete rs -n $KUBE_NONRTRIC_NAMESPACE -l "autotest=ECS")
		if [ $? -ne 0 ]; then
			echo -e $RED" Could not delete replica set "$RED
			((RES_CONF_FAIL++))
			return 1
		fi
	else
		docker stop $ECS_APP_NAME &> ./tmp/.dockererr
		if [ $? -ne 0 ]; then
			__print_err "Could not stop $ECS_APP_NAME" $@
			cat ./tmp/.dockererr
			((RES_CONF_FAIL++))
			return 1
		fi
	fi
	echo -e $BOLD$GREEN"Stopped"$EGREEN$EBOLD
	echo ""
	return 0
}

# Start a previously stopped ecs
# args: -
# (Function for test scripts)
start_stopped_ecs() {
	echo -e $BOLD"Starting (the previously stopped) $ECS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Tie the PMS to the same worker node it was initially started on
		# A PVC of type hostPath is mounted to PMS, for persistent storage, so the PMS must always be on the node which mounted the volume
		if [ -z "$__ECS_WORKER_NODE" ]; then
			echo -e $RED" No initial worker node found for pod "$RED
			((RES_CONF_FAIL++))
			return 1
		else
			echo -e $BOLD" Setting nodeSelector kubernetes.io/hostname=$__ECS_WORKER_NODE to deployment for $ECS_APP_NAME. Pod will always run on this worker node: $__PA_WORKER_NODE"$BOLD
			echo -e $BOLD" The mounted volume is mounted as hostPath and only available on that worker node."$BOLD
			tmp=$(kubectl patch deployment $ECS_APP_NAME -n $KUBE_NONRTRIC_NAMESPACE --patch '{"spec": {"template": {"spec": {"nodeSelector": {"kubernetes.io/hostname": "'$__ECS_WORKER_NODE'"}}}}}')
			if [ $? -ne 0 ]; then
				echo -e $YELLOW" Cannot set nodeSelector to deployment for $ECS_APP_NAME, persistency may not work"$EYELLOW
			fi
			__kube_scale deployment $ECS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

	else
		docker start $ECS_APP_NAME &> ./tmp/.dockererr
		if [ $? -ne 0 ]; then
			__print_err "Could not start (the stopped) $ECS_APP_NAME" $@
			cat ./tmp/.dockererr
			((RES_CONF_FAIL++))
			return 1
		fi
	fi
	__check_service_start $ECS_APP_NAME $ECS_PATH$ECS_ALIVE_URL
	if [ $? -ne 0 ]; then
		return 1
	fi
	echo ""
	return 0
}

# Turn on debug level tracing in ECS
# args: -
# (Function for test scripts)
set_ecs_debug() {
	echo -e $BOLD"Setting ecs debug logging"$EBOLD
	curlString="$ECS_PATH$ECS_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "Could not set debug mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Turn on trace level tracing in ECS
# args: -
# (Function for test scripts)
set_ecs_trace() {
	echo -e $BOLD"Setting ecs trace logging"$EBOLD
	curlString="$ECS_PATH/actuator/loggers/org.oransc.enrichment -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "Could not set trace mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Perform curl retries when making direct call to ECS for the specified http response codes
# Speace separated list of http response codes
# args: [<response-code>]*
use_ecs_retries() {
	echo -e $BOLD"Do curl retries to the ECS REST inteface for these response codes:$@"$EBOLD
	ECS_RETRY_CODES=$@
	echo ""
	return 0
}

# Check the ecs logs for WARNINGs and ERRORs
# args: -
# (Function for test scripts)
check_ecs_logs() {
	__check_container_logs "ECS" $ECS_APP_NAME $ECS_LOGPATH WARN ERR
}


# Tests if a variable value in the ECS is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
ecs_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test ECS "$ECS_PATH/" $1 "=" $2 $3
	else
		__print_err "Wrong args to ecs_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}


##########################################
######### A1-E Enrichment  API ##########
##########################################
#Function prefix: ecs_api_a1

# API Test function: GET /A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs
# args: <response-code> <type-id>  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]
# args (flat uri structure): <response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]
# (Function for test scripts)
ecs_api_a1_get_job_ids() {
	__log_test_start $@

	if [ -z "$FLAT_A1_EI" ]; then
		# Valid number of parameters 4,5,6 etc
    	if [ $# -lt 3 ]; then
			__print_err "<response-code> <type-id>  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]" $@
			return 1
		fi
	else
		echo -e $YELLOW"INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		# Valid number of parameters 4,5,6 etc
    	if [ $# -lt 3 ]; then
			__print_err "<response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]" $@
			return 1
		fi
	fi
	search=""
	if [ $3 != "NOWNER" ]; then
		search="?owner="$3
	fi

	if [  -z "$FLAT_A1_EI" ]; then
		query="/A1-EI/v1/eitypes/$2/eijobs$search"
	else
		if [ $2 != "NOTYPE" ]; then
			if [ -z "$search" ]; then
				search="?eiTypeId="$2
			else
				search=$search"&eiTypeId="$2
			fi
		fi
		query="/A1-EI/v1/eijobs$search"
	fi
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:4} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "EMPTY" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done

		targetJson=$targetJson"]"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}
# args: <response-code> <type-id> [<schema-file>]
# (Function for test scripts)
ecs_api_a1_get_type() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
		__print_err "<response-code> <type-id> [<schema-file>]" $@
		return 1
	fi

	query="/A1-EI/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 3 ]; then
		body=${res:0:${#res}-3}
		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Schema file "$3", does not exist"
			return 1
		fi
		if [ -z "$FLAT_A1_EI" ]; then
			targetJson="{\"eiJobParametersSchema\":$schema}"
		else
			targetJson=$schema
		fi
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /A1-EI/v1/eitypes
# args: <response-code> [ (EMPTY | [<type-id>]+) ]
# (Function for test scripts)
ecs_api_a1_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ (EMPTY | [<type-id>]+) ]" $@
		return 1
	fi

	query="/A1-EI/v1/eitypes"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $2 != "EMPTY" ]; then
			for pid in ${@:2} ; do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"$pid\""
			done
		fi
		targetJson=$targetJson"]"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}​/status
# args: <response-code> <type-id> <job-id> [<status>]
# args (flat uri structure): <response-code> <job-id> [<status> [<timeout>]]
# (Function for test scripts)
ecs_api_a1_get_job_status() {
	__log_test_start $@

	if [ -z "$FLAT_A1_EI" ]; then
		if [ $# -ne 3 ] && [ $# -ne 4 ]; then
			__print_err "<response-code> <type-id> <job-id> [<status>]" $@
			return 1
		fi

		query="/A1-EI/v1/eitypes/$2/eijobs/$3/status"

		res="$(__do_curl_to_api ECS GET $query)"
		status=${res:${#res}-3}

		if [ $status -ne $1 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi
		if [ $# -eq 4 ]; then
			body=${res:0:${#res}-3}
			targetJson="{\"operationalState\": \"$4\"}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		echo -e $YELLOW"INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -lt 2 ] && [ $# -gt 4 ]; then
			__print_err "<response-code> <job-id> [<status> [<timeout>]]" $@
			return 1
		fi

		query="/A1-EI/v1/eijobs/$2/status"

		start=$SECONDS
		for (( ; ; )); do
			res="$(__do_curl_to_api ECS GET $query)"
			status=${res:${#res}-3}

			if [ $# -eq 4 ]; then
				duration=$((SECONDS-start))
				echo -ne " Response=${status} after ${duration} seconds, waiting for ${3} ${SAMELINE}"
				if [ $duration -gt $4 ]; then
					echo ""
					duration=-1  #Last iteration
				fi
			else
				duration=-1 #single test, no wait
			fi

			if [ $status -ne $1 ]; then
				if [ $duration -eq -1 ]; then
					__log_test_fail_status_code $1 $status
					return 1
				fi
			fi
			if [ $# -ge 3 ] && [ $status -eq $1 ]; then
				body=${res:0:${#res}-3}
				targetJson="{\"eiJobStatus\": \"$3\"}"
				echo " TARGET JSON: $targetJson" >> $HTTPLOG
				res=$(python3 ../common/compare_json.py "$targetJson" "$body")

				if [ $res -ne 0 ]; then
					if [ $duration -eq -1 ]; then
						__log_test_fail_body
						return 1
					fi
				else
					duration=-1  #Goto pass
				fi
			fi
			if [ $duration -eq -1 ]; then
				if [ $# -eq 4 ]; then
					echo ""
				fi
				__log_test_pass
				return 0
			else
				sleep 1
			fi
		done
	fi

	__log_test_pass
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <type-id> <job-id> [<target-url> <owner-id> <template-job-file>]
# args (flat uri structure): <response-code> <job-id> [<type-id> <target-url> <owner-id> <template-job-file>]
# (Function for test scripts)
ecs_api_a1_get_job() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -ne 3 ] && [ $# -ne 6 ]; then
			__print_err "<response-code> <type-id> <job-id> [<target-url> <owner-id> <template-job-file>]" $@
			return 1
		fi
		query="/A1-EI/v1/eitypes/$2/eijobs/$3"
	else
		echo -e $YELLOW"INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -ne 2 ] && [ $# -ne 7 ]; then
			__print_err "<response-code> <job-id> [<type-id> <target-url> <owner-id> <notification-url> <template-job-file>]" $@
			return 1
		fi
		query="/A1-EI/v1/eijobs/$2"
	fi
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -eq 6 ]; then
			body=${res:0:${#res}-3}

			if [ -f $6 ]; then
				jobfile=$(cat $6)
				jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
			else
				_log_test_fail_general "Job template file "$6", does not exist"
				return 1
			fi
			targetJson="{\"targetUri\": \"$4\",\"jobOwner\": \"$5\",\"jobParameters\": $jobfile}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				__log_test_fail_body
				return 1
			fi
		fi
	else
		if [ $# -eq 7 ]; then
			body=${res:0:${#res}-3}

			if [ -f $7 ]; then
				jobfile=$(cat $7)
				jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
			else
				_log_test_fail_general "Job template file "$6", does not exist"
				return 1
			fi
			targetJson="{\"eiTypeId\": \"$3\", \"jobResultUri\": \"$4\",\"jobOwner\": \"$5\",\"jobStatusNotificationUri\": \"$6\",\"jobDefinition\": $jobfile}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
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

# API Test function: DELETE ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <type-id> <job-id>
# args (flat uri structure): <response-code> <job-id>
# (Function for test scripts)
ecs_api_a1_delete_job() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -ne 3 ]; then
			__print_err "<response-code> <type-id> <job-id>" $@
			return 1
		fi

		query="/A1-EI/v1/eitypes/$2/eijobs/$3"
	else
		echo -e $YELLOW"INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -ne 2 ]; then
			__print_err "<response-code> <job-id>" $@
			return 1
		fi
		query="/A1-EI/v1/eijobs/$2"
	fi
    res="$(__do_curl_to_api ECS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <type-id> <job-id> <target-url> <owner-id> <template-job-file>
# args (flat uri structure): <response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file>
# (Function for test scripts)
ecs_api_a1_put_job() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		if [ $# -lt 6 ]; then
			__print_err "<response-code> <type-id> <job-id> <target-url> <owner-id> <template-job-file>" $@
			return 1
		fi
		if [ -f $6 ]; then
			jobfile=$(cat $6)
			jobfile=$(echo "$jobfile" | sed "s/XXXX/$3/g")
		else
			_log_test_fail_general "Job template file "$6", does not exist"
			return 1
		fi

		inputJson="{\"targetUri\": \"$4\",\"jobOwner\": \"$5\",\"jobParameters\": $jobfile}"
		file="./tmp/.p.json"
		echo "$inputJson" > $file

		query="/A1-EI/v1/eitypes/$2/eijobs/$3"
	else
		echo -e $YELLOW"INTERFACE - FLAT URI STRUCTURE"$EYELLOW
		if [ $# -lt 7 ]; then
			__print_err "<response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file>" $@
			return 1
		fi
		if [ -f $7 ]; then
			jobfile=$(cat $7)
			jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
		else
			_log_test_fail_general "Job template file "$7", does not exist"
			return 1
		fi

		inputJson="{\"eiTypeId\": \"$3\", \"jobResultUri\": \"$4\",\"jobOwner\": \"$5\",\"jobStatusNotificationUri\": \"$6\",\"jobDefinition\": $jobfile}"
		file="./tmp/.p.json"
		echo "$inputJson" > $file

		query="/A1-EI/v1/eijobs/$2"
	fi

    res="$(__do_curl_to_api ECS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}


##########################################
####   Enrichment Data Producer API   ####
##########################################
# Function prefix: ecs_api_edp

# API Test function: GET /ei-producer/v1/eitypes
# args: <response-code> [ EMPTY | <type-id>+]
# (Function for test scripts)
ecs_api_edp_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ EMPTY | <type-id>+]" $@
		return 1
	fi

	query="/ei-producer/v1/eitypes"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $2 != "EMPTY" ]; then
			for pid in ${@:2} ; do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"$pid\""
			done
		fi
		targetJson=$targetJson"]"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/status
# args: <response-code> <producer-id> [<status> [<timeout>]]
# (Function for test scripts)
ecs_api_edp_get_producer_status() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 4 ]; then
		__print_err "<response-code> <producer-id> [<status> [<timeout>]]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2/status"
	start=$SECONDS
	for (( ; ; )); do
		res="$(__do_curl_to_api ECS GET $query)"
		status=${res:${#res}-3}

		if [ $# -eq 4 ]; then
			duration=$((SECONDS-start))
			echo -ne " Response=${status} after ${duration} seconds, waiting for ${3} ${SAMELINE}"
			if [ $duration -gt $4 ]; then
				echo ""
				duration=-1  #Last iteration
			fi
		else
			duration=-1 #single test, no wait
		fi

		if [ $status -ne $1 ]; then
			if [ $duration -eq -1 ]; then
				__log_test_fail_status_code $1 $status
				return 1
			fi
		fi
		if [ $# -ge 3 ] && [ $status -eq $1 ]; then
			body=${res:0:${#res}-3}
			targetJson="{\"operational_state\": \"$3\"}"
			echo " TARGET JSON: $targetJson" >> $HTTPLOG
			res=$(python3 ../common/compare_json.py "$targetJson" "$body")

			if [ $res -ne 0 ]; then
				if [ $duration -eq -1 ]; then
					__log_test_fail_body
					return 1
				fi
			else
				duration=-1  #Goto pass
			fi
		fi
		if [ $duration -eq -1 ]; then
			if [ $# -eq 4 ]; then
				echo ""
			fi
			__log_test_pass
			return 0
		else
			sleep 1
		fi
	done
}


# API Test function: GET /ei-producer/v1/eiproducers
# args (v1_1): <response-code> [ EMPTY | <producer-id>+]
# (Function for test scripts)
ecs_api_edp_get_producer_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ EMPTY | <producer-id>+]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		targetJson="["

		for pid in ${@:2} ; do
			if [ "$targetJson" != "[" ]; then
				targetJson=$targetJson","
			fi
			if [ $pid != "EMPTY" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done

		targetJson=$targetJson"]"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers
# args (v1_2): <response-code> [ ( NOTYPE | <type-id> ) [ EMPTY | <producer-id>+] ]
# (Function for test scripts)
ecs_api_edp_get_producer_ids_2() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ ( NOTYPE | <type-id> ) [ EMPTY | <producer-id>+] ]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers"
	if [ $# -gt 1 ] && [ $2 != "NOTYPE" ]; then
		query=$query"?ei_type_id=$2"
	fi
    res="$(__do_curl_to_api ECS GET $query)"
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
			if [ $pid != "EMPTY" ]; then
				targetJson=$targetJson"\"$pid\""
			fi
		done

		targetJson=$targetJson"]"
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eitypes/{eiTypeId}
# args: (v1_1) <response-code> <type-id> [<job-schema-file> (EMPTY | [<producer-id>]+)]
# (Function for test scripts)
ecs_api_edp_get_type() {
	__log_test_start $@

	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -gt 3 ]; then
		paramError=0
	fi
    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <type-id> [<job-schema-file> 'EMPTY' | ([<producer-id>]+)]" $@
		return 1
	fi

	query="/ei-producer/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 3 ]; then
		body=${res:0:${#res}-3}

		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Job template file "$3", does not exist"
			return 1
		fi

		targetJson=""
		if [ $4 != "EMPTY" ]; then
			for pid in ${@:4} ; do
				if [ "$targetJson" != "" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"$pid\""
			done
		fi
		targetJson="{\"ei_job_data_schema\":$schema, \"ei_producer_ids\": [$targetJson]}"

		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi
	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eitypes/{eiTypeId}
# args: (v1_2) <response-code> <type-id> [<job-schema-file> ]
# (Function for test scripts)
ecs_api_edp_get_type_2() {
	__log_test_start $@

	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 3 ]; then
		paramError=0
	fi
    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <type-id> [<job-schema-file> ]" $@
		return 1
	fi

	query="/ei-producer/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -eq 3 ]; then
		body=${res:0:${#res}-3}

		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Job template file "$3", does not exist"
			return 1
		fi

		targetJson="{\"ei_job_data_schema\":$schema}"

		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi
	__log_test_pass
	return 0
}

# API Test function: PUT /ei-producer/v1/eitypes/{eiTypeId}
# args: (v1_2) <response-code> <type-id> <job-schema-file>
# (Function for test scripts)
ecs_api_edp_put_type_2() {
	__log_test_start $@

    if [ $# -ne 3 ]; then
		__print_err "<response-code> <type-id> <job-schema-file>" $@
		return 1
	fi

	if [ ! -f $3 ]; then
		__log_test_fail_general "Job schema file "$3", does not exist"
		return 1
	fi
	schema=$(cat $3)
	input_json="{\"ei_job_data_schema\":$schema}"
	file="./tmp/put_type.json"
	echo $input_json > $file

	query="/ei-producer/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE /ei-producer/v1/eitypes/{eiTypeId}
# args: (v1_2) <response-code> <type-id>
# (Function for test scripts)
ecs_api_edp_delete_type_2() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <type-id>" $@
		return 1
	fi

	query="/ei-producer/v1/eitypes/$2"
    res="$(__do_curl_to_api ECS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}
# args: (v1_1) <response-code> <producer-id> [<job-callback> <supervision-callback> (EMPTY | [<type-id> <schema-file>]+) ]
# (Function for test scripts)
ecs_api_edp_get_producer() {
	__log_test_start $@

	#Possible arg count: 2, 5 6, 8, 10 etc
	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 5 ] && [ "$5" == "EMPTY" ]; then
		paramError=0
	fi
	variablecount=$(($#-4))
	if [ $# -gt 5 ] && [ $(($variablecount%2)) -eq 0 ]; then
		paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <producer-id> [<job-callback> <supervision-callback> (NOID | [<type-id> <schema-file>]+) ]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 5 ]; then
			arr=(${@:5})
			for ((i=0; i<$(($#-5)); i=i+2)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				if [ -f ${arr[$i+1]} ]; then
					schema=$(cat ${arr[$i+1]})
				else
					_log_test_fail_general "Schema file "${arr[$i+1]}", does not exist"
					return 1
				fi

				targetJson=$targetJson"{\"ei_type_identity\":\"${arr[$i]}\",\"ei_job_data_schema\":$schema}"
			done
		fi
		targetJson=$targetJson"]"
		if [ $# -gt 4 ]; then
			targetJson="{\"supported_ei_types\":$targetJson,\"ei_job_callback_url\": \"$3\",\"ei_producer_supervision_callback_url\": \"$4\"}"
		fi
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}
# args (v1_2): <response-code> <producer-id> [<job-callback> <supervision-callback> (EMPTY | <type-id>+) ]
# (Function for test scripts)
ecs_api_edp_get_producer_2() {
	__log_test_start $@

	#Possible arg count: 2, 5, 6, 7, 8 etc
	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 5 ] && [ "$5" == "EMPTY" ]; then
		paramError=0
	fi
	if [ $# -ge 5 ]; then
		paramError=0
	fi

    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <producer-id> [<job-callback> <supervision-callback> (EMPTY | <type-id>+) ]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 4 ] && [ "$5" != "EMPTY" ]; then
			arr=(${@:5})
			for ((i=0; i<$(($#-4)); i=i+1)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\"${arr[$i]}\""
			done
		fi
		targetJson=$targetJson"]"
		if [ $# -gt 4 ]; then
			targetJson="{\"supported_ei_types\":$targetJson,\"ei_job_callback_url\": \"$3\",\"ei_producer_supervision_callback_url\": \"$4\"}"
		fi
		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE /ei-producer/v1/eiproducers/{eiProducerId}
# args: <response-code> <producer-id>
# (Function for test scripts)
ecs_api_edp_delete_producer() {
	__log_test_start $@

    if [ $# -lt 2 ]; then
		__print_err "<response-code> <producer-id>" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT /ei-producer/v1/eiproducers/{eiProducerId}
# args: (v1_1) <response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id> <schema-file>]+
# (Function for test scripts)
ecs_api_edp_put_producer() {
	__log_test_start $@

	#Valid number of parametrer 5,6,8,10,
	paramError=1
	if  [ $# -eq 5 ] && [ "$5" == "NOTYPE" ]; then
		paramError=0
	elif [ $# -gt 5 ] && [ $(($#%2)) -eq 0 ]; then
		paramError=0
	fi
	if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id> <schema-file>]+" $@
		return 1
	fi

	inputJson="["
	if [ $# -gt 5 ]; then
		arr=(${@:5})
		for ((i=0; i<$(($#-5)); i=i+2)); do
			if [ "$inputJson" != "[" ]; then
				inputJson=$inputJson","
			fi
			if [ -f ${arr[$i+1]} ]; then
				schema=$(cat ${arr[$i+1]})
			else
				_log_test_fail_general "Schema file "${arr[$i+1]}", does not exist"
				return 1
			fi
			inputJson=$inputJson"{\"ei_type_identity\":\"${arr[$i]}\",\"ei_job_data_schema\":$schema}"
		done
	fi
	inputJson="\"supported_ei_types\":"$inputJson"]"

	inputJson=$inputJson",\"ei_job_callback_url\": \"$3\",\"ei_producer_supervision_callback_url\": \"$4\""

	inputJson="{"$inputJson"}"

	file="./tmp/.p.json"
	echo "$inputJson" > $file
	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT /ei-producer/v1/eiproducers/{eiProducerId}
# args: (v1_2) <response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id>+]
# (Function for test scripts)
ecs_api_edp_put_producer_2() {
	__log_test_start $@

	#Valid number of parametrer 5,6,8,10,
	paramError=1
	if  [ $# -eq 5 ] && [ "$5" == "NOTYPE" ]; then
		paramError=0
	elif [ $# -ge 5 ]; then
		paramError=0
	fi
	if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id>+]" $@
		return 1
	fi

	inputJson="["
	if [ $# -gt 4 ] && [ "$5" != "NOTYPE" ]; then
		arr=(${@:5})
		for ((i=0; i<$(($#-4)); i=i+1)); do
			if [ "$inputJson" != "[" ]; then
				inputJson=$inputJson","
			fi
			inputJson=$inputJson"\""${arr[$i]}"\""
		done
	fi
	inputJson="\"supported_ei_types\":"$inputJson"]"

	inputJson=$inputJson",\"ei_job_callback_url\": \"$3\",\"ei_producer_supervision_callback_url\": \"$4\""

	inputJson="{"$inputJson"}"

	file="./tmp/.p.json"
	echo "$inputJson" > $file
	query="/ei-producer/v1/eiproducers/$2"
    res="$(__do_curl_to_api ECS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/eijobs
# args: (V1-1) <response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)
# (Function for test scripts)
ecs_api_edp_get_producer_jobs() {
	__log_test_start $@

	#Valid number of parameter 2,3,7,11
	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 3 ] && [ "$3" == "EMPTY" ]; then
		paramError=0
	fi
	variablecount=$(($#-2))
	if [ $# -gt 3 ] && [ $(($variablecount%5)) -eq 0 ]; then
		paramError=0
	fi
	if [ $paramError -eq 1 ]; then
		__print_err "<response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2/eijobs"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}
	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 3 ]; then
			arr=(${@:3})
			for ((i=0; i<$(($#-3)); i=i+5)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				if [ -f ${arr[$i+4]} ]; then
					jobfile=$(cat ${arr[$i+4]})
					jobfile=$(echo "$jobfile" | sed "s/XXXX/${arr[$i]}/g")
				else
					_log_test_fail_general "Job template file "${arr[$i+4]}", does not exist"
					return 1
				fi
				targetJson=$targetJson"{\"ei_job_identity\":\"${arr[$i]}\",\"ei_type_identity\":\"${arr[$i+1]}\",\"target_uri\":\"${arr[$i+2]}\",\"owner\":\"${arr[$i+3]}\",\"ei_job_data\":$jobfile}"
			done
		fi
		targetJson=$targetJson"]"

		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

# API Test function: GET /ei-producer/v1/eiproducers/{eiProducerId}/eijobs
# args: (V1-2) <response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)
# (Function for test scripts)
ecs_api_edp_get_producer_jobs_2() {
	__log_test_start $@

	#Valid number of parameter 2,3,7,11
	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 3 ] && [ "$3" == "EMPTY" ]; then
		paramError=0
	fi
	variablecount=$(($#-2))
	if [ $# -gt 3 ] && [ $(($variablecount%5)) -eq 0 ]; then
		paramError=0
	fi
	if [ $paramError -eq 1 ]; then
		__print_err "<response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers/$2/eijobs"
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}
	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $# -gt 3 ]; then
			arr=(${@:3})
			for ((i=0; i<$(($#-3)); i=i+5)); do
				if [ "$targetJson" != "[" ]; then
					targetJson=$targetJson","
				fi
				if [ -f ${arr[$i+4]} ]; then
					jobfile=$(cat ${arr[$i+4]})
					jobfile=$(echo "$jobfile" | sed "s/XXXX/${arr[$i]}/g")
				else
					_log_test_fail_general "Job template file "${arr[$i+4]}", does not exist"
					return 1
				fi
				targetJson=$targetJson"{\"ei_job_identity\":\"${arr[$i]}\",\"ei_type_identity\":\"${arr[$i+1]}\",\"target_uri\":\"${arr[$i+2]}\",\"owner\":\"${arr[$i+3]}\",\"ei_job_data\":$jobfile, \"last_updated\":\"????\"}"
			done
		fi
		targetJson=$targetJson"]"

		echo " TARGET JSON: $targetJson" >> $HTTPLOG
		res=$(python3 ../common/compare_json.py "$targetJson" "$body")

		if [ $res -ne 0 ]; then
			__log_test_fail_body
			return 1
		fi
	fi

	__log_test_pass
	return 0
}

##########################################
####          Service status          ####
##########################################
# Function prefix: ecs_api_service

# API Test function: GET ​/status
# args: <response-code>
# (Function for test scripts)
ecs_api_service_status() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [<producer-id>]*|NOID" $@
		return 1
	fi
	res="$(__do_curl_to_api ECS GET /status)"
    status=${res:${#res}-3}
	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	__log_test_pass
	return 0
}


##########################################
####          Reset jobs              ####
##########################################
# Function prefix: ecs_api_admin

# Admin to remove all jobs
# args: <response-code> [ <type> ]
# (Function for test scripts)

ecs_api_admin_reset() {
	__log_test_start $@

	if [  -z "$FLAT_A1_EI" ]; then
		query="/A1-EI/v1/eitypes/$2/eijobs"
	else
		query="/A1-EI/v1/eijobs"
	fi
    res="$(__do_curl_to_api ECS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne 200 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	#Remove brackets and response code
	body=${res:1:${#res}-4}
	list=$(echo ${body//,/ })
	list=$(echo ${list//[/})
	list=$(echo ${list//]/})
	list=$(echo ${list//\"/})
	list=$list" "
	for job in $list; do
		if [  -z "$FLAT_A1_EI" ]; then
			echo "Not supported for non-flat EI api"
		else
			query="/A1-EI/v1/eijobs/$job"
			res="$(__do_curl_to_api ECS DELETE $query)"
			status=${res:${#res}-3}
			if [ $status -ne 204 ]; then
				__log_test_fail_status_code $1 $status
				return 1
			fi
			echo " Deleted job: "$job
		fi
	done

	__log_test_pass
	return 0
}