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
var MRSTUB_PORT="3905"
var AGENT_PORT="8081"
var CR_PORT="8090"
var ECS_PORT="8083"
var PRODSTUB_PORT="8092"

var http = require('http');

var express = require('express');
const { POINT_CONVERSION_HYBRID } = require('constants')
var app = express();
var fieldSize=32;

var flagstore={}

//I am alive
app.get("/",function(req, res){
	res.send("ok");
})

//Get parameter value from other server
function getSimCtr(url, index, cb) {
    var data = '';

    try {
        http.get(url, (resp) => {
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

//Function to check if the previous call has returned, if so return true, if not return false
//For preventing multiple calls to slow containers.
function checkFunctionFlag(flag) {
    if (flagstore.hasOwnProperty(flag)) {
        if (flagstore[flag] == 0) {
            flagstore[flag]=1
            return true
        } else if (flagstore[flag] > 10) {
            //Reset flag after ten attempts
            console.log("Force release flag "+flag)
            flagstore[flag]=1
            return true
        } else {
            //Previous call not returned
            console.log("Flag not available "+flag)
            flagstore[flag]=flagstore[flag]+1
            return false
        }
    } else {
        flagstore[flag]=1
        return true
    }
}
//Clear flag for parameter
function clearFlag(flag) {
    flagstore[flag]=0
}

//Status variables, for parameters values fetched from other simulators
var mr1="", mr2="", mr3="", mr4="", mr5="", mr6="";

//Status variables for agent
var ag1=""
var ag2=""
var ag3=""
var ag4=""
var ag5=""

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
var simvar6=[]

//Status variables, for parameters values fetched from ecs
var ecs1="", ecs2="", ecs3="", ecs4="", ecs_types="-", ecs_producers="-";
var ecs_producer_arr=new Array(0)
var ecs_producer_type_arr=new Array(0)
var ecs_producer_jobs_arr=new Array(0)
var ecs_producer_status_arr=new Array(0)

//Status variables, for parameters values fetched from prodstub
var ps2="", ps3="", ps4="", ps_types="-", ps_producers="-";
var ps_producer_type_arr=new Array(0)
var ps_producer_jobs_arr=new Array(0)
var ps_producer_delivery_arr=new Array(0)

//Full CR DB
var cr_db={}

//Counts the number of get request for the html page
var getCtr=0

var refreshCount_pol=-1

var refreshCount_ecs=-1

var refreshCount_cr=-1

var ricbasename="ricsim"

function fetchAllMetrics_pol() {

    console.log("Fetching policy metrics " + refreshCount_pol)

    if (refreshCount_pol < 0) {
        refreshCount_pol = -1
        return
    } else {
        refreshCount_pol = refreshCount_pol - 1
    }
    setTimeout(() => {

        if (getCtr%3 == 0) {
            //Extract the port numbers from the running simulators, for every 3 calls
            const { exec } = require('child_process');
            exec('docker ps --filter "name='+ricbasename+'" --format "{{.Names}} {{.Ports}}" | sed s/0.0.0.0:// | cut -d \'>\' -f1 | sed \'s/[[-]]*$//\'', (err, stdout, stderr) => {

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

            if (checkFunctionFlag("simvar1_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/num_instances", index, function(data, index) {
                    simvar1[index] = data;
                    clearFlag("simvar1_"+index)
                });
            }
            if (checkFunctionFlag("simvar2_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/num_types", index, function(data,index) {
                    simvar2[index] = data;
                    clearFlag("simvar2_"+index)
                });
            }
            if (checkFunctionFlag("simvar3_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/policytypes", index, function(data,index) {
                    data=data.replace(/\[/g,'');
                    data=data.replace(/\]/g,'');
                    data=data.replace(/ /g,'');
                    data=data.replace(/\"/g,'');
                    simvar3[index] = data;
                    clearFlag("simvar3_"+index)
                });
            }
            if (checkFunctionFlag("simvar4_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/interface", index, function(data,index) {
                    simvar4[index] = data;
                    clearFlag("simvar4_"+index)
                });
            }
            if (checkFunctionFlag("simvar5_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/remote_hosts", index, function(data,index) {
                    simvar5[index] = data;
                    clearFlag("simvar5_"+index)
                });
            }
            if (checkFunctionFlag("simvar6_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/datadelivery", index, function(data,index) {
                    simvar6[index] = data;
                    clearFlag("simvar6_"+index)
                });
            }
        }

        //MR - get metrics values from the MR stub
        if (checkFunctionFlag("mr1")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/requests_submitted", 0, function(data, index) {
                mr1 = data;
                clearFlag("mr1")
            });
        }
        if (checkFunctionFlag("mr2")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/requests_fetched", 0, function(data, index) {
                mr2 = data;
                clearFlag("mr2")
            });
        }
        if (checkFunctionFlag("mr3")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/current_requests", 0, function(data, index) {
                mr3 = data;
                clearFlag("mr3")
            });
        }
        if (checkFunctionFlag("mr4")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/responses_submitted", 0, function(data, index) {
                mr4 = data;
                clearFlag("mr4")
            });
        }
        if (checkFunctionFlag("mr5")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/responses_fetched", 0, function(data, index) {
                mr5 = data;
                clearFlag("mr5")
            });
        }
        if (checkFunctionFlag("mr6")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/current_responses", 0, function(data, index) {
                mr6 = data;
                clearFlag("mr6")
            });
        }

        //CR - get metrics values from the callbackreceiver
        if (checkFunctionFlag("cr1")) {
            getSimCtr(LOCALHOST+CR_PORT+"/counter/received_callbacks", 0, function(data, index) {
                cr1 = data;
                clearFlag("cr1")
            });
        }
        if (checkFunctionFlag("cr2")) {
            getSimCtr(LOCALHOST+CR_PORT+"/counter/fetched_callbacks", 0, function(data, index) {
                cr2 = data;
                clearFlag("cr2")
            });
        }
        if (checkFunctionFlag("cr3")) {
            getSimCtr(LOCALHOST+CR_PORT+"/counter/current_messages", 0, function(data, index) {
                cr3 = data;
                clearFlag("cr3")
            });
        }
        //Agent - more get metrics from the agent
        if (checkFunctionFlag("ag1")) {
            getSimCtr(LOCALHOST+AGENT_PORT+"/status", 0, function(data, index) {
                ag1 = data;
                clearFlag("ag1")
            });
        }
        if (checkFunctionFlag("ag2")) {
            getSimCtr(LOCALHOST+AGENT_PORT+"/services", 0, function(data, index) {
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
                clearFlag("ag2")
            });
        }
        if (checkFunctionFlag("ag3")) {
            getSimCtr(LOCALHOST+AGENT_PORT+"/policy_types", 0, function(data, index) {
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
                clearFlag("ag3")
            });
        }

        if (checkFunctionFlag("ag4")) {
            getSimCtr(LOCALHOST+AGENT_PORT+"/policy_ids", 0, function(data, index) {
                try {
                    var jd=JSON.parse(data);
                    ag4=""+jd.length
                }
                catch (err) {
                    ag4=""
                }
                clearFlag("ag4")
            });
        }

        if (checkFunctionFlag("ag5")) {
            getSimCtr(LOCALHOST+AGENT_PORT+"/rics", 0, function(data, index) {
                try {
                    var jd=JSON.parse(data);
                    ag5=""+jd.length
                }
                catch (err) {
                    ag5=""
                }
                clearFlag("ag5")
            });
        }

        fetchAllMetrics_pol();

    }, 500)
}

