#!/usr/bin/env bash

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

# Set up the image and tags for the test. Do not add the image tag to the image names.

# NOTE: A env var for each container is created by the test script.
# This var will point to the local or remote var depending on how
# the test script is started. The name format is <container-name>_IMAGE, ie with 'LOCAL' or 'REMOTE'.

# Local Policy Agent image and tag
POLICY_AGENT_LOCAL_IMAGE="o-ran-sc/nonrtric-policy-agent"
POLICY_AGENT_LOCAL_IMAGE_TAG="1.0.0-SNAPSHOT"
# Remote Policy Agent image and tag
POLICY_AGENT_REMOTE_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-policy-agent"
POLICY_AGENT_REMOTE_IMAGE_TAG="1.0.0"


# Control Panel local image and tag
CONTROL_PANEL_LOCAL_IMAGE="o-ran-sc/nonrtric-controlpanel"
CONTROL_PANEL_LOCAL_IMAGE_TAG="1.0.0-SNAPSHOT"
# Control Panel remote image and tag
CONTROL_PANEL_REMOTE_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-controlpanel"
CONTROL_PANEL_REMOTE_IMAGE_TAG="1.0.0"


# SDNC A1 Controller local image and tag
SDNC_A1_CONTROLLER_LOCAL_IMAGE="o-ran-sc/nonrtric-a1-controller"
SDNC_A1_CONTROLLER_LOCAL_IMAGE_TAG="1.0.0-SNAPSHOT"
# SDNC A1 Controller remote image and tag
SDNC_A1_CONTROLLER_REMOTE_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-a1-controller"
SDNC_A1_CONTROLLER_REMOTE_IMAGE_TAG="1.7.4"


#SDNC DN remote image and tag
SDNC_DB_REMOTE_IMAGE="mysql/mysql-server"
SDNC_DB_REMOTE_IMAGE_TAG="5.6"
#No local image for DB, remote image always used


# Near RT RIC Simulator local image and tag
RIC_SIM_LOCAL_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/a1-simulator"
RIC_SIM_LOCAL_IMAGE_TAG="latest"
# Near RT RIC Simulator remote image and tag
RIC_SIM_REMOTE_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/a1-simulator"
RIC_SIM_REMOTE_IMAGE_TAG="1.0.1"


#Consul remote image and tag
CONSUL_REMOTE_IMAGE="consul"
CONSUL_REMOTE_IMAGE_TAG="1.7.2"
#No local image for Consul, remote image always used


#CBS remote image and tag
CBS_REMOTE_IMAGE="nexus3.onap.org:10001/onap/org.onap.dcaegen2.platform.configbinding.app-app"
CBS_REMOTE_IMAGE="onap/org.onap.dcaegen2.platform.configbinding.app-app"
CBS_REMOTE_IMAGE_TAG="2.3.0"
#No local image for CBS, remote image always used


#MR stub image and tag
MRSTUB_LOCAL_IMAGE="mrstub"
MRSTUB_LOCAL_IMAGE_TAG="latest"
#No remote image for MR stub, local image always used

#Callback receiver image and tag
CR_LOCAL_IMAGE="callback-receiver"
CR_LOCAL_IMAGE_TAG="latest"
#No remote image for CR, local image always used

# Common env var for auto-test. Vars used by docker-compose need to be exported
export DOCKER_SIM_NWNAME="nonrtric-docker-net"                  # Name of docker private network

export POLICY_AGENT_EXTERNAL_PORT=8081                          # Policy Agent container external port (host -> container)
export POLICY_AGENT_INTERNAL_PORT=8081                          # Policy Agent container internal port (container -> container)
export POLICY_AGENT_APP_NAME="policy-agent"                     # Name for Policy Agent container
POLICY_AGENT_LOGPATH="/var/log/policy-agent/application.log"    # Path the application log in the Policy Agent container

export MR_EXTERNAL_PORT=3905                                    # MR stub container external port (host -> container)
export MR_INTERNAL_PORT=3905                                    # MR stub container internal port (container -> container)
export MR_APP_NAME="message-router"                             # Name for the MR

export CR_EXTERNAL_PORT=8090                                    # Callback receiver container external port (host -> container)
export CR_INTERNAL_PORT=8090                                    # Callback receiver container internal port (container -> container)
export CR_APP_NAME="callback-receiver"                          # Name for the Callback receiver

export CONSUL_HOST="consul-server"                              # Host name of consul
export CONSUL_EXTERNAL_PORT=8500                                # Consul container external port (host -> container)
export CONSUL_INTERNAL_PORT=8500                                # Consul container internal port (container -> container)
export CONSUL_APP_NAME="polman-consul"                          # Name for consul container

export CBS_APP_NAME="polman-cbs"                                # Name for CBS container
export CBS_EXTERNAL_PORT=10000                                  # CBS container external port (host -> container)
export CBS_INTERNAL_PORT=10000                                  # CBS container internal port (container -> container)
export CONFIG_BINDING_SERVICE="config-binding-service"          # Host name of CBS

export RIC_SIM_BASE="g"                                         # Base name of the RIC Simulator container, shall be the group code
                                                                # Note, a prefix is added to each container name by the .env file in the 'ric' dir
RIC_SIM_PREFIX="ricsim"                                         # Prefix added to ric container name, added in the .env file in the 'ric' dir
export RIC_SIM_INTERNAL_PORT=8085                               # RIC Simulator container internal port (container -> container).
                                                                # (externl ports allocated by docker)

export SDNC_APP_NAME="a1-controller"                            # Name of the SNDC A1 Controller container
export SDNC_EXTERNAL_PORT=8282                                  # SNDC A1 Controller container external port (host -> container)
export SDNC_INTERNAL_PORT=8181                                  # SNDC A1 Controller container internal port (container -> container)
export SDNC_DB_APP_NAME="sdnc-db"                               # Name of the SDNC DB container
SDNC_USER="admin"
SDNC_PWD="Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"

export CONTROL_PANEL_APP_NAME="control-panel"                   # Name of the Control Panel container
export CONTROL_PANEL_EXTERNAL_PORT=8080                         # Control Panel container external port (host -> container)
export CONTROL_PANEL_INTERNAL_PORT=8080                         # Control Panel container external port (host -> container)
CONTROL_PANEL_LOGPATH="/logs/nonrtric-controlpanel.log"         # Path the application log in the Control Panel container

RESTBASE="http://localhost:"$POLICY_AGENT_EXTERNAL_PORT         # Base url to the R-APP interface
DMAAPBASE="http://localhost:"$MR_EXTERNAL_PORT                  # Base url to the Dmaap adapter
ADAPTER=$RESTBASE                                               # Adapter holds the address the agent R-APP interface (REST OR DMAAP)
                                                                # The values of this var is swiched between the two base url when needed