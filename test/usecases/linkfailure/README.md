# Use case Link Failure

## General

The Link Failure use case test provides a python script that regularly polls DMaaP Message Router (MR) for "CUS Link Failure"
messages.

When such a message appears with the "eventSeverity" set to anything but "NORMAL", a configuration change message with the
"administrative-state" set to "UNLOCKED" will be sent to the O-DU mapped to the O-RU that sent the alarm.

When such a message appears with the "eventSeverity" set to "NORMAL" a printout will be made to signal that the
alarm has been cleared, provided that the verbose option has been used when the test was started.

## Prerequisits

To run this script Python3 needs to be installed. To install the script's dependencies, run the following command from
the `app` folder: `pip install -r requirements.txt`

Also, the MR needs to be up and running with a topic created for the alarms and there must be an endpoint for the
configuration change event that will accept these.

The host names and the ports to the MR and SDNR services can be provided when the container is started if the default
values are not correct. The topic can also be changed.

The mapping from O-RU ID to O-DU ID is specified in the file `o-ru-to-o-du-map.txt`. This can be replaced by providing
a different file when starting the application.

For convenience, a message generator and a change event endpoint simulator are provided.

## How to run from command line

Go to the `app/` folder and run `python3 main.py`. The script will start and run until stopped. Use the `-h` option to
see the options available for the script.

## How to run in Docker

Go to the `app/` folder and run `docker build -t oru-app .`.

The container must be connected to the same network as the MR and SDNR are running in. Some of the parameters to the application
can be provided with the `-e PARAM_NAME=PARAM_VALUE` notation. Start the container by using the command, with available params listed:
 `docker run --network [NETWORK NAME] --name oru-app -e VERBOSE=on -e MR-HOST=[HOST NAME OF MR] -e MR-PORT=[PORT OF MR] -e SDNR-HOST=[HOST NAME OF SDNR] -e SDNR-PORT=[PORT OF SDNR] oru-app`.

To build the image for the message generator, run the following command from the `simulators` folder:
`docker build -f Dockerfile-message-generator -t message-generator .`

The message generator's container must be connected to the same network as the other components are running in. Some of the
parameters to the application can be provided with the `-e PARAM_NAME=PARAM_VALUE` notation. Start the container by
using the command, with available params listed:
 `docker run --network [NETWORK NAME] --name message-generator -e MR-HOST=[HOST NAME OF MR] -e MR-PORT=[PORT OF MR] message-generator`.

To build the image for the SDNR simulator, run the following command from the `simulators` folder:
`docker build -f Dockerfile-sdnr-sim -t sdnr-simulator .`

The SDNR simulator's container must be connected to the same network as the the other components are running in. Some of the
parameters to the application can be provided with the `-e PARAM_NAME=PARAM_VALUE` notation. Start the container by
using the command, with available params listed:
 `docker run --network [NETWORK NAME] --name sdnr-simulator -e MR-HOST=[HOST NAME OF MR] -e MR-PORT=[PORT OF MR] sdnr-simulator`.

## Use docker-compose

Go to the `docker-compose/` folder and run `bash start.sh`.

This scripts will start up four components:
dmaap-mr
oru-app
sdnr-simulator
message-generator

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
