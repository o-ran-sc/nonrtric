#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2024-2025 OpenInfra Foundation Europe. All rights reserved.
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

# This is a script that contains management and test functions for A1PMS

################ Test engine functions ################

# Create the image var used during the test
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__A1PMS_imagesetup() {
  __check_and_create_image_var A1PMS "A1PMS_IMAGE" "A1PMS_IMAGE_BASE" "A1PMS_IMAGE_TAG" $1 "$A1PMS_DISPLAY_NAME" ""
}

# Pull image from remote repo or use locally built image
# arg: <pull-policy-override> <pull-policy-original>
# <pull-policy-override> Shall be used for images allowing overriding. For example use a local image when test is started to use released images
# <pull-policy-original> Shall be used for images that does not allow overriding
# Both var may contain: 'remote', 'remote-remove' or 'local'
__A1PMS_imagepull() {
  __check_and_pull_image $1 "$A1PMS_DISPLAY_NAME" $A1PMS_APP_NAME A1PMS_IMAGE
}

# Build image (only for simulator or interfaces stubs owned by the test environment)
# arg: <image-tag-suffix> (selects staging, snapshot, release etc)
# <image-tag-suffix> is present only for images with staging, snapshot,release tags
__A1PMS_imagebuild() {
  echo -e $RED" Image for app A1PMS shall never be built"$ERED
}

# Generate a string for each included image using the app display name and a docker images format string
# If a custom image repo is used then also the source image from the local repo is listed
# arg: <docker-images-format-string> <file-to-append>
__A1PMS_image_data() {
  echo -e "$A1PMS_DISPLAY_NAME\t$(docker images --format $1 $A1PMS_IMAGE)" >>$2
  if [ ! -z "$A1PMS_IMAGE_SOURCE" ]; then
    echo -e "-- source image --\t$(docker images --format $1 $A1PMS_IMAGE_SOURCE)" >>$2
  fi
}

# Scale kubernetes resources to zero
# All resources shall be ordered to be scaled to 0, if relevant. If not relevant to scale, then do no action.
# This function is called for apps fully managed by the test script
__A1PMS_kube_scale_zero() {
  __kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest A1PMS
}

# Scale kubernetes resources to zero and wait until this has been accomplished, if relevant. If not relevant to scale, then do no action.
# This function is called for pre-started apps not managed by the test script.
__A1PMS_kube_scale_zero_and_wait() {
  __kube_scale_and_wait_all_resources $KUBE_NONRTRIC_NAMESPACE app "$KUBE_NONRTRIC_NAMESPACE"-policymanagementservice
}

# Delete all kube resources for the app
# This function is called for apps managed by the test script.
__A1PMS_kube_delete_all() {
  __kube_delete_all_resources $KUBE_NONRTRIC_NAMESPACE autotest A1PMS
}

# Store docker logs
# This function is called for apps managed by the test script.
# args: <log-dir> <file-prefix>
__A1PMS_store_docker_logs() {
  if [ $RUNMODE == "KUBE" ]; then
    kubectl $KUBECONF logs -l "autotest=A1PMS" -n $KUBE_NONRTRIC_NAMESPACE --tail=-1 >$1$2_a1pms.log 2>&1
  else
    docker logs $A1PMS_APP_NAME >$1$2_a1pms.log 2>&1
  fi
}

# Initial setup of protocol, host and ports
# This function is called for apps managed by the test script.
# args: -
__A1PMS_initial_setup() {
  use_a1pms_rest_http
  export A1PMS_SIDECAR_JWT_FILE=""
}

# Set app short-name, app name and namespace for logging runtime statistics of kubernetes pods or docker containers
# For docker, the namespace shall be excluded
# This function is called for apps managed by the test script as well as for pre-started apps.
# args: -
__A1PMS_statistics_setup() {
  if [ $RUNMODE == "KUBE" ]; then
    echo "A1PMS $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE"
  else
    echo "A1PMS $A1PMS_APP_NAME"
  fi
}

# Check application requirements, e.g. helm, the the test needs. Exit 1 if req not satisfied
# args: -
__A1PMS_test_requirements() {
  :
}

#######################################################

###########################
### A1PMSs functions
###########################

# Set http as the protocol to use for all communication to the A1PMS
# args: -
# (Function for test scripts)
use_a1pms_rest_http() {
  __a1pms_set_protocoll "http" $A1PMS_INTERNAL_PORT $A1PMS_EXTERNAL_PORT
}

# Set https as the protocol to use for all communication to the A1PMS
# args: -
# (Function for test scripts)
use_a1pms_rest_https() {
  __a1pms_set_protocoll "https" $A1PMS_INTERNAL_SECURE_PORT $A1PMS_EXTERNAL_SECURE_PORT
}

# All calls to the a1pms will be directed to the a1pms dmaap interface over http from now on
# args: -
# (Function for test scripts)
use_a1pms_dmaap_http() {
  echo -e $BOLD"$A1PMS_DISPLAY_NAME dmaap protocol setting"$EBOLD
  echo -e " Using $BOLD http $EBOLD and $BOLD DMAAP $EBOLD towards the a1pms"
  A1PMS_ADAPTER_TYPE="MR-HTTP"
  echo ""
}

# All calls to the a1pms will be directed to the a1pms dmaap interface over https from now on
# args: -
# (Function for test scripts)
use_a1pms_dmaap_https() {
  echo -e $BOLD"$A1PMS_DISPLAY_NAME dmaap protocol setting"$EBOLD
  echo -e " Using $BOLD https $EBOLD and $BOLD DMAAP $EBOLD towards the a1pms"
  echo -e $YELLOW" Setting http instead of https - MR only uses http"$EYELLOW
  A1PMS_ADAPTER_TYPE="MR-HTTPS"
  echo ""
}

# Setup paths to svc/container for internal and external access
# args: <protocol> <internal-port> <external-port>
__a1pms_set_protocoll() {
  echo -e $BOLD"$A1PMS_DISPLAY_NAME protocol setting"$EBOLD
  echo -e " Using $BOLD $1 $EBOLD towards $A1PMS_DISPLAY_NAME"

  ## Access to Dmaap adapter

  A1PMS_SERVICE_PATH=$1"://"$A1PMS_APP_NAME":"$2 # docker access, container->container and script->container via proxy
  if [ $RUNMODE == "KUBE" ]; then
    A1PMS_SERVICE_PATH=$1"://"$A1PMS_APP_NAME.$KUBE_NONRTRIC_NAMESPACE":"$3 # kube access, pod->svc and script->svc via proxy
  fi

  # A1PMS_ADAPTER used for switching between REST and DMAAP (only REST supported currently)
  A1PMS_ADAPTER_TYPE="REST"
  A1PMS_ADAPTER=$A1PMS_SERVICE_PATH

  echo ""
}

# Make curl retries towards the a1pms for http response codes set in this env var, space separated list of codes
A1PMS_RETRY_CODES=""

#Save first worker node the pod is started on
__A1PMS_WORKER_NODE=""

# Export env vars for config files, docker compose and kube resources
# args: PROXY|NOPROXY
__export_a1pms_vars() {

  export A1PMS_APP_NAME
  export A1PMS_APP_NAME_ALIAS
  export A1PMS_DISPLAY_NAME

  export KUBE_NONRTRIC_NAMESPACE
  export A1PMS_IMAGE
  export A1PMS_INTERNAL_PORT
  export A1PMS_INTERNAL_SECURE_PORT
  export A1PMS_EXTERNAL_PORT
  export A1PMS_EXTERNAL_SECURE_PORT
  export A1PMS_CONFIG_MOUNT_PATH
  export A1PMS_DATA_MOUNT_PATH
  export A1PMS_CONFIG_CONFIGMAP_NAME=$A1PMS_APP_NAME"-config"
  export A1PMS_DATA_CONFIGMAP_NAME=$A1PMS_APP_NAME"-data"
  export A1PMS_PKG_NAME
  export A1PMS_CONFIG_KEY
  export DOCKER_SIM_NWNAME
  export A1PMS_HOST_MNT_DIR
  export A1PMS_CONFIG_FILE

  export A1PMS_DATA_PV_NAME=$A1PMS_APP_NAME"-pv"
  export A1PMS_DATA_PVC_NAME=$A1PMS_APP_NAME"-pvc"
  ##Create a unique path for the pv each time to prevent a previous volume to be reused
  export A1PMS_PV_PATH="a1pmsdata-"$(date +%s)
  export A1PMS_CONTAINER_MNT_DIR
  export HOST_PATH_BASE_DIR

  if [ $1 == "PROXY" ]; then
    export A1PMS_HTTP_PROXY_CONFIG_PORT=$HTTP_PROXY_CONFIG_PORT           #Set if proxy is started
    export A1PMS_HTTP_PROXY_CONFIG_HOST_NAME=$HTTP_PROXY_CONFIG_HOST_NAME #Set if proxy is started
    if [ $A1PMS_HTTP_PROXY_CONFIG_PORT -eq 0 ] || [ -z "$A1PMS_HTTP_PROXY_CONFIG_HOST_NAME" ]; then
      echo -e $YELLOW" Warning: HTTP PROXY will not be configured, proxy app not started"$EYELLOW
    else
      echo " Configured with http proxy"
    fi
  else
    export A1PMS_HTTP_PROXY_CONFIG_PORT=0
    export A1PMS_HTTP_PROXY_CONFIG_HOST_NAME=""
    echo " Configured without http proxy"
  fi
}

