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

echo "This script requires golang to be installed and a running kafka instance on (or available to) localhost"

# Script to build and start app locally
if [ $# -ne 1 ]; then
    echo "usage: ./start-local.sh <kafka-boostrapserver-port>"
    echo "example: ./start-local.sh 30098"
    exit 1
fi

export KAFKA_BOOTSTRAP_SERVER=localhost:$1

echo "Starting kafka-procon on local machine"
go run main.go
