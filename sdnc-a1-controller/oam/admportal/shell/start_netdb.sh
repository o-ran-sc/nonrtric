#!/bin/bash

. /etc/attappl.env
. ${PROJECT_HOME}/etc/default.env
. ${PROJECT_HOME}/siteconfig/siteconf.info

# only run on primary adm vm
# ${Rank} comes from siteconf.info
if [ ${Rank} == '1' ]; then
        cd /opt/admportal/server
        node ./netdb_updater.js -t ${1} -d ${2} >/dev/null 2>&1
fi
