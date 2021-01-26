.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix

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
 * The A1 Enrichment Information Coordinator Service
 * The R-App Catalogue


A1 Policy Management Service
============================

For information about the A1 Policy Management Service that is implemented in ONAP CCSDK, see `ONAP docs <https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html>`_ and `wiki <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_.

Enrichment Coordinator Service
==============================

See `A1 Enrichment Information Coordination Service API <./ecs-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:

.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5,5

   "A1 Enrichment Information Coordination Service API", ":download:`json <../enrichment-coordinator-service/api/ecs-api.json>`", ":download:`yaml <../enrichment-coordinator-service/api/ecs-api.yaml>`"

R-App Catalogue
===============

The R-App (Non RT-RIC Service) Catalogue provides a way for services to register themselves for other services to discover.

See `R-App Catalogue API <./rac-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:

.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5, 5

   "R-App Catalogue API", ":download:`json <../r-app-catalogue/api/rac-api.json>`", ":download:`yaml <../r-app-catalogue/api/rac-api.yaml>`"


See Also: Non-RT RIC Control Panel
==================================

The Non-RT RIC Control Panel provides a user interface that allows the user to interact with the Non-RT RIC.

Documentation for the Control Panel can be found here: :ref:`Non-RT RIC Control Panel <portal-nonrtric-controlpanel:master_index>`

It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/portal/nonrtric-controlpanel".

See Also: A1 / Near-RT RIC simulator
====================================

The Near-RT RIC simulator simulates an A1 protocol termination endpoint.

Documentation for the simulator can be found here: :ref:`A1 Interface Simulator <sim-a1-interface:a1-interface-simulator>`

It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/sim/a1-interface"
