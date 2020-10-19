.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2020 Nordix

Installation Guide
==================

.. contents::
   :depth: 3
   :local:

Abstract
--------

This document describes how to install the Non-RT RIC SDNC A1 Controller, its dependencies and required system resources.

This work is in progress. Please visit :ref:`api_docs` for more information about the SDNC A1 Controller and the Policy Agent.

.. _api-docs page: ./api-docs.html

Installation
------------

Download the SDNC repo:

   git clone "https://gerrit.o-ran-sc.org/r/nonrtric"

The SDNC A1 Controller could be found in this repo.

Build SDNC project:

   Enter into the sdnc-a1-controller project, northbound and oam project will located there.

      cd sdnc-a1-controller

   Build northbound project with command:

      mvn clean install -Dmaven.test.skip=true

   Build oam project with command:

      mvn clean install -Dmaven.test.skip=true -P docker

   Enter into this directory:

      cd nonrtric/sdnc-a1-controller/oam/installation/src/main/yaml

   and run the command:

      MTU=1500 docker-compose up a1-controller

Version history
---------------

+--------------------+--------------------+--------------------+--------------------+
| **Date**           | **Ver.**           | **Author**         | **Comment**        |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
| 2019-11-12         | 0.1.0              | Maxime Bonneau     | First draft        |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
| 2020-03-24         | 0.1.1              | Maxime Bonneau     | Second draft       |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+
|                    | 1.0                |                    |                    |
|                    |                    |                    |                    |
|                    |                    |                    |                    |
+--------------------+--------------------+--------------------+--------------------+



