#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023 OpenInfra Foundation Europe. All rights reserved.
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
#Profile for ONAP montreal release
TEST_ENV_PROFILE="ONAP-MONTREAL"
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
# Example: A1PMS_IMAGE_BASE -> A1PMS_IMAGE
# This var will point to the local or remote image depending on cmd line arguments.
# In addition, the repo and the image tag version are selected from the list of image tags based on the cmd line argument.
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

#############################################################################
# Note:
# The image tags for a1pms and sdnc are updated AFTER the release.
# This means that the latest staging/snapshot images for these two components have
# version one step (0.0.1 - bug-level) higher than the
# latest release image version.

# This is only applicable for ONAP images
#############################################################################

# A1PMS image and tags
A1PMS_IMAGE_BASE="onap/ccsdk-oran-a1policymanagementservice"
A1PMS_IMAGE_TAG_LOCAL="1.6.4-SNAPSHOT"
A1PMS_IMAGE_TAG_REMOTE_SNAPSHOT="1.6.4-SNAPSHOT"
A1PMS_IMAGE_TAG_REMOTE="1.6.4-STAGING-latest" #Will use snapshot repo
A1PMS_IMAGE_TAG_REMOTE_RELEASE="1.6.3"

# SDNC A1 Controller remote image and tag
SDNC_A1_CONTROLLER_IMAGE_BASE="onap/sdnc-image"
SDNC_A1_CONTROLLER_IMAGE_TAG_LOCAL="2.5.1-SNAPSHOT" ###CHECK THIS
SDNC_A1_CONTROLLER_IMAGE_TAG_REMOTE_SNAPSHOT="2.5.1-STAGING-latest"
SDNC_A1_CONTROLLER_IMAGE_TAG_REMOTE="2.5.1-STAGING-latest"  #Will use snapshot repo
SDNC_A1_CONTROLLER_IMAGE_TAG_REMOTE_RELEASE="2.5.1"

#SDNC DB remote image and tag
#The DB is part of SDNC so handled in the same way as SDNC
SDNC_DB_IMAGE_BASE="mariadb"
SDNC_DB_IMAGE_TAG_REMOTE_PROXY="10.5"

# ICS image and tag - using i release
ICS_IMAGE_BASE="o-ran-sc/nonrtric-plt-informationcoordinatorservice"
ICS_IMAGE_TAG_REMOTE_RELEASE_ORAN="1.6.0"
#Note: Update var ICS_FEATURE_LEVEL if image version is changed

# Control Panel image and tag - using i release
CONTROL_PANEL_IMAGE_BASE="o-ran-sc/nonrtric-controlpanel"
CONTROL_PANEL_IMAGE_TAG_REMOTE_RELEASE_ORAN="2.5.0"

# Gateway image and tags - used i release
NRT_GATEWAY_IMAGE_BASE="o-ran-sc/nonrtric-gateway"
NRT_GATEWAY_IMAGE_TAG_REMOTE_RELEASE_ORAN="1.2.0"

# RAPP Catalogue image and tags - used i release
RAPP_CAT_IMAGE_BASE="o-ran-sc/nonrtric-plt-rappcatalogue"
RAPP_CAT_IMAGE_TAG_REMOTE_RELEASE_ORAN="1.2.0"


# Near RT RIC Simulator image and tags - used i release
RIC_SIM_IMAGE_BASE="o-ran-sc/a1-simulator"
RIC_SIM_IMAGE_TAG_REMOTE_RELEASE_ORAN="2.6.1"

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


#Http proxy remote image and tag
HTTP_PROXY_IMAGE_BASE="nodejs-http-proxy"
HTTP_PROXY_IMAGE_TAG_LOCAL="latest"
#No local image for http proxy, remote image always used

#ONAP Zookeeper remote image and tag
ONAP_ZOOKEEPER_IMAGE_BASE="onap/dmaap/zookeeper"
ONAP_ZOOKEEPER_IMAGE_TAG_REMOTE_RELEASE_ONAP="6.1.0"
#No local image for ONAP Zookeeper, remote image always used

