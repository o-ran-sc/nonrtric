.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright (C) 2021 Nordix

Installation Guide
==================

Abstract
--------

This document describes how to install the Non-RT RIC components, their dependencies and required system resources.

Software Installation and Deployment
------------------------------------

Install with Docker
+++++++++++++++++++

Docker compose files are provided, in the "docker-compose" folder, to install the components. Run the following
command to start the components:

      .. code-block:: bash

         docker-compose -f docker-compose.yaml
           -f policy-service/docker-compose.yaml
           -f ics/docker-compose.yaml

The example above is just an example to start some of the components.
For more information on running and configuring the functions can be found in the README file in the "`docker-compose <https://gerrit.o-ran-sc.org/r/gitweb?p=nonrtric.git;a=tree;f=docker-compose>`__" folder, and on the `wiki page <https://wiki.o-ran-sc.org/display/RICNR/Release+E+-+Run+in+Docker>`_

Install with Helm
+++++++++++++++++

Helm charts and an example recipe are provided in the `it/dep repo <https://gerrit.o-ran-sc.org/r/admin/repos/it/dep>`_,
under "nonrtric". By modifying the variables named "installXXX" in the beginning of the example recipe file, which
components that will be installed can be controlled. Then the components can be installed and started by running the
following command:

      .. code-block:: bash

        bin/deploy-nonrtric -f nonrtric/RECIPE_EXAMPLE/example_recipe.yaml
