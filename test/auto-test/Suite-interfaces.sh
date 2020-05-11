#!/bin/bash

TS_ONELINE_DESCR="Test suite - interface testing. Agent REST, DMAAP and SNDC controller resconf"

. ../common/testsuite_common.sh

suite_setup

############# TEST CASES #################

./FTC100.sh $1
./FTC110.sh $1
./FTC150.sh $1

##########################################

suite_complete