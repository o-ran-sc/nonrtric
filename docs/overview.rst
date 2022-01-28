.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2021 Nordix

.. |archpic| image:: ./images/nonrtric-architecture-E.png
  :alt: Image: O-RAN SC - NONRTRIC Overall Architecture

Summary
-------

The Non-RealTime RIC (RAN Intelligent Controller) is an Orchestration and Automation function described by the O-RAN Alliance for non-real-time intelligent management of RAN (Radio Access Network) functions.

The primary goal of the Non-RealTime RIC is to support non-real-time radio resource management, higher layer procedure optimization, policy optimization in RAN, and providing guidance, parameters, policies and AI/ML models to support the operation of near-RealTime RIC functions in the RAN to achieve higher-level non-real-time objectives.

Non-RealTime RIC functions include service and policy management, RAN analytics and model-training for the near-RealTime RICs.
The Non-RealTime RIC platform hosts and coordinates rApps (Non-RT RIC applications) to perform Non-RealTime RIC tasks.
The Non-RealTime RIC also hosts the new R1 interface (between rApps and SMO/Non-RealTime-RIC services).

The O-RAN-SC (OSC) NONRTRIC project provides concepts, architecture and reference implementations as defined and described by the `O-RAN Alliance <https://www.o-ran.org>`_ architecture.
The OSC NONRTRIC implementation communicates with near-RealTime RIC elements in the RAN via the A1 interface. Using the A1 interface the NONRTRIC will facilitate the provision of policies for individual UEs or groups of UEs; monitor and provide basic feedback on policy state from near-RealTime RICs; provide enrichment information as required by near-RealTime RICs; and facilitate ML model training, distribution and inference in cooperation with the near-RealTime RICs.

|archpic|

Find detailed description of the NONRTRIC project see the `O-RAN SC NONRTRIC Project Wiki <https://wiki.o-ran-sc.org/display/RICNR/>`_.

NONRTRIC components
-------------------

These are the components that make up the Non-RT-RIC:

* Non-RT-RIC Control Panel
* Information Coordinator Service
* A1 Policy Management Service
* A1 Policy Controller / Adapter
* Near-RT RIC A1 Simulator
* Non-RT-RIC (Spring Cloud) Service Gateway
* Non-RT-RIC (Kong) Service Exposure Prototyping
* DMaaP/Kafka Information Producer Adapters
* Initial Non-RT-RIC App Catalogue
* Initial K8S Helm Chart LCM Manager
* Test Framework
* Use Cases

  + "Helloworld" O-RU Fronthaul Recovery use case
  + "Helloworld" O-DU Slice Assurance use case

The source code for "E" Release is in the `NONRTRIC <https://gerrit.o-ran-sc.org/r/admin/repos/nonrtric>`_, `NONRTRIC-ControlPanel <https://gerrit.o-ran-sc.org/r/admin/repos/portal/nonrtric-controlpanel>`_, and `Near-RT-RIC A1-Simulator <https://gerrit.o-ran-sc.org/r/admin/repos/sim/a1-interface>`_ Gerrit source repositories (E-Release Branch).

Non-RT-RIC Control Panel / NONRTRIC Dashboard
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Graphical user interface.

* View and Manage A1 policies in the RAN (near-RT-RICs)
* Graphical A1 policy creation/editing is model-driven, based on policy type's JSON schema
* View and manage producers and jobs for the Information coordinator service
* Configure A1 Policy Management Service (e.g. add/remove near-rt-rics)
* Interacts with the A1-Policy Management Service & Information Coordination Service (REST NBIs) via Service Exposure gateway

Implementation:

* Frontend: Angular framework
* Repo: *portal/nonrtric-controlpanel*

Please refer the developer guide and the `Wiki <https://wiki.o-ran-sc.org/display/RICNR/>`_ to set up in your local environment.

More details available at the `NONRTRIC-Portal documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel>`_.

Information Coordination Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Coordinate/Register Information Types, Producers, Consumers, and Jobs.

Coordinate/Register A1-EI Types, Producers, Consumers, and Jobs (A1 Enrichment Information Job Coordination).

* Maintains a registry of:

  + Information Types / schemas
  + Information Producers
  + Information Consumers
  + Information Jobs

* Information Query API (e.g. per producer, per consumer, per types).
* Query status of Information jobs.
* After Information-type/Producer/Consumer/Job is successfully registered delivery/flow can happen directly between Information Producers and Information Consumers.
* The Information Coordinator Service natively supports the O-RAN A1 Enrichment Information (A1-EI) interface, supporting coordination A1-EI Jobs where information (A1-EI)flow from the SMO/Non-RT-RIC/rApps to near-RT-RICs over the A1 interface.

Implementation:

* Implemented as a Java Spring Boot application.

Information Coordination Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Coordinate/Register Information Types, Producers, Consumers, and Jobs.

Coordinate/Register A1-EI Types, Producers, Consumers, and Jobs (A1 Enrichment Information Job Coordination).

* Maintains a registry of:

  + Information Types / schemas
  + Information Producers
  + Information Consumers
  + Information Jobs

* Information Query API (e.g. per producer, per consumer, per types)
* Query status of Information jobs
* After Information-type/Producer/Consumer/Job is successfully registered delivery/flow can happen directly between Information Producers and Information Consumers
* The Information Coordinator Service natively supports the O-RAN A1 Enrichment Information (A1-EI) interface, supporting coordination A1-EI Jobs where information (A1-EI)flow from the SMO/Non-RT-RIC/rApps to near-RT-RICs over the A1 interface.

Implementation:

* Implemented as a Java Spring Boot application

