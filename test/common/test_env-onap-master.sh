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

TEST_ENV_PROFILE="ONAP-MASTER"

# Set up the image and tags for the test. Do not add the image tag to the image names.

# NOTE: A env var for each container is created by the test script.
# This var will point to the local or remote var depending on how
# the test script is started. The name format is <container-name>_IMAGE, ie with 'LOCAL' or 'REMOTE'.

# Local Policy Agent image and tag
POLICY_AGENT_LOCAL_IMAGE="onap/ccsdk-oran-a1policymanagementservice"
POLICY_AGENT_LOCAL_IMAGE_TAG="1.1.0-SNAPSHOT"
# Remote Policy Agent image and tag
POLICY_AGENT_REMOTE_IMAGE="nexus3.onap.org:10003/onap/ccsdk-oran-a1policymanagementservice"
POLICY_AGENT_REMOTE_IMAGE_TAG="1.1.0-SNAPSHOT"

# Local ECS image and tag
ECS_LOCAL_IMAGE="o-ran-sc/nonrtric-enrichment-coordinator-service"
ECS_LOCAL_IMAGE_TAG="1.0.0-SNAPSHOT"
# Remote ECS image and tag
ECS_REMOTE_IMAGE="nexus3.o-ran-sc.org:10003/o-ran-sc/nonrtric-enrichment-coordinator-service"
ECS_REMOTE_IMAGE_TAG="1.0.0-SNAPSHOT"

# Control Panel local image and tag
CONTROL_PANEL_LOCAL_IMAGE="o-ran-sc/nonrtric-controlpanel"
CONTROL_PANEL_LOCAL_IMAGE_TAG="2.0.0-SNAPSHOT"
# Control Panel remote image and tag
CONTROL_PANEL_REMOTE_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-controlpanel"
CONTROL_PANEL_REMOTE_IMAGE_TAG="2.0.0"


# SDNC A1 Controller remote image and tag
SDNC_A1_CONTROLLER_REMOTE_IMAGE="nexus3.onap.org:10003/onap/sdnc-image"
SDNC_A1_CONTROLLER_REMOTE_IMAGE_TAG="2.1.0-STAGING-latest"


#SDNC DB remote image and tag
SDNC_DB_REMOTE_IMAGE="mysql/mysql-server"
SDNC_DB_REMOTE_IMAGE_TAG="5.6"
#No local image for DB, remote image always used

# Near RT RIC Simulator local image and tag
RIC_SIM_LOCAL_IMAGE="o-ran-sc/a1-simulator"
RIC_SIM_LOCAL_IMAGE_TAG="latest"
# Near RT RIC Simulator remote image and tag
RIC_SIM_REMOTE_IMAGE="nexus3.o-ran-sc.org:10004/o-ran-sc/a1-simulator"
RIC_SIM_REMOTE_IMAGE_TAG="2.1.0"


#Consul remote image and tag
CONSUL_REMOTE_IMAGE="consul"
CONSUL_REMOTE_IMAGE_TAG="1.7.2"
#No local image for Consul, remote image always used


#CBS remote image and tag
CBS_REMOTE_IMAGE="nexus3.onap.org:10001/onap/org.onap.dcaegen2.platform.configbinding.app-app"
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

#Producer stub image and tag
PROD_STUB_LOCAL_IMAGE="producer-stub"
PROD_STUB_LOCAL_IMAGE_TAG="latest"
#No remote image for producer stub, local image always used

# Common env var for auto-test. Vars used by docker-compose need to be exported
export DOCKER_SIM_NWNAME="nonrtric-docker-net"                  # Name of docker private network

export POLICY_AGENT_EXTERNAL_PORT=8081                          # Policy Agent container external port (host -> container)
export POLICY_AGENT_INTERNAL_PORT=8081                          # Policy Agent container internal port (container -> container)
export POLICY_AGENT_EXTERNAL_SECURE_PORT=8433                   # Policy Agent container external secure port (host -> container)
export POLICY_AGENT_INTERNAL_SECURE_PORT=8433                   # Policy Agent container internal secure port (container -> container)
export POLICY_AGENT_APIS="V1 V2"                                # Supported northbound api versions
export PMS_VERSION="V2"

export POLICY_AGENT_APP_NAME="policy-agent"                     # Name for Policy Agent container
POLICY_AGENT_LOGPATH="/var/log/policy-agent/application.log"    # Path the application log in the Policy Agent container
export POLICY_AGENT_APP_NAME_ALIAS="policy-agent-container"     # Alias name, name used by the control panel

export ECS_EXTERNAL_PORT=8083                                   # ECS container external port (host -> container)
export ECS_INTERNAL_PORT=8083                                   # ECS container internal port (container -> container)
export ECS_EXTERNAL_SECURE_PORT=8434                            # ECS container external secure port (host -> container)
export ECS_INTERNAL_SECURE_PORT=8434                            # ECS container internal secure port (container -> container)

export ECS_APP_NAME="ecs"                                       # Name for ECS container
ECS_LOGPATH="/var/log/enrichment-coordinator-service/application.log" # Path the application log in the ECS container
export ECS_APP_NAME_ALIAS="enrichment-service-container"        # Alias name, name used by the control panel
export ECS_HOST_MNT_DIR="./mnt"                                 # Mounted dir, relative to compose file, on the host
export ECS_CONTAINER_MNT_DIR="/var/enrichment-coordinator-service" # Mounted dir in the container

