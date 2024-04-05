#!/bin/bash

# Source the utils script
source utils.sh

# Check Prerequisites
checkJava
checkMaven
checkDocker
checkDockerCompose

# Make build the demo docker image
#cd ./producer/
#make jar
#make build
#cd ../consumer/
#make jar
#make build
#cd ..

# Start the Docker containers in detached mode
docker-compose up -d

# Wait for the Kafka container to be running
wait_for_container "kafka-zkless" "Kafka Server started"

# Once Kafka container is running, start the producers and consumers
echo "Kafka container is up and running. Starting producer and consumer..."
space

echo "Start 1 Producer on mytopic"
#echo "sh runproducer.sh"
curl -X GET http://localhost:8080/startProducer/mytopic
space

echo "Start 1 Consumer on mytopic"
#echo "sh runconsumer.sh"
curl -X GET http://localhost:8081/startConsumer/mytopic
space

sleep 10
# curl -X GET http://localhost:8081/stopConsumer
# sleep 1
# curl -X GET http://localhost:8080/stopProducer

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

echo "Done."