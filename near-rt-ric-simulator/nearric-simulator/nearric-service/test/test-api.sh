#!/bin/bash

#Script for basic testing of the A1 simulator API
#Note: policy is reset before test

HOST_PORT="localhost:8080"

echo "Usage: populate.sh [<host:port>]"

HOST_PORT="localhost:8080"

if [ $# == 1 ]; then
	echo "Setting host and port from cmd line: "$1
	HOST_PORT=$1
fi

echo "======================================="
echo "Using host and port:" $HOST_PORT
echo "======================================="

echo "======================================="
echo "Resetting db"
curl 'http://'$HOST_PORT'/reset'
echo "======================================="

#Create a policy type
create_pt() {
    PATTERN="s/XXXX/${1}/g"
    sed $PATTERN pt-template.json > .tmp.json
    res=$(curl -sw "%{http_code}" -X PUT --header 'Content-Type: application/json' --header 'Accept: */*' -d @.tmp.json 'http://'${HOST_PORT}'/a1-p/policytypes/'$1)
	http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
}

get_pt() {
    res=$(curl -sw "%{http_code}" --header 'Accept: application/json'  'http://'${HOST_PORT}'/a1-p/policytypes/'$1)
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
    echo "Response: " ${res:0:${#res}-3}
}

get_pts() {
    res=$(curl -sw "%{http_code}" --header 'Accept: application/json'  'http://'${HOST_PORT}'/a1-p/policytypes/')
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
    echo "Response: " ${res:0:${#res}-3}
}

del_pt() {
    res=$(curl -sw "%{http_code}" -X DELETE --header 'Accept: */*'  'http://'${HOST_PORT}'/a1-p/policytypes/'$1)
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code  
}

get_pis() {
    res=$(curl -sw "%{http_code}" --header 'Accept: application/json'  'http://'${HOST_PORT}'/a1-p/policytypes/'${1}'/policies')
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
    echo "Response: " ${res:0:${#res}-3}
}

create_pi() {
    PATTERN="s/XXXX/${2}/g"
    sed $PATTERN pi-template.json > .tmp.json
    res=$(curl -sw "%{http_code}" -X PUT --header 'Content-Type: application/json' --header 'Accept: */*' -d @.tmp.json 'http://'${HOST_PORT}'/a1-p/policytypes/'$1'/policies/'$2)
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
}

get_pi() {
    res=$(curl -sw "%{http_code}" --header 'Accept: application/json'  'http://'${HOST_PORT}'/a1-p/policytypes/'${1}'/policies/'$2)
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
    echo "Response: " ${res:0:${#res}-3}
}

del_pi() {
    res=$(curl -sw "%{http_code}" -X DELETE --header 'Accept: application/json'  'http://'${HOST_PORT}'/a1-p/policytypes/'${1}'/policies/'$2)
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code 
}

stat_pi() {
    res=$(curl -sw "%{http_code}" --header 'Accept: application/json'  'http://'${HOST_PORT}'/a1-p/policytypes/'${1}'/policies/'$2'/status')
    http_code="${res:${#res}-3}"
    echo "Response code: " $http_code
    echo "Response: " ${res:0:${#res}-3}
}


echo "== Create policy type 23"
create_pt 23
echo "== Get policy type 23"
get_pt 23
echo "== Create policy type 23 again"
create_pt 23
echo "== Create policy type 24"
create_pt 24
echo "== Get all policy types"
get_pts
echo "== Delete policy type 24"
del_pt 24
echo "== Delete policy type 24 again"
del_pt 24
echo "== Get all policy types"
get_pts
echo "== Get all policy instancess for type 23"
get_pis 23
echo "== Create policy instance 16 for type 23"
create_pi 23 16
echo "== Create policy instance 16 for type 23 again"
create_pi 23 16
echo "== Get policy instance 16 for type 23"
get_pi 23 16
echo "== Get missing policy instance 17 for type 23"
get_pi 23 17
echo "== Create policy instance 18 for type 23"
create_pi 23 18
echo "== Get all policy instances for type 23"
get_pis 23
echo "== Delete policy instance 18 for type 23"
del_pi 23 18
echo "== Get all policy instances for type 23"
get_pis 23
echo "== Get status for policy instance 16 for type 23"
stat_pi 23 16
