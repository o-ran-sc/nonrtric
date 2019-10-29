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

RIC Dashboard Configuration and Deployment
==========================================

This documents the configuration and deployment of the O-RAN SC RIC
Dashboard web application, which is often deployed together with the
ONAP Portal.

Configuration
-------------

The application requires the following configuration files::

    application.properties
    key.properties
    portal.properties

In the usual Kubernetes deployment, all file contents are provided by
a configuration map.

Application Properties
^^^^^^^^^^^^^^^^^^^^^^

The file ``application.properties`` must be provided when the
application is launched, either in the current working directory or in
a ``config`` subdirectory (latter is preferred). The Helm chart that
deploys the application should mount this file appropriately.

Many properties have default values cached within the application, in
file ``src/main/resources/application.properties``.  Properties with
default values do NOT need to be repeated in a deployment-specific
configuration.  Properties without default values MUST be specified in
a deployment-specific configuration.

The properties are listed below in alphabetical order.

``a1med.url.prefix``

A1 Mediator URL prefix.  No useful default. Usually a service name
like ``http://ricplt-entry/a1mediator``

``a1med.url.suffix``

A1 Mediator URL suffix. Default is the empty string.

``anrxapp.url.prefix``

ANR Application URL prefix.  No useful default. Usually a service name
like ``http://ricxapp-entry/anr``

``anrxapp.url.suffix``

ANR Application URL suffix. Default is the empty string.

``appmgr.url.prefix``

Application Manager URL prefix. No useful default. Usually a service
name like ``http://ricplt-entry/appmgr``

``appmgr.url.suffix``

Application Manager URL suffix. Default is ``/ric/v1``.

``caasingress.aux.url.prefix``

CAAS-Ingress application URL prefix for the RIC Auxiliary cluster.  No useful default.

``caasingress.aux.url.suffix``

CAAS-Ingress application URL suffix for the RIC Auxiliary cluster. Default is ``api``.

``caasingress.insecure``

Flag whether to disable SSL/TLS certificate and hostname verification.
If true, the dashboard can communicate with a CAAS-Ingress endpoint that
uses self-signed certificates.

``caasingress.plt.url.prefix``

CAAS-Ingress application URL prefix for the RIC Platform cluster.  No useful default.

``caasingress.plt.url.suffix``

CAAS-Ingress application URL suffix for the RIC-PLT cluster. Default is ``api``.

``e2mgr.url.prefix``

E2 Manager URL prefix. No useful default. Usually a service name like
``http://ricplt-entry/e2mgr``

``e2mgr.url.suffix``

E2 Manager URL prefix. Default is ``/v1``.

``mock.config.delay``

Sleep period for mock methods in milliseconds.  This mimics slow
endpoints. Default is ``0``.

``portalapi.appname``

Application name expected at ONAP portal. Default is ``RIC Dashboard``

``portalapi.decryptor``

Java class that decrypts ciphertext from Portal. Default is
``org.oransc.ric.portal.dashboard.portalapi.PortalSdkDecryptorAes``.

``portalapi.password``

REST password expected at ONAP portal. No default value.

``portalapi.security``

Boolean flag whether the Dashboard limits access to users (browsers)
that present security tokens set by the ONAP Portal.  If false, no
access control is performed, which is only appropriate for isolated
lab testing.

``portalapi.usercookie``

Name of request cookie with user ID. Default is ``UserId``.

``portalapi.username``

REST user name expected at ONAP portal. No default value.

``server.port``

Port where the Tomcat server listens for requests. Default is ``8080``.

``metrics.url.ac``

Url to the kibana source which visualizes AC App metrics. No default value and needs to be replaced with actual value during deployment time.

``userfile``

Path of file that stores user details. Default is ``users.json``.


Key Properties
^^^^^^^^^^^^^^

The file ``key.properties`` must be provided on the Java classpath for
the Spring-Boot application, as required by the EPSDK-FW library. The
Helm chart for the application should mount this file appropriately.
A sample file is in directory ``src/test/resources``.

The file must contain the following entries, listed here in
alphabetical order.

``cipher.enc.key``

Encryption key used by the EPSDK-FW library.  No default value.


Portal Properties
^^^^^^^^^^^^^^^^^

The file ``portal.properties`` must be provided on the Java classpath
for the application, as required by the EPSDK-FW library.  The Helm
chart for the application should mount this file appropriately.  A
sample file is in directory ``src/test/resources``.

The file must contain the following entries, listed here in
alphabetical order.

``ecomp_redirect_url``

Portal URL that is reachable by a user's browser.  This is a value
like
``https://portal.api.simpledemo.onap.org:30225/ONAPPORTAL/login.htm``

``ecomp_rest_url``

Portal REST URL that is reachable by the Dashboard back-end. 
This is a value like ``http://portal-app.onap:8989/ONAPPORTAL/auxapi``

``portal.api.impl.class``

Java class name.  No default value.  Value must be
``org.oransc.ric.portal.dashboard.portalapi.PortalRestCentralServiceImpl``

``role_access_centralized``

Selector for role access.  No default value.  Value must be ``remote``.

``ueb_app_key``

Unique key assigned by ONAP Portal to the RIC Dashboard application.
No default value.


Deployment
----------

A production server requires the configuration files listed above.
All files should be placed in a ``config`` directory.  That name is
important; Spring automatically searches that directory for the
``application.properties`` file. Further, that directory can easily be
placed on the Java classpath so the additional files can be found at
runtime.


On-Board Dashboard to ONAP Portal
^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^

When on-boarding the Dashboard to the ONAP Portal the administrator
must supply the following information about the deployed instance:

- Dashboard URL that is reachable by a user's browser. The domain of
  this host name must match the Portal URL that is similarly reachable
  by a user's browser for cookie-based authentication to function as
  expected.  This should be a value like
  ``http://dashboard.simpledemo.onap.org:8080``
- Dashboard REST URL that is reachable by the Portal back-end server.
  This can be a host name or an IP address, because it does not use
  cookie-based authentication.  This must be a URL with suffix "/api/v3"
  for example ``http://192.168.1.1:8080/api/v3``.

The Dashboard server only listens on a single port, so the examples
above both use the same port number.  Different port numbers might be
required if an ingress controller or other proxy server is used.

After the on-boarding process is complete, the administrator must
enter values from the Portal for the following properties explained
above:

- ``portalapi.password``
- ``portalapi.username``
- ``ueb_app_key``

Launch Server
^^^^^^^^^^^^^

After creating, populating and mounting Kubernetes config maps
appropriately, launch the server with this command-line invocation to
include the ``config`` directory on the Java classpath::

    java -cp config:target/ric-dash-be-1.2.0-SNAPSHOT.jar \
        -Dloader.main=org.oransc.ric.portal.dashboard.DashboardApplication \
        org.springframework.boot.loader.PropertiesLauncher

Alternately, to use the configuration in the "application-abc.properties" file,
modify the command to have "spring.config.name=name" like this::

    java -cp config:target/ric-dash-be-1.2.0-SNAPSHOT.jar \
        -Dspring.config.name=application-abc \
        -Dloader.main=org.oransc.ric.portal.dashboard.DashboardApplication \
        org.springframework.boot.loader.PropertiesLauncher
