.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2020 Nordix

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC.

A1 Policy Management Service
----------------------------

The A1 Policy Management Service is implemented in ONAP. For documentation see `ONAP CCSDK documentation <https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html>`_ and `wiki`_.

.. _wiki: https://wiki.onap.org/pages/viewpage.action?pageId=84672221

Enrichment Coordinator Service
------------------------------
The Enrichment Coordinator Service is a Java 11 web application built using the Spring Framework.
Using Spring Boot dependencies, it runs as a standalone application.

Its main functionality is to act as a data subscription broker and to decouple data 
producer from data consumers.

See the ./config/README file on how to create and setup the cerificates and private keys needed for HTTPS. 


Service Catalogue
-----------------

See the README.md file in the r-app-catalogue folder for how to run the component.

Kubernetes deployment
^^^^^^^^^^^^^^^^^^^^^

Non-RT RIC can be also deploy as a kubernetes cluster, it/dep repository hosts deployment and integration artifacts and can be clone from this `page <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_. Instructions and helm charts to deploy the Non-RT-RIC functions in OSC's integrated test environment can be found in the ./nonrtric directory.
For more information see `Integration and Testing documentation <https://docs.o-ran-sc.org/projects/o-ran-sc-it-dep/en/latest/index.html>`_