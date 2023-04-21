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

# This is a script that contains container/service management functions
# for PVCCLEANER

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__PVCCLEANER_imagesetup() {
	__check_and_create_image_var PVCCLEANER "PVC_CLEANER_IMAGE" "PVC_CLEANER_IMAGE_BASE" "PVC_CLEANER_IMAGE_TAG" REMOTE_PROXY "$PVC_CLEANER_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__PVCCLEANER_imagepull() {
	__check_and_pull_image $1 "$PVC_CLEANER_DISPLAY_NAME" $PVC_CLEANER_APP_NAME PVC_CLEANER_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__PVCCLEANER_imagebuild() {
	echo -e $RED"Image for app PVCCLEANER shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__PVCCLEANER_image_data() {
	echo -e "$PVC_CLEANER_DISPLAY_NAME\t$(docker images --format $1 $PVC_CLEANER_IMAGE)" >>   $2
	if [ ! -z "$PVC_CLEANER_IMAGE_SOURCE" ]; then
		echo -e "-- source image --\t$(docker images --format $1 $PVC_CLEANER_IMAGE_SOURCE)" >>   $2
	fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__PVCCLEANER_kube_scale_zero() {
	:
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__PVCCLEANER_kube_scale_zero_and_wait() {
	:
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__PVCCLEANER_kube_delete_all() {
	:
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__PVCCLEANER_store_docker_logs() {
	if [ $RUNMODE == "KUBE" ]; then
		kubectl $KUBECONF  logs -l "autotest=PVCCLEANER" -A --tail=-1 > $1$2_pvs_cleaner.log 2>&1
	fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__PVCCLEANER_initial_setup() {
	:
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__PVCCLEANER_statistics_setup() {
	echo ""
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__PVCCLEANER_test_requirements() {
	:
}

#######################################################

# This is a system app, all usage in testcase_common.sh