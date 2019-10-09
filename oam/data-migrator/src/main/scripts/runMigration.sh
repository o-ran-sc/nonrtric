#!/bin/bash

###
# ============LICENSE_START=======================================================
# openECOMP : SDN-C
# ================================================================================
# Copyright (C) 2019 AMDOCS
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END=========================================================
###

PROPERTY_DIR=${PROPERTY_DIR:-/opt/onap/sdnc/data/properties}
MIGRATION=data-migrator
MIGRATION_ROOT=${MIGRATION_ROOT:-/opt/onap/sdnc/data-migrator}
JAVA_HOME=${JAVA_HOME:-/usr/lib/jvm/java-8-oracle}
JAVA_OPTS=${JAVA_OPTS:--Dhttps.protocols=TLSv1.1,TLSv1.2}
JAVA=${JAVA:-${JAVA_HOME}/bin/java}

# Redirect output from script to MIGRATION.out
exec >> ${MIGRATION_ROOT}/logs/$MIGRATION.out
exec 2>&1

if [ ! -d ${MIGRATION_ROOT}/logs ]
then
  mkdir ${MIGRATION_ROOT}/logs
fi

for file in ${MIGRATION_ROOT}/lib/*.jar
do
  CLASSPATH=$CLASSPATH:$file
done

${JAVA} ${JAVA_OPTS} -Dlog4j.configuration=file:${MIGRATION_ROOT}/properties/log4j.properties -cp ${CLASSPATH} org.onap.sdnc.oam.datamigrator.DataMigration $@

echo $! 

exit 0
