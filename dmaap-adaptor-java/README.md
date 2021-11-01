# O-RAN-SC Non-RealTime RIC DMaaP Information Producer
This product is a generic information producer (as defined by the Information Coordinator Service (ICS)). It can produce any information that can be retrieved from DMaaP. Its main tasks is to register information types and itself as a producer using the ICS Data Producer API.

A data consumer may create information jobs through the ICS Data Producer API.

This service will retrieve data from the DMaaP Message Router (MR) and distribute it further to the data consumers (information job owners).

The component is a springboot service and is configured as any springboot service through the file `config/application.yaml`. The component log can be retrieved and logging can be controled by means of REST call. See the API documentation (api/api.yaml).

The file `config/application_configuration.json` contains the configuration of job types that the producer will support. Here follows an example with one type:
  
```sh
    {
       "types":
        [
          {
             "id":  "STD_Fault_Messages",
             "dmaapTopicUrl":  events/unauthenticated.SEC_FAULT_OUTPUT/dmaapmediatorproducer/STD_Fault_Messages",
             "useHttpProxy": false
          } 
        ]
    }
```    

Each information has the following properties:
 - id the information type identity as exposed in the Information Coordination Service data consumer API
 - dmaapTopicUrl the URL to for fetching information from  DMaaP
 - useHttpProxy if true, the received information will be delivered using a HTTP proxy (provided that one is setup in the application.yaml file). This might for instance be needed if the data consumer is in the RAN or outside the cluster. 

The service producer will constantly poll MR for all configured job types. When receiving messages for a type, it will distribute these messages to all jobs registered for the type. If no jobs for that type are registered, the messages will be discarded. If a consumer is unavailable for distribution, the messages will be discarded for that consumer.

## License

Copyright (C) 2021 Nordix Foundation. Licensed under the Apache License, Version 2.0 (the "License") you may not use this file except in compliance with the License. You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
For more information about license please see the [LICENSE](LICENSE.txt) file for details.