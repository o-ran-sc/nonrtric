.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2020 Nordix

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC.


==================

Prerequisites
-------------

1. Java development kit (JDK), version 11
2. Maven dependency-management tool, version 3.6 or later
3. Python, version 2
4. Docker, version 19.03.1 or later (latest version)
5. Docker Compose, version 1.24.1 or later (latest version)

Build and run
-------------
1. Download the nonrtric repo (defaults to master branch): 
     git clone "https://gerrit.o-ran-sc.org/r/nonrtric"
	 
2. Configure policy-agent:

   To support local test with two separate NearRT-RIC simulator instances:  

create a new nonrtric/policy-agent/config/application_configuration.json with the configuration below.
The controller hostname and port values come from and must match those defined in nonrtric/sdnc-a1-controller/oam/installation/src/main/yaml/docker-compose.yml
any defined ric names must match the given docker container names in nearRT-RIC simulator startup - port is always the simulator's internal 8085

application_configuration.yaml

{
   "config": {
      "//description": "Application configuration",
      "controller": [
         {
            "name": "controller1",
            "baseUrl": "http://a1-controller-container:8181",
            "userName": "admin",
            "password": "Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U"
         }
      ],
      "ric": [
         {
            "name": "ric1",
            "baseUrl": "http://ric1:8085/",
            "controller": "controller1",
            "managedElementIds": [
               "kista_1",
               "kista_2"
            ]
         },
                  {
            "name": "ric2",
            "baseUrl": "http://ric2:8085/",
            "controller": "controller1",
            "managedElementIds": [
               "kista_3",
               "kista_4"
            ]
         }
      ]
   }
}	 

3. Build the code and create docker images

    To build docker images of sdnc-a1-controller and policy-agent:

cd nonrtric
mvn clean install -Dmaven.test.skip=true	 

This will build the project and create artifcats in maven repo

4. Build near-rt-ric-simulator container
   Download the near-rt-ric-simulator repo (defaults to master branch): 

     git clone "https://gerrit.o-ran-sc.org/r/sim/a1-interface"
	 
5. Create docker image
To create docker image near-rt-ric-simulator (note that the given image name must match the name given in docker startup later):

cd a1-interface/near-rt-ric-simulator

docker build -t near-rt-ric-simulator:latest .

6. Build controlpanel container
   Download the nonrtric repo (defaults to master branch): 

     git clone "https://gerrit.o-ran-sc.org/r/portal/nonrtric-controlpanel"
	 
   cd nonrtric-controlpanel
   mvn clean install -Dmaven.test.skip=true

7. Run A1 Controller Docker Container
   A1 Controller must be started first to set up docker network
  Change directory: 
  cd nonrtric/sdnc-a1-controller/oam/installation/src/main/yaml
  Run docker container using the command below 
  
  docker-compose up a1-controller


8. Run Near-RT-RIC Simulator Docker Containers

Start docker containers for each ric defined in nonrtric/policy-agent/config/application_configuration.json in previous steps (in this example for ric1 and ric2) and providing A1 interface version OSC_2.1.0 with the following commands:: 
docker run -p 8085:8085 -e A1_VERSION=OSC_2.1.0 --network=nonrtric-docker-net --name=ric1 near-rt-ric-simulator:latest
docker run -p 8086:8085 -e A1_VERSION=OSC_2.1.0 --network=nonrtric-docker-net --name=ric2 near-rt-ric-simulator:latest

Change directory:
  
cd a1-interface/near-rt-ric-simulator/test/OSC_2.1.0/jsonfiles
Put an example policy_type into the started near-rt-ric-simulator instances by running these curl commands (in this example to ric1 exposed to port 8085 and ric2 exposed to port 8086):
curl -X PUT -v "http://localhost:8085/a1-p/policytypes/123" -H "accept: application/json" \
 -H "Content-Type: application/json" --data-binary @pt1.json
curl -X PUT -v "http://localhost:8086/a1-p/policytypes/123" -H "accept: application/json" \
 -H "Content-Type: application/json" --data-binary @pt1.json
 
9. Run Policy-agent Docker Container

Run docker container using this command once A1 Controller and simulators have been fully started: 
docker run -p 8081:8081 --network=nonrtric-docker-net --name=policy-agent-container o-ran-sc/nonrtric-policy-agent:1.0.0-SNAPSHOT

Once policy-agent is up and running, it establishes connections to all configured NearRT-RICs
If policy-agent-container is configured to log at DEBUG level, the following logs should appear to log to show that connection to the configured RICs has been established successfully via A1 Controller.

SDNC A1 Client
$ docker logs policy-agent-container | grep "protocol version"
2020-04-17 11:10:11.357 DEBUG 1 --- [or-http-epoll-1] o.o.policyagent.clients.A1ClientFactory  : Established protocol version:SDNC_OSC_OSC_V1 for Ric: ric1
2020-04-17 11:10:11.387 DEBUG 1 --- [or-http-epoll-1] o.o.policyagent.clients.A1ClientFactory  : Established protocol version:SDNC_OSC_OSC_V1 for Ric: ric2

10. policy-agent Swagger API
    Access policy-agent swagger API from url: http://localhost:8081/swagger-ui.html

11. Run Non-RT-RIC Controlpanel Docker Container
     Run docker container using this command: 

    docker run -p 8080:8080 --network=nonrtric-docker-net o-ran-sc/nonrtric-controlpanel:1.0.0-SNAPSHOT
	
12. Open Daylight GUI
    Open Daylight GUI can be accessed by pointing web-browser to this URL:
    http://localhost:8282/apidoc/explorer/index.html
    Username/password: admin/Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U
	
13. Open Controlpanel UI
    Dashboard UI can be accessed by pointing the web-browser to this URL: 

    http://localhost:8080/


Policy Agent
============

The O-RAN Non-RT RIC Policy Agent provides a REST API for management of policices. It provides support for:

 * Supervision of clients (R-APPs) to eliminate stray policies in case of failure
 * Consistency monitoring of the SMO view of policies and the actual situation in the RICs
 * Consistency monitoring of RIC capabilities (policy types)
 * Policy configuration. This includes:

   * One REST API towards all RICs in the network
   * Query functions that can find all policies in a RIC, all policies owned by a service (R-APP), all policies of a type etc.
   * Maps O1 resources (ManagedElement) as defined in O1 to the controlling RIC.

| The Policy Agent can be accessed over the REST API or through the DMaaP Interface. The REST API is documented in the
| *nonrtric/policy-agent/docs/api.yaml* file. Please refer to the README file of Policy Agent to know more about the API's.

End-to-end call
===============

In order to make a complete end-to-end call, follow the instructions given in this `guide`_.

.. _guide: https://wiki.o-ran-sc.org/pages/viewpage.action?pageId=12157166