A1 Policy Management Service (from ONAP CCSDK)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A1 Controller Service above A1 Controller/Adaptor that provides:

* Unified REST & DMaaP NBI APIs for managing A1 Policies in all near-RT-RICs.

  + Query A1 Policy Types in near-RT-RICs.
  + Create/Query/Update/Delete A1 Policy Instances in near-RT-RICs.
  + Query Status for A1 Policy Instances.

* Maintains (persistent) cache of RAN's A1 Policy information.

  * Support RAN-wide view of A1 Policy information.
  * Streamline A1 traffic.
  * Enable (optional) re-synchronization after inconsistencies / near-RT-RIC restarts.
  * Supports a large number of near-RT-RICs (& multi-version support).

* Converged ONAP & O-RAN-SC A1 Adapter/Controller functions in ONAP SDNC/CCSDK (Optionally deploy without A1 Adaptor to connect direct to near-RT-RICs).
* Support for different Southbound connectors per near-RT-RIC - e.g. different A1 versions, different near-RT-RIC version, different A1 adapter/controllers supports different or proprietary A1 controllers/EMSs.

See also: `A1 Policy Management Service in ONAP <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_ .

Implementation:

* Implemented as a Java Spring Boot application.

A1/SDNC Controller & A1 Adapter (Controller plugin)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Mediation point for A1 interface termination in SMO/NONRTRIC.

* Implemented as CCSDK OSGI Feature/Bundles.
* A1 REST southbound.
* RESTCONF Northbound.
* NETCONF YANG > RESTCONF adapter.
* SLI Mapping logic supported.
* Can be included in an any controller based on ONAP CCSDK.
See also: `A1 Adapter/Controller Functions in ONAP <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_ .
A1 Interface / Near-RT-RIC Simulator
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Stateful A1 test stub.

* Used to create multiple stateful A1 providers (simulated near-rt-rics).
* Supports A1-Policy and A1-Enrichment Information.
* Swagger-based northbound interface, so easy to change the A1 profile exposed (e.g. A1 version, A1 Policy Types, A1-E1 consumers, etc).
* All A1-AP versions supported.

Implementation:

* Implemented as a Python application.
* Repo: *sim/a1-interface*.

More details available at the `A1 Simulator documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface>`_

Non-RT-RIC (Spring Cloud) Service Gateway
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Support Apps to use A1 Services.

* `Spring Cloud Gateway <https://cloud.spring.io/spring-cloud-gateway>`_ provides the library to build a basic API gateway.
* Exposes A1 Policy Management Service & Information Coordinator Service.
* Additional predicates can be added in code or preferably in the Gateway yaml configuration.

Implementation:

* Implemented as a Java Spring Cloud application.
* Repo: *portal/nonrtric-controlpanel*.


Non-RT-RIC (Kong) Service Exposure Prototyping
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Support Apps to use NONRTRIC, SMO and other App interfaces.
A building block for coming releases as the R1 Interface concept matures .

* Support dynamic registration and exposure of service interfaces to Non-RT-RIC applications (& NONRTRIC Control panel).
* Extends a static gateway function specifically for NONRTRIC Control panel (described above).
* Initial version based on `Kong API Gateway <https://docs.konghq.com/gateway-oss>`_ function.
* Initial exposure candidates include A1 (NONRTRIC) services & O1 (OAM/SMO) services.

NONRTRIC Kubernetes deployment - including Kong configurations can be found in the OSC `it/dep <https://gerrit.o-ran-sc.org/r/gitweb?p=it/dep.git;a=tree;f=nonrtric/helm/nonrtric>`_ Gerrit repo.

DMaaP/Kafka Information Producer Adapters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configurable mediators to take information from DMaaP and Kafka and present it as a coordinated Information Producer.

These mediators/adapters are generic information producers, which register themselves as information producers of defined information types in Information Coordination Service (ICS).
The information types are defined in a configuration file.
Information jobs defined using ICS then allow information consumers to retrieve data from DMaaP MR or Kafka topics (accessing the ICS API).

There are two alternative implementations to allow Information Consumers to consume DMaaP or Kafka events as coordinated Information Jobs.

1. A version implemented in Java Spring (DMaaP Adaptor Service).
2. A version implemented in Go (DMaaP Mediator Producer).
1. A version implemented in Java (Spring) - Supporting DMaaP and Kafka mediation
2. A version implemented in Go - Supporting DMaaP mediation 

Initial Non-RT-RIC App Catalogue
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Register for Non-RT-RIC Apps.

* Non-RT-RIC Apps can be registered / queried.
* Limited functionality/integration for now.
* *More work required in coming releases as the rApp concept matures*.

Initial K8S Helm Chart LCM Manager
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Onboard, start, stop, and modify Non-RT-RIC App µServices as Helm Charts.
*A building block for coming releases as the R-APP concept matures*.

* Interfaces that accepts Non-RT-RIC App µServices Helm Charts.
* Support basic LCM operations.
* Onboard, Start, Stop, Modify, Monitor.
* Initial version co-developed with v. similar functions in ONAP.
* *Limited functionality/integration for now*.

Test Framework
~~~~~~~~~~~~~~

A full test environment with extensive test cases/scripts can be found in the ``test`` directory in the *nonrtric* source code.

Use Cases
~~~~~~~~~

"Helloworld" O-RU Fronthaul Recovery use case
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A very simplified closed-loop rApp use case to re-establish front-haul connections between O-DUs and O-RUs if they fail. Not intended to to be 'real-world'.

"Helloworld" O-DU Slice Assurance use case
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A very simplified closed-loop rApp use case to re-prioritize a RAN slice's radio resource allocation priority if sufficient throughput cannot be maintained. Not intended to to be 'real-world'.
