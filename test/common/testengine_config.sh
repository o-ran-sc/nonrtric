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

# List of short names for all supported apps, including simulators etc
APP_SHORT_NAMES="PA ECS SDNC CP NGW RC RICSIM HTTPPROXY CBS CONSUL DMAAPMR MR CR PRODSTUB KUBEPROXY"

# List of available apps that built and released of the project
PROJECT_IMAGES="PA ECS SDNC CP NGW RICSIM RC"

# List of available apps to override with local or remote staging/snapshot/release image
AVAILABLE_IMAGES_OVERRIDE="PA ECS SDNC CP NGW RICSIM RC"

# List of available apps where the image is built by the test environment
LOCAL_IMAGE_BUILD="MR CR PRODSTUB KUBEPROXY"


#Integrate a new app into the test environment
# 1 Choose a short name for the app
#   Note than an app might use more than one image
# 2 Add the short name to APP_SHORT_NAMES
# 3 If the image is built and released as part of the project,
#   add the short name to PROJECT_IMAGES
# 4 If it possible to override with for example  a local image,
#   add the short name to AVAILABLE_IMAGES_OVERRIDE
#   This is default...so normally images shall be possible to override
# 5 If the image is built by the test script,
#   add the short name to LOCAL_IMAGE_BUILD
# Summary:
# All app short name shall exist in APP_SHORT_NAMES
# Then the app short name be added to both PROJECT_IMAGES and AVAILABLE_IMAGES_OVERRIDE
# or only to LOCAL_IMAGE_BUILD
