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
source ./utils/utils.sh

docker build -t nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-sample-simple-icsproducer:0.0.1 ../kafka-producer/
docker build -t nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-sample-simple-icsconsumer:0.0.1 ../kafka-consumer/

docker compose up -d

# Wait for the Kafka container to be running
wait_for_container "broker" "Kafka Server started"
wait_for_container "kafka-producer" "Started KafkaProducerApplication"
wait_for_container "kafka-consumer" "Started KafkaConsumerApplication"

# Once Kafka container is running, start the producers and consumers
echo "Kafka container is up and running. Starting producer and consumer..."
space

curl -v -i -X POST -H 'Content-Type: application/json' -d '{"configuredLevel": "TRACE"}' http://localhost:8083/actuator/loggers/org.oransc.ics

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
    "topic": "mytopic",
    "bootStrapServers": "broker:9092"
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
  "info_producer_supervision_callback_url": "http://kafka-producer:8080/health-check",
  "supported_info_types": [
    "type1"
  ],
  "info_job_callback_url": "http://kafka-producer:8080/info-job"
}'

echo "Getting Producers Infos from ICS"
curl -H 'Content-Type: application/json' 'http://localhost:8083/data-producer/v1/info-producers/1'
space

echo "Sending Consumer Subscription Job infos to ICS"
curl -X 'PUT' \
  'http://localhost:8083/data-consumer/v1/info-type-subscription/1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "status_result_uri": "http://kafka-consumer:9090/info-type-status",
  "owner": "demo"
}'
echo "Getting Consumer Subscription Job infos from ICS"
curl -X 'GET' 'http://localhost:8083/data-consumer/v1/info-type-subscription/1' -H 'accept: application/json'
space

#start Consumer
echo "Sending type1 to ICS to use the callback, This will start a CONSUMER"
curl -X 'PUT' \
  'http://localhost:8083/data-producer/v1/info-types/type1' \
  -H 'accept: application/json' \
  -H 'Content-Type: application/json' \
  -d '{
  "info_job_data_schema": {
    "$schema":"http://json-schema.org/draft-07/schema#",
    "title":"STD_Type1_1.0.0",
    "description":"Type 1",
    "topic": "mytopic",
    "bootStrapServers": "broker:9092"
    }
}'

sleep 3

#ICS starts a producer (healthcheck to status)
echo "Sending Consumer Job infos to ICS, This will start a PRODUCER"
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
      "bootStrapServers": "broker:9092",
      "numberOfMessages": 100
    }
  },
  "job_result_uri": "http://kafka-producer:8080/info-job",
  "status_notification_uri": "http://kafka-consumer:9090/info-type-status"
}'

echo "Getting Consumer Job Infos from ICS"
curl -H 'Content-Type: application/json' 'http://localhost:8083/data-consumer/v1/info-jobs/1'
space

for i in {1..10}; do
  echo
  curl -X GET "http://localhost:8080/publish/$i"
  sleep 1
done

space
echo "Deleting Producer Job infos to ICS"
curl -X 'DELETE' \
  'http://localhost:8083/data-producer/v1/info-producers/1'

echo "Deleting Consumer Job infos to ICS"
curl -X 'DELETE' \
  'http://localhost:8083/data-consumer/v1/info-jobs/1'

echo "Deleting type1 to ICS to use the callback and stop consuming"
curl -X 'DELETE' \
  'http://localhost:8083/data-producer/v1/info-types/type1'

echo "ICS Producer Docker logs "
docker logs informationcoordinatorservice | grep -E 'o.o.i.c.r1producer.ProducerCallbacks|o.o.i.repository.InfoTypeSubscriptions'
space
echo "Demo Producer Docker logs "
docker logs kafka-producer | grep c.d.k.controller.KafkaController
space
echo "Demo Consumer Docker logs "
docker logs kafka-consumer | grep c.d.kafkaconsumer.service.KafkaConsumer
space

echo "Done."

containers=("kafka-producer" "kafka-consumer")

for container in "${containers[@]}"; do
  if docker logs "$container" | grep -q ERROR; then
    echo "Errors found in logs of $container"
    docker logs "$container" | grep ERROR
    echo "FAIL"
    exit 1
  else
    echo "No errors found in logs of $container"
  fi
done
echo "SUCCESS"
docker compose down
exit 0
