/*
#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#
*/

// Sim mon server - query the agent and the simulators for counters and other data
// Presents a web page on localhost:9999/mon

var LOCALHOST="http://127.0.0.1:"
var LOCALHOSTSECURE="https://127.0.0.1:"
//This var may switch between LOCALHOST and LOCALHOSTSECURE
var SIM_LOCALHOST=LOCALHOST
var MRSTUB_PORT="3905"
var AGENT_PORT="8081"
var CR_PORT="8090"
var http = require('http');
var https = require('https');

var express = require('express');
var app = express();
var fieldSize=32;



//I am alive
app.get("/",function(req, res){
	res.send("ok");
})

//Get parameter valuue from other server
function getSimCtr(httpx, url, index, cb) {
    var data = '';
    var http_type=http
    if (httpx=="https") {
        http_type=https
    }
    console.log("URL: "+ url + " - " + httpx)
    try {
        http_type.get(url, (resp) => {
            // A chunk of data has been recieved.
            resp.on('data', (chunk) => {
                data += chunk;
            });

            // The whole response has been received.
            resp.on('end', () => {
                var code=resp.statusCode
                if (code > 199 && code < 300) {
                    cb(data, index);
                } else {
                    cb("not found", index);
                }
            });

        }).on("error", (err) => {
            console.log("Error: " + err.message);
            cb("no response", index);
        });
    } catch(err) {
        cb("no response", index);
    }
};


//Format a comma separated list of data to a html-safe string with fixed fieldsizes
function formatDataRow(commaList) {
	var str = "";
	var tmp=commaList.split(',');
    for(var i=0;i<tmp.length;i++) {
        var data=tmp[i];
        var len = fieldSize-data.length;
        while(len>0) {
            data = data+"&nbsp;";
            len--;
        }
        str=str+data+"&nbsp;&nbsp;&nbsp;";
     }
	return str;
}

//Format a comma separated list of ids to a html-safe string with fixed fieldsizes
function formatIdRow(commaList) {
	var str = "";
	var tmp=commaList.split(',');
    for(var i=0;i<tmp.length;i++) {
    	tmp[i] = tmp[i].trim();
        var data="&lt"+tmp[i]+"&gt";
        var len = fieldSize+4-data.length;
        while(len>0) {
            data = data+"&nbsp;";
            len--;
        }
        str=str+data+"&nbsp;&nbsp;&nbsp;";
    }
	return str;
}

//Format a list of ids to a html-safe string in compact format
function formatIdRowCompact(commaList) {
    if (commaList == undefined) {
        commaList= "";
    }
	var str = "";
	var tmp=commaList.split(',');
    for(var i=0;i<tmp.length;i++) {
    	tmp[i] = tmp[i].trim();
        var data="&lt"+tmp[i]+"&gt";
        str=str+data+"&nbsp;";
    }
	return str;
}

//Pad a string upto a certain size using a pad string
function padding(val, fieldSize, pad) {
	var s=""+val;
	for(var i=s.length;i<fieldSize;i++) {
		s=s+pad
	}
	return s;
}

//Status variables, for parameters values fetched from other simulators
var mr1="", mr2="", mr3="", mr4="", mr5="", mr6="";

//Status variables for agent
var ag1=""
var ag2=""
var ag3=""
var ag4=""

//Status variables for callback receiver
var cr1=""
var cr2=""
var cr3=""


//Container names and ports of the ric simulator
var simnames=[]
var simports=[]

//Status variables for each ric simulator
var simvar1=[]
var simvar2=[]
var simvar3=[]
var simvar4=[]
var simvar5=[]

//Counts the number of get request for the html page
var getCtr=0

var refreshInterval=4000

//Ignore self signed cert
process.env["NODE_TLS_REJECT_UNAUTHORIZED"] = 0;

var sim_http_type="http"