export MR_EXTERNAL_PORT=3905                                    # MR stub container external port (host -> container)
export MR_INTERNAL_PORT=3905                                    # MR stub container internal port (container -> container)
export MR_EXTERNAL_SECURE_PORT=3906                             # MR stub container external secure port (host -> container)
export MR_INTERNAL_SECURE_PORT=3906                             # MR stub container internal secure port (container -> container)
export MR_APP_NAME="message-router"                             # Name for the MR
export MR_READ_URL="/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=15000&limit=100" # Path to read messages from MR
export MR_WRITE_URL="/events/A1-POLICY-AGENT-WRITE"             # Path write messages to MR

export CR_EXTERNAL_PORT=8090                                    # Callback receiver container external port (host -> container)
export CR_INTERNAL_PORT=8090                                    # Callback receiver container internal port (container -> container)
export CR_EXTERNAL_SECURE_PORT=8091                             # Callback receiver container external secure port (host -> container)
export CR_INTERNAL_SECURE_PORT=8091                             # Callback receiver container internal secure port (container -> container)
export CR_APP_NAME="callback-receiver"                          # Name for the Callback receiver
export CR_APP_CALLBACK="/callbacks"                             # Url for callbacks

export PROD_STUB_EXTERNAL_PORT=8092                             # Producer stub container external port (host -> container)
export PROD_STUB_INTERNAL_PORT=8092                             # Producer stub container internal port (container -> container)
export PROD_STUB_EXTERNAL_SECURE_PORT=8093                      # Producer stub container external secure port (host -> container)
export PROD_STUB_INTERNAL_SECURE_PORT=8093                      # Producer stub container internal secure port (container -> container)
export PROD_STUB_APP_NAME="producer-stub"                       # Name for the Producer stub

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
                                                                # This prefix can be changed from the command line
export RIC_SIM_INTERNAL_PORT=8085                               # RIC Simulator container internal port (container -> container).
                                                                # (external ports allocated by docker)
export RIC_SIM_INTERNAL_SECURE_PORT=8185                        # RIC Simulator container internal secure port (container -> container).
                                                                # (external ports allocated by docker)

export SDNC_APP_NAME="a1-controller"                            # Name of the SNDC A1 Controller container
export SDNC_EXTERNAL_PORT=8282                                  # SNDC A1 Controller container external port (host -> container)
export SDNC_INTERNAL_PORT=8181                                  # SNDC A1 Controller container internal port (container -> container)
export SDNC_EXTERNAL_SECURE_PORT=8443                           # SNDC A1 Controller container external securee port (host -> container)
export SDNC_INTERNAL_SECURE_PORT=8443                           # SNDC A1 Controller container internal secure port (container -> container)
export SDNC_DB_APP_NAME="sdnc-db"                               # Name of the SDNC DB container
export SDNC_A1_TRUSTSTORE_PASSWORD="a1adapter"                  # SDNC truststore password
SDNC_USER="admin"                                               # SDNC username
SDNC_PWD="Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"          # SNDC PWD
SDNC_API_URL="/restconf/operations/A1-ADAPTER-API:"             # Base url path for SNDC API
SDNC_ALIVE_URL="/apidoc/explorer/"                              # Base url path for SNDC API docs (for alive check)
SDNC_KARAF_LOG="/opt/opendaylight/data/log/karaf.log"           # Path to karaf log


export CONTROL_PANEL_APP_NAME="control-panel"                   # Name of the Control Panel container
export CONTROL_PANEL_EXTERNAL_PORT=8080                         # Control Panel container external port (host -> container)
export CONTROL_PANEL_INTERNAL_PORT=8080                         # Control Panel container external port (host -> container)
CONTROL_PANEL_LOGPATH="/logs/nonrtric-controlpanel.log"         # Path the application log in the Control Panel container

UUID=""                                                         # UUID used as prefix to the policy id to simulate a real UUID
                                                                # Testscript need to set the UUID to use other this empty prefix is used

RESTBASE="http://localhost:"$POLICY_AGENT_EXTERNAL_PORT         # Base url to the Agent NB REST interface
RESTBASE_SECURE="https://localhost:"$POLICY_AGENT_EXTERNAL_SECURE_PORT # Base url to the secure Agent NB REST interface
DMAAPBASE="http://localhost:"$MR_EXTERNAL_PORT                  # Base url to the Dmaap adapter, http
DMAAPBASE_SECURE="https://localhost:"$MR_EXTERNAL_SECURE_PORT   # Base url to the Dmaap adapter, https
ADAPTER=$RESTBASE                                               # Adapter holds the address the agent R-APP interface (REST OR DMAAP)
                                                                # The values of this var is swiched between the two base url when needed
                                                                # The values of this var is swiched between the four base url when needed

ECS_RESTBASE="http://localhost:"$ECS_EXTERNAL_PORT              # Base url to the ECS NB REST interface
ECS_RESTBASE_SECURE="https://localhost:"$ECS_EXTERNAL_SECURE_PORT # Base url to the secure ECS NB REST interface
ECS_DMAAPBASE="http://localhost:"$MR_EXTERNAL_PORT              # Base url to the Dmaap adapter, http
ECS_DMAAPBASE_SECURE="https://localhost:"$MR_EXTERNAL_SECURE_PORT   # Base url to the Dmaap adapter, https
ECS_ADAPTER=$ECS_RESTBASE                                       # Adapter holds the address the ECS R-APP interface (REST OR DMAAP)
                                                                # The values of this var is swiched between the four base url when needed

CR_RESTBASE="http://localhost:"$CR_EXTERNAL_PORT                # Base url to the Callback receiver REST interface
CR_RESTBASE_SECURE="https://localhost:"$CR_EXTERNAL_SECURE_PORT # Base url to the secure Callback receiver REST interface
CR_ADAPTER=$CR_RESTBASE                                         # Adapter holds the address the CR admin interface (REST only)
                                                                # The values of this var is swiched between the two base url when needed