#ONAP Kafka remote image and tag
ONAP_KAFKA_IMAGE_BASE="onap/dmaap/kafka111"
ONAP_KAFKA_IMAGE_TAG_REMOTE_RELEASE_ONAP="1.1.1"
#No local image for ONAP Kafka, remote image always used

#ONAP DMAAP-MR remote image and tag
ONAP_DMAAPMR_IMAGE_BASE="onap/dmaap/dmaap-mr"
ONAP_DMAAPMR_IMAGE_TAG_REMOTE_RELEASE_ONAP="1.3.0"
#No local image for ONAP DMAAP-MR, remote image always used

#Kube proxy remote image and tag
KUBE_PROXY_IMAGE_BASE="nodejs-kube-proxy"
KUBE_PROXY_IMAGE_TAG_LOCAL="latest"
#No remote image for kube proxy, local image always used

#Kube proxy remote image and tag
PVC_CLEANER_IMAGE_BASE="ubuntu"
PVC_CLEANER_IMAGE_TAG_REMOTE_PROXY="20.10"
#No local image for pvc cleaner, remote image always used

# List of app short names produced by the project
PROJECT_IMAGES_APP_NAMES="A1PMS SDNC"

# List of app short names which images pulled from ORAN
ORAN_IMAGES_APP_NAMES="CP ICS RICSIM RC NGW"

# List of app short names which images pulled from ONAP
ONAP_IMAGES_APP_NAMES=""   # Not used


########################################
# Detailed settings per app
########################################


DOCKER_SIM_NWNAME="nonrtric-docker-net"                  # Name of docker private network

KUBE_NONRTRIC_NAMESPACE="nonrtric"                       # Namespace for all nonrtric components
KUBE_SIM_NAMESPACE="nonrtric-ft"                         # Namespace for simulators (except MR and RICSIM)
KUBE_A1SIM_NAMESPACE="a1-sim"                            # Namespace for a1-p simulators (RICSIM)
KUBE_ONAP_NAMESPACE="onap"                               # Namespace for onap (only message router)
KUBE_SDNC_NAMESPACE="onap"                               # Namespace for sdnc

A1PMS_EXTERNAL_PORT=8081                                   # A1PMS container external port (host -> container)
A1PMS_INTERNAL_PORT=8081                                   # A1PMS container internal port (container -> container)
A1PMS_EXTERNAL_SECURE_PORT=8433                            # A1PMS container external secure port (host -> container)
A1PMS_INTERNAL_SECURE_PORT=8433                            # A1PMS container internal secure port (container -> container)
A1PMS_APIS="V1 V2"                                         # Supported northbound api versions
A1PMS_VERSION="V2"                                         # Tested version of northbound API
# A1PMS_V3="V3"                                            # To be used this property in further env releases
A1PMS_API_PREFIX="/a1-policy"                              # api url prefix, only for V2. Shall contain leading "/"
# A1PMS_API_PREFIX_V3="/a1policymanagement"                # To be used this property in further env releases
# A1PMS_V3_FLAG="true"                                       # To be used this property in further env releases

