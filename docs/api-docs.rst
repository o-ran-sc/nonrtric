.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2020 Nordix

.. _api_docs:


========
API-Docs
========

This is the API-docs of Non-RT RIC.

The Non-RT RIC consists of three parts, described in the sections below:
 * The Policy Agent
 * The A1 Controller
 * The rAPP Catalogue


Policy Agent & A1 Controller
============================

For information about the The Policy Agent and A1 Controller that are implemented in ONAP, see `readthedocs`_ and `wiki`_.

.. _readthedocs: https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html
.. _wiki: https://wiki.onap.org/pages/viewpage.action?pageId=84644984

rAPP Catalogue
==============

The Non RT-RIC Service Catalogue provides a way for services to register themselves for other services to discover.

See `RAC API <./rac-api.html>`_ for how to use the API.

.. |swagger-icon| image:: ./images/swagger.png
                  :width: 40px

.. |yaml-icon| image:: ./images/yaml_logo.png
                  :width: 40px


.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5, 5

   "RAC API", ":download:`link <../r-app-catalogue/api/rac-api.json>`", ":download:`link <../r-app-catalogue/api/rac-api.yaml>`"

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
