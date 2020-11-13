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

# This is a script that contains all the functions needed for auto test
# Arg: local|remote|remote-remove [auto-clean] [--stop-at-error] [--ricsim-prefix <prefix> ] [ --env-file <environment-filename> ] [--use-local-image <app-nam> [<app-name>]*]


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
	echo -e $RED"docker-compose is required to run the test environment, pls install"$ERED
	exit 1
fi

# Just resetting any previous echo formatting...
echo -ne $EBOLD

# default test environment variables
TEST_ENV_VAR_FILE=""

echo "Test case started as: ${BASH_SOURCE[$i+1]} "$@

#Localhost constant
LOCALHOST="http://localhost:"

# Make curl retries towards ECS for http response codes set in this env var, space separated list of codes
ECS_RETRY_CODES=""

# Make curl retries towards the agent for http response codes set in this env var, space separated list of codes
AGENT_RETRY_CODES=""

# Var to contol if the agent runs in a container (normal = 0) or as application on the local machine ( = 1)
AGENT_STAND_ALONE=0

# Var to hold 'auto' in case containers shall be stopped when test case ends
AUTO_CLEAN=""

# Var to hold the app names to use local image for when running 'remote' or 'remote-remove'
USE_LOCAL_IMAGES=""

# List of available apps to override with local image
AVAILABLE_LOCAL_IMAGES_OVERRIDE="PA ECS CP SDNC RICSIM"

