#!/bin/bash

###
# ============LICENSE_START=======================================================
# openECOMP : SDN-C
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights
# 							reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

ODL_HOME=${ODL_HOME:-/opt/opendaylight/current}
ODL_ADMIN_PASSWORD=${ODL_ADMIN_PASSWORD:-Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U}
SDNC_HOME=${SDNC_HOME:-/opt/onap/sdnc}
CCSDK_HOME=${CCSDK_HOME:-/opt/onap/ccsdk}
CCSDK_FEATURE_DIR=${CCSDK_FEATURE_DIR:-${CCSDK_HOME}/features}
SDNC_FEATURE_DIR=${SDNC_FEATURE_DIR:-${SDNC_HOME}/features}

CCSDK_EXTRAS=" \
   ansible-adapter \
   lcm \
   netbox-client"



SDNC_NORTHBOUND_FEATURES=" \
  generic-resource-api \
  vnfapi \
  vnftools"


SDNC_NORTHBOUND_VERSION=${SDNC_NORTHBOUND_VERSION:-1.3.1-SNAPSHOT}

# Install CCSDK features
${CCSDK_HOME}/bin/installCcsdkFeatures.sh

# Install CCSDK extras, used by SDNC but not APP-C
echo "Installing CCSDK extras"
for feature in ${CCSDK_EXTRAS}
do
	if [ -f ${CCSDK_FEATURE_DIR}/ccsdk-${feature}/install-feature.sh ]
	then
		${CCSDK_FEATURE_DIR}/ccsdk-${feature}/install-feature.sh
    else
    	     echo "No installer found for ${feature}"
    fi
done


echo "Installing SDN-C northbound"
for feature in ${SDNC_NORTHBOUND_FEATURES}
do
  if [ -f ${SDNC_FEATURE_DIR}/sdnc-${feature}/install-feature.sh ]
  then
    ${SDNC_FEATURE_DIR}/sdnc-${feature}/install-feature.sh
  else
    echo "No installer found for feature sdnc-${feature}"
  fi
done


