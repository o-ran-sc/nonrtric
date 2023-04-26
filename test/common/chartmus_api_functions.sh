#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021-2023 Nordix Foundation. All rights reserved.
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

# This is a script that contains container/service management functions and test functions for Chartmuseum


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CHARTMUS_imagesetup() {
	__check_and_create_image_var CHARTMUS "CHART_MUS_IMAGE" "CHART_MUS_IMAGE_BASE" "CHART_MUS_IMAGE_TAG" REMOTE_OTHER "$CHART_MUS_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__CHARTMUS_imagepull() {
	__check_and_pull_image $2 "$CHART_MUS_DISPLAY_NAME" $CHART_MUS_APP_NAME CHART_MUS_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__CHARTMUS_imagebuild() {
	echo -e $RED" Image for app CHARTMUS shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__CHARTMUS_image_data() {
	echo -e "$CHART_MUS_DISPLAY_NAME\t$(docker images --format $1 $CHART_MUS_IMAGE)" >>   $2
	if [ ! -z "$CHART_MUS_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $CHART_MUS_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__CHARTMUS_kube_scale_zero() {
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest CHARTMUS
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__CHARTMUS_kube_scale_zero_and_wait() {
	echo -e $RED" CHARTMUS app is not scaled in this state"$ERED
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__CHARTMUS_kube_delete_all() {
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest CHARTMUS
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__CHARTMUS_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=CHARTMUS" -n $KUBE_SIM_NAMESPACE --tail=-1 > $1$2_chartmuseum.log 2>&1
	else
		docker logs $CHART_MUS_APP_NAME > $1$2_chartmuseum.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__CHARTMUS_initial_setup() {
	use_chart_mus_http
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__CHARTMUS_statistics_setup() {
	if [ $RUNMODE == "KUBE" ]; then
		echo "CHARTMUS $CHART_MUS_APP_NAME $KUBE_SIM_NAMESPACE"
	else
		echo "CHARTMUS $CHART_MUS_APP_NAME"
	fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__CHARTMUS_test_requirements() {
	:
}

#######################################################

# Set http as the protocol to use for all communication to the Chartmuseum
# args: -
# (Function for test scripts)
use_chart_mus_http() {
	__chart_mus_set_protocoll "http" $CHART_MUS_INTERNAL_PORT $CHART_MUS_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the Chartmuseum
# args: -
# (Function for test scripts)
use_chart_mus_https() {
	__chart_mus_set_protocoll "https" $CHART_MUS_INTERNAL_SECURE_PORT $CHART_MUS_EXTERNAL_SECURE_PORT
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__chart_mus_set_protocoll() {
	echo -e $BOLD"$CHART_MUS_DISPLAY_NAME protocol setting"$EBOLD
	echo -e " Using $BOLD $1 $EBOLD towards $CHART_MUS_DISPLAY_NAME"

	## Access to Chartmuseum

	CHART_MUS_SERVICE_PATH=$1"://"$CHART_MUS_APP_NAME":"$2  # docker access, container->container and script->container via proxy
	CHART_MUS_SERVICE_PORT=$2
	CHART_MUS_SERVICE_HOST=$CHART_MUS_APP_NAME
	if [ $RUNMODE == "KUBE" ]; then
		CHART_MUS_SERVICE_PATH=$1"://"$CHART_MUS_APP_NAME.$KUBE_SIM_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
		CHART_MUS_SERVICE_PORT=$3
		CHART_MUS_SERVICE_HOST=$CHART_MUS_APP_NAME.$KUBE_SIM_NAMESPACE
	fi
	CHART_MUS_SERVICE_HTTPX=$1

	echo ""
}

### Admin API functions Chartmuseum

###########################
### Chartmuseum functions
###########################

# Export env vars for config files, docker compose and kube resources
# args:
__chartmuseum_export_vars() {
	export CHART_MUS_APP_NAME
	export CHART_MUS_DISPLAY_NAME

	export DOCKER_SIM_NWNAME
	export KUBE_SIM_NAMESPACE

	export CHART_MUS_IMAGE
	export CHART_MUS_INTERNAL_PORT
	export CHART_MUS_EXTERNAL_PORT

	export CHART_MUS_CHART_CONTR_CHARTS

}


# Start the Chartmuseum in the simulator group
# args: -
# (Function for test scripts)
start_chart_museum() {

	echo -e $BOLD"Starting $CHART_MUS_DISPLAY_NAME"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then

		# Check if app shall be fully managed by the test script
		__check_included_image "CHARTMUS"
		retcode_i=$?

		# Check if app shall only be used by the test script
		__check_prestarted_image "CHARTMUS"
		retcode_p=$?

		if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
			echo -e $RED"The $CHART_MUS_NAME app is not included as managed nor prestarted in this test script"$ERED
			echo -e $RED"The $CHART_MUS_APP_NAME will not be started"$ERED
			exit
		fi
		if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
			echo -e $RED"The $CHART_MUS_APP_NAME app is included both as managed and prestarted in this test script"$ERED
			echo -e $RED"The $CHART_MUS_APP_NAME will not be started"$ERED
			exit
		fi

		if [ $retcode_p -eq 0 ]; then
			echo -e " Using existing $CHART_MUS_APP_NAME deployment and service"
			echo " Setting CHARTMUS replicas=1"
			__kube_scale deployment $CHART_MUS_APP_NAME $KUBE_SIM_NAMESPACE 1
		fi

		if [ $retcode_i -eq 0 ]; then
			echo -e " Creating $CHART_MUS_APP_NAME deployment and service"

            __kube_create_namespace $KUBE_SIM_NAMESPACE

			__chartmuseum_export_vars

			# Create service
			input_yaml=$SIM_GROUP"/"$CHART_MUS_COMPOSE_DIR"/"svc.yaml
			output_yaml=$PWD/tmp/chartmus_svc.yaml
			__kube_create_instance service $CHART_MUS_APP_NAME $input_yaml $output_yaml

			# Create app
			input_yaml=$SIM_GROUP"/"$CHART_MUS_COMPOSE_DIR"/"app.yaml
			output_yaml=$PWD/tmp/chartmus_app.yaml
			__kube_create_instance app $CHART_MUS_APP_NAME $input_yaml $output_yaml
		fi

		__check_service_start $CHART_MUS_APP_NAME $CHART_MUS_SERVICE_PATH$CHART_MUS_ALIVE_URL
	else

		# Check if docker app shall be fully managed by the test script
		__check_included_image 'CHARTMUS'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Chartmuseum app is not included as managed in this test script"$ERED
			echo -e $RED"The Chartmuseum will not be started"$ERED
			exit
		fi

		__chartmuseum_export_vars

		__start_container $CHART_MUS_COMPOSE_DIR "" NODOCKERARGS 1 $CHART_MUS_APP_NAME

        __check_service_start $CHART_MUS_APP_NAME $CHART_MUS_SERVICE_PATH$CHART_MUS_ALIVE_URL
	fi
    echo ""
    return 0
}

# Execute a curl cmd towards the chartmuseum simulator and check the response code.
# args: TEST|CONF <expected-response-code> <curl-cmd-string>
__execute_curl_to_chartmuseum() {
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
	echo " CMD: $3 -skw %{http_code} $proxyflag" >> $HTTPLOG
	res=$($3 -skw %{http_code} $proxyflag)
	echo " RESP: $res" >> $HTTPLOG
	retcode=$?
    if [ $retcode -ne 0 ]; then
        __log_conf_fail_general " Fatal error when executing curl, response: "$retcode
        return 1
    fi
    status=${res:${#res}-3}
    if [ $status -eq $2 ]; then
        if [ $1 == "TEST" ]; then
            __log_test_pass
        else
            __log_conf_ok
        fi
        return 0
    fi
    if [ $1 == "TEST" ]; then
        __log_test_fail_status_code $2 $status
        else
        __log_conf_fail_status_code $2 $status
    fi
    return 1
}

# upload helmchart
# arg: <chart-name>
chartmus_upload_test_chart() {
	__log_conf_start $@
    if [ $# -ne 1 ]; then
        __print_err "<chart-name>" $@
        return 1
    fi
	chart_path=$TESTENV_TEMP_FILES/$1"-0.1.0.tgz"
	if [ ! -f "$chart_path" ]; then
		echo -e $RED" Cannot find package chart: $chart_path"$ERED
		__log_conf_fail_general
		return 1
	fi
	__execute_curl_to_chartmuseum CONF 201 "curl --data-binary @$chart_path $CHART_MUS_SERVICE_PATH/api/charts"
}

# delete helmchart
# arg: <chart-name> [<version>]
chartmus_delete_test_chart() {
	__log_conf_start $@
    if [ $# -gt 2 ]; then
        __print_err "<chart-name> [<version>]" $@
        return 1
    fi
	if [ $# -eq 1 ]; then
		chart_path="/$1/0.1.0"
	else
		chart_path="/$1/$2"
	fi
	__execute_curl_to_chartmuseum CONF 200 "curl -X DELETE $CHART_MUS_SERVICE_PATH/api/charts"$chart_path
}