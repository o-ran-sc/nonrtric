.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0


========
API-Docs
========

This is the API-docs of Non-RT RIC.

.. contents::
   :depth: 3
   :local:

The Non-RT RIC consists of four parts, described in the sections below:
 * The Dashboard
 * The Policy Agent
 * The Near-RT RIC simulator
 * The Sdnc A1 Controller

Dashboard
=========

The Non-RT RIC dashboard is an interface that allows human users to create, edit and delete policy instances, for each existing policy type. The policy types are owned by the Near-RT RIC, Non-RT RIC can just query them, so it's not possible to act on them.

See the README.md file in the nonrtric/dashboard repo for info about how to use it.

API Functions
-------------

To run the dashboard locally, you can follow these steps:

- Fetch the latest code from `gerrit`_

.. _gerrit: https://gerrit.o-ran-sc.org/r/admin/repos/nonrtric

- Before compiling, run the following commands::

    git submodule init

    git submodule update

- Start the backend (you might have to build it first)::

    mvn clean install

    mvn -Dorg.oransc.ric.portal.dashboard=mock -Dtest=DashboardTestServer -DfailIfNoTests=false test


- Now you can open URL:  `localhost:8080`_ in a browser.

.. _localhost:8080: localhost:8080

From the main page, click on the "Policy Control" card. From here, it is possible to create or list instances for each existing policy type.

When the instances are listed, it is possible to edit or delete each instance from the expanded view.

.. image:: ./images/non-RT_RIC_dashboard.png

Policy Agent
============

The Policy Agent provides common functionality useful for R-Apps, for instance:
 * A repository of available Near-T RICs, their policy types and policy instances.
 * An A1 connection to he Near-RT RICs.

See the README.md file in the nonrtric/policy-agent repo for info about how to use it.

API Functins

------------
See the following document for the Policy Agent API: nonrtric/policy-agent/docs/api.doc.

Near-RT RIC Simulator
=====================

A simulator that simulates a Near-RT RIC, with a termination of the A1 interface. It also provides an administrative API to manage types and instances so it can be programatically set up for use in tests.

See the README.md file in the nonrtric/near-rt-ric-simulator repo for info about how to use it.

API Functions
-------------

See the admnistrative API in: nonrtric/near-rt-ric-simulator/ric-plt/a1/main.py.

Sdnc A1 Controller
==================

An ONAP SDNC Controller for the A1 interface.

See the README.md file in the nonrtric/sdnc-a1-controller repo for info about how to use it.

