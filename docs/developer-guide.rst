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
2. Maven dependency-management tool, version 3.6 or later
3. Python, version 2
4. Docker, version 19.03.1 or latest
5. Docker Compose, version 1.24.1 or latest

Build and run
-------------
Go to the northbound directory and run this command ::
    mvn clean install

This will build the project and create artifcats in maven repo

Go to oam/installation directory and run this command ::
    mvn clean install -P docker

This will create the docker images required for A1 controller.

After this step check for the docker images created by the maven build with this command ::
    docker images | grep a1-controller

Go to oam/installation/src/main/yaml and run this command ::
    docker-compose up -d a1-controller

This will create the docker containers with the A1 controller image, you can check the status of the docker container using ::
    docker-compose logs -f a1-controller

The SDNC url to access the Northbound API,
    http://localhost:8282/apidoc/explorer/index.html

Credentials: admin/Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U

Configuration of certs
----------------------
The SDNC-A1 controller uses the default keystore and truststore that are built into the container.

The paths and passwords for these stores are located in a properties file:
 nonrtric/sdnc-a1-controller/oam/installation/src/main/properties/https-props.properties

The default truststore includes the a1simulator cert as a trusted cert which is located here:
 https://gerrit.o-ran-sc.org/r/gitweb?p=sim/a1-interface.git;a=tree;f=near-rt-ric-simulator/certificate;h=172c1e5aacd52d760e4416288dc5648a5817ce65;hb=HEAD

The default keystore, truststore, and https-props.properties files can be overridden by mounting new files using the "volumes" field of docker-compose. Uncommment the following lines in docker-compose to do this, and provide paths to the new files:

::

#volumes:
#   - <path_to_keystore>:/etc/ssl/certs/java/keystore.jks:ro
#   - <path_to_truststore>:/etc/ssl/certs/java/truststore.jks:ro
#   - <path_to_https-props>:/opt/onap/sdnc/data/properties/https-props.properties:ro

The target paths in the container should not be modified.

For example, assuming that the keystore, truststore, and https-props.properties files are located in the same directory as docker-compose:

`volumes:`
    `- ./new_keystore.jks:/etc/ssl/certs/java/keystore.jks:ro`

    `- ./new_truststore.jks:/etc/ssl/certs/java/truststore.jks:ro`

    `- ./new_https-props.properties:/opt/onap/sdnc/data/properties/https-props.properties:ro`

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

Configuration of certs
----------------------
The Policy Agent uses the default keystore and truststore that are built into the container. The paths and passwords for these stores are located in a yaml file:
 nonrtric/policy-agent/config/application.yaml

The default truststore includes a1simulator cert as a trusted cert which is located here:
 https://gerrit.o-ran-sc.org/r/gitweb?p=sim/a1-interface.git;a=tree;f=near-rt-ric-simulator/certificate;h=172c1e5aacd52d760e4416288dc5648a5817ce65;hb=HEAD

The default truststore also includes a1controller cert as a trusted cert which is located here (keystore.jks file):
 https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=sdnc-a1-controller/oam/installation/sdnc-a1/src/main/resources;h=17fdf6cecc7a866c5ce10a35672b742a9f0c4acf;hb=HEAD

There is also Policy Agent's own cert in the default truststore for mocking purposes and unit-testing (ApplicationTest.java).

The default keystore, truststore, and application.yaml files can be overridden by mounting new files using the "volumes" field of docker-compose or docker run command.

Assuming that the keystore, truststore, and application.yaml files are located in the same directory as docker-compose, the volumes field should have these entries:

`volumes:`
      `- ./new_keystore.jks:/opt/app/policy-agent/etc/cert/keystore.jks:ro`

      `- ./new_truststore.jks:/opt/app/policy-agent/etc/cert/truststore.jks:ro`

      `- ./new_application.yaml:/opt/app/policy-agent/config/application.yaml:ro`

The target paths in the container should not be modified.

Example docker run command for mounting new files (assuming they are located in the current directory):

`docker run -p 8081:8081 -p 8433:8433 --name=policy-agent-container --network=nonrtric-docker-net --volume "$PWD/new_keystore.jks:/opt/app/policy-agent/etc/cert/keystore.jks" --volume "$PWD/new_truststore.jks:/opt/app/policy-agent/etc/cert/truststore.jks" --volume "$PWD/new_application.yaml:/opt/app/policy-agent/config/application.yaml" o-ran-sc/nonrtric-policy-agent:2.0.0-SNAPSHOT`

End-to-end call
===============

In order to make a complete end-to-end call, follow the instructions given in this `guide`_.

.. _guide: https://wiki.o-ran-sc.org/pages/viewpage.action?pageId=12157166
