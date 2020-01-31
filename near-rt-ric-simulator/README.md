# O-RAN-SC Near-RealTime RIC Simulator

The O-RAN SC Near-RealTime RIC simulates the A1 as an generic REST API which can receive and send northbound messages. The simulator validates the payload and applies policy.

The simulator handles the requests that are defined in the A1 open API yaml file. All these requests are simulated in the a1.py file. The available requests and the addresses are currently:
 - GET all policy identities (respectively for a policy type if query parameter used): http://localhost:8085/A1-P/v1/policies?policyTypeId={policyTypeId}
 - PUT a policy instance(create or update it): http://localhost:8085/A1-P/v1/policies/{policyId}?policyTypeId={policyTypeId}
 - GET a policy: http://localhost:8085/A1-P/v1/policies/{policyId}
 - DELETE a policy instance: http://localhost:8085/A1-P/v1/policies/{policyId}
 - GET a policy status: http://localhost:8085/A1-P/v1/policystatus
 - GET all policy types: http://localhost:8085/A1-P/v1/policytypes
 - GET the schemas for a policy type: http://localhost:8085/A1-P/v1/policytypes/{policyTypeId}

Additionally, there are requests that are defined in main.py as an administrative API. The goal is to handle information that couldn't be handled using the A1 interface. The available requests and the addresses are currently:
 - GET, a basic healthcheck: http://localhost:8085/
 - PUT a policy type: http://localhost:8085/policytypes/{policyTypeId}
 - DELETE a policy type: http://localhost:8085/policytypes/{policyTypeId}
 - DELETE all policy instances: http://localhost:8085/deleteinstances
 - DELETE all policy types: http://localhost:8085/deletetypes
 - PUT a status to a policy instance with an enforceStatus parameter only: http://localhost:8085/{policyId}/{enforceStatus}
 - PUT a status to a policy instance with both enforceStatus and enforceReason: http://localhost:8085/{policyId}/{enforceStatus}/{enforceReason}

The backend server publishes live API documentation at the URL `http://your-host-name-here:8080/swagger-ui.html`

# Starting up the simulator
First, download the nonrtric repo on gerrit:
git clone "https://gerrit.o-ran-sc.org/r/nonrtric"

Then, build the docker container:
docker build -t simulator .

To run it, use the command:
docker run -it -p 8085:8085 simulator

Note: -p 8085:8085 allows to map the port inside the container to any port you choose. One can for example choose -p 8084:8085; in that case, all the addresses mentioned above should be modified accordingly.

Let the simulator run in one terminal; in another terminal, one can run the command ./commands.sh. It contains the main requests, and will eventually leave the user with a policy type STD_QoSNudging_0.2.0 and a policy instance pi1 with an enforceStatus set to NOT_ENFORCED and an enforce Reason set to 300.
All the response codes should be 20X, otherwise something went wrong.

## License

Copyright (C) 2019 Nordix Foundation.
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
