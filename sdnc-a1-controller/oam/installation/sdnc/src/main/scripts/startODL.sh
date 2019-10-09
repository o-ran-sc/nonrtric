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

# Append features to karaf boot feature configuration
# $1 additional feature to be added
# $2 repositories to be added (optional)
function addToFeatureBoot() {
  CFG=$ODL_HOME/etc/org.apache.karaf.features.cfg
  ORIG=$CFG.orig
  if [ -n "$2" ] ; then
    echo "Add repository: $2"
    mv $CFG $ORIG
    cat $ORIG | sed -e "\|featuresRepositories|s|$|,$2|" > $CFG
  fi
  echo "Add boot feature: $1"
  mv $CFG $ORIG
  cat $ORIG | sed -e "\|featuresBoot *=|s|$|,$1|" > $CFG
}

# Append features to karaf boot feature configuration
# $1 search pattern
# $2 replacement
function replaceFeatureBoot() {
  CFG=$ODL_HOME/etc/org.apache.karaf.features.cfg
  ORIG=$CFG.orig
  echo "Replace boot feature $1 with: $2"
  sed -i "/featuresBoot/ s/$1/$2/g" $CFG
}

function install_sdnrwt_features() {
  addToFeatureBoot "$SDNRWT_BOOTFEATURES" $SDNRWT_REPOSITORY
}

function install_sdnr_northbound_features() {
  addToFeatureBoot "$SDNR_NORTHBOUND_BOOTFEATURES" $SDNR_NORTHBOUND_REPOSITORY
}

function enable_odl_cluster(){
  if [ -z $SDNC_REPLICAS ]; then
     echo "SDNC_REPLICAS is not configured in Env field"
     exit
  fi

  echo "Installing Opendaylight cluster features"
  replaceFeatureBoot odl-netconf-topology odl-netconf-clustered-topology
  replaceFeatureBoot odl-mdsal-all odl-mdsal-all,odl-mdsal-clustering
  addToFeatureBoot odl-jolokia
  #${ODL_HOME}/bin/client feature:install odl-mdsal-clustering
  #${ODL_HOME}/bin/client feature:install odl-jolokia

  echo "Update cluster information statically"
  hm=$(hostname)
  echo "Get current Hostname ${hm}"

  node=($(echo ${hm} | tr '-' '\n'))
  node_name=${node[0]}
  node_index=${node[1]}

  if [ -z $PEER_ODL_CLUSTER ]; then
    echo "This is a local cluster"
    node_list="${node_name}-0.sdnhost-cluster.onap.svc.cluster.local";

    for ((i=1;i<${SDNC_REPLICAS};i++));
    do
      node_list="${node_list} ${node_name}-$i.sdnhost-cluster.onap.svc.cluster.local"
    done
    /opt/opendaylight/current/bin/configure_cluster.sh $((node_index+1)) ${node_list}
  else
    echo "This is a Geo cluster"

    if $IS_PRIMARY_CLUSTER; then
       PRIMARY_NODE=${MY_ODL_CLUSTER}
       SECONDARY_NODE=${PEER_ODL_CLUSTER}
    else
       PRIMARY_NODE=${PEER_ODL_CLUSTER}
       SECONDARY_NODE=${MY_ODL_CLUSTER}
       member_offset=4
    fi

    node_list="${PRIMARY_NODE} ${SECONDARY_NODE}"
    /opt/onap/sdnc/bin/configure_geo_cluster.sh $((node_index+member_offset)) ${node_list}
  fi
}


# Install SDN-C platform components if not already installed and start container

ODL_HOME=${ODL_HOME:-/opt/opendaylight/current}
ODL_ADMIN_USERNAME=${ODL_ADMIN_USERNAME:-admin}
ODL_ADMIN_PASSWORD=${ODL_ADMIN_PASSWORD:-Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U}
SDNC_HOME=${SDNC_HOME:-/opt/onap/sdnc}
SDNC_BIN=${SDNC_BIN:-/opt/onap/sdnc/bin}
CCSDK_HOME=${CCSDK_HOME:-/opt/onap/ccsdk}
SLEEP_TIME=${SLEEP_TIME:-120}
MYSQL_PASSWD=${MYSQL_PASSWD:-openECOMP1.0}
ENABLE_ODL_CLUSTER=${ENABLE_ODL_CLUSTER:-false}
IS_PRIMARY_CLUSTER=${IS_PRIMARY_CLUSTER:-false}
MY_ODL_CLUSTER=${MY_ODL_CLUSTER:-127.0.0.1}
INSTALLED_DIR=${INSTALLED_FILE:-/opt/opendaylight/current/daexim}
SDNRWT=${SDNRWT:-false}
SDNRWT_BOOTFEATURES=${SDNRWT_BOOTFEATURES:-sdnr-wt-feature-aggregator}
SDNR_NORTHBOUND=${SDNR_NORTHBOUND:-false}
SDNR_NORTHBOUND_BOOTFEATURES=${SDNR_NORTHBOUND_BOOTFEATURES:-sdnr-northbound-all}
export ODL_ADMIN_PASSWORD ODL_ADMIN_USERNAME

echo "Settings:"
echo "  ENABLE_ODL_CLUSTER=$ENABLE_ODL_CLUSTER"
echo "  SDNC_REPLICAS=$SDNC_REPLICAS"
echo "  SDNRWT=$SDNRWT"
echo "  SDNR_NORTHBOUND=$SDNR_NORTHBOUND"


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
	echo "Installing SDN-C database"
	${SDNC_HOME}/bin/installSdncDb.sh
	echo "Installing SDN-C keyStore"
	${SDNC_HOME}/bin/addSdncKeyStore.sh

	#${CCSDK_HOME}/bin/installOdlHostKey.sh

	if [ -x ${SDNC_HOME}/svclogic/bin/install.sh ]
	then
		echo "Installing directed graphs"
		${SDNC_HOME}/svclogic/bin/install.sh
	fi

    if $ENABLE_ODL_CLUSTER ; then enable_odl_cluster ; fi

	if $SDNRWT ; then install_sdnrwt_features ; fi

  if $SDNR_NORTHBOUND ; then install_sdnr_northbound_features ; fi

	echo "Installed at `date`" > ${INSTALLED_DIR}/.installed
fi

cp /opt/opendaylight/current/certs/* /tmp

nohup python ${SDNC_BIN}/installCerts.py &

exec ${ODL_HOME}/bin/karaf server
