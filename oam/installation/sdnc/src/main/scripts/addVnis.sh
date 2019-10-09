#!/bin/bash

MYSQL_USER=${MYSQL_USER:-sdnctl}
MYSQL_PWD=${MYSQL_PWD:-gamma}
MYSQL_DB=${MYSQL_DB:-sdnctl}
MYSQL_HOST=${MYSQL_HOST:-dbhost}

start=$1

if [ $# -eq 1 ]
then
  mysql --user=${MYSQL_USER} --password=${MYSQL_PWD} --host ${MYSQL_HOST} ${MYSQL_DB} <<EOF
INSERT INTO VLAN_ID_POOL (purpose, status, vlan_id) VALUES('VNI', 'AVAILABLE', $start);
EOF
elif [ $# -eq 2 ]
then
   stop=$2
   vlanid=$start
   
   while [ $vlanid -le $stop ]
   do
   mysql --user=${MYSQL_USER} --password=${MYSQL_PWD}  --host ${MYSQL_HOST} ${MYSQL_DB} <<EOF
INSERT INTO VLAN_ID_POOL (purpose, status, vlan_id) VALUES( 'VNI', 'AVAILABLE', $vlanid);
EOF
vlanid=$(( vlanid+1 ))
done
else
  echo "Usage: $0 start [stop]"
  exit 1
fi

