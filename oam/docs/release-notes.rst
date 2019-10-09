.. This work is licensed under a Creative Commons Attribution 4.0 International License.

Release Notes
=============

Version 1.5.4
-------------
:Release Date: 2019-06-13


**New Features**

The full list of Dublin epics and user stories for SDNC maybe be found at <https://jira.onap.org/issues/?filter=11803>.

The following list summarizes some of the most significant epics:

+------------+----------------------------------------------------------------------------+
| Jira #     | Abstract                                                                   |
+============+============================================================================+
| [SDNC-551] | OpenDaylight Fluorine Support                                              |
+------------+----------------------------------------------------------------------------+
| [SDNC-564] | 5G Use Case                                                                |
+------------+----------------------------------------------------------------------------+
| [SDNC-565] | CCVPN Use Case Extension                                                   |
+------------+----------------------------------------------------------------------------+
| [SDNC-570] | SDN-R: Server side component                                               |
+------------+----------------------------------------------------------------------------+
| [SDNC-579] | SDN-R : UX-Client                                                          |
+------------+----------------------------------------------------------------------------+
| [SDNC-631] | SDNC support for the PNF Use Case Network Assign for Plug and Play feature |
+------------+----------------------------------------------------------------------------+


**Bug Fixes**
The full list of bug fixes in the SDNC Dublin release may be found at <https://jira.onap.org/issues/?filter=11805>

**Known Issues**
The full list of known issues in SDNC may be found in the ONAP Jira at <https://jira.onap.org/issues/?filter=11119>

One item of note is that the SDNC admin portal was determined to have a number of security vulnerabilities,
under Known Security Issues.  As a temporary remediation, the admin portal is disabled in
Dublin.  It will be re-enabled in El Alto once the security issues are addressed.

**Security Notes**

*Fixed Security Issues*

- CVE-2019-12132 `OJSI-41 <https://jira.onap.org/browse/OJSI-41>`_ SDNC service allows for arbitrary code execution in sla/dgUpload form
  Fixed temporarily by disabling admportal
- CVE-2019-12123 `OJSI-42 <https://jira.onap.org/browse/OJSI-42>`_ SDNC service allows for arbitrary code execution in sla/printAsXml form
  Fixed temporarily by disabling admportal
- CVE-2019-12113 `OJSI-43 <https://jira.onap.org/browse/OJSI-43>`_ SDNC service allows for arbitrary code execution in sla/printAsGv form
  Fixed temporarily by disabling admportal
- `OJSI-91 <https://jira.onap.org/browse/OJSI-91>`_ SDNC exposes unprotected API for user creation
  Fixed temporarily by disabling admportal
- `OJSI-98 <https://jira.onap.org/browse/OJSI-98>`_ In default deployment SDNC (sdnc-portal) exposes HTTP port 30201 outside of cluster.
  Fixed temporarily by disabling admportal
- CVE-2019-12112 `OJSI-199 <https://jira.onap.org/browse/OJSI-199>`_ SDNC service allows for arbitrary code execution in sla/upload form
  Fixed temporarily by disabling admportal

*Known Security Issues*

- `OJSI-34 <https://jira.onap.org/browse/OJSI-34>`_ Multiple SQL Injection issues in SDNC
- `OJSI-99 <https://jira.onap.org/browse/OJSI-99>`_ In default deployment SDNC (sdnc) exposes HTTP port 30202 outside of cluster.
- `OJSI-100 <https://jira.onap.org/browse/OJSI-100>`_ In default deployment SDNC (sdnc-dgbuilder) exposes HTTP port 30203 outside of cluster.
- `OJSI-179 <https://jira.onap.org/browse/OJSI-179>`_ dev-sdnc-sdnc exposes JDWP on port 1830 which allows for arbitrary code execution
- `OJSI-183 <https://jira.onap.org/browse/OJSI-183>`_ SDNC exposes ssh service on port 30208

*Known Vulnerabilities in Used Modules*

Quick Links:

