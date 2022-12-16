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

- Non-RT-RIC Control Panel
- Information Coordinator Service
- A1 Policy Management Service
- A1 Policy Controller / Adapter
- Near-RT RIC A1 Simulator
- Non-RT-RIC (Spring Cloud) Service Gateway
- Non-RT-RIC (Kong) Service Exposure Prototyping
- DMaaP/Kafka Information Producer Adapters
- Initial Non-RT-RIC App Catalogue
- Initial K8S Helm Chart LCM Manager
- Test Framework
- Use Cases

  + "Helloworld" O-RU Fronthaul Recovery use case
  + "Helloworld" O-DU Slice Assurance use case

The source code for "E" Release is in the `NONRTRIC <https://gerrit.o-ran-sc.org/r/admin/repos/nonrtric>`_, `NONRTRIC-ControlPanel <https://gerrit.o-ran-sc.org/r/admin/repos/portal/nonrtric-controlpanel>`_, and `Near-RT-RIC A1-Simulator <https://gerrit.o-ran-sc.org/r/admin/repos/sim/a1-interface>`_ Gerrit source repositories (E-Release Branch).

Non-RT-RIC Control Panel / NONRTRIC Dashboard
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Graphical user interface.

- View and Manage A1 policies in the RAN (near-RT-RICs)
- Graphical A1 policy creation/editing is model-driven, based on policy type's JSON schema
- View and manage producers and jobs for the Information coordinator service
- Configure A1 Policy Management Service (e.g. add/remove near-rt-rics)
- Interacts with the A1-Policy Management Service & Information Coordination Service (REST NBIs) via Service Exposure gateway

Implementation:

- Frontend: Angular framework
- Repo: *portal/nonrtric-controlpanel*
- `Wiki <https://wiki.o-ran-sc.org/display/RICNR/>`_ to set up in your local environment.
- Documentation at the `NONRTRIC-Portal documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-portal-nonrtric-controlpanel>`_.

Information Coordination Service
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The ICS is a data subscription service which decouples data producers from data consumers. A data consumer can create a data subscription (Information Job) without any knowledge of its data producers (one subscription may involve several data producers). A data producer has the ability to produce one or several types of data (Information Type). One type of data can be produced by zero to many producers.

A data consumer can have several active data subscriptions (Information Job). One Information Job consists of the type of data to produce and additional parameters, which may be different for different data types. These parameters are not defined or limited by this service.

Maintains a registry of:
- Information Types / schemas
- Information Producers
- Information Consumers
- Information Jobs

The service is not involved in data delivery and hence does not put restrictions on this.

Implementation:

- Implemented as a Java Spring Boot application.
- Repo: *nonrtric/plt/informationcoordinatorservice*.
- Documentation at the `Information Coordination Service site <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-informationcoordinatorservice/en/latest/>`_

A1 Policy Management Service (from ONAP CCSDK)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A1 Controller Service above A1 Controller/Adapter that provides:

- Unified REST & DMaaP NBI APIs for managing A1 Policies in all near-RT-RICs.

  - Query A1 Policy Types in near-RT-RICs.
  - Create/Query/Update/Delete A1 Policy Instances in near-RT-RICs.
  - Query Status for A1 Policy Instances.

Maintains (persistent) cache of RAN's A1 Policy information.

- Support RAN-wide view of A1 Policy information.
- Streamline A1 traffic.
- Enable (optional) re-synchronization after inconsistencies / near-RT-RIC restarts.
- Supports a large number of near-RT-RICs (& multi-version support).

- Converged ONAP & O-RAN-SC A1 Adapter/Controller functions in ONAP SDNC/CCSDK (Optionally deploy without A1 Adapter to connect direct to near-RT-RICs).
- Support for different Southbound connectors per near-RT-RIC - e.g. different A1 versions, different near-RT-RIC version, different A1 adapter/controllers supports different or proprietary A1 controllers/EMSs.

Implementation:

- Implemented as a Java Spring Boot application.
- Wiki: `A1 Policy Management Service in ONAP <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_ .

Authentification Support
~~~~~~~~~~~~~~~~~~~~~~~~

The auth-token-fetch provides support for authentification.
It is intended to be used as a sidecar and does the authentification procedure, gets and saves the access token
in the local file system. This includes refresh of the token before it expires.
This means that the service only needs to read the token from a file.

It is tested using Keycloak as authentification provider.

.. image:: ./AuthSupport.png
   :width: 500pt

So, a service just needs to read the token file and for instance insert it in the authorization header when using HTTP.
The file needs to be re-read if it has been updated.

The auth-token-fetch is configured by the following environment variables.

* CERT_PATH - the file path of the cert to use for TSL, example: security/tls.crt
* CERT_KEY_PATH - the file path of the private key file for the cert, example: "security/tls.key"
* ROOT_CA_CERTS_PATH - the file path of the trust store.
* CREDS_GRANT_TYPE - the grant_type used for authentification, example: client_credentials
* CREDS_CLIENT_SECRET - the secret/private shared key used for authentification
* CREDS_CLIENT_ID - the client id used for authentification
* OUTPUT_FILE - the path where the fetched authorization token is stored, example: "/tmp/authToken.txt"
* AUTH_SERVICE_URL - the URL to the authentification service (Keycloak)
* REFRESH_MARGIN_SECONDS - how long in advance before the authorization token expires it is refreshed

A1/SDNC Controller & A1 Adapter (Controller plugin)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Mediation point for A1 interface termination in SMO/NONRTRIC.

- Implemented as CCSDK OSGI Feature/Bundles.
- A1 REST southbound.
- RESTCONF Northbound.
- NETCONF YANG > RESTCONF adapter.
- SLI Mapping logic supported.
- Can be included in an any controller based on ONAP CCSDK.

Implementation:

- Repo: *nonrtric/plt/sdnca1controller*
- Wiki: `A1 Adapter/Controller Functions in ONAP <https://wiki.onap.org/pages/viewpage.action?pageId=84672221>`_ .