A1PMS_APP_NAME="policymanagementservice"                   # Name for A1PMS container
A1PMS_DISPLAY_NAME="Policy Management Service"
A1PMS_HOST_MNT_DIR="./mnt"                                 # Mounted dir, relative to compose file, on the host
A1PMS_LOGPATH="/var/log/policy-agent/application.log"      # Path the application log in the A1PMS container
A1PMS_APP_NAME_ALIAS="policy-agent-container"              # Alias name, name used by the control panel
A1PMS_CONFIG_KEY="policy-agent"                            # Key for consul config
A1PMS_PKG_NAME="org.onap.ccsdk.oran.a1policymanagementservice"  # Java base package name
A1PMS_ACTUATOR="/actuator/loggers/$A1PMS_PKG_NAME"           # Url for trace/debug
A1PMS_ALIVE_URL="$A1PMS_API_PREFIX/v2/status"                # Base path for alive check
# A1PMS_ALIVE_URL_V3=/v1/status                            # To be used this property in further env releases
A1PMS_COMPOSE_DIR="a1pms"                                    # Dir in simulator_group for docker-compose
A1PMS_CONFIG_MOUNT_PATH="/opt/app/policy-agent/config"     # Path in container for config file
A1PMS_DATA_MOUNT_PATH="/opt/app/policy-agent/data"         # Path in container for data file
A1PMS_CONFIG_FILE="application.yaml"                       # Container config file name
A1PMS_DATA_FILE="application_configuration.json"           # Container data file name
A1PMS_CONTAINER_MNT_DIR="/var/policy-management-service"   # Mounted dir in the container
A1PMS_FEATURE_LEVEL="NO-DMAAP ADAPTER-CLASS"               # Space separated list of features
A1PMS_ADAPTER_CLASS=""                                     # Class name set by override file
A1PMS_ADAPTER_POLICY_NAME=""                               # Policy name set by override file

ICS_APP_NAME="informationservice"                        # Name for ICS container
ICS_DISPLAY_NAME="Enrichment Coordinator Service"        # Display name for ICS container
ICS_EXTERNAL_PORT=8083                                   # ICS container external port (host -> container)
ICS_INTERNAL_PORT=8083                                   # ICS container internal port (container -> container)
ICS_EXTERNAL_SECURE_PORT=8434                            # ICS container external secure port (host -> container)
ICS_INTERNAL_SECURE_PORT=8434                            # ICS container internal secure port (container -> container)

ICS_LOGPATH="/var/log/information-coordinator-service/application.log" # Path the application log in the ICS container
ICS_APP_NAME_ALIAS="information-service-container"       # Alias name, name used by the control panel
ICS_HOST_MNT_DIR="./mnt"                                 # Mounted dir, relative to compose file, on the host
ICS_CONTAINER_MNT_DIR="/var/information-coordinator-service" # Mounted dir in the container
ICS_ACTUATOR="/actuator/loggers/org.oransc.ics"          # Url for trace/debug
ICS_CERT_MOUNT_DIR="./cert"
ICS_ALIVE_URL="/status"                                  # Base path for alive check
ICS_COMPOSE_DIR="ics"                                    # Dir in simulator_group for docker-compose
ICS_CONFIG_MOUNT_PATH=/opt/app/information-coordinator-service/config # Internal container path for configuration
ICS_CONFIG_FILE=application.yaml                         # Config file name
ICS_FEATURE_LEVEL=""                                     # Space separated list of features

MR_DMAAP_APP_NAME="message-router"                       # Name for the Dmaap MR
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
MR_READ_TOPIC="A1-POLICY-AGENT-READ"                     # Read topic
MR_WRITE_TOPIC="A1-POLICY-AGENT-WRITE"                   # Write topic
MR_READ_URL="/events/$MR_READ_TOPIC/users/policy-agent?timeout=15000&limit=100" # Path to read messages from MR
MR_WRITE_URL="/events/$MR_WRITE_TOPIC"                   # Path to write messages to MR
MR_STUB_ALIVE_URL="/"                                    # Base path for mr stub alive check
MR_DMAAP_ALIVE_URL="/topics"                             # Base path for dmaap-mr alive check
MR_DMAAP_COMPOSE_DIR="dmaapmr"                           # Dir in simulator_group for dmaap mr for - docker-compose
MR_STUB_COMPOSE_DIR="mrstub"                             # Dir in simulator_group for mr stub for - docker-compose
MR_KAFKA_APP_NAME="message-router-kafka"                 # Kafka app name, if just named "kafka" the image will not start...
MR_KAFKA_PORT=9092                                       # Kafka port number
MR_KAFKA_DOCKER_LOCALHOST_PORT=30098                     # Kafka port number for docker localhost
MR_KAFKA_KUBE_NODE_PORT=30099                            # Kafka node port number for kube
MR_ZOOKEEPER_APP_NAME="zookeeper"                        # Zookeeper app name
MR_ZOOKEEPER_PORT="2181"                                 # Zookeeper port number
MR_DMAAP_HOST_MNT_DIR="/mnt"                             # Basedir localhost for mounted files
MR_DMAAP_HOST_CONFIG_DIR="/configs1"                      # Config files dir on localhost

