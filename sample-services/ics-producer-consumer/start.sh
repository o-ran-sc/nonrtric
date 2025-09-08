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
source utils.sh

PREFIX="nexus3.o-ran-sc.org:10004"
VERSION="0.0.1"

# Create a network for Kafka Containers
docker network create kafka-net

# Start Kafka
docker run -d \
  --network kafka-net \
  --name kafka-zkless \
  -p 9092:9092 \
  -e LOG_DIR="/tmp/logs" \
  -e KAFKA_LISTENER_SECURITY_PROTOCOL_MAP="CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT" \
  -e KAFKA_LISTENERS="PLAINTEXT://:29092,PLAINTEXT_HOST://:9092,CONTROLLER://:9093" \
  -e KAFKA_ADVERTISED_LISTENERS="PLAINTEXT://kafka-zkless:29092,PLAINTEXT_HOST://kafka-zkless:9092" \
  quay.io/strimzi/kafka:latest-kafka-2.8.1-amd64 \
  /bin/sh -c 'export CLUSTER_ID=$(bin/kafka-storage.sh random-uuid) && \
  bin/kafka-storage.sh format -t $CLUSTER_ID -c config/kraft/server.properties && \
  bin/kafka-server-start.sh config/kraft/server.properties --override advertised.listeners=$KAFKA_ADVERTISED_LISTENERS --override listener.security.protocol.map=$KAFKA_LISTENER_SECURITY_PROTOCOL_MAP --override listeners=$KAFKA_LISTENERS'

# Start ICS
docker run -d \
  --network kafka-net \
  --name informationcoordinatorservice \
  -p 8083:8083 \
  -v ./application.yaml:/opt/app/information-coordinator-service/config/application.yaml \
  nexus3.o-ran-sc.org:10001/o-ran-sc/nonrtric-plt-informationcoordinatorservice:1.6.1

# Start Producer
docker run -d \
  --network kafka-net \
  --name kafka-producer \
  -p 8080:8080 \
  -e KAFKA_SERVERS=kafka-zkless:9092 \
  $PREFIX/o-ran-sc/nonrtric-sample-icsproducer:$VERSION

#Start Consumer
docker run -d \
  --network kafka-net \
  --name kafka-consumer \
  -p 8081:8081 \
  -e KAFKA_SERVERS=kafka-zkless:9092 \
  $PREFIX/o-ran-sc/nonrtric-sample-icsconsumer:$VERSION

# Wait for the Kafka container to be running
wait_for_container "kafka-zkless" "Kafka Server started"
wait_for_container "kafka-producer" "Started Application"
wait_for_container "kafka-consumer" "Started Application"

# Once Kafka container is running, start the producers and consumers
echo "Kafka container is up and running. Starting producer and consumer..."
space

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
    "bootStrapServers": "kafka-zkless:9092"
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
      "bootStrapServers": "kafka-zkless:9092",
      "numberOfMessages": 100
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
  "status_result_uri": "http://kafka-consumer:8081/consumer/info-type-status",
  "owner": "demo"
}'
echo "Getting Consumer Subscription Job infos from ICS"
curl -X 'GET' 'http://localhost:8083/data-consumer/v1/info-type-subscription/1' -H 'accept: application/json'
space

#To Set Kafka Broker in Consumer
echo "Sending type1 to ICS to use the callback"
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
    "bootStrapServers": "kafka-zkless:9092"
    }
}'

#Using the autostart flag in the application.yaml
echo "Start 1 Producer on mytopic"
curl -X GET http://localhost:8080/startProducer/mytopic
space

echo "Start 1 Consumer on mytopic"
curl -X GET http://localhost:8081/startConsumer/mytopic
space

sleep 10

echo "ICS Producer Docker logs "
docker logs informationcoordinatorservice | grep -E 'o.o.i.c.r1producer.ProducerCallbacks|o.o.i.repository.InfoTypeSubscriptions'
space
echo "Demo Producer Docker logs "
docker logs kafka-producer | grep c.d.p.p.SimpleProducer
space
echo "Demo Consumer Docker logs "
docker logs kafka-consumer | grep c.d.c.c.SimpleConsumer
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
docker stop kafka-zkless
docker stop informationcoordinatorservice
docker stop kafka-producer
docker stop kafka-consumer
docker rm kafka-zkless
docker rm informationcoordinatorservice
docker rm kafka-producer
docker rm kafka-consumer
exit 0