- `SDNC project page <https://wiki.onap.org/display/DW/Software+Defined+Network+Controller+Project>`_
- `Passing Badge information for SDNC <https://bestpractices.coreinfrastructure.org/en/projects/1703>`_
- `Project Vulnerability Review Table for Casablanca Release <https://wiki.onap.org/pages/viewpage.action?pageId=45307811>`_

Version: 1.4.4
--------------

**Bugs Fixes**

The following bugs are fixed in the SDNC Casablanca January 2019 maintenance release:

+------------+------------------------------------------------------------------------------------------+
| Jira #     | Abstract                                                                                 |
+============+==========================================================================================+
| [SDNC-405] | SDNC API documentation is missing on ReadTheDocs                                         |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-523] | vnf-information.vnf-id validation check should not be mandatory in validate-vnf-input DG |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-532] | oof query failed due to hostname change, returning unknown host                          |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-534] | wrong "input" field in DMaaP message template                                            |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-536] | Upgrade zjsonpatch version to remediate vulnerabilities                                  |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-537] | Update to spring-boot 2.1.0-RELEASE                                                      |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-540] | CCVPN closed loop testing failed.                                                        |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-542] | [PORT] Network Discovery microservice does not log                                       |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-546] | CCVPN bugs fix for manual free integration test                                          |
+------------+------------------------------------------------------------------------------------------+
| [SDNC-549] | Retain MD-SAL data on pod recreate                                                       |
+------------+------------------------------------------------------------------------------------------+



Version: 1.4.3
--------------


:Release Date: 2018-11-30

**New Features**

The Casablanca release of SDNC introduces the following new features:

	- Network Discovery, in support of POMBA
	- Support for CCVPN use case
	- Change Management enhancements

**Bug Fixes**

The list of bugs fixed in the SDNC Casablanca release may be found in the ONAP Jira at <https://jira.onap.org/issues/?filter=11544>


**Known Issues**

The list of known issues in the SDNC project may be found in the ONAP Jira at <https://jira.onap.org/issues/?filter=11119>


**Security Notes**

SDNC code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been addressed, items that remain open have been assessed for risk and determined to be false positive. The SDNC open Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=45307811>`_.

Quick Links:

- `SDNC project page <https://wiki.onap.org/display/DW/Software+Defined+Network+Controller+Project>`_
- `Passing Badge information for SDNC <https://bestpractices.coreinfrastructure.org/en/projects/1703>`_
- `Project Vulnerability Review Table for Casablanca Release <https://wiki.onap.org/pages/viewpage.action?pageId=45307811>`_

**Upgrade Notes**
   NA

**Deprecation Notes**
   NA

**Other**
   NA

Version: 1.3.4
--------------


:Release Date: 2018-07-06

**New Features**

The full list of SDNC Beijing Epics and user stories can be found in the ONAP Jira at <https://jira.onap.org/issues/?filter=10791>.  The
following table lists the major features included in the Beijing release.

+------------+-------------------------------------------------------------------------------------------------------------+
| Jira #     | Abstract                                                                                                    |
+============+=============================================================================================================+
| [SDNC-278] | Change management in-place software upgrade execution using Ansible <https://jira.onap.org/browse/SDNC-278> |
+------------+-------------------------------------------------------------------------------------------------------------+
| [SDNC-163] | Deploy a SDN-C high availability environment - Kubernetes <https://jira.onap.org/browse/SDNC-163>           |
+------------+-------------------------------------------------------------------------------------------------------------+


**Bug Fixes**

The list of bugs fixed in the SDNC Beijing release may be found in the ONAP Jira at <https://jira.onap.org/issues/?filter=11118>


**Known Issues**

