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

TS_ONELINE_DESCR="Test suite - short list of tests on alternative A1PMS endpoint image testing. a1pms RES and SNDC controller resconf"

. ../common/testsuite_common.sh

suite_setup

############# TEST CASES #################

./FTC1.sh $@
./FTC10.sh $@
./FTC100.sh $@
./FTC110.sh $@
./FTC2001.sh $@

##########################################

suite_complete