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

# List of short names for all supported apps, including simulators etc
APP_SHORT_NAMES="A1PMS ICS SDNC CP NGW RC RICSIM RICMEDIATORSIM HTTPPROXY DMAAPMR MR CR PRODSTUB KUBEPROXY DMAAPMED DMAAPADP PVCCLEANER KAFKAPC CHARTMUS HELMMANAGER LOCALHELM KEYCLOAK ISTIO AUTHSIDECAR"

# List of available apps that built and released of the project
PROJECT_IMAGES="A1PMS ICS SDNC CP NGW RICSIM RC DMAAPMED DMAAPADP HELMMANAGER AUTHSIDECAR"

# List of available apps to override with local or remote staging/snapshot/release image
AVAILABLE_IMAGES_OVERRIDE="A1PMS ICS SDNC CP NGW RICSIM RICMEDIATORSIM RC DMAAPMED DMAAPADP HELMMANAGER AUTHSIDECAR"

# List of available apps where the image is built by the test environment
LOCAL_IMAGE_BUILD="MR CR PRODSTUB KUBEPROXY HTTPPROXY KAFKAPC"

# List of system app used only by the test env - kubernetes
TESTENV_KUBE_SYSTEM_APPS="PVCCLEANER"

# List of system app used only by the test env - docker
TESTENV_DOCKER_SYSTEM_APPS=""


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
# 6 Special app used only by the test env is added to TESTENV_KUBE_SYSTEM_APPS and/or TESTENV_DOCKER_SYSTEM_APPS
# Summary:
# All app short name shall exist in APP_SHORT_NAMES
# Then the app short name be added to both PROJECT_IMAGES and AVAILABLE_IMAGES_OVERRIDE
# or only to LOCAL_IMAGE_BUILD