+------------+----------------------------------------------------------------------------------------------------------------------------------+
| Jira #     | Abstract                                                                                                                         |
+============+==================================================================================================================================+
| [SDNC-324] | IPV4_ADDRESS_POOL is empty <https://jira.onap.org/browse/SDNC-324>                                                               |
+------------+----------------------------------------------------------------------------------------------------------------------------------+
| [SDNC-321] | dgbuilder won't save DG <https://jira.onap.org/browse/SDNC-321>                                                                  |
+------------+----------------------------------------------------------------------------------------------------------------------------------+
| [SDNC-304] | SDNC OOM intermittent Healthcheck failure - JSONDecodeError - on different startup order <https://jira.onap.org/browse/SDNC-304> |
+------------+----------------------------------------------------------------------------------------------------------------------------------+
| [SDNC-115] | VNFAPI DGs contain plugin references to software not part of ONAP <https://jira.onap.org/browse/SDNC-115>                        |
+------------+----------------------------------------------------------------------------------------------------------------------------------+
| [SDNC-114] | Generic API DGs contain plugin references to software not part of ONAP <https://jira.onap.org/browse/SDNC-114>                   |
+------------+----------------------------------------------------------------------------------------------------------------------------------+
| [SDNC-106] | VNFAPI DGs contain old openecomp and com.att based plugin references <https://jira.onap.org/browse/SDNC-106>                     |
+------------+----------------------------------------------------------------------------------------------------------------------------------+
| [SDNC-64]  | SDNC is not setting FromApp identifier in logging MDC <https://jira.onap.org/browse/SDNC-64>                                     |
+------------+----------------------------------------------------------------------------------------------------------------------------------+


**Security Notes**

SDNC code has been formally scanned during build time using NexusIQ and all Critical vulnerabilities have been addressed, items that remain open have been assessed for risk and determined to be false positive. The SDNC open Critical security vulnerabilities and their risk assessment have been documented as part of the `project <https://wiki.onap.org/pages/viewpage.action?pageId=28379582>`_.

Quick Links:

- `SDNC project page <https://wiki.onap.org/display/DW/Software+Defined+Network+Controller+Project>`_
- `Passing Badge information for SDNC <https://bestpractices.coreinfrastructure.org/en/projects/1703>`_
- `Project Vulnerability Review Table for SDNC <https://wiki.onap.org/pages/viewpage.action?pageId=28379582>`_

**Upgrade Notes**
	NA

**Deprecation Notes**
	NA

**Other**
	NA

Version: 1.2.1
--------------

:Release Date: 2018-01-18

**Bug Fixes**

- `SDNC-145 <https://jira.onap.org/browse/SDNC-145>`_ Error message refers to wrong parameters
- `SDNC-195 <https://jira.onap.org/browse/SDNC-195>`_ UEB listener doesn't insert correct parameters for allotted resources in DB table ALLOTTED_RESOURCE_MODEL
- `SDNC-198 <https://jira.onap.org/browse/SDNC-198>`_ CSIT job fails
- `SDNC-201 <https://jira.onap.org/browse/SDNC-201>`_ Fix DG bugs from integration tests
- `SDNC-202 <https://jira.onap.org/browse/SDNC-202>`_ Search for service -data null match, set vGW LAN IP via Heat
- `SDNC-211 <https://jira.onap.org/browse/SDNC-211>`_ Update SDNC Amsterdam branch to use maintenance release versions
- `SDNC-212 <https://jira.onap.org/browse/SDNC-212>`_ Duplicate file name

Version: 1.2.0
--------------

:Release Date: 2017-11-16

**New Features**

The ONAP Amsterdam release introduces the following changes to SDNC from
the original openECOMP seed code:
   - Refactored / moved common platform code to new CCSDK project
   - Refactored code to rename openecomp to onap
   - Introduced new GENERIC-RESOURCE-API api, used by vCPE and VoLTE use cases
   - Introduced new docker containers for SDC and DMAAP interfaces

**Bug Fixes**
	NA
**Known Issues**
The following known high priority issues are being worked and are expected to be delivered
in release 1.2.1:
- `SDNC-179 <https://jira.onap.org/browse/SDNC-179>`_ Failed to make HTTPS connection in restapicall node
- `SDNC-181 <https://jira.onap.org/browse/SDNC-181>`_ Change call to brg-wan-ip-address vbrg-wan-ip brg topo activate DG
- `SDNC-182 <https://jira.onap.org/browse/SDNC-182>`_ Fix VNI Consistency: Add vG vxlan tunnel setup and bridge domain setup to brg-topo-activate DG

**Security Issues**
	NA

**Upgrade Notes**
	NA

**Deprecation Notes**
	NA

**Other**
	NA
