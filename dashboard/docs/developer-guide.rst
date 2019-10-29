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

RIC Dashboard Developer Guide
=============================

This document provides a quickstart for developers of the O-RAN SC RIC Dashboard web
application.

Prerequisites
-------------

1. Java development kit (JDK), version 11 or later
2. Maven dependency-management tool, version 3.4 or later

Other required tools including the Node Package Manager (npm) are fetched dynamically.

Clone and Update Submodules
---------------------------

After cloning the repository, initialize and update all git submodules like this::

    git submodule init
    git submodule update
    
Check the submodule status at any time like this::

    git submodule status


Angular Front-End Application
-----------------------------

The Angular 8 application files are in subdirectory ``webapp-frontend``.
Build the front-end application via ``mvn package``.  For development and debugging,
build the application, then launch an ng development server using this command::

    ./ng serve --proxy-config proxy.conf.json

The app will automatically reload in the browser if you change any of the source files.
The ng server listens for requests at this URL:

    http://localhost:4200


Spring-Boot Back-End Application
--------------------------------

A development (not production) server uses local configuration and serves mock data
that simulates the behavior of remote endpoints.  The back-end server listens for
requests at this URL:

    http://localhost:8080

The directory ``src/test/resources`` contains usable versions of the required property
files.  These steps are required to launch:

1. Set an environment variable via JVM argument: ``-Dorg.oransc.ric.portal.dashboard=mock``
2. Run the JUnit test case ``DashboardServerTest`` which is not exactly a "test" because it never finishes.

Both steps can be done with this command-line invocation::

     mvn -Dorg.oransc.ric.portal.dashboard=mock -Dtest=DashboardTestServer test

Development user authentication
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The development server requires basic HTTP user authentication for all requests. Like
the production server, it requires HTTP headers with authentication for Portal API
requests.  The username and password are stored in constants in this Java class in
the ``src/test/java`` folder::

    org.oransc.ric.portal.dashboard.config.WebSecurityMockConfiguration

Like the production server, the development server also performs role-based
authentication on requests. The user name-role name associations are also defined
in the class shown above.

Production user authentication
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

The server uses the ONAP Portal's "EPSDK-FW" library to support a
single-sign-on (SSO) feature, which requires users to authenticate
at the ONAP Portal UI. The RIC Dashboard can be on-boarded as an 
application on the ONAP Portal using its application on-boarding UI.

The server authenticates requests using cookies that are set
by the ONAP Portal::

     EPService
     UserId

The EPService value is not checked.  The UserId value is decrypted
using a secret key shared with the ONAP Portal to yield a user ID.
That ID must match a user's loginId defined in the user manager.

The regular server checks requests for the following granted
authorities (role names), as defined in the java class ``DashboardConstants``.
A standard user can invoke all "GET" methods but not make changes.
A system administrator can invoke all methods ("GET", "POST", "PUT",
"DELETE") to make arbitrary changes::

    Standard_User
    System_Administrator

Use the following structure in a JSON file to publish a user for the
user manager::

    [
     {
      "orgId":null,
      "managerId":null,
      "firstName":"Demo",
      "middleInitial":null,
      "lastName":"User",
      "phone":null,
      "email":null,
      "hrid":null,
      "orgUserId":null,
      "orgCode":null,
      "orgManagerUserId":null,
      "jobTitle":null,
      "loginId":"demo",
      "active":true,
      "roles":[
         {
            "id":null,
            "name":"Standard_User",
            "roleFunctions":null
         }
      ]
     }
    ]
