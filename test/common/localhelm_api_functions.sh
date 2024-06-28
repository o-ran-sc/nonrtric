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

# This is a script that contains function to handle helm on localhost


################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__LOCALHELM_imagesetup() {
	:
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__LOCALHELM_imagepull() {
	:
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__LOCALHELM_imagebuild() {
	:
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__LOCALHELM_image_data() {
	:
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__LOCALHELM_kube_scale_zero() {
	:
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__LOCALHELM_kube_scale_zero_and_wait() {
	:
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__LOCALHELM_kube_delete_all() {
	:
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__LOCALHELM_store_docker_logs() {
	:
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__LOCALHELM_initial_setup() {
	:
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__LOCALHELM_statistics_setup() {
	:
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__LOCALHELM_test_requirements() {
	tmp=$(which helm)
	if [ $? -ne 0 ]; then
		echo $RED" Helm3 is required for running this test. Pls install helm3"
		exit 1
	fi
	tmp_version=$(helm version | grep 'v3')
	if [ -z "$tmp_version" ]; then
		echo $RED" Helm3 is required for running this test. Pls install helm3"
		exit 1
	fi
}

#######################################################


# Create a dummy helmchart
# arg: <chart-name>
localhelm_create_test_chart() {
	__log_conf_start $@
    if [ $# -ne 1 ]; then
        __print_err "<path-to-chart-dir>" $@
        return 1
    fi
	if [[ "$1" == *"/"* ]]; then
		echo -e $RED"Chart name cannot contain '/'"
		__log_conf_fail_general
		return 1
	fi
	helm create $TESTENV_TEMP_FILES/$1 | indent1
	if [ $? -ne 0 ]; then
		__log_conf_fail_general
		return 1
	fi
	__log_conf_ok
	return 0
}

# Package a created helmchart
# arg: <chart-name>
localhelm_package_test_chart() {
	__log_conf_start $@
    if [ $# -ne 1 ]; then
        __print_err "<path-to-chart-dir>" $@
        return 1
    fi
	if [[ "$1" == *"/"* ]]; then
		echo -e $RED"Chart name cannot contain '/'"
		__log_conf_fail_general
		return 1
	fi
	helm package -d $TESTENV_TEMP_FILES $TESTENV_TEMP_FILES/$1 | indent1
	if [ $? -ne 0 ]; then
		__log_conf_fail_general
		return 1
	fi
	__log_conf_ok
	return 0
}

# Check if a release is installed
# arg: INSTALLED|NOTINSTALLED <release-name> <name-space>
localhelm_installed_chart_release() {
	__log_test_start $@
    if [ $# -ne 3 ]; then
        __print_err "INSTALLED|NOTINSTALLED <release-name> <name-space>" $@
        return 1
    fi
	if [ $1 != "INSTALLED" ] && [ $1 != "NOTINSTALLED" ]; then
        __print_err "INSTALLED|NOTINSTALLED <release-name> <name-space>" $@
        return 1
	fi

	filter="helm ls -n $3 --filter ^$2"
	res=$($filter -q)
	if [ $? -ne 0 ]; then
		__log_test_fail_general "Failed to list helm releases"
		return 1
	fi
	if [ $1 == "INSTALLED" ]; then
		if [ "$res" != $2 ]; then
			echo -e "$RED Release $2 does not exists $ERED"
			__log_test_fail_general
			return 1
		fi
	elif [ $1 == "NOTINSTALLED" ]; then
		if [ "$res" == $2 ]; then
			__log_test_fail_general "Release $2 exists"
			return 1
		fi
	fi
	echo " Currently installed releases in namespace $3"
	helm ls -n $3  | indent2
	__log_test_pass
	return 0
}
