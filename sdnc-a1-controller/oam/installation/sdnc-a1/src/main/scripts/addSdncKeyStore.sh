#!/bin/bash

SDNC_HOME=${SDNC_HOME:-/opt/onap/sdnc}

keyStoreFile=${SDNC_HOME}/data/stores/sdnc.p12

if [ ! -f ${keyStoreFile} ]
then
  keytool -genkeypair -dname "CN=SDNC, OU=ONAP, O=ONAP, L=, S=, C=" -alias sdncKey -keyalg RSA -keysize 1024 -keystore $keyStoreFile -storepass adminadmin -storetype pkcs12
fi

