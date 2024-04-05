<!--
* ========================LICENSE_START=================================
* O-RAN-SC
*
* Copyright (C) 2024 OpenInfra Foundation Europe. All rights reserved.
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* ========================LICENSE_END===================================
-->
# Automatic
### Using Kafka with a Java Producer and Consumer

Run the demo script
It will check prerequisites, build a consumer and producer, and run them with kafka and ICS.

```shell
./start.sh
```
Or run the other script to bring up RedPanda on port 8888 and NONRTRIC control panel UI on port 8181

```shell
./red.sh
```

For a faster execution you can add:

--skip-build to skip creating the app jar and building the docker images

--no-console to skip running RedPanda and NONRTRIC control panel

```shell
./red.sh --skip-build --no-console
```
# Manual
### Run Kafka in a container

```shell
docker-compose up -d kafka-zkless
```

### Starting the REST application individually

In a new terminal window:

```shell
mvn spring-boot:run
```

### Starting a producer

```shell
sh ./runproducer.sh
```

### Starting a Consumer

```shell
sh ./runproducer.sh
```

## Reading the logs

A sample of the output is as follows:

```
Demo Producer Docker logs

2024-04-02 12:48:05 INFO  c.d.p.p.SimpleProducer:141 - {"bootstrapServers":"kafka-zkless:9092","topic":"mytopic","source":"com.demo.producer.producer.SimpleProducer","message":"ygHwxXSIxW","key":"f8f1a7a7-a78e-4c7d-9b8d-108bb0cc9e2c"}
2024-04-02 12:48:06 INFO  c.d.p.p.SimpleProducer:141 - {"bootstrapServers":"kafka-zkless:9092","topic":"mytopic","source":"com.demo.producer.producer.SimpleProducer","message":"KNIbP10zfN","key":"b058d00f-bbcd-4d2c-936b-6327847d4c2a"}
2024-04-02 12:48:07 INFO  c.d.p.p.SimpleProducer:141 - {"bootstrapServers":"kafka-zkless:9092","topic":"mytopic","source":"com.demo.producer.producer.SimpleProducer","message":"V6fH1NkdeH","key":"ae1a83a3-d8a7-40c8-9d98-529230f8b585"}
2024-04-02 12:48:08 INFO  c.d.p.p.SimpleProducer:141 - {"bootstrapServers":"kafka-zkless:9092","topic":"mytopic","source":"com.demo.producer.producer.SimpleProducer","message":"m76qvRFh6f","key":"abccde52-fa72-4fd4-99ab-5bc21514d825"}
2024-04-02 12:48:09 INFO  c.d.p.p.SimpleProducer:141 - {"bootstrapServers":"kafka-zkless:9092","topic":"mytopic","source":"com.demo.producer.producer.SimpleProducer","message":"t7FJYnFr43","key":"0602239e-34e9-45a6-a04a-3c67b4c7d9e4"}

++++++++++++++++++++++++++++++++++++++++++++++++++++

Demo Consumer Docker logs

2024-04-02 12:48:05 INFO  c.d.c.c.SimpleConsumer:158 - {"message":"Topic: mytopicMessage: ygHwxXSIxW"}
2024-04-02 12:48:06 INFO  c.d.c.c.SimpleConsumer:158 - {"message":"Topic: mytopicMessage: KNIbP10zfN"}
2024-04-02 12:48:07 INFO  c.d.c.c.SimpleConsumer:158 - {"message":"Topic: mytopicMessage: V6fH1NkdeH"}
2024-04-02 12:48:08 INFO  c.d.c.c.SimpleConsumer:158 - {"message":"Topic: mytopicMessage: m76qvRFh6f"}
2024-04-02 12:48:09 INFO  c.d.c.c.SimpleConsumer:158 - {"message":"Topic: mytopicMessage: t7FJYnFr43"}

++++++++++++++++++++++++++++++++++++++++++++++++++++

ICS logs

2024-04-02T12:48:05.615Z DEBUG 1 --- [or-http-epoll-2] o.o.i.c.r1producer.ProducerCallbacks     : Job subscription 1 started OK 1
2024-04-02T12:48:05.820Z DEBUG 1 --- [io-8083-exec-10] o.o.i.repository.InfoTypeSubscriptions   : Added type status subscription 1
```

The script will fail (exit 1) if there are anny ERRORS logged in the kafka-producer and kafka-consumer.