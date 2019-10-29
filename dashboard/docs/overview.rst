.. ===============LICENSE_START=======================================================
.. O-RAN SC CC-BY-4.0
.. %%
.. Copyright (C) 2019 AT&T Intellectual Property
.. %%
.. Licensed under the Apache License, Version 2.0 (the "License");
.. you may not use this file except in compliance with the License.
.. You may obtain a copy of the License at
..
..      http://www.apache.org/licenses/LICENSE-2.0
..
.. Unless required by applicable law or agreed to in writing, software
.. distributed under the License is distributed on an "AS IS" BASIS,
.. WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
.. See the License for the specific language governing permissions and
.. limitations under the License.
.. ===============LICENSE_END=========================================================

RIC Dashboard Overview
======================

The O-RAN SC RIC Dashboard provides administrative and operator
functions for a radio access network (RAN) controller.  The web app is
built as a single-page app using an Angular (version 8) front end and
a Java (version 11) Spring-Boot (version 2.1) back end.

Project Resources
-----------------

The source code is available from the Linux Foundation Gerrit server:

    `<https://gerrit.o-ran-sc.org/r/portal/ric-dashboard;a=summary>`_

The build (CI) jobs are in the Linux Foundation Jenkins server:

    `<https://jenkins.o-ran-sc.org/view/portal-ric-dashboard>`_

Issues are tracked in the Linux Foundation Jira server:

    `<https://jira.o-ran-sc.org/secure/Dashboard.jspa>`_

Project information is available in the Linux Foundation Wiki:

    `<https://wiki.o-ran-sc.org>`_


A1 Mediator
-----------

The Dashboard interfaces with the A1 Mediator.  This platform
component is accessed via HTTP/REST requests using a client that is
generated from an API specification published by the A1 Mediator team.

The A1 Mediator supports fetching and storing configuration of
applications, which is referred to as getting or setting a policy.
The Dashboard UI provides screens to view and modify configuration
data for such applications.  As of this writing, the only application
that is managed via the A1 Mediator interface is the Admission Control
("AC") application.


Application Manager
-------------------

The Dashboard interfaces with the Application Manager.  This platform
component is accessed via HTTP/REST requests using a client that is
generated from an API specification published by the Application
Manager team.

The Application Manager supports deploying, undeploying and
configuring applications in the RIC. The Dashboard UI provides screens
for these functions.


Automatic Neighbor Relation Application
---------------------------------------

The Dashboard interfaces with the Automatic Neighbor Relation (ANR)
application.  This RIC application is accessed via HTTP/REST requests
using a client that is generated from an API specification published
by the ANR team.

This RIC application discovers and manages the Neighbor Cell Relation
Table (NCRT). The Dashboard UI provides screens to view and modify
NCRT data.


E2 Manager
----------

The Dashboard interfaces with the E2 Manager.  This platform
component is accessed via HTTP/REST requests using a client that is
generated from an API specification published by the E2 Manager team.

The E2 Manager platform component supports connecting and
disconnecting RAN elements.  The Dashboard UI provides controls for
operators to create "ENDC" and "X2" connections, and to disconnect RAN
elements.
