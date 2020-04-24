//  ============LICENSE_START===============================================
//  Copyright (C) 2020 Nordix Foundation. All rights reserved.
//  ========================================================================
//  Licensed under the Apache License, Version 2.0 (the "License");
//  you may not use this file except in compliance with the License.
//  You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//  Unless required by applicable law or agreed to in writing, software
//  distributed under the License is distributed on an "AS IS" BASIS,
//  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  See the License for the specific language governing permissions and
//  limitations under the License.
//  ============LICENSE_END=================================================
//

//This script acts like a proxy. All operations, except MR GET messages, are re-sent to the python backend server.
//The MR GET is intercepted and the python backend is polled until a message is available or up to a
//maximum time decided by a query parameter. No query parameter will result in a 10s long poll.


const proxy = require('express-http-proxy');
const app = require('express')();
const http = require('http');
const https = require('https');
const fs = require('fs');
var privateKey;
var certificate;
var credentials;

try {
  privateKey  = fs.readFileSync('cert/key.crt', 'utf8');
  certificate = fs.readFileSync('cert/cert.crt', 'utf8');
  credentials = {key: privateKey,
                     cert: certificate,
                     passphrase: 'test'};
} catch(exp) {
  console.log("Could not load cert and key")
}

const httpPort=3905;
const httpsPort=3906;
const proxyport=2222

const sleep = (milliseconds) => {
  return new Promise(resolve => setTimeout(resolve, milliseconds))
}

app.get("*/events/A1-POLICY-AGENT-READ/users/policy-agent*", inititate_long_poll);

function inititate_long_poll(req,res) {
  var millis=10000  //MR default is 10sec
  var tmp=req.query.timeout
  if (tmp != undefined) {
    millis=parseInt(tmp);
    millis=(millis < 0 ? 10000 : Math.min(millis, 60000)) //Max poll is 60 sec
    console.log("Setting poll time to (ms): " + millis)
  }
  do_poll(req, res, req.url, Date.now()+millis)
}

function do_poll(req,res, url, millis) {
    const options = {
      hostname: 'localhost',
      port: proxyport,
      path: url,
      method: 'GET'
    }
    http.get(options, function(resp) {
      let data = '';
      // Receiving chunk by chunk
      resp.on('data', (chunk) => {
        data += chunk;
      });

      // Full response received
      resp.on('end', () => {
        var payload=data.trim();
        if (resp.statusCode == 200 && payload.length == 2 && payload=="[]" && Date.now()<this.millis) {
          // Sleep before next poll
          sleep(10).then(() => {
            do_poll(req,res, this.url, this.millis)
          })
        } else {
          //Eventually return the response
          res.statusCode=resp.statusCode
          res.header(resp.headers);
          res.send(data)
          return;
        }
      });
    }.bind({millis:millis, url:url})).on("error", (err) => {
      console.log("Error when reading from backend: " + err.message);
      res.statusCode=500
      res.send("ERROR detected in frontend: "+ err.message)
      return
    });
}

//Catch all, resend from proxy
app.use(proxy('localhost:'+proxyport, {
  limit: '5mb',
}));

//Start serving http
var httpServer = http.createServer(app);
var httpsServer;
console.log("Running on http: "+httpPort)
httpServer.listen(httpPort, "::");

//Start serving https if cert is available
if (credentials != undefined) {
  httpsServer = https.createServer(credentials, app);
  console.log("Running on https: "+httpsPort)
  httpsServer.listen(httpsPort, "::");
}
