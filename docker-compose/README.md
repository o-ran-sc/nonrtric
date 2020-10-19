# O-RAN-SC docker-compose files

The docker-compose.yml file will create an entire nonrtric system with one command:
docker-compose up

Two docker-compose files are provided in this folder:

nosdnc/docker-compose.yml
This file is to create nonrtric system without sdnc a1-controller

sdnc/docker-compose.yml
This file is to create nonrtric system with sdnc a1-controller

Howto:
cd nosdnc/
docker-compose up

or

cd sdnc/
docker-compose up

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
