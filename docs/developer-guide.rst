.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0

SDNC A1 Controller Developer Guide
==================================

This document provides a quickstart for developers of the O-RAN SC A1 Controller SDNC Application

Prerequisites
-------------

1. Java development kit (JDK), version 8
2. Maven dependency-management tool, version 3.4 or later
3. Python, version 2
4. Docker, version 19.03.1 or later
5. Docker Compose, version 1.24.1 or later

Go to the northbound directory and run this command ::
    mvn clean install

This will build the project and create artifcats in maven repo

Go to oam/installation directory and run this command ::
    mvn clean install -P docker

This will create the docker images required for sdnc

After this step check for the docker images created by the maven build with this command ::
    docker images | grep sdnc

Go to oam/installation/src/main/yaml and run this command ::
    docker-compose up -d sdnc

This will create the docker containers with the sdnc image, you can check the status of the docker container using ::
    docker-compose logs -f sdnc

The SDNC url to access the Northbound API,
    http://localhost:8282/apidoc/explorer/index.html

Credentials: admin/Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U

Policy Agent
=====================================
The O-RAN NonRT RIC PolicyAgent provides a REST API for management of policices. It provides support for: -Supervision of clients (R-APPs) to eliminate stray policies in case of failure -Consistency monitoring of the SMO view of policies and the actual situation in the RICs -Consistency monitoring of RIC capabilities (policy types) -Policy configuration. This includes: -One REST API towards all RICs in the network -Query functions that can find all policies in a RIC, all policies owned by a service (R-APP), all policies of a type etc. -Maps O1 resources (ManagedElement) as defined in O1 to the controlling RIC.
The PolicyAgent can be accessed over the REST API or throught DMaaP Interface. Please Refer README file of PolicyAgent to know more about the API's.


Near-RT RIC Simulator Developer Guide
=====================================

Prerequisites
-------------

1. Java development kit (JDK), version 8
2. Maven dependency-management tool, version 3.4 or later

Go to the nearric-simulator/ directory and run this command ::
     mvn clean install

If you want to genereate the sources from A1 yaml file, Go to nearric-simulator/a1-med-api/ and run this command ::
     mvn generate-sources

This will generate the A1PApi.java you need to call the generate-sources maven life cycle to generate the file

The backend server listens for requests at this URL:
   http://localhost:8080

The backend server publishes live API documentation at the URL
   http://localhost:8080/swagger-ui.html

End-to-end call
===============

In order to make a complete end-to-end call, follow the instructions given in this `guide`_.

.. _guide: https://wiki.o-ran-sc.org/pages/viewpage.action?pageId=12157166
