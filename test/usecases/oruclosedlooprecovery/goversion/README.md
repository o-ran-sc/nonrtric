# O-RAN-SC Non-RealTime RIC O-RU O-DU Link Failure Consumer

This consumer creates a job of type `STD_Fault_Messages` in the Information Coordinator Service (ICS). When it recieves messages, it checks if they are link failure messages. If they are, it checks if the event severity is other than normal. If so, it looks up the O-DU ID mapped to the O-RU the message originates from and sends a configuration message to the O-DU through SDNC. If the event severity is normal, then it logs, on `Debug` level, that the link failure has been cleared.

The producer takes a number of environment variables, described below, as configuration.

>- CONSUMER_HOST        **Required**. The host for the consumer.                                   Example: `http://mrproducer`
>- CONSUMER_HOST        **Required**. The port for the consumer.                                   Example: `8095`
>- LOG_LEVEL            Optional. The log level, which can be `Error`, `Warn`, `Info` or `Debug`.  Defaults to `Info`.
>- INFO_COORD_ADDR      Optional. The address of the Information Coordinator.                      Defaults to `http://enrichmentservice:8083`.
>- SDNR_HOST            Optional. The host for SDNR.                                               Defaults to `http://localhost`.
>- SDNR_PORT            Optional. The port for SDNR.                                               Defaults to `3904`.
>- SDNR_USER            Optional. The user for the SDNR.                                           Defaults to `admin`.
>- SDNR_PASSWORD        Optional. The password for the SDNR user.                                  Defaults to `Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U`.
>- ORU_TO_ODU_MAP_FILE  Optional. The file containing the mapping from O-RU ID to O-DU ID.         Defaults to `o-ru-to-o-du-map.csv`.

The creation of the job is not done when the consumer is started. Instead the consumer provides a REST API where it can be started and stopped, described below.

>- /start  Creates the job in ICS.
>- /stop   Deletes the job in ICS.

If the consumer is shut down with a SIGTERM, it will also delete the job before exiting.

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
