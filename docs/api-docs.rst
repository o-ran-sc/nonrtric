.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2020 Nordix

.. _api_docs:


========
API-Docs
========

This is the API-docs of Non-RT RIC.

.. contents::
   :depth: 3
   :local:

The Non-RT RIC consists of two parts, described in the sections below:
 * The Policy Agent
 * The SDNC A1 Controller


Policy Agent
============

The Policy Agent provides common functionality useful for R-Apps, for instance:
 * A repository of available Near-RT RICs, their policy types and policy instances.
 * An A1 connection to the Near-RT RICs.

See :ref:`policy-agent-api` for how to use the API.

See the README.md file in the nonrtric/policy-agent repo for info about how to use it.

API Functions
-------------
See the following document for the Policy Agent API: nonrtric/policy-agent/docs/api.yaml

SDNC A1 Controller
==================

An ONAP SDNC Controller for the A1 interface.

See :ref:`sdnc-a1-controller-api` for how to use the API.

See the README.md file in the nonrtric/sdnc-a1-controller repo for info about how to use it.

Complementary tools
===================

There are two additional tools that can be used together with the Non-RT RIC, namely the Control Panel and the Near-RT RIC simulator.

The Non-RT RIC Control Panel provides a user interface that allows the user to interact with the Non-RT RIC.
Documentation for the Control Panel can be found here:
:doc:`Non-RT RIC Control Panel <nonrtric-controlpanel:index>`.
It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/portal/nonrtric-controlpanel".

The Near-RT RIC simulator simulates an A1 protocol termination endpoint. Documentation for the simulator can be found
here: :doc:`A1 Interface Simulator <sim-a1-interface:index>`. It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/sim/a1-interface"
