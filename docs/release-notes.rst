.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2023 Nordix. All rights reserved.
.. Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.

=============
Release-Notes
=============

This document provides the release-notes for various parts of the NONRTRIC project.

Prior to release F several functions were homed in this 'nonrtric' repository, and released from here, but now have their own dedicated repos/release-notes. 
Currently the only function released from here is the "Auth Token Fetch" utility image (see below).

Functions, previously here, but now with their own repos have their own release notes

* :doc:`A1 Policy Management Service (ONAP) <onapa1policymanagementservice:releasenotes/release-notes>`.
* :doc:`rApp Management Service <rappmanager:release-notes>`.
* :doc:`Service Management Service (SME) <sme:release-notes>`.
* :doc:`Information Coordinator Service (DME) <informationcoordinatorservice:release-notes>`.
* :doc:`Near-RT RIC A1 Simulator <simulator:release-notes>`.
* DMaaP/Kafka Information Producer Adapters. :doc:`DMaaP Adapter <dmaapadapter:release-notes>`. :doc:`DMaaP Mediator Producer <dmaapmediatorproducer:release-notes>`.
* :doc:`Non-RT-RIC Control Panel <controlpanel:release-notes>`.
* :doc:`Non-RT-RIC App Catalogue <rappcatalogue:release-notes>`.
* :doc:`K8S Helm Chart LCM Manager <helmmanager:release-notes>`.
* :doc:`Topology Exposure & Inventory (TEIV) <teiv:release-notes>`.
* Use Cases: 

  * (See also rApp Manager)
  * :doc:`"Helloworld" O-RU Fronthaul Recovery use case <orufhrecovery:release-notes>`.
  * :doc:`"Helloworld" O-DU Slice Assurance use case <ransliceassurance:release-notes>`.

Release-Notes (Only for the 'nonrtric' repo)
============================================

(*Release notes for other functions/repos can be found using the appropriate links above*)

Bronze
------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/2466f9d370214b578efedd1d3e38b1de17e6ca1c |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | Bronze                                            |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2020-06-18                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Improved stability                                |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

Bronze Maintenance
------------------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/5d4f252a530a0d9abbf2a363354c5e56e8f2f33e |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | Bronze                                            |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2020-07-29                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Introduce configuration of certificates           |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

Cherry
------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/90ce16238dd6970153e1c0fbddb15e32c68c504f |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | Cherry                                            |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2020-12-03                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Introduction of Enrichment Service Coordinator    |
|                             | and rAPP Catalogue                                |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

D
-
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/dd3ebfd784e96919a00ddd745826f8a8e074c66f |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | D                                                 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2021-06-23                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Improvements                                      |
|                             | Introduction of initial version of Helm Manager   |
+-----------------------------+---------------------------------------------------+

D Maintenance
-------------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/973ae56894fb29a929fba9e344cae42e7607087b |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | D                                                 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2021-08-10                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Minor bug fixes                                   |
+-----------------------------+---------------------------------------------------+

E Release
---------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/b472c167413a55a42fc7bfa08d2138f967a204fb |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | E                                                 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2021-12-13                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Improvements and renaming.                        |
|                             | Introduction of more usecase implementations.     |
+-----------------------------+---------------------------------------------------+

E Maintenance Release
---------------------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/4df1f9ca4cd1ebc21e0c5ea57bcb0b7ef096d067 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | E                                                 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2022-02-09                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | Improvements and bug fixes                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

F Release
---------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/46f2c66ed30ceef4cedd7992b88c9563df0f24a5 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | F                                                 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2022-08-18                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | First version of nonrtric-plt-auth-token-fetch    |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

H Release
---------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric/3db8626c0900dc391b8e810541de9761c78043d8 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release designation**     | H                                                 |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Release date**            | 2023-06-16                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Purpose of the delivery** | nonrtric-plt-auth-token-fetch:1.1.1               |
|                             | Updated Springboot version                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

I Release
---------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric                                          |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Note**                    | No new images released from this repo for         |
|                             |     the I Release.                                |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

J Release
---------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric                                          |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Note**                    | No new images released from this repo for         |
|                             |     the J Release.                                |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

K Release
---------
+-----------------------------+---------------------------------------------------+
| **Project**                 | Non-RT RIC                                        |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Repo/commit-ID**          | nonrtric                                          |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+
| **Note**                    | No new images released from this repo for         |
|                             |     the K Release.                                |
|                             |                                                   |
+-----------------------------+---------------------------------------------------+

