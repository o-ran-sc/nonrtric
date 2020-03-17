#!/usr/bin/env bash

. ../common/test_env.sh

echo "Test case started as: ${BASH_SOURCE[$i+1]} "$1 $2
echo "Numbers of ric simulator started" $2

# This is a script that contains all the functions needed for auto test
# Arg: local | remote (1, 2, 3, 4....)

STARTED_POLICY_AGENT="" #Policy agent app names added to this var to keep track of started container in the script
START_ARG=$1
IMAGE_TAG="1.0.0-SNAPSHOT"
IMAGE_TAG_REMOTE="latest"
RIC_NUMBER=$2

if [ $# -lt 1 ] || [ $# -gt 2 ]; then
	echo "Expected arg: local  | remote  and numbers of the rics "
	exit 1
elif [ $1 == "local" ]; then
	if [ -z $POLICY_AGENT_LOCAL_IMAGE ]; then
		echo "POLICY_AGENT_LOCAL_IMAGE not set in test_env"
		exit 1
	fi
	POLICY_AGENT_IMAGE=$POLICY_AGENT_LOCAL_IMAGE":"$IMAGE_TAG
elif [ $1 == "remote" ]; then
    if [ -z $POLICY_AGENT_REMOTE_IMAGE ]; then
        echo "POLICY_AGENT_REMOTE_IMAGE not set in test_env"
        exit 1
    fi
    POLICY_AGENT_IMAGE=$POLICY_AGENT_REMOTE_IMAGE":"$IMAGE_TAG_REMOTE
fi

# Set a description string for the test case
if [ -z "$TC_ONELINE_DESCR" ]; then
	TC_ONELINE_DESCR="<no-description>"
	echo "No test case description found, TC_ONELINE_DESCR should be set on in the test script , using "$TC_ONELINE_DESCR
fi

ATC=$(basename "${BASH_SOURCE[$i+1]}" .sh)


# Create the logs dir if not already created in the current dir
if [ ! -d "logs" ]; then
    mkdir logs
fi

TESTLOGS=$PWD/logs

mkdir -p $TESTLOGS/$ATC

TCLOG=$TESTLOGS/$ATC/TC.log
exec &>  >(tee ${TCLOG})

#Variables for counting tests as well as passed and failed tests
RES_TEST=0
RES_PASS=0
RES_FAIL=0
TCTEST_START=$SECONDS

echo "-------------------------------------------------------------------------------------------------"
echo "-----------------------------------      Test case: "$ATC
echo "-----------------------------------      Started:   "$(date)
echo "-------------------------------------------------------------------------------------------------"
echo "-- Description: "$TC_ONELINE_DESCR
echo "-------------------------------------------------------------------------------------------------"
echo "-----------------------------------      Test case setup      -----------------------------------"


if [ -z "$SIM_GROUP" ]; then
		SIM_GROUP=$PWD/../simulator-group
		if [ ! -d  $SIM_GROUP ]; then
			echo "Trying to set env var SIM_GROUP to dir 'simulator-group' in the integration repo, but failed."
			echo "Please set the SIM_GROUP manually in the test_env.sh"
			exit 1
		else
			echo "SIM_GROUP auto set to: " $SIM_GROUP
		fi
elif [ $SIM_GROUP = *simulator_group ]; then
			echo "Env var SIM_GROUP does not seem to point to dir 'simulator-group' in the integration repo, check test_env.sh"
			exit 1
fi

echo ""

if [ $1 !=  "manual-container" ] && [ $1 !=  "manual-app" ]; then
	#echo -e "Policy agent image tag set to: \033[1m" $IMAGE_TAG"\033[0m"
	echo "Configured image for policy agent app(s) (${1}): "$POLICY_AGENT_IMAGE
	tmp_im=$(docker images ${POLICY_AGENT_IMAGE} | grep -v REPOSITORY)

	if [ $1 == "local" ]; then
		if [ -z "$tmp_im" ]; then
			echo "Local image (non nexus) "$POLICY_AGENT_IMAGE" does not exist in local registry, need to be built"
			exit 1
		else
			echo -e "Policy agent local image: \033[1m"$tmp_im"\033[0m"
			echo "If the policy agent image seem outdated, rebuild the image and run the test again."
		fi
	elif [ $1 == "remote" ]; then
	    if [ -z "$tmp_im" ]; then
			echo "Pulling policy agent image from nexus: "$POLICY_AGENT_IMAGE
			docker pull $POLICY_AGENT_IMAGE	 > /dev/null
			tmp_im=$(docker images ${POLICY_AGENT_IMAGE} | grep -v REPOSITORY)
			if [ -z "$tmp_im" ]; then
				echo "Image could not be pulled"
				exit 1
			fi
			echo -e "Policy Agent image: \033[1m"$tmp_im"\033[0m"
		else
			echo -e "Policy Agent image: \033[1m"$tmp_im"\033[0m"
			echo "!! If the Policy agent image seem outdated, consider removing it from your docker registry and run the test again."
		fi
	fi
fi

echo ""

echo "Building images for the simulators"
if [ -z "$SIM_DIR" ]; then
		SIM_DIR=$(find . -type d -path "*a1-interface/near-rt-ric-simulator/scripts*" 2>/dev/null -print -quit)
		if [ ! -d  $SIM_DIR ]; then
			echo "Trying to set env var SIM_DIR to dir 'a1-interface/near-rt-ric-simulator/scripts' in the sim repo, but failed."
			echo "It might be that you did not download the repository of the near-rt-ric simulator.In that case, run the command:"
			echo "git clone 'https://gerrit.o-ran-sc.org/oransc/sim/a1-interface'"
			echo "Otherwise, please set the SIM_DIR manually in the test_env.sh"
			exit 1
		else
			echo "SIM_DIR auto set to: " $SIM_DIR
		fi
fi
cd $SIM_DIR
docker build -t ric-simulator:latest . &> /dev/null
cd $curdir

echo ""

echo "Local registry images for simulators:"
echo "Consul:             " $(docker images | grep consul)
echo "CBS:                " $(docker images | grep platform.configbinding.app)
echo "RIC:                " $(docker images | grep ric-simulator)
echo ""


__consul_config() {

	appname=$PA_APP_BASE

	echo "Configuring consul for " $appname " from " $1
	curl -s http://127.0.0.1:${CONSUL_PORT}/v1/kv/${appname}?dc=dc1 -X PUT -H 'Accept: application/json' -H 'Content-Type: application/json' -H 'X-Requested-With: XMLHttpRequest' --data-binary "@"$1 >/dev/null
}


consul_config_app() {

    __consul_config $1

}



start_ric_simulator() {

  DOCKER_SIM_NWNAME="nonrtric-docker-net"
  echo "Creating docker network $DOCKER_SIM_NWNAME, if needed"
  docker network ls| grep $DOCKER_SIM_NWNAME > /dev/null || docker network create $DOCKER_SIM_NWNAME

  echo "start ric simulator"
  curdir=$PWD
  cd $SIM_GROUP
  cd ric/

  docker-compose up --scale ric-simulator=$RIC_NUMBER -d

  cd $curdir
	echo ""
}

prepare_consul_config() {
  echo "prepare consul config"
  curdir=$PWD
  cd $SIM_GROUP
  cd ric/

  python3 cleanConsul.py
  python3 prepareConsul.py

  cd $curdir
	echo ""


}


# Start all simulators in the simulator group
start_simulators() {

	echo "Starting all simulators"
	curdir=$PWD
	cd $SIM_GROUP

	$SIM_GROUP/simulators-start.sh
	cd $curdir
	echo ""
}

clean_containers() {
	echo "Stopping all containers, policy agent app(s) and simulators with name prefix 'policy_agent'"
	docker stop $(docker ps -q --filter name=/policy-agent) &> /dev/null
	echo "Removing all containers, policy agent app and simulators with name prefix 'policy_agent'"
	docker rm $(docker ps -a -q --filter name=/policy-agent) &> /dev/null
	echo "Stopping all containers, policy agent app(s) and simulators with name prefix 'ric-simulator'"
	docker stop $(docker ps -q --filter name=ric-simulator) &> /dev/null
	echo "Removing all containers, policy agent app and simulators with name prefix 'ric-simulator'"
	docker rm $(docker ps -a -q --filter name=ric-simulator) &> /dev/null
	echo "Removing unused docker networks with substring 'policy agent' in network name"
	docker network rm $(docker network ls -q --filter name=nonrtric)
	echo ""
}

start_policy_agent() {

	appname=$PA_APP_BASE

	if [ $START_ARG == "local" ] || [ $START_ARG == "remote" ]; then
		__start_policy_agent_image $appname
	fi
}

__start_policy_agent_image() {

	appname=$1
	localport=$POLICY_AGENT_PORT

	echo "Creating docker network $DOCKER_SIM_NWNAME, if needed"

	docker network ls| grep $DOCKER_SIM_NWNAME > /dev/null || docker network create $DOCKER_SIM_NWNAME

	echo "Starting policy agent: " $appname " with ports mapped to " $localport " in docker network "$DOCKER_SIM_NWNAME
	docker run -d -p $localport":8081" --network=$DOCKER_SIM_NWNAME -e CONSUL_HOST=$CONSUL_HOST -e CONSUL_PORT=$CONSUL_PORT -e CONFIG_BINDING_SERVICE=$CONFIG_BINDING_SERVICE -e HOSTNAME=$appname --name $appname $POLICY_AGENT_IMAGE
	#docker run -d -p 8081:8081 --network=nonrtric-docker-net -e CONSUL_HOST=CONSUL_HOST=$CONSUL_HOST -e CONSUL_PORT=$CONSUL_PORT -e CONFIG_BINDING_SERVICE=$CONFIG_BINDING_SERVICE -e HOSTNAME=policy-agent
	sleep 3
	set +x
	pa_started=false
	for i in {1..10}; do
		if [ $(docker inspect --format '{{ .State.Running }}' $appname) ]
		 	then
			 	echo " Image: $(docker inspect --format '{{ .Config.Image }}' ${appname})"
		   		echo "Policy Agent container ${appname} running"
				pa_started=true
		   		break
		 	else
		   		sleep $i
	 	fi
	done
	if ! [ $pa_started  ]; then
		echo "Policy Agent container ${appname} could not be started"
		exit 1
	fi

	pa_st=false
	echo "Waiting for Policy Agent ${appname} service status..."
	for i in {1..10}; do
		result="$(__do_curl http://127.0.0.1:${localport}/status)"
		if [ $? -eq 0 ]; then
	   		echo "Policy Agent ${appname} responds to service status: " $result
	   		echo "Policy Agent is alive."
	   		pa_st=true
	   		break
	 	else
	   		sleep $i
	 	fi
	done

	if [ "$pa_st" = "false"  ]; then
		echo "Policy Agent ${appname} did not respond to service status"
		exit 1
	fi
}

check_policy_agent_logs() {

		appname=$PA_APP_BASE
		tmp=$(docker ps | grep $appname)
		if ! [ -z "$tmp" ]; then  #Only check logs for running policy agent apps
			__check_policy_agent_log $appname
		fi

}

__check_policy_agent_log() {
	echo "Checking $1 log $POLICY_AGENT_LOGPATH for WARNINGs and ERRORs"
	foundentries=$(docker exec -it $1 grep WARN /var/log/policy-agent/application.log | wc -l)
	if [ $? -ne  0 ];then
		echo "  Problem to search $1 log $POLICY_AGENT_LOGPATH"
	else
		if [ $foundentries -eq 0 ]; then
			echo "  No WARN entries found in $1 log $POLICY_AGENT_LOGPATH"
		else
			echo -e "  Found \033[1m"$foundentries"\033[0m WARN entries in $1 log $POLICY_AGENT_LOGPATH"
		fi
	fi
	foundentries=$(docker exec -it $1 grep ERR $POLICY_AGENT_LOGPATH | wc -l)
	if [ $? -ne  0 ];then
		echo "  Problem to search $1 log $POLICY_AGENT_LOGPATH"
	else
		if [ $foundentries -eq 0 ]; then
			echo "  No ERR entries found in $1 log $POLICY_AGENT_LOGPATH"
		else
			echo -e "  Found \033[1m"$foundentries"\033[0m ERR entries in $1 log $POLICY_AGENT_LOGPATH"
		fi
	fi
}

store_logs() {
	if [ $# != 1 ]; then
    	__print_err "need one arg, <file-prefix>"
		exit 1
	fi
	echo "Storing all container logs and policy agent app log using prefix: "$1

	docker logs polman_consul > $TESTLOGS/$ATC/$1_consul.log 2>&1
	docker logs polman_cbs > $TESTLOGS/$ATC/$1_cbs.log 2>&1
}

__do_curl() {
	res=$(curl -skw "%{http_code}" $1)
	http_code="${res:${#res}-3}"
	if [ ${#res} -eq 3 ]; then
  		echo "<no-response-from-server>"
		return 1
	else
		if [ $http_code -lt 200 ] && [ $http_code -gt 299]; then
			echo "<not found, resp:${http_code}>"
			return 1
		fi
		if [ $# -eq 2 ]; then
  			echo "${res:0:${#res}-3}" | xargs
		else
  			echo "${res:0:${#res}-3}"
		fi

		return 0
	fi
}