function fetchAllMetrics_ecs() {

    console.log("Fetching enrichment metrics - timer:" + refreshCount_ecs)

    if (refreshCount_ecs < 0) {
        refreshCount_ecs = -1
        return
    } else {
        refreshCount_ecs = refreshCount_ecs - 1
    }
    setTimeout(() => {

        if (checkFunctionFlag("ecs_stat")) {
            getSimCtr(LOCALHOST+ECS_PORT+"/status", 0, function(data, index) {
                ecs1=""
                ecs2=""
                ecs3=""
                ecs4=""
                try {
                    var jd=JSON.parse(data);
                    ecs1=jd["status"]
                    ecs2=""+jd["no_of_producers"]
                    ecs3=""+jd["no_of_types"]
                    ecs4=""+jd["no_of_jobs"]
                }
                catch (err) {
                    ecs1="error response"
                    ecs2="error response"
                    ecs3="error response"
                    ecs4="error response"
                }
            });

            getSimCtr(LOCALHOST+ECS_PORT+"/ei-producer/v1/eitypes", 0, function(data, index) {
                ecs_types="-"
                try {
                    var jd=JSON.parse(data);
                    for(var i=0;i<jd.length;i++) {
                        if (ecs_types.length == 1) {
                            ecs_types=""
                        }
                        ecs_types=""+ecs_types+jd[i]+" "
                    }
                }
                catch (err) {
                    ecs_types="error response"
                }
            });

            getSimCtr(LOCALHOST+ECS_PORT+"/ei-producer/v1/eiproducers", 0, function(data, index) {
                ecs_producers="-"
                try {
                    var jd=JSON.parse(data);
                    var tmp_ecs_producer_arr=new Array(jd.length)
                    for(var i=0;i<jd.length;i++) {
                        if (ecs_producers.length == 1) {
                            ecs_producers=""
                        }
                        ecs_producers=""+ecs_producers+jd[i]+" "
                        tmp_ecs_producer_arr[i]=jd[i]
                    }
                    ecs_producer_arr = tmp_ecs_producer_arr
                }
                catch (err) {
                    ecs_producers="error response"
                    ecs_producer_arr=new Array(0)
                }
            });

            ecs_producer_type_arr = JSON.parse(JSON.stringify(ecs_producer_arr))
            for(var x=0;x<ecs_producer_type_arr.length;x++) {
                getSimCtr(LOCALHOST+ECS_PORT+"/ei-producer/v1/eiproducers/"+ecs_producer_type_arr[x], x, function(data, x) {
                    var row=""+ecs_producer_type_arr[x]+" : "
                    try {
                        var jd=JSON.parse(data);
                        var jda=jd["supported_ei_types"]
                        for(var j=0;j<jda.length;j++) {
                            row=""+row+jda[j]["ei_type_identity"]+" "
                        }
                        ecs_producer_type_arr[x]=row
                    }
                    catch (err) {
                        ecs_producer_type_arr=new Array(0)
                    }
                });
            }

            ecs_producer_jobs_arr = JSON.parse(JSON.stringify(ecs_producer_arr))
            for(var x=0;x<ecs_producer_jobs_arr.length;x++) {
                getSimCtr(LOCALHOST+ECS_PORT+"/ei-producer/v1/eiproducers/"+ecs_producer_jobs_arr[x]+"/eijobs", x, function(data, x) {
                    var row=""+ecs_producer_jobs_arr[x]+" : "
                    try {
                        var jd=JSON.parse(data);
                        for(var j=0;j<jd.length;j++) {
                            var jda=jd[j]
                            row=""+row+jda["ei_job_identity"]+"("+jda["ei_type_identity"]+") "
                        }
                        ecs_producer_jobs_arr[x]=row
                    }
                    catch (err) {
                        ecs_producer_jobs_arr=new Array(0)
                    }
                });
            }

            ecs_producer_status_arr = JSON.parse(JSON.stringify(ecs_producer_arr))
            for(var x=0;x<ecs_producer_status_arr.length;x++) {
                getSimCtr(LOCALHOST+ECS_PORT+"/ei-producer/v1/eiproducers/"+ecs_producer_status_arr[x]+"/status", x, function(data, x) {
                    var row=""+ecs_producer_status_arr[x]+" : "
                    try {
                        var jd=JSON.parse(data);
                        row=""+row+jd["operational_state"]
                        ecs_producer_status_arr[x]=row
                    }
                    catch (err) {
                        ecs_producer_status_arr=new Array(0)
                    }
                });
            }
            clearFlag("ecs_stat")
        }
        if (checkFunctionFlag("prodstub_stat")) {
            getSimCtr(LOCALHOST+PRODSTUB_PORT+"/status", x, function(data, x) {
                var ctr2_map=new Map()
                var ctr3_map=new Map()
                var ctr2=0
                var ctr4=0
                ps_producers=""
                ps_types=""
                ps_producer_type_arr=new Array()
                ps_producer_jobs_arr=new Array()
                ps_producer_delivery_arr=new Array()
                ps2=""
                ps3=""
                ps4=""
                try {
                    var jp=JSON.parse(data);
                    for(var prod_name in jp) {
                        ctr2_map.set(prod_name, prod_name)
                        ctr2 += 1
                        var jj=jp[prod_name]
                        var row=""+prod_name+" : "
                        var rowj=""+prod_name+" : "
                        var rowd=""+prod_name+" : "
                        ps_producers += prod_name + " "
                        for(var ji in jj) {
                            if (ji == "types") {
                                var ta=jj[ji]
                                for(var i=0;i<ta.length;i++) {
                                    ctr3_map.set(ta[i], ta[i])
                                    row += " "+ta[i]
                                }
                            } else if (ji == "supervision_response") {
                            } else if (ji == "supervision_counter") {
                            } else if (ji == "types") {
                            } else {
                                ctr4 += 1
                                rowj += " "+ji
                                rowd += " "+ji
                                var job_data=jj[ji]["json"]
                                if (job_data != undefined) {
                                    rowj += "("+job_data["ei_type_identity"]+")"
                                }
                                rowd += "("+jj[ji]["delivery_attempts"]+")"
                            }
                        }
                        ps_producer_type_arr[(ctr2-1)]=row
                        ps_producer_jobs_arr[(ctr2-1)]=rowj
                        ps_producer_delivery_arr[(ctr2-1)]=rowd
                    }
                    ps2=""+ctr2_map.size
                    ps3=""+ctr3_map.size
                    for(const [key, value] of ctr3_map.entries()) {
                        ps_types += key + " "
                    }
                    ps4=""+ctr4
                }
                catch (err) {
                    console.error(err);
                    ps_producers="error response"
                    ps_types="error response"
                    ps_producer_type_arr=new Array()
                    ps_producer_jobs_arr=new Array()
                    ps_producer_delivery_arr=new Array()
                    ps2="error response"
                    ps3="error response"
                    ps4="error response"
                }
            });
            clearFlag("prodstub_stat")
        }

        fetchAllMetrics_ecs();

    }, 500)
}

