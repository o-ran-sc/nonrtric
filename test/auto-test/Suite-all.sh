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

TS_ONELINE_DESCR="Test suite - all test cases except the stab test(s)"

. ../common/testsuite_common.sh

suite_setup

############# TEST CASES #################
ARG1=$1

./FTC1.sh $ARG1

if [ $ARG1 == "remote-remove" ]; then
    #Prevent image removal for every test case
    ARG1="remote"
fi

./FTC10.sh $ARG1
./FTC100.sh $ARG1
./FTC110.sh $ARG1
./FTC150.sh $ARG1
./FTC300.sh $ARG1
./FTC310.sh $ARG1
./FTC350.sh $ARG1
./FTC800.sh $ARG1
./FTC850.sh $ARG1

##########################################

suite_complete