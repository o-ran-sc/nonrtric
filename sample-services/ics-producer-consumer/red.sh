#  ========================LICENSE_START=================================
#  O-RAN-SC
#
#  Copyright (C) 2024: OpenInfra Foundation Europe
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

#!/bin/bash
skip_build=false
no_console=false

# Parse command line arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --skip-build)
            skip_build=true
            shift
            ;;
        --no-console)
            no_console=true
            shift
            ;;
        *)
            echo "Unknown option: $1"
            exit 1
            ;;
    esac
done
# Source the utils script
source utils.sh

# Check Prerequisites
checkJava
checkDocker
checkDockerCompose

if ! $skip_build; then
    # Make build the demo docker image
    cd ./producer/
    make build
    cd ../consumer/
    make build
    cd ..
fi

# Start the Docker containers in detached mode
docker-compose up -d

# Wait for the Kafka container to be running
wait_for_container "kafka-zkless" "Kafka Server started"
space

if ! $no_console; then
    echo "Start RedPanda Console"
    docker-compose -f docker-composeRedPanda.yaml up -d
    space

    echo "Start NONRTRIC control panel"
    docker-compose -f ./docker-compose/docker-compose.yaml -f ./docker-compose/control-panel/docker-compose.yaml -f ./docker-compose/nonrtric-gateway/docker-compose.yaml up -d
    space
fi

# Once Kafka container is running, start the producers and consumers
echo "Kafka container is up and running. Starting producer and consumer..."
space

echo "Start 1 Producer on mytopic"
curl -X GET http://localhost:8080/startProducer/mytopic
space

echo "Start 1 Consumer on mytopic"
curl -X GET http://localhost:8081/startConsumer/mytopic
space

sleep 10

echo "Sending type1 to ICS"
curl -X 'PUT' \
  'http://localhost:8083/data-producer/v1/info-types/type1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "info_job_data_schema": {
    "$schema":"http://json-schema.org/draft-07/schema#",
    "title":"STD_Type1_1.0.0",
    "description":"Type 1",
    "type":"object"
  }
}'

echo "Getting types from ICS"
curl -X 'GET' 'http://localhost:8083/data-producer/v1/info-types/type1'
space

echo "Sending Producer infos to ICS"
curl -X 'PUT' \
  'http://localhost:8083/data-producer/v1/info-producers/1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "info_producer_supervision_callback_url": "http://kafka-producer:8080/producer/supervision",
  "supported_info_types": [
    "type1"
  ],
  "info_job_callback_url": "http://kafka-producer:8080/producer/job"
}'

echo "Getting Producers Infos from ICS"
curl -H 'Content-Type: application/json' 'http://localhost:8083/data-producer/v1/info-producers/1'
space

echo "Sending Consumer Job infos to ICS"
curl -X 'PUT' \
  'http://localhost:8083/data-consumer/v1/info-jobs/1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "info_type_id": "type1",
  "job_owner": "demo",
  "job_definition": {
    "deliveryInfo": {
      "topic": "mytopic",
      "bootStrapServers": "http://kafka-zkless:9092",
      "numberOfMessages": 0
    }
  },
  "job_result_uri": "http://kafka-producer:8080/producer/job",
  "status_notification_uri": "http://kafka-producer:8080/producer/supervision"
}'

echo "Getting Consumer Job Infos from ICS"
curl -H 'Content-Type: application/json' 'http://localhost:8083/data-consumer/v1/info-jobs/1'
space

echo "Sending Consumer Subscription Job infos to ICS"
curl -X 'PUT' \
  'http://localhost:8083/data-consumer/v1/info-type-subscription/1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "status_result_uri": "http://kafka-consumer:8081/info-type-status",
  "owner": "owner"
}'
echo "Getting Consumer Subscription Job infos from ICS"
curl -X 'GET' 'http://localhost:8083/data-consumer/v1/info-type-subscription/1' -H 'accept: application/json'
space

sleep 5
echo "ICS Producer Docker logs "
docker logs informationcoordinatorservice | grep -E 'o.o.i.c.r1producer.ProducerCallbacks|o.o.i.repository.InfoTypeSubscriptions'
space
echo "Demo Producer Docker logs "
docker logs kafka-producer | grep c.d.p.p.SimpleProducer
space
echo "Demo Consumer Docker logs "
docker logs kafka-consumer | grep c.d.c.c.SimpleConsumer
space

if ! $no_console; then
    echo "Red Panda Console: http://localhost:8888"
    echo "Control Panel Console: http://localhost:8181"
fi

echo "Done."

containers=("kafka-producer" "kafka-consumer")

for container in "${containers[@]}"; do
  if docker logs "$container" | grep -q ERROR; then
    echo "Errors found in logs of $container"
    echo "FAIL"
    exit 1
  else
    echo "No errors found in logs of $container"
  fi
done
echo "SUCCESS"
exit 0
