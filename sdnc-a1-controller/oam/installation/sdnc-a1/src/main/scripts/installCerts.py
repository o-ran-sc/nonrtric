# ============LICENSE_START=======================================================
#  Copyright (C) 2019 Nordix Foundation.
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
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#


# coding=utf-8
import os
import httplib
import base64
import time
import zipfile
import shutil

Path = "/tmp"

zipFileList = []

username = os.environ['ODL_ADMIN_USERNAME']
password = os.environ['ODL_ADMIN_PASSWORD']
TIMEOUT=1000
INTERVAL=30
timePassed=0

postKeystore= "/restconf/operations/netconf-keystore:add-keystore-entry"
postPrivateKey= "/restconf/operations/netconf-keystore:add-private-key"
postTrustedCertificate= "/restconf/operations/netconf-keystore:add-trusted-certificate"


headers = {'Authorization':'Basic %s' % base64.b64encode(username + ":" + password),
           'X-FromAppId': 'csit-sdnc',
           'X-TransactionId': 'csit-sdnc',
           'Accept':"application/json",
           'Content-type':"application/json"}

def readFile(folder, file):
    key = open(Path + "/" + folder + "/" + file, "r")
    fileRead = key.read()
    key.close()
    fileRead = "\n".join(fileRead.splitlines()[1:-1])
    return fileRead

def readTrustedCertificate(folder, file):
    listCert = list()
    caPem = ""
    startCa = False
    key = open(Path + "/" + folder + "/" + file, "r")
    lines = key.readlines()
    for line in lines:
        if not "BEGIN CERTIFICATE" in line and not "END CERTIFICATE" in line and startCa:
            caPem += line
        elif "BEGIN CERTIFICATE" in line:
            startCa = True
        elif "END CERTIFICATE" in line:
            startCa = False
            listCert.append(caPem)
            caPem = ""
    return listCert

def makeKeystoreKey(clientKey, count):
    odl_private_key="ODL_private_key_%d" %count

    json_keystore_key='{{\"input\": {{ \"key-credential\": {{\"key-id\": \"{odl_private_key}\", \"private-key\" : ' \
                      '\"{clientKey}\",\"passphrase\" : \"\"}}}}}}'.format(
        odl_private_key=odl_private_key,
        clientKey=clientKey)

    return json_keystore_key



def makePrivateKey(clientKey, clientCrt, certList, count):
    caPem = ""
    for cert in certList:
        caPem += '\"%s\",' % cert

    caPem = caPem.rsplit(',', 1)[0]
    odl_private_key="ODL_private_key_%d" %count

    json_private_key='{{\"input\": {{ \"private-key\":{{\"name\": \"{odl_private_key}\", \"data\" : ' \
                     '\"{clientKey}\",\"certificate-chain\":[\"{clientCrt}\",{caPem}]}}}}}}'.format(
        odl_private_key=odl_private_key,
        clientKey=clientKey,
        clientCrt=clientCrt,
        caPem=caPem)

    return json_private_key

def makeTrustedCertificate(certList, count):
    number = 0
    json_cert_format = ""
    for cert in certList:
        cert_name = "xNF_CA_certificate_%d_%d" %(count, number)
        json_cert_format += '{{\"name\": \"{trusted_name}\",\"certificate\":\"{cert}\"}},\n'.format(
            trusted_name=cert_name,
            cert=cert.strip())
        number += 1

    json_cert_format = json_cert_format.rsplit(',', 1)[0]
    json_trusted_cert='{{\"input\": {{ \"trusted-certificate\": [{certificates}]}}}}'.format(
        certificates=json_cert_format)
    return json_trusted_cert


def makeRestconfPost(conn, json_file, apiCall):
    req = conn.request("POST", apiCall, json_file, headers=headers)
    res = conn.getresponse()
    res.read()
    if res.status != 200:
        print "Error here, response back wasnt 200: Response was : %d , %s" % (res.status, res.reason)
    else:
        print res.status, res.reason

def extractZipFiles(zipFileList, count):
    for zipFolder in zipFileList:
        with zipfile.ZipFile(Path + "/" + zipFolder.strip(),"r") as zip_ref:
            zip_ref.extractall(Path)
        folder = zipFolder.rsplit(".")[0]
        processFiles(folder, count)

def processFiles(folder, count):
    conn = httplib.HTTPConnection("localhost",8181)
    for file in os.listdir(Path + "/" + folder):
        if os.path.isfile(Path + "/" + folder + "/" + file.strip()):
            if ".key" in file:
                clientKey = readFile(folder, file.strip())
            elif "trustedCertificate" in file:
                certList = readTrustedCertificate(folder, file.strip())
            elif ".crt" in file:
                clientCrt = readFile(folder, file.strip())
        else:
            print "Could not find file %s" % file.strip()
    shutil.rmtree(Path + "/" + folder)
    json_keystore_key = makeKeystoreKey(clientKey, count)
    json_private_key = makePrivateKey(clientKey, clientCrt, certList, count)
    json_trusted_cert = makeTrustedCertificate(certList, count)

    makeRestconfPost(conn, json_keystore_key, postKeystore)
    makeRestconfPost(conn, json_private_key, postPrivateKey)
    makeRestconfPost(conn, json_trusted_cert, postTrustedCertificate)

def makeHealthcheckCall(headers, timePassed):
    connected = False
    # WAIT 10 minutes maximum and test every 30 seconds if HealthCheck API is returning 200
    while timePassed < TIMEOUT:
        try:
            conn = httplib.HTTPConnection("localhost",8181)
            req = conn.request("POST", "/restconf/operations/SLI-API:healthcheck",headers=headers)
            res = conn.getresponse()
            res.read()
            if res.status == 200:
                print ("Healthcheck Passed in %d seconds." %timePassed)
                connected = True
                break
            else:
                print ("Sleep: %d seconds before testing if Healthcheck worked. Total wait time up now is: %d seconds. Timeout is: %d seconds" %(INTERVAL, timePassed, TIMEOUT))
        except:
            print ("Cannot execute REST call. Sleep: %d seconds before testing if Healthcheck worked. Total wait time up now is: %d seconds. Timeout is: %d seconds" %(INTERVAL, timePassed, TIMEOUT))
        timePassed = timeIncrement(timePassed)

    if timePassed > TIMEOUT:
        print ("TIME OUT: Healthcheck not passed in  %d seconds... Could cause problems for testing activities..." %TIMEOUT)
    return connected


def timeIncrement(timePassed):
    time.sleep(INTERVAL)
    timePassed = timePassed + INTERVAL
    return timePassed

def readCertProperties():
    connected = makeHealthcheckCall(headers, timePassed)

    if connected:
        count = 0
        if os.path.isfile(Path + "/certs.properties"):
            with open(Path + "/certs.properties", "r") as f:
                for line in f:
                    if not "*****" in line:
                        zipFileList.append(line)
                    else:
                        extractZipFiles(zipFileList, count)
                        count += 1
                        del zipFileList[:]
        else:
            print "Error: File not found in path entered"

readCertProperties()
