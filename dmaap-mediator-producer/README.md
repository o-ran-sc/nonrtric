# O-RAN-SC Non-RealTime RIC DMaaP Mediator Producer

This product is a producer of Information Coordinator Service (ICS) jobs for polling topics in DMaaP Message Router (MR) and pushing the messages to a consumer.

## Configuration

The producer takes a number of environment variables, described below, as configuration.

>- INFO_PRODUCER_HOST  **Required**. The host for the producer.                                   Example: `https://mrproducer`
>- INFO_PRODUCER_PORT  Optional. The port for the product.                                        Defaults to `8085`.
>- INFO_COORD_ADDR     Optional. The address of the Information Coordinator.                      Defaults to `https://enrichmentservice:8434`.
>- DMAAP_MR_ADDR       Optional. The address of the DMaaP Message Router.                         Defaults to `https://message-router.onap:3905`.
>- PRODUCER_CERT_PATH  Optional. The path to the certificate to use for https.                    Defaults to `security/producer.crt`
>- PRODUCER_KEY_PATH   Optional. The path to the key to the certificate to use for https.         Defaults to `security/producer.key`
>- LOG_LEVEL           Optional. The log level, which can be `Error`, `Warn`, `Info` or `Debug`.  Defaults to `Info`.

The file `configs/type_config.json` contains the configuration of job types that the producer will support.

    {
       "types":
        [
          {
            "id": The ID of the job type, e.g. "STD_Fault_Messages",
            "dmaapTopicUrl": The topic URL to poll from DMaaP Message Router, e.g. "events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/STD_Fault_Messages"
          }
      ]
    }

Any of the addresses used by this product can be configured to use https, by specifying it as the scheme of the address URI. Clients configured to use https will not use server certificate verification. The communication towards the consumers will use https if their callback address URI uses that scheme. The producer's own callback will only listen to the scheme configured in the scheme of the info producer host address.

The configured public key and cerificate shall be PEM-encoded. A self signed certificate and key are provided in the `security` folder of the project. These files should be replaced for production. To generate a self signed key and certificate, use the example code below:

    openssl req -new -x509 -sha256 -key server.key -out server.crt -days 3650

## Functionality

At start up the producer will register the configured job types in ICS and also register itself as a producer supporting these types. If ICS is unavailable, the producer will retry to connect indefinetely. The same goes for MR.

Once the initial registration is done, the producer will constantly poll MR for all configured job types. When receiving messages for a type, it will distribute these messages to all jobs registered for the type. If no jobs for that type are registered, the messages will be discarded. If a consumer is unavailable for distribution, the messages will be discarded for that consumer.

## Development

To make it easy to test during development of the producer, two stubs are provided in the `stub` folder.

One, under the `dmaap` folder, called `dmaap` that stubs MR and respond with an array with one message with `eventSeverity` alternating between `NORMAL` and `CRITICAL`. The default port is `3905`, but this can be overridden by passing a `-port [PORT]` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/dmaap
>2. go build
>3. ./dmaap

One, under the `consumer` folder, called `consumer` that at startup will register a job of type `STD_Fault_Messages` in ICS, and then listen for REST calls and print the body of them. By default, it listens to the port `40935`, but his can be overridden by passing a `-port [PORT]` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/consumer
>2. go build
>3. ./consumer

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
