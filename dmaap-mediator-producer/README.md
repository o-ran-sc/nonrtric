# O-RAN-SC Non-RealTime RIC DMaaP Mediator Producer

This product is a producer of Information Coordinator Service (ICS) jobs for polling topics in DMaaP Message Router (MR) and pushing the messages to a consumer.

## Configuration

The producer takes a number of environment variables, described below, as configuration.

>- INFO_PRODUCER_HOST  **Required**. The host for the producer.                                   Example: `https://mrproducer`
>- INFO_PRODUCER_PORT  Optional. The port for the product.                                        Defaults to `8085`.
>- INFO_COORD_ADDR     Optional. The address of the Information Coordinator.                      Defaults to `https://informationservice:8434`.
>- DMAAP_MR_ADDR       Optional. The address of the DMaaP Message Router.                         Defaults to `https://message-router.onap:3905`.
>- PRODUCER_CERT_PATH  Optional. The path to the certificate to use for https.                    Defaults to `security/producer.crt`
>- PRODUCER_KEY_PATH   Optional. The path to the key to the certificate to use for https.         Defaults to `security/producer.key`
>- LOG_LEVEL           Optional. The log level, which can be `Error`, `Warn`, `Info` or `Debug`.  Defaults to `Info`.

Any of the addresses used by this product can be configured to use https, by specifying it as the scheme of the address URI. Clients configured to use https will not use server certificate verification. The communication towards the consumers will use https if their callback address URI uses that scheme. The producer's own callback will only listen to the scheme configured in the scheme of the info producer host address.

The configured public key and cerificate shall be PEM-encoded. A self signed certificate and key are provided in the `security` folder of the project. These files should be replaced for production. To generate a self signed key and certificate, use the example code below:

    openssl req -new -x509 -sha256 -key server.key -out server.crt -days 3650

The file `configs/type_config.json` contains the configuration of job types that the producer will support, see example below.

    {
       "types":
        [
          {
            "id": The ID of the job type, e.g. "STD_Fault_Messages",
            "dmaapTopicUrl": The topic URL to poll from DMaaP Message Router, e.g. "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/STD_Fault_Messages"
          },
          {
            "id": The ID of the job type, e.g. "Kafka_TestTopic",
            "kafkaInputTopic": The Kafka topic to poll
          }
      ]
    }

Each information type has the following properties:
 - id the information type identity as exposed in the Information Coordination Service data consumer API
 - dmaapTopicUrl the URL to for fetching information from  DMaaP
 - kafkaInputTopic the Kafka topic to get input from

Either the "dmaapTopicUrl" or the "kafkaInputTopic" must be provided for each type, not both.

## Functionality

At start up the producer will register the configured job types in ICS and also register itself as a producer supporting these types. If ICS is unavailable, the producer will retry to connect indefinetely. The same goes for MR.

Once the initial registration is done, the producer will constantly poll MR and/or Kafka for all configured job types. When receiving messages for a type, it will distribute these messages to all jobs registered for the type. If no jobs for that type are registered, the messages will be discarded. If a consumer is unavailable for distribution, the messages will be discarded for that consumer until it is available again.

The producer provides a REST API that fulfills the ICS Data producer API, see [Data producer (callbacks)](<https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric/en/latest/ics-api.html#tag/Data-producer-(callbacks)>). The health check method returns the registration status of the producer in ICS as JSON. It also provides a method to control the log level of the producer. The available log levels are the same as the ones used in the configuration above.

    PUT https://mrproducer:8085/admin/log?level=<new level>

The Swagger documentation of the producer's API is also available, through the `/swagger` path.

When an Information Job is created in the Information Coordinator Service Consumer API, it is possible to define a number of job specific properties. For an Information type that has a Kafka topic defined, the following Json schema defines the properties that can be used:


```sh
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
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
-bufferTimeout, can be used to reduce the number of REST calls to the consumer. If defined, a number of objects will be
 buffered and sent in one REST call to the consumer.
 The buffered objects will be put in a Json array and quoted. Example;
   Object1 and Object2 may be posted in one call -->  ["Object1", "Object2"]
 The bufferTimeout is a Json object and the parameters in the object are:
   - maxSize the maximum number of buffered objects before posting
   - maxTimeMiliseconds the maximum delay time to buffer before posting
 If no bufferTimeout is specified, each object will be posted as received in separate calls (not quoted and put in a Json array).


For an information type that only has a DMaaP topic, the following Json schema is used:

```sh
{
  "$schema": "http://json-schema.org/draft-04/schema#",
  "type": "object",
  "properties": {
  },
  "additionalProperties": false
}

## Development

To make it easy to test during development of the producer, three stubs are provided in the `stub` folder.

One, under the `dmaap` folder, called `dmaap` that stubs MR and respond with an array with one message with `eventSeverity` alternating between `NORMAL` and `CRITICAL`. The default port is `3905`, but this can be overridden by passing a `-port <PORT>` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/dmaap
>2. go build
>3. ./dmaap [-port \<PORT>]

An ICS stub, under the `ics` folder, that listens for registration calls from the producer. When it gets a call it prints out the data of the call. By default, it listens to the port `8434`, but his can be overridden by passing a `-port [PORT]` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/ics
>2. go build [-port \<PORT>]
>3. ./ics

One, under the `consumer` folder, called `consumer` that at startup will register a job of type `STD_Fault_Messages` in ICS, if it is available, and then listen for REST calls and print the body of them. By default, it listens to the port `40935`, but his can be overridden by passing a `-port <PORT>` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/consumer
>2. go build
>3. ./consumer [-port \<PORT>]

Mocks needed for unit tests have been generated using `github.com/stretchr/testify/mock` and are checked in under the `mocks` folder. **Note!** Keep in mind that if any of the mocked interfaces change, a new mock for that interface must be generated and checked in.

## License

Copyright (C) 2021 Nordix Foundation.
Licensed under the Apache License, Version 2.0 (the "License")
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

For more information about license please see the [LICENSE](LICENSE.txt) file for details.