CR_APP_NAME="callback-receiver"                          # Name for the Callback receiver
CR_DISPLAY_NAME="Callback Reciever"
CR_EXTERNAL_PORT=8090                                    # Callback receiver container external port (host -> container)
CR_INTERNAL_PORT=8090                                    # Callback receiver container internal port (container -> container)
CR_EXTERNAL_SECURE_PORT=8091                             # Callback receiver container external secure port (host -> container)
CR_INTERNAL_SECURE_PORT=8091                             # Callback receiver container internal secure port (container -> container)
CR_APP_NAME="callback-receiver"                          # Name for the Callback receiver
CR_APP_CALLBACK="/callbacks"                             # Url for callbacks
CR_APP_CALLBACK_MR="/callbacks-mr"                       # Url for callbacks (data from mr which contains string encoded jsons in a json arr)
CR_APP_CALLBACK_TEXT="/callbacks-text"                   # Url for callbacks (data containing text data)
CR_ALIVE_URL="/reset"                                    # Base path for alive check
CR_COMPOSE_DIR="cr"                                      # Dir in simulator_group for docker-compose

PROD_STUB_APP_NAME="producer-stub"                       # Name for the Producer stub
PROD_STUB_DISPLAY_NAME="Producer Stub"
PROD_STUB_EXTERNAL_PORT=8092                             # Producer stub container external port (host -> container)
PROD_STUB_INTERNAL_PORT=8092                             # Producer stub container internal port (container -> container)
PROD_STUB_EXTERNAL_SECURE_PORT=8093                      # Producer stub container external secure port (host -> container)
PROD_STUB_INTERNAL_SECURE_PORT=8093                      # Producer stub container internal secure port (container -> container)
PROD_STUB_JOB_CALLBACK="/callbacks/job"                  # Callback path for job create/update/delete
PROD_STUB_SUPERVISION_CALLBACK="/callbacks/supervision"  # Callback path for producer supervision
PROD_STUB_ALIVE_URL="/"                                  # Base path for alive check
PROD_STUB_COMPOSE_DIR="prodstub"                         # Dir in simulator_group for docker-compose

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
RIC_SIM_COMMON_SVC_NAME=""                               # Name of svc if one common svc is used for all ric sim groups (stateful sets)

SDNC_APP_NAME="a1controller"                             # Name of the SNDC A1 Controller container
SDNC_DISPLAY_NAME="SDNC A1 Controller"
SDNC_EXTERNAL_PORT=8282                                  # SNDC A1 Controller container external port (host -> container)
SDNC_INTERNAL_PORT=8181                                  # SNDC A1 Controller container internal port (container -> container)
SDNC_EXTERNAL_SECURE_PORT=8443                           # SNDC A1 Controller container external secure port (host -> container)
SDNC_INTERNAL_SECURE_PORT=8443                           # SNDC A1 Controller container internal secure port (container -> container)
SDNC_DB_APP_NAME="sdncdb"                                # Name of the SDNC DB container
SDNC_A1_TRUSTSTORE_PASSWORD="a1adapter"                  # SDNC truststore password
SDNC_USER="admin"                                        # SDNC username
SDNC_PWD="admin"                                         # SNDC PWD
SDNC_PWD="Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"   # SNDC PWD
SDNC_API_URL="/rests/operations/A1-ADAPTER-API:"         # Base url path for SNDC API (for upgraded sdnc)
#SDNC_API_URL="/restconf/operations/A1-ADAPTER-API:"      # Base url path for SNDC API
SDNC_ALIVE_URL="/apidoc/explorer/index.html"             # Base url path for SNDC API docs (for alive check)
SDNC_COMPOSE_DIR="sdnc"
SDNC_COMPOSE_FILE="docker-compose-2.yml"
SDNC_KUBE_APP_FILE="app2.yaml"
SDNC_KARAF_LOG="/opt/opendaylight/data/log/karaf.log"    # Path to karaf log
SDNC_RESPONSE_JSON_KEY="A1-ADAPTER-API:output"           # Key name for output json in replies from sdnc (for upgraded sdnc)
#SDNC_RESPONSE_JSON_KEY="output"                          # Key name for output json in replies from sdnc
SDNC_FEATURE_LEVEL="TRANS_RESP_CODE NO_NB_HTTPS"         # Space separated list of features
                                                         # TRANS_RESP_CODE: SDNC return southbound response code

