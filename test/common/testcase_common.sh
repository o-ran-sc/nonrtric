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

# This is a script that contains all the common functions needed for auto test.
# Specific test function are defined in scripts  XXXX_functions.sh

. ../common/api_curl.sh

# List of short names for all supported apps, including simulators etc
APP_SHORT_NAMES="PA RICSIM SDNC CP ECS RC CBS CONSUL RC MR DMAAPMR CR PRODSTUB"

__print_args() {
	echo "Args: remote|remote-remove docker|kube --env-file <environment-filename> [release] [auto-clean] [--stop-at-error] "
	echo "      [--ricsim-prefix <prefix> ] [--use-local-image <app-nam>+]  [--use-snapshot-image <app-nam>+]"
	echo "      [--use-staging-image <app-nam>+] [--use-release-image <app-nam>+]"
}

if [ $# -eq 1 ] && [ "$1" == "help" ]; then

	if [ ! -z "$TC_ONELINE_DESCR" ]; then
		echo "Test script description:"
		echo $TC_ONELINE_DESCR
		echo ""
	fi
	__print_args
	echo ""
	echo "remote                -  Use images from remote repositories. Can be overridden for individual images using the '--use_xxx' flags"
	echo "remote-remove         -  Same as 'remote' but will also try to pull fresh images from remote repositories"
	echo "docker                -  Test executed in docker environment"
	echo "kube                  -  Test executed in kubernetes environment - requires an already started kubernetes environment"
	echo "--env-file            -  The script will use the supplied file to read environment variables from"
	echo "release               -  If this flag is given the script will use release version of the images"
	echo "auto-clean            -  If the function 'auto_clean_containers' is present in the end of the test script then all containers will be stopped and removed. If 'auto-clean' is not given then the function has no effect."
    echo "--stop-at-error       -  The script will stop when the first failed test or configuration"
	echo "--ricsim-prefix       -  The a1 simulator will use the supplied string as container prefix instead of 'ricsim'"
	echo "--use-local-image     -  The script will use local images for the supplied apps, space separated list of app short names"
	echo "--use-snapshot-image  -  The script will use images from the nexus snapshot repo for the supplied apps, space separated list of app short names"
	echo "--use-staging-image   -  The script will use images from the nexus staging repo for the supplied apps, space separated list of app short names"
	echo "--use-release-image   -  The script will use images from the nexus release repo for the supplied apps, space separated list of app short names"
	echo ""
	echo "List of app short names supported: "$APP_SHORT_NAMES
	exit 0
fi

# Create a test case id, ATC (Auto Test Case), from the name of the test case script.
# FTC1.sh -> ATC == FTC1
ATC=$(basename "${BASH_SOURCE[$i+1]}" .sh)

#Create result file (containing '1' for error) for this test case
#Will be replaced with a file containing '0' if all test cases pass
echo "1" > "$PWD/.result$ATC.txt"

#Formatting for 'echo' cmd
BOLD="\033[1m"
EBOLD="\033[0m"
RED="\033[31m\033[1m"
ERED="\033[0m"
GREEN="\033[32m\033[1m"
EGREEN="\033[0m"
YELLOW="\033[33m\033[1m"
EYELLOW="\033[0m"
SAMELINE="\033[0K\r"

# Just resetting any previous echo formatting...
echo -ne $EBOLD

# default test environment variables
TEST_ENV_VAR_FILE=""

echo "Test case started as: ${BASH_SOURCE[$i+1]} "$@

#Localhost constants
LOCALHOST_NAME="localhost"
LOCALHOST_HTTP="http://localhost"
LOCALHOST_HTTPS="https://localhost"

# Var to hold 'auto' in case containers shall be stopped when test case ends
AUTO_CLEAN=""

# Var to hold the app names to use local images for
USE_LOCAL_IMAGES=""

# Var to hold the app names to use remote snapshot images for
USE_SNAPSHOT_IMAGES=""

# Var to hold the app names to use remote staging images for
USE_STAGING_IMAGES=""

# Var to hold the app names to use remote release images for
USE_RELEASE_IMAGES=""

# List of available apps to override with local or remote staging/snapshot/release image
AVAILABLE_IMAGES_OVERRIDE="PA ECS CP SDNC RICSIM RC"

# Use this var (STOP_AT_ERROR=1 in the test script) for debugging/trouble shooting to take all logs and exit at first FAIL test case
STOP_AT_ERROR=0

# The default value "DEV" indicate that development image tags (SNAPSHOT) and nexus repos (nexus port 10002) are used.
# The value "RELEASE" indicate that relase image tag and nexus repos (nexus port) are used
# Applies only to images defined in the test-env files with image names and tags defined as XXXX_RELEASE
IMAGE_CATEGORY="DEV"

# Function to indent cmd output with one space
indent1() { sed 's/^/ /'; }

# Function to indent cmd output with two spaces
indent2() { sed 's/^/  /'; }

# Set a description string for the test case
if [ -z "$TC_ONELINE_DESCR" ]; then
	TC_ONELINE_DESCR="<no-description>"
	echo "No test case description found, TC_ONELINE_DESCR should be set on in the test script , using "$TC_ONELINE_DESCR
fi

# Counter for test suites
if [ -f .tmp_tcsuite_ctr ]; then
	tmpval=$(< .tmp_tcsuite_ctr)
	((tmpval++))
	echo $tmpval > .tmp_tcsuite_ctr
fi

# Create the logs dir if not already created in the current dir
if [ ! -d "logs" ]; then
    mkdir logs
fi
TESTLOGS=$PWD/logs

# Create the tmp dir for temporary files that is not needed after the test
# hidden files for the test env is still stored in the current dir
if [ ! -d "tmp" ]; then
    mkdir tmp
fi

# Create a http message log for this testcase
HTTPLOG=$PWD"/.httplog_"$ATC".txt"
echo "" > $HTTPLOG

# Create a log dir for the test case
mkdir -p $TESTLOGS/$ATC

# Save create for current logs
mkdir -p $TESTLOGS/$ATC/previous

rm $TESTLOGS/$ATC/previous/*.log &> /dev/null
rm $TESTLOGS/$ATC/previous/*.txt &> /dev/null
rm $TESTLOGS/$ATC/previous/*.json &> /dev/null

mv  $TESTLOGS/$ATC/*.log $TESTLOGS/$ATC/previous &> /dev/null
mv  $TESTLOGS/$ATC/*.txt $TESTLOGS/$ATC/previous &> /dev/null
mv  $TESTLOGS/$ATC/*.txt $TESTLOGS/$ATC/previous &> /dev/null

# Clear the log dir for the test case
rm $TESTLOGS/$ATC/*.log &> /dev/null
rm $TESTLOGS/$ATC/*.txt &> /dev/null
rm $TESTLOGS/$ATC/*.json &> /dev/null

# Log all output from the test case to a TC log
TCLOG=$TESTLOGS/$ATC/TC.log
exec &>  >(tee ${TCLOG})

#Variables for counting tests as well as passed and failed tests
RES_TEST=0
RES_PASS=0
RES_FAIL=0
RES_CONF_FAIL=0
RES_DEVIATION=0

#File to keep deviation messages
DEVIATION_FILE=".tmp_deviations"
rm $DEVIATION_FILE &> /dev/null

# Trap "command not found" and make the script fail
trap_fnc() {

	if [ $? -eq 127 ]; then
		echo -e $RED"Function not found, setting script to FAIL"$ERED
		((RES_CONF_FAIL++))
	fi
}
trap trap_fnc ERR

# Counter for tests
TEST_SEQUENCE_NR=1

# Function to log the start of a test case
__log_test_start() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
	echo -e $BOLD"TEST $TEST_SEQUENCE_NR (${BASH_LINENO[1]}): ${FUNCNAME[1]}" $@ $EBOLD
    echo "TEST $TEST_SEQUENCE_NR - ${TIMESTAMP}: (${BASH_LINENO[1]}): ${FUNCNAME[1]}" $@ >> $HTTPLOG
	((RES_TEST++))
	((TEST_SEQUENCE_NR++))
}

# General function to log a failed test case
__log_test_fail_general() {
	echo -e $RED" FAIL."$1 $ERED
	((RES_FAIL++))
	__check_stop_at_error
}

# Function to log a test case failed due to incorrect response code
__log_test_fail_status_code() {
	echo -e $RED" FAIL. Exepected status "$1", got "$2 $3 $ERED
	((RES_FAIL++))
	__check_stop_at_error
}

# Function to log a test case failed due to incorrect response body
__log_test_fail_body() {
	echo -e $RED" FAIL, returned body not correct"$ERED
	((RES_FAIL++))
	__check_stop_at_error
}

# Function to log a test case that is not supported
__log_test_fail_not_supported() {
	echo -e $RED" FAIL, function not supported"$ERED
	((RES_FAIL++))
	__check_stop_at_error
}

# General function to log a passed test case
__log_test_pass() {
	if [ $# -gt 0 ]; then
		echo $@
	fi
	((RES_PASS++))
	echo -e $GREEN" PASS"$EGREEN
}

#Counter for configurations
CONF_SEQUENCE_NR=1

# Function to log the start of a configuration setup
__log_conf_start() {
	TIMESTAMP=$(date "+%Y-%m-%d %H:%M:%S")
	echo -e $BOLD"CONF $CONF_SEQUENCE_NR (${BASH_LINENO[1]}): "${FUNCNAME[1]} $@ $EBOLD
	echo "CONF $CONF_SEQUENCE_NR - ${TIMESTAMP}: (${BASH_LINENO[1]}): "${FUNCNAME[1]} $@  >> $HTTPLOG
	((CONF_SEQUENCE_NR++))
}

# Function to log a failed configuration setup
__log_conf_fail_general() {
	echo -e $RED" FAIL."$1 $ERED
	((RES_CONF_FAIL++))
	__check_stop_at_error
}

# Function to log a failed configuration setup due to incorrect response code
__log_conf_fail_status_code() {
	echo -e $RED" FAIL. Exepected status "$1", got "$2 $3 $ERED
	((RES_CONF_FAIL++))
	__check_stop_at_error
}

# Function to log a failed configuration setup due to incorrect response body
__log_conf_fail_body() {
	echo -e $RED" FAIL, returned body not correct"$ERED
	((RES_CONF_FAIL++))
	__check_stop_at_error
}

# Function to log a passed configuration setup
__log_conf_ok() {
	if [ $# -gt 0 ]; then
		echo $@
	fi
	echo -e $GREEN" OK"$EGREEN
}

#Var for measuring execution time
TCTEST_START=$SECONDS

#File to save timer measurement results
TIMER_MEASUREMENTS=".timer_measurement.txt"
echo -e "Activity \t Duration" > $TIMER_MEASUREMENTS


echo "-------------------------------------------------------------------------------------------------"
echo "-----------------------------------      Test case: "$ATC
echo "-----------------------------------      Started:   "$(date)
echo "-------------------------------------------------------------------------------------------------"
echo "-- Description: "$TC_ONELINE_DESCR
echo "-------------------------------------------------------------------------------------------------"
echo "-----------------------------------      Test case setup      -----------------------------------"

START_ARG=$1
paramerror=0
paramerror_str=""
if [ $# -lt 1 ]; then
	paramerror=1
fi
if [ $paramerror -eq 0 ]; then
	if [ "$1" != "remote" ] && [ "$1" != "remote-remove" ]; then
		paramerror=1
		if [ -z "$paramerror_str" ]; then
			paramerror_str="First arg shall be 'remote' or 'remote-remove'"
		fi
	else
		shift;
	fi
fi
if [ $paramerror -eq 0 ]; then
	if [ "$1" != "docker" ] && [ "$1" != "kube" ]; then
		paramerror=1
		if [ -z "$paramerror_str" ]; then
			paramerror_str="Second arg shall be 'docker' or 'kube'"
		fi
	else
		if [ $1 == "docker" ]; then
			RUNMODE="DOCKER"
			echo "Setting RUNMODE=DOCKER"
		fi
		if [ $1 == "kube" ]; then
			RUNMODE="KUBE"
			echo "Setting RUNMODE=KUBE"
		fi
		shift;
	fi
fi
foundparm=0
while [ $paramerror -eq 0 ] && [ $foundparm -eq 0 ]; do
	foundparm=1
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "release" ]; then
			IMAGE_CATEGORY="RELEASE"
			echo "Option set - Release image tags used for applicable images "
			shift;
			foundparm=0
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "auto-clean" ]; then
			AUTO_CLEAN="auto"
			echo "Option set - Auto clean at end of test script"
			shift;
			foundparm=0
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--stop-at-error" ]; then
			STOP_AT_ERROR=1
			echo "Option set - Stop at first error"
			shift;
			foundparm=0
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--ricsim-prefix" ]; then
			shift;
			TMP_RIC_SIM_PREFIX=$1  #RIC_SIM_PREFIX need to be updated after sourcing of the env file
			if [ -z "$1" ]; then
				paramerror=1
				if [ -z "$paramerror_str" ]; then
					paramerror_str="No prefix found for flag: '--ricsim-prefix'"
				fi
			else
				echo "Option set - Overriding RIC_SIM_PREFIX with: "$1
				shift;
				foundparm=0
			fi
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--env-file" ]; then
			shift;
			TEST_ENV_VAR_FILE=$1
			if [ -z "$1" ]; then
				paramerror=1
				if [ -z "$paramerror_str" ]; then
					paramerror_str="No env file found for flag: '--env-file'"
				fi
			else
				echo "Option set - Reading test env from: "$1
				shift;
				foundparm=0
			fi
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--use-local-image" ]; then
			USE_LOCAL_IMAGES=""
			shift
			while [ $# -gt 0 ] && [[ "$1" != "--"* ]]; do
				USE_LOCAL_IMAGES=$USE_LOCAL_IMAGES" "$1
				if [[ "$AVAILABLE_IMAGES_OVERRIDE" != *"$1"* ]]; then
					paramerror=1
					if [ -z "$paramerror_str" ]; then
						paramerror_str="App name $1 is not available for local override for flag: '--use-local-image'"
					fi
				fi
				shift;
			done
			foundparm=0
			if [ -z "$USE_LOCAL_IMAGES" ]; then
				paramerror=1
				if [ -z "$paramerror_str" ]; then
					paramerror_str="No app name found for flag: '--use-local-image'"
				fi
			else
				echo "Option set - Overriding with local images for app(s):"$USE_LOCAL_IMAGES
			fi
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--use-snapshot-image" ]; then
			USE_SNAPSHOT_IMAGES=""
			shift
			while [ $# -gt 0 ] && [[ "$1" != "--"* ]]; do
				USE_SNAPSHOT_IMAGES=$USE_SNAPSHOT_IMAGES" "$1
				if [[ "$AVAILABLE_IMAGES_OVERRIDE" != *"$1"* ]]; then
					paramerror=1
					if [ -z "$paramerror_str" ]; then
						paramerror_str="App name $1 is not available for snapshot override for flag: '--use-snapshot-image'"
					fi
				fi
				shift;
			done
			foundparm=0
			if [ -z "$USE_SNAPSHOT_IMAGES" ]; then
				paramerror=1
				if [ -z "$paramerror_str" ]; then
					paramerror_str="No app name found for flag: '--use-snapshot-image'"
				fi
			else
				echo "Option set - Overriding with snapshot images for app(s):"$USE_SNAPSHOT_IMAGES
			fi
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--use-staging-image" ]; then
			USE_STAGING_IMAGES=""
			shift
			while [ $# -gt 0 ] && [[ "$1" != "--"* ]]; do
				USE_STAGING_IMAGES=$USE_STAGING_IMAGES" "$1
				if [[ "$AVAILABLE_IMAGES_OVERRIDE" != *"$1"* ]]; then
					paramerror=1
					if [ -z "$paramerror_str" ]; then
						paramerror_str="App name $1 is not available for staging override for flag: '--use-staging-image'"
					fi
				fi
				shift;
			done
			foundparm=0
			if [ -z "$USE_STAGING_IMAGES" ]; then
				paramerror=1
				if [ -z "$paramerror_str" ]; then
					paramerror_str="No app name found for flag: '--use-staging-image'"
				fi
			else
				echo "Option set - Overriding with staging images for app(s):"$USE_STAGING_IMAGES
			fi
		fi
	fi
	if [ $paramerror -eq 0 ]; then
		if [ "$1" == "--use-release-image" ]; then
			USE_RELEASE_IMAGES=""
			shift
			while [ $# -gt 0 ] && [[ "$1" != "--"* ]]; do
				USE_RELEASE_IMAGES=$USE_RELEASE_IMAGES" "$1
				if [[ "$AVAILABLE_IMAGES_OVERRIDE" != *"$1"* ]]; then
					paramerror=1
					if [ -z "$paramerror_str" ]; then
						paramerror_str="App name $1 is not available for release override for flag: '--use-release-image'"
					fi
				fi
				shift;
			done
			foundparm=0
			if [ -z "$USE_RELEASE_IMAGES" ]; then
				paramerror=1
				if [ -z "$paramerror_str" ]; then
					paramerror_str="No app name found for flag: '--use-release-image'"
				fi
			else
				echo "Option set - Overriding with release images for app(s):"$USE_RELEASE_IMAGES
			fi
		fi
	fi
done
echo ""

#Still params left?
if [ $paramerror -eq 0 ] && [ $# -gt 0 ]; then
	paramerror=1
	if [ -z "$paramerror_str" ]; then
		paramerror_str="Unknown parameter(s): "$@
	fi
fi

if [ $paramerror -eq 1 ]; then
	echo -e $RED"Incorrect arg list: "$paramerror_str$ERED
	__print_args
	exit 1
fi

# sourcing the selected env variables for the test case
if [ -f "$TEST_ENV_VAR_FILE" ]; then
	echo -e $BOLD"Sourcing env vars from: "$TEST_ENV_VAR_FILE$EBOLD
	. $TEST_ENV_VAR_FILE

	if [ -z "$TEST_ENV_PROFILE" ] || [ -z "$SUPPORTED_PROFILES" ]; then
		echo -e $YELLOW"This test case may not work with selected test env file. TEST_ENV_PROFILE is missing in test_env file or SUPPORTED_PROFILES is missing in test case file"$EYELLOW
	else
		found_profile=0
		for prof in $SUPPORTED_PROFILES; do
			if [ "$TEST_ENV_PROFILE" == "$prof" ]; then
				echo -e $GREEN"Test case supports the selected test env file"$EGREEN
				found_profile=1
			fi
		done
		if [ $found_profile -ne 1 ]; then
			echo -e $RED"Test case does not support the selected test env file"$ERED
			echo "Profile: "$TEST_ENV_PROFILE"     Supported profiles: "$SUPPORTED_PROFILES
			echo -e $RED"Exiting...."$ERED
			exit 1
		fi
	fi
else
	echo -e $RED"Selected env var file does not exist: "$TEST_ENV_VAR_FILE$ERED
	echo " Select one of following env var file matching the intended target of the test"
	echo " Restart the test using the flag '--env-file <path-to-env-file>"
	ls ../common/test_env* | indent1
	exit 1
fi

#This var need be preserved from the command line option, if set, when env var is sourced.
if [ ! -z "$TMP_RIC_SIM_PREFIX" ]; then
	RIC_SIM_PREFIX=$TMP_RIC_SIM_PREFIX
fi

if [ -z "$PROJECT_IMAGES_APP_NAMES" ]; then
	echo -e $RED"Var PROJECT_IMAGES_APP_NAMES must be defined in: "$TEST_ENV_VAR_FILE $ERED
	exit 1
fi

if [[ $SUPPORTED_RUNMODES != *"$RUNMODE"* ]]; then
	echo -e $RED"This test script does not support RUNMODE $RUNMODE"$ERED
	echo "Supported RUNMODEs: "$SUPPORTED_RUNMODES
	exit 1
fi

# Choose list of included apps depending on run-mode
if [ $RUNMODE == "KUBE" ]; then
	INCLUDED_IMAGES=$KUBE_INCLUDED_IMAGES
else
	INCLUDED_IMAGES=$DOCKER_INCLUDED_IMAGES
fi

# Check needed installed sw
tmp=$(which python3)
if [ $? -ne 0 ] || [ -z tmp ]; then
	echo -e $RED"python3 is required to run the test environment, pls install"$ERED
	exit 1
fi
tmp=$(which docker)
if [ $? -ne 0 ] || [ -z tmp ]; then
	echo -e $RED"docker is required to run the test environment, pls install"$ERED
	exit 1
fi

tmp=$(which docker-compose)
if [ $? -ne 0 ] || [ -z tmp ]; then
	if [ $RUNMODE == "DOCKER" ]; then
		echo -e $RED"docker-compose is required to run the test environment, pls install"$ERED
		exit 1
	fi
fi

tmp=$(which kubectl)
if [ $? -ne 0 ] || [ -z tmp ]; then
	if [ $RUNMODE == "KUBE" ]; then
		echo -e $RED"kubectl is required to run the test environment in kubernetes mode, pls install"$ERED
		exit 1
	fi
fi

echo -e $BOLD"Checking configured image setting for this test case"$EBOLD

#Temp var to check for image variable name errors
IMAGE_ERR=0
#Create a file with image info for later printing as a table
image_list_file="./tmp/.image-list"
echo -e " Container\tImage\ttag\ttag-switch" > $image_list_file

# Check if image env var is set and if so export the env var with image to use (used by docker compose files)
# arg: <image name> <target-variable-name> <image-variable-name> <image-tag-variable-name> <tag-suffix> <app-short-name>
__check_and_create_image_var() {
	if [ $# -ne 6 ]; then
		echo "Expected arg: <image name> <target-variable-name> <image-variable-name> <image-tag-variable-name> <tag-suffix> <app-short-name>"
		((IMAGE_ERR++))
		return
	fi
	__check_included_image $6
	if [ $? -ne 0 ]; then
		echo -e "$1\t<image-excluded>\t<no-tag>"  >> $image_list_file
		# Image is excluded since the corresponding app is not used in this test
		return
	fi
	tmp=${1}"\t"
	#Create var from the input var names
	image="${!3}"
	tmptag=$4"_"$5
	tag="${!tmptag}"

	if [ -z $image ]; then
		echo -e $RED"\$"$3" not set in $TEST_ENV_VAR_FILE"$ERED
		((IMAGE_ERR++))
		echo ""
		tmp=$tmp"<no-image>\t"
	else
		#Add repo depending on image type
		if [ "$5" == "REMOTE_RELEASE" ]; then
			image=$NEXUS_RELEASE_REPO$image
		fi
		if [ "$5" == "REMOTE" ]; then
			image=$NEXUS_STAGING_REPO$image
		fi
		if [ "$5" == "REMOTE_SNAPSHOT" ]; then
			image=$NEXUS_SNAPSHOT_REPO$image
		fi
		if [ "$5" == "REMOTE_PROXY" ]; then
			image=$NEXUS_PROXY_REPO$image
		fi
		if [ "$5" == "REMOTE_RELEASE_ONAP" ]; then
			image=$NEXUS_RELEASE_REPO_ONAP$image
		fi
		if [ "$5" == "REMOTE_RELEASE_ORAN" ]; then
			image=$NEXUS_RELEASE_REPO_ORAN$image
		fi
		#No nexus repo added for local images, tag: LOCAL
		tmp=$tmp$image"\t"
	fi
	if [ -z $tag ]; then
		echo -e $RED"\$"$tmptag" not set in $TEST_ENV_VAR_FILE"$ERED
		((IMAGE_ERR++))
		echo ""
		tmp=$tmp"<no-tag>\t"
	else
		tmp=$tmp$tag
	fi
	tmp=$tmp"\t"$5
	echo -e "$tmp" >> $image_list_file
	#Export the env var
	export "${2}"=$image":"$tag
}

# Check if app uses image included in this test run
# Returns 0 if image is included, 1 if not
__check_included_image() {
	for im in $INCLUDED_IMAGES; do
		if [ "$1" == "$im" ]; then
			return 0
		fi
	done
	return 1
}

# Check if app is included in the prestarted set of apps
# Returns 0 if image is included, 1 if not
__check_prestarted_image() {
	for im in $KUBE_PRESTARTED_IMAGES; do
		if [ "$1" == "$im" ]; then
			return 0
		fi
	done
	return 1
}

# Check if an app shall use a local image, based on the cmd parameters
__check_image_local_override() {
	for im in $USE_LOCAL_IMAGES; do
		if [ "$1" == "$im" ]; then
			return 1
		fi
	done
	return 0
}

# Check if app uses image override
# Returns the image/tag suffix LOCAL for local image or REMOTE/REMOTE_RELEASE/REMOTE_SNAPSHOT for staging/release/snapshot image
__check_image_override() {

	for im in $ORAN_IMAGES_APP_NAMES; do
		if [ "$1" == "$im" ]; then
			echo "REMOTE_RELEASE_ORAN"
			return 0
		fi
	done

	for im in $ONAP_IMAGES_APP_NAMES; do
		if [ "$1" == "$im" ]; then
			echo "REMOTE_RELEASE_ONAP"
			return 0
		fi
	done

	found=0
	for im in $PROJECT_IMAGES_APP_NAMES; do
		if [ "$1" == "$im" ]; then
			found=1
		fi
	done

	if [ $found -eq 0 ]; then
		echo "REMOTE_PROXY"
		return 0
	fi

	suffix=""
	if [ $IMAGE_CATEGORY == "RELEASE" ]; then
		suffix="REMOTE_RELEASE"
	fi
	if [ $IMAGE_CATEGORY == "DEV" ]; then
		suffix="REMOTE"
	fi
	CTR=0
	for im in $USE_STAGING_IMAGES; do
		if [ "$1" == "$im" ]; then
			suffix="REMOTE"
			((CTR++))
		fi
	done
	for im in $USE_RELEASE_IMAGES; do
		if [ "$1" == "$im" ]; then
			suffix="REMOTE_RELEASE"
			((CTR++))
		fi
	done
	for im in $USE_SNAPSHOT_IMAGES; do
		if [ "$1" == "$im" ]; then
			suffix="REMOTE_SNAPSHOT"
			((CTR++))
		fi
	done
	for im in $USE_LOCAL_IMAGES; do
		if [ "$1" == "$im" ]; then
			suffix="LOCAL"
			((CTR++))
		fi
	done
	echo $suffix
	if [ $CTR -gt 1 ]; then
		exit 1
	fi
	return 0
}

# Check that image env setting are available
echo ""

#Agent image
__check_included_image 'PA'
	if [ $? -eq 0 ]; then
	IMAGE_SUFFIX=$(__check_image_override 'PA')
	if [ $? -ne 0 ]; then
		echo -e $RED"Image setting from cmd line not consistent for PA."$ERED
		((IMAGE_ERR++))
	fi
	__check_and_create_image_var " Policy Agent" "POLICY_AGENT_IMAGE" "POLICY_AGENT_IMAGE_BASE" "POLICY_AGENT_IMAGE_TAG" $IMAGE_SUFFIX PA
fi

#Remote Control Panel image
__check_included_image 'CP'
if [ $? -eq 0 ]; then
	IMAGE_SUFFIX=$(__check_image_override 'CP')
	if [ $? -ne 0 ]; then
		echo -e $RED"Image setting from cmd line not consistent for CP."$ERED
		((IMAGE_ERR++))
	fi
	__check_and_create_image_var " Control Panel" "CONTROL_PANEL_IMAGE" "CONTROL_PANEL_IMAGE_BASE" "CONTROL_PANEL_IMAGE_TAG" $IMAGE_SUFFIX CP
fi

#Remote SDNC image
__check_included_image 'SDNC'
if [ $? -eq 0 ]; then
	IMAGE_SUFFIX=$(__check_image_override 'SDNC')
	if [ $? -ne 0 ]; then
		echo -e $RED"Image setting from cmd line not consistent for SDNC."$ERED
		((IMAGE_ERR++))
	fi
	__check_and_create_image_var " SDNC A1 Controller" "SDNC_A1_CONTROLLER_IMAGE" "SDNC_A1_CONTROLLER_IMAGE_BASE" "SDNC_A1_CONTROLLER_IMAGE_TAG" $IMAGE_SUFFIX SDNC
fi

#Remote ric sim image
__check_included_image 'RICSIM'
if [ $? -eq 0 ]; then
	IMAGE_SUFFIX=$(__check_image_override 'RICSIM')
	if [ $? -ne 0 ]; then
		echo -e $RED"Image setting from cmd line not consistent for RICSIM."$ERED
		((IMAGE_ERR++))
	fi
	__check_and_create_image_var " RIC Simulator" "RIC_SIM_IMAGE" "RIC_SIM_IMAGE_BASE" "RIC_SIM_IMAGE_TAG" $IMAGE_SUFFIX RICSIM
fi

#Remote ecs image
__check_included_image 'ECS'
if [ $? -eq 0 ]; then
	IMAGE_SUFFIX=$(__check_image_override 'ECS')
	if [ $? -ne 0 ]; then
		echo -e $RED"Image setting from cmd line not consistent for ECS."$EREDs
		((IMAGE_ERR++))
	fi
	__check_and_create_image_var " ECS" "ECS_IMAGE" "ECS_IMAGE_BASE" "ECS_IMAGE_TAG" $IMAGE_SUFFIX ECS
fi

#Remote rc image
__check_included_image 'RC'
if [ $? -eq 0 ]; then
	IMAGE_SUFFIX=$(__check_image_override 'RC')
	if [ $? -ne 0 ]; then
		echo -e $RED"Image setting from cmd line not consistent for RC."$ERED
		((IMAGE_ERR++))
	fi
	__check_and_create_image_var " RC" "RAPP_CAT_IMAGE" "RAPP_CAT_IMAGE_BASE" "RAPP_CAT_IMAGE_TAG" $IMAGE_SUFFIX RC
fi

# These images are not built as part of this project official images, just check that env vars are set correctly
__check_included_image 'MR'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " Message Router stub"    "MRSTUB_IMAGE"    "MRSTUB_IMAGE_BASE"    "MRSTUB_IMAGE_TAG"    LOCAL               MR
fi
__check_included_image 'DMAAPMR'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " DMAAP Message Router"    "ONAP_DMAAPMR_IMAGE"   "ONAP_DMAAPMR_IMAGE_BASE"    "ONAP_DMAAPMR_IMAGE_TAG"    REMOTE_RELEASE_ONAP               DMAAPMR
	__check_and_create_image_var " ZooKeeper"   "ONAP_ZOOKEEPER_IMAGE" "ONAP_ZOOKEEPER_IMAGE_BASE"  "ONAP_ZOOKEEPER_IMAGE_TAG"  REMOTE_RELEASE_ONAP               DMAAPMR
	__check_and_create_image_var " Kafka"       "ONAP_KAFKA_IMAGE"     "ONAP_KAFKA_IMAGE_BASE"      "ONAP_KAFKA_IMAGE_TAG"      REMOTE_RELEASE_ONAP               DMAAPMR
fi
__check_included_image 'CR'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " Callback Receiver" "CR_IMAGE"        "CR_IMAGE_BASE"        "CR_IMAGE_TAG"        LOCAL               CR
fi
__check_included_image 'PRODSTUB'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " Producer stub"     "PROD_STUB_IMAGE" "PROD_STUB_IMAGE_BASE" "PROD_STUB_IMAGE_TAG" LOCAL               PRODSTUB
fi
__check_included_image 'CONSUL'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " Consul"            "CONSUL_IMAGE"    "CONSUL_IMAGE_BASE"    "CONSUL_IMAGE_TAG"    REMOTE_PROXY        CONSUL
fi
__check_included_image 'CBS'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " CBS"               "CBS_IMAGE"       "CBS_IMAGE_BASE"       "CBS_IMAGE_TAG"       REMOTE_RELEASE_ONAP CBS
fi
__check_included_image 'SDNC'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " SDNC DB"           "SDNC_DB_IMAGE"   "SDNC_DB_IMAGE_BASE"   "SDNC_DB_IMAGE_TAG"   REMOTE_PROXY        SDNC #Uses sdnc app name
fi
__check_included_image 'HTTPPROXY'
if [ $? -eq 0 ]; then
	__check_and_create_image_var " Http Proxy"        "HTTP_PROXY_IMAGE" "HTTP_PROXY_IMAGE_BASE" "HTTP_PROXY_IMAGE_TAG" REMOTE_PROXY HTTPPROXY
fi

#Errors in image setting - exit
if [ $IMAGE_ERR -ne 0 ]; then
	exit 1
fi

#Print a tables of the image settings
echo -e $BOLD"Images configured for start arg: "$START $EBOLD
column -t -s $'\t' $image_list_file

echo ""


#Set the SIM_GROUP var
echo -e $BOLD"Setting var to main dir of all container/simulator scripts"$EBOLD
if [ -z "$SIM_GROUP" ]; then
	SIM_GROUP=$PWD/../simulator-group
	if [ ! -d  $SIM_GROUP ]; then
		echo "Trying to set env var SIM_GROUP to dir 'simulator-group' in the nontrtric repo, but failed."
		echo -e $RED"Please set the SIM_GROUP manually in the applicable $TEST_ENV_VAR_FILE"$ERED
		exit 1
	else
		echo " SIM_GROUP auto set to: " $SIM_GROUP
	fi
elif [ $SIM_GROUP = *simulator_group ]; then
	echo -e $RED"Env var SIM_GROUP does not seem to point to dir 'simulator-group' in the repo, check $TEST_ENV_VAR_FILE"$ERED
	exit 1
else
	echo " SIM_GROUP env var already set to: " $SIM_GROUP
fi

echo ""

#Temp var to check for image pull errors
IMAGE_ERR=0

#Function to check if image exist and stop+remove the container+pull new images as needed
#args <script-start-arg> <descriptive-image-name> <container-base-name> <image-with-tag>
__check_and_pull_image() {

	echo -e " Checking $BOLD$2$EBOLD container(s) with basename: $BOLD$3$EBOLD using image: $BOLD$4$EBOLD"
	format_string="\"{{.Repository}}\\t{{.Tag}}\\t{{.CreatedSince}}\\t{{.Size}}\""
	tmp_im=$(docker images --format $format_string ${4})

	if [ $1 == "local" ]; then
		if [ -z "$tmp_im" ]; then
			echo -e "  "$2" (local image): \033[1m"$4"\033[0m $RED does not exist in local registry, need to be built (or manually pulled)"$ERED
			((IMAGE_ERR++))
			return 1
		else
			echo -e "  "$2" (local image): \033[1m"$4"\033[0m "$GREEN"OK"$EGREEN
		fi
	elif [ $1 == "remote" ] || [ $1 == "remote-remove" ]; then
		if [ $1 == "remote-remove" ]; then
			if [ $RUNMODE == "DOCKER" ]; then
				echo -ne "  Attempt to stop and remove container(s), if running - ${SAMELINE}"
				tmp="$(docker ps -aq --filter name=${3})"
				if [ $? -eq 0 ] && [ ! -z "$tmp" ]; then
					docker stop $tmp &> ./tmp/.dockererr
					if [ $? -ne 0 ]; then
						((IMAGE_ERR++))
						echo ""
						echo -e $RED"  Container(s) could not be stopped - try manual stopping the container(s)"$ERED
						cat ./tmp/.dockererr
						return 1
					fi
				fi
				echo -ne "  Attempt to stop and remove container(s), if running - "$GREEN"stopped"$EGREEN"${SAMELINE}"
				tmp="$(docker ps -aq --filter name=${3})" &> /dev/null
				if [ $? -eq 0 ] && [ ! -z "$tmp" ]; then
					docker rm $tmp &> ./tmp/.dockererr
					if [ $? -ne 0 ]; then
						((IMAGE_ERR++))
						echo ""
						echo -e $RED"  Container(s) could not be removed - try manual removal of the container(s)"$ERED
						cat ./tmp/.dockererr
						return 1
					fi
				fi
				echo -e "  Attempt to stop and remove container(s), if running - "$GREEN"stopped removed"$EGREEN
				tmp_im=""
			else
				tmp_im=""
			fi
		fi
		if [ -z "$tmp_im" ]; then
			echo -ne "  Pulling image${SAMELINE}"
			out=$(docker pull $4)
			if [ $? -ne 0 ]; then
				echo ""
				echo -e "  Pulling image -$RED could not be pulled"$ERED
				((IMAGE_ERR++))
				echo $out > ./tmp/.dockererr
				echo $out
				return 1
			fi
			echo $out > ./tmp/.dockererr
			if [[ $out == *"up to date"* ]]; then
				echo -e "  Pulling image -$GREEN Image is up to date $EGREEN"
			elif [[ $out == *"Downloaded newer image"* ]]; then
				echo -e "  Pulling image -$GREEN Newer image pulled $EGREEN"
			else
				echo -e "  Pulling image -$GREEN Pulled $EGREEN"
			fi
		else
			echo -e "  Pulling image -$GREEN OK $EGREEN(exists in local repository)"
		fi
	fi
	return 0
}

# The following sequence pull the configured images

echo -e $BOLD"Pulling configured images, if needed"$EBOLD

__check_included_image 'PA'
if [ $? -eq 0 ]; then
	START_ARG_MOD=$START_ARG
	__check_image_local_override 'PA'
	if [ $? -eq 1 ]; then
		START_ARG_MOD="local"
	fi
	app="Policy Agent";             __check_and_pull_image $START_ARG_MOD "$app" $POLICY_AGENT_APP_NAME $POLICY_AGENT_IMAGE
else
	echo -e $YELLOW" Excluding PA image from image check/pull"$EYELLOW
fi

__check_included_image 'ECS'
if [ $? -eq 0 ]; then
	START_ARG_MOD=$START_ARG
	__check_image_local_override 'ECS'
	if [ $? -eq 1 ]; then
		START_ARG_MOD="local"
	fi
	app="ECS";             __check_and_pull_image $START_ARG_MOD "$app" $ECS_APP_NAME $ECS_IMAGE
else
	echo -e $YELLOW" Excluding ECS image from image check/pull"$EYELLOW
fi

__check_included_image 'CP'
if [ $? -eq 0 ]; then
	START_ARG_MOD=$START_ARG
	__check_image_local_override 'CP'
	if [ $? -eq 1 ]; then
		START_ARG_MOD="local"
	fi
	app="Non-RT RIC Control Panel"; __check_and_pull_image $START_ARG_MOD "$app" $CONTROL_PANEL_APP_NAME $CONTROL_PANEL_IMAGE
else
	echo -e $YELLOW" Excluding Non-RT RIC Control Panel image from image check/pull"$EYELLOW
fi

__check_included_image 'RC'
if [ $? -eq 0 ]; then
	START_ARG_MOD=$START_ARG
	__check_image_local_override 'RC'
	if [ $? -eq 1 ]; then
		START_ARG_MOD="local"
	fi
	app="RAPP Catalogue"; __check_and_pull_image $START_ARG_MOD "$app" $RAPP_CAT_APP_NAME $RAPP_CAT_IMAGE
else
	echo -e $YELLOW" Excluding RAPP Catalogue image from image check/pull"$EYELLOW
fi

__check_included_image 'RICSIM'
if [ $? -eq 0 ]; then
	START_ARG_MOD=$START_ARG
	__check_image_local_override 'RICSIM'
	if [ $? -eq 1 ]; then
		START_ARG_MOD="local"
	fi
	app="Near-RT RIC Simulator";    __check_and_pull_image $START_ARG_MOD "$app" $RIC_SIM_PREFIX"_"$RIC_SIM_BASE $RIC_SIM_IMAGE
else
	echo -e $YELLOW" Excluding Near-RT RIC Simulator image from image check/pull"$EYELLOW
fi


__check_included_image 'CONSUL'
if [ $? -eq 0 ]; then
	app="Consul";                   __check_and_pull_image $START_ARG "$app" $CONSUL_APP_NAME $CONSUL_IMAGE
else
	echo -e $YELLOW" Excluding Consul image from image check/pull"$EYELLOW
fi

__check_included_image 'CBS'
if [ $? -eq 0 ]; then
	app="CBS";                      __check_and_pull_image $START_ARG "$app" $CBS_APP_NAME $CBS_IMAGE
else
	echo -e $YELLOW" Excluding CBS image from image check/pull"$EYELLOW
fi

__check_included_image 'SDNC'
if [ $? -eq 0 ]; then
	START_ARG_MOD=$START_ARG
	__check_image_local_override 'SDNC'
	if [ $? -eq 1 ]; then
		START_ARG_MOD="local"
	fi
	app="SDNC A1 Controller";       __check_and_pull_image $START_ARG_MOD "$app" $SDNC_APP_NAME $SDNC_A1_CONTROLLER_IMAGE
	app="SDNC DB";                  __check_and_pull_image $START_ARG "$app" $SDNC_APP_NAME $SDNC_DB_IMAGE
else
	echo -e $YELLOW" Excluding SDNC image and related DB image from image check/pull"$EYELLOW
fi

__check_included_image 'HTTPPROXY'
if [ $? -eq 0 ]; then
	app="HTTPPROXY";                __check_and_pull_image $START_ARG "$app" $HTTP_PROXY_APP_NAME $HTTP_PROXY_IMAGE
else
	echo -e $YELLOW" Excluding Http Proxy image from image check/pull"$EYELLOW
fi

__check_included_image 'DMAAPMR'
if [ $? -eq 0 ]; then
	app="DMAAP Message Router";      __check_and_pull_image $START_ARG "$app" $MR_DMAAP_APP_NAME $ONAP_DMAAPMR_IMAGE
	app="ZooKeeper";                 __check_and_pull_image $START_ARG "$app" $MR_ZOOKEEPER_APP_NAME $ONAP_ZOOKEEPER_IMAGE
	app="Kafka";                     __check_and_pull_image $START_ARG "$app" $MR_KAFKA_APP_NAME $ONAP_KAFKA_IMAGE
else
	echo -e $YELLOW" Excluding DMAAP MR image and images (zookeeper, kafka) from image check/pull"$EYELLOW
fi

#Errors in image setting - exit
if [ $IMAGE_ERR -ne 0 ]; then
	echo ""
	echo "#################################################################################################"
	echo -e $RED"One or more images could not be pulled or containers using the images could not be stopped/removed"$ERED
	echo -e $RED"Or local image, overriding remote image, does not exist"$ERED
	if [ $IMAGE_CATEGORY == "DEV" ]; then
		echo -e $RED"Note that SNAPSHOT images may be purged from nexus after a certain period."$ERED
		echo -e $RED"In that case, switch to use a released image instead."$ERED
	fi
	echo "#################################################################################################"
	echo ""
	exit 1
fi

echo ""

echo -e $BOLD"Building images needed for test"$EBOLD

curdir=$PWD
__check_included_image 'MR'
if [ $? -eq 0 ]; then
	cd $curdir
	cd ../mrstub
	echo " Building mrstub image: $MRSTUB_IMAGE"
	docker build  --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $MRSTUB_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN" Build Ok"$EGREEN
	else
		echo -e $RED" Build Failed"$ERED
		((RES_CONF_FAIL++))
		cat .dockererr
		echo -e $RED"Exiting...."$ERED
		exit 1
	fi
	cd $curdir
else
	echo -e $YELLOW" Excluding mrstub from image build"$EYELLOW
fi

__check_included_image 'CR'
if [ $? -eq 0 ]; then
	cd ../cr
	echo " Building Callback Receiver image: $CR_IMAGE"
	docker build  --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $CR_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN" Build Ok"$EGREEN
	else
		echo -e $RED" Build Failed"$ERED
		((RES_CONF_FAIL++))
		cat .dockererr
		echo -e $RED"Exiting...."$ERED
		exit 1
	fi
	cd $curdir
else
	echo -e $YELLOW" Excluding Callback Receiver from image build"$EYELLOW
fi

__check_included_image 'PRODSTUB'
if [ $? -eq 0 ]; then
	cd ../prodstub
	echo " Building Producer stub image: $PROD_STUB_IMAGE"
	docker build  --build-arg NEXUS_PROXY_REPO=$NEXUS_PROXY_REPO -t $PROD_STUB_IMAGE . &> .dockererr
	if [ $? -eq 0 ]; then
		echo -e  $GREEN" Build Ok"$EGREEN
	else
		echo -e $RED" Build Failed"$ERED
		((RES_CONF_FAIL++))
		cat .dockererr
		echo -e $RED"Exiting...."$ERED
		exit 1
	fi
	cd $curdir
else
	echo -e $YELLOW" Excluding Producer stub from image build"$EYELLOW
fi

echo ""

# Create a table of the images used in the script
echo -e $BOLD"Local docker registry images used in the this test script"$EBOLD

docker_tmp_file=./tmp/.docker-images-table
format_string="{{.Repository}}\\t{{.Tag}}\\t{{.CreatedSince}}\\t{{.Size}}\\t{{.CreatedAt}}"
echo -e " Application\tRepository\tTag\tCreated since\tSize\tCreated at" > $docker_tmp_file

__check_included_image 'PA'
if [ $? -eq 0 ]; then
	echo -e " Policy Agent\t$(docker images --format $format_string $POLICY_AGENT_IMAGE)" >>   $docker_tmp_file
fi

__check_included_image 'ECS'
if [ $? -eq 0 ]; then
	echo -e " ECS\t$(docker images --format $format_string $ECS_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'CP'
if [ $? -eq 0 ]; then
	echo -e " Control Panel\t$(docker images --format $format_string $CONTROL_PANEL_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'RICSIM'
if [ $? -eq 0 ]; then
	echo -e " RIC Simulator\t$(docker images --format $format_string $RIC_SIM_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'RC'
if [ $? -eq 0 ]; then
	echo -e " RAPP Catalogue\t$(docker images --format $format_string $RAPP_CAT_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'MR'
if [ $? -eq 0 ]; then
	echo -e " Message Router stub\t$(docker images --format $format_string $MRSTUB_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'DMAAPMR'
if [ $? -eq 0 ]; then
	echo -e " DMAAP Message Router\t$(docker images --format $format_string $ONAP_DMAAPMR_IMAGE)" >>   $docker_tmp_file
	echo -e " ZooKeeper\t$(docker images --format $format_string $ONAP_ZOOKEEPER_IMAGE)" >>   $docker_tmp_file
	echo -e " Kafka\t$(docker images --format $format_string $ONAP_KAFKA_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'CR'
if [ $? -eq 0 ]; then
	echo -e " Callback Receiver\t$(docker images --format $format_string $CR_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'PRODSTUB'
if [ $? -eq 0 ]; then
	echo -e " Producer stub\t$(docker images --format $format_string $PROD_STUB_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'CONSUL'
if [ $? -eq 0 ]; then
	echo -e " Consul\t$(docker images --format $format_string $CONSUL_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'CBS'
if [ $? -eq 0 ]; then
	echo -e " CBS\t$(docker images --format $format_string $CBS_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'SDNC'
if [ $? -eq 0 ]; then
	echo -e " SDNC A1 Controller\t$(docker images --format $format_string $SDNC_A1_CONTROLLER_IMAGE)" >>   $docker_tmp_file
	echo -e " SDNC DB\t$(docker images --format $format_string $SDNC_DB_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'HTTPPROXY'
if [ $? -eq 0 ]; then
	echo -e " Http Proxy\t$(docker images --format $format_string $HTTP_PROXY_IMAGE)" >>   $docker_tmp_file
fi

column -t -s $'\t' $docker_tmp_file

echo ""

echo -e $BOLD"======================================================="$EBOLD
echo -e $BOLD"== Common test setup completed -  test script begins =="$EBOLD
echo -e $BOLD"======================================================="$EBOLD
echo ""

# Function to print the test result, shall be the last cmd in a test script
# args: -
# (Function for test scripts)
print_result() {

	TCTEST_END=$SECONDS
	duration=$((TCTEST_END-TCTEST_START))

	echo "-------------------------------------------------------------------------------------------------"
	echo "-------------------------------------     Test case: "$ATC
	echo "-------------------------------------     Ended:     "$(date)
	echo "-------------------------------------------------------------------------------------------------"
	echo "-- Description: "$TC_ONELINE_DESCR
	echo "-- Execution time: " $duration " seconds"
	echo "-- Used env file: "$TEST_ENV_VAR_FILE
	echo "-------------------------------------------------------------------------------------------------"
	echo "-------------------------------------     RESULTS"
	echo ""


	if [ $RES_DEVIATION -gt 0 ]; then
		echo "Test case deviations"
		echo "===================================="
		cat $DEVIATION_FILE
	fi
	echo ""
	echo "Timer measurement in the test script"
	echo "===================================="
	column -t -s $'\t' $TIMER_MEASUREMENTS
	echo ""

	total=$((RES_PASS+RES_FAIL))
	if [ $RES_TEST -eq 0 ]; then
		echo -e "\033[1mNo tests seem to have been executed. Check the script....\033[0m"
 		echo -e "\033[31m\033[1m ___  ___ ___ ___ ___ _____   ___ _   ___ _   _   _ ___ ___ \033[0m"
 		echo -e "\033[31m\033[1m/ __|/ __| _ \_ _| _ \_   _| | __/_\ |_ _| | | | | | _ \ __|\033[0m"
		echo -e "\033[31m\033[1m\__ \ (__|   /| ||  _/ | |   | _/ _ \ | || |_| |_| |   / _| \033[0m"
 		echo -e "\033[31m\033[1m|___/\___|_|_\___|_|   |_|   |_/_/ \_\___|____\___/|_|_\___|\033[0m"
	elif [ $total != $RES_TEST ]; then
		echo -e "\033[1mTotal number of tests does not match the sum of passed and failed tests. Check the script....\033[0m"
		echo -e "\033[31m\033[1m ___  ___ ___ ___ ___ _____   ___ _   ___ _   _   _ ___ ___ \033[0m"
		echo -e "\033[31m\033[1m/ __|/ __| _ \_ _| _ \_   _| | __/_\ |_ _| | | | | | _ \ __|\033[0m"
		echo -e "\033[31m\033[1m\__ \ (__|   /| ||  _/ | |   | _/ _ \ | || |_| |_| |   / _| \033[0m"
 		echo -e "\033[31m\033[1m|___/\___|_|_\___|_|   |_|   |_/_/ \_\___|____\___/|_|_\___|\033[0m"
	elif [ $RES_CONF_FAIL -ne 0 ]; then
		echo -e "\033[1mOne or more configurations has failed. Check the script log....\033[0m"
		echo -e "\033[31m\033[1m ___  ___ ___ ___ ___ _____   ___ _   ___ _   _   _ ___ ___ \033[0m"
		echo -e "\033[31m\033[1m/ __|/ __| _ \_ _| _ \_   _| | __/_\ |_ _| | | | | | _ \ __|\033[0m"
		echo -e "\033[31m\033[1m\__ \ (__|   /| ||  _/ | |   | _/ _ \ | || |_| |_| |   / _| \033[0m"
 		echo -e "\033[31m\033[1m|___/\___|_|_\___|_|   |_|   |_/_/ \_\___|____\___/|_|_\___|\033[0m"
	elif [ $RES_PASS = $RES_TEST ]; then
		echo -e "All tests \033[32m\033[1mPASS\033[0m"
		echo -e "\033[32m\033[1m  ___  _   ___ ___ \033[0m"
		echo -e "\033[32m\033[1m | _ \/_\ / __/ __| \033[0m"
		echo -e "\033[32m\033[1m |  _/ _ \\__ \__ \\ \033[0m"
		echo -e "\033[32m\033[1m |_|/_/ \_\___/___/ \033[0m"
		echo ""

		# Update test suite counter
		if [ -f .tmp_tcsuite_pass_ctr ]; then
			tmpval=$(< .tmp_tcsuite_pass_ctr)
			((tmpval++))
			echo $tmpval > .tmp_tcsuite_pass_ctr
		fi
		if [ -f .tmp_tcsuite_pass ]; then
			echo " - "$ATC " -- "$TC_ONELINE_DESCR"  Execution time: "$duration" seconds" >> .tmp_tcsuite_pass
		fi
		#Create file with OK exit code
		echo "0" > "$PWD/.result$ATC.txt"
	else
		echo -e "One or more tests with status  \033[31m\033[1mFAIL\033[0m "
		echo -e "\033[31m\033[1m  ___ _   ___ _    \033[0m"
		echo -e "\033[31m\033[1m | __/_\ |_ _| |   \033[0m"
		echo -e "\033[31m\033[1m | _/ _ \ | || |__ \033[0m"
		echo -e "\033[31m\033[1m |_/_/ \_\___|____|\033[0m"
		echo ""
		# Update test suite counter
		if [ -f .tmp_tcsuite_fail_ctr ]; then
			tmpval=$(< .tmp_tcsuite_fail_ctr)
			((tmpval++))
			echo $tmpval > .tmp_tcsuite_fail_ctr
		fi
		if [ -f .tmp_tcsuite_fail ]; then
			echo " - "$ATC " -- "$TC_ONELINE_DESCR"  Execution time: "$duration" seconds" >> .tmp_tcsuite_fail
		fi
	fi

	echo "++++ Number of tests:          "$RES_TEST
	echo "++++ Number of passed tests:   "$RES_PASS
	echo "++++ Number of failed tests:   "$RES_FAIL
	echo ""
	echo "++++ Number of failed configs: "$RES_CONF_FAIL
	echo ""
	echo "++++ Number of test case deviations: "$RES_DEVIATION
	echo ""
	echo "-------------------------------------     Test case complete    ---------------------------------"
	echo "-------------------------------------------------------------------------------------------------"
	echo ""
}

#####################################################################
###### Functions for start, configuring, stoping, cleaning etc ######
#####################################################################

# Start timer for time measurement
# args - (any args will be printed though)
start_timer() {
	echo -e $BOLD"INFO(${BASH_LINENO[0]}): "${FUNCNAME[0]}"," $@ $EBOLD
	TC_TIMER=$SECONDS
	echo " Timer started"
}

# Print the value of the time (in seconds)
# args - <timer message to print>  -  timer value and message will be printed both on screen
#                                     and in the timer measurement report
print_timer() {
	echo -e $BOLD"INFO(${BASH_LINENO[0]}): "${FUNCNAME[0]}"," $@ $EBOLD
	if [ $# -lt 1 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need 1 or more args,  <timer message to print>" $@
		exit 1
	fi
	duration=$(($SECONDS-$TC_TIMER))
	if [ $duration -eq 0 ]; then
		duration="<1 second"
	else
		duration=$duration" seconds"
	fi
	echo " Timer duration :" $duration

	echo -e "${@:1} \t $duration" >> $TIMER_MEASUREMENTS
}

# Print the value of the time (in seconds) and reset the timer
# args - <timer message to print>  -  timer value and message will be printed both on screen
#                                     and in the timer measurement report
print_and_reset_timer() {
	echo -e $BOLD"INFO(${BASH_LINENO[0]}): "${FUNCNAME[0]}"," $@ $EBOLD
	if [ $# -lt 1 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need 1 or more args,  <timer message to print>" $@
		exit 1
	fi
	duration=$(($SECONDS-$TC_TIMER))" seconds"
	if [ $duration -eq 0 ]; then
		duration="<1 second"
	else
		duration=$duration" seconds"
	fi
	echo " Timer duration :" $duration
	TC_TIMER=$SECONDS
	echo " Timer reset"

	echo -e "${@:1} \t $duration" >> $TIMER_MEASUREMENTS

}
# Print info about a deviations from intended tests
# Each deviation counted is also printed in the testreport
# args <deviation message to print>
deviation() {
	echo -e $BOLD"DEVIATION(${BASH_LINENO[0]}): "${FUNCNAME[0]} $EBOLD
	if [ $# -lt 1 ]; then
		((RES_CONF_FAIL++))
		__print_err "need 1 or more args,  <deviation message to print>" $@
		exit 1
	fi
	((RES_DEVIATION++))
	echo -e $BOLD$YELLOW" Test case deviation: ${@:1}"$EYELLOW$EBOLD
	echo "Line: ${BASH_LINENO[0]} - ${@:1}" >> $DEVIATION_FILE
	echo ""
}

# Stop at first FAIL test case and take all logs - only for debugging/trouble shooting
__check_stop_at_error() {
	if [ $STOP_AT_ERROR -eq 1 ]; then
		echo -e $RED"Test script configured to stop at first FAIL, taking all logs and stops"$ERED
		store_logs "STOP_AT_ERROR"
		exit 1
	fi
	return 0
}

# Check if app name var is set. If so return the app name otherwise return "NOTSET"
__check_app_name() {
	if [ $# -eq 1 ]; then
		echo $1
	else
		echo "NOTSET"
	fi
}

# Stop and remove all containers
# args: -
# (Not for test scripts)
__clean_containers() {

	echo -e $BOLD"Stopping and removing all running containers, by container name"$EBOLD

	CONTAINTER_NAMES=("Policy Agent           " $(__check_app_name $POLICY_AGENT_APP_NAME)\
					  "ECS                    " $(__check_app_name $ECS_APP_NAME)\
					  "RAPP Catalogue         " $(__check_app_name $RAPP_CAT_APP_NAME)\
					  "Non-RT RIC Simulator(s)" $(__check_app_name $RIC_SIM_PREFIX)\
					  "Message Router stub    " $(__check_app_name $MR_STUB_APP_NAME)\
					  "DMAAP Message Router   " $(__check_app_name $MR_DMAAP_APP_NAME)\
					  "Zookeeper              " $(__check_app_name $MR_ZOOKEEPER_APP_NAME)\
					  "Kafka                  " $(__check_app_name $MR_KAFKA_APP_NAME)\
					  "Callback Receiver      " $(__check_app_name $CR_APP_NAME)\
					  "Producer stub          " $(__check_app_name $PROD_STUB_APP_NAME)\
					  "Control Panel          " $(__check_app_name $CONTROL_PANEL_APP_NAME)\
					  "SDNC A1 Controller     " $(__check_app_name $SDNC_APP_NAME)\
					  "SDNC DB                " $(__check_app_name $SDNC_DB_APP_NAME)\
					  "CBS                    " $(__check_app_name $CBS_APP_NAME)\
					  "Consul                 " $(__check_app_name $CONSUL_APP_NAME)\
					  "Http Proxy             " $(__check_app_name $HTTP_PROXY_APP_NAME))

	nw=0 # Calc max width of container name, to make a nice table
	for (( i=1; i<${#CONTAINTER_NAMES[@]} ; i+=2 )) ; do

		if [ ${#CONTAINTER_NAMES[i]} -gt $nw ]; then
			nw=${#CONTAINTER_NAMES[i]}
		fi
	done

	for (( i=0; i<${#CONTAINTER_NAMES[@]} ; i+=2 )) ; do
		APP="${CONTAINTER_NAMES[i]}"
		CONTR="${CONTAINTER_NAMES[i+1]}"
		if [ $CONTR != "NOTSET" ]; then
			for((w=${#CONTR}; w<$nw; w=w+1)); do
				CONTR="$CONTR "
			done
			echo -ne " $APP: $CONTR - ${GREEN}stopping${EGREEN}${SAMELINE}"
			docker stop $(docker ps -qa --filter name=${CONTR}) &> /dev/null
			echo -ne " $APP: $CONTR - ${GREEN}stopped${EGREEN}${SAMELINE}"
			docker rm --force $(docker ps -qa --filter name=${CONTR}) &> /dev/null
			echo -e  " $APP: $CONTR - ${GREEN}stopped removed${EGREEN}"
		fi
	done

	echo ""

	echo -e $BOLD" Removing docker network"$EBOLD
	TMP=$(docker network ls -q --filter name=$DOCKER_SIM_NWNAME)
	if [ "$TMP" ==  $DOCKER_SIM_NWNAME ]; then
		docker network rm $DOCKER_SIM_NWNAME | indent2
		if [ $? -ne 0 ];  then
			echo -e $RED" Cannot remove docker network. Manually remove or disconnect containers from $DOCKER_SIM_NWNAME"$ERED
			exit 1
		fi
	fi
	echo -e "$GREEN  Done$EGREEN"

	echo -e $BOLD" Removing all unused docker neworks"$EBOLD
	docker network prune --force | indent2
	echo -e "$GREEN  Done$EGREEN"

	echo -e $BOLD" Removing all unused docker volumes"$EBOLD
	docker volume prune --force | indent2
	echo -e "$GREEN  Done$EGREEN"

	echo -e $BOLD" Removing all dangling/untagged docker images"$EBOLD
    docker rmi --force $(docker images -q -f dangling=true) &> /dev/null
	echo -e "$GREEN  Done$EGREEN"
	echo ""

	CONTRS=$(docker ps | awk '$1 != "CONTAINER" { n++ }; END { print n+0 }')
	if [ $? -eq 0 ]; then
		if [ $CONTRS -ne 0 ]; then
			echo -e $RED"Containers running, may cause distubance to the test case"$ERED
			docker ps -a | indent1
			echo ""
		fi
	fi
}

###################################
### Functions for kube management
###################################

# Scale a kube resource to a specific count
# args: <resource-type> <resource-name> <namespace> <target-count>
# (Not for test scripts)
__kube_scale() {
	echo -ne "  Setting $1 $2 replicas=$4 in namespace $3"$SAMELINE
	kubectl scale  $1 $2  -n $3 --replicas=$4 1> /dev/null 2> ./tmp/kubeerr
	if [ $? -ne 0 ]; then
		echo -e "  Setting $1 $2 replicas=$4 in namespace $3 $RED Failed $ERED"
		((RES_CONF_FAIL++))
		echo "  Message: $(<./tmp/kubeerr)"
		return 1
	else
		echo -e "  Setting $1 $2 replicas=$4 in namespace $3 $GREEN OK $EGREEN"
	fi

	TSTART=$SECONDS

	for i in {1..500}; do
		count=$(kubectl get $1/$2  -n $3 -o jsonpath='{.status.replicas}' 2> /dev/null)
		retcode=$?
		if [ -z "$count" ]; then
			#No value is sometimes returned for some reason, in case the resource has replica 0
			count=0
		fi
		if [ $retcode -ne 0 ]; then
			echo -e "$RED  Cannot fetch current replica count for $1 $2 in namespace $3 $ERED"
			((RES_CONF_FAIL++))
			return 1
		fi
		#echo ""
		if [ $count -ne $4 ]; then
			echo -ne "  Waiting for $1 $2 replicas=$4 in namespace $3. Replicas=$count after $(($SECONDS-$TSTART)) seconds $SAMELINE"
			sleep $i
		else
			echo -e "  Waiting for $1 $2 replicas=$4 in namespace $3. Replicas=$count after $(($SECONDS-$TSTART)) seconds"
			echo -e "  Replicas=$4 after $(($SECONDS-$TSTART)) seconds $GREEN OK $EGREEN"
			echo ""
			return 0
		fi
	done
	echo ""
	echo -e "$RED  Replica count did not reach target replicas=$4. Failed with replicas=$count $ERED"
	((RES_CONF_FAIL++))
	return 0
}

# Scale all kube resource sets to 0 in a namespace for resources having a certain lable and label-id
# This function does not wait for the resource to reach 0
# args: <namespace> <label-name> <label-id>
# (Not for test scripts)
__kube_scale_all_resources() {
	namespace=$1
	labelname=$2
	labelid=$3
	resources="deployment replicaset statefulset"
	for restype in $resources; do
		result=$(kubectl get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.'$labelname'=="'$labelid'")].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			deleted_resourcetypes=$deleted_resourcetypes" "$restype
			for resid in $result; do
				echo -ne "  Ordered caling $restype $resid from namespace $namespace with label $labelname=$labelid to 0"$SAMELINE
				kubectl scale  $restype $resid  -n $namespace --replicas=0 1> /dev/null 2> ./tmp/kubeerr
				echo -e "  Ordered scaling $restype $resid from namespace $namespace with label $labelname=$labelid to 0 $GREEN OK $EGREEN"
			done
		fi
	done
}

# Scale all kube resource sets to 0 in a namespace for resources having a certain lable and label-id
# This function do wait for the resource to reach 0
# args: <namespace> <label-name> <label-id>
# (Not for test scripts)
__kube_scale_and_wait_all_resources() {
	namespace=$1
	labelname=$2
	labelid=$3
	resources="deployment replicaset statefulset"
	scaled_all=1
	while [ $scaled_all -ne 0 ]; do
		scaled_all=0
		for restype in $resources; do
			result=$(kubectl get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.'$labelname'=="'$labelid'")].metadata.name}')
			if [ $? -eq 0 ] && [ ! -z "$result" ]; then
				for resid in $result; do
					echo -e "  Ordered scaling $restype $resid from namespace $namespace with label $labelname=$labelid to 0"
					kubectl scale  $restype $resid  -n $namespace --replicas=0 1> /dev/null 2> ./tmp/kubeerr
					count=1
					T_START=$SECONDS
					while [ $count -ne 0 ]; do
						count=$(kubectl get $restype $resid  -n $namespace -o jsonpath='{.status.replicas}' 2> /dev/null)
						echo -ne "  Scaling $restype $resid from namespace $namespace with label $labelname=$labelid to 0,count=$count"$SAMELINE
						if [ $? -eq 0 ] && [ ! -z "$count" ]; then
							sleep 0.5
						else
							count=0
						fi
						duration=$(($SECONDS-$T_START))
						if [ $duration -gt 100 ]; then
							#Forcring count 0, to avoid hanging for failed scaling
							scaled_all=1
							count=0
						fi
					done
					echo -e "  Scaled $restype $resid from namespace $namespace with label $labelname=$labelid to 0,count=$count $GREEN OK $EGREEN"
				done
			fi
		done
	done
}

# Remove all kube resources in a namespace for resources having a certain label and label-id
# This function wait until the resources are gone. Scaling to 0 must have been ordered previously
# args: <namespace> <label-name> <label-id>
# (Not for test scripts)
__kube_delete_all_resources() {
	namespace=$1
	labelname=$2
	labelid=$3
	resources="deployments replicaset statefulset services pods configmaps pvc"
	deleted_resourcetypes=""
	for restype in $resources; do
		result=$(kubectl get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.'$labelname'=="'$labelid'")].metadata.name}')
		if [ $? -eq 0 ] && [ ! -z "$result" ]; then
			deleted_resourcetypes=$deleted_resourcetypes" "$restype
			for resid in $result; do
				if [ $restype == "replicaset" ] || [ $restype == "statefulset" ]; then
					count=1
					while [ $count -ne 0 ]; do
						count=$(kubectl get $restype $resid  -n $namespace -o jsonpath='{.status.replicas}' 2> /dev/null)
						echo -ne "  Scaling $restype $resid from namespace $namespace with label $labelname=$labelid to 0,count=$count"$SAMELINE
						if [ $? -eq 0 ] && [ ! -z "$count" ]; then
							sleep 0.5
						else
							count=0
						fi
					done
					echo -e "  Scaled $restype $resid from namespace $namespace with label $labelname=$labelid to 0,count=$count $GREEN OK $EGREEN"
				fi
				echo -ne "  Deleting $restype $resid from namespace $namespace with label $labelname=$labelid "$SAMELINE
				kubectl delete $restype $resid -n $namespace 1> /dev/null 2> ./tmp/kubeerr
				if [ $? -eq 0 ]; then
					echo -e "  Deleted $restype $resid from namespace $namespace with label $labelname=$labelid $GREEN OK $EGREEN"
				else
					echo -e "  Deleted $restype $resid from namespace $namespace with label $labelname=$labelid $GREEN Does not exist - OK $EGREEN"
				fi
				#fi
			done
		fi
	done
	if [ ! -z "$deleted_resourcetypes" ]; then
		for restype in $deleted_resources; do
			echo -ne "  Waiting for $restype in namespace $namespace with label $labelname=$labelid to be deleted..."$SAMELINE
			T_START=$SECONDS
			result="dummy"
			while [ ! -z "$result" ]; do
				sleep 0.5
				result=$(kubectl get $restype -n $namespace -o jsonpath='{.items[?(@.metadata.labels.'$labelname'=="'$labelid'")].metadata.name}')
				echo -ne "  Waiting for $restype in namespace $namespace with label $labelname=$labelid to be deleted...$(($SECONDS-$T_START)) seconds "$SAMELINE
				if [ -z "$result" ]; then
					echo -e " Waiting for $restype in namespace $namespace with label $labelname=$labelid to be deleted...$(($SECONDS-$T_START)) seconds $GREEN OK $EGREEN"
				elif [ $(($SECONDS-$T_START)) -gt 300 ]; then
					echo -e " Waiting for $restype in namespace $namespace with label $labelname=$labelid to be deleted...$(($SECONDS-$T_START)) seconds $RED Failed $ERED"
					result=""
				fi
			done
		done
	fi
}

# Creates a namespace if it does not exists
# args: <namespace>
# (Not for test scripts)
__kube_create_namespace() {

	#Check if test namespace exists, if not create it
	kubectl get namespace $1 1> /dev/null 2> ./tmp/kubeerr
	if [ $? -ne 0 ]; then
		echo -ne " Creating namespace "$1 $SAMELINE
		kubectl create namespace $1 1> /dev/null 2> ./tmp/kubeerr
		if [ $? -ne 0 ]; then
			echo -e " Creating namespace $1 $RED$BOLD FAILED $EBOLD$ERED"
			((RES_CONF_FAIL++))
			echo "  Message: $(<./tmp/kubeerr)"
			return 1
		else
			echo -e " Creating namespace $1 $GREEN$BOLD OK $EBOLD$EGREEN"
		fi
	else
		echo -e " Creating namespace $1 $GREEN$BOLD Already exists, OK $EBOLD$EGREEN"
	fi
	return 0
}

# Find the host ip of an app (using the service resource)
# args: <app-name> <namespace>
# (Not for test scripts)
__kube_get_service_host() {
	if [ $# -ne 2 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need 2 args, <app-name> <namespace>" $@
		exit 1
	fi
	for timeout in {1..60}; do
		host=$(kubectl get svc $1  -n $2 -o jsonpath='{.spec.clusterIP}')
		if [ $? -eq 0 ]; then
			if [ ! -z "$host" ]; then
				echo $host
				return 0
			fi
		fi
		sleep 0.5
	done
	((RES_CONF_FAIL++))
	echo "host-not-found-fatal-error"
	return 1
}

# Translate ric name to kube host name
# args: <ric-name>
# For test scripts
get_kube_sim_host() {
	name=$(echo "$1" | tr '_' '-')  #kube does not accept underscore in names
	#example gnb_1_2 -> gnb-1-2
	set_name=$(echo $name | rev | cut -d- -f2- | rev) # Cut index part of ric name to get the name of statefulset
	# example gnb-g1-2 -> gnb-g1 where gnb-g1-2 is the ric name and gnb-g1 is the set name
	echo $name"."$set_name"."$KUBE_NONRTRIC_NAMESPACE
}

# Find the named port to an app (using the service resource)
# args: <app-name> <namespace> <port-name>
# (Not for test scripts)
__kube_get_service_port() {
	if [ $# -ne 3 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need 3 args, <app-name> <namespace> <port-name>" $@
		exit 1
	fi

	for timeout in {1..60}; do
		port=$(kubectl get svc $1  -n $2 -o jsonpath='{...ports[?(@.name=="'$3'")].port}')
		if [ $? -eq 0 ]; then
			if [ ! -z "$port" ]; then
				echo $port
				return 0
			fi
		fi
		sleep 0.5
	done
	((RES_CONF_FAIL++))
	echo "0"
	return 1
}

# Create a kube resource from a yaml template
# args: <resource-type> <resource-name> <template-yaml> <output-yaml>
# (Not for test scripts)
__kube_create_instance() {
	echo -ne " Creating $1 $2"$SAMELINE
	envsubst < $3 > $4
	kubectl apply -f $4 1> /dev/null 2> ./tmp/kubeerr
	if [ $? -ne 0 ]; then
		((RES_CONF_FAIL++))
		echo -e " Creating $1 $2 $RED Failed $ERED"
		echo "  Message: $(<./tmp/kubeerr)"
		return 1
	else
		echo -e " Creating $1 $2 $GREEN OK $EGREEN"
	fi
}

# Function to create a configmap in kubernetes
# args: <configmap-name> <namespace> <labelname> <labelid> <path-to-data-file> <path-to-output-yaml>
# (Not for test scripts)
__kube_create_configmap() {
	echo -ne " Creating configmap $1 "$SAMELINE
	envsubst < $5 > $5"_tmp"
	cp $5"_tmp" $5  #Need to copy back to orig file name since create configmap neeed the original file name
	kubectl create configmap $1  -n $2 --from-file=$5 --dry-run=client -o yaml > $6
	if [ $? -ne 0 ]; then
		echo -e " Creating configmap $1 $RED Failed $ERED"
		((RES_CONF_FAIL++))
		return 1
	fi

	kubectl apply -f $6 1> /dev/null 2> ./tmp/kubeerr
	if [ $? -ne 0 ]; then
		echo -e " Creating configmap $1 $RED Apply failed $ERED"
		echo "  Message: $(<./tmp/kubeerr)"
		((RES_CONF_FAIL++))
		return 1
	fi
	kubectl label configmap $1 -n $2 $3"="$4 --overwrite 1> /dev/null 2> ./tmp/kubeerr
	if [ $? -ne 0 ]; then
		echo -e " Creating configmap $1 $RED Labeling failed $ERED"
		echo "  Message: $(<./tmp/kubeerr)"
		((RES_CONF_FAIL++))
		return 1
	fi
	# Log the resulting map
	kubectl get configmap $1 -n $2 -o yaml > $6

	echo -e " Creating configmap $1 $GREEN OK $EGREEN"
	return 0
}

# This function scales or deletes all resources for app selected by the testcase.
# args: -
# (Not for test scripts)
__clean_kube() {
	echo -e $BOLD"Initialize kube services//pods/statefulsets/replicaset to initial state"$EBOLD

	# Scale prestarted or managed apps
	__check_prestarted_image 'RICSIM'
	if [ $? -eq 0 ]; then
		echo -e " Scaling all kube resources for app $BOLD RICSIM $EBOLD to 0"
		__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app nonrtric-a1simulator
	else
		echo -e " Scaling all kube resources for app $BOLD RICSIM $EBOLD to 0"
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest RICSIM
	fi

	__check_prestarted_image 'PA'
	if [ $? -eq 0 ]; then
		echo -e " Scaling all kube resources for app $BOLD PA $EBOLD to 0"
		__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app nonrtric-policymanagementservice
	else
	    echo -e " Scaling all kube resources for app $BOLD PA $EBOLD to 0"
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest PA
	fi

	__check_prestarted_image 'ECS'
	if [ $? -eq 0 ]; then
		echo -e " Scaling all kube resources for app $BOLD ECS $EBOLD to 0"
		__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app nonrtric-enrichmentservice
	else
		echo -e " Scaling all kube resources for app $BOLD ECS $EBOLD to 0"
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ECS
	fi

	__check_prestarted_image 'RC'
	if [ $? -eq 0 ]; then
		echo -e " Scaling all kube resources for app $BOLD RC $EBOLD to 0"
		__kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app nonrtric-rappcatalogueservice
	else
		echo -e " Scaling all kube resources for app $BOLD RC $EBOLD to 0"
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest RC
	fi

	__check_prestarted_image 'CP'
	if [ $? -eq 0 ]; then
		echo -e " CP replicas kept as is"
	else
		echo -e " Scaling all kube resources for app $BOLD CP $EBOLD to 0"
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest CP
	fi

	__check_prestarted_image 'SDNC'
	if [ $? -eq 0 ]; then
		echo -e " SDNC replicas kept as is"
	else
		echo -e " Scaling all kube resources for app $BOLD SDNC $EBOLD to 0"
		__kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest SDNC
	fi

	__check_prestarted_image 'MR'
	if [ $? -eq 0 ]; then
		echo -e " MR replicas kept as is"
	else
		echo -e " Scaling all kube resources for app $BOLD MR $EBOLD to 0"
		__kube_scale_all_resources $KUBE_ONAP_NAMESPACE autotest MR
	fi

	__check_prestarted_image 'DMAAPMR'
	if [ $? -eq 0 ]; then
		echo -e " DMAAP replicas kept as is"
	else
		echo -e " Scaling all kube resources for app $BOLD DMAAPMR $EBOLD to 0"
		__kube_scale_all_resources $KUBE_ONAP_NAMESPACE autotest DMAAPMR
	fi

	echo -e " Scaling all kube resources for app $BOLD CR $EBOLD to 0"
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest CR

	echo -e " Scaling all kube resources for app $BOLD PRODSTUB $EBOLD to 0"
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest PRODSTUB

	echo -e " Scaling all kube resources for app $BOLD HTTPPROXY $EBOLD to 0"
	__kube_scale_all_resources $KUBE_SIM_NAMESPACE autotest HTTPPROXY


	## Clean all managed apps

	__check_prestarted_image 'RICSIM'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD RICSIM $EBOLD"
		__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest RICSIM
	fi

	__check_prestarted_image 'PA'
	if [ $? -eq 1 ]; then
	    echo -e " Deleting all kube resources for app $BOLD PA $EBOLD"
		__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest PA
	fi

	__check_prestarted_image 'ECS'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD ECS $EBOLD"
		__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest ECS
	fi

	__check_prestarted_image 'RC'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD RC $EBOLD"
		__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest RC
	fi

	__check_prestarted_image 'CP'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD CP $EBOLD"
		__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest CP
	fi

	__check_prestarted_image 'SDNC'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD SDNC $EBOLD"
		__kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest SDNC
	fi

	__check_prestarted_image 'MR'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD MR $EBOLD"
		__kube_delete_all_resources $KUBE_ONAP_NAMESPACE autotest MR
	fi

	__check_prestarted_image 'DMAAPMR'
	if [ $? -eq 1 ]; then
		echo -e " Deleting all kube resources for app $BOLD DMAAPMR $EBOLD"
		__kube_delete_all_resources $KUBE_ONAP_NAMESPACE autotest DMAAPMR
	fi

	echo -e " Deleting all kube resources for app $BOLD CR $EBOLD"
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest CR

	echo -e " Deleting all kube resources for app $BOLD PRODSTUB $EBOLD"
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest PRODSTUB

	echo -e " Deleting all kube resources for app $BOLD HTTPPROXY $EBOLD"
	__kube_delete_all_resources $KUBE_SIM_NAMESPACE autotest HTTPPROXY

	echo ""
}

# Function stop and remove all containers (docker) and services/deployments etc(kube)
# args: -
# Function for test script
clean_environment() {
	if [ $RUNMODE == "KUBE" ]; then
		__clean_kube
	else
		__clean_containers
	fi
}

# Function stop and remove all containers (docker) and services/deployments etc(kube) in the end of the test script, if the arg 'auto-clean' is given at test script start
# args: -
# (Function for test scripts)
auto_clean_environment() {
	echo
	if [ "$AUTO_CLEAN" == "auto" ]; then
		echo -e $BOLD"Initiating automatic cleaning of environment"$EBOLD
		clean_environment
	fi
}

# Function to sleep a test case for a numner of seconds. Prints the optional text args as info
# args: <sleep-time-in-sec> [any-text-in-quotes-to-be-printed]
# (Function for test scripts)
sleep_wait() {

	echo -e $BOLD"INFO(${BASH_LINENO[0]}): "${FUNCNAME[0]}"," $@ $EBOLD
	if [ $# -lt 1 ]; then
		((RES_CONF_FAIL++))
		__print_err "need at least one arg, <sleep-time-in-sec> [any-text-to-printed]" $@
		exit 1
	fi
	#echo "---- Sleep for " $1 " seconds ---- "$2
	start=$SECONDS
	duration=$((SECONDS-start))
	while [ $duration -lt $1 ]; do
		echo -ne "  Slept for ${duration} seconds${SAMELINE}"
		sleep 1
		duration=$((SECONDS-start))
	done
	echo -ne "  Slept for ${duration} seconds${SAMELINE}"
	echo ""
}

# Print error info for the call in the parent script (test case). Arg: <error-message-to-print>
# Not to be called from the test script itself.
__print_err() {
    echo -e $RED ${FUNCNAME[1]} " "$1" " ${BASH_SOURCE[2]} " line" ${BASH_LINENO[1]} $ERED
	if [ $# -gt 1 ]; then
		echo -e $RED" Got: "${FUNCNAME[1]} ${@:2} $ERED
	fi
	((RES_CONF_FAIL++))
}


# Helper function to get a the port of a specific ric simulator
# args: <ric-id>
# (Not for test scripts)
__find_sim_port() {
    name=$1" " #Space appended to prevent matching 10 if 1 is desired....
    cmdstr="docker inspect --format='{{(index (index .NetworkSettings.Ports \"$RIC_SIM_PORT/tcp\") 0).HostPort}}' ${name}"
    res=$(eval $cmdstr)
	if [[ "$res" =~ ^[0-9]+$ ]]; then
		echo $res
	else
		echo "0"
    fi
}

# Helper function to get a the port and host name of a specific ric simulator
# args: <ric-id>
# (Not for test scripts)
__find_sim_host() {
	if [ $RUNMODE == "KUBE" ]; then
		ricname=$(echo "$1" | tr '_' '-')
		for timeout in {1..60}; do
			host=$(kubectl get pod $ricname  -n $KUBE_NONRTRIC_NAMESPACE -o jsonpath='{.status.podIP}' 2> /dev/null)
			if [ ! -z "$host" ]; then
				echo $RIC_SIM_HTTPX"://"$host":"$RIC_SIM_PORT
				return 0
			fi
			sleep 0.5
		done
		echo "host-not-found-fatal-error"
	else
		name=$1" " #Space appended to prevent matching 10 if 1 is desired....
		cmdstr="docker inspect --format='{{(index (index .NetworkSettings.Ports \"$RIC_SIM_PORT/tcp\") 0).HostPort}}' ${name}"
		res=$(eval $cmdstr)
		if [[ "$res" =~ ^[0-9]+$ ]]; then
			echo $RIC_SIM_HOST:$res
			return 0
		else
			echo "0"
		fi
	fi
	return 1
}

# Function to create the docker network for the test
# Not to be called from the test script itself.
__create_docker_network() {
	tmp=$(docker network ls --format={{.Name}} --filter name=$DOCKER_SIM_NWNAME)
	if [ $? -ne 0 ]; then
		echo -e $RED" Could not check if docker network $DOCKER_SIM_NWNAME exists"$ERED
		return 1
	fi
	if [ "$tmp" != $DOCKER_SIM_NWNAME ]; then
		echo -e " Creating docker network:$BOLD $DOCKER_SIM_NWNAME $EBOLD"
		docker network create $DOCKER_SIM_NWNAME | indent2
		if [ $? -ne 0 ]; then
			echo -e $RED" Could not create docker network $DOCKER_SIM_NWNAME"$ERED
			return 1
		else
			echo -e "$GREEN  Done$EGREEN"
		fi
	else
		echo -e " Docker network $DOCKER_SIM_NWNAME already exists$GREEN OK $EGREEN"
	fi
}

# Function to start container with docker-compose and wait until all are in state running.
#args: <docker-compose-dir> <docker-compose-arg>|NODOCKERARGS <count> <app-name>+
# (Not for test scripts)
__start_container() {
	if [ $# -lt 4 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need 4 or more args, <docker-compose-dir> <docker-compose-arg>|NODOCKERARGS <count> <app-name>+" $@
		exit 1
	fi

	__create_docker_network

	curdir=$PWD
	cd $SIM_GROUP
	compose_dir=$1
	cd $1
	shift
	compose_args=$1
	shift
	appcount=$1
	shift

	if [ "$compose_args" == "NODOCKERARGS" ]; then
		docker-compose up -d &> .dockererr
		if [ $? -ne 0 ]; then
			echo -e $RED"Problem to launch container(s) with docker-compose"$ERED
			cat .dockererr
			echo -e $RED"Stopping script...."$ERED
			exit 1
		fi
	else
		docker-compose up -d $compose_args &> .dockererr
		if [ $? -ne 0 ]; then
			echo -e $RED"Problem to launch container(s) with docker-compose"$ERED
			cat .dockererr
			echo -e $RED"Stopping script...."$ERED
			exit 1
		fi
	fi

	cd $curdir

	appindex=0
	while [ $appindex -lt $appcount ]; do
		appname=$1
		shift
		app_started=0
		for i in {1..10}; do
			if [ "$(docker inspect --format '{{ .State.Running }}' $appname)" == "true" ]; then
					echo -e " Container $BOLD${appname}$EBOLD$GREEN running$EGREEN on$BOLD image $(docker inspect --format '{{ .Config.Image }}' ${appname}) $EBOLD"
					app_started=1
					break
				else
					sleep $i
			fi
		done
		if [ $app_started -eq 0 ]; then
			((RES_CONF_FAIL++))
			echo ""
			echo -e $RED" Container $BOLD${appname}$EBOLD could not be started"$ERED
			echo -e $RED" Stopping script..."$ERED
			exit 1
		fi
		let appindex=appindex+1
	done
	return 0
}

# Generate a UUID to use as prefix for policy ids
generate_uuid() {
	UUID=$(python3 -c 'import sys,uuid; sys.stdout.write(uuid.uuid4().hex)')
	#Reduce length to make space for serial id, uses 'a' as marker where the serial id is added
	UUID=${UUID:0:${#UUID}-4}"a"
}


# Function to check if container/service is responding to http/https
# args: <container-name>|<service-name> url
# (Not for test scripts)
__check_service_start() {

	if [ $# -ne 2 ]; then
		((RES_CONF_FAIL++))
		__print_err "need 2 args, <container-name>|<service-name> url" $@
		return 1
	fi

	if [ $RUNMODE == "KUBE" ]; then
		ENTITY="service/set/deployment"
	else
		ENTITY="container"
	fi
	appname=$1
	url=$2
	echo -ne " Container $BOLD${appname}$EBOLD starting${SAMELINE}"


	pa_st=false
	echo -ne " Waiting for ${ENTITY} ${appname} service status...${SAMELINE}"
	TSTART=$SECONDS
	for i in {1..50}; do
		result="$(__do_curl $url)"
		if [ $? -eq 0 ]; then
			if [ ${#result} -gt 15 ]; then
				#If response is too long, truncate
				result="...response text too long, omitted"
			fi
			echo -ne " Waiting for {ENTITY} $BOLD${appname}$EBOLD service status on ${3}, result: $result${SAMELINE}"
	   		echo -ne " The ${ENTITY} $BOLD${appname}$EBOLD$GREEN is alive$EGREEN, responds to service status:$GREEN $result $EGREEN on ${url} after $(($SECONDS-$TSTART)) seconds"
	   		pa_st=true
	   		break
	 	else
		 	TS_TMP=$SECONDS
			while [ $(($TS_TMP+$i)) -gt $SECONDS ]; do
				echo -ne " Waiting for ${ENTITY} ${appname} service status on ${url}...$(($SECONDS-$TSTART)) seconds, retrying in $(($TS_TMP+$i-$SECONDS)) seconds   ${SAMELINE}"
				sleep 1
			done
	 	fi
	done

	if [ "$pa_st" = "false"  ]; then
		((RES_CONF_FAIL++))
		echo -e $RED" The ${ENTITY} ${appname} did not respond to service status on ${url} in $(($SECONDS-$TSTART)) seconds"$ERED
		return 1
	fi

	echo ""
	return 0
}


#################
### Log functions
#################

# Check the agent logs for WARNINGs and ERRORs
# args: -
# (Function for test scripts)

check_policy_agent_logs() {
	__check_container_logs "Policy Agent" $POLICY_AGENT_APP_NAME $POLICY_AGENT_LOGPATH WARN ERR
}

check_ecs_logs() {
	__check_container_logs "ECS" $ECS_APP_NAME $ECS_LOGPATH WARN ERR
}

check_control_panel_logs() {
	__check_container_logs "Control Panel" $CONTROL_PANEL_APP_NAME $CONTROL_PANEL_LOGPATH WARN ERR
}

check_sdnc_logs() {
	__check_container_logs "SDNC A1 Controller" $SDNC_APP_NAME $SDNC_KARAF_LOG WARN ERROR
}

__check_container_logs() {

	dispname=$1
	appname=$2
	logpath=$3
	warning=$4
	error=$5

	echo -e $BOLD"Checking $dispname container $appname log ($logpath) for WARNINGs and ERRORs"$EBOLD

	if [ $RUNMODE == "KUBE" ]; then
		echo -e $YELLOW" Internal log for $dispname not checked in kube"$EYELLOW
		return
	fi

	#tmp=$(docker ps | grep $appname)
	tmp=$(docker ps -q --filter name=$appname) #get the container id
	if [ -z "$tmp" ]; then  #Only check logs for running Policy Agent apps
		echo $dispname" is not running, no check made"
		return
	fi
	foundentries="$(docker exec -t $tmp grep $warning $logpath | wc -l)"
	if [ $? -ne  0 ];then
		echo "  Problem to search $appname log $logpath"
	else
		if [ $foundentries -eq 0 ]; then
			echo "  No WARN entries found in $appname log $logpath"
		else
			echo -e "  Found \033[1m"$foundentries"\033[0m WARN entries in $appname log $logpath"
		fi
	fi
	foundentries="$(docker exec -t $tmp grep $error $logpath | wc -l)"
	if [ $? -ne  0 ];then
		echo "  Problem to search $appname log $logpath"
	else
		if [ $foundentries -eq 0 ]; then
			echo "  No ERR entries found in $appname log $logpath"
		else
			echo -e $RED"  Found \033[1m"$foundentries"\033[0m"$RED" ERR entries in $appname log $logpath"$ERED
		fi
	fi
	echo ""
}

# Store all container logs and other logs in the log dir for the script
# Logs are stored with a prefix in case logs should be stored several times during a test
# args: <logfile-prefix>
# (Function for test scripts)
store_logs() {
	if [ $# != 1 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need one arg, <file-prefix>" $@
		exit 1
	fi
	echo -e $BOLD"Storing all container logs in $TESTLOGS/$ATC using prefix: "$1$EBOLD

	docker stats --no-stream > $TESTLOGS/$ATC/$1_docker_stats.log 2>&1

	docker ps -a  > $TESTLOGS/$ATC/$1_docker_ps.log 2>&1

	cp .httplog_${ATC}.txt $TESTLOGS/$ATC/$1_httplog_${ATC}.txt 2>&1

	if [ $RUNMODE == "DOCKER" ]; then
		__check_included_image 'CONSUL'
		if [ $? -eq 0 ]; then
			docker logs $CONSUL_APP_NAME > $TESTLOGS/$ATC/$1_consul.log 2>&1
		fi

		__check_included_image 'CBS'
		if [ $? -eq 0 ]; then
			docker logs $CBS_APP_NAME > $TESTLOGS/$ATC/$1_cbs.log 2>&1
			body="$(__do_curl $LOCALHOST_HTTP:$CBS_EXTERNAL_PORT/service_component_all/$POLICY_AGENT_APP_NAME)"
			echo "$body" > $TESTLOGS/$ATC/$1_consul_config.json 2>&1
		fi

		__check_included_image 'PA'
		if [ $? -eq 0 ]; then
			docker logs $POLICY_AGENT_APP_NAME > $TESTLOGS/$ATC/$1_policy-agent.log 2>&1
		fi

		__check_included_image 'ECS'
		if [ $? -eq 0 ]; then
			docker logs $ECS_APP_NAME > $TESTLOGS/$ATC/$1_ecs.log 2>&1
		fi

		__check_included_image 'CP'
		if [ $? -eq 0 ]; then
			docker logs $CONTROL_PANEL_APP_NAME > $TESTLOGS/$ATC/$1_control-panel.log 2>&1
		fi

		__check_included_image 'MR'
		if [ $? -eq 0 ]; then
			docker logs $MR_STUB_APP_NAME > $TESTLOGS/$ATC/$1_mr_stub.log 2>&1
		fi

		__check_included_image 'DMAAPSMR'
		if [ $? -eq 0 ]; then
			docker logs $MR_DMAAP_APP_NAME > $TESTLOGS/$ATC/$1_mr.log 2>&1
			docker logs $MR_KAFKA_APP_NAME > $TESTLOGS/$ATC/$1_mr_kafka.log 2>&1
			docker logs $MR_ZOOKEEPER_APP_NAME > $TESTLOGS/$ATC/$1_mr_zookeeper.log 2>&1

		fi

		__check_included_image 'CR'
		if [ $? -eq 0 ]; then
			docker logs $CR_APP_NAME > $TESTLOGS/$ATC/$1_cr.log 2>&1
		fi

		__check_included_image 'SDNC'
		if [ $? -eq 0 ]; then
			docker exec -t $SDNC_APP_NAME cat $SDNC_KARAF_LOG> $TESTLOGS/$ATC/$1_SDNC_karaf.log 2>&1
		fi

		__check_included_image 'RICSIM'
		if [ $? -eq 0 ]; then
			rics=$(docker ps -f "name=$RIC_SIM_PREFIX" --format "{{.Names}}")
			for ric in $rics; do
				docker logs $ric > $TESTLOGS/$ATC/$1_$ric.log 2>&1
			done
		fi

		__check_included_image 'PRODSTUB'
		if [ $? -eq 0 ]; then
			docker logs $PROD_STUB_APP_NAME > $TESTLOGS/$ATC/$1_prodstub.log 2>&1
		fi
	fi
	if [ $RUNMODE == "KUBE" ]; then
		namespaces=$(kubectl  get namespaces -o jsonpath='{.items[?(@.metadata.name)].metadata.name}')
		for nsid in $namespaces; do
			pods=$(kubectl get pods -n $nsid -o jsonpath='{.items[?(@.metadata.labels.autotest)].metadata.name}')
			for podid in $pods; do
				kubectl logs -n $nsid $podid > $TESTLOGS/$ATC/$1_${podid}.log
			done
		done
	fi
	echo ""
}

###############
## Generic curl
###############
# Generic curl function, assumes all 200-codes are ok
# args: <valid-curl-args-including full url>
# returns: <returned response (without respose code)>  or "<no-response-from-server>" or "<not found, <http-code>>""
# returns: The return code is 0 for ok and 1 for not ok
__do_curl() {
	echo ${FUNCNAME[1]} "line: "${BASH_LINENO[1]} >> $HTTPLOG
	curlString="curl -skw %{http_code} $@"
	echo " CMD: $curlString" >> $HTTPLOG
	res=$($curlString)
	echo " RESP: $res" >> $HTTPLOG
	http_code="${res:${#res}-3}"
	if [ ${#res} -eq 3 ]; then
		if [ $http_code -lt 200 ] || [ $http_code -gt 299 ]; then
			echo "<no-response-from-server>"
			return 1
		else
			return 0
		fi
	else
		if [ $http_code -lt 200 ] || [ $http_code -gt 299 ]; then
			echo "<not found, resp:${http_code}>"
			return 1
		fi
		if [ $# -eq 2 ]; then
  			echo "${res:0:${#res}-3}" | xargs
		else
  			echo "${res:0:${#res}-3}"
		fi

		return 0
	fi
}

#######################################
### Basic helper function for test cases
#######################################

# Test a simulator container variable value towards target value using an condition operator with an optional timeout.
# Arg: <simulator-name> <host> <variable-name> <condition-operator> <target-value>  - This test is done
# immediately and sets pass or fail depending on the result of comparing variable and target using the operator.
# Arg: <simulator-name> <host> <variable-name> <condition-operator> <target-value> <timeout>  - This test waits up to the timeout
# before setting pass or fail depending on the result of comparing variable and target using the operator.
# If the <variable-name> has the 'json:' prefix, the the variable will be used as url and the <target-value> will be compared towards the length of the json array in the response.
# Not to be called from test script.

__var_test() {
	checkjsonarraycount=0

	if [ $# -eq 6 ]; then
		if [[ $3 == "json:"* ]]; then
			checkjsonarraycount=1
		fi

		echo -e $BOLD"TEST $TEST_SEQUENCE_NR (${BASH_LINENO[1]}): ${1}, ${3} ${4} ${5} within ${6} seconds"$EBOLD
		((RES_TEST++))
		((TEST_SEQUENCE_NR++))
		start=$SECONDS
		ctr=0
		for (( ; ; )); do
			if [ $checkjsonarraycount -eq 0 ]; then
				result="$(__do_curl $2$3)"
				retcode=$?
				result=${result//[[:blank:]]/} #Strip blanks
			else
				path=${3:5}
				result="$(__do_curl $2$path)"
				retcode=$?
				echo "$result" > ./tmp/.tmp.curl.json
				result=$(python3 ../common/count_json_elements.py "./tmp/.tmp.curl.json")
			fi
			duration=$((SECONDS-start))
			echo -ne " Result=${result} after ${duration} seconds${SAMELINE}"
			let ctr=ctr+1
			if [ $retcode -ne 0 ]; then
				if [ $duration -gt $6 ]; then
					((RES_FAIL++))
					echo -e $RED" FAIL${ERED} - ${3} ${4} ${5} not reached in ${6} seconds, result = ${result}"
					__check_stop_at_error
					return
				fi
			elif [ $4 = "=" ] && [ "$result" -eq $5 ]; then
				((RES_PASS++))
				echo -e " Result=${result} after ${duration} seconds${SAMELINE}"
				echo -e $GREEN" PASS${EGREEN} - Result=${result} after ${duration} seconds"
				return
			elif [ $4 = ">" ] && [ "$result" -gt $5 ]; then
				((RES_PASS++))
				echo -e " Result=${result} after ${duration} seconds${SAMELINE}"
				echo -e $GREEN" PASS${EGREEN} - Result=${result} after ${duration} seconds"
				return
			elif [ $4 = "<" ] && [ "$result" -lt $5 ]; then
				((RES_PASS++))
				echo -e " Result=${result} after ${duration} seconds${SAMELINE}"
				echo -e $GREEN" PASS${EGREEN} - Result=${result} after ${duration} seconds"
				return
			elif [ $4 = "contain_str" ] && [[ $result =~ $5 ]]; then
				((RES_PASS++))
				echo -e " Result=${result} after ${duration} seconds${SAMELINE}"
				echo -e $GREEN" PASS${EGREEN} - Result=${result} after ${duration} seconds"
				return
			else
				if [ $duration -gt $6 ]; then
					((RES_FAIL++))
					echo -e $RED" FAIL${ERED} - ${3} ${4} ${5} not reached in ${6} seconds, result = ${result}"
					__check_stop_at_error
					return
				fi
			fi
			sleep 1
		done
	elif [ $# -eq 5 ]; then
		if [[ $3 == "json:"* ]]; then
			checkjsonarraycount=1
		fi

		echo -e $BOLD"TEST $TEST_SEQUENCE_NR (${BASH_LINENO[1]}): ${1}, ${3} ${4} ${5}"$EBOLD
		((RES_TEST++))
		((TEST_SEQUENCE_NR++))
		if [ $checkjsonarraycount -eq 0 ]; then
			result="$(__do_curl $2$3)"
			retcode=$?
			result=${result//[[:blank:]]/} #Strip blanks
		else
			path=${3:5}
			result="$(__do_curl $2$path)"
			retcode=$?
			echo "$result" > ./tmp/.tmp.curl.json
			result=$(python3 ../common/count_json_elements.py "./tmp/.tmp.curl.json")
		fi
		if [ $retcode -ne 0 ]; then
			((RES_FAIL++))
			echo -e $RED" FAIL ${ERED}- ${3} ${4} ${5} not reached, result = ${result}"
			__check_stop_at_error
		elif [ $4 = "=" ] && [ "$result" -eq $5 ]; then
			((RES_PASS++))
			echo -e $GREEN" PASS${EGREEN} - Result=${result}"
		elif [ $4 = ">" ] && [ "$result" -gt $5 ]; then
			((RES_PASS++))
			echo -e $GREEN" PASS${EGREEN} - Result=${result}"
		elif [ $4 = "<" ] && [ "$result" -lt $5 ]; then
			((RES_PASS++))
			echo -e $GREEN" PASS${EGREEN} - Result=${result}"
		elif [ $4 = "contain_str" ] && [[ $result =~ $5 ]]; then
			((RES_PASS++))
			echo -e $GREEN" PASS${EGREEN} - Result=${result}"
		else
			((RES_FAIL++))
			echo -e $RED" FAIL${ERED} - ${3} ${4} ${5} not reached, result = ${result}"
			__check_stop_at_error
		fi
	else
		echo "Wrong args to __var_test, needs five or six args: <simulator-name> <host> <variable-name> <condition-operator> <target-value> [ <timeout> ]"
		echo "Got:" $@
		exit 1
	fi
}
