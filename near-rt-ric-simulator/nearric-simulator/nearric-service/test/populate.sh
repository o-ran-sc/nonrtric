#!/bin/bash

echo "Usage: populate.sh [<host:port>]"

HOST_PORT="localhost:8080"

if [ $# == 1 ]; then
	echo "Setting host and port from cmd line: "$1
	HOST_PORT=$1
fi

echo "======================================="
echo "Using host and port:" $HOST_PORT
echo "======================================="


PT_MAX=10
PI_MAX=20
PI_ID=0
pt=0
while [ $pt -lt $PT_MAX ]; do
    pi=0
    PATTERN="s/XXXX/${pt}/g"
    sed $PATTERN pt-template.json > .tmp.json
    curl -v -X PUT --header 'Content-Type: application/json' --header 'Accept: */*' -d @.tmp.json 'http://'$HOST_PORT'/a1-p/policytypes/'$pt
    while [ $pi -lt $PI_MAX ]; do  
        echo $pt"--"$pi"-"$PI_ID
        
        PATTERN="s/XXXX/${PI_ID}/g"
        sed $PATTERN pi-template.json > .tmp.json
        curl -v -X PUT --header 'Content-Type: application/json' --header 'Accept: */*' -d @.tmp.json 'http://'$HOST_PORT'/a1-p/policytypes/'$pt'/policies/'$PI_ID
        let pi=pi+1
        let PI_ID=PI_ID+1
    done
    let pt=pt+1
done

curl -v --header 'Accept: application/json'  'http://'$HOST_PORT'/a1-p/policytypes/'
curl -v --header 'Accept: application/json'  'http://'$HOST_PORT'/a1-p/policytypes/1/policies'