RAPP_CAT_APP_NAME="rappcatalogueservice"                 # Name for the RAPP Catalogue
RAPP_CAT_DISPLAY_NAME="RAPP Catalogue Service"
RAPP_CAT_EXTERNAL_PORT=8680                              # RAPP Catalogue container external port (host -> container)
RAPP_CAT_INTERNAL_PORT=8680                              # RAPP Catalogue container internal port (container -> container)
RAPP_CAT_EXTERNAL_SECURE_PORT=8633                       # RAPP Catalogue container external secure port (host -> container)
RAPP_CAT_INTERNAL_SECURE_PORT=8633                       # RAPP Catalogue container internal secure port (container -> container)
RAPP_CAT_ALIVE_URL="/services"                           # Base path for alive check
RAPP_CAT_COMPOSE_DIR="rapp_catalogue"                    # Dir in simulator_group for docker-compose

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
CONTROL_PANEL_HOST_MNT_DIR="./mnt"                       # Mounted dir, relative to compose file, on the host

NRT_GATEWAY_APP_NAME="nonrtricgateway"                   # Name of the Gateway container
NRT_GATEWAY_DISPLAY_NAME="NonRT-RIC Gateway"
NRT_GATEWAY_EXTERNAL_PORT=9090                           # Gateway container external port (host -> container)
NRT_GATEWAY_INTERNAL_PORT=9090                           # Gateway container internal port (container -> container)
NRT_GATEWAY_EXTERNAL_SECURE_PORT=9091                    # Gateway container external port (host -> container)
NRT_GATEWAY_INTERNAL_SECURE_PORT=9091                    # Gateway container internal port (container -> container)
NRT_GATEWAY_LOGPATH="/var/log/nonrtric-gateway/application.log" # Path the application log in the Gateway container
NRT_GATEWAY_HOST_MNT_DIR="./mnt"                         # Mounted dir, relative to compose file, on the host
NRT_GATEWAY_ALIVE_URL="/actuator/metrics"                # Base path for alive check
NRT_GATEWAY_COMPOSE_DIR="ngw"                            # Dir in simulator_group for docker-compose
NRT_GATEWAY_CONFIG_MOUNT_PATH=/opt/app/nonrtric-gateway/config  # Container internal path for config
NRT_GATEWAY_CONFIG_FILE=application.yaml                 # Config file name
NRT_GATEWAY_PKG_NAME="org.springframework.cloud.gateway" # Java base package name
NRT_GATEWAY_ACTUATOR="/actuator/loggers/$NRT_GATEWAY_PKG_NAME" # Url for trace/debug

