#!/bin/bash
################################################################################
#   Copyright (c) 2021 Nordix Foundation.                                      #
#                                                                              #
#   Licensed under the Apache License, Version 2.0 (the "License");            #
#   you may not use this file except in compliance with the License.           #
#   You may obtain a copy of the License at                                    #
#                                                                              #
#       http://www.apache.org/licenses/LICENSE-2.0                             #
#                                                                              #
#   Unless required by applicable law or agreed to in writing, software        #
#   distributed under the License is distributed on an "AS IS" BASIS,          #
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   #
#   See the License for the specific language governing permissions and        #
#   limitations under the License.                                             #
################################################################################

# Override file for running the e-release helm recipe including all components

KUBE_A1SIM_NAMESPACE="nonrtric"
KUBE_SDNC_NAMESPACE="nonrtric"

RIC_SIM_PREFIX="a1-sim"
RIC_SIM_COMMON_SVC_NAME="a1-sim"

ICS_EXTERNAL_PORT=9082
ICS_EXTERNAL_SECURE_PORT=9083

POLICY_AGENT_EXTERNAL_PORT=9080
POLICY_AGENT_EXTERNAL_SECURE_PORT=9081

SDNC_EXTERNAL_PORT=8282
SDNC_EXTERNAL_SECURE_PORT=8383

RAPP_CAT_EXTERNAL_PORT=9085
RAPP_CAT_EXTERNAL_SECURE_PORT=9086

HELM_MANAGER_APP_NAME="helmmanager"
