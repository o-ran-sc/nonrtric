## License
Copyright (C) 2020 Nordix Foundation.
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

## O-RAN-SC docker-compose files:
The docker compose file helps the user to deploy all or partial components of nonrtric with one command.

All the components in nonrtric has individual docker compose file so you can simply mix and match different components and deploy
it at your preference.

For ex.
I want to Control Panel, A1 Policy Management Service & A1 Simulator,
docker-compose -f docker-compose.yaml -f control-panel/docker-compose.yaml -f policy-service/docker-compose.yaml -f a1-sim/docker-compose.yaml up -d

To remove all the containers use the same command,
docker-compose -f docker-compose.yaml -f control-panel/docker-compose.yaml -f policy-service/docker-compose.yaml -f a1-sim/docker-compose.yaml down

It can be used with any combination to deploy nonrtric components.

## Policy Service Prerequisite:
The A1 Policy Service can perform A1 Policy management with or without A1 Adapter. To enable/disable A1 Adapter all you have to do is,
With SDNC A1 Adapter:
Rename application_configuration.controller.json to application_configuration.json & start the container. Don't forget to add the A1 controller url,username & password in the application_configuration.json file.
Ex:
docker-compose -f docker-compose.yaml -f policy-service/docker-compose.yaml -f a1-sim/docker-compose.yaml up -d
Without SDNC A1 Adapter:
Rename application_configuration.nocontroller.json to application_configuration.json & start the container.
Ex:
docker-compose -f docker-compose.yaml -f policy-service/docker-compose.yaml -f a1-sim/docker-compose.yaml up -d

## To create sample data:
The scripts in data/ will generate some dummy data in the running system.
It will create:
one policy type in a1-sim-OSC
one service in policy agent
one policy in a1-sim-OSC
one policy in a1-sim-STD

Run command:
cd data/
./preparePmsData.sh [policy-agent-port] [a1-sim-OSC-port] [a1-sim-STD-port] [http/https]

Open link:
http://localhost:[control-panel-port]/

All the generated data is shown on the web page

By default, if the containers are started up and running by docker-compose file in the same directory, just run commands:
./preparePmsData.sh

prepareEcsData.sh
This is to generate some data into the ECS microservice

prepareDmaapMsg.sh
This is to generate some data into the Dmaap MR, so that PMS reads message from MR