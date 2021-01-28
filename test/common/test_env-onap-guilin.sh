#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#    http://www.apache.org/licenses/LICENSE-2.0
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
FLAVOUR="ONAP"

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
POLICY_AGENT_IMAGE_TAG_REMOTE_RELEASE="1.0.2"


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

#Http proxy remote image and tag
HTTP_PROXY_IMAGE_BASE="mitmproxy/mitmproxy"
HTTP_PROXY_IMAGE_TAG_REMOTE_PROXY="6.0.2"
#No local image for SSDNC DB, remote image always used


#ONAP Zookeeper remote image and tag
ONAP_ZOOKEEPER_IMAGE_BASE="onap/dmaap/zookeeper"
ONAP_ZOOKEEPER_IMAGE_TAG_REMOTE_RELEASE_ONAP="6.0.3"
#No local image for ONAP Zookeeper, remote image always used

#ONAP Kafka remote image and tag
ONAP_KAFKA_IMAGE_BASE="onap/dmaap/kafka111"
ONAP_KAFKA_IMAGE_TAG_REMOTE_RELEASE_ONAP="1.0.4"
#No local image for ONAP Kafka, remote image always used

#ONAP DMAAP-MR remote image and tag
ONAP_DMAAPMR_IMAGE_BASE="onap/dmaap/dmaap-mr"
ONAP_DMAAPMR_IMAGE_TAG_REMOTE_RELEASE_ONAP="1.1.18"
#No local image for ONAP DMAAP-MR, remote image always used

# List of app short names produced by the project
PROJECT_IMAGES_APP_NAMES="PA SDNC"

# List of app short names which images pulled from ORAN
ORAN_IMAGES_APP_NAMES="CP RICSIM"

# List of app short names which images pulled from ONAP
ONAP_IMAGES_APP_NAMES=""   # Not used

########################################
# Detailed settings per app
########################################


DOCKER_SIM_NWNAME="nonrtric-docker-net"                  # Name of docker private network

KUBE_NONRTRIC_NAMESPACE="nonrtric"                       # Namespace for all nonrtric components
KUBE_SIM_NAMESPACE="nonrtric-ft"                         # Namespace for simulators (except MR and RICSIM)
KUBE_ONAP_NAMESPACE="onap"                               # Namespace for onap (only message router)

POLICY_AGENT_EXTERNAL_PORT=8081                          # Policy Agent container external port (host -> container)
POLICY_AGENT_INTERNAL_PORT=8081                          # Policy Agent container internal port (container -> container)
POLICY_AGENT_EXTERNAL_SECURE_PORT=8433                   # Policy Agent container external secure port (host -> container)
POLICY_AGENT_INTERNAL_SECURE_PORT=8433                   # Policy Agent container internal secure port (container -> container)
POLICY_AGENT_APIS="V1"                                   # Supported northbound api versions
PMS_VERSION="V1"                                         # Tested version of northbound API
PMS_API_PREFIX=""                                        # api url prefix, only for V2

POLICY_AGENT_APP_NAME="policymanagementservice"          # Name for Policy Agent container
POLICY_AGENT_DISPLAY_NAME="Policy Management Service"
POLICY_AGENT_HOST_MNT_DIR="./mnt"                        # Mounted dir, relative to compose file, on the host
POLICY_AGENT_LOGPATH="/var/log/policy-agent/application.log" # Path the application log in the Policy Agent container
POLICY_AGENT_APP_NAME_ALIAS="policy-agent-container"     # Alias name, name used by the control panel
POLICY_AGENT_CONFIG_KEY="policy-agent"                   # Key for consul config
POLICY_AGENT_PKG_NAME="org.onap.ccsdk.oran.a1policymanagementservice"  # Java base package name
POLICY_AGENT_ACTUATOR="/actuator/loggers/$POLICY_AGENT_PKG_NAME" # Url for trace/debug
POLICY_AGENT_ALIVE_URL="/status"                         # Base path for alive check
POLICY_AGENT_COMPOSE_DIR="policy_agent"                  # Dir in simulator_group for docker-compose
POLICY_AGENT_CONFIG_MOUNT_PATH="/opt/app/policy-agent/config" # Path in container for config file
POLICY_AGENT_DATA_MOUNT_PATH="/opt/app/policy-agent/data" # Path in container for data file
POLICY_AGENT_CONFIG_FILE="application.yaml"              # Container config file name
POLICY_AGENT_DATA_FILE="application_configuration.json"  # Container data file name

