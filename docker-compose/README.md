# O-RAN-SC docker-compose files

The docker-compose.yml file will create an entire nonrtric system with one command:
docker-compose up

Two docker-compose files are provides in this folder:

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
./fill_data.sh [policy-agent port] [a1-sim-OSC port] [a1-sim-STD port] [http/https]

## License

Copyright (C) 2019 AT&T Intellectual Property. All rights reserved.
Modifications Copyright (C) 2019 Nordix Foundation
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