function fetchAllMetrics_cr() {

    console.log("Fetching CR DB - timer:" + refreshCount_ecs)

    if (refreshCount_cr < 0) {
        refreshCount_cr = -1
        return
    } else {
        refreshCount_cr = refreshCount_cr - 1
    }
    setTimeout(() => {

        if (checkFunctionFlag("cr_stat")) {
            getSimCtr(LOCALHOST+CR_PORT+"/db", 0, function(data, index) {
                ecs4=""
                try {
                    cr_db=JSON.parse(data);
                }
                catch (err) {
                    cr_db={}
                }
            });
            clearFlag("cr_stat")
        }
        fetchAllMetrics_cr();
    }, 500)
}

// Monitor for CR db
app.get("/mon3",function(req, res){

    console.log("Creating CR DB page - timer: " + refreshCount_ecs)

    if (refreshCount_cr < 0) {
        refreshCount_cr=5
        fetchAllMetrics_cr()
    }
    refreshCount_cr=5
    var json_str=JSON.stringify(cr_db, null, 1)
    var htmlStr = "<!DOCTYPE html>" +
    "<html>" +
    "<head>" +
      "<meta http-equiv=\"refresh\" content=\"2\">"+  //2 sec auto refresh
      "<title>CR DB dump</title>"+
      "</head>" +
      "<body style=\"white-space: pre-wrap\">" +
      json_str +
      "</body>" +
      "</html>";
    res.send(htmlStr);
})