# Start the ms
# args: (docker) PROXY|NOPROXY <config-file>
# args: (kube) PROXY|NOPROXY <config-file> [ <data-file>]
# (Function for test scripts)
start_a1pms() {
  echo -e $BOLD"Starting $A1PMS_DISPLAY_NAME"$EBOLD

  if [ $RUNMODE == "KUBE" ]; then

    # Check if app shall be fully managed by the test script
    __check_included_image "A1PMS"
    retcode_i=$?

    # Check if app shall only be used by the test script
    __check_prestarted_image "A1PMS"
    retcode_p=$?

    if [ $retcode_i -ne 0 ] && [ $retcode_p -ne 0 ]; then
      echo -e $RED"The $A1PMS_APP_NAME app is not included as managed nor prestarted in this test script"$ERED
      echo -e $RED"The $A1PMS_APP_NAME will not be started"$ERED
      exit
    fi
    if [ $retcode_i -eq 0 ] && [ $retcode_p -eq 0 ]; then
      echo -e $RED"The $A1PMS_APP_NAME app is included both as managed and prestarted in this test script"$ERED
      echo -e $RED"The $A1PMS_APP_NAME will not be started"$ERED
      exit
    fi

    if [ $retcode_p -eq 0 ]; then
      echo -e " Using existing $A1PMS_APP_NAME deployment and service"
      echo " Setting $A1PMS_APP_NAME replicas=1"
      res_type=$(__kube_get_resource_type $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
      __kube_scale $res_type $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
    fi

    if [ $retcode_i -eq 0 ]; then

      echo -e " Creating $A1PMS_APP_NAME app and expose service"

      #Check if nonrtric namespace exists, if not create it
      __kube_create_namespace $KUBE_NONRTRIC_NAMESPACE

      __export_a1pms_vars $1

      # Create config map for config
      configfile=$PWD/tmp/$A1PMS_CONFIG_FILE
      cp $2 $configfile
      output_yaml=$PWD/tmp/a1pms-cfc.yaml
      __kube_create_configmap $A1PMS_CONFIG_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest A1PMS $configfile $output_yaml

      # Create config map for data
      data_json=$PWD/tmp/$A1PMS_DATA_FILE
      if [ $# -lt 3 ]; then
        #create empty dummy file
        echo "{}" >$data_json
      else
        cp $3 $data_json
      fi
      output_yaml=$PWD/tmp/a1pms-cfd.yaml
      __kube_create_configmap $A1PMS_DATA_CONFIGMAP_NAME $KUBE_NONRTRIC_NAMESPACE autotest A1PMS $data_json $output_yaml

      ## Create pv
      input_yaml=$SIM_GROUP"/"$A1PMS_COMPOSE_DIR"/"pv.yaml
      output_yaml=$PWD/tmp/a1pms-pv.yaml
      __kube_create_instance pv $A1PMS_APP_NAME $input_yaml $output_yaml

      ## Create pvc
      input_yaml=$SIM_GROUP"/"$A1PMS_COMPOSE_DIR"/"pvc.yaml
      output_yaml=$PWD/tmp/a1pms-pvc.yaml
      __kube_create_instance pvc $A1PMS_APP_NAME $input_yaml $output_yaml

      # Create service
      input_yaml=$SIM_GROUP"/"$A1PMS_COMPOSE_DIR"/"svc.yaml
      output_yaml=$PWD/tmp/a1pmssvc.yaml
      __kube_create_instance service $A1PMS_APP_NAME $input_yaml $output_yaml

      # Create app
      input_yaml=$SIM_GROUP"/"$A1PMS_COMPOSE_DIR"/"app.yaml
      output_yaml=$PWD/tmp/a1pmsapp.yaml
      if [ -z "$A1PMS_SIDECAR_JWT_FILE" ]; then
        cat $input_yaml | sed '/#A1PMS_JWT_START/,/#A1PMS_JWT_STOP/d' >$PWD/tmp/a1pmsapp_tmp.yaml
        input_yaml=$PWD/tmp/a1pmsapp_tmp.yaml
      fi
      __kube_create_instance app $A1PMS_APP_NAME $input_yaml $output_yaml

    fi

    # Keep the initial worker node in case the pod need to be "restarted" - must be made to the same node due to a volume mounted on the host
    if [ $retcode_i -eq 0 ]; then
      __A1PMS_WORKER_NODE=$(kubectl $KUBECONF get pod -l "autotest=A1PMS" -n $KUBE_NONRTRIC_NAMESPACE -o jsonpath='{.items[*].spec.nodeName}')
      if [ -z "$__A1PMS_WORKER_NODE" ]; then
        echo -e $YELLOW" Cannot find worker node for pod for $A1PMS_APP_NAME, persistency may not work"$EYELLOW
      fi
    else
      echo -e $YELLOW" Persistency may not work for app $A1PMS_APP_NAME in multi-worker node config when running it as a prestarted app"$EYELLOW
    fi

    __check_service_start $A1PMS_APP_NAME $A1PMS_SERVICE_PATH$A1PMS_ALIVE_URL

  else
    __check_included_image 'A1PMS'
    if [ $? -eq 1 ]; then
      echo -e $RED"The A1PMS app is not included in this test script"$ERED
      echo -e $RED"The A1PMS will not be started"$ERED
      exit
    fi

    curdir=$PWD
    cd $SIM_GROUP
    cd a1pms
    cd $A1PMS_HOST_MNT_DIR
    #cd ..
    if [ -d db ]; then
      if [ "$(ls -A $DIR)" ]; then
        echo -e $BOLD" Cleaning files in mounted dir: $PWD/db"$EBOLD
        rm -rf db/* &>/dev/null
        if [ $? -ne 0 ]; then
          echo -e $RED" Cannot remove database files in: $PWD"$ERED
          exit 1
        fi
      fi
    else
      echo " No files in mounted dir or dir does not exists"
      # The 'db' directory is created with full permissions (777) and is not removed at the end of the script.
      mkdir -m 777 db
      echo "Creating directory under $SIM_GROUP/a1pms/$A1PMS_HOST_MNT_DIR/ with 777 permission. This directory will need to be manually removed when tests complete."
    fi
    cd $curdir

    __export_a1pms_vars $1

    dest_file=$SIM_GROUP/$A1PMS_COMPOSE_DIR/$A1PMS_HOST_MNT_DIR/application.yaml

    envsubst <$2 >$dest_file

    __start_container $A1PMS_COMPOSE_DIR "" NODOCKERARGS 1 $A1PMS_APP_NAME

    __check_service_start $A1PMS_APP_NAME $A1PMS_SERVICE_PATH$A1PMS_ALIVE_URL
  fi

  __collect_endpoint_stats_image_info "A1PMS" $A1PMS_IMAGE
  echo ""
  return 0
}

# Stop the a1pms
# args: -
# args: -
# (Function for test scripts)
stop_a1pms() {
  echo -e $BOLD"Stopping $A1PMS_DISPLAY_NAME"$EBOLD

  if [ $RUNMODE == "KUBE" ]; then

    __check_prestarted_image "A1PMS"
    if [ $? -eq 0 ]; then
      echo -e $YELLOW" Persistency may not work for app $A1PMS_APP_NAME in multi-worker node config when running it as a prestarted app"$EYELLOW
      res_type=$(__kube_get_resource_type $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
      __kube_scale $res_type $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 0
      return 0
    fi
    __kube_scale_all_resources $KUBE_NONRTRIC_NAMESPACE autotest A1PMS
    echo "  Deleting the replica set - a new will be started when the app is started"
    tmp=$(kubectl $KUBECONF delete rs -n $KUBE_NONRTRIC_NAMESPACE -l "autotest=PA")
    if [ $? -ne 0 ]; then
      echo -e $RED" Could not delete replica set "$RED
      ((RES_CONF_FAIL++))
      return 1
    fi
  else
    docker stop $A1PMS_APP_NAME &>./tmp/.dockererr
    if [ $? -ne 0 ]; then
      __print_err "Could not stop $A1PMS_APP_NAME" $@
      cat ./tmp/.dockererr
      ((RES_CONF_FAIL++))
      return 1
    fi
  fi
  echo -e $BOLD$GREEN"Stopped"$EGREEN$EBOLD
  echo ""
  return 0
}

# Start a previously stopped a1pms
# args: -
# (Function for test scripts)
start_stopped_a1pms() {
  echo -e $BOLD"Starting (the previously stopped) $A1PMS_DISPLAY_NAME"$EBOLD

  if [ $RUNMODE == "KUBE" ]; then

    __check_prestarted_image "A1PMS"
    if [ $? -eq 0 ]; then
      echo -e $YELLOW" Persistency may not work for app $A1PMS_APP_NAME in multi-worker node config when running it as a prestarted app"$EYELLOW
      res_type=$(__kube_get_resource_type $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE)
      __kube_scale $res_type $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
      __check_service_start $A1PMS_APP_NAME $A1PMS_SERVICE_PATH$A1PMS_ALIVE_URL
      return 0
    fi

    # Tie the A1PMS to the same worker node it was initially started on
    # A PVC of type hostPath is mounted to A1PMS, for persistent storage, so the A1PMS must always be on the node which mounted the volume
    if [ -z "$__A1PMS_WORKER_NODE" ]; then
      echo -e $RED" No initial worker node found for pod "$RED
      ((RES_CONF_FAIL++))
      return 1
    else
      echo -e $BOLD" Setting nodeSelector kubernetes.io/hostname=$__A1PMS_WORKER_NODE to deployment for $A1PMS_APP_NAME. Pod will always run on this worker node: $__A1PMS_WORKER_NODE"$BOLD
      echo -e $BOLD" The mounted volume is mounted as hostPath and only available on that worker node."$BOLD
      tmp=$(kubectl $KUBECONF patch deployment $A1PMS_APP_NAME -n $KUBE_NONRTRIC_NAMESPACE --patch '{"spec": {"template": {"spec": {"nodeSelector": {"kubernetes.io/hostname": "'$__A1PMS_WORKER_NODE'"}}}}}')
      if [ $? -ne 0 ]; then
        echo -e $YELLOW" Cannot set nodeSelector to deployment for $A1PMS_APP_NAME, persistency may not work"$EYELLOW
      fi
      __kube_scale deployment $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE 1
    fi
  else
    docker start $A1PMS_APP_NAME &>./tmp/.dockererr
    if [ $? -ne 0 ]; then
      __print_err "Could not start (the stopped) $A1PMS_APP_NAME" $@
      cat ./tmp/.dockererr
      ((RES_CONF_FAIL++))
      return 1
    fi
  fi
  __check_service_start $A1PMS_APP_NAME $A1PMS_SERVICE_PATH$A1PMS_ALIVE_URL
  if [ $? -ne 0 ]; then
    return 1
  fi
  echo ""
  return 0
}

# Function to prepare the a1pms configuration according to the current simulator configuration
# args: SDNC|NOSDNC <output-file> [ <sim-group> <adapter-class> ]
# (Function for test scripts)
prepare_a1pms_config() {
  echo -e $BOLD"Prepare A1PMS config"$EBOLD

  echo " Writing a1pms config for "$A1PMS_APP_NAME" to file: "$2

  if [ $# != 2 ] && [ $# != 4 ]; then
    ((RES_CONF_FAIL++))
    __print_err "need two or four args,  SDNC|NOSDNC <output-file> [ <sim-group> <adapter-class> ]" $@
    exit 1
  fi

  if [ $1 == "SDNC" ]; then
    echo -e " Config$BOLD including SDNC$EBOLD configuration"
  elif [ $1 == "NOSDNC" ]; then
    echo -e " Config$BOLD excluding SDNC$EBOLD configuration"
  else
    ((RES_CONF_FAIL++))
    __print_err "need three args,  SDNC|NOSDNC <output-file> HEADER|NOHEADER" $@
    exit 1
  fi

  config_json="\n            {"
  if [ $1 == "SDNC" ]; then
    config_json=$config_json"\n   \"controller\": ["
    config_json=$config_json"\n                     {"
    config_json=$config_json"\n                       \"name\": \"$SDNC_APP_NAME\","
    config_json=$config_json"\n                       \"baseUrl\": \"$SDNC_SERVICE_PATH\","
    config_json=$config_json"\n                       \"userName\": \"$SDNC_USER\","
    config_json=$config_json"\n                       \"password\": \"$SDNC_PWD\""
    config_json=$config_json"\n                     }"
    config_json=$config_json"\n   ],"
  fi
  if [[ "$A1PMS_FEATURE_LEVEL" == *"NO-DMAAP"* ]]; then
    :
  else
    config_json=$config_json"\n   \"streams_publishes\": {"
    config_json=$config_json"\n                            \"dmaap_publisher\": {"
    config_json=$config_json"\n                              \"type\": \"message-router\","
    config_json=$config_json"\n                              \"dmaap_info\": {"
    config_json=$config_json"\n                                \"topic_url\": \"$MR_SERVICE_PATH$MR_WRITE_URL\""
    config_json=$config_json"\n                              }"
    config_json=$config_json"\n                            }"
    config_json=$config_json"\n   },"
    config_json=$config_json"\n   \"streams_subscribes\": {"
    config_json=$config_json"\n                             \"dmaap_subscriber\": {"
    config_json=$config_json"\n                               \"type\": \"message-router\","
    config_json=$config_json"\n                               \"dmaap_info\": {"
    config_json=$config_json"\n                                   \"topic_url\": \"$MR_SERVICE_PATH$MR_READ_URL\""
    config_json=$config_json"\n                                 }"
    config_json=$config_json"\n                               }"
    config_json=$config_json"\n   },"
  fi

  config_json=$config_json"\n   \"ric\": ["

  if [ $RUNMODE == "KUBE" ]; then
    result=$(kubectl $KUBECONF get pods -n $KUBE_A1SIM_NAMESPACE -o jsonpath='{.items[?(@.metadata.labels.autotest=="RICSIM")].metadata.name}')
    rics=""
    ric_cntr=0
    if [ $? -eq 0 ] && [ ! -z "$result" ]; then
      for im in $result; do
        if [[ $im != *"-0" ]]; then
          ric_subdomain=$(kubectl $KUBECONF get pod $im -n $KUBE_A1SIM_NAMESPACE -o jsonpath='{.spec.subdomain}')
          rics=$rics" "$im"."$ric_subdomain"."$KUBE_A1SIM_NAMESPACE
          let ric_cntr=ric_cntr+1
        fi
      done
    fi
    result=$(kubectl $KUBECONF get pods -n $KUBE_A1SIM_NAMESPACE -o jsonpath='{.items[?(@.metadata.labels.autotest=="RICMEDIATORSIM")].metadata.name}')
    oranrics=""
    if [ $? -eq 0 ] && [ ! -z "$result" ]; then
      for im in $result; do
        if [[ $im != *"-0" ]]; then
          ric_subdomain=$(kubectl $KUBECONF get pod $im -n $KUBE_A1SIM_NAMESPACE -o jsonpath='{.spec.subdomain}')
          rics=$rics" "$im"."$ric_subdomain"."$KUBE_A1SIM_NAMESPACE
          oranrics=$oranrics" "$im"."$ric_subdomain"."$KUBE_A1SIM_NAMESPACE
          let ric_cntr=ric_cntr+1
        fi
      done
    fi
    if [ $ric_cntr -eq 0 ]; then
      echo $YELLOW"Warning: No rics found for the configuration"$EYELLOW
    fi
  else
    rics=$(docker ps --filter "name=$RIC_SIM_PREFIX" --filter "network=$DOCKER_SIM_NWNAME" --filter "label=a1sim" --filter "status=running" --format {{.Names}})
    oranrics=$(docker ps --filter "name=$RIC_SIM_PREFIX" --filter "network=$DOCKER_SIM_NWNAME" --filter "label=orana1sim" --filter "status=running" --format {{.Names}})

    rics="$rics $oranrics"

    if [ $? -ne 0 ] || [ -z "$rics" ]; then
      echo -e $RED" FAIL - the names of the running RIC Simulator or ORAN RIC cannot be retrieved." $ERED
      ((RES_CONF_FAIL++))
      exit 1
    fi
  fi
  cntr=0
  for ric in $rics; do
    if [ $cntr -gt 0 ]; then
      config_json=$config_json"\n          ,"
    fi
    config_json=$config_json"\n          {"
    if [ $RUNMODE == "KUBE" ]; then
      ric_id=${ric%.*.*} #extract pod id from full hosthame
      ric_id=$(echo "$ric_id" | tr '-' '_')
    else
      ric_id=$(echo "$ric" | tr '-' '_') #ric var still needs underscore as it is different from the container name
    fi
    echo " Found a1 sim: "$ric
    config_json=$config_json"\n            \"name\": \"$ric_id\","

    xricfound=0
    for xric in $oranrics; do
      if [ $xric == $ric ]; then
        xricfound=1
      fi
    done
    if [ $xricfound -eq 0 ]; then
      config_json=$config_json"\n            \"baseUrl\": \"$RIC_SIM_HTTPX://$ric:$RIC_SIM_PORT\","
    else
      config_json=$config_json"\n            \"baseUrl\": \"$RICMEDIATOR_SIM_HTTPX://$ric:$RICMEDIATOR_SIM_PORT\","
    fi
    if [ ! -z "$3" ]; then
      if [[ $ric == "$3"* ]]; then
        config_json=$config_json"\n            \"customAdapterClass\": \"$4\","
      fi
    fi
    if [ $1 == "SDNC" ]; then
      config_json=$config_json"\n            \"controller\": \"$SDNC_APP_NAME\","
    fi
    config_json=$config_json"\n            \"managedElementIds\": ["
    config_json=$config_json"\n              \"me1_$ric_id\","
    config_json=$config_json"\n              \"me2_$ric_id\""
    config_json=$config_json"\n            ]"
    config_json=$config_json"\n          }"
    let cntr=cntr+1
  done

  config_json=$config_json"\n           ]"
  config_json=$config_json"\n}"

  config_json="{\"config\":"$config_json"}"

  printf "$config_json" >$2

  echo ""
}

# Load the the appl config for the a1pms into a config map
a1pms_load_config() {
  echo -e $BOLD"A1PMS - load config from "$EBOLD$1
  data_json=$PWD/tmp/$A1PMS_DATA_FILE
  cp $1 $data_json
  output_yaml=$PWD/tmp/a1pms-cfd.yaml
  __kube_create_configmap $A1PMS_APP_NAME"-data" $KUBE_NONRTRIC_NAMESPACE autotest A1PMS $data_json $output_yaml
  echo ""
}

# Turn on debug level tracing in the a1pms
# args: -
# (Function for test scripts)
set_a1pms_debug() {
  echo -e $BOLD"Setting a1pms debug logging"$EBOLD
  curlString="$A1PMS_SERVICE_PATH$A1PMS_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"debug\"}"
  result=$(__do_curl "$curlString")
  if [ $? -ne 0 ]; then
    __print_err "could not set debug mode" $@
    ((RES_CONF_FAIL++))
    return 1
  fi
  echo ""
  return 0
}

# Turn on trace level tracing in the a1pms
# args: -
# (Function for test scripts)
set_a1pms_trace() {
  echo -e $BOLD"Setting a1pms trace logging"$EBOLD
  curlString="$A1PMS_SERVICE_PATH$A1PMS_ACTUATOR -X POST  -H Content-Type:application/json -d {\"configuredLevel\":\"trace\"}"
  result=$(__do_curl "$curlString")
  if [ $? -ne 0 ]; then
    __print_err "could not set trace mode" $@
    ((RES_CONF_FAIL++))
    return 1
  fi
  echo ""
  return 0
}

# Perform curl retries when making direct call to the a1pms for the specified http response codes
# Speace separated list of http response codes
# args: [<response-code>]*
use_a1pms_retries() {
  echo -e $BOLD"Do curl retries to the a1pms REST inteface for these response codes:$@"$EBOLD
  AGENT_RETRY_CODES=$@
  echo ""
  return
}

# Check the a1pms logs for WARNINGs and ERRORs
# args: -
# (Function for test scripts)
check_a1pms_logs() {
  __check_container_logs "A1PMS" $A1PMS_APP_NAME $A1PMS_LOGPATH WARN ERR
}

#########################################################
#### Test case functions A1 Policy management service
#########################################################

# This function compare the size, towards a target value, of a json array returned from <url> of the A1PMS.
# This is done immediately by setting PASS or FAIL or wait up to and optional timeout before setting PASS or FAIL
# args: json:<url> <target-value> [<timeout-in-seconds]
# (Function for test scripts)
a1pms_equal() {
  echo "(${BASH_LINENO[0]}): ${FUNCNAME[0]}" $@ >>$HTTPLOG
  if [ $# -eq 2 ] || [ $# -eq 3 ]; then
    if [[ $1 == "json:"* ]]; then
      if [ "$A1PMS_VERSION" == "V2" ]; then
        __var_test "A1PMS" $A1PMS_SERVICE_PATH$A1PMS_API_PREFIX"/v2/" $1 "=" $2 $3
      elif [ "$A1PMS_VERSION" == "V3" ]; then
        echo "var test execution for V3"
        __var_test "A1PMS" $A1PMS_SERVICE_PATH$A1PMS_API_PREFIX"/v1/" $1 "=" $2 $3
      else
        __var_test "A1PMS" $A1PMS_SERVICE_PATH"/" $1 "=" $2 $3
      fi
      return 0
    fi
  fi
  __print_err "needs two or three args: json:<json-array-param> <target-value> [ timeout ]" $@
  return 1
}

# API Test function: GET /policies and V2 GET /v2/policy-instances
# args: <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <template-file>]*]
# args(V2): <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <transient> <notification-url> <template-file>]*]
# (Function for test scripts)
a1pms_api_get_policies() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    paramError=0
    variableParams=$(($# - 4))
    if [ $# -lt 4 ]; then
      paramError=1
    elif [ $# -eq 5 ] && [ $5 != "NOID" ]; then
      paramError=1
    elif [ $# -gt 5 ] && [ $(($variableParams % 7)) -ne 0 ]; then
      paramError=1
    fi

    if [ $paramError -ne 0 ]; then
      __print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <transient> <notification-url> <template-file>]*]" $@
      return 1
    fi
  else
    paramError=0
    variableParams=$(($# - 4))
    if [ $# -lt 4 ]; then
      paramError=1
    elif [ $# -eq 5 ] && [ $5 != "NOID" ]; then
      paramError=1
    elif [ $# -gt 5 ] && [ $(($variableParams % 5)) -ne 0 ]; then
      paramError=1
    fi

    if [ $paramError -ne 0 ]; then
      __print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <template-file>]*]" $@
      return 1
    fi
  fi

  queryparams=""
  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $2 != "NORIC" ]; then
      queryparams="?ric_id="$2
    fi
    if [ $3 != "NOSERVICE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?service_id="$3
      else
        queryparams=$queryparams"&service_id="$3
      fi
    fi
    if [ $4 != "NOTYPE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?policytype_id="$4
      else
        queryparams=$queryparams"&policytype_id="$4
      fi
    fi

    query="/v2/policy-instances"$queryparams
    res="$(__do_curl_to_api A1PMS GET $query)"
    status=${res:${#res}-3}

    if [ $status -ne $1 ]; then
      __log_test_fail_status_code $1 $status
      return 1
    fi

    if [ $# -gt 4 ]; then
      body=${res:0:${#res}-3}
      if [ $# -eq 5 ] && [ $5 == "NOID" ]; then
        targetJson="["
      else
        targetJson="["
        arr=(${@:5})

        for ((i = 0; i < $(($# - 4)); i = i + 7)); do

          if [ "$targetJson" != "[" ]; then
            targetJson=$targetJson","
          fi
          targetJson=$targetJson"{\"policy_id\":\"$UUID${arr[$i]}\",\"ric_id\":\"${arr[$i + 1]}\",\"service_id\":\"${arr[$i + 2]}\",\"policytype_id\":"
          if [ "${arr[$i + 3]}" == "EMPTY" ]; then
            targetJson=$targetJson"\"\","
          else
            targetJson=$targetJson"\"${arr[$i + 3]}\","
          fi
          targetJson=$targetJson"\"transient\":${arr[$i + 4]},\"status_notification_uri\":\"${arr[$i + 5]}\","
          file="./tmp/.p.json"
          sed 's/XXX/'${arr[$i]}'/g' ${arr[$i + 6]} >$file
          json=$(cat $file)
          targetJson=$targetJson"\"policy_data\":"$json"}"
        done
      fi

      targetJson=$targetJson"]"
      targetJson="{\"policies\": $targetJson}"
      echo "TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")

      if [ $res -ne 0 ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  else
    if [ $2 != "NORIC" ]; then
      queryparams="?ric="$2
    fi
    if [ $3 != "NOSERVICE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?service="$3
      else
        queryparams=$queryparams"&service="$3
      fi
    fi
    if [ $4 != "NOTYPE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?type="$4
      else
        queryparams=$queryparams"&type="$4
      fi
    fi

    query="/policies"$queryparams
    res="$(__do_curl_to_api A1PMS GET $query)"
    status=${res:${#res}-3}

    if [ $status -ne $1 ]; then
      __log_test_fail_status_code $1 $status
      return 1
    fi

    if [ $# -gt 4 ]; then
      if [ $# -eq 5 ] && [ $5 == "NOID" ]; then
        targetJson="["
      else
        body=${res:0:${#res}-3}
        targetJson="["
        arr=(${@:5})

        for ((i = 0; i < $(($# - 4)); i = i + 5)); do

          if [ "$targetJson" != "[" ]; then
            targetJson=$targetJson","
          fi
          targetJson=$targetJson"{\"id\":\"$UUID${arr[$i]}\",\"lastModified\":\"????\",\"ric\":\"${arr[$i + 1]}\",\"service\":\"${arr[$i + 2]}\",\"type\":"
          if [ "${arr[$i + 3]}" == "EMPTY" ]; then
            targetJson=$targetJson"\"\","
          else
            targetJson=$targetJson"\"${arr[$i + 3]}\","
          fi
          file="./tmp/.p.json"
          sed 's/XXX/'${arr[$i]}'/g' ${arr[$i + 4]} >$file
          json=$(cat $file)
          targetJson=$targetJson"\"json\":"$json"}"
        done
      fi

      targetJson=$targetJson"]"
      echo "TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")

      if [ $res -ne 0 ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  fi
  __collect_endpoint_stats "A1PMS" 00 "GET" $A1PMS_API_PREFIX"/v2/policy-instances" $status
  __log_test_pass
  return 0

}

# API Test function: GET /policy, V2 GET /v2/policies/{policy_id} and V3 GET a1-policy-management/v1/policies/{policy_id}
# args: <response-code>  <policy-id> [<template-file>]
# args(V2): <response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>|NOTYPE <transient> <notification-url>|NOURL ]
# (Function for test scripts)
a1pms_api_get_policy() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -ne 2 ] && [ $# -ne 8 ]; then
      __print_err "<response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>|NOTYPE <transient> <notification-url>|NOURL ]" $@
      return 1
    fi
    query="/v2/policies/$UUID$2"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    if [ $# -ne 2 ] && [ $# -ne 8 ]; then
      __print_err "<response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>|NOTYPE <transient> <notification-url>|NOURL ]" $@
      return 1
    fi
    query="/v1/policies/$UUID$2"
  else
    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
      __print_err "<response-code>  <policy-id> [<template-file>] " $@
      return 1
    fi
    query="/policy?id=$UUID$2"
  fi
  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -eq 8 ]; then

      #Create a policy json to compare with
      body=${res:0:${#res}-3}

      targetJson="\"ric_id\":\"$5\",\"policy_id\":\"$UUID$2\",\"service_id\":\"$4\""
      if [ $7 != "NOTRANSIENT" ]; then
        targetJson=$targetJson", \"transient\":$7"
      fi
      if [ $6 != "NOTYPE" ]; then
        targetJson=$targetJson", \"policytype_id\":\"$6\""
      else
        targetJson=$targetJson", \"policytype_id\":\"\""
      fi
      if [ $8 != "NOURL" ]; then
        targetJson=$targetJson", \"status_notification_uri\":\"$8\""
      fi

      data=$(sed 's/XXX/'${2}'/g' $3)
      targetJson=$targetJson", \"policy_data\":$data"
      targetJson="{$targetJson}"

      echo "TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")
      if [ $res -ne 0 ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    if [ $# -eq 8 ]; then
      #Create a policy json to compare with
      body=${res:0:${#res}-3}
      data=$(sed 's/XXX/'${2}'/g' $3)
      targetJson=$data

      echo "TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")
      if [ $res -ne 0 ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  else
    if [ $# -eq 3 ]; then
      #Create a policy json to compare with
      body=${res:0:${#res}-3}
      file="./tmp/.p.json"
      sed 's/XXX/'${2}'/g' $3 >$file
      targetJson=$(<$file)
      echo "TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")
      if [ $res -ne 0 ]; then
        __log_test_fail_body
      fi
    fi
  fi

  __collect_endpoint_stats "A1PMS" 01 "GET" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

# API Test function: PUT /policy and V2 PUT /policies
# args: <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <template-file> [<count>]
# args(V2): <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <notification-url>|NOURL <template-file> [<count>]
# (Function for test scripts)
a1pms_api_put_policy() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ] || [ "$A1PMS_VERSION" == "V3" ]; then
    if [ $# -lt 8 ] || [ $# -gt 9 ]; then
      __print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <notification-url>|NOURL <template-file> [<count>]" $@
      return 1
    fi
  else
    if [ $# -lt 7 ] || [ $# -gt 8 ]; then
      __print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <template-file> [<count>]" $@
      return 1
    fi
  fi

  count=0
  max=1
  serv=$2
  ric=$3
  pt=$4
  pid=$5
  trans=$6

  if [ "$A1PMS_VERSION" == "V2" ]; then
    noti=$7
    temp=$8
    if [ $# -eq 9 ]; then
      max=$9
    fi
  else
    temp=$7
    if [ $# -eq 8 ]; then
      max=$8
    fi
  fi

  while [ $count -lt $max ]; do
    if [ "$A1PMS_VERSION" == "V2" ]; then

      query="/v2/policies"

      inputJson="\"ric_id\":\"$ric\",\"policy_id\":\"$UUID$pid\",\"service_id\":\"$serv\""
      if [ $trans != "NOTRANSIENT" ]; then
        inputJson=$inputJson", \"transient\":$trans"
      fi
      if [ $pt != "NOTYPE" ]; then
        inputJson=$inputJson", \"policytype_id\":\"$pt\""
      else
        inputJson=$inputJson", \"policytype_id\":\"\""
      fi
      if [ $noti != "NOURL" ]; then
        inputJson=$inputJson", \"status_notification_uri\":\"$noti\""
      fi
      file="./tmp/.p.json"
      data=$(sed 's/XXX/'${pid}'/g' $temp)
      inputJson=$inputJson", \"policy_data\":$data"
      inputJson="{$inputJson}"
      echo $inputJson >$file
    else
      query="/policy?id=$UUID$pid&ric=$ric&service=$serv"

      if [ $pt != "NOTYPE" ]; then
        query=$query"&type=$pt"
      fi

      if [ $trans != NOTRANSIENT ]; then
        query=$query"&transient=$trans"
      fi

      file="./tmp/.p.json"
      sed 's/XXX/'${pid}'/g' $temp >$file
    fi
    res="$(__do_curl_to_api A1PMS PUT $query $file)"
    status=${res:${#res}-3}
    echo -ne " Executing "$count"("$max")${SAMELINE}"
    if [ $status -ne $1 ]; then
      echo " Executed "$count"?("$max")"
      __log_test_fail_status_code $1 $status
      return 1
    fi
    let pid=$pid+1
    let count=$count+1
    echo -ne " Executed  "$count"("$max")${SAMELINE}"
  done
  __collect_endpoint_stats "A1PMS" 02 "PUT" ${A1PMS_API_PREFIX}${query} ${status} ${max}
  echo ""

  __log_test_pass
  return 0
}

# API Test function: V3 PUT a1-policy-management/v1/policies
# args: <response-code>  <policy-id>  <template-file> [<count>]
# args(V2): <response-code> <policy-id> <template-file> [<count>]
# (Function for test scripts)
a1pms_api_put_policy_v3() {
  __log_test_start $@

  if [ $# -lt 3 ] || [ $# -gt 4 ]; then
    __print_err "<response-code> <policy-id> <template-file> [<count>]" $@
    return 1
  fi

  count=0
  max=1
  pid=$2
  temp=$3

  if [ $# -eq 4 ]; then
    max=$4
  fi

  while [ $count -lt $max ]; do
    query="/v1/policies/$UUID$pid"
    file="./tmp/.p_v3.json"
    let update_value=$pid+300
    data=$(sed 's/XXX/'${update_value}'/g' $temp)
    inputJson="$data"
    echo $inputJson >$file
    res="$(__do_curl_to_api A1PMS PUT $query $file)"
    status=${res:${#res}-3}
    echo -ne " Executing "$count"("$max")${SAMELINE}"
    if [ $status -ne $1 ]; then
      echo " Executed "$count"?("$max")"
      __log_test_fail_status_code $1 $status
      return 1
    fi
    let pid=$pid+1
    let count=$count+1
    echo -ne " Executed  "$count"("$max")${SAMELINE}"
  done
  __collect_endpoint_stats "A1PMS" 02 "PUT" ${A1PMS_API_PREFIX}${query} ${status} ${max}
  echo ""

  __log_test_pass
  return 0
}

# API Test function: V3 POST a1-policy-management/v1/policies
# args: <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <template-file> [<count>]
# args(V2): <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <notification-url>|NOURL <template-file> [<count>]
# (Function for test scripts)
a1pms_api_post_policy_v3() {
  __log_test_start $@

  if [ $# -lt 7 ] || [ $# -gt 8 ]; then
    __print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT  <template-file> [<count>]" $@
    return 1
  fi

  count=0
  max=1
  serv=$2
  ric=$3
  pt=$4
  pid=$5
  trans=$6
  temp=$7
  if [ $# -eq 8 ]; then
    max=$8
  fi

  while [ $count -lt $max ]; do
    query="/v1/policies"

    inputJson="\"nearRtRicId\":\"$ric\""
    if [ $pt != "NOTYPE" ]; then
      inputJson=$inputJson", \"policyTypeId\":\"$pt\""
    else
      inputJson=$inputJson", \"policyTypeId\":\"\""
    fi
    if [ $serv != "NOSERVICE" ]; then
      inputJson=$inputJson", \"serviceId\":\"$serv\""
    fi
    if [ $trans != "NOTRANSIENT" ]; then
      inputJson=$inputJson", \"transient\":\"$trans\""
    fi
    file="./tmp/.p.json"
    data=$(sed 's/XXX/'${pid}'/g' $temp)
    inputJson=$inputJson", \"policyObject\":$data"
    inputJson=$inputJson", \"policyId\":\"$UUID$pid\""
    inputJson="{$inputJson}"
    echo $inputJson >$file
    res="$(__do_curl_to_api A1PMS POST $query $file)"
    status=${res:${#res}-3}
    echo -ne " Executing "$count"("$max")${SAMELINE}"
    if [ $status -ne $1 ]; then
      echo " Executed "$count"?("$max")"
      __log_test_fail_status_code $1 $status
      return 1
    fi
    let pid=$pid+1
    let count=$count+1
    echo -ne " Executed  "$count"("$max")${SAMELINE}"
  done
  __collect_endpoint_stats "A1PMS" 02 "PUT" ${A1PMS_API_PREFIX}${A1PMS_VERSION} ${status} ${max}
  echo ""

  __log_test_pass
  return 0
}

# API Test function: PUT /policy and V2 PUT /policies, to run in batch
# args: <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <template-file> [<count>]
# args(V2): <response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <notification-url>|NOURL <template-file> [<count>]
# (Function for test scripts)

a1pms_api_put_policy_batch() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -lt 8 ] || [ $# -gt 9 ]; then
      __print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <notification-url>|NOURL <template-file> [<count>]" $@
      return 1
    fi
  else
    if [ $# -lt 7 ] || [ $# -gt 8 ]; then
      __print_err "<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient> <template-file> [<count>]" $@
      return 1
    fi
  fi

  count=0
  max=1
  serv=$2
  ric=$3
  pt=$4
  pid=$5
  trans=$6
  if [ "$A1PMS_VERSION" == "V2" ]; then
    noti=$7
    temp=$8
    if [ $# -eq 9 ]; then
      max=$9
    fi
  else
    temp=$7
    if [ $# -eq 8 ]; then
      max=$8
    fi
  fi

  ARR=""
  while [ $count -lt $max ]; do
    if [ "$A1PMS_VERSION" == "V2" ]; then
      query="/v2/policies"

      inputJson="\"ric_id\":\"$ric\",\"policy_id\":\"$UUID$pid\",\"service_id\":\"$serv\""
      if [ $trans != "NOTRANSIENT" ]; then
        inputJson=$inputJson", \"transient\":$trans"
      fi
      if [ $pt != "NOTYPE" ]; then
        inputJson=$inputJson", \"policytype_id\":\"$pt\""
      else
        inputJson=$inputJson", \"policytype_id\":\"\""
      fi
      if [ $noti != "NOURL" ]; then
        inputJson=$inputJson", \"status_notification_uri\":\"$noti\""
      fi
      file="./tmp/.p.json"
      data=$(sed 's/XXX/'${pid}'/g' $temp)
      inputJson=$inputJson", \"policy_data\":$data"
      inputJson="{$inputJson}"
      echo $inputJson >$file
    else
      query="/policy?id=$UUID$pid&ric=$ric&service=$serv"

      if [ $pt != "NOTYPE" ]; then
        query=$query"&type=$pt"
      fi

      if [ $trans != NOTRANSIENT ]; then
        query=$query"&transient=$trans"
      fi
      file="./tmp/.p.json"
      sed 's/XXX/'${pid}'/g' $temp >$file
    fi
    res="$(__do_curl_to_api A1PMS PUT_BATCH $query $file)"
    status=${res:${#res}-3}
    echo -ne " Requesting(batch) "$count"("$max")${SAMELINE}"

    if [ $status -ne 200 ]; then
      echo " Requested(batch) "$count"?("$max")"
      __log_test_fail_status_code 200 $status
      return 1
    fi
    cid=${res:0:${#res}-3}
    ARR=$ARR" "$cid
    let pid=$pid+1
    let count=$count+1
    echo -ne " Requested(batch)  "$count"("$max")${SAMELINE}"
  done

  echo ""
  count=0
  for cid in $ARR; do

    res="$(__do_curl_to_api A1PMS RESPONSE $cid)"
    status=${res:${#res}-3}
    echo -ne " Accepting(batch) "$count"("$max")${SAMELINE}"

    if [ $status -ne $1 ]; then
      echo " Accepted(batch) "$count"?("$max")"
      __log_test_fail_status_code $1 $status
      return 1
    fi

    let count=$count+1
    echo -ne " Accepted(batch)  "$count"("$max")${SAMELINE}"
  done
  __collect_endpoint_stats "A1PMS" 02 "PUT" $A1PMS_API_PREFIX"/v2/policies" $1 $max

  echo ""

  __log_test_pass
  return 0
}

# API Test function: PUT /policy and V2 PUT /policies, to run in i parallel for a number of rics
# args: <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>
# args(V2): <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>
# (Function for test scripts)
a1pms_api_put_policy_parallel() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -ne 11 ]; then
      __print_err "These all arguments needed <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>" $@
      return 1
    fi
  fi

  resp_code=$1
  shift
  serv=$1
  shift
  ric_base=$1
  shift
  num_rics=$1
  shift
  type=$1
  shift
  start_id=$1
  shift
  transient=$1
  shift
  if [ "$A1PMS_VERSION" == "V2" ]; then
    noti=$1
    shift
  else
    noti=""
  fi
  template=$1
  shift
  count=$1
  shift
  pids=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_put_policy_parallel uses only the a1pms REST interface - create over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi
  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $serv == "NOSERVICE" ]; then
      serv=""
    fi
    query="$A1PMS_API_PREFIX/v2/policies"
  else
    if [ $serv == "NOSERVICE" ]; then
      serv=""
    fi
    query="/policy?service=$serv"

    if [ $type != "NOTYPE" ]; then
      query=$query"&type=$type"
    fi

    if [ $transient != NOTRANSIENT ]; then
      query=$query"&transient=$transient"
    fi
  fi

  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    uuid=$UUID
    if [ -z "$uuid" ]; then
      uuid="NOUUID"
    fi
    echo "" >"./tmp/.pid${i}.res.txt"
    if [ "$A1PMS_VERSION" == "V2" ]; then
      echo $resp_code $urlbase $ric_base $num_rics $uuid $start_id $serv $type $transient $noti $template $count $pids $i $httpproxy >"./tmp/.pid${i}.txt"
    else
      echo $resp_code $urlbase $ric_base $num_rics $uuid $start_id $template $count $pids $i $httpproxy >"./tmp/.pid${i}.txt"
    fi
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.txt")
		python3 ../common/create_policies_process.py $tmp > ./tmp/.pid${arg}.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 02 "PUT" $A1PMS_API_PREFIX"/v2/policies" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi
  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: V3 POST /policies, to run in i parallel for a number of rics
# args: <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>
# args(V3): <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>
# (Function for test scripts)
a1pms_api_post_policy_parallel() {
  __log_test_start $@

  if [ $# -ne 10 ]; then
    __print_err "These all arguments needed <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>" $@
    return 1
  fi

  resp_code=$1
  shift
  serv=$1
  shift
  ric_base=$1
  shift
  num_rics=$1
  shift
  type=$1
  shift
  start_id=$1
  shift
  transient=$1
  shift
  template=$1
  shift
  count=$1
  shift
  pids=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_put_policy_parallel uses only the a1pms REST interface - create over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi

  if [ $serv == "NOSERVICE" ]; then
    serv=""
  fi
  query="$A1PMS_API_PREFIX/v1/policies"

  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    uuid=$UUID
    if [ -z "$uuid" ]; then
      uuid="NOUUID"
    fi
    echo "" >"./tmp/.pid${i}.res.txt"
    echo $resp_code $urlbase $ric_base $num_rics $uuid $start_id $serv $type $transient "noValue" $template $count $pids $i $httpproxy >"./tmp/.pid${i}.txt"
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.txt")
		python3 ../common/create_policies_process.py $tmp > ./tmp/.pid${arg}.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 02 "POST" $A1PMS_API_PREFIX"/v1/policies" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi
  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: V3 PUT /policies, to run in i parallel for a number of rics
# args: <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads> <policy-ids-file-path>
# args(V3): <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads> <policy-ids-file-path>
# (Function for test scripts)
a1pms_api_update_policy_parallel() {
  __log_test_start $@

  if [ $# -ne 12 ]; then
    __print_err "These all arguments needed <response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads> <policy-ids-file-path>" $@
    return 1
  fi

  resp_code=$1
  shift
  serv=$1
  shift
  ric_base=$1
  shift
  num_rics=$1
  shift
  type=$1
  shift
  start_id=$1
  shift
  transient=$1
  shift
  noti=$1
  shift
  template=$1
  shift
  count=$1
  shift
  pids=$1
  shift
  policy_ids_file_path=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_put_policy_parallel uses only the a1pms REST interface - create over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi

  if [ $serv == "NOSERVICE" ]; then
    serv=""
  fi
  query="$A1PMS_API_PREFIX/v1/policies"

  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    uuid=$UUID
    if [ -z "$uuid" ]; then
      uuid="NOUUID"
    fi
    echo "" >"./tmp/.pid${i}.res.txt"
    echo $resp_code $urlbase $ric_base $num_rics $uuid $start_id $serv $type $transient $noti $template $count $pids $i $httpproxy $policy_ids_file_path >"./tmp/.pid${i}.txt"
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.txt")
      python3 ../common/update_policies_process.py $tmp > ./tmp/.pid${arg}.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 02 "POST" $A1PMS_API_PREFIX"/v1/policies" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi
  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: DELETE /policy, V2 DELETE /v2/policies/{policy_id} and V3 DELETE a1-policy-management/v1/policies/{policy_id}
# args: <response-code> <policy-id> [count]
# (Function for test scripts)
a1pms_api_delete_policy() {
  __log_test_start $@

  if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    __print_err "<response-code> <policy-id> [count]" $@
    return 1
  fi

  count=0
  max=1

  if [ $# -eq 3 ]; then
    max=$3
  fi

  pid=$2

  while [ $count -lt $max ]; do
    if [ "$A1PMS_VERSION" == "V2" ]; then
      query="/v2/policies/"$UUID$pid
    elif [ "$A1PMS_VERSION" == "V3" ]; then
      query="/v1/policies/"$UUID$pid
    else
      query="/policy?id="$UUID$pid
    fi
    res="$(__do_curl_to_api A1PMS DELETE $query)"
    status=${res:${#res}-3}
    echo -ne " Executing "${count}"("${max}")${SAMELINE}"

    if [ $status -ne $1 ]; then
      echo " Executed "${count}"?("${max}")"
      __log_test_fail_status_code $1 $status
      return 1
    fi

    let pid=$pid+1
    let count=$count+1
    echo -ne " Executed  "${count}"("${max}")${SAMELINE}"
  done
  __collect_endpoint_stats "A1PMS" 03 "DELETE" ${A1PMS_API_PREFIX}${query} ${status} ${max}
  echo ""

  __log_test_pass
  return 0
}

# API Test function: DELETE /policy and V2 DELETE /v2/policies/{policy_id}, to run in batch
# args: <response-code> <policy-id> [count]
# (Function for test scripts)
a1pms_api_delete_policy_batch() {
  __log_test_start $@

  if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    __print_err "<response-code> <policy-id> [count]" $@
    return 1
  fi

  count=0
  max=1

  if [ $# -eq 3 ]; then
    max=$3
  fi

  pid=$2
  ARR=""
  while [ $count -lt $max ]; do
    if [ "$A1PMS_VERSION" == "V2" ]; then
      query="/v2/policies/"$UUID$pid
    else
      query="/policy?id="$UUID$pid
    fi
    res="$(__do_curl_to_api A1PMS DELETE_BATCH $query)"
    status=${res:${#res}-3}
    echo -ne " Requesting(batch) "$count"("$max")${SAMELINE}"

    if [ $status -ne 200 ]; then
      echo " Requested(batch) "$count"?("$max")"
      __log_test_fail_status_code 200 $status
      return 1
    fi
    cid=${res:0:${#res}-3}
    ARR=$ARR" "$cid
    let pid=$pid+1
    let count=$count+1
    echo -ne " Requested(batch)  "$count"("$max")${SAMELINE}"
  done

  echo ""

  count=0
  for cid in $ARR; do

    res="$(__do_curl_to_api A1PMS RESPONSE $cid)"
    status=${res:${#res}-3}
    echo -ne " Deleting(batch) "$count"("$max")${SAMELINE}"

    if [ $status -ne $1 ]; then
      echo " Deleted(batch) "$count"?("$max")"
      __log_test_fail_status_code $1 $status
      return 1
    fi

    let count=$count+1
    echo -ne " Deleted(batch)  "$count"("$max")${SAMELINE}"
  done
  __collect_endpoint_stats "A1PMS" 03 "DELETE" $A1PMS_API_PREFIX"/v2/policies/{policy_id}" $1 $max

  echo ""

  __log_test_pass
  return 0
}

# API Test function: DELETE /policy and V2 DELETE /v2/policies/{policy_id}, to run in i parallel for a number of rics
# args: <response-code> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>
# (Function for test scripts)
a1pms_api_delete_policy_parallel() {
  __log_test_start $@

  if [ $# -ne 5 ]; then
    __print_err " <response-code> <ric-id-base> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>" $@
    return 1
  fi
  resp_code=$1
  shift
  num_rics=$1
  shift
  start_id=$1
  shift
  count=$1
  shift
  pids=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_delete_policy_parallel uses only the a1pms REST interface - delete over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="$A1PMS_API_PREFIX/v2/policies/"
  else
    query="/policy"
  fi

  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    uuid=$UUID
    if [ -z "$uuid" ]; then
      uuid="NOUUID"
    fi
    echo "" >"./tmp/.pid${i}.del.res.txt"
    echo $resp_code $urlbase $num_rics $uuid $start_id $count $pids $i $httpproxy >"./tmp/.pid${i}.del.txt"
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.del.txt")
		python3 ../common/delete_policies_process.py $tmp > ./tmp/.pid${arg}.del.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.del.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 03 "DELETE" $A1PMS_API_PREFIX"/v2/policies/{policy_id}" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi

  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: V3 DELETE a1-policy-management/v1/policies/{policy_id}, to run in i parallel for a number of rics
# args: <responseCode> <numberOfRics> <PolicyIdsFilePath> <countPerRic> <numberOfThreads>
# (Function for test scripts)
a1pms_api_delete_policy_parallel_v3() {
  __log_test_start $@

  if [ $# -ne 6 ]; then
    __print_err " <responseCode> <numberOfRics> <PolicyIdsFilePath> <StartID> <countPerRic> <numberOfThreads>" $@
    return 1
  fi
  resp_code=$1
  shift
  num_rics=$1
  shift
  policy_ids_file_path=$1
  shift
  start_id=$1
  shift
  count=$1
  shift
  pids=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_delete_policy_parallel uses only the a1pms REST interface - delete over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi

  query="$A1PMS_API_PREFIX/v1/policies/"
  urlbase=${A1PMS_ADAPTER}${query}

  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    echo "" >"./tmp/.pid${i}.del.res.txt"
    echo $resp_code $urlbase $policy_ids_file_path $start_id $pids $i $httpproxy >"./tmp/.pid${i}.del.txt"
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.del.txt")
		python3 ../common/delete_policies_process_v3.py $tmp > ./tmp/.pid${arg}.del.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.del.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 03 "DELETE" $A1PMS_API_PREFIX"/v1/policies/{policy_id}" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi

  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: GET /policy and V2 GET /v2/policies/{policy_id}, to run in i parallel for a number of rics
# args: <response-code> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>
# (Function for test scripts)
a1pms_api_get_policy_parallel() {
  __log_test_start $@

  if [ $# -ne 5 ]; then
    __print_err " <response-code> <ric-id-base> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>" $@
    return 1
  fi
  resp_code=$1
  shift
  num_rics=$1
  shift
  start_id=$1
  shift
  count=$1
  shift
  pids=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_get_policy_parallel uses only the a1pms REST interface - GET over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="$A1PMS_API_PREFIX/v2/policies/"
  else
    query="/policy"
  fi

  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    uuid=$UUID
    if [ -z "$uuid" ]; then
      uuid="NOUUID"
    fi
    echo "" >"./tmp/.pid${i}.get.res.txt"
    echo $resp_code $urlbase $num_rics $uuid $start_id $count $pids $i $httpproxy >"./tmp/.pid${i}.get.txt"
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.get.txt")
		python3 ../common/get_policies_process.py $tmp > ./tmp/.pid${arg}.get.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.get.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 04 "GET" $A1PMS_API_PREFIX"/v2/policies/{policy_id}" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi

  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: V3 GET a1-policy-management/v1/policies/{policy_id}, to run in i parallel for a number of rics
# args: <response-code> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>
# (Function for test scripts)
a1pms_api_get_policy_parallel_v3() {
  __log_test_start $@

  if [ $# -ne 6 ]; then
    __print_err " <responseCode> <numberOfRics> <PolicyIdsFilePath> <startID> <countPerRic> <numberOfThreads>" $@
    return 1
  fi
  resp_code=$1
  shift
  num_rics=$1
  shift
  policy_ids_file_path=$1
  shift
  start_id=$1
  shift
  count=$1
  shift
  pids=$1
  shift

  #if [ $A1PMS_ADAPTER != $RESTBASE ] && [ $A1PMS_ADAPTER != $RESTBASE_SECURE ]; then
  if [ $A1PMS_ADAPTER_TYPE != "REST" ]; then
    echo " Info - a1pms_api_get_policy_parallel uses only the a1pms REST interface - GET over dmaap in parallel is not supported"
    echo " Info - will execute over a1pms REST"
  fi

  query="$A1PMS_API_PREFIX/v1/policies/"
  urlbase=${A1PMS_ADAPTER}${query}

  httpproxy="NOPROXY"
  if [ ! -z "$KUBE_PROXY_PATH" ]; then
    httpproxy=$KUBE_PROXY_PATH
  fi

  for ((i = 1; i <= $pids; i++)); do
    echo "" >"./tmp/.pid${i}.get.res.txt"
    echo $resp_code $urlbase $policy_ids_file_path $start_id $pids $i $httpproxy >"./tmp/.pid${i}.get.txt"
    echo $i
  done | xargs -n 1 -I{} -P $pids bash -c '{
		arg=$(echo {})
		echo " Parallel process $arg started"
		tmp=$(< "./tmp/.pid${arg}.get.txt")
		python3 ../common/get_policies_process_v3.py $tmp > ./tmp/.pid${arg}.get.res.txt
	}'
  msg=""
  for ((i = 1; i <= $pids; i++)); do
    file="./tmp/.pid${i}.get.res.txt"
    tmp=$(<$file)
    if [ -z "$tmp" ]; then
      echo " Process $i : unknown result (result file empty"
      msg="failed"
    else
      res=${tmp:0:1}
      if [ $res == "0" ]; then
        echo " Process $i : OK - "${tmp:1}
      else
        echo " Process $i : failed - "${tmp:1}
        msg="failed"
      fi
    fi
  done
  if [ -z $msg ]; then
    __collect_endpoint_stats "A1PMS" 04 "GET" $A1PMS_API_PREFIX"/v1/policies/{policyId}" $resp_code $(($count * $num_rics))
    __log_test_pass " $(($count * $num_rics)) policy request(s) executed"
    return 0
  fi

  __log_test_fail_general "One of more processes failed to execute"
  return 1
}

# API Test function: GET /policy_ids and V2 GET /v2/policies
# args: <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)
# (Function for test scripts)
a1pms_api_get_policy_ids() {
  __log_test_start $@

  if [ $# -lt 4 ]; then
    __print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)" $@
    return 1
  fi

  queryparams=""

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $2 != "NORIC" ]; then
      queryparams="?ric_id="$2
    fi

    if [ $3 != "NOSERVICE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?service_id="$3
      else
        queryparams=$queryparams"&service_id="$3
      fi
    fi
    if [ $4 != "NOTYPE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?policytype_id="$4
      else
        queryparams=$queryparams"&policytype_id="$4
      fi
    fi

    query="/v2/policies"$queryparams
  else
    if [ $2 != "NORIC" ]; then
      queryparams="?ric="$2
    fi

    if [ $3 != "NOSERVICE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?service="$3
      else
        queryparams=$queryparams"&service="$3
      fi
    fi
    if [ $4 != "NOTYPE" ]; then
      if [ -z $queryparams ]; then
        queryparams="?type="$4
      else
        queryparams=$queryparams"&type="$4
      fi
    fi

    query="/policy_ids"$queryparams
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 4 ]; then
    body=${res:0:${#res}-3}
    targetJson="["

    for pid in ${@:5}; do
      if [ "$targetJson" != "[" ]; then
        targetJson=$targetJson","
      fi
      if [ $pid != "NOID" ]; then
        targetJson=$targetJson"\"$UUID$pid\""
      fi
    done

    targetJson=$targetJson"]"
    if [ "$A1PMS_VERSION" == "V2" ]; then
      targetJson="{\"policy_ids\": $targetJson}"
    fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 04 "GET" $A1PMS_API_PREFIX"/v2/policies" $status
  __log_test_pass
  return 0
}

# API Test function: V3 GET a1-policy-management/v1/policies
# args: <response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)
# (Function for test scripts)
a1pms_api_get_all_policies_v3() {
  __log_test_start $@

  if [ $# -lt 4 ]; then
    __print_err "<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)" $@
    return 1
  fi

  queryparams=""

  if [ $2 != "NORIC" ]; then
    queryparams="?nearRtRicId="$2
  fi

  if [ $3 != "NOSERVICE" ]; then
    if [ -z $queryparams ]; then
      queryparams="?serviceId="$3
    else
      queryparams=$queryparams"&serviceId="$3
    fi
  fi
  if [ $4 != "NOTYPE" ]; then
    if [ -z $queryparams ]; then
      queryparams="?policyTypeId="$4
    else
      queryparams=$queryparams"&policyTypeId="$4
    fi
  fi

  query="/v1/policies"$queryparams

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 4 ]; then
    body=${res:0:${#res}-3}
    targetJson="["

    for pid in ${@:5}; do
      if [ "$targetJson" != "[" ]; then
        targetJson=$targetJson","
      fi
      IFS=':' read -r policy_id ric_id <<<"$pid"
      if [ $policy_id != "NOID" ]; then
        targetJson=$targetJson"{ \"policyId\": \"${UUID}${policy_id}\", \"nearRtRicId\": \"$ric_id\" }"
      fi
    done

    targetJson=$targetJson"]"
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 04 "GET" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

# API Test function: V2 GET a1-policy/v2/policy-types/{policyTypeId} and V3 GET a1-policy-management/v1/policy-types/{policyTypeId}
# args(V2): <response-code> <policy-type-id> [<schema-file>]
# (Function for test scripts)
a1pms_api_get_policy_type() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" != "V2" ] && [ "$A1PMS_VERSION" != "V3" ]; then
    __log_test_fail_not_supported
    return 1
  fi

  if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    __print_err "<response-code> <policy-type-id> [<schema-file>]" $@
    return 1
  fi
  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/policy-types/$2"
  fi
  if [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/policy-types/$2"
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -eq 3 ]; then

    body=${res:0:${#res}-3}

    targetJson=$(<$3)
    if [ "$A1PMS_VERSION" == "V2" ]; then
      targetJson="{\"policy_schema\":$targetJson}"
    elif [ "$A1PMS_VERSION" == "V3" ]; then
      targetJson="{\"policySchema\":$targetJson, \"statusSchema\": null}"
    fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 05 "GET" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

# API Test function: GET /policy_schema
# args: <response-code> <policy-type-id> [<schema-file>]
# (Function for test scripts)
a1pms_api_get_policy_schema() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    __log_test_fail_not_supported
    return 1
  fi

  if [ $# -lt 2 ] || [ $# -gt 3 ]; then
    __print_err "<response-code> <policy-type-id> [<schema-file>]" $@
    return 1
  fi
  query="/policy_schema?id=$2"
  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -eq 3 ]; then

    body=${res:0:${#res}-3}

    targetJson=$(<$3)

    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 06 "GET" $A1PMS_API_PREFIX"/v2/policy_schema" $status
  __log_test_pass
  return 0
}

# API Test function: GET /policy_schemas
# args: <response-code>  <ric-id>|NORIC [<schema-file>|NOFILE]*
# args(V2): <response-code>
# (Function for test scripts)
a1pms_api_get_policy_schemas() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -ne 1 ]; then
      __print_err "<response-code>" $@
      return 1
    fi
  else
    if [ $# -lt 2 ]; then
      __print_err "<response-code> <ric-id>|NORIC [<schema-file>|NOFILE]*" $@
      return 1
    fi
  fi
  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/policy-schemas"
  else
    query="/policy_schemas"
    if [ $2 != "NORIC" ]; then
      query=$query"?ric="$2
    fi
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 2 ]; then
    body=${res:0:${#res}-3}
    targetJson="["

    for file in ${@:3}; do
      if [ "$targetJson" != "[" ]; then
        targetJson=$targetJson","
      fi
      if [ $file == "NOFILE" ]; then
        targetJson=$targetJson"{}"
      else
        targetJson=$targetJson$(<$file)
      fi
    done

    targetJson=$targetJson"]"
    if [ "$A1PMS_VERSION" == "V2" ]; then
      targetJson="{\"policy_schemas\": $targetJson }"
    fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 07 "GET" $A1PMS_API_PREFIX"/v2/policy-schemas" $status
  __log_test_pass
  return 0
}

# API Test function: GET /policy_status and V2 GET /policies/{policy_id}/status
# arg: <response-code> <policy-id> [ (STD|STD2 <enforce-status>|EMPTY [<reason>|EMPTY])|(OSC <instance-status> <has-been-deleted>) ]
# (Function for test scripts)
a1pms_api_get_policy_status() {
  __log_test_start $@

  if [ $# -lt 2 ] || [ $# -gt 5 ]; then
    __print_err "<response-code> <policy-id> [(STD <enforce-status>|EMPTY [<reason>|EMPTY])|(OSC <instance-status> <has-been-deleted>)]" $@
    return 1
  fi

  targetJson=""
  if [ $# -eq 2 ]; then
    :
  elif [ "$3" == "STD" ]; then
    targetJson="{\"enforceStatus\":\"$4\""
    if [ $# -eq 5 ]; then
      targetJson=$targetJson",\"reason\":\"$5\""
    fi
    targetJson=$targetJson"}"
  elif [ "$3" == "STD2" ]; then
    if [ $4 == "EMPTY" ]; then
      targetJson="{\"enforceStatus\":\"\""
    else
      targetJson="{\"enforceStatus\":\"$4\""
    fi
    if [ $# -eq 5 ]; then
      if [ $5 == "EMPTY" ]; then
        targetJson=$targetJson",\"enforceReason\":\"\""
      else
        targetJson=$targetJson",\"enforceReason\":\"$5\""
      fi
    fi
    targetJson=$targetJson"}"
  elif [ "$3" == "OSC" ]; then
    if [[ $TEST_ENV_PROFILE =~ ^ORAN-[A-H] ]] || [[ $TEST_ENV_PROFILE =~ ^ONAP-[A-L] ]]; then
      targetJson="{\"instance_status\":\"$4\""
      if [ $# -eq 5 ]; then
        targetJson=$targetJson",\"has_been_deleted\":\"$5\""
      fi
      targetJson=$targetJson",\"created_at\":\"????\"}"
    else
      targetJson="{\"enforceStatus\":\"$4\""
      if [ $# -eq 5 ]; then
        targetJson=$targetJson",\"enforceReason\":\"$5\"}"
      fi
    fi
  else
    __print_err "<response-code> (STD <enforce-status> [<reason>])|(OSC <instance-status> <has-been-deleted>)" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/policies/$UUID$2/status"
    targetJson="{\"last_modified\":\"????\",\"status\":$targetJson}"
  else
    query="/policy_status?id="$UUID$2
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi
  if [ $# -gt 2 ]; then
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    body=${res:0:${#res}-3}
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi
  __collect_endpoint_stats "A1PMS" 08 "GET" $A1PMS_API_PREFIX"/v2/policies/{policy_id}/status" $status
  __log_test_pass
  return 0
}

# API Test function: GET /policy_types and V2 GET /v2/policy-types
# args: <response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]
# (Function for test scripts)
a1pms_api_get_policy_types() {
  __log_test_start $@

  if [ $# -lt 1 ]; then
    __print_err "<response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -eq 1 ]; then
      query="/v2/policy-types"
    elif [ $2 == "NORIC" ]; then
      query="/v2/policy-types"
    else
      query="/v2/policy-types?ric_id=$2"
    fi
  else
    if [ $# -eq 1 ]; then
      query="/policy_types"
    elif [ $2 == "NORIC" ]; then
      query="/policy_types"
    else
      query="/policy_types?ric=$2"
    fi
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 2 ]; then
    body=${res:0:${#res}-3}
    targetJson="["

    for pid in ${@:3}; do
      if [ "$targetJson" != "[" ]; then
        targetJson=$targetJson","
      fi
      if [ $pid == "EMPTY" ]; then
        pid=""
      fi
      targetJson=$targetJson"\"$pid\""
    done

    targetJson=$targetJson"]"
    if [ "$A1PMS_VERSION" == "V2" ]; then
      targetJson="{\"policytype_ids\": $targetJson }"
    fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 09 "GET" $A1PMS_API_PREFIX"/v2/policy-types" $status
  __log_test_pass
  return 0
}

# API Test function:  V3 GET a1-policy-management/v1/policy-types
# args: <response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]
# (Function for test scripts)
a1pms_api_get_policy_types_v3() {
  __log_test_start $@

  if [ $# -lt 1 ]; then
    __print_err "<response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]" $@
    return 1
  fi

  if [ $# -eq 1 ]; then
    query="/v1/policy-types"
  elif [ $2 == "NORIC" ]; then
    query="/v1/policy-types"
  else
    query="/v1/policy-types?nearRtRicId=$2"
  fi
  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 2 ]; then
    body=${res:0:${#res}-3}
    targetJson="["

    for pid in ${@:3}; do
      if [ "$targetJson" != "[" ]; then
        targetJson=$targetJson","
      fi
      IFS=':' read -r policy_type_id ric_id <<<"$pid"
      #			if [ -n "$policy_type_id" ] && [ -n "$ric_id" ]; then
      if [ $policy_type_id == "EMPTY" ]; then
        policy_type_id=""
      fi
      targetJson=$targetJson"{ \"policyTypeId\": \"$policy_type_id\", \"nearRtRicId\": \"$ric_id\" }"
      #			fi
    done

    targetJson=$targetJson"]"

    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 09 "GET" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

# API Test function: GET /policies/{{policyId}}/status
# args: <response-code> <policy-id> [STD2 <enforce-status>|EMPTY [<reason>|EMPTY]]
# (Function for test scripts)
a1pms_api_get_policy_status_v3() {
  __log_test_start $@

  if [ $# -lt 2 ] || [ $# -gt 6 ]; then
    __print_err "<response-code> <policy-id> [STD2 <enforce-status>|EMPTY [<reason>|EMPTY]]" $@
    return 1
  fi

  targetJson=""
  if [ $# -eq 2 ]; then
    :
  elif [ "$3" == "STD2" ]; then
    if [ $4 == "EMPTY" ]; then
      targetJson="{\"enforceStatus\":\"\""
    else
      targetJson="{\"enforceStatus\":\"$4\""
    fi
    if [ $# -eq 5 ]; then
      if [ $5 == "EMPTY" ]; then
        targetJson=$targetJson",\"enforceReason\":\"\""
      else
        targetJson=$targetJson",\"enforceReason\":\"$5\""
      fi
    fi
    targetJson=$targetJson"}"
  else
    __print_err "<response-code> <policy-id> [STD2 <enforce-status>|EMPTY [<reason>|EMPTY]]" $@
    return 1
  fi

  query="/v1/policies/$UUID$2/status"

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi
  if [ $# -gt 2 ]; then
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    body=${res:0:${#res}-3}
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi
  __collect_endpoint_stats "A1PMS" 08 "GET" $A1PMS_API_PREFIX"/v1/policies/{policyId}/status" $status
  __log_test_pass
  return 0
}

#########################################################
#### Test case functions Health check
#########################################################

# API Test function: GET /status and V2 GET /status or (v1/status for a1pmsV3)
# args: <response-code>
# (Function for test scripts)
a1pms_api_get_status() {
  __log_test_start $@
  if [ $# -ne 1 ]; then
    __print_err "<response-code>" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/status"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/status"
  else
    query="/status"
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 10 "GET" $A1PMS_API_PREFIX$query $status
  __log_test_pass
  return 0
}

# API Test function: GET /status (root) without api prefix
# args: <response-code>
# (Function for test scripts)
a1pms_api_get_status_root() {
  __log_test_start $@
  if [ $# -ne 1 ]; then
    __print_err "<response-code>" $@
    return 1
  fi
  query="/status"
  TMP_PREFIX=$A1PMS_API_PREFIX
  A1PMS_API_PREFIX=""
  res="$(__do_curl_to_api A1PMS GET $query)"
  A1PMS_API_PREFIX=$TMP_PREFIX
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 19 "GET" "/status" $status
  __log_test_pass
  return 0
}

#########################################################
#### Test case functions RIC Repository
#########################################################

# API Test function: GET /ric, V2 GET /v2/rics/ric, and V3 GET a1-policy-management/v1/rics/ric
# args: <reponse-code> <management-element-id> [<ric-id>]
# (V2) args: <reponse-code> <management-element-id>|NOME <ric-id>|<NORIC> [<string-of-ricinfo>]
# (V2) example of <string-of-ricinfo> = "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2,4"
# (V2) format of ric-info:  <ric-id>:<list-of-mes>:<list-of-policy-type-ids>

# (Function for test scripts)
a1pms_api_get_ric() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" == "V2" ]; then
    if [ $# -lt 3 ]; then
      __print_err "<reponse-code> <management-element-id>|NOME <ric-id>|<NORIC> [string-of-ricinfo>]" $@
      return 1
    fi
    search=""
    if [ $2 != "NOME" ]; then
      search="?managed_element_id="$2
    fi
    if [ $3 != "NORIC" ]; then
      if [ -z $search ]; then
        search="?ric_id="$3
      else
        search=$search"&ric_id="$3
      fi
    fi
    query="/v2/rics/ric"$search

    res="$(__do_curl_to_api A1PMS GET $query)"
    status=${res:${#res}-3}

    if [ $status -ne $1 ]; then
      __log_test_fail_status_code $1 $status
      return 1
    fi

    if [ $# -gt 3 ]; then
      body=${res:0:${#res}-3}
      res=$(python3 ../common/create_rics_json.py "./tmp/.tmp_rics.json" "V2" "$4")
      if [ $res -ne 0 ]; then
        __log_test_fail_general "Could not create target ric info json"
        return 1
      fi

      targetJson=$(<./tmp/.tmp_rics.json)
      targetJson=${targetJson:1:${#targetJson}-2} #remove array brackets
      echo " TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")
      if [ $res -ne 0 ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    if [ $# -lt 3 ]; then
      __print_err "<reponseCode> <managementElementId>|NOME <ricId>|<NORIC> [stringOfRicInfo>]" $@
      return 1
    fi

    search=""

    if [ $3 != "NORIC" ]; then

      if [ $2 != "NOME" ]; then
        search="/"$3"?managedElementId="$2
      else
        search="/"$3
      fi
    elif [ $# -gt 3 ]; then
      search="/${4%%:*}"
    else
      search="/test"
    fi

#    if [ $3 = "NORIC" ] && [ $# -lt 4 ]; then
#      search="/test"
#    fi

    query="/v1/rics"$search

    res="$(__do_curl_to_api A1PMS GET $query)"
    status=${res:${#res}-3}

    if [ $status -ne $1 ]; then
      __log_test_fail_status_code $1 $status
      return 1
    fi

    if [ $# -gt 3 ]; then
      body=${res:0:${#res}-3}
      res=$(python3 ../common/create_rics_json.py "./tmp/.tmp_rics.json" "${A1PMS_VERSION}" "$4")
      if [ $res -ne 0 ]; then
        __log_test_fail_general "Could not create target ric info json"
        return 1
      fi

      targetJson=$(<./tmp/.tmp_rics.json)
      targetJson=${targetJson:1:${#targetJson}-2} #remove array brackets
      echo " TARGET JSON: $targetJson" >>$HTTPLOG
      res=$(python3 ../common/compare_json.py "$targetJson" "$body")
      if [ $res -ne 0 ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  else
    if [ $# -lt 2 ] || [ $# -gt 3 ]; then
      __print_err "<reponseCode> <managedElementIds> [<ricId>]" $@
      return 1
    fi

    query="/ric?managedElementId="$2

    res="$(__do_curl_to_api A1PMS GET $query)"
    status=${res:${#res}-3}

    if [ $status -ne $1 ]; then
      __log_test_fail_status_code $1 $status
      return 1
    fi

    if [ $# -eq 3 ]; then
      body=${res:0:${#res}-3}
      if [ "$body" != "$3" ]; then
        __log_test_fail_body
        return 1
      fi
    fi
  fi

  __collect_endpoint_stats "A1PMS" 11 "GET" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

# API test function: GET /rics, V2 GET /v2/rics, and V3 GET /a1-policy-management/v1/rics
# args: <reponse-code> <policy-type-id>|NOTYPE [<space-separate-string-of-ricinfo>]
# example of <space-separate-string-of-ricinfo> = "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2,4 ricsim_g1_1:me2_........."
# format of ric-info:  <ric-id>:<list-of-mes>:<list-of-policy-type-ids>
# (Function for test scripts)
a1pms_api_get_rics() {
  __log_test_start $@

  if [ $# -lt 2 ]; then
    __print_err "<reponse-code> <policy-type-id>|NOTYPE [<space-separate-string-of-ricinfo>]" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/rics"
    if [ $2 != "NOTYPE" ]; then
      query="/v2/rics?policytype_id="$2
    fi
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/rics"
    if [ $2 != "NOTYPE" ]; then
      query=${query}"?policyTypeId="$2
    fi
  else
    query="/rics"
    if [ $2 != "NOTYPE" ]; then
      query="/rics?policyType="$2
    fi
  fi

  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 2 ]; then
    body=${res:0:${#res}-3}
    res=$(python3 ../common/create_rics_json.py "./tmp/.tmp_rics.json" "${A1PMS_VERSION}" "$3")
    if [ $res -ne 0 ]; then
      __log_test_fail_general "Could not create target ric info json"
      return 1
    fi

    targetJson=$(<./tmp/.tmp_rics.json)
    if [ "$A1PMS_VERSION" == "V2" ] || [ "$A1PMS_VERSION" == "V3" ]; then
      targetJson="{\"rics\": $targetJson }"
    fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")
    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 12 "GET" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

##################################################################
#### API Test case functions Service registry and supervision ####
##################################################################

# API test function: PUT /service, V2 PUT /service and V3 PUT a1-policy-management/v1/services
# args: <response-code>  <service-name> <keepalive-timeout> <callbackurl>
# (Function for test scripts)
a1pms_api_put_service() {
  __log_test_start $@
  if [ $# -ne 4 ]; then
    __print_err "<response-code>  <service-name> <keepalive-timeout> <callbackurl>" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/services"
    json="{\"callback_url\": \""$4"\",\"keep_alive_interval_seconds\": \""$3"\",\"service_id\": \""$2"\"}"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/services"
    json="{\"callbackUrl\": \""$4"\",\"keepAliveIntervalSeconds\": \""$3"\",\"serviceId\": \""$2"\"}"
  else
    query="/service"
    json="{\"callbackUrl\": \""$4"\",\"keepAliveIntervalSeconds\": \""$3"\",\"serviceName\": \""$2"\"}"
  fi
  file="./tmp/.tmp.json"
  echo "$json" >$file

  res="$(__do_curl_to_api A1PMS PUT $query $file)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 13 "PUT" $A1PMS_API_PREFIX$query $status
  __log_test_pass
  return 0
}

# API test function: GET /services, V2 GET /v2/services and V3 /a1-policy-management/v1/services
#args: <response-code> [ (<query-service-name> <target-service-name> <keepalive-timeout> <callbackurl>) | (NOSERVICE <target-service-name> <keepalive-timeout> <callbackurl> [<target-service-name> <keepalive-timeout> <callbackurl>]* )]
# (Function for test scripts)
a1pms_api_get_services() {
  __log_test_start $@
  #Number of accepted parameters: 1, 2, 4, 7, 10, 13,...
  paramError=1
  if [ $# -eq 1 ]; then
    paramError=0
  elif [ $# -eq 2 ] && [ $2 != "NOSERVICE" ]; then
    paramError=0
  elif [ $# -eq 5 ]; then
    paramError=0
  elif [ $# -gt 5 ] && [ $2 == "NOSERVICE" ]; then
    argLen=$(($# - 2))
    if [ $(($argLen % 3)) -eq 0 ]; then
      paramError=0
    fi
  fi

  if [ $paramError -ne 0 ]; then
    __print_err "<response-code> [ (<query-service-name> <target-service-name> <keepalive-timeout> <callbackurl>) | (NOSERVICE <target-service-name> <keepalive-timeout> <callbackurl> [<target-service-name> <keepalive-timeout> <callbackurl>]* )]" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/services"
    if [ $# -gt 1 ] && [ $2 != "NOSERVICE" ]; then
      query="/v2/services?service_id="$2
    fi
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/services"
    if [ $# -gt 1 ] && [ $2 != "NOSERVICE" ]; then
      query="/v1/services?serviceId="$2
    fi
  else
    query="/services"

    if [ $# -gt 1 ] && [ $2 != "NOSERVICE" ]; then
      query="/services?name="$2
    fi
  fi
  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -gt 2 ]; then
    variableArgCount=$(($# - 2))
    body=${res:0:${#res}-3}
    targetJson="["
    shift
    shift
    cntr=0
    while [ $cntr -lt $variableArgCount ]; do
      servicename=$1
      shift
      timeout=$1
      shift
      callback=$1
      shift
      if [ $cntr -gt 0 ]; then
        targetJson=$targetJson","
      fi
      # timeSinceLastActivitySeconds value cannot be checked since value varies
      if [ "$A1PMS_VERSION" == "V2" ]; then
        targetJson=$targetJson"{\"service_id\": \""$servicename"\",\"keep_alive_interval_seconds\": "$timeout",\"time_since_last_activity_seconds\":\"????\",\"callback_url\": \""$callback"\"}"
      elif [ "$A1PMS_VERSION" == "V3" ]; then
        targetJson=$targetJson"{\"serviceId\": \""$servicename"\",\"keepAliveIntervalSeconds\": "$timeout",\"timeSinceLastActivitySeconds\":\"????\",\"callbackUrl\": \""$callback"\"}"
      else
        targetJson=$targetJson"{\"serviceName\": \""$servicename"\",\"keepAliveIntervalSeconds\": "$timeout",\"timeSinceLastActivitySeconds\":\"????\",\"callbackUrl\": \""$callback"\"}"
      fi
      let cntr=cntr+3
    done
    targetJson=$targetJson"]"
    if [ "$A1PMS_VERSION" == "V2" ]; then
      targetJson="{\"service_list\": $targetJson }"
      URL_for_Collect_End_Point="/v2/services"
    elif [ "$A1PMS_VERSION" == "V3" ]; then
      targetJson="{\"serviceList\": $targetJson }"
      URL_for_Collect_End_Point="/v1/services"
    fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")
    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  __collect_endpoint_stats "A1PMS" 14 "GET" $A1PMS_API_PREFIX$URL_for_Collect_End_Point $status
  __log_test_pass
  return 0
}

# API test function: GET /services, V2 GET /v2/services and V3 /a1-policy-management/v1/services -  (only checking service names)
# args: <response-code> [<service-name>]*"
# (Function for test scripts)
a1pms_api_get_service_ids() {
  __log_test_start $@

  if [ $# -lt 1 ]; then
    __print_err "<response-code> [<service-name>]*" $@
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/services"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/services"
  else
    query="/services"
  fi
  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  body=${res:0:${#res}-3}
  targetJson="["
  for rapp in ${@:2}; do
    if [ "$targetJson" != "[" ]; then
      targetJson=$targetJson","
    fi
    if [ "$A1PMS_VERSION" == "V2" ]; then
      targetJson=$targetJson"{\"callback_url\":\"????\",\"keep_alive_interval_seconds\":\"????\",\"service_id\":\""$rapp"\",\"time_since_last_activity_seconds\":\"????\"}"
    elif [ "$A1PMS_VERSION" == "V3" ]; then
      targetJson=$targetJson"{\"callbackUrl\":\"????\",\"keepAliveIntervalSeconds\":\"????\",\"serviceId\":\""$rapp"\",\"timeSinceLastActivitySeconds\":\"????\"}"
    else
      targetJson=$targetJson"{\"callbackUrl\":\"????\",\"keepAliveIntervalSeconds\":\"????\",\"serviceName\":\""$rapp"\",\"timeSinceLastActivitySeconds\":\"????\"}"
    fi
  done

  targetJson=$targetJson"]"
  if [ "$A1PMS_VERSION" == "V2" ]; then
    targetJson="{\"service_list\": $targetJson }"
    URL_for_Collect_End_Point="/v2/services"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    targetJson="{\"serviceList\": $targetJson }"
    URL_for_Collect_End_Point="/v1/services"
  fi
  echo "TARGET JSON: $targetJson" >>$HTTPLOG
  res=$(python3 ../common/compare_json.py "$targetJson" "$body")

  if [ $res -ne 0 ]; then
    __log_test_fail_body
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 14 "GET" $A1PMS_API_PREFIX$URL_for_Collect_End_Point $status
  __log_test_pass
  return 0
}

# API test function: DELETE /services, V2 DELETE /v2/services/{serviceId} and V3 DELETE a1-policy-management/v1/services/{serviceId}
# args: <response-code> <service-name>
# (Function for test scripts)
a1pms_api_delete_services() {
  __log_test_start $@

  if [ $# -ne 2 ]; then
    __print_err "<response-code> <service-name>" $@
    return 1
  fi
  if [ "$A1PMS_VERSION" == "V2" ]; then
    url_part="/v2/services/"
    query=${url_part}${2}
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    url_part="/v1/services/"
    query=${url_part}${2}
  else
    query="/services?name="$2
  fi
  res="$(__do_curl_to_api A1PMS DELETE $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 15 "DELETE" ${A1PMS_API_PREFIX}${url_part}"{serviceId}" $status
  __log_test_pass
  return 0
}

# API test function: PUT /services/keepalive, V2 PUT /v2/services/{service_id}/keepalive and V3 DELETE a1-policy-management/v1/services/{serviceId}
# args: <response-code> <service-name>
# (Function for test scripts)
a1pms_api_put_services_keepalive() {
  __log_test_start $@

  if [ $# -ne 2 ]; then
    __print_err "<response-code> <service-name>" $@
    return 1
  fi
  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/services/$2/keepalive"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    query="/v1/services/$2/keepalive"
  else
    query="/services/keepalive?name="$2
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    empty_json_body={}
    res="$(__do_curl_to_api A1PMS PUT ${query} ${empty_json_body})"
  else
    res="$(__do_curl_to_api A1PMS PUT ${query})"
  fi
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code ${1} ${status}
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 16 "PUT" ${A1PMS_API_PREFIX}${query} ${status}
  __log_test_pass
  return 0
}

##################################################################
#### API Test case functions Configuration                    ####
##################################################################

# API Test function: PUT "/v2/configuration" or V3 PUT "a1-policy-management/v1/configuration"
# args: <response-code> <config-file>
# (Function for test scripts)
a1pms_api_put_configuration() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" != "V2" ] && [ "$A1PMS_VERSION" != "V3" ]; then
    __log_test_fail_not_supported
    return 1
  fi

  if [ $# -ne 2 ]; then
    __print_err "<response-code> <config-file>" $@
    return 1
  fi
  if [ ! -f $2 ]; then
    __log_test_fail_general "Config file "$2", does not exist"
    return 1
  fi
  inputJson=$(<$2)
  # if [ $RUNMODE == "DOCKER" ]; then  #In kube the file already has a header
  # 	inputJson="{\"config\":"$inputJson"}"
  # fi
  file="./tmp/.config.json"
  echo $inputJson >$file
  if [ "$A1PMS_VERSION" == "V2" ]; then
    query="/v2/configuration"
  elif [ "$A1PMS_VERSION" == "V3" ]; then
    #V3 has baseurl changes 'a1-policy/v2' to 'a1-policy-management/v1'
    query="/v1/configuration"
  fi
  res="$(__do_curl_to_api A1PMS PUT $query $file)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  __collect_endpoint_stats "A1PMS" 17 "PUT" $A1PMS_API_PREFIX$query $status
  __log_test_pass
  return 0
}

# API Test function: GET /v2/configuration and V3 GET a1-policy-management/v1/configuration
# args: <response-code> [<config-file>]
# (Function for test scripts)
a1pms_api_get_configuration() {
  __log_test_start $@

  if [ "$A1PMS_VERSION" != "V2" ] && [ "$A1PMS_VERSION" != "V3" ]; then
    __log_test_fail_not_supported
    return 1
  fi

  if [ $# -lt 1 ] || [ $# -gt 2 ]; then
    __print_err "<response-code> [<config-file>]" $@
    return 1
  fi
  if [ ! -f $2 ]; then
    __log_test_fail_general "Config file "$2" for comparison, does not exist"
    return 1
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    #The V3 of a1-pms URL is a1-policy-management/v1 and the v2 is a1-policy/v2
    query="/v1/configuration"
  else
    query="/v2/configuration"
  fi
  res="$(__do_curl_to_api A1PMS GET $query)"
  status=${res:${#res}-3}

  if [ $status -ne $1 ]; then
    __log_test_fail_status_code $1 $status
    return 1
  fi

  if [ $# -eq 2 ]; then

    body=${res:0:${#res}-3}

    targetJson=$(<$2)
    # if [ $RUNMODE == "DOCKER" ]; then  #In kube the file already has a header
    # 	inputJson="{\"config\":"$inputJson"}"
    # fi
    echo "TARGET JSON: $targetJson" >>$HTTPLOG
    res=$(python3 ../common/compare_json.py "$targetJson" "$body")

    if [ $res -ne 0 ]; then
      __log_test_fail_body
      return 1
    fi
  fi

  if [ "$A1PMS_VERSION" == "V3" ]; then
    __collect_endpoint_stats "A1PMS" 18 "GET" $A1PMS_API_PREFIX"/v1/configuration" $status
  else
    __collect_endpoint_stats "A1PMS" 18 "GET" $A1PMS_API_PREFIX"/v2/configuration" $status
  fi
  __log_test_pass
  return 0
}

##########################################
####     Reset types and instances    ####
##########################################

# Admin reset to remove all policies and services
# All types and instances etc are removed - types and instances in a1 sims need to be removed separately
# NOTE - only works in kubernetes and the pod should not be running
# args: -
# (Function for test scripts)

a1pms_kube_pvc_reset() {
  __log_test_start $@

  pvc_name=$(kubectl $KUBECONF get pvc -n $KUBE_NONRTRIC_NAMESPACE --no-headers -o custom-columns=":metadata.name" | grep policy)
  if [ -z "$pvc_name" ]; then
    pvc_name=policymanagementservice-vardata-pvc
  fi
  echo " Trying to reset pvc: "$pvc_name
  __kube_clean_pvc $A1PMS_APP_NAME $KUBE_NONRTRIC_NAMESPACE $pvc_name $A1PMS_CONTAINER_MNT_DIR

  __log_test_pass
  return 0
}

# args: <realm> <client-name> <client-secret>
a1pms_configure_sec() {
  export A1PMS_CREDS_GRANT_TYPE="client_credentials"
  export A1PMS_CREDS_CLIENT_SECRET=$3
  export A1PMS_CREDS_CLIENT_ID=$2
  export A1PMS_AUTH_SERVICE_URL=$KEYCLOAK_SERVICE_PATH$KEYCLOAK_TOKEN_URL_PREFIX/$1/protocol/openid-connect/token
  export A1PMS_SIDECAR_MOUNT="/token-cache"
  export A1PMS_SIDECAR_JWT_FILE=$A1PMS_SIDECAR_MOUNT"/jwt.txt"

  export AUTHSIDECAR_APP_NAME
  export AUTHSIDECAR_DISPLAY_NAME
}
