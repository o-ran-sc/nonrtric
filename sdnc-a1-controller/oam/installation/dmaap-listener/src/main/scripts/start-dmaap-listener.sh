#!/bin/bash

###
# ============LICENSE_START=======================================================
# ONAP : SDN-C
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights
# 			reserved.
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

PROPERTY_DIR=${PROPERTY_DIR:-/opt/onap/sdnc/data/properties}


LISTENER=dmaap-listener

DMAAPLISTENERROOT=${DMAAPLISTENERROOT:-/opt/onap/sdnc/dmaap-listener}
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-7-oracle}
JAVA_OPTS=${JAVA_OPTS:--Dhttps.protocols=TLSv1.1,TLSv1.2}
JAVA=${JAVA:-${JAVA_HOME}/bin/java}



for file in ${DMAAPLISTENERROOT}/lib/*.jar
do
  LISTENERCLASSPATH=$LISTENERCLASSPATH:$file
done

echo "Starting dmaap-listener"
exec ${JAVA} ${JAVA_OPTS} -Dlog4j.configurationFile=${PROPERTY_DIR}/log4j.properties -jar ${DMAAPLISTENERROOT}/lib/dmaap-listener*.jar dmaap-listener.properties -cp ${LISTENERCLASSPATH}



