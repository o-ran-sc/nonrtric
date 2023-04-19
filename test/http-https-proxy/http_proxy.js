//  ============LICENSE_START===============================================
//  Copyright (C) 2021 Nordix Foundation. All rights reserved.
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

// Basic http/https proxy
// Call the the proxy on 8080/8433 for http/https
// The destination (proxied) protocol may be http or https
// Proxy healthcheck on 8081/8434 for http/https - answers with statistics in json

const http = require('http');
const net = require('net');
const urlp = require('url');
const process = require('process')
const https = require('https');
const fs = require('fs');

// Proxy server port for http
const proxyport = 8080;
// Proxy server port for https
const proxyporthttps = 8433;
// Proxy server alive check, port for http
const aliveport = 8081;
// Proxy server alive check,  port for https
const aliveporthttps = 8434;

// Default https destination port
const defaulthttpsport = "443";

var debug = false;

// Certs etc for https
const httpsoptions = {
  key: fs.readFileSync('cert/key.crt'),
  cert: fs.readFileSync('cert/cert.crt'),
  passphrase: fs.readFileSync('cert/pass', 'utf8')
};

const stats = {
  'http-requests-initiated': 0,
  'http-requests-failed': 0,
  'https-requests-initiated': 0,
  'https-requests-failed': 0
};

// handle a http proxy request
function httpclientrequest(clientrequest, clientresponse) {
  stats['http-requests-initiated']++;

  // Extract destination information
  var crurl=clientrequest.url;
  var crhost=clientrequest.headers['host'];
  var crproto=clientrequest.headers['x-forwarded-proto'];

  if (debug) {
    console.log("crurl: "+crurl)
    console.log("crhost: "+crhost)
    console.log("crproto: "+crproto)
  }

  // If this server is running behind a proxy (like istio envoy proxy) then the 'clientrequest.url'
  // only contains the path component (i.e /test ). The host name and port is included in the
  // 'host' header and the protocol (http/https) is in the header 'x-forwarded-proto'.
  // In case of istio - https to a pod over mTLS does not seem to work. Only http.
  // Othewise, if no front proxy, the full url is included in the 'clientrequest.url'
  if (crproto != undefined) {
    crurl=crproto+"://"+crhost+crurl
    if (debug) {
      console.log(" Constructed url: "+crurl)
    }
  } else if (crurl.startsWith('/')) {
    console.log("Catched bad url in http request: "+crurl)
    clientresponse.end();
    return;
  }

  const clientrequesturl = new URL(crurl);

  var proxyrequestoptions = {
    'host': clientrequesturl.hostname,
    'port': clientrequesturl.port,
    'method': clientrequest.method,
    'path': clientrequesturl.pathname+clientrequesturl.search,
    'agent': clientrequest.agent,
    'auth': clientrequest.auth,
    'headers': clientrequest.headers
  };

  // Setup connection to destination
  var proxyrequest = http.request(
    proxyrequestoptions,
    function (proxyresponse) {
      clientresponse.writeHead(proxyresponse.statusCode, proxyresponse.headers);
      proxyresponse.on('data', function (chunk) {
        clientresponse.write(chunk);
      });
      proxyresponse.on('end', function () {
        clientresponse.end();
      });

    }
  );

  // Handle the connection and data transfer between source and destination
  proxyrequest.on('error', function (error) {
    clientresponse.writeHead(500);
    stats['http-requests-failed']++;
    console.log(error);
    clientresponse.write("<h1>500 Error</h1>\r\n" + "<p>Error was <pre>" + error + "</pre></p>\r\n" + "</body></html>\r\n");
    clientresponse.end();
  });
  clientrequest.addListener('data', function (chunk) {
    proxyrequest.write(chunk);
  });
  clientrequest.addListener('end', function () {
    proxyrequest.end();
  });
}

