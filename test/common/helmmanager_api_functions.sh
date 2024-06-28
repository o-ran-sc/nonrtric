#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions test functions for Helm Manager

################ Test engine functions ################

# Create the image var used during the test
# arg: [<image-tag-suffix>] (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__HELMMANAGER_imagesetup() {
	__check_and_create_image_var HELMMANAGER "HELM_MANAGER_IMAGE" "HELM_MANAGER_IMAGE_BASE" "HELM_MANAGER_IMAGE_TAG" $1 "$HELM_MANAGER_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both arg var may contain: 'remote', 'remote-remove' or 'local'
__HELMMANAGER_imagepull() {
	__check_and_pull_image $1 "$HELM_MANAGER_DISPLAY_NAME" $HELM_MANAGER_APP_NAME HELM_MANAGER_IMAGE
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__HELMMANAGER_image_data() {
	echo -e "$HELM_MANAGER_DISPLAY_NAME\t$(docker images --format $1 $HELM_MANAGER_IMAGE)" >>   $2
	if [ ! -z "$HELM_MANAGER_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $HELM_MANAGER_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__HELMMANAGER_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest HELMMANAGER
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__HELMMANAGER_kube_scale_zero_and_wait() {
	__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app "$KUBE_NONRTRIC_NAMESPACE"-"$HELM_MANAGER_APP_NAME"
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__HELMMANAGER_kube_delete_all() {
	__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest HELMMANAGER
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__HELMMANAGER_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=HELMMANAGER" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 > $1$2_helmmanager.log 2>&1
	else
		docker logs $HELM_MANAGER_APP_NAME > $1$2_helmmanager.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__HELMMANAGER_initial_setup() {
	use_helm_manager_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__HELMMANAGER_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "HELMMANAGER $HELM_MANAGER_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
	else
		echo "HELMMANAGER $HELM_MANAGER_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__HELMMANAGER_test_requirements() {
	tmp=$(which helm)
	if [ $? -ne 0 ]; then
		echo $RED" Helm3 is required for running helm manager tests. Pls install helm3"
		exit 1
	fi
	tmp_version=$(helm version | grep 'v3')
	if [ -z "$tmp_version" ]; then
		echo $RED" Helm3 is required for running helm manager tests. Pls install helm3"
		exit 1
	fi
}

#######################################################

# Set http as the protocol to use for all communication to the Helm Manager
# args: -
# (Function for test scripts)
use_helm_manager_http() {
	__helm_manager_set_protocoll "http" $HELM_MANAGER_INTERNAL_PORT $HELM_MANAGER_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Helm Manager
# args: -
# (Function for test scripts)
use_helm_manager_https() {
	__helm_manager_set_protocoll "https" $HELM_MANAGER_INTERNAL_SECURE_PORT $HELM_MANAGER_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__helm_manager_set_protocoll() {
	echo -e $BOLD"$HELM_MANAGER_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $HELM_MANAGER_DISPLAY_NAME"

	## Access to Helm Manager

	HELMMANAGER_SERVICE_PATH=$1"://$HELM_MANAGER_USER:$HELM_MANAGER_PWD@"$HELM_MANAGER_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	if [ $RUNMODE == "KUBE" ]; then
		HELMMANAGER_SERVICE_PATH=$1"://$HELM_MANAGER_USER:$HELM_MANAGER_PWD@"$HELM_MANAGER_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
	fi

	echo ""
}

# Export env vars for config files, docker compose and kube resources
# args:
__helm_manager_export_vars() {

	export HELM_MANAGER_APP_NAME
	export HELM_MANAGER_DISPLAY_NAME

	export DOCKER_SIM_NWNAME
	export KUBE_NONRTRIC_NAMESPACE

	export HELM_MANAGER_EXTERNAL_PORT
	export HELM_MANAGER_INTERNAL_PORT
	export HELM_MANAGER_EXTERNAL_SECURE_PORT
	export HELM_MANAGER_INTERNAL_SECURE_PORT
	export HELM_MANAGER_CLUSTER_ROLE
	export HELM_MANAGER_SA_NAME
	export HELM_MANAGER_ALIVE_URL
	export HELM_MANAGER_COMPOSE_DIR
	export HELM_MANAGER_USER
	export HELM_MANAGER_PWD
}

# Start the Helm Manager container
# args: -
# (Function for test scripts)
start_helm_manager() {

	echo -e $BOLD"Starting $HELM_MANAGER_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "HELMMANAGER"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "HELMMANAGER"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $HELM_MANAGER_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $HELM_MANAGER_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $HELM_MANAGER_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $HELM_MANAGER_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $HELM_MANAGER_APP_NAME deployment and service"
			echo " Setting $HELM_MANAGER_APP_NAME replicas=1"
			__kube_scale sts $HELM_MANAGER_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then

			echo -e " Creating $HELM_MANAGER_APP_NAME app and expose service"

			#Check if nonrtric namespace exists, if not create it
			__kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

			__helm_manager_export_vars

			#Create sa
			input_yaml=$SIM_GROUP"/"$HELM_MANAGER_COMPOSE_DIR"/"sa.yaml
			output_yaml=$PWD/tmp/helmmanager_sa_svc.yaml
			__kube_create_instance sa $HELM_MANAGER_APP_NAME $input_yaml $output_yaml

			#Create service
			input_yaml=$SIM_GROUP"/"$HELM_MANAGER_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/helmmanager_svc.yaml
			__kube_create_instance service $HELM_MANAGER_APP_NAME $input_yaml $output_yaml

			#Create app
			input_yaml=$SIM_GROUP"/"$HELM_MANAGER_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/helmmanager_app.yaml
			__kube_create_instance app $HELM_MANAGER_APP_NAME $input_yaml $output_yaml
		fi

		__check_service_start $HELM_MANAGER_APP_NAME $HELMMANAGER_SERVICE_PATH$HELM_MANAGER_ALIVE_URL

	else
		__check_included_image 'HELMMANAGER'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Helm Manager app is not included as managed in this test script"$ERED
			echo -e $RED"The Helm Manager will not be started"$ERED
			exit
		fi

		__helm_manager_export_vars

		__start_container $HELM_MANAGER_COMPOSE_DIR "" NODOCKERARGS 1 $HELM_MANAGER_APP_NAME

		__check_service_start $HELM_MANAGER_APP_NAME $HELMMANAGER_SERVICE_PATH$HELM_MANAGER_ALIVE_URL
	fi
	echo ""
}

# Execute a curl cmd towards the helm manager.
# args: GET <path>
# args: POST <path> <file-to-post>
# args: POST3 <path> <name> <file-to-post> <name> <file-to-post> <name> <file-to-post>
__execute_curl_to_helmmanger() {
    TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
    echo "(${BASH_LINENO[0]}) - ${TIMESTAMP}: ${FUNCNAME[0]}" $@ >> $HTTPLOG
	proxyflag=""
	if [ ! -z "$KUBE_PROXY_PATH" ]; then
		if [ $KUBE_PROXY_HTTPX == "http" ]; then
			proxyflag=" --proxy $KUBE_PROXY_PATH"
		else
			proxyflag=" --proxy-insecure --proxy $KUBE_PROXY_PATH"
		fi
	fi
	if [ $1 == "GET" ]; then
		curlstring="curl -skw %{http_code} $proxyflag $HELMMANAGER_SERVICE_PATH$2"
	elif [ $1 == "POST" ]; then
		curlstring="curl -skw %{http_code} $proxyflag $HELMMANAGER_SERVICE_PATH$2 -X POST --data-binary @$3 -H Content-Type:application/json"
	elif [ $1 == "POST1_2" ]; then
		curlstring="curl -skw %{http_code} $proxyflag $HELMMANAGER_SERVICE_PATH$2 -X POST -F $3=<$4 -F $5=@$6 -F $7=@$8 "
	elif [ $1 == "DELETE" ]; then
		curlstring="curl -skw %{http_code} $proxyflag $HELMMANAGER_SERVICE_PATH$2 -X DELETE"
	else
		echo " Unknown operation $1" >> $HTTPLOG
		echo "000"
		return 1
	fi
	echo " CMD: $curlstring" >> $HTTPLOG
	res="$($curlstring)"
	retcode=$?
	echo " RESP: $res" >> $HTTPLOG
    if [ $retcode -ne 0 ]; then
        echo "000"
		return 1
    fi
    echo $res
	return 0
}

# API Test function: GET ​/helm/charts
# args: <response-code> [ EMPTY | ( <chart> <version> <namespace> <release> <repo> )+ ]
# (Function for test scripts)
helm_manager_api_get_charts() {
	__log_test_start $@

	error_params=1
	variablecount=$(($#-1))
    if [ $# -eq 1 ]; then
		error_params=0
	elif [ $# -eq 2 ] && [ $2 == "EMPTY" ]; then
		error_params=0
	elif [ $(($variablecount%5)) -eq 0 ]; then
		error_params=0
	fi


	if [ $error_params -eq 1 ]; then
		__print_err "<response-code>" $@
		return 1
	fi

	query="/helm/charts"
    res="$(__execute_curl_to_helmmanger GET $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	if [ $# -gt 1 ]; then
		body=${res:0:${#res}-3}
		shift
		if [ $# -eq 1 ]; then
			targetJson='{"charts":[]}'
		else
			targetJson='{"charts":['
			arr=(${@})
			for ((i=0; i<$#; i=i+5)); do
				if [ "$i" -gt 0 ]; then
					targetJson=$targetJson","
				fi
				chart_version=${arr[$i+2]}
				if [ $chart_version == "DEFAULT-VERSION" ]; then
					chart_version="0.1.0"
				fi
				targetJson=$targetJson'{"releaseName":"'${arr[$i+3]}'","chartId":{"name":"'${arr[$i+1]}'","version":"'0.1.0'"},"namespace":"'${arr[$i+4]}'","repository":{"repoName":"'${arr[$i+0]}'","protocol":null,"address":null,"port":null,"userName":null,"password":null},"overrideParams":null}'
			done
			targetJson=$targetJson']}'
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

# API Test function: POST ​/helm/repo - add repo
# args: <response-code> <repo-name> <repo-protocol> <repo-address> <repo-port>
# (Function for test scripts)
helm_manager_api_post_repo() {
	__log_test_start $@

    if [ $# -ne 5 ]; then
		__print_err "<response-code> <repo-name> <repo-protocol> <repo-address> <repo-port>" $@
		return 1
	fi

	query="/helm/repo"
	file="./tmp/cm-repo.json"
	file_data='{"address" : "'$4'","repoName": "'$2'","protocol": "'$3'","port": "'$5'"}'
	echo $file_data > $file
	echo " FILE: $file_data" >> $HTTPLOG
    res="$(__execute_curl_to_helmmanger POST $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: POST /helm/onboard/chart - onboard chart
# args: <response-code> <repo> <chart> <version> <release> <namespace>
# (Function for test scripts)
helm_manager_api_post_onboard_chart() {
	__log_test_start $@

    if [ $# -ne 6 ]; then
		__print_err "<response-code> <repo> <chart> <version> <release> <namespace> " $@
		return 1
	fi

	query="/helm/onboard/chart"
	file="./tmp/chart.json"
	chart_version=$4
	if [ $chart_version == "DEFAULT-VERSION" ]; then
		chart_version="0.1.0"
	fi
	file_data='{"chartId":{"name":"'$3'","version":"'$chart_version'"},"namespace":"'$6'","repository":{"repoName":"'$2'"},"releaseName":"'$5'"}'
	echo $file_data > $file
	echo " FILE - ($file): $file_data" >> $HTTPLOG
	file2="./tmp/override.yaml"
	echo "" >> $file2
	file3="$TESTENV_TEMP_FILES/"$3"-"$chart_version".tgz"
    res="$(__execute_curl_to_helmmanger POST1_2 $query info $file values $file2 chart $file3)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: POST /helm/install - install chart
# args: <response-code> <chart> <version>
# (Function for test scripts)
helm_manager_api_post_install_chart() {
	__log_test_start $@

    if [ $# -ne 3 ]; then
		__print_err "<response-code> <chart> <version>" $@
		return 1
	fi

	query="/helm/install"
	file="./tmp/app-installation.json"
	chart_version=$3
	if [ $chart_version == "DEFAULT-VERSION" ]; then
		chart_version="0.1.0"
	fi
	file_data='{"name": "'$2'","version": "'$chart_version'"}'
	echo $file_data > $file
	echo " FILE - ($file): $file_data" >> $HTTPLOG
    res="$(__execute_curl_to_helmmanger POST $query $file)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE /helm/uninstall - uninstall chart
# args: <response-code> <chart> <version>
# (Function for test scripts)
helm_manager_api_uninstall_chart() {
	__log_test_start $@

    if [ $# -ne 3 ]; then
		__print_err "<response-code> <chart> <version> " $@
		return 1
	fi

	chart_version=$3
	if [ $chart_version == "DEFAULT-VERSION" ]; then
		chart_version="0.1.0"
	fi
	query="/helm/uninstall/$2/$chart_version"
    res="$(__execute_curl_to_helmmanger DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# API Test function: DELETE /helm/chart - delete chart
# args: <response-code> <chart> <version>
# (Function for test scripts)
helm_manager_api_delete_chart() {
	__log_test_start $@

    if [ $# -ne 3 ]; then
		__print_err "<response-code> <chart> <version> " $@
		return 1
	fi

	chart_version=$3
	if [ $chart_version == "DEFAULT-VERSION" ]; then
		chart_version="0.1.0"
	fi
	query="/helm/chart/$2/$chart_version"
    res="$(__execute_curl_to_helmmanger DELETE $query)"
    status=${res:${#res}-3}

	if [ $status -ne $1 ]; then
		__log_test_fail_status_code $1 $status
		return 1
	fi

	__log_test_pass
	return 0
}

# Config function: Add repo in helm manager by helm using exec
# args: <repo-name> <repo-url>
# (Function for test scripts)
helm_manager_api_exec_add_repo() {
	__log_conf_start $@

    if [ $# -ne 2 ]; then
		__print_err "<repo-name> <repo-url>" $@
		return 1
	fi

	if [ $RUNMODE == "DOCKER" ]; then
		retmsg=$(docker exec -it $HELM_MANAGER_APP_NAME helm repo add $1 $2)
		retcode=$?
		if [ $retcode -ne 0 ]; then
			__log_conf_fail_general " Cannot add repo to helm, return code: $retcode, msg: $retmsg"
			return 1
		fi
	else
		retmsg=$(kubectl $KUBECONF exec -it $HELM_MANAGER_APP_NAME -n $KUBE_NONRTRIC_NAMESPACE -- helm repo add $1 $2)
		retcode=$?
		if [ $retcode -ne 0 ]; then
			__log_conf_fail_general " Cannot add repo to helm, return code: $retcode, msg: $retmsg"
			return 1
		fi
	fi
	__log_conf_ok
	return 0
}

