#!/bin/bash

###
# ============LICENSE_START=======================================================
# openECOMP : SDN-C
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights
# 							reserved.
# Modifications Copyright (C) 2020 Nordix Foundation.
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

# Install SDN-C platform components if not already installed and start container

ODL_HOME=${ODL_HOME:-/opt/opendaylight/current}
ODL_ADMIN_USERNAME=${ODL_ADMIN_USERNAME:-admin}
ODL_ADMIN_PASSWORD=${ODL_ADMIN_PASSWORD:-Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U}
HTTPS_PROPS=${HTTPS_PROPS:-/opt/onap/sdnc/data/properties/https-props.properties}
SDNC_HOME=${SDNC_HOME:-/opt/onap/sdnc}
SDNC_BIN=${SDNC_BIN:-/opt/onap/sdnc/bin}
MYSQL_PASSWD=${MYSQL_PASSWD:-openECOMP1.0}
INSTALLED_DIR=${INSTALLED_FILE:-/opt/opendaylight/current/daexim}
export ODL_ADMIN_PASSWORD ODL_ADMIN_USERNAME

echo org.ops4j.pax.web.ssl.keystore=$(cat $HTTPS_PROPS | grep -w key-store | cut -d '=' -f2) >> /opt/opendaylight/etc/custom.properties
echo org.ops4j.pax.web.ssl.password=$(cat $HTTPS_PROPS | grep -w keystore-password | cut -d '=' -f2) >> /opt/opendaylight/etc/custom.properties
echo org.ops4j.pax.web.ssl.keypassword=$(cat $HTTPS_PROPS | grep -w key-password | cut -d '=' -f2) >> /opt/opendaylight/etc/custom.properties

#
# Wait for database
#
echo "Waiting for mysql"
until mysql -h dbhost -u root -p${MYSQL_PASSWD} mysql &> /dev/null
do
  printf "."
  sleep 1
done
echo -e "\nmysql ready"

if [ ! -d ${INSTALLED_DIR} ]
then
    mkdir -p ${INSTALLED_DIR}
fi

if [ ! -f ${INSTALLED_DIR}/.installed ]
then
	echo "Installing SDNC-A1 database"
	${SDNC_HOME}/bin/installSdncDb.sh

	if [ -x ${SDNC_HOME}/svclogic/bin/install.sh ]
	then
		echo "Installing directed graphs"
		${SDNC_HOME}/svclogic/bin/install.sh
	fi
fi

nohup python ${SDNC_BIN}/healthcheck.py &

exec ${ODL_HOME}/bin/karaf server
