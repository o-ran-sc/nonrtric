#!/bin/bash

MYSQL_USER=${MYSQL_USER:-sdnctl}
MYSQL_PWD=${MYSQL_PWD:-gamma}
MYSQL_DB=${MYSQL_DB:-sdnctl}
MYSQL_HOST=${MYSQL_HOST:-dbhost}


if [ $# -ne 2 ]
then
  echo "Usage: $0 table foreign-key"
  exit 1
fi

mysql --user=${MYSQL_USER} --password=${MYSQL_PWD} --host ${MYSQL_HOST} ${MYSQL_DB} <<EOF
ALTER TABLE $1
DROP FOREIGN KEY $2;
EOF
