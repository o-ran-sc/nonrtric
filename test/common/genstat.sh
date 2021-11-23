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

# This script collects container statistics to a file. Data is separated with semicolon.
# Works for both docker container and kubernetes pods.
# Relies on 'docker stats' so will not work for other container runtimes.
# Used by the test env.

# args: docker <start-time-seconds> <log-file> <app-short-name> <app-name> [ <app-short-name> <app-name> ]*
# or
# args: kube <start-time-seconds> <log-file> <app-short-name> <app-name> <namespace> [ <app-short-name> <app-name> <namespace> ]*

print_usage() {
  echo "Usage: genstat.sh DOCKER <start-time-seconds> <log-file> <app-short-name> <app-name> [ <app-short-name> <app-name> ]*"
  echo "or"
  echo "Usage: genstat.sh KUBE <start-time-seconds> <log-file> <app-short-name> <app-name> <namespace> [ <app-short-name> <app-name> <namespace> ]*"
}
STARTTIME=-1

if [ $# -lt 4 ]; then
  print_usage
  exit 1
fi
if [ $1 == "DOCKER" ]; then
  STAT_TYPE=$1
  shift
  STARTTIME=$1
  shift
  LOGFILE=$1
  shift
  if [ $(($#%2)) -ne 0 ]; then
    print_usage
    exit 1
  fi
elif [ $1 == "KUBE" ]; then
  STAT_TYPE=$1
  shift
  STARTTIME=$1
  shift
  LOGFILE=$1
  shift
  if [ $(($#%3)) -ne 0 ]; then
    print_usage
    exit 1
  fi
else
  print_usage
  exit 1
fi


echo "Name;Time;PIDS;CPU perc;Mem perc" > $LOGFILE

if [ "$STARTTIME" -ne -1 ]; then
    STARTTIME=$(($SECONDS-$STARTTIME))
fi

while [ true ]; do
  docker stats --no-stream --format "table {{.Name}};{{.PIDs}};{{.CPUPerc}};{{.MemPerc}}" > tmp/.tmp_stat_out.txt
  if [ "$STARTTIME" -eq -1 ]; then
    STARTTIME=$SECONDS
  fi
  CTIME=$(($SECONDS-$STARTTIME))

  TMP_APPS=""

  while read -r line; do
    APP_LIST=(${@})
    if [ $STAT_TYPE == "DOCKER" ]; then
      for ((i=0; i<$#; i=i+2)); do
        SAPP=${APP_LIST[$i]}
        APP=${APP_LIST[$i+1]}
        d=$(echo $line | grep -v "k8s" | grep $APP)
        if [ ! -z $d ]; then
          d=$(echo $d | cut -d';' -f 2- | sed -e 's/%//g' | sed 's/\./,/g')
          echo "$SAPP;$CTIME;$d" >> $LOGFILE
          TMP_APPS=$TMP_APPS" $SAPP "
        fi
      done
    else
      for ((i=0; i<$#; i=i+3)); do
        SAPP=${APP_LIST[$i]}
        APP=${APP_LIST[$i+1]}
        NS=${APP_LIST[$i+2]}
        d=$(echo "$line" | grep -v "k8s_POD" | grep "k8s" | grep $APP | grep $NS)
        if [ ! -z "$d" ]; then
          d=$(echo "$d" | cut -d';' -f 2- | sed -e 's/%//g' | sed 's/\./,/g')
          data="$SAPP-$NS;$CTIME;$d"
          echo $data >> $LOGFILE
          TMP_APPS=$TMP_APPS" $SAPP-$NS "
        fi
      done
    fi
  done < tmp/.tmp_stat_out.txt

  APP_LIST=(${@})
  if [ $STAT_TYPE == "DOCKER" ]; then
    for ((i=0; i<$#; i=i+2)); do
      SAPP=${APP_LIST[$i]}
      APP=${APP_LIST[$i+1]}
      if [[ $TMP_APPS != *" $SAPP "* ]]; then
        data="$SAPP;$CTIME;0;0,00;0,00"
        echo $data >> $LOGFILE
      fi
    done
  else
    for ((i=0; i<$#; i=i+3)); do
      SAPP=${APP_LIST[$i]}
      APP=${APP_LIST[$i+1]}
      NS=${APP_LIST[$i+2]}
      if [[ $TMP_APPS != *" $SAPP-$NS "* ]]; then
        data="$SAPP-$NS;$CTIME;0;0,00;0,00"
        echo $data >> $LOGFILE
      fi
    done
  fi
  sleep 1
done