function fetchAllMetrics() {
    setTimeout(() => {

        console.log("Fetching all metics data")
        if (refreshInterval < 20000) {
            refreshInterval+=100
        }
        if (getCtr%3 == 0) {
            //Extract the port numbers from the running simulators, for every 3 calls
            const { exec } = require('child_process');
            exec('docker ps --filter "name=ricsim" --format "{{.Names}} {{.Ports}}" | sed s/0.0.0.0:// | cut -d \'>\' -f1 | sed \'s/[[-]]*$//\'', (err, stdout, stderr) => {

                var simulators = ""
                simulators=`${stdout}`.replace(/(\r\n|\n|\r)/gm," ");
                simulators=simulators.trim();
                var sims=simulators.split(" ")
                simnames=[]
                simports=[]
                for(i=0;i<sims.length;i=i+2) {
                    simnames[i/2]=sims[i]
                    simports[i/2]=sims[i+1]
                }
            });
        }
        getCtr=getCtr+1

        //Get metric values from the simulators
        for(var index=0;index<simnames.length;index++) {
            try {
                if (index == 0) {
                    // Check is simulator are running on http or https - no response assumes http
                    getSimCtr("https",LOCALHOSTSECURE+simports[index]+"/", index, function(data, index) {
                        if (data=="OK") {
                            console.log("Found https simulator - assuming all simulators using https" )
                            sim_http_type="https"
                            SIM_LOCALHOST=LOCALHOSTSECURE
                        } else {
                            console.log("No https simulator found - assuming all simulators using http" )
                            sim_http_type="http"
                            SIM_LOCALHOST=LOCALHOST
                        }
                    });

                }
            } catch(err) {
                console.log("No https simulator found - assuming all simulators using http" )
                sim_http_type="http"
                SIM_LOCALHOST=LOCALHOST
            }
            getSimCtr(sim_http_type, SIM_LOCALHOST+simports[index]+"/counter/num_instances", index, function(data, index) {
                simvar1[index] = data;
            });
            getSimCtr(sim_http_type, SIM_LOCALHOST+simports[index]+"/counter/num_types", index, function(data,index) {
                simvar2[index] = data;
            });
            getSimCtr(sim_http_type, SIM_LOCALHOST+simports[index]+"/policytypes", index, function(data,index) {
                data=data.replace(/\[/g,'');
                data=data.replace(/\]/g,'');
                data=data.replace(/ /g,'');
                data=data.replace(/\"/g,'');
                simvar3[index] = data;
            });
            getSimCtr(sim_http_type, SIM_LOCALHOST+simports[index]+"/counter/interface", index, function(data,index) {
                simvar4[index] = data;
            });
            getSimCtr(sim_http_type, SIM_LOCALHOST+simports[index]+"/counter/remote_hosts", index, function(data,index) {
                simvar5[index] = data;
            });
        }

        //MR - get metrics values from the MR stub
        getSimCtr("http", LOCALHOST+MRSTUB_PORT+"/counter/requests_submitted", 0, function(data, index) {
            mr1 = data;
        });
        getSimCtr("http", LOCALHOST+MRSTUB_PORT+"/counter/requests_fetched", 0, function(data, index) {
            mr2 = data;
        });
        getSimCtr("http", LOCALHOST+MRSTUB_PORT+"/counter/current_requests", 0, function(data, index) {
            mr3 = data;
        });
        getSimCtr("http", LOCALHOST+MRSTUB_PORT+"/counter/responses_submitted", 0, function(data, index) {
            mr4 = data;
        });
        getSimCtr("http", LOCALHOST+MRSTUB_PORT+"/counter/responses_fetched", 0, function(data, index) {
            mr5 = data;
        });
        getSimCtr("http", LOCALHOST+MRSTUB_PORT+"/counter/current_responses", 0, function(data, index) {
            mr6 = data;
        });

        //CR - get metrics values from the callbackreceiver
        getSimCtr("http", LOCALHOST+CR_PORT+"/counter/received_callbacks", 0, function(data, index) {
            cr1 = data;
        });
        getSimCtr("http", LOCALHOST+CR_PORT+"/counter/fetched_callbacks", 0, function(data, index) {
            cr2 = data;
        });
        getSimCtr("http", LOCALHOST+CR_PORT+"/counter/current_messages", 0, function(data, index) {
            cr3 = data;
        });

        //Agent - get metrics from the agent
        getSimCtr("http", LOCALHOST+AGENT_PORT+"/status", 0, function(data, index) {
            ag1 = data;
        });
        getSimCtr("http", LOCALHOST+AGENT_PORT+"/services", 0, function(data, index) {
            ag2="";
            try {
                var jd=JSON.parse(data);
                for(var key in jd) {
                    if (ag2.length > 1) {
                        ag2=ag2+", "
                    }
                    ag2=ag2+(jd[key]["serviceName"]).trim()
                }
            }
            catch (err) {
                ag2=data
            }
        });
        getSimCtr("http", LOCALHOST+AGENT_PORT+"/policy_types", 0, function(data, index) {
            ag3="";
            try {
                var jd=JSON.parse(data);
                for(var key in jd) {
                    if (ag3.length > 0) {
                        ag3=ag3+", "
                    }
                    ag3=ag3+jd[key].trim()
                }
            }
            catch (err) {
                ag3=""
            }
        });
        getSimCtr("http", LOCALHOST+AGENT_PORT+"/policy_ids", 0, function(data, index) {
            ag4=""
            try {
                var jd=JSON.parse(data);
                ag4=""+jd.length
            }
            catch (err) {
                ag4=""
            }
        });



        fetchAllMetrics();
    }, refreshInterval)
}

fetchAllMetrics();

setInterval(() => {
    console.log("Setting interval "+refreshInterval+"ms")
}, refreshInterval)

app.get("/mon",function(req, res){


    refreshInterval=2000

  //Build web page
	var htmlStr = "<!DOCTYPE html>" +
          "<html>" +
          "<head>" +
            "<meta http-equiv=\"refresh\" content=\"2\">"+  //2 sec auto refresh
            "<title>Policy Agent and simulator monitor</title>"+
            "</head>" +
            "<body>" +
            "<h3>Policy agent</h3>" +
            "<font face=\"monospace\">" +
            "Status:..............................." + formatDataRow(ag1) + "<br>" +
            "Services:............................." + formatIdRowCompact(ag2) + "<br>" +
            "Types:................................" + formatIdRowCompact(ag3) + "<br>" +
            "Number of instances:.................." + formatDataRow(ag4) + "<br>" +
            "</font>" +
            "<h3>MR Stub interface</h3>" +
            "<font face=\"monospace\">"+
            "Submitted requests:............................" + formatDataRow(mr1) + "<br>" +
            "Fetched requests:.............................." + formatDataRow(mr2) + "<br>" +
            "Current requests waiting:......................" + formatDataRow(mr3) + "<br>" +
            "Submitted responses:..........................." + formatDataRow(mr4) + "<br>" +
            "Fetched responses.............................." + formatDataRow(mr5) + "<br>" +
            "Current responses waiting......................" + formatDataRow(mr6) + "<br>" +
            "</font>"+
            "<h3>Callback receiver</h3>" +
            "<font face=\"monospace\">" +
            "Callbacks received:..................." + formatDataRow(cr1) + "<br>" +
            "Callbacks fetched:...................." + formatDataRow(cr2) + "<br>" +
            "Number of waiting callback messages:.." + formatDataRow(cr3) + "<br>" +
            "</font>" +
            "<h3>Near-RT RIC Simulators</h3>" +
            "<font face=\"monospace\">"

            htmlStr=htmlStr+padding("Near-RT RIC Simulator name", 35,"&nbsp;")
            htmlStr=htmlStr+padding("Types", 10,"&nbsp;")
            htmlStr=htmlStr+padding("Instances", 10,"&nbsp;")+"<br>"
            htmlStr=htmlStr+padding("",55,"=")+"<br>"
            for(var simIndex=0;simIndex<simnames.length;simIndex++) {
                htmlStr=htmlStr+padding(simnames[simIndex]+ " ("+simports[simIndex]+")",35,"&nbsp;");
                htmlStr=htmlStr+padding(simvar2[simIndex],10,"&nbsp;")
                htmlStr=htmlStr+padding(simvar1[simIndex],10,"&nbsp;")
                htmlStr=htmlStr+"<br>";
            }

            htmlStr=htmlStr+"<br>";
            htmlStr=htmlStr+padding("Near-RT RIC Simulator name", 35,"&nbsp;")
            htmlStr=htmlStr+padding("Version", 20,"&nbsp;")
            htmlStr=htmlStr+padding("Type-IDs", 10,"&nbsp;")+"<br>"
            htmlStr=htmlStr+padding("",65,"=")+"<br>"
            for(simIndex=0;simIndex<simnames.length;simIndex++) {
                htmlStr=htmlStr+padding(simnames[simIndex]+ " ("+simports[simIndex]+")",35,"&nbsp;");
                htmlStr=htmlStr+padding(simvar4[simIndex],20,"&nbsp;")
                htmlStr=htmlStr+padding(formatIdRowCompact(simvar3[simIndex]),10,"&nbsp;")
                htmlStr=htmlStr+"<br>";
            }

            htmlStr=htmlStr+"<br>";
            htmlStr=htmlStr+padding("Near-RT RIC Simulator name", 35,"&nbsp;")
            htmlStr=htmlStr+padding("Remote hosts", 50,"&nbsp;")+"<br>"
            htmlStr=htmlStr+padding("",90,"=")+"<br>"
            for(simIndex=0;simIndex<simnames.length;simIndex++) {
                htmlStr=htmlStr+padding(simnames[simIndex]+ " ("+simports[simIndex]+")",35,"&nbsp;");
                htmlStr=htmlStr+padding(simvar5[simIndex],50,"&nbsp;")
                htmlStr=htmlStr+"<br>";
            }

            htmlStr=htmlStr+
           "</body>" +
          "</html>";
	res.send(htmlStr);
})

var httpServer = http.createServer(app);
var httpPort=9999;
httpServer.listen(httpPort);
console.log("Simulator monitor listening (http) at "+httpPort);
console.log("Open the web page on localhost:9999/mon to view the statistics page.")