#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions and test functions for ICS

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__ICS_imagesetup() {
	__check_and_create_image_var ICS "ICS_IMAGE" "ICS_IMAGE_BASE" "ICS_IMAGE_TAG" $1 "$ICS_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__ICS_imagepull() {
	__check_and_pull_image $1 "$ICS_DISPLAY_NAME" $ICS_APP_NAME ICS_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__ICS_imagebuild() {
	echo -e $RED" Image for app ICS shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__ICS_image_data() {
	echo -e "$ICS_DISPLAY_NAME\t$(docker images --format $1 $ICS_IMAGE)" >>   $2
	if [ ! -z "$ICS_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $ICS_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__ICS_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ICS
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__ICS_kube_scale_zero_and_wait() {
	__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app "$KUBE_NONRTRIC_NAMESPACE"-informationservice
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__ICS_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ICS
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__ICS_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=ICS" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 > $1$2_ics.log 2>&1
	else
		docker logs $ICS_APP_NAME > $1$2_ics.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__ICS_initial_setup() {
	use_ics_rest_http
	export ICS_SIDECAR_JWT_FILE=""
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__ICS_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "ICS $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
	else
		echo "ICS $ICS_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__ICS_test_requirements() {
	:
}

#######################################################


# Make curl retries towards ICS for http response codes set in this env var, space separated list of codes
ICS_RETRY_CODES=""

#Save first worker node the pod is started on
__ICS_WORKER_NODE=""

###########################
### ICS functions
###########################

# All calls to ICS will be directed to the ICS REST interface from now on
# args: -
# (Function for test scripts)
use_ics_rest_http() {
	__ics_set_protocoll "http" $ICS_INTERNAL_PORT $ICS_EXTERNAL_PORT
}

# All calls to ICS will be directed to the ICS REST interface from now on
# args: -
# (Function for test scripts)
use_ics_rest_https() {
	__ics_set_protocoll "https" $ICS_INTERNAL_SECURE_PORT $ICS_EXTERNAL_SECURE_PORT
}

# All calls to ICS will be directed to the ICS dmaap interface over http from now on
# args: -
# (Function for test scripts)
use_ics_dmaap_http() {
	echo -e $BOLD"ICS dmaap protocol setting"$EBOLD
	echo -e $RED" - NOT SUPPORTED - "$ERED
	echo -e " Using $BOLD http $EBOLD and $BOLD DMAAP $EBOLD towards ICS"
	ICS_ADAPTER_TYPE="MR-HTTP"
	echo ""
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__ics_set_protocoll() {
	echo -e $BOLD"$ICS_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $ICS_DISPLAY_NAME"

	## Access to ICS

	ICS_SERVICE_PATH=$1"://"$ICS_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		ICS_SERVICE_PATH=$1"://"$ICS_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	# ICS_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
	ICS_ADAPTER_TYPE="REST"
	ICS_ADAPTER=$ICS_SERVICE_PATH

	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args: PROXY|NOPROXY
__ics_export_vars() {
		export ICS_APP_NAME
		export ICS_APP_NAME_ALIAS
		export KUBE_NONRTRIC_NAMESPACE
		export ICS_IMAGE
		export ICS_INTERNAL_PORT
		export ICS_INTERNAL_SECURE_PORT
		export ICS_EXTERNAL_PORT
		export ICS_EXTERNAL_SECURE_PORT
		export ICS_CONFIG_MOUNT_PATH
		export ICS_CONFIG_CONFIGMAP_NAME=$ICS_APP_NAME"-config"
		export ICS_DATA_CONFIGMAP_NAME=$ICS_APP_NAME"-data"
		export ICS_CONTAINER_MNT_DIR
		export ICS_HOST_MNT_DIR
		export ICS_CONFIG_FILE
		export DOCKER_SIM_NWNAME
		export ICS_DISPLAY_NAME
		export ICS_LOGPATH

		export ICS_DATA_PV_NAME=$ICS_APP_NAME"-pv"
		export ICS_DATA_PVC_NAME=$ICS_APP_NAME"-pvc"
		#Create a unique path for the pv each time to prevent a previous volume to be reused
		export ICS_PV_PATH="icsdata-"$(date +%s)
		export HOST_PATH_BASE_DIR

		if [ $1 == "PROXY" ]; then
			export ICS_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT  #Set if proxy is started
			export ICS_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
			if [ $ICS_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$ICS_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
				echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
			else
				echo " Configured with http proxy"
			fi
		else
			export ICS_HTTP_PROXY_CONFIG_PORT=0
			export ICS_HTTP_PROXY_CONFIG_HOST_NAME=""
			echo " Configured without http proxy"
		fi
}


# Start the ICS
# args: PROXY|NOPROXY <config-file>
# (Function for test scripts)
start_ics() {

	echo -e $BOLD"Starting $ICS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "ICS"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "ICS"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $ICS_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $ICS_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $ICS_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $ICS_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $ICS_APP_NAME deployment and service"
			echo " Setting ICS replicas=1"
			res_type=$(__kube_get_resource_type $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
			__kube_scale $res_type $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		# Check if app shall be fully managed by the test script
		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $ICS_APP_NAME app and expose service"

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			__ics_export_vars $1

			# Create config map for config
			datafile=$PWD/tmp/$ICS_CONFIG_FILE
			cp $2 $datafile
			output_yaml=$PWD/tmp/ics_cfc.yaml
			__kube_create_configmap $ICS_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest ICS $datafile $output_yaml

			# Create pv
			input_yaml=$SIM_GROUP"/"$ICS_COMPOSE_DIR"/"pv.yaml
			output_yaml=$PWD/tmp/ics_pv.yaml
			__kube_create_instance pv $ICS_APP_NAME $input_yaml $output_yaml

			# Create pvc
			input_yaml=$SIM_GROUP"/"$ICS_COMPOSE_DIR"/"pvc.yaml
			output_yaml=$PWD/tmp/ics_pvc.yaml
			__kube_create_instance pvc $ICS_APP_NAME $input_yaml $output_yaml

			# Create service
			input_yaml=$SIM_GROUP"/"$ICS_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/ics_svc.yaml
			__kube_create_instance service $ICS_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$ICS_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/ics_app.yaml
			if [ -z "$ICS_SIDECAR_JWT_FILE" ]; then
				cat $input_yaml | sed  '/#ICS_JWT_START/,/#ICS_JWT_STOP/d' > $PWD/tmp/ics_app_tmp.yaml
				input_yaml=$PWD/tmp/ics_app_tmp.yaml
			fi
			__kube_create_instance app $ICS_APP_NAME $input_yaml $output_yaml
		fi

		# Tie the ICS to a worker node so that ICS will always be scheduled to the same worker node if the ICS pod is restarted
		# A PVC of type hostPath is mounted to ICS, for persistent storage, so the ICS must always be on the node which mounted the volume

		# Keep the initial worker node in case the pod need to be "restarted" - must be made to the same node due to a volume mounted on the host
		if [ $retcode_i -eq 0 ]; then
			__ICS_WORKER_NODE=$(kubectl $KUBECONF get pod -l "autotest=ICS" -n $KUBE_NONRTRIC_NAMESPACE -o jsonpath='{.items[*].spec.nodeName}')
			if [ -z "$__ICS_WORKER_NODE" ]; then
				echo -e $YELLOW" Cannot find worker node for pod for $ICS_APP_NAME, persistency may not work"$EYELLOW
			fi
		else
			echo -e $YELLOW" Persistency may not work for app $ICS_APP_NAME in multi-worker node config when running it as a prestarted app"$EYELLOW
		fi


		__check_service_start $ICS_APP_NAME $ICS_SERVICE_PATH$ICS_ALIVE_URL

	else
		__check_included_image 'ICS'
		if [ $? -eq 1 ]; then
			echo -e $RED"The ICS app is not included in this test script"$ERED
			echo -e $RED"ICS will not be started"$ERED
			exit 1
		fi

		curdir=$PWD
		cd $SIM_GROUP
		cd ics
		cd $ICS_HOST_MNT_DIR
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
			mkdir db
		fi

		cd $curdir

		__ics_export_vars $1

		dest_file=$SIM_GROUP/$ICS_COMPOSE_DIR/$ICS_HOST_MNT_DIR/$ICS_CONFIG_FILE

		envsubst < $2 > $dest_file

		__start_container $ICS_COMPOSE_DIR "" NODOCKERARGS 1 $ICS_APP_NAME

		__check_service_start $ICS_APP_NAME $ICS_SERVICE_PATH$ICS_ALIVE_URL
	fi
	echo ""
	return 0
}

# Stop the ics
# args: -
# args: -
# (Function for test scripts)
stop_ics() {
	echo -e $BOLD"Stopping $ICS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		__check_prestarted_image "ICS"
		if [ $? -eq 0 ]; then
			echo -e $YELLOW" Persistency may not work for app $ICS_APP_NAME in multi-worker node config when running it as a prestarted app"$EYELLOW
			res_type=$(__kube_get_resource_type $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
			__kube_scale $res_type $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 0
			return 0
		fi

		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ICS
		echo "  Deleting the replica set - a new will be started when the app is started"
		tmp=$(kubectl $KUBECONF delete rs -n $KUBE_NONRTRIC_NAMESPACE -l "autotest=ICS")
		if [ $? -ne 0 ]; then
			echo -e $RED" Could not delete replica set "$RED
			((RES_CONF_FAIL++))
			return 1
		fi
	else
		docker stop $ICS_APP_NAME &> ./tmp/.dockererr
		if [ $? -ne 0 ]; then
			__print_err "Could not stop $ICS_APP_NAME" $@
			cat ./tmp/.dockererr
			((RES_CONF_FAIL++))
			return 1
		fi
	fi
	echo -e $BOLD$GREEN"Stopped"$EGREEN$EBOLD
	echo ""
	return 0
}

# Start a previously stopped ics
# args: -
# (Function for test scripts)
start_stopped_ics() {
	echo -e $BOLD"Starting (the previously stopped) $ICS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		__check_prestarted_image "ICS"
		if [ $? -eq 0 ]; then
			echo -e $YELLOW" Persistency may not work for app $ICS_APP_NAME in multi-worker node config when running it as a prestarted app"$EYELLOW
			res_type=$(__kube_get_resource_type $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
			__kube_scale $res_type $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
			__check_service_start $ICS_APP_NAME $ICS_SERVICE_PATH$ICS_ALIVE_URL
			return 0
		fi

		# Tie the ICS to the same worker node it was initially started on
		# A PVC of type hostPath is mounted to A1PMS, for persistent storage, so the A1PMS must always be on the node which mounted the volume
		if [ -z "$__ICS_WORKER_NODE" ]; then
			echo -e $RED" No initial worker node found for pod "$RED
			((RES_CONF_FAIL++))
			return 1
		else
			echo -e $BOLD" Setting nodeSelector kubernetes.io/hostname=$__ICS_WORKER_NODE to deployment for $ICS_APP_NAME. Pod will always run on this worker node: $__ICS_WORKER_NODE"$BOLD
			echo -e $BOLD" The mounted volume is mounted as hostPath and only available on that worker node."$BOLD
			tmp=$(kubectl $KUBECONF patch deployment $ICS_APP_NAME -n $KUBE_NONRTRIC_NAMESPACE --patch '{"spec": {"template": {"spec": {"nodeSelector": {"kubernetes.io/hostname": "'$__ICS_WORKER_NODE'"}}}}}')
			if [ $? -ne 0 ]; then
				echo -e $YELLOW" Cannot set nodeSelector to deployment for $ICS_APP_NAME, persistency may not work"$EYELLOW
			fi
			__kube_scale deployment $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi
	else
		docker start $ICS_APP_NAME &> ./tmp/.dockererr
		if [ $? -ne 0 ]; then
			__print_err "Could not start (the stopped) $ICS_APP_NAME" $@
			cat ./tmp/.dockererr
			((RES_CONF_FAIL++))
			return 1
		fi
	fi
	__check_service_start $ICS_APP_NAME $ICS_SERVICE_PATH$ICS_ALIVE_URL
	if [ $? -ne 0 ]; then
		return 1
	fi
	echo ""
	return 0
}

# Turn on debug level tracing in ICS
# args: -
# (Function for test scripts)
set_ics_debug() {
	echo -e $BOLD"Setting ics debug logging"$EBOLD
	curlString="$ICS_SERVICE_PATH$ICS_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "Could not set debug mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Turn on trace level tracing in ICS
# args: -
# (Function for test scripts)
set_ics_trace() {
	echo -e $BOLD"Setting ics trace logging"$EBOLD
	curlString="$ICS_SERVICE_PATH$ICS_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		__print_err "Could not set trace mode" $@
		((RES_CONF_FAIL++))
		return 1
	fi
	echo ""
	return 0
}

# Perform curl retries when making direct call to ICS for the specified http response codes
# Speace separated list of http response codes
# args: [<response-code>]*
use_ics_retries() {
	echo -e $BOLD"Do curl retries to the ICS REST inteface for these response codes:$@"$EBOLD
	ICS_RETRY_CODES=$@
	echo ""
	return 0
}

# Check the ics logs for WARNINGs and ERRORs
# args: -
# (Function for test scripts)
check_ics_logs() {
	__check_container_logs "ICS" $ICS_APP_NAME $ICS_LOGPATH WARN ERR
}


# Tests if a variable value in the ICS is equal to a target value and and optional timeout.
# Arg: <variable-name> <target-value> - This test set pass or fail depending on if the variable is
# equal to the target or not.
# Arg: <variable-name> <target-value> <timeout-in-sec>  - This test waits up to the timeout seconds
# before setting pass or fail depending on if the variable value becomes equal to the target
# value or not.
# (Function for test scripts)
ics_equal() {
	if [ $# -eq 2 ] || [ $# -eq 3 ]; then
		__var_test ICS "$ICS_SERVICE_PATH/" $1 "=" $2 $3
	else
		__print_err "Wrong args to ics_equal, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}


##########################################
######### A1-E information  API ##########
##########################################
#Function prefix: ics_api_a1

# API Test function: GET /A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs
# args: <response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]
# (Function for test scripts)
ics_api_a1_get_job_ids() {
	__log_test_start $@

	# Valid number of parameters 4,5,6 etc
	if [ $# -lt 3 ]; then
		__print_err "<response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]" $@
		return 1
	fi
	search=""
	if [ $3 != "NOWNER" ]; then
		search="?owner="$3
	fi

	if [ $2 != "NOTYPE" ]; then
		if [ -z "$search" ]; then
			search="?eiTypeId="$2
		else
			search=$search"&eiTypeId="$2
		fi
	fi
	query="/A1-EI/v1/eijobs$search"
    res="$(__do_curl_to_api ICS GET $query)"
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
ics_api_a1_get_type() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
		__print_err "<response-code> <type-id> [<schema-file>]" $@
		return 1
	fi

	query="/A1-EI/v1/eitypes/$2"
    res="$(__do_curl_to_api ICS GET $query)"
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
		targetJson=$schema
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
ics_api_a1_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ (EMPTY | [<type-id>]+) ]" $@
		return 1
	fi

	query="/A1-EI/v1/eitypes"
    res="$(__do_curl_to_api ICS GET $query)"
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
# args: <response-code> <job-id> [<status> [<timeout>]]
# (Function for test scripts)
ics_api_a1_get_job_status() {
	__log_test_start $@

	if [ $# -lt 2 ] && [ $# -gt 4 ]; then
		__print_err "<response-code> <job-id> [<status> [<timeout>]]" $@
		return 1
	fi

	query="/A1-EI/v1/eijobs/$2/status"

	start=$SECONDS
	for (( ; ; )); do
		res="$(__do_curl_to_api ICS GET $query)"
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

	__log_test_pass
	return 0
}

# API Test function: GET ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <job-id> [<type-id> <target-url> <owner-id> <template-job-file>]
# (Function for test scripts)
ics_api_a1_get_job() {
	__log_test_start $@

	if [ $# -ne 2 ] && [ $# -ne 7 ]; then
		__print_err "<response-code> <job-id> [<type-id> <target-url> <owner-id> <notification-url> <template-job-file>]" $@
		return 1
	fi
	query="/A1-EI/v1/eijobs/$2"
    res="$(__do_curl_to_api ICS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 7 ]; then
		body=${res:0:${#res}-3}

		if [ -f $7 ]; then
			jobfile=$(cat $7)
			jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
		else
			__log_test_fail_general "Job template file "$6", does not exist"
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

	__log_test_pass
	return 0
}

# API Test function: DELETE ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args: <response-code> <job-id>
# (Function for test scripts)
ics_api_a1_delete_job() {
	__log_test_start $@

	if [ $# -ne 2 ]; then
		__print_err "<response-code> <job-id>" $@
		return 1
	fi
	query="/A1-EI/v1/eijobs/$2"
    res="$(__do_curl_to_api ICS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT ​/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}
# args <response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file>
# (Function for test scripts)
ics_api_a1_put_job() {
	__log_test_start $@

	if [ $# -lt 7 ]; then
		__print_err "<response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file>" $@
		return 1
	fi
	if [ -f $7 ]; then
		jobfile=$(cat $7)
		jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
	else
		__log_test_fail_general "Job template file "$7", does not exist"
		return 1
	fi

	inputJson="{\"eiTypeId\": \"$3\", \"jobResultUri\": \"$4\",\"jobOwner\": \"$5\",\"jobStatusNotificationUri\": \"$6\",\"jobDefinition\": $jobfile}"
	file="./tmp/.p.json"
	echo "$inputJson" > $file

	query="/A1-EI/v1/eijobs/$2"

    res="$(__do_curl_to_api ICS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}


##########################################
####   information Data Producer API   ####
##########################################
# Function prefix: ics_api_edp

# API Test function: GET /ei-producer/v1/eitypes
# API Test function: GET /data-producer/v1/info-types
# args: <response-code> [ EMPTY | <type-id>+]
# (Function for test scripts)
ics_api_edp_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ EMPTY | <type-id>+]" $@
		return 1
	fi
	query="/data-producer/v1/info-types"
    res="$(__do_curl_to_api ICS GET $query)"
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
# API Test function: GET /data-producer/v1/info-producers/{infoProducerId}/status
# args: <response-code> <producer-id> [<status> [<timeout>]]
# (Function for test scripts)
ics_api_edp_get_producer_status() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 4 ]; then
		__print_err "<response-code> <producer-id> [<status> [<timeout>]]" $@
		return 1
	fi
	query="/data-producer/v1/info-producers/$2/status"
	start=$SECONDS
	for (( ; ; )); do
		res="$(__do_curl_to_api ICS GET $query)"
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
ics_api_edp_get_producer_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ EMPTY | <producer-id>+]" $@
		return 1
	fi

	query="/ei-producer/v1/eiproducers"
    res="$(__do_curl_to_api ICS GET $query)"
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
# API Test function: GET /data-producer/v1/info-producers
# args (v1_2): <response-code> [ ( NOTYPE | <type-id> ) [ EMPTY | <producer-id>+] ]
# (Function for test scripts)
ics_api_edp_get_producer_ids_2() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ ( NOTYPE | <type-id> ) [ EMPTY | <producer-id>+] ]" $@
		return 1
	fi
	query="/data-producer/v1/info-producers"
	if [ $# -gt 1 ] && [ $2 != "NOTYPE" ]; then
		query=$query"?info_type_id=$2&infoTypeId=$2"  #info_type_id changed to infoTypeId in F-release.
														#Remove info_type_id when F-release is no longer supported
	fi
    res="$(__do_curl_to_api ICS GET $query)"
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
ics_api_edp_get_type() {
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
    res="$(__do_curl_to_api ICS GET $query)"
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
# API Test function: GET /data-producer/v1/info-types/{infoTypeId}
# args: (v1_2) <response-code> <type-id> [<job-schema-file> [ <info-type-info> ]]
# (Function for test scripts)
ics_api_edp_get_type_2() {
	__log_test_start $@

	paramError=1
	if [ $# -eq 2 ]; then
		paramError=0
	fi
	if [ $# -eq 3 ]; then
		paramError=0
	fi
	if [ $# -eq 4 ]; then
		paramError=0
	fi
    if [ $paramError -ne 0 ]; then
		__print_err "<response-code> <type-id> [<job-schema-file> [ <info-type-info> ]]" $@
		return 1
	fi
	query="/data-producer/v1/info-types/$2"

    res="$(__do_curl_to_api ICS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	if [ $# -ge 3 ]; then
		body=${res:0:${#res}-3}

		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Job template file "$3", does not exist"
			return 1
		fi
		info_data=""
		if [ $# -gt 3 ]; then
			if [ -f $4 ]; then
				info_data=$(cat $4)
			else
				__log_test_fail_general "Info-data file "$4", does not exist"
				return 1
			fi
			info_data=",\"info_type_information\":$info_data"
		fi
		targetJson="{\"info_job_data_schema\":$schema $info_data}"

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
# API Test function: PUT /data-producer/v1/info-types/{infoTypeId}
# args: (v1_2) <response-code> <type-id> <job-schema-file> [ <info-type-info> ]
# (Function for test scripts)
ics_api_edp_put_type_2() {
	__log_test_start $@

	if [ $# -lt 3 ] || [ $# -gt 4 ]; then
		__print_err "<response-code> <type-id> <job-schema-file> [ <info-type-info> ]" $@
		return 1
	fi

	if [ ! -f $3 ]; then
		__log_test_fail_general "Job schema file "$3", does not exist"
		return 1
	fi

	info_data=""
	if [ $# -gt 3 ]; then
		if [ -f $4 ]; then
			info_data=$(cat $4)
		else
			__log_test_fail_general "Info-data file "$4", does not exist"
			return 1
		fi
		info_data=",\"info_type_information\":$info_data"
	fi

	schema=$(cat $3)
	input_json="{\"info_job_data_schema\":$schema $info_data}"
	file="./tmp/put_type.json"
	echo $input_json > $file

	query="/data-producer/v1/info-types/$2"
    res="$(__do_curl_to_api ICS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE /ei-producer/v1/eitypes/{eiTypeId}
# API Test function: DELETE /data-producer/v1/info-types/{infoTypeId}
# args: (v1_2) <response-code> <type-id>
# (Function for test scripts)
ics_api_edp_delete_type_2() {
	__log_test_start $@

    if [ $# -ne 2 ]; then
		__print_err "<response-code> <type-id>" $@
		return 1
	fi

	query="/data-producer/v1/info-types/$2"
    res="$(__do_curl_to_api ICS DELETE $query)"
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
ics_api_edp_get_producer() {
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
    res="$(__do_curl_to_api ICS GET $query)"
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
					__log_test_fail_general "Schema file "${arr[$i+1]}", does not exist"
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
# API Test function: GET /data-producer/v1/info-producers/{infoProducerId}
# args (v1_2): <response-code> <producer-id> [<job-callback> <supervision-callback> (EMPTY | <type-id>+) ]
# (Function for test scripts)
ics_api_edp_get_producer_2() {
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
	query="/data-producer/v1/info-producers/$2"
    res="$(__do_curl_to_api ICS GET $query)"
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
			targetJson="{\"supported_info_types\":$targetJson,\"info_job_callback_url\": \"$3\",\"info_producer_supervision_callback_url\": \"$4\"}"
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
# API Test function: DELETE /data-producer/v1/info-producers/{infoProducerId}
# args: <response-code> <producer-id>
# (Function for test scripts)
ics_api_edp_delete_producer() {
	__log_test_start $@

    if [ $# -lt 2 ]; then
		__print_err "<response-code> <producer-id>" $@
		return 1
	fi
	query="/data-producer/v1/info-producers/$2"
    res="$(__do_curl_to_api ICS DELETE $query)"
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
ics_api_edp_put_producer() {
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
				__log_test_fail_general "Schema file "${arr[$i+1]}", does not exist"
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
    res="$(__do_curl_to_api ICS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: PUT /ei-producer/v1/eiproducers/{eiProducerId}
# API Test function: PUT /data-producer/v1/info-producers/{infoProducerId}
# args: (v1_2) <response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE|[<type-id>+]
# (Function for test scripts)
ics_api_edp_put_producer_2() {
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
	inputJson="\"supported_info_types\":"$inputJson"]"

	inputJson=$inputJson",\"info_job_callback_url\": \"$3\",\"info_producer_supervision_callback_url\": \"$4\""

	inputJson="{"$inputJson"}"

	file="./tmp/.p.json"
	echo "$inputJson" > $file
	query="/data-producer/v1/info-producers/$2"
    res="$(__do_curl_to_api ICS PUT $query $file)"
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
ics_api_edp_get_producer_jobs() {
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
    res="$(__do_curl_to_api ICS GET $query)"
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
					__log_test_fail_general "Job template file "${arr[$i+4]}", does not exist"
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
# API Test function: GET /data-producer/v1/info-producers/{infoProducerId}/info-jobs
# args: (V1-2) <response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)
# (Function for test scripts)
ics_api_edp_get_producer_jobs_2() {
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
	query="/data-producer/v1/info-producers/$2/info-jobs"
    res="$(__do_curl_to_api ICS GET $query)"
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
					__log_test_fail_general "Job template file "${arr[$i+4]}", does not exist"
					return 1
				fi
				targetJson=$targetJson"{\"info_job_identity\":\"${arr[$i]}\",\"info_type_identity\":\"${arr[$i+1]}\",\"target_uri\":\"${arr[$i+2]}\",\"owner\":\"${arr[$i+3]}\",\"info_job_data\":$jobfile, \"last_updated\":\"????\"}"

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
# Function prefix: ics_api_service

# API Test function: GET ​/status
# args: <response-code>
# (Function for test scripts)
ics_api_service_status() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code>" $@
		return 1
	fi
	res="$(__do_curl_to_api ICS GET /status)"
    status=${res:${#res}-3}
	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi
	__log_test_pass
	return 0
}

###########################################
######### Info data consumer API ##########
###########################################
#Function prefix: ics_api_idc


# API Test function: GET /data-consumer/v1/info-types
# args: <response-code> [ (EMPTY | [<type-id>]+) ]
# (Function for test scripts)
ics_api_idc_get_type_ids() {
	__log_test_start $@

    if [ $# -lt 1 ]; then
		__print_err "<response-code> [ (EMPTY | [<type-id>]+) ]" $@
		return 1
	fi

	query="/data-consumer/v1/info-types"
    res="$(__do_curl_to_api ICS GET $query)"
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

# API Test function: GET /data-consumer/v1/info-jobs
# args: <response-code> <type-id>|NOTYPE <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]
# (Function for test scripts)
ics_api_idc_get_job_ids() {
	__log_test_start $@

	# Valid number of parameters 4,5,6 etc
	if [ $# -lt 3 ]; then
		__print_err "<response-code> <type-id>|NOTYPE  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]" $@
		return 1
	fi
	search=""
	if [ $3 != "NOWNER" ]; then
		search="?owner="$3
	fi

	if [ $2 != "NOTYPE" ]; then
		if [ -z "$search" ]; then
			search="?infoTypeId="$2
		else
			search=$search"&infoTypeId="$2
		fi
	fi
	query="/data-consumer/v1/info-jobs$search"

    res="$(__do_curl_to_api ICS GET $query)"
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

# API Test function: GET /data-consumer/v1/info-jobs/{infoJobId}
# args: <response-code> <job-id> [<type-id> <target-url> <owner-id> <template-job-file>]
# (Function for test scripts)
ics_api_idc_get_job() {
	__log_test_start $@

	if [ $# -ne 2 ] && [ $# -ne 7 ]; then
		__print_err "<response-code> <job-id> [<type-id> <target-url> <owner-id> <notification-url> <template-job-file>]" $@
		return 1
	fi
	query="/data-consumer/v1/info-jobs/$2"
    res="$(__do_curl_to_api ICS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -eq 7 ]; then
		body=${res:0:${#res}-3}

		if [ -f $7 ]; then
			jobfile=$(cat $7)
			jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
		else
			__log_test_fail_general "Job template file "$6", does not exist"
			return 1
		fi
		targetJson="{\"info_type_id\": \"$3\", \"job_result_uri\": \"$4\",\"job_owner\": \"$5\",\"status_notification_uri\": \"$6\",\"job_definition\": $jobfile}"
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


# API Test function: PUT ​/data-consumer/v1/info-jobs/{infoJobId}
# args: <response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file> [ VALIDATE ]
# (Function for test scripts)
ics_api_idc_put_job() {
	__log_test_start $@

	if [ $# -lt 7 ] || [ $# -gt 8 ]; then
		__print_err "<response-code> <job-id> <type-id> <target-url> <owner-id> <notification-url> <template-job-file> [ VALIDATE ]" $@
		return 1
	fi
	if [ -f $7 ]; then
		jobfile=$(cat $7)
		jobfile=$(echo "$jobfile" | sed "s/XXXX/$2/g")
	else
		__log_test_fail_general "Job template file "$7", does not exist"
		return 1
	fi

	inputJson="{\"info_type_id\": \"$3\", \"job_result_uri\": \"$4\",\"job_owner\": \"$5\",\"status_notification_uri\": \"$6\",\"job_definition\": $jobfile}"
	file="./tmp/.p.json"
	echo "$inputJson" > $file

	query="/data-consumer/v1/info-jobs/$2"

	if [ $# -eq 8 ]; then
		if [ $8 == "VALIDATE" ]; then
			query=$query"?typeCheck=true"
		fi
	fi

    res="$(__do_curl_to_api ICS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE ​/data-consumer/v1/info-jobs/{infoJobId}
# args: <response-code> <job-id>
# (Function for test scripts)
ics_api_idc_delete_job() {
	__log_test_start $@

	if [ $# -ne 2 ]; then
		__print_err "<response-code> <job-id>" $@
		return 1
	fi
	query="/data-consumer/v1/info-jobs/$2"
    res="$(__do_curl_to_api ICS DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: GET ​/data-consumer/v1/info-types/{infoTypeId}
# args: <response-code> <type-id> [<schema-file> [<type-status> <producers-count]]
# (Function for test scripts)
ics_api_idc_get_type() {
	__log_test_start $@

    if [ $# -lt 2 ] || [ $# -gt 5 ]; then
		__print_err "<response-code> <type-id> [<schema-file> [<type-status> <producers-count]]" $@
		return 1
	fi

	query="/data-consumer/v1/info-types/$2"
    res="$(__do_curl_to_api ICS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		if [ -f $3 ]; then
			schema=$(cat $3)
		else
			__log_test_fail_general "Schema file "$3", does not exist"
			return 1
		fi
		if [ $# -eq 5 ]; then
			targetJson="{\"job_data_schema\":$schema, \"type_status\":\"$4\", \"no_of_producers\":$5}"
		else
			targetJson="{\"job_data_schema\":$schema}"
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

# API Test function: GET /data-consumer/v1/info-jobs/{infoJobId}/status
# This test only status during an optional timeout. No test of the list of producers
# args: <response-code> <job-id> [<status> [<timeout>]]
# (Function for test scripts)
ics_api_idc_get_job_status() {
	__log_test_start $@

	if [ $# -lt 2 ] && [ $# -gt 4 ]; then
		__print_err "<response-code> <job-id> [<status> [<timeout>]]" $@
		return 1
	fi

	query="/data-consumer/v1/info-jobs/$2/status"

	start=$SECONDS
	for (( ; ; )); do
		res="$(__do_curl_to_api ICS GET $query)"
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
			targetJson="{\"info_job_status\": \"$3\"}"
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

	__log_test_pass
	return 0
}

# API Test function: GET /data-consumer/v1/info-jobs/{infoJobId}/status
# This function test status and the list of producers with and optional timeout
# args: <response-code> <job-id> [<status> EMPTYPROD|( <prod-count> <producer-id>+ ) [<timeout>]]
# (Function for test scripts)
ics_api_idc_get_job_status2() {

	__log_test_start $@
	param_error=0
	if [ $# -lt 2 ]; then
		param_error=1
	fi
	args=("$@")
	timeout=0
	if [ $# -gt 2 ]; then
		if [ $# -lt 4 ]; then
			param_error=1
		fi
		targetJson="{\"info_job_status\": \"$3\""
		if [ "$4" == "EMPTYPROD" ]; then
			targetJson=$targetJson",\"producers\": []}"
			if [ $# -gt 4 ]; then
				timeout=$5
			fi
		else
			targetJson=$targetJson",\"producers\": ["
			if [ $# -eq $(($4+5)) ]; then
				idx=$(($4+4))
				timeout=${args[$idx]}
			fi
			for ((ics_i = 0 ; ics_i < $4 ; ics_i++)); do
				idx=$(($ics_i+4))
				if [ $ics_i -gt 0 ]; then
					targetJson=$targetJson","
				fi
				targetJson=$targetJson"\""${args[$idx]}"\""
			done
			targetJson=$targetJson"]}"
		fi
	fi

	if [ $param_error -ne 0 ]; then
		__print_err "<response-code> <job-id> [<status> EMPTYPROD|( <prod-count> <producer-id>+ ) [<timeout>]]" $@
		return 1
	fi

	query="/data-consumer/v1/info-jobs/$2/status"

	start=$SECONDS
	for (( ; ; )); do
		res="$(__do_curl_to_api ICS GET $query)"
		status=${res:${#res}-3}

		if [ $# -gt 2 ]; then
			duration=$((SECONDS-start))
			echo -ne " Response=${status} after ${duration} seconds, waiting for ${3} ${SAMELINE}"
			if [ $duration -gt $timeout ]; then
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
		if [ $# -gt 2 ] && [ $status -eq $1 ]; then
			body=${res:0:${#res}-3}
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

	__log_test_pass
	return 0
}

##########################################
####     Type subscriptions           ####
##########################################

# API Test function: GET /data-consumer/v1/info-type-subscription
# args: <response-code>  <owner-id>|NOOWNER [ EMPTY | <subscription-id>+]
# (Function for test scripts)
ics_api_idc_get_subscription_ids() {
	__log_test_start $@

    if [ $# -lt 3 ]; then
		__print_err "<response-code> <owner-id>|NOOWNER [ EMPTY | <subscription-id>+]" $@
		return 1
	fi

	query="/data-consumer/v1/info-type-subscription"
	search=""
	if [ $2 != "NOOWNER" ]; then
		search="?owner="$2
	fi

    res="$(__do_curl_to_api ICS GET $query$search)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="["
		if [ $3 != "EMPTY" ]; then
			for pid in ${@:3} ; do
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

# API Test function: GET /data-consumer/v1/info-type-subscription/{subscriptionId}
# args: <response-code>  <subscription-id> [ <owner-id> <status-uri> ]
# (Function for test scripts)
ics_api_idc_get_subscription() {
	__log_test_start $@

    if [ $# -ne 2 ] && [ $# -ne 4 ]; then
		__print_err "<response-code>  <subscription-id> [ <owner-id> <status-uri> ]" $@
		return 1
	fi

	query="/data-consumer/v1/info-type-subscription/$2"
    res="$(__do_curl_to_api ICS GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 2 ]; then
		body=${res:0:${#res}-3}
		targetJson="{\"owner\":\"$3\",\"status_result_uri\":\"$4\"}"
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

# API Test function: PUT /data-consumer/v1/info-type-subscription/{subscriptionId}
# args: <response-code>  <subscription-id> <owner-id> <status-uri>
# (Function for test scripts)
ics_api_idc_put_subscription() {
	__log_test_start $@

    if [ $# -ne 4 ]; then
		__print_err "<response-code>  <subscription-id> <owner-id> <status-uri>" $@
		return 1
	fi

	inputJson="{\"owner\": \"$3\",\"status_result_uri\": \"$4\"}"
	file="./tmp/.p.json"
	echo "$inputJson" > $file

	query="/data-consumer/v1/info-type-subscription/$2"
    res="$(__do_curl_to_api ICS PUT $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE /data-consumer/v1/info-type-subscription/{subscriptionId}
# args: <response-code>  <subscription-id>
# (Function for test scripts)
ics_api_idc_delete_subscription() {
	__log_test_start $@

	if [ $# -ne 2 ]; then
		__print_err "<response-code>  <subscription-id> " $@
		return 1
	fi

	query="/data-consumer/v1/info-type-subscription/$2"
    res="$(__do_curl_to_api ICS DELETE $query)"
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
# Function prefix: ics_api_admin

# Admin to remove all jobs
# args: <response-code> [ <type> ]
# (Function for test scripts)

ics_api_admin_reset() {
	__log_test_start $@

	query="/A1-EI/v1/eijobs"
    res="$(__do_curl_to_api ICS GET $query)"
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
		query="/A1-EI/v1/eijobs/$job"
		res="$(__do_curl_to_api ICS DELETE $query)"
		status=${res:${#res}-3}
		if [ $status -ne 204 ]; then
			__log_test_fail_status_code $1 $status
			return 1
		fi
		echo " Deleted job: "$job
	done

	__log_test_pass
	return 0
}

##########################################
####     Reset jobs and producers     ####
##########################################


# Admin reset to remove all data in ics; jobs, producers etc
# NOTE - only works in kubernetes and the pod should not be running
# args: -
# (Function for test scripts)

ics_kube_pvc_reset() {
	__log_test_start $@

	pvc_name=$(kubectl $KUBECONF get pvc -n $KUBE_NONRTRIC_NAMESPACE  --no-headers -o custom-columns=":metadata.name" | grep information)
	if [ -z "$pvc_name" ]; then
		pvc_name=informationservice-pvc
	fi
	echo " Trying to reset pvc: "$pvc_name

	__kube_clean_pvc $ICS_APP_NAME $KUBE_NONRTRIC_NAMESPACE $pvc_name $ICS_CONTAINER_MNT_DIR

	__log_test_pass
	return 0
}

# args: <realm> <client-name> <client-secret>
ics_configure_sec() {
	export ICS_CREDS_GRANT_TYPE="client_credentials"
	export ICS_CREDS_CLIENT_SECRET=$3
	export ICS_CREDS_CLIENT_ID=$2
	export ICS_AUTH_SERVICE_URL=$KEYCLOAK_SERVICE_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$1/protocol/openid-connect/token
	export ICS_SIDECAR_MOUNT="/token-cache"
	export ICS_SIDECAR_JWT_FILE=$ICS_SIDECAR_MOUNT"/jwt.txt"

	export AUTHSIDECAR_APP_NAME
	export AUTHSIDECAR_DISPLAY_NAME
}