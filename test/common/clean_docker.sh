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

# Script to clean all docker containers having the label 'nrttest_app', i.e started by autotest

echo "Will stop and remove all docker containers with label 'nrttest_app'"
echo " Stopping containers..."
docker stop $(docker ps -qa  --filter "label=nrttest_app") 2> /dev/null
echo " Removing stopped containers..."
docker rm $(docker ps -qa  --filter "label=nrttest_app") 2> /dev/null

echo "Done"