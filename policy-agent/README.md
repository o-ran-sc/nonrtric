# O-RAN-SC NonRT RIC Dashboard Web Application

The O-RAN NonRT RIC PolicyAgent provides a REST API for management of 
policices. It provides support for 
-Policy configuration. This includes
 -One REST API towards all RICs in the network
 -Query functions that can find all policies in a RIC, all policies owned by a service (R-APP), all policies of a type etc.
 -Maps O1 resources (ManagedElement) as defined in O1 to the controlling RIC 
-Supervision of clients (R-APPs) to eliminate stray policies in case of failure
-Consistency monitoring of the SMO view of policies and the actual situation in the RICs
-Consistency monitoring of RIC capabilities (policy types)

The agent can be run stand alone in a simulated test mode. Then it 
simulates RICs. 
The REST API is published on port 8081 and it is started by command:
mvn -Dtest=MockPolicyAgent test

The backend server publishes live API documentation at the
URL `http://your-host-name-here:8080/swagger-ui.html`

## License

Copyright (C) 2019 Nordix Foundation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