MR_DMAAP_APP_NAME="dmaap-mr"                             # Name for the Dmaap MR
MR_STUB_APP_NAME="mr-stub"                               # Name of the MR stub
MR_DMAAP_DISPLAY_NAME="DMAAP Message Router"
MR_STUB_DISPLAY_NAME="Message Router stub"
MR_STUB_CERT_MOUNT_DIR="./cert"
MR_EXTERNAL_PORT=3904                                    # MR dmaap/stub container external port
MR_INTERNAL_PORT=3904                                    # MR dmaap/stub container internal port
MR_EXTERNAL_SECURE_PORT=3905                             # MR dmaap/stub container external secure port
MR_INTERNAL_SECURE_PORT=3905                             # MR dmaap/stub container internal secure port
MR_DMAAP_LOCALHOST_PORT=3904                             # MR stub container external port (host -> container)
MR_STUB_LOCALHOST_PORT=3908                              # MR stub container external port (host -> container)
MR_DMAAP_LOCALHOST_SECURE_PORT=3905                      # MR stub container internal port (container -> container)
MR_STUB_LOCALHOST_SECURE_PORT=3909                       # MR stub container external secure port (host -> container)
MR_READ_URL="/events/A1-POLICY-AGENT-READ/users/policy-agent?timeout=15000&limit=100" # Path to read messages from MR
MR_WRITE_URL="/events/A1-POLICY-AGENT-WRITE"             # Path write messages to MR
MR_READ_TOPIC="A1-POLICY-AGENT-READ"                     # Read topic
MR_WRITE_TOPIC="A1-POLICY-AGENT-WRITE"                   # Write topic
MR_STUB_ALIVE_URL="/"                                    # Base path for mr stub alive check
MR_DMAAP_ALIVE_URL="/topics"                             # Base path for dmaap-mr alive check
MR_DMAAP_COMPOSE_DIR="dmaapmr"                           # Dir in simulator_group for dmaap mr for - docker-compose
MR_STUB_COMPOSE_DIR="mrstub"                             # Dir in simulator_group for mr stub for - docker-compose
MR_KAFKA_APP_NAME="kafka"                                # Kafka app name
MR_ZOOKEEPER_APP_NAME="zookeeper"                        # Zookeeper app name


CR_APP_NAME="callback-receiver"                          # Name for the Callback receiver
CR_DISPLAY_NAME="Callback Reciever"
CR_EXTERNAL_PORT=8090                                    # Callback receiver container external port (host -> container)
CR_INTERNAL_PORT=8090                                    # Callback receiver container internal port (container -> container)
CR_EXTERNAL_SECURE_PORT=8091                             # Callback receiver container external secure port (host -> container)
CR_INTERNAL_SECURE_PORT=8091                             # Callback receiver container internal secure port (container -> container)
CR_APP_CALLBACK="/callbacks"                             # Url for callbacks
CR_ALIVE_URL="/"                                         # Base path for alive check
CR_COMPOSE_DIR="cr"                                      # Dir in simulator_group for docker-compose

CONSUL_HOST="consul-server"                              # Host name of consul
CONSUL_DISPLAY_NAME="Consul"
CONSUL_EXTERNAL_PORT=8500                                # Consul container external port (host -> container)
CONSUL_INTERNAL_PORT=8500                                # Consul container internal port (container -> container)
CONSUL_APP_NAME="polman-consul"                          # Name for consul container
CONSUL_ALIVE_URL="/ui/dc1/kv"                            # Base path for alive check
CONSUL_CBS_COMPOSE_DIR="consul_cbs"                      # Dir in simulator group for docker compose

CBS_APP_NAME="polman-cbs"                                # Name for CBS container
CBS_DISPLAY_NAME="Config Binding Service"
CBS_EXTERNAL_PORT=10000                                  # CBS container external port (host -> container)
CBS_INTERNAL_PORT=10000                                  # CBS container internal port (container -> container)
CONFIG_BINDING_SERVICE="config-binding-service"          # Host name of CBS
CBS_ALIVE_URL="/healthcheck"                             # Base path for alive check

RIC_SIM_DISPLAY_NAME="Near-RT RIC A1 Simulator"
RIC_SIM_BASE="g"                                         # Base name of the RIC Simulator container, shall be the group code
                                                         # Note, a prefix is added to each container name by the .env file in the 'ric' dir