HTTP_PROXY_APP_NAME="httpproxy"                          # Name of the Http Proxy container
HTTP_PROXY_DISPLAY_NAME="Http Proxy"
HTTP_PROXY_EXTERNAL_PORT=8740                            # Http Proxy container external port (host -> container)
HTTP_PROXY_INTERNAL_PORT=8080                            # Http Proxy container internal port (container -> container)
HTTP_PROXY_EXTERNAL_SECURE_PORT=8742                     # Http Proxy container external secure port (host -> container)
HTTP_PROXY_INTERNAL_SECURE_PORT=8433                     # Http Proxy container internal secure port (container -> container)
HTTP_PROXY_WEB_EXTERNAL_PORT=8741                        # Http Proxy container external port (host -> container)
HTTP_PROXY_WEB_INTERNAL_PORT=8081                        # Http Proxy container internal port (container -> container)
HTTP_PROXY_WEB_EXTERNAL_SECURE_PORT=8743                 # Http Proxy container external secure port (host -> container)
HTTP_PROXY_WEB_INTERNAL_SECURE_PORT=8434                 # Http Proxy container internal secure port (container -> container
HTTP_PROXY_CONFIG_PORT=0                                 # Port number for proxy config, will be set if proxy is started
HTTP_PROXY_CONFIG_HOST_NAME=""                           # Proxy host, will be set if proxy is started
HTTP_PROXY_ALIVE_URL="/"                                 # Base path for alive check
HTTP_PROXY_COMPOSE_DIR="httpproxy"                       # Dir in simulator_group for docker-compose
HTTP_PROXY_BUILD_DIR="http-https-proxy"                  # Dir in simulator_group for image build - note, reuses source from kubeproxy

KUBE_PROXY_APP_NAME="kubeproxy"                          # Name of the Kube Http Proxy container
KUBE_PROXY_DISPLAY_NAME="Kube Http Proxy"
KUBE_PROXY_EXTERNAL_PORT=8730                            # Kube Http Proxy container external port (host -> container)
KUBE_PROXY_INTERNAL_PORT=8080                            # Kube Http Proxy container internal port (container -> container)
KUBE_PROXY_EXTERNAL_SECURE_PORT=8782                     # Kube Proxy container external secure port (host -> container)
KUBE_PROXY_INTERNAL_SECURE_PORT=8433                     # Kube Proxy container internal secure port (container -> container)
KUBE_PROXY_WEB_EXTERNAL_PORT=8731                        # Kube Http Proxy container external port (host -> container)
KUBE_PROXY_WEB_INTERNAL_PORT=8081                        # Kube Http Proxy container internal port (container -> container)
KUBE_PROXY_WEB_EXTERNAL_SECURE_PORT=8783                 # Kube Proxy container external secure port (host -> container)
KUBE_PROXY_WEB_INTERNAL_SECURE_PORT=8434                 # Kube Proxy container internal secure port (container -> container

KUBE_PROXY_DOCKER_EXTERNAL_PORT=8732                     # Kube Http Proxy container external port, docker (host -> container)
KUBE_PROXY_DOCKER_EXTERNAL_SECURE_PORT=8784              # Kube Proxy container external secure port, docker (host -> container)
KUBE_PROXY_WEB_DOCKER_EXTERNAL_PORT=8733                 # Kube Http Proxy container external port, docker (host -> container)
KUBE_PROXY_WEB_DOCKER_EXTERNAL_SECURE_PORT=8785          # Kube Proxy container external secure port, docker (host -> container)

KUBE_PROXY_PATH=""                                       # Proxy url path, will be set if proxy is started
KUBE_PROXY_ALIVE_URL="/"                                 # Base path for alive check
KUBE_PROXY_COMPOSE_DIR="kubeproxy"                       # Dir in simulator_group for docker-compose

PVC_CLEANER_APP_NAME="pvc-cleaner"                      # Name for Persistent Volume Cleaner container
PVC_CLEANER_DISPLAY_NAME="Persistent Volume Cleaner"    # Display name for Persistent Volume Cleaner
PVC_CLEANER_COMPOSE_DIR="pvc-cleaner"                   # Dir in simulator_group for yamls

########################################
# Setting for common curl-base function
########################################

UUID=""                                                  # UUID used as prefix to the policy id to simulate a real UUID
                                                         # Testscript need to set the UUID to use other this empty prefix is used
