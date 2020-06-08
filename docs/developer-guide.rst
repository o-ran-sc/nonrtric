.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2020 Nordix

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC.

SDNC A1 Controller
==================

Prerequisites
-------------

1. Java development kit (JDK), version 8
2. Maven dependency-management tool, version 3.4 or later
3. Python, version 2
4. Docker, version 19.03.1 or later
5. Docker Compose, version 1.24.1 or later

Build and run
-------------
Go to the northbound directory and run this command ::
    mvn clean install

This will build the project and create artifcats in maven repo

Go to oam/installation directory and run this command ::
    mvn clean install -P docker

This will create the docker images required for the A1 Controller

After this step check for the docker images created by the maven build with this command ::
    docker images | grep a1-controller

Go to oam/installation/src/main/yaml and run this command ::
    docker-compose up -d a1-controller

This will create the docker containers with the A1 Controller image, you can check the status of the docker container using ::
    docker-compose logs -f a1-controller

The SDNC url to access the Northbound API,
    http://localhost:8282/apidoc/explorer/index.html

Credentials: admin/Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U

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