A1 Interface / Near-RT-RIC Simulator
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Stateful A1 test stub.

- Used to create multiple stateful A1 providers (simulated near-rt-rics).
- Supports A1-Policy and A1-Enrichment Information.
- Swagger-based northbound interface, so easy to change the A1 profile exposed (e.g. A1 version, A1 Policy Types, A1-E1 consumers, etc).
- All A1-AP versions supported.

Implementation:

- Implemented as a Python application.
- Repo: *sim/a1-interface*.
- Documentation at the `A1 Simulator documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-sim-a1-interface>`_

Non-RT-RIC (Spring Cloud) Service Gateway
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
Support Apps to use A1 Services.

- `Spring Cloud Gateway <https://cloud.spring.io/spring-cloud-gateway>`_ provides the library to build a basic API gateway.
- Exposes A1 Policy Management Service & Information Coordinator Service.
- Additional predicates can be added in code or preferably in the Gateway yaml configuration.

Implementation:

- Implemented as a Java Spring Cloud application.
- Repo: *portal/nonrtric-controlpanel*.


Non-RT-RIC (Kong) Service Exposure Prototyping
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Support Apps to use NONRTRIC, SMO and other App interfaces.
A building block for coming releases as the R1 Interface concept matures .

- Support dynamic registration and exposure of service interfaces to Non-RT-RIC applications (& NONRTRIC Control panel).
- Extends a static gateway function specifically for NONRTRIC Control panel (described above).
- Initial version based on `Kong API Gateway <https://docs.konghq.com/gateway-oss>`_ function.
- Initial exposure candidates include A1 (NONRTRIC) services & O1 (OAM/SMO) services.

NONRTRIC Kubernetes deployment - including Kong configurations can be found in the OSC `it/dep <https://gerrit.o-ran-sc.org/r/gitweb?p=it/dep.git;a=tree;f=nonrtric/helm/nonrtric>`_ Gerrit repo.

DMaaP/Kafka Information Producer Adapters
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Configurable mediators to take information from DMaaP and Kafka and present it as a coordinated Information Producer.

These mediators/adapters are generic information producers, which register themselves as information producers of defined information types in Information Coordination Service (ICS).
The information types are defined in a configuration file.
Information jobs defined using ICS then allow information consumers to retrieve data from DMaaP MR or Kafka topics (accessing the ICS API).

There are two alternative implementations to allow Information Consumers to consume DMaaP or Kafka events as coordinated Information Jobs.

Implementation:

- Implementation in Java Spring (DMaaP Adapter), repo: *nonrtric/plt/dmaapadapter*, see `DMaaP Adapter documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-dmaapadapter/en/latest/>`_.
- Implemention in Go (DMaaP Mediator Producer), repo: *nonrtric/plt/dmaapmediatorproducer*, see `DMaaP Mediator Producer documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-dmaapmediatorproducer>`_.

Initial Non-RT-RIC App Catalogue
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Register for Non-RT-RIC Apps.

- Non-RT-RIC Apps can be registered / queried.
- Limited functionality/integration for now.
- *More work required in coming releases as the rApp concept matures*.

Implementation:

- Implemented as a Java Spring Boot application.
- Repo: *nonrtric/plt/rappcatalogue*
- Documentation at the `rApp Catalogue documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-rappcatalogue>`_.

Initial K8S Helm Chart LCM Manager
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Onboard, start, stop, and modify Non-RT-RIC App µServices as Helm Charts.
*A building block for coming releases as the R-APP concept matures*.

- Interfaces that accepts Non-RT-RIC App µServices Helm Charts.
- Support basic LCM operations.
- Onboard, Start, Stop, Modify, Monitor.
- Initial version co-developed with v. similar functions in ONAP.
- *Limited functionality/integration for now*.

Implementation:

- Implemented as a Java Spring Boot application.
- Repo: *nonrtric/plt/helmmanager*
- Documentation at the `Helm Manager documentation site <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-plt-helmmanager>`_.

Test Framework
~~~~~~~~~~~~~~

A full test environment with extensive test cases/scripts can be found in the ``test`` directory in the *nonrtric* source code.

Use Cases
~~~~~~~~~

"Helloworld" O-RU Fronthaul Recovery use case
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A very simplified closed-loop rApp use case to re-establish front-haul connections between O-DUs and O-RUs if they fail. Not intended to to be 'real-world'.

Implementation:

- One version implemented in Python, one in Go as an Information Coordination Service Consumer, and one as an apex policy.
- Repo: *nonrtric/rapp/orufhrecovery*

"Helloworld" O-DU Slice Assurance use case
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

A very simplified closed-loop rApp use case to re-prioritize a RAN slice's radio resource allocation priority if sufficient throughput cannot be maintained. Not intended to to be 'real-world'.

Implementation:

- One version implemented in Go as a micro service, one in Go as an Information Coordination Service Consumer.
- Repo: *nonrtric/rapp/ransliceassurance*
- Documentation at the `O-DU Slice Assurance site <https://docs.o-ran-sc.org/projects/o-ran-sc-nonrtric-rapp-ransliceassurance>`__.