RIC_SIM_PREFIX="ricsim"                                  # Prefix added to ric container name, added in the .env file in the 'ric' dir
                                                         # This prefix can be changed from the command line
RIC_SIM_INTERNAL_PORT=8085                               # RIC Simulator container internal port (container -> container).
                                                         # (external ports allocated by docker)
RIC_SIM_INTERNAL_SECURE_PORT=8185                        # RIC Simulator container internal secure port (container -> container).
                                                         # (external ports allocated by docker)
RIC_SIM_CERT_MOUNT_DIR="./cert"
RIC_SIM_COMPOSE_DIR="ric"                                # Dir in simulator group for docker compose
RIC_SIM_ALIVE_URL="/"

SDNC_APP_NAME="a1controller"                             # Name of the SNDC A1 Controller container
SDNC_DISPLAY_NAME="SDNC A1 Controller"
SDNC_EXTERNAL_PORT=8282                                  # SNDC A1 Controller container external port (host -> container)
SDNC_INTERNAL_PORT=8181                                  # SNDC A1 Controller container internal port (container -> container)
SDNC_EXTERNAL_SECURE_PORT=8443                           # SNDC A1 Controller container external securee port (host -> container)
SDNC_INTERNAL_SECURE_PORT=8443                           # SNDC A1 Controller container internal secure port (container -> container)
SDNC_DB_APP_NAME="sdncdb"                                # Name of the SDNC DB container
SDNC_A1_TRUSTSTORE_PASSWORD="a1adapter"                  # SDNC truststore password
SDNC_USER="admin"                                        # SDNC username
SDNC_PWD="Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"   # SNDC PWD
SDNC_API_URL="/restconf/operations/A1-ADAPTER-API:"      # Base url path for SNDC API
SDNC_ALIVE_URL="/apidoc/explorer/"                       # Base url path for SNDC API docs (for alive check)
SDNC_COMPOSE_DIR="sdnc"                                  # Dir in simulator_group for docker-compose
SDNC_KARAF_LOG="/opt/opendaylight/data/log/karaf.log"    # Path to karaf log


CONTROL_PANEL_APP_NAME="controlpanel"                    # Name of the Control Panel container
CONTROL_PANEL_DISPLAY_NAME="Non-RT RIC Control Panel"
CONTROL_PANEL_EXTERNAL_PORT=8080                         # Control Panel container external port (host -> container)
CONTROL_PANEL_INTERNAL_PORT=8080                         # Control Panel container internal port (container -> container)
CONTROL_PANEL_EXTERNAL_SECURE_PORT=8880                  # Control Panel container external port (host -> container)
CONTROL_PANEL_INTERNAL_SECURE_PORT=8082                  # Control Panel container internal port (container -> container)
CONTROL_PANEL_LOGPATH="/logs/nonrtric-controlpanel.log"  # Path the application log in the Control Panel container
CONTROL_PANEL_ALIVE_URL="/"                              # Base path for alive check
CONTROL_PANEL_COMPOSE_DIR="control_panel"                # Dir in simulator_group for docker-compose
CONTROL_PANEL_CONFIG_MOUNT_PATH=/maven                   # Container internal path for config
CONTROL_PANEL_CONFIG_FILE=application.properties         # Config file name

HTTP_PROXY_APP_NAME="httpproxy"                          # Name of the Http Proxy container
HTTP_PROXY_DISPLAY_NAME="Http Proxy"
HTTP_PROXY_EXTERNAL_PORT=8780                            # Http Proxy container external port (host -> container)
HTTP_PROXY_INTERNAL_PORT=8080                            # Http Proxy container internal port (container -> container)
HTTP_PROXY_WEB_EXTERNAL_PORT=8781                        # Http Proxy container external port (host -> container)
HTTP_PROXY_WEB_INTERNAL_PORT=8081                        # Http Proxy container internal port (container -> container)
HTTP_PROXY_CONFIG_PORT=0                                 # Port number for proxy config, will be set if proxy is started
HTTP_PROXY_CONFIG_HOST_NAME=""                           # Proxy host, will be set if proxy is started
HTTP_PROXY_ALIVE_URL="/"                                 # Base path for alive check
HTTP_PROXY_COMPOSE_DIR="httpproxy"                       # Dir in simulator_group for docker-compose

########################################
# Setting for common curl-base function
########################################


UUID=""                                                  # UUID used as prefix to the policy id to simulate a real UUID
                                                         # Testscript need to set the UUID to use other this empty prefix is used