# Use this var (STOP_AT_ERROR=1 in the test script) for debugging/trouble shooting to take all logs and exit at first FAIL test case
STOP_AT_ERROR=0

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
if [ $# -lt 1 ]; then
	paramerror=1
fi
if [ $paramerror -eq 0 ]; then
	if [ "$1" != "remote" ] && [ "$1" != "remote-remove" ] && [ "$1" != "local" ]; then
		paramerror=1
	else
		shift;
	fi
fi
foundparm=0
while [ $paramerror -eq 0 ] && [ $foundparm -eq 0 ]; do
	foundparm=1
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
			RIC_SIM_PREFIX=$1
			if [ -z "$1" ]; then
				paramerror=1
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
				if [[ "$AVAILABLE_LOCAL_IMAGES_OVERRIDE" != *"$1"* ]]; then
					paramerror=1
				fi
				shift;
			done
			foundparm=0
			if [ -z "$USE_LOCAL_IMAGES" ]; then
				paramerror=1
			else
				echo "Option set - Override remote images for app(s):"$USE_LOCAL_IMAGES
			fi
		fi
	fi
done
echo ""

#Still params left?
if [ $paramerror -eq 0 ] && [ $# -gt 0 ]; then
	paramerror=1
fi

if [ $paramerror -eq 1 ]; then
	echo -e $RED"Expected arg: local|remote|remote-remove [auto-clean] [--stop-at-error] [--ricsim-prefix <prefix> ] [ --env-file <environment-filename> ] [--use-local-image <app-nam> [<app-name>]*]"$ERED
	exit 1
fi

# sourcing the selected env variables for the test case
if [ -f "$TEST_ENV_VAR_FILE" ]; then
	echo -e $BOLD"Sourcing env vars from: "$TEST_ENV_VAR_FILE$EBOLD
	. $TEST_ENV_VAR_FILE

	if [ -z "$TEST_ENV_PROFILE" ] || [ -z "$SUPPORTED_PROFILES" ]; then
		echo -e $YELLOW"This test case may no work with selected test env file. TEST_ENV_PROFILE is missing in test_env file or SUPPORTED_PROFILES is missing in test case file"$EYELLOW
	else
		if [[ "$SUPPORTED_PROFILES" == *"$TEST_ENV_PROFILE"* ]]; then
			echo -e $GREEN"Test case support the selected test env file"$EGREEN
		else
			echo -e $RED"Test case does not support the selected test env file"$ERED
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

#Vars for A1 interface version and container count
G1_A1_VERSION=""
G2_A1_VERSION=""
G3_A1_VERSION=""
G4_A1_VERSION=""
G5_A1_VERSION=""
G1_COUNT=0
G2_COUNT=0
G3_COUNT=0
G4_COUNT=0
G5_COUNT=0

# Vars to switch between http and https. Extra curl flag needed for https
export RIC_SIM_HTTPX="http"
export RIC_SIM_LOCALHOST=$RIC_SIM_HTTPX"://localhost:"
export RIC_SIM_PORT=$RIC_SIM_INTERNAL_PORT
export RIC_SIM_CERT_MOUNT_DIR="./cert"

export MR_HTTPX="http"
export MR_PORT=$MR_INTERNAL_PORT
export MR_LOCAL_PORT=$MR_EXTERNAL_PORT #When agent is running outside the docker net

export CR_HTTPX="http"
export CR_PORT=$CR_INTERNAL_PORT
export CR_LOCAL_PORT=$CR_EXTERNAL_PORT #When CR is running outside the docker net
export CR_PATH="$CR_HTTPX://$CR_APP_NAME:$CR_PORT$CR_APP_CALLBACK"

export PROD_STUB_HTTPX="http"
export PROD_STUB_PORT=$PROD_STUB_INTERNAL_PORT
export PROD_STUB_LOCAL_PORT=$PROD_STUB_EXTERNAL_PORT #When CR is running outside the docker net
export PROD_STUB_LOCALHOST=$PROD_STUB_HTTPX"://localhost:"$PROD_STUB_LOCAL_PORT

export SDNC_HTTPX="http"
export SDNC_PORT=$SDNC_INTERNAL_PORT
export SDNC_LOCAL_PORT=$SDNC_EXTERNAL_PORT #When agent is running outside the docker net

echo -e $BOLD"Checking configured image setting for this test case"$EBOLD

#Temp var to check for image variable name errors
IMAGE_ERR=0
#Create a file with image info for later printing as a table
image_list_file="./tmp/.image-list"
echo -e " Container\tImage\ttag" > $image_list_file

# Check if image env var is set and if so export the env var with image to use (used by docker compose files)
# arg: <image name> <script start-arg> <target-variable-name> <image-variable-name> <image-tag-variable-name> <app-short-name>
__check_image_var() {
	if [ $# -ne 6 ]; then
		echo "Expected arg: <image name> <script start-arg> <target-variable-name> <image-variable-name> <image-tag-variable-name> <app-short-name>"
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
	image="${!4}"
	tag="${!5}"

	if [ -z $image ]; then
	 	echo -e $RED"\$"$4" not set in $TEST_ENV_VAR_FILE"$ERED
	 	((IMAGE_ERR++))
		echo ""
		tmp=$tmp"<no-image>\t"
	else
		tmp=$tmp$image"\t"
	fi
	if [ -z $tag ]; then
	 	echo -e $RED"\$"$5" not set in $TEST_ENV_VAR_FILE"$ERED
	 	((IMAGE_ERR++))
		echo ""
		tmp=$tmp"<no-tag>\t"
	else
		tmp=$tmp$tag
	fi
	echo -e "$tmp" >> $image_list_file
	#Export the env var
	export "${3}"=$image":"$tag

	#echo " Configured image for ${1} (script start arg=${2}): "$image":"$tag
}


#Check if app local image shall override remote image
# Possible IDs for local image override: PA, CP, SDNC, RICSIM, ECS
__check_image_local_override() {
	for im in $USE_LOCAL_IMAGES; do
		if [ "$1" == "$im" ]; then
			return 1
		fi
	done
	return 0
}

# Check if app uses image included in this test run
# Returns 0 if image is included, 1 if not
# Possible IDs for image inclusion: CBS, CONSUL, CP, CR, ECS, MR, PA, PRODSTUB, RICSIM, SDNC
__check_included_image() {
	for im in $INCLUDED_IMAGES; do
		if [ "$1" == "$im" ]; then
			return 0
		fi
	done
	return 1
}

# Check that image env setting are available
echo ""

if [ $START_ARG == "local" ]; then

	#Local agent image
	__check_image_var " Policy Agent" $START_ARG "POLICY_AGENT_IMAGE" "POLICY_AGENT_LOCAL_IMAGE" "POLICY_AGENT_LOCAL_IMAGE_TAG" PA

	#Local Control Panel image
	__check_image_var " Control Panel" $START_ARG "CONTROL_PANEL_IMAGE" "CONTROL_PANEL_LOCAL_IMAGE" "CONTROL_PANEL_LOCAL_IMAGE_TAG" CP

	#Local SNDC image
	__check_image_var " SDNC A1 Controller" $START_ARG "SDNC_A1_CONTROLLER_IMAGE" "SDNC_A1_CONTROLLER_LOCAL_IMAGE" "SDNC_A1_CONTROLLER_LOCAL_IMAGE_TAG" SDNC

	#Local ric sim image
	__check_image_var " RIC Simulator" $START_ARG "RIC_SIM_IMAGE" "RIC_SIM_LOCAL_IMAGE" "RIC_SIM_LOCAL_IMAGE_TAG" RICSIM

elif [ $START_ARG == "remote" ] || [ $START_ARG == "remote-remove" ]; then

	__check_image_local_override 'PA'
	if [ $? -eq 0 ]; then
		#Remote agent image
		__check_image_var " Policy Agent" $START_ARG "POLICY_AGENT_IMAGE" "POLICY_AGENT_REMOTE_IMAGE" "POLICY_AGENT_REMOTE_IMAGE_TAG" PA
	else
		#Local agent image
		__check_image_var " Policy Agent" $START_ARG "POLICY_AGENT_IMAGE" "POLICY_AGENT_LOCAL_IMAGE" "POLICY_AGENT_LOCAL_IMAGE_TAG" PA
	fi

	__check_image_local_override 'CP'
	if [ $? -eq 0 ]; then
		#Remote Control Panel image
		__check_image_var " Control Panel" $START_ARG "CONTROL_PANEL_IMAGE" "CONTROL_PANEL_REMOTE_IMAGE" "CONTROL_PANEL_REMOTE_IMAGE_TAG" CP
	else
		#Local Control Panel image
		__check_image_var " Control Panel" $START_ARG "CONTROL_PANEL_IMAGE" "CONTROL_PANEL_LOCAL_IMAGE" "CONTROL_PANEL_LOCAL_IMAGE_TAG" CP
	fi

	__check_image_local_override 'SDNC'
	if [ $? -eq 0 ]; then
		#Remote SDNC image
		__check_image_var " SDNC A1 Controller" $START_ARG "SDNC_A1_CONTROLLER_IMAGE" "SDNC_A1_CONTROLLER_REMOTE_IMAGE" "SDNC_A1_CONTROLLER_REMOTE_IMAGE_TAG" SDNC
	else
		#Local SNDC image
		__check_image_var " SDNC A1 Controller" $START_ARG "SDNC_A1_CONTROLLER_IMAGE" "SDNC_A1_CONTROLLER_LOCAL_IMAGE" "SDNC_A1_CONTROLLER_LOCAL_IMAGE_TAG" SDNC
	fi

	__check_image_local_override 'RICSIM'
	if [ $? -eq 0 ]; then
		#Remote ric sim image
		__check_image_var " RIC Simulator" $START_ARG "RIC_SIM_IMAGE" "RIC_SIM_REMOTE_IMAGE" "RIC_SIM_REMOTE_IMAGE_TAG" RICSIM
	else
		#Local ric sim image
		__check_image_var " RIC Simulator" $START_ARG "RIC_SIM_IMAGE" "RIC_SIM_LOCAL_IMAGE" "RIC_SIM_LOCAL_IMAGE_TAG" RICSIM
	fi

	__check_image_local_override 'ECS'
	if [ $? -eq 0 ]; then
		#Remote ecs image
		__check_image_var " ECS" $START_ARG "ECS_IMAGE" "ECS_REMOTE_IMAGE" "ECS_REMOTE_IMAGE_TAG" ECS
	else
		#Local ecs image
		__check_image_var " ECS" $START_ARG "ECS_IMAGE" "ECS_LOCAL_IMAGE" "ECS_LOCAL_IMAGE_TAG" ECS
	fi

else
	#Should never get here....
	echo "Unknow args: "$@
	exit 1
fi


# These images are not built as part of this project official images, just check that env vars are set correctly
__check_image_var " Message Router" $START_ARG "MRSTUB_IMAGE" "MRSTUB_LOCAL_IMAGE" "MRSTUB_LOCAL_IMAGE_TAG" MR
__check_image_var " Callback Receiver" $START_ARG "CR_IMAGE" "CR_LOCAL_IMAGE" "CR_LOCAL_IMAGE_TAG" CR
__check_image_var " Producer stub" $START_ARG "PROD_STUB_IMAGE" "PROD_STUB_LOCAL_IMAGE" "PROD_STUB_LOCAL_IMAGE_TAG" PRODSTUB
__check_image_var " Consul" $START_ARG "CONSUL_IMAGE" "CONSUL_REMOTE_IMAGE" "CONSUL_REMOTE_IMAGE_TAG" CONSUL
__check_image_var " CBS" $START_ARG "CBS_IMAGE" "CBS_REMOTE_IMAGE" "CBS_REMOTE_IMAGE_TAG" CBS
__check_image_var " SDNC DB" $START_ARG "SDNC_DB_IMAGE" "SDNC_DB_REMOTE_IMAGE" "SDNC_DB_REMOTE_IMAGE_TAG" SDNC #Uses sdnc app name

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
			echo -ne "  Removing image - ${SAMELINE}"
			tmp="$(docker images -q ${4})" &> /dev/null
			if [ $? -eq 0 ] && [ ! -z "$tmp" ]; then
				docker rmi --force $4 &> ./tmp/.dockererr
				if [ $? -ne 0 ]; then
					((IMAGE_ERR++))
					echo ""
					echo -e $RED"  Image could not be removed - try manual removal of the image"$ERED
					cat ./tmp/.dockererr
					return 1
				fi
				echo -e "  Removing image - "$GREEN"removed"$EGREEN
			else
				echo -e "  Removing image - "$GREEN"image not in repository"$EGREEN
			fi
			tmp_im=""
		fi
		if [ -z "$tmp_im" ]; then
			echo -ne "  Pulling image${SAMELINE}"
			docker pull $4	&> ./tmp/.dockererr
			tmp_im=$(docker images ${4} | grep -v REPOSITORY)
			if [ -z "$tmp_im" ]; then
				echo ""
				echo -e "  Pulling image -$RED could not be pulled"$ERED
				((IMAGE_ERR++))
				cat ./tmp/.dockererr
				return 1
			fi
			echo -e "  Pulling image -$GREEN Pulled $EGREEN"
		else
			echo -e "  Pulling image -$GREEN OK $EGREEN(exists in local repository)"
		fi
	fi
	return 0
}


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

#Errors in image setting - exit
if [ $IMAGE_ERR -ne 0 ]; then
	echo ""
	echo "#################################################################################################"
	echo -e $RED"One or more images could not be pulled or containers using the images could not be stopped/removed"$ERED
	echo -e $RED"Or local image, overriding remote image, does not exist"$ERED
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
	echo " Building mrstub image: $MRSTUB_LOCAL_IMAGE:$MRSTUB_LOCAL_IMAGE_TAG"
	docker build -t $MRSTUB_LOCAL_IMAGE . &> .dockererr
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
	echo " Building Callback Receiver image: $CR_LOCAL_IMAGE:$CR_IMAGE_TAG"
	docker build -t $CR_LOCAL_IMAGE . &> .dockererr
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
	echo " Building Producer stub image: $PROD_STUB_LOCAL_IMAGE:$PROD_STUB_LOCAL_IMAGE_TAG"
	docker build -t $PROD_STUB_LOCAL_IMAGE . &> .dockererr
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
__check_included_image 'MR'
if [ $? -eq 0 ]; then
	echo -e " Message Router\t$(docker images --format $format_string $MRSTUB_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'CR'
if [ $? -eq 0 ]; then
	echo -e " Callback Receiver\t$(docker images --format $format_string $CR_IMAGE)" >>   $docker_tmp_file
fi
__check_included_image 'PRODSTUB'
if [ $? -eq 0 ]; then
	echo -e " Produccer stub\t$(docker images --format $format_string $PROD_STUB_IMAGE)" >>   $docker_tmp_file
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
		echo -e "\033[1mOne or more configure regest has failed. Check the script log....\033[0m"
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
# (Function for test scripts)
clean_containers() {

	echo -e $BOLD"Stopping and removing all running containers, by container name"$EBOLD

	CONTAINTER_NAMES=("Policy Agent           " $(__check_app_name $POLICY_AGENT_APP_NAME)\
					  "ECS                    " $(__check_app_name $ECS_APP_NAME)\
					  "Non-RT RIC Simulator(s)" $(__check_app_name $RIC_SIM_PREFIX)\
					  "Message Router         " $(__check_app_name $MR_APP_NAME)\
					  "Callback Receiver      " $(__check_app_name $CR_APP_NAME)\
					  "Producer stub          " $(__check_app_name $PROD_STUB_APP_NAME)\
					  "Control Panel          " $(__check_app_name $CONTROL_PANEL_APP_NAME)\
					  "SDNC A1 Controller     " $(__check_app_name $SDNC_APP_NAME)\
					  "SDNC DB                " $(__check_app_name $SDNC_DB_APP_NAME)\
					  "CBS                    " $(__check_app_name $CBS_APP_NAME)\
					  "Consul                 " $(__check_app_name $CONSUL_APP_NAME))

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
			docker ps -a
		fi
	fi
}

# Function stop and remove all container in the end of the test script, if the arg 'auto-clean' is given at test script start
# args: -
# (Function for test scripts)
auto_clean_containers() {
	echo
	if [ "$AUTO_CLEAN" == "auto" ]; then
		echo -e $BOLD"Initiating automatic cleaning of started containers"$EBOLD
		clean_containers
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
}


# Helper function to get a the port of a specific ric simulatpor
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

# Check if container is started by calling url on localhost using a port, expects response code 2XX
# args: <container-name> <port> <url> https|https
# Not to be called from the test script itself.
__check_container_start() {
	paramError=0
	if [ $# -ne 4 ]; then
		paramError=1
	elif [ $4 != "http" ] && [ $4 != "https" ]; then
		paramError=1
	fi
	if [ $paramError -ne 0 ]; then
		((RES_CONF_FAIL++))
		__print_err "need 3 args, <container-name> <port> <url> https|https" $@
		return 1
	fi
	echo -ne " Container $BOLD$1$EBOLD starting${SAMELINE}"
	appname=$1
	localport=$2
	url=$3
	if [[ $appname != "STANDALONE_"* ]]	; then
		app_started=0
		for i in {1..10}; do
			if [ "$(docker inspect --format '{{ .State.Running }}' $appname)" == "true" ]; then
					echo -e " Container $BOLD$1$EBOLD$GREEN running$EGREEN on$BOLD image $(docker inspect --format '{{ .Config.Image }}' ${appname}) $EBOLD"
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
		if [ $localport -eq 0 ]; then
			while [ $localport -eq 0 ]; do
				echo -ne " Waiting for container ${appname} to publish its ports...${SAMELINE}"
				localport=$(__find_sim_port $appname)
				sleep 1
				echo -ne " Waiting for container ${appname} to publish its ports...retrying....${SAMELINE}"
			done
			echo -ne " Waiting for container ${appname} to publish its ports...retrying....$GREEN OK $EGREEN"
			echo ""
		fi
	fi

	pa_st=false
	echo -ne " Waiting for container ${appname} service status...${SAMELINE}"
	TSTART=$SECONDS
	for i in {1..50}; do
		if [ $4 == "https" ]; then
			result="$(__do_curl "-k https://localhost:"${localport}${url})"
		else
			result="$(__do_curl $LOCALHOST${localport}${url})"
		fi
		if [ $? -eq 0 ]; then
			if [ ${#result} -gt 15 ]; then
				#If response is too long, truncate
				result="...response text too long, omitted"
			fi
			echo -ne " Waiting for container $BOLD${appname}$EBOLD service status, result: $result${SAMELINE}"
	   		echo -ne " Container $BOLD${appname}$EBOLD$GREEN is alive$EGREEN, responds to service status:$GREEN $result $EGREEN after $(($SECONDS-$TSTART)) seconds"
	   		pa_st=true
	   		break
	 	else
		 	TS_TMP=$SECONDS
			while [ $(($TS_TMP+$i)) -gt $SECONDS ]; do
				echo -ne " Waiting for container ${appname} service status...$(($SECONDS-$TSTART)) seconds, retrying in $(($TS_TMP+$i-$SECONDS)) seconds   ${SAMELINE}"
				sleep 1
			done
	 	fi
	done

	if [ "$pa_st" = "false"  ]; then
		((RES_CONF_FAIL++))
		echo -e $RED" Container ${appname} did not respond to service status in $(($SECONDS-$TSTART)) seconds"$ERED
		return 0
	fi

	echo ""
	return 0
}


# Function to start a container and wait until it responds on the given port and url.
#args: <docker-compose-dir> NODOCKERARGS|<docker-compose-arg> <app-name> <port-number> <alive-url> [<app-name> <port-number> <alive-url>]*
__start_container() {

	variableArgCount=$(($#-2))
	if [ $# -lt 6 ] && [ [ $(($variableArgCount%4)) -ne 0 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need 6 or more args,  <docker-compose-dir> NODOCKERARGS|<docker-compose-arg> <app-name> <port-number> <alive-url> http|https [<app-name> <port-number> <alive-url> http|https ]*" $@
		exit 1
	fi

	__create_docker_network

	curdir=$PWD
	cd $SIM_GROUP
	cd $1

	if [ "$2" == "NODOCKERARGS" ]; then
		docker-compose up -d &> .dockererr
		if [ $? -ne 0 ]; then
			echo -e $RED"Problem to launch container(s) with docker-compose"$ERED
			cat .dockererr
			echo -e $RED"Stopping script...."$ERED
			exit 1
		fi
	elif [ "$2" == "STANDALONE" ]; then
		echo "Skipping docker-compose"
	else
		docker-compose up -d $2 &> .dockererr
		if [ $? -ne 0 ]; then
			echo -e $RED"Problem to launch container(s) with docker-compose"$ERED
			cat .dockererr
			echo -e $RED"Stopping script...."$ERED
			exit 1
		fi
	fi
	app_prefix=""
	if [ "$2" == "STANDALONE" ]; then
		app_prefix="STANDALONE_"
	fi
	shift; shift;
	cntr=0
	while [ $cntr -lt $variableArgCount ]; do
		app=$app_prefix$1; shift;
		port=$1; shift;
		url=$1; shift;
		httpx=$1; shift;
		let cntr=cntr+4

		__check_container_start "$app" "$port" "$url" $httpx
	done

	cd $curdir
	echo ""
	return 0
}

# Generate a UUID to use as prefix for policy ids
generate_uuid() {
	UUID=$(python3 -c 'import sys,uuid; sys.stdout.write(uuid.uuid4().hex)')
	#Reduce length to make space for serial id, us 'a' as marker where the serial id is added
	UUID=${UUID:0:${#UUID}-4}"a"
}

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

	curlString="$LOCALHOST${CONSUL_EXTERNAL_PORT}/v1/kv/${POLICY_AGENT_APP_NAME}?dc=dc1 -X PUT -H Accept:application/json -H Content-Type:application/json -H X-Requested-With:XMLHttpRequest --data-binary @"$1
	result=$(__do_curl "$curlString")
	if [ $? -ne 0 ]; then
		echo -e $RED" FAIL - json config could not be loaded to consul" $ERED
		((RES_CONF_FAIL++))
		return 1
	fi
	body="$(__do_curl $LOCALHOST$CBS_EXTERNAL_PORT/service_component_all/$POLICY_AGENT_APP_NAME)"
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
		if [ $AGENT_STAND_ALONE -eq 0 ]; then
			config_json=$config_json"\n                       \"baseUrl\": \"$SDNC_HTTPX://$SDNC_APP_NAME:$SDNC_PORT\","
		else
			config_json=$config_json"\n                       \"baseUrl\": \"$SDNC_HTTPX://localhost:$SDNC_LOCAL_PORT\","
		fi
		config_json=$config_json"\n                       \"userName\": \"$SDNC_USER\","
		config_json=$config_json"\n                       \"password\": \"$SDNC_PWD\""
		config_json=$config_json"\n                     }"
		config_json=$config_json"\n   ],"
	fi

	config_json=$config_json"\n   \"streams_publishes\": {"
	config_json=$config_json"\n                            \"dmaap_publisher\": {"
	config_json=$config_json"\n                              \"type\": \"$MR_APP_NAME\","
	config_json=$config_json"\n                              \"dmaap_info\": {"
	if [ $AGENT_STAND_ALONE -eq 0 ]; then
		config_json=$config_json"\n                                \"topic_url\": \"$MR_HTTPX://$MR_APP_NAME:$MR_PORT$MR_WRITE_URL\""
	else
		config_json=$config_json"\n                                \"topic_url\": \"$MR_HTTPX://localhost:$MR_LOCAL_PORT$MR_WRITE_URL\""
	fi
	config_json=$config_json"\n                              }"
	config_json=$config_json"\n                            }"
	config_json=$config_json"\n   },"
	config_json=$config_json"\n   \"streams_subscribes\": {"
	config_json=$config_json"\n                             \"dmaap_subscriber\": {"
	config_json=$config_json"\n                               \"type\": \"$MR_APP_NAME\","
	config_json=$config_json"\n                               \"dmaap_info\": {"
	if [ $AGENT_STAND_ALONE -eq 0 ]; then
		config_json=$config_json"\n                                   \"topic_url\": \"$MR_HTTPX://$MR_APP_NAME:$MR_PORT$MR_READ_URL\""
	else
		config_json=$config_json"\n                                   \"topic_url\": \"$MR_HTTPX://localhost:$MR_LOCAL_PORT$MR_READ_URL\""
	fi
	config_json=$config_json"\n                                 }"
	config_json=$config_json"\n                               }"
	config_json=$config_json"\n   },"

	config_json=$config_json"\n   \"ric\": ["

	rics=$(docker ps | grep $RIC_SIM_PREFIX | awk '{print $NF}')

	if [ $? -ne 0 ] || [ -z "$rics" ]; then
		echo -e $RED" FAIL - the names of the running RIC Simulator cannot be retrieved." $ERED
		((RES_CONF_FAIL++))
		return 1
	fi

	cntr=0
	for ric in $rics; do
		if [ $cntr -gt 0 ]; then
			config_json=$config_json"\n          ,"
		fi
		config_json=$config_json"\n          {"
		config_json=$config_json"\n            \"name\": \"$ric\","
		if [ $AGENT_STAND_ALONE -eq 0 ]; then
			config_json=$config_json"\n            \"baseUrl\": \"$RIC_SIM_HTTPX://$ric:$RIC_SIM_PORT\","
		else
			config_json=$config_json"\n            \"baseUrl\": \"$RIC_SIM_HTTPX://localhost:$(__find_sim_port $ric)\","
		fi
		if [ $1 == "SDNC" ]; then
			config_json=$config_json"\n            \"controller\": \"$SDNC_APP_NAME\","
		fi
		config_json=$config_json"\n            \"managedElementIds\": ["
		config_json=$config_json"\n              \"me1_$ric\","
		config_json=$config_json"\n              \"me2_$ric\""
		config_json=$config_json"\n            ]"
		config_json=$config_json"\n          }"
		let cntr=cntr+1
	done

	config_json=$config_json"\n           ]"
	config_json=$config_json"\n}"


	printf "$config_json">$2

	echo ""
}


# Start Consul and CBS
# args: -
# (Function for test scripts)
start_consul_cbs() {

	echo -e $BOLD"Starting Consul and CBS"$EBOLD
	__check_included_image 'CONSUL'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Consul image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"Consul will not be started"$ERED
		exit
	fi
	__start_container consul_cbs NODOCKERARGS  "$CONSUL_APP_NAME" "$CONSUL_EXTERNAL_PORT" "/ui/dc1/kv" "http" \
	                                             "$CBS_APP_NAME" "$CBS_EXTERNAL_PORT" "/healthcheck" "http"
}

###########################
### RIC Simulator functions
###########################

use_simulator_http() {
	echo -e "Using $BOLD http $EBOLD towards the simulators"
	export RIC_SIM_HTTPX="http"
	export RIC_SIM_LOCALHOST=$RIC_SIM_HTTPX"://localhost:"
	export RIC_SIM_PORT=$RIC_SIM_INTERNAL_PORT
	echo ""
}

use_simulator_https() {
	echo -e "Using $BOLD https $EBOLD towards the simulators"
	export RIC_SIM_HTTPX="https"
	export RIC_SIM_LOCALHOST=$RIC_SIM_HTTPX"://localhost:"
	export RIC_SIM_PORT=$RIC_SIM_INTERNAL_SECURE_PORT
	echo ""
}

# Start one group (ricsim_g1, ricsim_g2 .. ricsim_g5) with a number of RIC Simulators using a given A interface
# 'ricsim' may be set on command line to other prefix
# args:  ricsim_g1|ricsim_g2|ricsim_g3|ricsim_g4|ricsim_g5 <count> <interface-id>
# (Function for test scripts)
start_ric_simulators() {

	echo -e $BOLD"Starting RIC Simulators"$EBOLD

	__check_included_image 'RICSIM'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Near-RT RIC Simulator image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"The Near-RT RIC Simulartor(s) will not be started"$ERED
		exit
	fi

	RIC1=$RIC_SIM_PREFIX"_g1"
	RIC2=$RIC_SIM_PREFIX"_g2"
	RIC3=$RIC_SIM_PREFIX"_g3"
	RIC4=$RIC_SIM_PREFIX"_g4"
	RIC5=$RIC_SIM_PREFIX"_g5"

	if [ $# != 3 ]; then
		((RES_CONF_FAIL++))
    	__print_err "need three args,  $RIC1|$RIC2|$RIC3|$RIC4|$RIC5 <count> <interface-id>" $@
		exit 1
	fi
	echo " $2 simulators using basename: $1 on interface: $3"
	#Set env var for simulator count and A1 interface vesion for the given group
	if [ $1 == "$RIC1" ]; then
		G1_COUNT=$2
		G1_A1_VERSION=$3
	elif [ $1 == "$RIC2" ]; then
		G2_COUNT=$2
		G2_A1_VERSION=$3
	elif [ $1 == "$RIC3" ]; then
		G3_COUNT=$2
		G3_A1_VERSION=$3
	elif [ $1 == "$RIC4" ]; then
		G4_COUNT=$2
		G4_A1_VERSION=$3
	elif [ $1 == "$RIC5" ]; then
		G5_COUNT=$2
		G5_A1_VERSION=$3
	else
		((RES_CONF_FAIL++))
    	__print_err "need three args, $RIC1|$RIC2|$RIC3|$RIC4|$RIC5 <count> <interface-id>" $@
		exit 1
	fi

	# Create .env file to compose project, all ric container will get this prefix
	echo "COMPOSE_PROJECT_NAME="$RIC_SIM_PREFIX > $SIM_GROUP/ric/.env

	export G1_A1_VERSION
	export G2_A1_VERSION
	export G3_A1_VERSION
	export G4_A1_VERSION
	export G5_A1_VERSION

	docker_args="--scale g1=$G1_COUNT --scale g2=$G2_COUNT --scale g3=$G3_COUNT --scale g4=$G4_COUNT --scale g5=$G5_COUNT"
	app_data=""
	cntr=1
	while [ $cntr -le $2 ]; do
		app=$1"_"$cntr
		port=0
		app_data="$app_data $app $port / "$RIC_SIM_HTTPX
		let cntr=cntr+1
	done
	__start_container ric "$docker_args" $app_data

}

###########################
### Control Panel functions
###########################

# Start the Control Panel container
# args: -
# (Function for test scripts)
start_control_panel() {

	echo -e $BOLD"Starting Control Panel"$EBOLD
	__check_included_image 'CP'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Control Panel image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"The Control Panel will not be started"$ERED
		exit
	fi
	__start_container control_panel NODOCKERARGS $CONTROL_PANEL_APP_NAME $CONTROL_PANEL_EXTERNAL_PORT "/" "http"

}

##################
### SDNC functions
##################

# Start the SDNC A1 Controller
# args: -
# (Function for test scripts)
start_sdnc() {

	echo -e $BOLD"Starting SDNC A1 Controller"$EBOLD

	__check_included_image 'SDNC'
	if [ $? -eq 1 ]; then
		echo -e $RED"The image for SDNC and the related DB has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"SDNC will not be started"$ERED
		exit
	fi

	__start_container sdnc NODOCKERARGS $SDNC_APP_NAME $SDNC_EXTERNAL_PORT $SDNC_ALIVE_URL "http"

}

use_sdnc_http() {
	echo -e "Using $BOLD http $EBOLD towards SDNC"
	export SDNC_HTTPX="http"
	export SDNC_PORT=$SDNC_INTERNAL_PORT
	export SDNC_LOCAL_PORT=$SDNC_EXTERNAL_PORT
	echo ""
}

use_sdnc_https() {
	echo -e "Using $BOLD https $EBOLD towards SDNC"
	export SDNC_HTTPX="https"
	export SDNC_PORT=$SDNC_INTERNAL_SECURE_PORT
	export SDNC_LOCAL_PORT=$SDNC_EXTERNAL_SECURE_PORT
	echo ""
}

#####################
### MR stub functions
#####################

# Start the Message Router stub interface in the simulator group
# args: -
# (Function for test scripts)
start_mr() {

	echo -e $BOLD"Starting Message Router 'mrstub'"$EBOLD
	__check_included_image 'MR'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Message Router image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"The Message Router will not be started"$ERED
		exit
	fi
	export MR_CERT_MOUNT_DIR="./cert"
	__start_container mr NODOCKERARGS $MR_APP_NAME $MR_EXTERNAL_PORT "/" "http"
}

use_mr_http() {
	echo -e "Using $BOLD http $EBOLD towards MR"
	export MR_HTTPX="http"
	export MR_PORT=$MR_INTERNAL_PORT
	export MR_LOCAL_PORT=$MR_EXTERNAL_PORT
	echo ""
}

use_mr_https() {
	echo -e "Using $BOLD https $EBOLD towards MR"
	export MR_HTTPX="https"
	export MR_PORT=$MR_INTERNAL_SECURE_PORT
	export MR_LOCAL_PORT=$MR_EXTERNAL_SECURE_PORT
	echo ""
}


################
### CR functions
################

# Start the Callback reciver in the simulator group
# args: -
# (Function for test scripts)
start_cr() {

	echo -e $BOLD"Starting Callback Receiver"$EBOLD
	__check_included_image 'CR'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Callback Receiver image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"The Callback Receiver will not be started"$ERED
		exit
	fi
	__start_container cr NODOCKERARGS $CR_APP_NAME $CR_EXTERNAL_PORT "/" "http"

}

use_cr_http() {
	echo -e "Using $BOLD http $EBOLD towards CR"
	export CR_HTTPX="http"
	export CR_PORT=$CR_INTERNAL_PORT
	export CR_LOCAL_PORT=$CR_EXTERNAL_PORT
	export CR_PATH="$CR_HTTPX://$CR_APP_NAME:$CR_PORT$CR_APP_CALLBACK"
	echo ""
}

use_cr_https() {
	echo -e "Using $BOLD https $EBOLD towards CR"
	export CR_HTTPX="https"
	export CR_PORT=$CR_INTERNAL_SECURE_PORT
	export CR_LOCAL_PORT=$CR_EXTERNAL_SECURE_PORT
	export CR_PATH="$CR_HTTPX://$CR_APP_NAME:$CR_PORT$CR_APP_CALLBACK"
	echo ""
}

###########################
### Producer stub functions
###########################

# Start the Producer stub in the simulator group
# args: -
# (Function for test scripts)
start_prod_stub() {

	echo -e $BOLD"Starting Producer stub"$EBOLD
	__check_included_image 'PRODSTUB'
	if [ $? -eq 1 ]; then
		echo -e $RED"The Producer stub image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"The Producer stub will not be started"$ERED
		exit
	fi
	__start_container prodstub NODOCKERARGS $PROD_STUB_APP_NAME $PROD_STUB_EXTERNAL_PORT "/" "http"

}

use_prod_stub_http() {
	echo -e "Using $BOLD http $EBOLD towards Producer stub"
	export PROD_STUB_HTTPX="http"
	export PROD_STUB_PORT=$PROD_STUB_INTERNAL_PORT
	export PROD_STUB_LOCAL_PORT=$PROD_STUB_EXTERNAL_PORT
	export PROD_STUB_LOCALHOST=$PROD_STUB_HTTPX"://localhost:"$PROD_STUB_LOCAL_PORT
	echo ""
}

use_prod_stub_https() {
	echo -e "Using $BOLD https $EBOLD towards Producer stub"
	export PROD_STUB_HTTPX="https"
	export PROD_STUB_PORT=$PROD_STUB_INTERNAL_SECURE_PORT
	export PROD_STUB_LOCAL_PORT=$PROD_STUB_EXTERNAL_SECURE_PORT
	export PROD_STUB_LOCALHOST=$PROD_STUB_HTTPX"://localhost:"$PROD_STUB_LOCAL_PORT
	echo ""
}

###########################
### Policy Agents functions
###########################

# Use an agent on the local machine instead of container
use_agent_stand_alone() {
	AGENT_STAND_ALONE=1
}

# Start the policy agent
# args: -
# (Function for test scripts)
start_policy_agent() {

	echo -e $BOLD"Starting Policy Agent"$EBOLD

	if [ $AGENT_STAND_ALONE -eq 0 ]; then
		__check_included_image 'PA'
		if [ $? -eq 1 ]; then
			echo -e $RED"The Policy Agent image has not been checked for this test run due to arg to the test script"$ERED
			echo -e $RED"The Policy Agent will not be started"$ERED
			exit
		fi
		__start_container policy_agent NODOCKERARGS $POLICY_AGENT_APP_NAME $POLICY_AGENT_EXTERNAL_PORT "/status" "http"
	else
		echo -e $RED"The consul config produced by this test script (filename '<fullpath-to-autotest-dir>.output<file-name>"$ERED
		echo -e $RED"where the file name is the file in the consul_config_app command in this script) must be pointed out by the agent "$ERED
		echo -e $RED"application.yaml"$ERED
		echo -e $RED"The application jar may need to be built before continuing"$ERED
		echo -e $RED"The agent shall now be running on port $POLICY_AGENT_EXTERNAL_PORT for http"$ERED

		read -p "<press any key to continue>"
		__start_container policy_agent "STANDALONE" $POLICY_AGENT_APP_NAME $POLICY_AGENT_EXTERNAL_PORT "/status" "http"
	fi

}

# All calls to the agent will be directed to the agent REST interface from now on
# args: -
# (Function for test scripts)
use_agent_rest_http() {
	echo -e "Using $BOLD http $EBOLD and $BOLD REST $EBOLD towards the agent"
	export ADAPTER=$RESTBASE
	echo ""
}

# All calls to the agent will be directed to the agent REST interface from now on
# args: -
# (Function for test scripts)
use_agent_rest_https() {
	echo -e "Using $BOLD https $EBOLD and $BOLD REST $EBOLD towards the agent"
	export ADAPTER=$RESTBASE_SECURE
	echo ""
	return 0
}

# All calls to the agent will be directed to the agent dmaap interface over http from now on
# args: -
# (Function for test scripts)
use_agent_dmaap_http() {
	echo -e "Using $BOLD http $EBOLD and $BOLD DMAAP $EBOLD towards the agent"
	export ADAPTER=$DMAAPBASE
	echo ""
	return 0
}

# All calls to the agent will be directed to the agent dmaap interface over https from now on
# args: -
# (Function for test scripts)
use_agent_dmaap_https() {
	echo -e "Using $BOLD https $EBOLD and $BOLD DMAAP $EBOLD towards the agent"
	export ADAPTER=$DMAAPBASE_SECURE
	echo ""
	return 0
}

# Turn on debug level tracing in the agent
# args: -
# (Function for test scripts)
set_agent_debug() {
	echo -e $BOLD"Setting agent debug"$EBOLD
	actuator="/actuator/loggers/org.oransc.policyagent"
	if [[ $POLICY_AGENT_IMAGE = *"onap"* ]]; then
		actuator="/actuator/loggers/org.onap.ccsdk.oran.a1policymanagementservice"
	fi
	curlString="$LOCALHOST$POLICY_AGENT_EXTERNAL_PORT$actuator -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
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
	echo -e $BOLD"Setting agent trace"$EBOLD
	actuator="/actuator/loggers/org.oransc.policyagent"
	if [[ $POLICY_AGENT_IMAGE = *"onap"* ]]; then
		actuator="/actuator/loggers/org.onap.ccsdk.oran.a1policymanagementservice"
	fi
	curlString="$LOCALHOST$POLICY_AGENT_EXTERNAL_PORT$actuator -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
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

###########################
### ECS functions
###########################

# Start the ECS
# args: -
# (Function for test scripts)
start_ecs() {

	echo -e $BOLD"Starting ECS"$EBOLD

	curdir=$PWD
	cd $SIM_GROUP
	cd ecs
	cd $ECS_HOST_MNT_DIR
	if [ -d database ]; then
		echo -e $BOLD" Cleaning files in mounted dir: $PWD/database"$EBOLD
		rm database/* > /dev/null
		if [ $? -ne 0 ]; then
			echo -e $RED" Cannot remove database files in: $PWD"$ERED
			exit 1
		fi
	else
		echo " No files in mounted dir or dir does not exists"
	fi
	cd $curdir

	__check_included_image 'ECS'
	if [ $? -eq 1 ]; then
		echo -e $RED"The ECS image has not been checked for this test run due to arg to the test script"$ERED
		echo -e $RED"ECS will not be started"$ERED
		exit
	fi
	export ECS_CERT_MOUNT_DIR="./cert"
	__start_container ecs NODOCKERARGS $ECS_APP_NAME $ECS_EXTERNAL_PORT "/status" "http"
}

# Restart ECS
# args: -
# (Function for test scripts)
restart_ecs() {
	docker restart $ECS_APP_NAME &> ./tmp/.dockererr
	if [ $? -ne 0 ]; then
		__print_err "Could restart $ECS_APP_NAME" $@
		cat ./tmp/.dockererr
		((RES_CONF_FAIL++))
		return 1
	fi

	__check_container_start $ECS_APP_NAME $ECS_EXTERNAL_PORT "/status" "http"
	echo ""
	return 0
}

# All calls to ECS will be directed to the ECS REST interface from now on
# args: -
# (Function for test scripts)
use_ecs_rest_http() {
	echo -e "Using $BOLD http $EBOLD and $BOLD REST $EBOLD towards ECS"
	export ECS_ADAPTER=$ECS_RESTBASE
	echo ""
}

# All calls to ECS will be directed to the ECS REST interface from now on
# args: -
# (Function for test scripts)
use_ecs_rest_https() {
	echo -e "Using $BOLD https $EBOLD and $BOLD REST $EBOLD towards ECS"
	export ECS_ADAPTER=$ECS_RESTBASE_SECURE
	echo ""
	return 0
}

# All calls to ECS will be directed to the ECS dmaap interface over http from now on
# args: -
# (Function for test scripts)
use_ecs_dmaap_http() {
	echo -e "Using $BOLD http $EBOLD and $BOLD DMAAP $EBOLD towards ECS"
	export ECS_ADAPTER=$ECS_DMAAPBASE
	echo ""
	return 0
}

# All calls to ECS will be directed to the ECS dmaap interface over https from now on
# args: -
# (Function for test scripts)
use_ecs_dmaap_https() {
	echo -e "Using $BOLD https $EBOLD and $BOLD REST $EBOLD towards ECS"
	export ECS_ADAPTER=$ECS_DMAAPBASE_SECURE
	echo ""
	return 0
}

# Turn on debug level tracing in ECS
# args: -
# (Function for test scripts)
set_ecs_debug() {
	echo -e $BOLD"Setting ecs debug"$EBOLD
	curlString="$LOCALHOST$ECS_EXTERNAL_PORT/actuator/loggers/org.oransc.enrichment -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
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
	echo -e $BOLD"Setting ecs trace"$EBOLD
	curlString="$LOCALHOST$ECS_EXTERNAL_PORT/actuator/loggers/org.oransc.enrichment -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
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
use_agent_retries() {
	echo -e $BOLD"Do curl retries to the ECS REST inteface for these response codes:$@"$EBOLD
	ECS_AGENT_RETRY_CODES=$@
	echo ""
	return
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

	__check_included_image 'CONSUL'
	if [ $? -eq 0 ]; then
		docker logs $CONSUL_APP_NAME > $TESTLOGS/$ATC/$1_consul.log 2>&1
	fi

	__check_included_image 'CBS'
	if [ $? -eq 0 ]; then
		docker logs $CBS_APP_NAME > $TESTLOGS/$ATC/$1_cbs.log 2>&1
		body="$(__do_curl $LOCALHOST$CBS_EXTERNAL_PORT/service_component_all/$POLICY_AGENT_APP_NAME)"
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
		docker logs $MR_APP_NAME > $TESTLOGS/$ATC/$1_mr.log 2>&1
	fi

	__check_included_image 'CR'
	if [ $? -eq 0 ]; then
		docker logs $CR_APP_NAME > $TESTLOGS/$ATC/$1_cr.log 2>&1
	fi

	cp .httplog_${ATC}.txt $TESTLOGS/$ATC/$1_httplog_${ATC}.txt 2>&1

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
			echo "X2" >> $HTTPLOG
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

		echo -e $BOLD"TEST(${BASH_LINENO[1]}): ${1}, ${3} ${4} ${5} within ${6} seconds"$EBOLD
		((RES_TEST++))
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

		echo -e $BOLD"TEST(${BASH_LINENO[1]}): ${1}, ${3} ${4} ${5}"$EBOLD
		((RES_TEST++))
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
		__var_test "MR" "$LOCALHOST$MR_EXTERNAL_PORT/counter/" $1 "=" $2 $3
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
		__var_test "MR" "$LOCALHOST$MR_EXTERNAL_PORT/counter/" $1 ">" $2 $3
	else
		((RES_CONF_FAIL++))
		__print_err "Wrong args to mr_greater, needs two or three args: <sim-param> <target-value> [ timeout ]" $@
	fi
}

# Read a variable value from MR sim and send to stdout. Arg: <variable-name>
mr_read() {
	echo "$(__do_curl $LOCALHOST$MR_EXTERNAL_PORT/counter/$1)"
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
	echo -e $BOLD"INFO(${BASH_LINENO[0]}): mrstub, $1 = $(__do_curl $LOCALHOST$MR_EXTERNAL_PORT/counter/$1)"$EBOLD
}
