# Using Kafka with a Java Producer and Consumer

Run the demo script
It will check prerequisites, build a consumer and producer, and run them with kafka and ICS


# Install Java Maven and Docker

```shell
sh ./prerun.sh
```

# Install Kafka as a container

```shell
sh ./runzlkafka.sh
```

# Starting the REST application

In a new terminal window:

```shell
mvn spring-boot:run
```

# Starting a producer 

In another window:

```shell
sh ./runproducer.sh mytopic
```

# Starting a Consumer 

In another window:

```shell
sh ./runproducer.sh mytopic
```

# Reading the logs

The logs will be saved in the log/ folder
- app.log
- Consumer.log
- Producer.log 

A sample of the output is as follows:

```
{"bootstrapServers":"localhost:9092","topic":"mytopic","source":"com.demo.kafka.KafkaMessageHandlerImpl","message":"4GPeV7Igy9","key":"84097ac3-f488-4595-86cc-dcb69bce2eda"}
```
