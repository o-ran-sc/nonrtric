.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2025 Nordix

Use Cases
#########

To support the use cases defined for the Non-RT RIC, there are implementations provided in the Non RT-RIC project.

Health Check
------------

The Health Check use case for the Non-RT RIC is a python script that regularly creates, reads, updates, and deletes a
policy in all Near-RT RICs that support the type used by the script. A self refreshing web page provides a view of
statistics for these regular checks.

For more information about it, see the README file in `repo <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric%2Frapp%2Fhealthcheck.git;a=summary>`_.

.. image:: ./images/healthcheck.png

O-RU Front-Haul Recovery
------------------------

This use case is a non-real-world closed-loop use case to demonstrate automated recovery when the front-haul connection between an O-DU and O-RU is reset.
An application in the NONRTRIC senses the fault from the O-RU (O1-FM) and initiates a NETCONF reset operation (O1-CM) using the OAM controller.
More details about the use case can be found on the O-RAN SC wiki: `RSAC <https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/15075523/Control+Loops+for+O-RU+Fronthaul+Recovery+usecase+G+Release>`_ and `OAM <https://lf-o-ran-sc.atlassian.net/wiki/spaces/OAM/pages/14680350/Closed+loop+use+case>`_.

Non-RT RIC provides multiple implementation versions of the recovery part of the use case. One in the form of a python
script, one utilizing the ONAP Policy Framework, and one Go version that utilizes Information Coordination Service (ICS).

The code is available in the `use case repo <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric%2Frapp%2Forufhrecovery.git;a=summary>`_

Standalone Script Solution
++++++++++++++++++++++++++

The script version consists of a python script that performs the tasks needed for the use case. There are also two
simulators. One message generator that generates alarm messages, and one SDN-R simulator that receives the config
change messages sent from the script and responds with alarm cleared messages to MR.

All parts are Dockerized and can be started as individual containers, in the same network, in Docker.

ONAP Policy Solution
++++++++++++++++++++

There is also another solution for performing the front-haul recovery that is based on `ONAP Policy Framework <https://lf-onap.atlassian.net/wiki/spaces/DW/pages/16230647/Policy+Framework+Project>`_.
A TOSCA Policy has been created that listens to DMaaP Message Router, makes a decision on an appropriate remedy and then signals the decision as a configuration change message via
REST call to the OAM controller.

There is a `docker-compose <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=docker-compose/docker-compose-policy-framework>`_ available
in the nonrtric repo for bringing up the complete standalone version of ONAP Policy Framework.

The detailed instructions for deploying and running this policy are provided in
the `wiki <https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/15074820/O-RU+Fronthaul+Recovery+usecase>`_.

ICS Consumer Solution
+++++++++++++++++++++

The ICS Consumer solution is implemented in Go and instead of polling MR itself, it registers as a consumer of the "STD_Fault_Messages" job in ICS.

O-DU Slice Assurance
--------------------

A very simplified closed-loop rApp use case to re-prioritize a RAN slice's radio resource allocation priority if sufficient throughput cannot be maintained. Not intended to to be 'real-world'.

The Go implementation of the solution can be found in
this `link <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric/rapp/ransliceassurance.git;a=tree;f=smoversion>`__.
