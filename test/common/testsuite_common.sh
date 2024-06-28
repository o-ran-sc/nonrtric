#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2023 Nordix Foundation. All rights reserved.
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

# Script containing all functions needed for auto testing of test suites

echo "Test suite started as: ${BASH_SOURCE[$i+1]} "$1 $2


IMAGE_TAG=""

paramError=1
if [ $# -gt 0 ]; then
	if [ $1 == "local" ] || [ $1 == "remote" ] || [ $1 == "remote-remove" ] ; then
		paramError=0
	fi
fi
if [ $paramError -ne 0 ]; then
	echo "Expected arg: local|remote|remote-remove"
	exit 1
fi

# Set a description string for the test suite
if [ -z "$TS_ONELINE_DESCR" ]; then
	TS_ONELINE_DESCR="<no-description>"
	echo "No test suite description found, TC_ONELINE_DESCR should be set on in the test script , using "$TS_ONELINE_DESCR
fi

TSTEST_START=$SECONDS

suite_setup() {
    ATS=$(basename "${BASH_SOURCE[$i+1]}" .sh)

    echo "#################################################################################################"
    echo "###################################      Test suite: "$ATS
    echo "###################################      Started:    "$(date)
    echo "#################################################################################################"
    echo "## Description: " $TS_ONELINE_DESCR
    echo "#################################################################################################"
    echo ""
    echo 0 > .tmp_tcsuite_ctr
    echo 0 > .tmp_tcsuite_pass_ctr
    echo 0 > .tmp_tcsuite_fail_ctr
    rm .tmp_tcsuite_pass &> /dev/null
    touch .tmp_tcsuite_pass
    rm .tmp_tcsuite_fail &> /dev/null
    touch .tmp_tcsuite_fail
}

__print_err() {
    echo ${FUNCNAME[1]} " "$1" " ${BASH_SOURCE[$i+2]} " line" ${BASH_LINENO[$i+1]}
}

run_tc() {
	if [ $# -eq 2 ]; then
		./$1 $2 $IMAGE_TAG
	elif [ $# -eq 3 ]; then
		./$1 $2 $3
	else
		echo -e "Test case \033[31m\033[1m./"$1 $2 $3 "could not be executed.\033[0m"
	fi
}

suite_complete() {
    TSTEST_END=$SECONDS
    echo ""
    echo "#################################################################################################"
    echo "###################################      Test suite: "$ATS
    echo "###################################      Ended:      "$(date)
    echo "#################################################################################################"
    echo "## Description: " $TS_ONELINE_DESCR
    echo "## Execution time: " $((TSTEST_END-TSTEST_START)) " seconds"
    echo "#################################################################################################"
    echo "###################################      RESULTS"
    echo ""

    TCSUITE_CTR=$(< .tmp_tcsuite_ctr)
    TCSUITE_PASS_CTR=$(< .tmp_tcsuite_pass_ctr)
    TCSUITE_FAIL_CTR=$(< .tmp_tcsuite_fail_ctr)

    total=$((TCSUITE_PASS_CTR+TCSUITE_FAIL_CTR))
    if [ $TCSUITE_CTR -eq 0 ]; then
		    echo -e "\033[1mNo test cases seem to have executed. Check the script....\033[0m"
	  elif [ $total != $TCSUITE_CTR ]; then
        echo -e "\033[1mTotal number of test cases does not match the sum of passed and failed test cases. Check the script....\033[0m"
    fi
    echo "Number of test cases : " $TCSUITE_CTR
    echo -e "Number of \033[31m\033[1mFAIL\033[0m:        " $TCSUITE_FAIL_CTR
    echo -e "Number of \033[32m\033[1mPASS\033[0m:        " $TCSUITE_PASS_CTR
    echo ""
    echo "PASS test cases"
    cat .tmp_tcsuite_pass
    echo ""
    echo "FAIL test cases"
    cat .tmp_tcsuite_fail
    echo ""
    if [ $TCSUITE_FAIL_CTR -ne 0 ]; then
      echo "###################################      Test suite completed with Tests FAIL     ##############################"
      echo "#################################################################################################"
    else
      echo "###################################      Test suite completed      ##############################"
      echo "#################################################################################################"
    fi

    exit $TCSUITE_FAIL_CTR
}