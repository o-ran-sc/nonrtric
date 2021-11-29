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
Rename application_configuration.controller.json to application_configuration.json & start the container. Don't forget to add the A1 Adapter url,username & password in the application_configuration.json file.
You also need to update the A1 Adapter url & credentials in the application_configuration file.
Ex:
For example if you use the OSC A1 Adapter then,
"baseUrl": "http://a1controller:8282"
"userName": "admin",
"password": "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"
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

prepareIcsData.sh
This is to generate some data into the ICS microservice

prepareDmaapMsg.sh
This is to generate some data into the Dmaap MR, so that PMS reads message from MR

## O-RAN-SC Control Panel

The Non-RT RIC Control Panel is a graphical user interface that enables the user to view and manage the A1 policies in the RAN and also view producers and jobs for the Information coordinator service.

### O-RAN-SC Control Panel Gateway:

To view the policy or information jobs and types in control panel gui along with Policy Management Service & Information Coordinator Service you should also have nonrtric gateway because all the request from the gui is passed through this API gateway.

#### Prerequisite:

Make sure to follow the section regarding sample data so there is data available to see in the interface.

To start all the necessary components, run the following command:

docker-compose -f docker-compose.yaml -f control-panel/docker-compose.yaml -f nonrtric-gateway/docker-compose.yaml -f policy-service/docker-compose.yaml -f ics/docker-compose.yaml -f a1-sim/docker-compose.yaml up
