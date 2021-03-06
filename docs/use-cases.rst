.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix

Use Cases
=========

To support the use cases defined for the Non-RT RIC, there are implementations provided in the nonrtric repo, see:
  nonrtric/test/usecases

Health Check
------------
The Health Check use case for the Non-RT RIC is a python script that regularly creates, reads, updates, and deletes a
policy in all Near-RT RICs that support the type used by the script. A self refreshing web page provides a view of
statistics for these regular checks.

For more information about it, see the README file in the use case's folder.

.. image:: ./images/healthcheck.png

O-RU closed loop recovery
-------------------------

This use case is a non-real-world closed-loop use case to demonstrate automated recovery when the front-haul connection between an O-DU and O-RU is reset. 
An application in the NONRTRIC senses the fault from the O-RU (O1-FM) and initiates a NETCONF reset operation (O1-CM) using the OAM controller. 
More details about the use case can be found on the O-RAN SC wiki: `(RSAC) <https://wiki.o-ran-sc.org/pages/viewpage.action?pageId=20878423>`_ and `(OAM) <https://wiki.o-ran-sc.org/display/OAM/Closed+loop+use+case>`_.

Non-RT RIC provides two implementation versions of the recovery part of the use case. One in the form of a python
script, and one utilizing the ONAP Policy Framework.

Standalone Script Solution
++++++++++++++++++++++++++
The script version consists of a python script that performs the tasks needed for the use case. There are also two
simulators. One message generator that generates alarm messages, and one SDN-R simulator that receives the config
change messages sent from the script and responds with alarm cleared messages to MR.

All parts are Dockerized and can be started as individual containers, in the same network, in Docker.

The script based solution can be found in
this `link <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=test/usecases/oruclosedlooprecovery/scriptversion;b=HEAD>`_.

ONAP Policy Solution
++++++++++++++++++++

There is also another solution for performing the fronthaul recovery that is based on ONAP Policy Framework.
A TOSCA Policy has been created that listens to DMaaP Message Router and sends a configuration change message via
REST call to the OAM controller. The policy based solution can be
found `here <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=test/usecases/oruclosedlooprecovery/apexpolicyversion;b=HEAD>`_.

There is a `docker-compose <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=docker-compose/docker-compose-policy-framework;b=HEAD>`_ available
in the nonrtric repo for bringing up the complete standalone version of ONAP Policy Framework.

The detailed instructions for deploying and running this policy are provided in
the `wiki <https://wiki.o-ran-sc.org/pages/viewpage.action?pageId=35881325>`_.