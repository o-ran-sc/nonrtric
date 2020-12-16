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
#Profile for ONAP guilin release
TEST_ENV_PROFILE="ONAP-GUILIN"

########################################
## Nexus repo settings
########################################

# Nexus repos for developed images
NEXUS_PROXY_REPO="nexus3.onap.org:10001/"
NEXUS_RELEASE_REPO="nexus3.onap.org:10002/"
NEXUS_SNAPSHOT_REPO="nexus3.onap.org:10003/"
NEXUS_STAGING_REPO=$NEXUS_SNAPSHOT_REPO  #staging repo not used in ONAP, using snapshot

# Nexus repos for images used by test (not developed by the project)
NEXUS_RELEASE_REPO_ORAN="nexus3.o-ran-sc.org:10002/" # Only for released ORAN images
NEXUS_RELEASE_REPO_ONAP=$NEXUS_RELEASE_REPO

########################################
# Set up of image and tags for the test.
########################################

# NOTE: One environment variable containing the image name and tag is create by the test script
# for each image from the env variables below.
# The variable is created by removing the suffix "_BASE" from the base image variable name.
# Example: POLICY_AGENT_IMAGE_BASE -> POLICY_AGENT_IMAGE
# This var will point to the local or remote image depending on cmd line arguments.
# In addition, the repo and the image tag version are selected from the list of image tags based on the cmd line argurment.
# For images built by the script, only tag #1 shall be specified
# For project images, only tag #1, #2, #3 and #4 shall be specified
# For ORAN images (non project), only tag #5 shall be specified
# For ONAP images (non project), only tag #6 shall be specified
# For all other images, only tag #7 shall be specified
# 1 XXX_LOCAL: local images: <image-name>:<local-tag>
# 2 XXX_REMOTE_SNAPSHOT: snapshot images: <snapshot-nexus-repo><image-name>:<snapshot-tag>
# 3 XXX_REMOTE: staging images: <staging-nexus-repo><image-name>:<staging-tag>
# 4 XXX_REMOTE_RELEASE: release images: <release-nexus-repo><image-name>:<release-tag>
# 5 XXX_REMOTE_RELEASE_ORAN: ORAN release images: <oran-release-nexus-repo><image-name>:<release-tag>
# 6 XXX_REMOTE_RELEASE_ONAP: ONAP release images: <onap-release-nexus-repo><image-name>:<release-tag>
# 7 XXX_PROXY: other images, not produced by the project: <proxy-nexus-repo><mage-name>:<proxy-tag>


# Policy Agent image and tags
POLICY_AGENT_IMAGE_BASE="onap/ccsdk-oran-a1policymanagementservice"
POLICY_AGENT_IMAGE_TAG_LOCAL="1.0.2-SNAPSHOT"
POLICY_AGENT_IMAGE_TAG_REMOTE_SNAPSHOT="1.0.2-SNAPSHOT"
POLICY_AGENT_IMAGE_TAG_REMOTE="1.0.2-SNAPSHOT" #Will use snapshot repo
POLICY_AGENT_IMAGE_TAG_REMOTE_RELEASE="1.0.1"


# Tag for guilin branch
# SDNC A1 Controller remote image and tag
SDNC_A1_CONTROLLER_IMAGE_BASE="onap/sdnc-image"
SDNC_A1_CONTROLLER_IMAGE_TAG_REMOTE_SNAPSHOT="2.0.5-STAGING-latest"
SDNC_A1_CONTROLLER_IMAGE_TAG_REMOTE="2.0.5-STAGING-latest"
SDNC_A1_CONTROLLER_IMAGE_TAG_REMOTE_RELEASE="2.0.4"   #Will use snapshot repo


#SDNC DB remote image and tag
#The DB is part of SDNC so handled in the same way as SDNC
SDNC_DB_IMAGE_BASE="mysql/mysql-server"
SDNC_DB_IMAGE_TAG_REMOTE_PROXY="5.6"