// Function to add a 'connect' message listener to a http server
function addhttpsconnect(httpserver) {
  httpserver.addListener(
    'connect',
    function (request, socketrequest, bodyhead) {

      if (debug) {
        console.log("Received 'connect' for: "+request['url'])
      }
      stats['https-requests-initiated']++;
      // Extract destination information
      var res = request['url'].split(":")
      var hostname = res[0]
      var port = defaulthttpsport;
      if (res[1] != null) {
        port = res[1]
      }

      // Setup connection to destination
      var httpversion = request['httpVersion'];
      var proxysocket = new net.Socket();

      proxysocket.connect(
        parseInt(port), hostname,
        function () {
          proxysocket.write(bodyhead);
          socketrequest.write("HTTP/" + httpversion + " 200 Connection established\r\n\r\n");
        }
      );

      // Handle the connection and data transfer between source and destination
      proxysocket.on('data', function (chunk) {
        socketrequest.write(chunk);
      });
      proxysocket.on('end', function () {
        socketrequest.end();
      });

      socketrequest.on('data', function (chunk) {
        proxysocket.write(chunk);
      });
      socketrequest.on('end', function () {
        proxysocket.end();
      });

      proxysocket.on('error', function (err) {
        stats['https-requests-failed']++;
        console.log(err);
        socketrequest.write("HTTP/" + httpversion + " 500 Connection error\r\n\r\n");
        socketrequest.end();
      });
      socketrequest.on('error', function (err) {
        stats['https-requests-failed']++;
        console.log(err);
        proxysocket.end();
      });
    }
  );
}

function main() {

  // -------------------- Alive server ----------------------------------
  // Respond with '200' and statistics for any path (except for GET|PUT|DELETE on /debug) on the alive address
  const alivelistener = function (req, res) {
    if (req.url == "/debug") {
      if (req.method == "GET") {
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.write(""+debug)
        res.end()
        return
      } else if (req.method == "PUT") {
        debug=true
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.write("OK")
        res.end()
        return
      } else if (req.method == "DELETE") {
        debug=false
        res.writeHead(200, { 'Content-Type': 'text/plain' });
        res.write("OK")
        res.end()
        return
      }
    }
    console.log(stats)
    res.writeHead(200, { 'Content-Type': 'application/json' });
    res.write(JSON.stringify(stats))
    res.end();
  };

  // The alive server - for healthckeck
  const aliveserver = http.createServer(alivelistener);

  // The alive server - for healthckeck
  const aliveserverhttps = https.createServer(httpsoptions, alivelistener);

  //Handle heatlhcheck requests
  aliveserver.listen(aliveport, () => {
    console.log('alive server on: '+aliveport);
    console.log(' example: curl localhost:'+aliveport)
  });

  //Handle heatlhcheck requests
  aliveserverhttps.listen(aliveporthttps, () => {
    console.log('alive server on: '+aliveporthttps);
    console.log(' example: curl -k https://localhost:'+aliveporthttps)
  });

  // -------------------- Proxy server ---------------------------------

  // The proxy server
  const proxyserver  = http.createServer(httpclientrequest).listen(proxyport);
  console.log('http/https proxy for http proxy calls on port ' + proxyport);
  console.log(' example: curl --proxy http://localhost:8080 http://100.110.120.130:1234')
  console.log(' example: curl -k --proxy http//localhost:8080 https://100.110.120.130:5678')

  // handle a http proxy request - https listener
  addhttpsconnect(proxyserver);

  const proxyserverhttps = https.createServer(httpsoptions, httpclientrequest).listen(proxyporthttps);
  console.log('http/https proxy for https proxy calls on port ' + proxyporthttps);
  console.log(' example: curl --proxy-insecure --proxy https://localhost:8433 http://100.110.120.130:1234')
  console.log(' example: curl -k --proxy-insecure --proxy https://localhost:8433 https://100.110.120.130:5678')

  // handle a https proxy request - https listener
  addhttpsconnect(proxyserverhttps);

}

//Handle ctrl c when running in interactive mode
process.on('SIGINT', () => {
  console.info("Interrupted")
  process.exit(0)
})

main();
