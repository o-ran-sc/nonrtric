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

if [ -z "$SETTINGS_FILE" -a -z "$GLOBAL_SETTINGS_FILE" -a -s "$HOME"/.m2/settings.xml ]
then
  DEFAULT_MAVEN_SETTINGS=${HOME}/.m2/settings.xml
  SETTINGS_FILE=${SETTINGS_FILE:-${DEFAULT_MAVEN_SETTINGS}}
  GLOBAL_SETTINGS_FILE=${GLOBAL_SETTINGS_FILE:-${DEFAULT_MAVEN_SETTINGS}}
fi

SDNC_HOME=${SDNC_HOME:-/opt/onap/sdnc}

targetDir=${1:-${SDNC_HOME}}
featureDir=${targetDir}/features


SDNC_NORTHBOUND_FEATURES=" \
  generic-resource-api \
  vnfapi \
  vnftools"



SDNC_NORTHBOUND_VERSION=${SDNC_NORTHBOUND_VERSION:-1.2.0}
SDNC_OAM_VERSION=${SDNC_OAM_VERSION:-1.2.0}

if [ ! -d ${targetDir} ]
then
  mkdir -p ${targetDir}
fi

if [ ! -d ${featureDir} ]
then
  mkdir -p ${featureDir}
fi

cwd=$(pwd)

mavenOpts=${2:-"-s ${SETTINGS_FILE} -gs ${GLOBAL_SETTINGS_FILE}"}
cd /tmp

echo "Installing SDN-C core version ${SDNC_CORE_VERSION}"
for feature in ${SDNC_CORE_FEATURES}
do
 rm -f /tmp/${feature}-installer*.zip
mvn -U ${mavenOpts} org.apache.maven.plugins:maven-dependency-plugin:2.9:copy -Dartifact=org.onap.sdnc.core:${feature}-installer:${SDNC_CORE_VERSION}:zip -DoutputDirectory=/tmp -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.ssl.insecure=true
 unzip -d ${featureDir} /tmp/${feature}-installer*zip
done

echo "Installing SDN-C adaptors version ${SDNC_ADAPTORS_VERSION}"
for feature in ${SDNC_ADAPTORS_FEATURES}
do
 rm -f /tmp/${feature}-installer*.zip
mvn -U ${mavenOpts} org.apache.maven.plugins:maven-dependency-plugin:2.9:copy -Dartifact=org.onap.sdnc.adaptors:${feature}-installer:${SDNC_ADAPTORS_VERSION}:zip -DoutputDirectory=/tmp -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.ssl.insecure=true
 unzip -d ${featureDir} /tmp/${feature}-installer*zip
done

echo "Installing SDN-C northbound version ${SDNC_NORTHBOUND_VERSION}"
for feature in ${SDNC_NORTHBOUND_FEATURES}
do
 rm -f /tmp/${feature}-installer*.zip
mvn -U ${mavenOpts} org.apache.maven.plugins:maven-dependency-plugin:2.9:copy -Dartifact=org.onap.sdnc.northbound:${feature}-installer:${SDNC_NORTHBOUND_VERSION}:zip -DoutputDirectory=/tmp -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.ssl.insecure=true
 unzip -d ${featureDir} /tmp/${feature}-installer*zip
done

echo "Installing SDN-C plugins version ${SDNC_PLUGINS_VERSION}"
for feature in ${SDNC_PLUGINS_FEATURES}
do
 rm -f /tmp/${feature}-installer*.zip
mvn -U ${mavenOpts} org.apache.maven.plugins:maven-dependency-plugin:2.9:copy -Dartifact=org.onap.sdnc.plugins:${feature}-installer:${SDNC_PLUGINS_VERSION}:zip -DoutputDirectory=/tmp -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.ssl.insecure=true
 unzip -d ${featureDir} /tmp/${feature}-installer*zip
done



echo "Installing platform-logic"
rm -f /tmp/platform-logic-installer*.zip
mvn -U ${mavenOpts} org.apache.maven.plugins:maven-dependency-plugin:2.9:copy -Dartifact=org.onap.sdnc.oam:platform-logic-installer:${SDNC_OAM_VERSION}:zip -DoutputDirectory=/tmp -Dmaven.wagon.http.ssl.allowall=true -Dmaven.wagon.ssl.insecure=true
unzip -d ${targetDir} /tmp/platform-logic-installer*.zip

find ${targetDir} -name '*.sh' -exec chmod +x '{}' \;

cd $cwd

