.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2020 Nordix

Developer Guide
===============

This document provides a quickstart for developers of the Non-RT RIC.

A1 Policy Management Service
----------------------------

The Policy Management is implemented in ONAP. For documentation see `readthedocs`_ and `wiki`_.

.. _readthedocs: https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html
.. _wiki: https://wiki.onap.org/pages/viewpage.action?pageId=84644984

Enrichment Coordinator Service
------------------------------
The Enrichment Coordinator Service is a Java 11 web application built using the Spring Framework.
Using Spring Boot dependencies, it runs as a standalone application.

Its main functionality is to act as a data subscription broker and to decouple data 
producer from data consumers.

See the ./config/README file on how to create and setup the cerificates and private keys needed for HTTPS. 


rAPP Catalogue
--------------

See the README.md file in the r-app-catalogue folder for how to run the component.


Gateway in Nonrtric
-------------------
Nonrtric currently supports Spring Cloud Gateway & Kong Gateway, follow the instructions give in this `page`_.

.. _page: https://wiki.o-ran-sc.org/display/RICNR/Release+D


End-to-end call
---------------

In order to make a complete end-to-end call, follow the instructions given in this `guide`_.

.. _guide: https://wiki.o-ran-sc.org/pages/viewpage.action?pageId=12157166
