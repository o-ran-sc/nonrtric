#!/bin/bash

MYSQL_USER=${MYSQL_USER:-sdnctl}
MYSQL_PWD=${MYSQL_PWD:-gamma}
MYSQL_DB=${MYSQL_DB:-sdnctl}
MYSQL_HOST=${MYSQL_HOST:-dbhost}

universe=$1
subnet=$2
start=$3

if [ $# -eq 3 ]
then
  mysql --user=${MYSQL_USER} --password=${MYSQL_PWD} --host=${MYSQL_HOST} ${MYSQL_DB} <<EOF
INSERT INTO IPV4_ADDRESS_POOL VALUES('$aicSiteId', '$universe', 'AVAILABLE', '${subnet}.${start}');
EOF
elif [ $# -eq 4 ]
then
   stop=$4
   ip=$start

   while [ $ip -le $stop ]
   do
   mysql --user=${MYSQL_USER} --password=${MYSQL_PWD} --host=${MYSQL_HOST} ${MYSQL_DB} <<EOF
INSERT INTO IPV4_ADDRESS_POOL VALUES('$aicSiteId', '$universe', 'AVAILABLE','${subnet}.${ip}');
EOF
ip=$(( ip+1 ))
done
else
  echo "Usage: $0 universe subnet start [stop]"
  exit 1
fi