# Control Panel image and tag - uses bronze release
CONTROL_PANEL_IMAGE_BASE="o-ran-sc/nonrtric-controlpanel"
CONTROL_PANEL_IMAGE_TAG_REMOTE_RELEASE_ORAN="2.0.0"


# Near RT RIC Simulator image and tags - uses bronze release
RIC_SIM_IMAGE_BASE="o-ran-sc/a1-simulator"
RIC_SIM_IMAGE_TAG_REMOTE_RELEASE_ORAN="2.0.0"


#Consul remote image and tag
CONSUL_IMAGE_BASE="consul"
CONSUL_IMAGE_TAG_REMOTE_PROXY="1.7.2"
#No local image for Consul, remote image always used


#CBS remote image and tag
CBS_IMAGE_BASE="onap/org.onap.dcaegen2.platform.configbinding.app-app"
CBS_IMAGE_TAG_REMOTE_RELEASE_ONAP="2.3.0"
#No local image for CBS, remote image always used


#MR stub image and tag
MRSTUB_IMAGE_BASE="mrstub"
MRSTUB_IMAGE_TAG_LOCAL="latest"
#No remote image for MR stub, local image always used


#Callback receiver image and tag
CR_IMAGE_BASE="callback-receiver"
CR_IMAGE_TAG_LOCAL="latest"
#No remote image for CR, local image always used


#Producer stub image and tag
PROD_STUB_IMAGE_BASE="producer-stub"
PROD_STUB_IMAGE_TAG_LOCAL="latest"
#No remote image for producer stub, local image always used


# List of app short names produced by the project
PROJECT_IMAGES_APP_NAMES="PA SDNC"

# List of app short names which images pulled from ORAN
ORAN_IMAGES_APP_NAMES="CP RICSIM"

# List of app short names which images pulled from ONAP
ONAP_IMAGES_APP_NAMES=""   # Not used

########################################
# Detailed settings per app
########################################

# Vars used by docker-compose need to be exported


export DOCKER_SIM_NWNAME="nonrtric-docker-net"                  # Name of docker private network

export POLICY_AGENT_EXTERNAL_PORT=8081                          # Policy Agent container external port (host -> container)
export POLICY_AGENT_INTERNAL_PORT=8081                          # Policy Agent container internal port (container -> container)
export POLICY_AGENT_EXTERNAL_SECURE_PORT=8433                   # Policy Agent container external secure port (host -> container)
export POLICY_AGENT_INTERNAL_SECURE_PORT=8433                   # Policy Agent container internal secure port (container -> container)
export POLICY_AGENT_APIS="V1"                                   # Supported northbound api versions

export POLICY_AGENT_APP_NAME="policy-agent"                     # Name for Policy Agent container
POLICY_AGENT_LOGPATH="/var/log/policy-agent/application.log"    # Path the application log in the Policy Agent container
export POLICY_AGENT_APP_NAME_ALIAS="policy-agent-container"     # Alias name, name used by the control panel

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


########################################
# Setting for common curl-base function
########################################


UUID=""                                                         # UUID used as prefix to the policy id to simulate a real UUID
                                                                # Testscript need to set the UUID to use other this empty prefix is used

RESTBASE="http://localhost:"$POLICY_AGENT_EXTERNAL_PORT         # Base url to the Agent NB REST interface
RESTBASE_SECURE="https://localhost:"$POLICY_AGENT_EXTERNAL_SECURE_PORT # Base url to the secure Agent NB REST interface
DMAAPBASE="http://localhost:"$MR_EXTERNAL_PORT                  # Base url to the Dmaap adapter, http
DMAAPBASE_SECURE="https://localhost:"$MR_EXTERNAL_SECURE_PORT   # Base url to the Dmaap adapter, https
ADAPTER=$RESTBASE                                               # Adapter holds the address the agent R-APP interface (REST OR DMAAP)
                                                                # The values of this var is swiched between the two base url when needed