// Monitor for ECS
app.get("/mon2",function(req, res){

    console.log("Creating enrichment metrics - timer: " + refreshCount_ecs)

    if (refreshCount_ecs < 0) {
        refreshCount_ecs=5
        fetchAllMetrics_ecs()
    }
    refreshCount_ecs=5

    var summary=req.query.summary

    if (summary == undefined) {
        return res.redirect('/mon2?summary=false');
    }

  //Build web page
	var htmlStr = "<!DOCTYPE html>" +
          "<html>" +
          "<head>" +
            "<meta http-equiv=\"refresh\" content=\"2\">"+  //2 sec auto refresh
            "<title>Enrichment coordinator service and producer stub</title>"+
            "</head>" +
            "<body>" +
            "<font size=\"-3\" face=\"summary\">"
            if (summary == "false") {
                htmlStr=htmlStr+"<p>Set query param '?summary' to true to only show summary statistics</p>"
            } else {
                htmlStr=htmlStr+"<p>Set query param '?summary' to false to only show full statistics</p>"
            }
            htmlStr=htmlStr+"</font>" +
            "<h3>Enrichment Coordinator Service</h3>" +
            "<font face=\"monospace\">" +
            "Status:..........." + formatDataRow(ecs1) + "<br>" +
            "Producers:........" + formatDataRow(ecs2) + "<br>" +
            "Types:............" + formatDataRow(ecs3) + "<br>" +
            "Jobs:............." + formatDataRow(ecs4) + "<br>" +
            "</font>"
            if (summary == "false") {
                htmlStr=htmlStr+
                "<h4>Details</h4>" +
                "<font face=\"monospace\">" +
                "Producer ids:....." + formatDataRow(ecs_producers) + "<br>" +
                "Type ids:........." + formatDataRow(ecs_types) + "<br>" +
                "<br>";
                for(var i=0;i<ecs_producer_type_arr.length;i++) {
                    var tmp=ecs_producer_type_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer types...." + formatDataRow(ecs_producer_type_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(var i=0;i<ecs_producer_jobs_arr.length;i++) {
                    var tmp=ecs_producer_jobs_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer jobs....." + formatDataRow(ecs_producer_jobs_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(var i=0;i<ecs_producer_status_arr.length;i++) {
                    var tmp=ecs_producer_status_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer status..." + formatDataRow(ecs_producer_status_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>"+"<br>" +
                "</font>"
            }
            htmlStr=htmlStr+
            "<h3>Producer stub</h3>" +
            "<font face=\"monospace\">" +
            "Producers:........" + formatDataRow(ps2) + "<br>" +
            "Types:............" + formatDataRow(ps3) + "<br>" +
            "Jobs:............." + formatDataRow(ps4) + "<br>" +
            "</font>"
            if (summary == "false") {
                htmlStr=htmlStr+
                "<h4>Details</h4>" +
                "<font face=\"monospace\">" +
                "Producer ids:....." + formatDataRow(ps_producers) + "<br>" +
                "Type ids:........." + formatDataRow(ps_types) + "<br>" +
                "<br>";
                for(var i=0;i<ps_producer_type_arr.length;i++) {
                    var tmp=ps_producer_type_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer types...." + formatDataRow(ps_producer_type_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(var i=0;i<ps_producer_jobs_arr.length;i++) {
                    var tmp=ps_producer_jobs_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer jobs....." + formatDataRow(ps_producer_jobs_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(var i=0;i<ps_producer_delivery_arr.length;i++) {
                    var tmp=ps_producer_delivery_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer delivery." + formatDataRow(ps_producer_delivery_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
            }
            htmlStr=htmlStr+
            "</font>" +
           "</body>" +
          "</html>";
	res.send(htmlStr);
})

// Monitor for policy management
app.get("/mon",function(req, res){

    console.log("Creating policy metrics page " + refreshCount_pol)

    if (refreshCount_pol < 0) {
        refreshCount_pol=5
        fetchAllMetrics_pol()
    }
    refreshCount_pol=5

    var bn=req.query.basename

    if (bn == undefined) {
        getCtr=0
        return res.redirect('/mon?basename=ricsim');
    } else {
        ricbasename=bn
    }

    //Build web page
	var htmlStr = "<!DOCTYPE html>" +
          "<html>" +
          "<head>" +
            "<meta http-equiv=\"refresh\" content=\"2\">"+  //2 sec auto refresh
            "<title>Policy Agent and simulator monitor</title>"+
            "</head>" +
            "<body>" +
            "<font size=\"-3\" face=\"monospace\">" +
            "<p>Change basename in url if other ric sim prefix is used</p>" +
            "</font>" +
            "<h3>Policy agent</h3>" +
            "<font face=\"monospace\">" +
            "Status:..............................." + formatDataRow(ag1) + "<br>" +
            "Services:............................." + formatIdRowCompact(ag2) + "<br>" +
            "Types:................................" + formatIdRowCompact(ag3) + "<br>" +
            "Number of instances:.................." + formatDataRow(ag4) + "<br>" +
            "Near-RT RICs:........................." + formatDataRow(ag5) + "<br>" +
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
            htmlStr=htmlStr+padding("Instances", 12,"&nbsp;")
            htmlStr=htmlStr+padding("Data delivery", 12,"&nbsp;")+"<br>"
            htmlStr=htmlStr+padding("",70,"=")+"<br>"
            for(var simIndex=0;simIndex<simnames.length;simIndex++) {
                htmlStr=htmlStr+padding(simnames[simIndex]+ " ("+simports[simIndex]+")",35,"&nbsp;");
                htmlStr=htmlStr+padding(simvar2[simIndex],10,"&nbsp;")
                htmlStr=htmlStr+padding(simvar1[simIndex],12    ,"&nbsp;")
                htmlStr=htmlStr+padding(simvar6[simIndex],12,"&nbsp;")
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
console.log("Open the web page on localhost:9999/mon to view the policy statistics page.")
console.log("Open the web page on localhost:9999/mon2 to view the enrichment statistics page.")
console.log("Open the web page on localhost:9999/mon3 to view CR DB in json.")