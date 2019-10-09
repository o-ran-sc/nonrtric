#!/bin/bash

ODL_HOME=${ODL_HOME:-/opt/opendaylight/current}
ODL_KARAF_CLIENT=${ODL_KARAF_CLIENT:-${ODL_HOME}/bin/client}
INSTALLERDIR=$(dirname $0)

REPOZIP=${INSTALLERDIR}/${features.boot}-${project.version}.zip

if [ -f ${REPOZIP} ]
then
	unzip -d ${ODL_HOME} ${REPOZIP}
else
	echo "ERROR : repo zip ($REPOZIP) not found"
	exit 1
fi

${ODL_KARAF_CLIENT} feature:repo-add ${features.repositories}
${ODL_KARAF_CLIENT} feature:install ${features.boot}
