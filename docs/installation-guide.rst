.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021-2025 Nordix Foundation. All rights Reserved
.. Copyright (C) 2023 OpenInfra Foundation Europe. All Rights Reserved

Installation Guide
==================

Abstract
--------

This document describes how to install some of the Non-RT RIC components, their dependencies and required system resources.

Software Installation and Deployment
------------------------------------

Install with Helm in Kubernetes
+++++++++++++++++++++++++++++++

The easiest and preferred way to install NONRTRIC functions is using Kubernetes, with installation instructions provided in Helm Charts. 
Full details of how to install NONRTRIC functions are provided in `Deploy NONRTRIC in Kubernetes <nonrtricwikik8s_>`_.

Helm charts and an example recipe are provided in the `it/dep repo <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_,
under "nonrtric". By modifying the variables named "installXXX" in the beginning of the example recipe file, which
components that will be installed can be controlled. Then the components can be installed and started by running the
following command:

      .. code-block:: bash

        bin/deploy-nonrtric -f nonrtric/RECIPE_EXAMPLE/example_recipe.yaml

Install with Docker
+++++++++++++++++++

Some NONRTRIC functions, and simpler usecases can be install directly using Docker.  
Full details of how to use Docker for NONRTRIC functions are provided in `Deploy NONRTRIC in Docker <nonrtricwikidocker_>`_.

Install with Docker Compose
+++++++++++++++++++++++++++

Some older docker compose files are provided, in the "docker-compose" folder, to install the components. Run the following
command to start the components:

      .. code-block:: bash

         docker-compose -f docker-compose.yaml
           -f policy-service/docker-compose.yaml
           -f ics/docker-compose.yaml

The example above is just an example to start some of the components.
For more information on running and configuring the functions can be found in the README file in the "`docker-compose <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=docker-compose>`_" folder, and on the `wiki page <https://lf-o-ran-sc.atlassian.net/wiki/spaces/RICNR/pages/86802677/Release+K+-+Run+in+Docker>`_
