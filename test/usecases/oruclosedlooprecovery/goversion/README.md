# O-RAN-SC Non-RealTime RIC O-RU O-DU Link Failure Consumer

This consumer creates a job of type `STD_Fault_Messages` in the Information Coordinator Service (ICS). When it recieves messages, it checks if they are link failure messages. If they are, it checks if the event severity is other than normal. If so, it looks up the O-DU ID mapped to the O-RU the message originates from and sends a configuration message to the O-DU through SDNC. If the event severity is normal, then it logs, on `Debug` level, that the link failure has been cleared.

## Configuration

The consumer takes a number of environment variables, described below, as configuration.

>- CONSUMER_HOST        **Required**. The host for the consumer.                                   Example: `http://mrproducer`
>- CONSUMER_PORT        **Required**. The port for the consumer.                                   Example: `8095`
>- CONSUMER_CERT_PATH   **Required**. The path to the certificate to use for https.                Defaults to `security/producer.crt`
>- CONSUMER_KEY_PATH    **Required**. The path to the key to the certificate to use for https.     Defaults to `security/producer.key`
>- INFO_COORD_ADDR      Optional. The address of the Information Coordinator.                      Defaults to `http://enrichmentservice:8083`.
>- SDNR_ADDRESS         Optional. The address for SDNR.                                            Defaults to `http://localhost:3904`.
>- SDNR_USER            Optional. The user for the SDNR.                                           Defaults to `admin`.
>- SDNR_PASSWORD        Optional. The password for the SDNR user.                                  Defaults to `Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U`.
>- ORU_TO_ODU_MAP_FILE  Optional. The file containing the mapping from O-RU ID to O-DU ID.         Defaults to `o-ru-to-o-du-map.csv`.
>- LOG_LEVEL            Optional. The log level, which can be `Error`, `Warn`, `Info` or `Debug`.  Defaults to `Info`.

Any of the addresses used by this product can be configured to use https, by specifying it as the scheme of the address URI. The client will not use server certificate verification. The consumer's own callback will only listen to the scheme configured in the scheme of the consumer host address.

The configured public key and cerificate shall be PEM-encoded. A self signed certificate and key are provided in the `security` folder of the project. These files should be replaced for production. To generate a self signed key and certificate, use the example code below:

    openssl req -new -x509 -sha256 -key server.key -out server.crt -days 3650

T## Functionality

he creation of the job is not done when the consumer is started. Instead the consumer provides a REST API where it can be started and stopped, described below.

>- /start  Creates the job in ICS.
>- /stop   Deletes the job in ICS.

If the consumer is shut down with a SIGTERM, it will also delete the job before exiting.

## Development

To make it easy to test during development of the consumer, two stubs are provided in the `stub` folder.

One, under the `producer` folder, called `producer` that stubs the producer and pushes an array with one message with `eventSeverity` alternating between `NORMAL` and `CRITICAL`. To build and start the stub, do the following:
>1. cd stub/producer
>2. go build
>3. ./producer

One, under the `sdnr` folder, called `sdnr` that at startup will listen for REST calls and print the body of them. By default, it listens to the port `3904`, but his can be overridden by passing a `-port [PORT]` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/sdnr
>2. go build
>3. ./sdnr

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
