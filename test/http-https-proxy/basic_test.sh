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

# Automated test script for callback receiver container


echo "NOTE - No automatic response check - check manually"
echo "All call shall return a json struct"
echo ""

CMD="curl --proxy localhost:8080 localhost:8081"
echo "Running cmd: "$CMD
$CMD
echo ""
if [ $? -eq 0 ]; then
    echo "CMD OK"
else
    echo "CMD FAIL"
    exti 1
fi
echo ""

CMD="curl --proxy localhost:8080 -k https://localhost:8434"
echo "Running cmd: "$CMD
$CMD
echo ""
if [ $? -eq 0 ]; then
    echo "CMD OK"
else
    echo "CMD FAIL"
    exti 1
fi
echo ""

CMD="curl --proxy-insecure --proxy https://localhost:8433 localhost:8081"
echo "Running cmd: "$CMD
$CMD
echo ""
if [ $? -eq 0 ]; then
    echo "CMD OK"
else
    echo "CMD FAIL"
    exti 1
fi
echo ""

CMD="curl --proxy-insecure --proxy https://localhost:8433 -k https://localhost:8434"
echo "Running cmd: "$CMD
$CMD
echo ""
if [ $? -eq 0 ]; then
    echo "CMD OK"
else
    echo "CMD FAIL"
    exti 1
fi
echo ""

echo "DONE"
