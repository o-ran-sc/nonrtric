.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2020 Nordix

.. _api_docs:

.. |swagger-icon| image:: ./images/swagger.png
                  :width: 40px

.. |yaml-icon| image:: ./images/yaml_logo.png
                  :width: 40px


========
API-Docs
========

Here we describe the APIs to access the Non-RT RIC functions.

The core Non-RT RIC consists of three parts, described in the sections below:
 * The A1 Policy Management Service
 * The Enrichment Coordinator Service
 * The Service Catalogue


A1 Policy Management Service
============================

For information about the A1 Policy Management Service that is implemented in ONAP, see `ONAP docs <https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html>`_ and `wiki <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_.

Enrichment Coordinator Service
==============================

See `A1 Enrichment Information Coordination Service API <./ecs-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:

.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5,5

   "A1 Enrichment Information Coordination Service API", ":download:`link <../enrichment-coordinator-service/api/ecs-api.json>`", ":download:`link <../enrichment-coordinator-service/api/ecs-api.yaml>`"


Service Catalogue
=================

The Service Catalogue provides a way for services to register themselves for other services to discover.

See `Service Catalogue API <./rac-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:


.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5, 5

   "Service Catalogue API", ":download:`link <../r-app-catalogue/api/rac-api.json>`", ":download:`link <../r-app-catalogue/api/rac-api.yaml>`"

See Also: Non-RT RIC Control Panel
==================================

The Non-RT RIC Control Panel provides a user interface that allows the user to interact with the Non-RT RIC.

Documentation for the Control Panel can be found here: `Non-RT RIC Control Panel <https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel/en/cherry/>`_.

It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/portal/nonrtric-controlpanel".

See Also: A1 / Near-RT RIC simulator
====================================

The Near-RT RIC simulator simulates an A1 protocol termination endpoint.

Documentation for the simulator can be found here: `A1 Interface Simulator <https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface/en/cherry/>`_.

It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/sim/a1-interface"
