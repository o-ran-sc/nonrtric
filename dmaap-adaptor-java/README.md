# O-RAN-SC Non-RealTime RIC DMaaP Information Producer
This product is a generic information producer (as defined by the Information Coordinator Service (ICS)). It can produce any information that can be retrieved from DMaaP or Kafka. Its main tasks is to register information types and itself as a producer using the ICS Data Producer API.

A data consumer may create information jobs through the ICS Data Producer API.

This service will retrieve data from the DMaaP Message Router (MR) or from the Kafka streaming platform and will distribute it further to the data consumers (information job owners).

The component is a springboot service and is configured as any springboot service through the file `config/application.yaml`. The component log can be retrieved and logging can be controled by means of REST call. See the API documentation (api/api.yaml).

The file `config/application_configuration.json` contains the configuration of job types that the producer will support. Here follows an example with one type:

```sh
    {
       "types":
        [
          {
             "id":  "ExampleInformationType1_1.0.0",
             "dmaapTopicUrl":  "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/STD-Fault-Messages_1.0.0",
             "useHttpProxy": true
          },
          {
             "id": "ExampleInformationType2_2.0.0",
             "kafkaInputTopic": "KafkaInputTopic",
             "useHttpProxy": false
          }
        ]
    }
```

Each information type has the following properties:
 - id the information type identity as exposed in the Information Coordination Service data consumer API
 - dmaapTopicUrl the URL to for fetching information from  DMaaP
 - kafkaInputTopic a Kafka topic to get input from
 - useHttpProxy if true, the received information will be delivered using a HTTP proxy (provided that one is setup in the application.yaml file). This might for instance be needed if the data consumer is in the RAN or outside the cluster.

The service producer will poll MR and/or listen to Kafka topics for all configured job types. When receiving messages for a type, it will distribute these messages to all jobs registered for the type. If a consumer is unavailable for distribution, the messages will be discarded for that consumer.

When a Information Job is created in the Information Coordinator Service Consumer API, it is possible to define a numer of job specific properties. For a Information type that has a Kafka topic defined, the following Json schema defines the properties that can be used:


```sh
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "filter": {
      "type": "string"
    },
    "maxConcurrency": {
      "type": "integer"
    },
    "bufferTimeout": {
      "type": "object",
      "properties": {
        "maxSize": {
          "type": "integer"
        },
        "maxTimeMiliseconds": {
          "type": "integer"
        }
      },
      "additionalProperties": false,
      "required": [
        "maxSize",
        "maxTimeMiliseconds"
      ]
    }
  },
  "additionalProperties": false
}
```
-filter is a regular expression. Only strings that matches the expression will be pushed furter to the consumer.
-maxConcurrency the maximum number of concurrent REST session for the data delivery. 
 The default is 1 and that is the number that must be used to guarantee that the object sequence is maintained. 
 A higher number will give higher throughtput. 
-bufferTimeout, can be used to reduce the numer of REST calls to the consumer. If defined, a number of objects will be 
 buffered and sent in one REST call to the consumer.
 The objects will be put in a Json array and quoted. Example; 
   Object1 and Object2 may be posted in one call -->  ["Object1", "Object2"]
 The parameters in the object are:
   - maxSize the maximum number of buffered objects before posting
   - maxTimeMiliseconds the maximum delay time to buffer before posting
 If no bufferTimeout is specified, each object will be posted as received in a separate calls.


For a information type that only has a DMAAP topic, the following Json schema define the possible parameters to use when careting an information job:

```sh
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
    "filter": {
       "type": "string"
     }
  },
  "additionalProperties": false
}
```
-filter is a regular expression. Only strings that matches the expression will be pushed furter to the consumer. This
 has a similar meaning as in jobs that receives data from Kafka.

## License

Copyright (C) 2021 Nordix Foundation. Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
For more information about license please see the [LICENSE](LICENSE.txt) file for details.