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

// Sim mon server - query the a1pms and the simulators for counters and other data
// Presents a web page on localhost:9999/mon

var LOCALHOST="http://127.0.0.1:"
var MRSTUB_PORT="3905"
var A1PMS_PORT="8081"
var CR_PORT="8090"
var ICS_PORT="8083"
var PRODSTUB_PORT="8092"
var RC_PORT="8680"

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
            // A chunk of data has been received.
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
}


//Format a comma separated list of data to a html-safe string with fixed field sizes
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

//Format a comma separated list of ids to a html-safe string with fixed field sizes
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
function padding(val, size, pad) {
	var s=""+val;
	for(var i=s.length;i<size;i++) {
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

//Status variables for a1pms
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

//Status variables, for parameters values fetched from ics
var ics1="", ics2="", ics3="", ics4="", ics_types="-", ics_producers="-";
var ics_producer_arr=new Array(0)
var ics_producer_type_arr=new Array(0)
var ics_producer_jobs_arr=new Array(0)
var ics_producer_status_arr=new Array(0)
var ics_jobs=new Array(0)
var ics_job_status=new Array(0)

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

var refreshCount_ics=-1

var refreshCount_cr=-1

var refreshCount_rc=-1

var ricbasename="ricsim"

var rc_services=""

var a1pmsprefix=""

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
            exec('docker ps --filter "name='+ricbasename+'" --filter "network=nonrtric-docker-net" --format "{{.Names}} {{.Ports}}" | sed s/0.0.0.0:// | cut -d \'>\' -f1 | sed \'s/[[-]]*$//\'', (err, stdout, stderr) => {

                var simulators = ""
                simulators=`${stdout}`.replace(/(\r\n|\n|\r)/gm," ");
                simulators=simulators.trim();
                var sims=simulators.split(" ")
                simnames=[]
                simports=[]
                for(var i=0;i<sims.length;i=i+2) {
                    simnames[i/2]=sims[i]
                    simports[i/2]=sims[i+1]
                }
            });
        }
        getCtr=getCtr+1

        //Get metric values from the simulators
        for(var index=0;index<simnames.length;index++) {

            if (checkFunctionFlag("simvar1_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/num_instances", index, function(data, idx) {
                    simvar1[idx] = data;
                    clearFlag("simvar1_"+idx)
                });
            }
            if (checkFunctionFlag("simvar2_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/num_types", index, function(data,idx) {
                    simvar2[idx] = data;
                    clearFlag("simvar2_"+idx)
                });
            }
            if (checkFunctionFlag("simvar3_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/policytypes", index, function(data,idx) {
                    data=data.replace(/\[/g,'');
                    data=data.replace(/\]/g,'');
                    data=data.replace(/ /g,'');
                    data=data.replace(/\"/g,'');
                    simvar3[idx] = data;
                    clearFlag("simvar3_"+idx)
                });
            }
            if (checkFunctionFlag("simvar4_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/interface", index, function(data,idx) {
                    simvar4[idx] = data;
                    clearFlag("simvar4_"+idx)
                });
            }
            if (checkFunctionFlag("simvar5_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/remote_hosts", index, function(data,idx) {
                    simvar5[idx] = data;
                    clearFlag("simvar5_"+idx)
                });
            }
            if (checkFunctionFlag("simvar6_"+index)) {
                getSimCtr(LOCALHOST+simports[index]+"/counter/datadelivery", index, function(data,idx) {
                    simvar6[idx] = data;
                    clearFlag("simvar6_"+idx)
                });
            }
        }

        //MR - get metrics values from the MR stub
        if (checkFunctionFlag("mr1")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/requests_submitted", 0, function(data, idx) {
                mr1 = data;
                clearFlag("mr1")
            });
        }
        if (checkFunctionFlag("mr2")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/requests_fetched", 0, function(data, idx) {
                mr2 = data;
                clearFlag("mr2")
            });
        }
        if (checkFunctionFlag("mr3")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/current_requests", 0, function(data, idx) {
                mr3 = data;
                clearFlag("mr3")
            });
        }
        if (checkFunctionFlag("mr4")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/responses_submitted", 0, function(data, idx) {
                mr4 = data;
                clearFlag("mr4")
            });
        }
        if (checkFunctionFlag("mr5")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/responses_fetched", 0, function(data, idx) {
                mr5 = data;
                clearFlag("mr5")
            });
        }
        if (checkFunctionFlag("mr6")) {
            getSimCtr(LOCALHOST+MRSTUB_PORT+"/counter/current_responses", 0, function(data, idx) {
                mr6 = data;
                clearFlag("mr6")
            });
        }

        //CR - get metrics values from the callbackreceiver
        if (checkFunctionFlag("cr1")) {
            getSimCtr(LOCALHOST+CR_PORT+"/counter/received_callbacks", 0, function(data, idx) {
                cr1 = data;
                clearFlag("cr1")
            });
        }
        if (checkFunctionFlag("cr2")) {
            getSimCtr(LOCALHOST+CR_PORT+"/counter/fetched_callbacks", 0, function(data, idx) {
                cr2 = data;
                clearFlag("cr2")
            });
        }
        if (checkFunctionFlag("cr3")) {
            getSimCtr(LOCALHOST+CR_PORT+"/counter/current_messages", 0, function(data, idx) {
                cr3 = data;
                clearFlag("cr3")
            });
        }
        //A1PMS - more get metrics from the a1pms
        if (checkFunctionFlag("ag1")) {
            getSimCtr(LOCALHOST+A1PMS_PORT+"/status", 0, function(data, idx) {
                ag1 = data;
                clearFlag("ag1")
            });
        }
        if (checkFunctionFlag("ag2")) {
            getSimCtr(LOCALHOST+A1PMS_PORT+"/services", 0, function(data, idx) {
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
            getSimCtr(LOCALHOST+A1PMS_PORT+"/policy_types", 0, function(data, idx) {
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
            getSimCtr(LOCALHOST+A1PMS_PORT+"/policy_ids", 0, function(data, idx) {
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
            getSimCtr(LOCALHOST+A1PMS_PORT+"/rics", 0, function(data, idx) {
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

function fetchAllMetrics_ics() {

    console.log("Fetching information metrics - timer:" + refreshCount_ics)

    if (refreshCount_ics < 0) {
        refreshCount_ics = -1
        return
    } else {
        refreshCount_ics = refreshCount_ics - 1
    }
    setTimeout(() => {

        if (checkFunctionFlag("ics_stat")) {
            getSimCtr(LOCALHOST+ICS_PORT+"/status", 0, function(data, index) {
                try {
                    var jd=JSON.parse(data);
                    ics1=jd["status"]
                    ics2=""+jd["no_of_producers"]
                    ics3=""+jd["no_of_types"]
                    ics4=""+jd["no_of_jobs"]
                }
                catch (err) {
                    ics1="error response"
                    ics2="error response"
                    ics3="error response"
                    ics4="error response"
                }
            });
            clearFlag("ics_stat")
        }
        if (checkFunctionFlag("ics_types")) {
            getSimCtr(LOCALHOST+ICS_PORT+"/ei-producer/v1/eitypes", 0, function(data, index) {
                var tmp_ics_types="-"
                try {
                    var jd=JSON.parse(data);
                    for(var i=0;i<jd.length;i++) {
                        if (tmp_ics_types.length == 1) {
                            tmp_ics_types=""
                        }
                        tmp_ics_types=""+tmp_ics_types+jd[i]+" "
                    }
                }
                catch (err) {
                    tmp_ics_types="error response"
                }
                ics_types = tmp_ics_types
            });
            clearFlag("ics_types")
        }
        if (checkFunctionFlag("ics_producers")) {
            getSimCtr(LOCALHOST+ICS_PORT+"/ei-producer/v1/eiproducers", 0, function(data, index) {
                var tmp_ics_producers="-"
                try {
                    var jd=JSON.parse(data);
                    var tmp_ics_producer_arr=new Array(jd.length)
                    for(var i=0;i<jd.length;i++) {
                        if (tmp_ics_producers.length == 1) {
                            tmp_ics_producers=""
                        }
                        tmp_ics_producers=""+tmp_ics_producers+jd[i]+" "
                        tmp_ics_producer_arr[i]=jd[i]
                    }
                    ics_producer_arr = tmp_ics_producer_arr
                    ics_producers = tmp_ics_producers
                }
                catch (err) {
                    ics_producers="error response"
                    ics_producer_arr=new Array(0)
                }
            });
            clearFlag("ics_producers")
        }
        if (checkFunctionFlag("ics_data")) {
            try {
                var tmp_ics_producer_type_arr = JSON.parse(JSON.stringify(ics_producer_arr))
                for(var x=0;x<tmp_ics_producer_type_arr.length;x++) {
                    getSimCtr(LOCALHOST+ICS_PORT+"/ei-producer/v1/eiproducers/"+tmp_ics_producer_type_arr[x], x, function(data, idx) {
                        var row=""+tmp_ics_producer_type_arr[idx]+" : "
                        try {
                            var jd=JSON.parse(data);
                            var jda=jd["supported_ei_types"]
                            for(var j=0;j<jda.length;j++) {
                                row=""+row+jda[j]+" "

                            }
                            tmp_ics_producer_type_arr[idx]=row
                        }
                        catch (err) {
                            tmp_ics_producer_type_arr=new Array(0)
                        }
                    });
                }
                ics_producer_type_arr = tmp_ics_producer_type_arr
            } catch (err) {
                ics_producer_type_arr=new Array(0)
            }
            try {
                var tmp_ics_producer_jobs_arr = JSON.parse(JSON.stringify(ics_producer_arr))
                for(x=0;x<tmp_ics_producer_jobs_arr.length;x++) {
                    getSimCtr(LOCALHOST+ICS_PORT+"/ei-producer/v1/eiproducers/"+tmp_ics_producer_jobs_arr[x]+"/eijobs", x, function(data, idx) {
                        var row=""+tmp_ics_producer_jobs_arr[idx]+" : "
                        try {
                            var jd=JSON.parse(data);
                            for(var j=0;j<jd.length;j++) {
                                var jda=jd[j]
                                row=""+row+jda["ei_job_identity"]+"("+jda["ei_type_identity"]+") "
                            }
                            tmp_ics_producer_jobs_arr[idx]=row
                        }
                        catch (err) {
                            tmp_ics_producer_jobs_arr=new Array(0)
                        }
                    });
                }
                ics_producer_jobs_arr = tmp_ics_producer_jobs_arr
            } catch (err) {
                ics_producer_jobs_arr=new Array(0)
            }

            try {
                var tmp_ics_producer_status_arr = JSON.parse(JSON.stringify(ics_producer_arr))
                for(x=0;x<tmp_ics_producer_status_arr.length;x++) {
                    getSimCtr(LOCALHOST+ICS_PORT+"/ei-producer/v1/eiproducers/"+tmp_ics_producer_status_arr[x]+"/status", x, function(data, idx) {
                        var row=""+tmp_ics_producer_status_arr[idx]+" : "
                        try {
                            var jd=JSON.parse(data);
                            row=""+row+jd["operational_state"]
                            tmp_ics_producer_status_arr[idx]=row
                        }
                        catch (err) {
                            tmp_ics_producer_status_arr=new Array(0)
                        }
                    });
                }
                ics_producer_status_arr = tmp_ics_producer_status_arr
            } catch (err) {
                ics_producer_status_arr=new Array(0)
            }
            clearFlag("ics_data")
        }
        if (checkFunctionFlag("ics_jobs")) {
            getSimCtr(LOCALHOST+ICS_PORT+"/A1-EI/v1/eijobs", 0, function(data, index) {
                try {
                    var jd=JSON.parse(data);
                    var tmpArr=new Array(jd.length)
                    for(var i=0;i<jd.length;i++) {
                        tmpArr[i]=jd[i]
                    }
                    ics_jobs=tmpArr
                }
                catch (err) {
                    ics_jobs=new Array(0)
                }
            });
            clearFlag("ics_jobs")
        }
        if (checkFunctionFlag("ics_job_status")) {
            try {
                var tmp_ics_job_status= JSON.parse(JSON.stringify(ics_jobs))
                for(x=0;x<tmp_ics_job_status.length;x++) {
                    getSimCtr(LOCALHOST+ICS_PORT+"/A1-EI/v1/eijobs/"+tmp_ics_job_status[x]+"/status", x, function(data, idx) {
                        try {
                            var jd=JSON.parse(data);
                            tmp_ics_job_status[idx]=""+tmp_ics_job_status[idx]+":"+jd["eiJobStatus"]
                        }
                        catch (err) {
                            tmp_ics_job_status="-"
                        }
                    });
                }
                ics_job_status = tmp_ics_job_status
            } catch (err) {
                ics_job_status="-"
            }
            clearFlag("ics_job_status")
        }
        if (checkFunctionFlag("prodstub_stat")) {
            getSimCtr(LOCALHOST+PRODSTUB_PORT+"/status", x, function(data, idx) {
                var ctr2_map=new Map()
                var ctr3_map=new Map()
                var ctr2=0
                var ctr4=0
                var tmp_ps_producers=""
                var tmp_ps_types=""
                var tmp_ps_producer_type_arr=new Array()
                var tmp_ps_producer_jobs_arr=new Array()
                var tmp_ps_producer_delivery_arr=new Array()
                var tmp_ps2=""
                var tmp_ps3=""
                var tmp_ps4=""
                try {
                    var jp=JSON.parse(data);
                    for(var prod_name in jp) {
                        ctr2_map.set(prod_name, prod_name)
                        ctr2 += 1
                        var jj=jp[prod_name]
                        var row=""+prod_name+" : "
                        var rowj=""+prod_name+" : "
                        var rowd=""+prod_name+" : "
                        tmp_ps_producers += prod_name + " "
                        for(var ji in jj) {
                            if (ji == "types") {
                                var ta=jj[ji]
                                for(var i=0;i<ta.length;i++) {
                                    ctr3_map.set(ta[i], ta[i])
                                    row += " "+ta[i]
                                }
                            } else if (ji == "supervision_response") {
                                //Do nothing
                            } else if (ji == "supervision_counter") {
                                //Do nothing
                            } else if (ji == "types") {
                                //Do nothing
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
                        tmp_ps_producer_type_arr[(ctr2-1)]=row
                        tmp_ps_producer_jobs_arr[(ctr2-1)]=rowj
                        tmp_ps_producer_delivery_arr[(ctr2-1)]=rowd
                    }
                    tmp_ps2=""+ctr2_map.size
                    tmp_ps3=""+ctr3_map.size
                    for(const [key, value] of ctr3_map.entries()) {
                        tmp_ps_types += key + " "
                    }
                    tmp_ps4=""+ctr4

                    ps_producers=tmp_ps_producers
                    ps_types=tmp_ps_types
                    ps_producer_type_arr=tmp_ps_producer_type_arr
                    ps_producer_jobs_arr=tmp_ps_producer_jobs_arr
                    ps_producer_delivery_arr=tmp_ps_producer_delivery_arr
                    ps2=tmp_ps2
                    ps3=tmp_ps3
                    ps4=tmp_ps4
                }
                catch (err) {
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

        fetchAllMetrics_ics();

    }, 500)
}

function fetchAllMetrics_cr() {

    console.log("Fetching CR DB - timer:" + refreshCount_ics)

    if (refreshCount_cr < 0) {
        refreshCount_cr = -1
        return
    } else {
        refreshCount_cr = refreshCount_cr - 1
    }
    setTimeout(() => {

        if (checkFunctionFlag("cr_stat")) {
            getSimCtr(LOCALHOST+CR_PORT+"/db", 0, function(data, index) {
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

function fetchAllMetrics_rc() {

    console.log("Fetching RC services - timer:" + refreshCount_ics)

    if (refreshCount_rc < 0) {
        refreshCount_rc = -1
        return
    } else {
        refreshCount_rc = refreshCount_rc - 1
    }
    setTimeout(() => {

        if (checkFunctionFlag("rc_stat")) {
            getSimCtr(LOCALHOST+RC_PORT+"/services", 0, function(data, index) {
                var tmp_serv=""
                try {
                    var jd=JSON.parse(data);
                    for(var i=0;i<jd.length;i++) {
                        if (tmp_serv.length > 0) {
                            tmp_serv=tmp_serv+","
                        }
                        tmp_serv=tmp_serv+jd[i]["name"]
                    }

                }
                catch (err) {
                    tmp_serv="no_response"
                }
                rc_services=tmp_serv
            });
            clearFlag("rc_stat")
        }
        fetchAllMetrics_rc();
    }, 500)
}

// Monitor for CR db
app.get("/mon3",function(req, res){

    console.log("Creating CR DB page - timer: " + refreshCount_ics)

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

// Monitor for ICS
app.get("/mon2",function(req, res){

    console.log("Creating information metrics - timer: " + refreshCount_ics)

    if (refreshCount_ics < 0) {
        refreshCount_ics=5
        fetchAllMetrics_ics()
    }
    refreshCount_ics=5

    var summary=req.query.summary

    if (summary == undefined) {
        return res.redirect('/mon2?summary=false');
    }

  //Build web page
	var htmlStr = "<!DOCTYPE html>" +
          "<html>" +
          "<head>" +
            "<meta http-equiv=\"refresh\" content=\"2\">"+  //2 sec auto refresh
            "<title>information coordinator service and producer stub</title>"+
            "</head>" +
            "<body>" +
            "<font size=\"-3\" face=\"summary\">"
            if (summary == "false") {
                htmlStr=htmlStr+"<p>Set query param '?summary' to true to only show summary statistics.</p>"
            } else {
                htmlStr=htmlStr+"<p>Set query param '?summary' to false to only show full statistics</p>"
            }
            if (ics_job_status.length > 10) {
                htmlStr=htmlStr+"<div style=\"color:red\"> Avoid running the server for large number of producers and/or jobs</div>"
            }
            htmlStr=htmlStr+"</font>" +
            "<h3>Information Coordinator Service</h3>" +
            "<font face=\"monospace\">" +
            "Status:..........." + formatDataRow(ics1) + "<br>" +
            "Producers:........" + formatDataRow(ics2) + "<br>" +
            "Types:............" + formatDataRow(ics3) + "<br>" +
            "Jobs:............." + formatDataRow(ics4) + "<br>" +
            "</font>"
            if (summary == "false") {
                htmlStr=htmlStr+
                "<h4>Details</h4>" +
                "<font face=\"monospace\">" +
                "Producer ids:....." + formatDataRow(ics_producers) + "<br>" +
                "Type ids:........." + formatDataRow(ics_types) + "<br>" +
                "<br>";
                for(var i=0;i<ics_producer_type_arr.length;i++) {
                    var tmp=ics_producer_type_arr[i]
                    if (tmp != undefined) {
                        var s = "Producer types...." + formatDataRow(ics_producer_type_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(i=0;i<ics_producer_jobs_arr.length;i++) {
                    tmp=ics_producer_jobs_arr[i]
                    if (tmp != undefined) {
                        s = "Producer jobs....." + formatDataRow(ics_producer_jobs_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(i=0;i<ics_producer_status_arr.length;i++) {
                    tmp=ics_producer_status_arr[i]
                    if (tmp != undefined) {
                        s = "Producer status..." + formatDataRow(tmp) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(i=0;i<ics_job_status.length;i++) {
                    tmp=ics_job_status[i]
                    if (tmp != undefined) {
                        s = padding("Job", 18, ".") + formatDataRow(tmp) + "<br>"
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
                for(i=0;i<ps_producer_type_arr.length;i++) {
                    tmp=ps_producer_type_arr[i]
                    if (tmp != undefined) {
                        s = "Producer types...." + formatDataRow(ps_producer_type_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(i=0;i<ps_producer_jobs_arr.length;i++) {
                    tmp=ps_producer_jobs_arr[i]
                    if (tmp != undefined) {
                        s = "Producer jobs....." + formatDataRow(ps_producer_jobs_arr[i]) + "<br>"
                        htmlStr=htmlStr+s
                    }
                }
                htmlStr=htmlStr+"<br>";
                for(i=0;i<ps_producer_delivery_arr.length;i++) {
                    tmp=ps_producer_delivery_arr[i]
                    if (tmp != undefined) {
                        s = "Producer delivery." + formatDataRow(ps_producer_delivery_arr[i]) + "<br>"
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

    if (refreshCount_rc < 0) {
        refreshCount_rc=5
        fetchAllMetrics_rc()
    }
    refreshCount_rc=5

    var bn=req.query.basename
    a1pmsprefix=req.query.a1pmsprefix

    console.log("A1PMS"+a1pmsprefix)
    if ((bn == undefined) || (a1pmsprefix == undefined)) {
        getCtr=0
        return res.redirect('/mon?basename=ricsim&a1pmsprefix=/a1-policy/v2');
    } else {
        ricbasename=bn
    }


    //Build web page
	var htmlStr = "<!DOCTYPE html>" +
          "<html>" +
          "<head>" +
            "<meta http-equiv=\"refresh\" content=\"2\">"+  //2 sec auto refresh
            "<title>Policy Management Service and simulator monitor</title>"+
            "</head>" +
            "<body>" +
            "<font size=\"-3\" face=\"monospace\">" +
            "<p>Change basename in url if other ric sim prefix is used</p>" +
            "<p>Change a1pmsprefix in url if a1pms with other prefix is used</p>" +
            "</font>" +
            "<h3>Policy Management Service</h3>" +
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
            "<h3>Callback|Notification receiver</h3>" +
            "<font face=\"monospace\">" +
            "Callbacks received:..................." + formatDataRow(cr1) + "<br>" +
            "Callbacks fetched:...................." + formatDataRow(cr2) + "<br>" +
            "Number of waiting callback messages:.." + formatDataRow(cr3) + "<br>" +
            "</font>" +
            "<h3>R-APP Catalogue</h3>" +
            "<font face=\"monospace\">" +
            "Services:............................." + formatIdRowCompact(rc_services) + "<br>" +
            "</font>" +
            "<h3>Near-RT RIC | A1 Simulators</h3>" +
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
console.log("Open the web page on localhost:9999/mon2 to view the information statistics page.")
console.log("Open the web page on localhost:9999/mon3 to view CR DB in json.")