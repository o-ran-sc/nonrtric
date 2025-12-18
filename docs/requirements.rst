.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. SPDX-License-Identifier: CC-BY-4.0
.. Copyright (C) 2021-2023 Nordix Foundation. All rights Reserved
.. Copyright (C) 2023-2025 OpenInfra Foundation Europe. All Rights Reserved

Requirements for the Non-RT RIC project
=======================================

Find detailed description of what Non-RT RIC is on this `page`_.

.. _page: https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/overview

The NONRTRIC project (and the O-RAN Non-RealTime RIC function) can be considered from a number of different viewpoints. This page presents some of these views:

**Scope 1: A1 Controller (Mediator, Endpoint)**

* Southbound: Provide termination point for A1 interface(s) - REST endpoint for messages to/from Near-RealTime RIC
* Northbound: Provide a more generic interface for A1 operations - provide interface to rApps, without the need for A1 message generation, addressing, coordination, mediation, etc.
* O1 interfaces do not terminate in NONRTRIC function (but may terminate in same controller host/instance)

**Scope 2: Coordinate/Host A1 Policy Management Services**

* Map high level RAN goal/intent directives to finely-scoped A1 Policies towards individual Near-RT RIC instances
* Informed by observed RAN context (provided over O1 via OAM functions), and other external context (via other SMO functions)
* Dynamically coordinate life cycles of A1 Policies in individual Near-RT RICs as contexts change

**Scope 3: Coordinate ML/AI Models - In RAN (“E2 nodes” & Near-RT RICs) and NONRTRIC (future)**

* Acts as model-training host
* May act as model-inference host (others: Near-RT RICs, “E2 nodes”)
* Dynamically coordinate ML/AI model lifecycle management (e.g. re-train, re-deploy, etc)
* *This work is now progressed in the* `OSC AIMLFW project <https://lf-o-ran-sc.atlassian.net/wiki/spaces/AIMLFEW/overview>`_.

**Scope 4: Enrichment Data Coordinator**

* Additional context that is unavailable to Near-RT RICs (e.g. RAN data, SMO context, External context)
* Dynamically coordinate access and pass data to appropriate Near-RT RICs (e.g. for use in ML/AI model inference)

**Scope 5: rApp Host & rApp Coordinator**

* Dynamically coordinate rApp lifecycle management
* rApps may act as, or form part of, NONRTRIC- or SMO-level applications
* rApps, via rApp host function, may produce or consume data or services to/from other rApps or SMO services 

**Scope 6: Provide R1 interface for rApps**

* rApps may only consume or produce data or services over the R1 interface (to/from NONRTRIC platform, or to/from SMO platform, or to/from other rApps)
* Exposure & consumption of data or services over R1 must be carefully coordinated by NONRTRIC Platform functions
* Services or data produced or consumed may be "standardized" R1 services or R1 extensions (while other may be proprietary)
