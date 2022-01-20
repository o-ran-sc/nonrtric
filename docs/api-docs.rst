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

The core Non-RT RIC consists of several parts, with available APIs described in the sections below:

* The A1 Policy Management Service
* The Information Coordinator Service
* DMaaP Adaptor
* The Non-RT-RIC App Catalogue
* K8S Helm Chart LCM Manager (Initial) **<ToDo>**

A1 Policy Management Service
============================

For information about the A1 Policy Management Service that is implemented in ONAP, see `ONAP docs <https://docs.onap.org/projects/onap-ccsdk-oran/en/latest/index.html>`_ and `wiki <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_.

Information Coordinator Service
===============================

See `A1 Information Information Coordination Service API <./ics-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:

.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5,5

   "A1 Information Coordination Service API", ":download:`link <../information-coordinator-service/api/ics-api.json>`", ":download:`link <../information-coordinator-service/api/ics-api.yaml>`"

DMaaP Adaptor
=============

The DMaaP Adaptor provides support for push delivery of any data received from DMaaP or Kafka.

See `DMaaP Adaptor API <./dmaap-adaptor-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:


.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5, 5

   "DMaaP Adaptor API", ":download:`link <../dmaap-adaptor-java/api/api.json>`", ":download:`link <../dmaap-adaptor-java/api/api.yaml>`"

DMaaP Mediator Producer
=======================

The DMaaP Mediator Producer provides support for push delivery of any data received from DMaaP or Kafka.

See `DMaaP Mediator Producer API <./dmaap-mediator-producer-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:


.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5, 5

   "DMaaP Mediator Producer API", ":download:`link <../dmaap-mediator-producer/api/swagger.json>`", ":download:`link <../dmaap-mediator-producer/api/swagger.yaml>`"

Non-RT-RIC App Catalogue (Initial)
==================================

The Service Catalogue provides a way for services to register themselves for other services to discover.

See `Service Catalogue API <./rac-api.html>`_ for full details of the API.

The API is also described in Swagger-JSON and YAML:


.. csv-table::
   :header: "API name", "|swagger-icon|", "|yaml-icon|"
   :widths: 10,5, 5

   "Service Catalogue API", ":download:`link <../r-app-catalogue/api/rac-api.json>`", ":download:`link <../r-app-catalogue/api/rac-api.yaml>`"

K8S Helm Chart LCM Manager (Initial)
====================================

**<ToDo>**

See Also: Non-RT RIC Control Panel
==================================

The Non-RT RIC Control Panel provides a user interface that allows the user to interact with the Non-RT RIC.

Documentation for the Control Panel can be found here: `Non-RT RIC Control Panel <https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel/en/latest/>`_.

It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/portal/nonrtric-controlpanel".

See Also: A1 / Near-RT RIC simulator
====================================

The Near-RT RIC simulator simulates an A1 protocol termination endpoint.

Documentation for the simulator can be found here: `A1 Interface Simulator <https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface/en/latest/>`_.

It can be downloaded from here: ::

  git clone "https://gerrit.o-ran-sc.org/r/sim/a1-interface"
