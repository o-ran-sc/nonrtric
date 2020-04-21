#!/bin/bash

TS_ONELINE_DESCR="Test suite - interface testing. Agent REST and SNDC controller resconf"

. ../common/testsuite_common.sh

suite_setup

############# TEST CASES #################

./FTC10.sh $1
./FTC500.sh $1

##########################################

suite_complete