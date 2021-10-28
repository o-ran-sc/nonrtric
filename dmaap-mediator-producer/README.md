# O-RAN-SC Non-RealTime RIC DMaaP Mediator Producer

This product is a producer of Information Coordinator Service (ICS) jobs for polling topics in DMaaP Message Router (MR) and pushing the messages to a consumer.

The producer takes a number of environment variables, described below, as configuration.

>- INFO_PRODUCER_HOST  **Required**. The host for the producer.                                   Example: `http://mrproducer`
>- LOG_LEVEL           Optional. The log level, which can be `Error`, `Warn`, `Info` or `Debug`.  Defaults to `Info`.
>- INFO_PRODUCER_PORT  Optional. The port for the product.                                        Defaults to `8085`.
>- INFO_COORD_ADDR     Optional. The address of the Information Coordinator.                      Defaults to `https://enrichmentservice:8434`.
>- DMAAP_MR_ADDR       Optional. The address of the DMaaP Message Router.                         Defaults to `https://message-router.onap:3905`.
>- PRODUCER_CERT       Optional. The certificate to use for https.                                Defaults to `configs/producer.crt`
>- PRODUCER_KEY        Optional. The key to the certificate to use for https.                     Defaults to `configs/producer.key`

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

The server part of the producer uses https, and the communication towards ICS and MR use https.

At start up the producer will register the configured job types in ICS and also register itself as a producer supporting these types.

Once the initial registration is done, the producer will constantly poll MR for all configured job types. When receiving messages for a type, it will distribute these messages to all jobs registered for the type. If no jobs for that type are registered, the messages will be discarded. If a consumer is unavailable for distribution, the messages will be discarded for that consumer.

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
