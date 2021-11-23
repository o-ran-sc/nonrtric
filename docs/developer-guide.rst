.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2020 Nordix

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC parts.

Additional developer guides are available on the `O-RAN SC NONRTRIC Developer wiki <https://wiki.o-ran-sc.org/display/RICNR/Release+D>`_

A1 Policy Management Service & SDNC/A1 Controller & A1 Adapter
--------------------------------------------------------------

The A1 Policy Management Service is implemented in ONAP. For documentation see `ONAP CCSDK documentation <https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html>`_
and `wiki <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_.

Information Coordinator Service
-------------------------------
The Information Coordinator Service is a Java 11 web application built using the Spring Framework. Using Spring Boot
dependencies, it runs as a standalone application.

Its main functionality is to act as a data subscription broker and to decouple data producer from data consumers.

See the ./config/README file in the *information-coordinator-service* directory Gerrit repo on how to create and setup
the certificates and private keys needed for HTTPS.

Start standalone
++++++++++++++++

The project uses Maven. To start the Information Coordinator Service as a freestanding application, run the following
command in the *information-coordinator-service* directory:

    +-----------------------------+
    | mvn spring-boot:run         |
    +-----------------------------+

Start in Docker
+++++++++++++++

To build and deploy the Information Coordinator Service, go to the "information-coordinator-service" folder and run the
following command:

    +-----------------------------+
    | mvn clean install           |
    +-----------------------------+

Then start the container by running the following command:

    +--------------------------------------------------------------------+
    | docker run nonrtric-information-coordinator-service                |
    +--------------------------------------------------------------------+

Initial Non-RT-RIC App Catalogue
--------------------------------

See the README.md file in the *r-app-catalogue* directory in the Gerrit repo for more details how to run the component.

Kubernetes deployment
=====================

Non-RT RIC can be also deployed in a Kubernetes cluster, `it/dep repository <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_
hosts deployment and integration artifacts. Instructions and helm charts to deploy the Non-RT-RIC functions in the
OSC NONRTRIC integrated test environment can be found in the *./nonrtric* directory.
For more information see `Integration and Testing documentation on the O-RAN-SC wiki <https://docs.o-ran-sc.org/projects/o-ran-sc-it-dep/en/latest/index.html>`_.
