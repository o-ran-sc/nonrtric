.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix

Installation Guide
==================

Abstract
--------

This document describes how to install the Non-RT RIC components, their dependencies and required system resources.

Preface
-------

See the README file in the "docker-compose" folder for more information.

Software Installation and Deployment
------------------------------------

Install with Docker
+++++++++++++++++++

Docker compose files are provided, in the "docker-compose" folder, to install the components. Run the following
command to start the components:

      .. code-block:: bash

         docker-compose -f docker-compose.yaml
           -f policy-service/docker-compose.yaml
           -f ecs/docker-compose.yaml

Install with Helm
+++++++++++++++++

Helm charts and an example recipe are provided in the `it/dep repo <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`__,
under "nonrtric". By modifying the variables named "installXXX" in the beginning of the example recipe file, which
components that will be installed can be controlled. Then the components can be installed and started by running the
following comand:

      .. code-block:: bash

        bin/deploy-nonrtric -f nonrtric/RECIPE_EXAMPLE/example_recipe.yaml
