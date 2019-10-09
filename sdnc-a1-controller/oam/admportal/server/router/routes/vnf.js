
// Helper functions for processing a VNF worksheet

var helpers = require('./helpers.js');
var _ = require('lodash');
var csvtojson = require('csvtojson');
var async = require('async');
var uuid = require('node-uuid');   // generate a uuid with "uuid.v1()"
var path = require('path');
var fs = require("fs");
var moment = require("moment");

var vnf = module.exports;
var getParam = helpers.getParam;

var callback;
var indir;
var csvGeneral, csvZones, csvNetworks, csvVMs, csvVMnetworks, csvVMnetworkIPs, csvVMnetworkMACs, csvTagValues;
var rawJson={}
var finalJson={};  
var platform;
var req, res;
var preloadVersion;  // 1607, 1610, etc...
var proc_error=false;
var filename;

puts = helpers.puts;
putd = helpers.putd;

vnf.go = function(lreq,lres,cb,dir){
  puts("Processing VNF workbook");
	proc_error=false;
  req = lreq;
  res = lres;
  callback = cb;
  if (dir!="") {
    platform="pc";
    indir=dir;
  } else {
    platform="portal";
    indir=process.cwd() + "/uploads/";
  }
  doGeneral();
}


// READ WORKSHEET: GENERAL

function doGeneral() {
  puts("Reading General worksheet");
  var csvFilename="General.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotGeneral);
  }
  else {
    puts('General.csv file is missing from upload.');
		proc_error=true;
  }
}

function gotGeneral(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('General.csv file is missing from upload.');
    return;
  }
  csvGeneral = jsonObj;
  puts("\nRead this: ");
  putd(csvGeneral);
  puts("\n");
  doAvailZones();
}

// READ WORKSHEET: AVAILABILITY ZONES

function doAvailZones() {
  puts("Reading Availability-zones worksheet");
  var csvFilename="Availability-zones.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotAvailZones);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotAvailZones(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('Availability-zones.csv file is missing from upload.');
    return;
  }
  csvZones = jsonObj;
  csvZones = _.reject(csvZones, 'field2', 'Availability Zones');
  csvZones = _.reject(csvZones, 'field2', 'List the availability zones for this VNF');
  csvZones = _.reject(csvZones, 'field2', '');
  puts("\nRead this: ");
  putd(csvZones);
  puts("\n");
  doNetworks();
}

// READ WORKSHEET: NETWORKS

function doNetworks() {
  puts("Reading Networks worksheet");
  var csvFilename="Networks.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotNetworks);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotNetworks(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('Networks.csv file is missing from upload.');
    return;
  }
  csvNetworks = jsonObj;
  csvNetworks = _.reject(csvNetworks, 'field2', 'Networks');
  csvNetworks = _.reject(csvNetworks, 'field2', 'List the VNF networks. (VM-networks are on a different worksheet.)');
  csvNetworks = _.reject(csvNetworks, 'field2', 'network-role');
  csvNetworks = _.reject(csvNetworks, 'field2', '');
  puts("\nRead this: ");
  putd(csvNetworks);
  puts("\n");
  doVMs();
}

// READ WORKSHEET: VMs

function doVMs() {
  puts("Reading VMs worksheet");
  var csvFilename="VMs.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotVMs);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotVMs(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('VMs.csv file is missing from upload.');
    return;
  }
  csvVMs = jsonObj;
  csvVMs = _.reject(csvVMs, 'field2', 'VMs');
  csvVMs = _.reject(csvVMs, 'field2', 'List the VM types for this VNF');
  csvVMs = _.reject(csvVMs, 'field2', 'vm-type');
  csvVMs = _.reject(csvVMs, 'field2', '');
  puts("\nRead this: ");
  putd(csvVMs);
  puts("\n");
  doVMnetworks();
}

// READ WORKSHEET: VM-NETWORKS

function doVMnetworks() {
  puts("Reading VM-networks worksheet");
  var csvFilename="VM-networks.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotVMnetworks);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotVMnetworks(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('VM-networks.csv file is missing from upload.');
    return;
  }
  csvVMnetworks = jsonObj;
  csvVMnetworks = _.reject(csvVMnetworks, 'field2', 'VM-networks');
  csvVMnetworks = _.reject(csvVMnetworks, 'field2', 'List the VM-networks for each VM type');
  csvVMnetworks = _.reject(csvVMnetworks, 'field2', 'vm-type');
  csvVMnetworks = _.reject(csvVMnetworks, 'field2', '');
  puts("\nRead this: ");
  putd(csvVMnetworks);
  puts("\n");
  doVMnetworkIPs();
}

// READ WORKSHEET: VM-NETWORK-IPS

function doVMnetworkIPs() {
  puts("Reading VM-network-IPs worksheet");
  var csvFilename="VM-network-IPs.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotVMnetworkIPs);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotVMnetworkIPs(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('VM-network-IPs.csv file is missing from upload.');
    return;
  }
  csvVMnetworkIPs = jsonObj;
  csvVMnetworkIPs = _.reject(csvVMnetworkIPs, 'field2', 'VM-network-IPs');
  csvVMnetworkIPs = _.reject(csvVMnetworkIPs, 'field2', 'List the IPs assigned to each VM-network');
  csvVMnetworkIPs = _.reject(csvVMnetworkIPs, 'field2', 'vm-type');
  csvVMnetworkIPs = _.reject(csvVMnetworkIPs, 'field2', '');
  puts("\nRead this: ");
  putd(csvVMnetworkIPs);
  puts("\n");
  doVMnetworkMACs();
}

// READ WORKSHEET: VM-NETWORK-MACS

function doVMnetworkMACs() {
  puts("Reading VM-network-MACs worksheet");
  var csvFilename="VM-network-MACs.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotVMnetworkMACs);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotVMnetworkMACs(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('VM-network-MACs.csv file is missing from upload.');
    return;
  }
  csvVMnetworkMACs = jsonObj;
  csvVMnetworkMACs = _.reject(csvVMnetworkMACs, 'field2', 'VM-network-MACs');
  csvVMnetworkMACs = _.reject(csvVMnetworkMACs, 'field2', 'List the MACs assigned to each VM-network');
  csvVMnetworkMACs = _.reject(csvVMnetworkMACs, 'field2', 'vm-type');
  csvVMnetworkMACs = _.reject(csvVMnetworkMACs, 'field2', '');
  puts("\nRead this: ");
  putd(csvVMnetworkMACs);
  puts("\n");
  doTagValues();
}

// READ WORKSHEET: TAG-VALUES

function doTagValues() {
  puts("Reading Tag-values worksheet");
  var csvFilename="Tag-values.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotTagValues);
  }
  else {
		proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
  }
	return;
}

function gotTagValues(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('Tag-values.csv file is missing from upload.');
    return;
  }
  csvTagValues = jsonObj;
  csvTagValues = _.reject(csvTagValues, 'field2', 'Tag-values');
  csvTagValues = _.reject(csvTagValues, 'field2', 'Extra data to be passed into the HEAT template for this VNF');
  csvTagValues = _.reject(csvTagValues, 'field2', 'vnf-parameter-name');
  csvTagValues = _.reject(csvTagValues, 'field2', 'vnf-parameter-value');
  csvTagValues = _.reject(csvTagValues, 'field2', '');
  puts("\nRead this: ");
  putd(csvTagValues);
  puts("\n");
  doneReadingFiles();
}




function doneReadingFiles() {
  puts("\n");
  puts("DONE READING FILES!");
  puts("\n");
  processJson();
}


// PROCESS THE CSV FILES INTO OBJECTS TO BE ASSEMBLED INTO FINAL OUTPUT
function processJson() {
  processGeneral();
  processAvailZones();
  processNetworks();
  processVMnetworks();
  processVMnetips();
  processVMnetmacs();
  processVMs();
  processTagValues();
  assembleJson();
  outputJson();

	puts('proc_error=');
  putd(proc_error);
  if ( proc_error ){
    puts('callback with failure');
    callback('Error was encountered processing upload.');
    return;
  }
  else
  {
    puts('callback with success');
    callback(null,  finalJson, filename);
    return;
  }
}

// ASSEMBLE AND OUTPUT RESULTS

function assembleJson() {
  puts("\n");
  puts("Using raw JSON and assembling final ouptut JSON.");
  puts("\n");

  vnfTopoID = { "service-type": "SDN-MOBILITY",
	        "vnf-name": rawJson['vf-module-name'],
		"vnf-type": rawJson['vf-module-model-name'], 
		"generic-vnf-name": rawJson['generic-vnf-name'], 
		"generic-vnf-type": rawJson['generic-vnf-type'] };

  vnfZones = rawJson['availability-zones'];

  vnfNetworks = rawJson['networks'];

  vnfVMs = rawJson['vms'];

  vnfParams = rawJson['tag-values'];

  vnfAssignments = { "availability-zones": vnfZones,
	             "vnf-networks": vnfNetworks,
		     "vnf-vms": vnfVMs};

  vnfTopo = { "vnf-topology-identifier": vnfTopoID,
              "vnf-assignments": vnfAssignments,
              "vnf-parameters": vnfParams };

  vnfInput = {'vnf-topology-information': vnfTopo}; 

  finalJson = {"input": vnfInput};

  //outputJson();
}

function outputJson() {
  puts("\n");
  puts("\n");
  puts(JSON.stringify(finalJson,null,2));
  puts("\n");
  puts("\n");
  var unixTime, fullpath_filename;
  unixTime = moment().unix();
  if (platform=='portal') {
    fullpath_filename = process.cwd() + "/uploads/" + unixTime + ".vnf_worksheet.json";
    filename = unixTime + ".vnf_worksheet.json.";
  } else {
    fullpath_filename = "./output.json."+unixTime;
    filename = "output.json." + unixTime;
  }
  //helpers.writeOutput(req, fullpath_filename, JSON.stringify(finalJson,null,2), callback);
  //callback(null,  finalJson, filename);
}


// Gather functions that actually process data after it is all read

function processGeneral() {

  preloadVersion = getParam(csvGeneral, 'field2', 'preload-version', 'field3');
  rawJson['preload-version'] = preloadVersion;
  puts("Preload version: " + preloadVersion);

  if ( (preloadVersion!='1607') && (preloadVersion!='1610') ) {
    puts("\nError - incorrect version of preload worksheet.");
    callback('Error - incorrect version of preload worksheet.');
  }

  rawJson['vf-module-name'] = getParam(csvGeneral, 'field2', 'vf-module-name', 'field3');
  // rawJson['vf-module-type'] = getParam(csvGeneral, 'field2', 'vf-module-type', 'field3');
  try {
    rawJson['vf-module-model-name'] = getParam(csvGeneral, 'field2', 'vf-module-model-name', 'field3');
  } catch (e) {
    puts("\n\n");
    puts("ERROR ERROR ERROR ERROR ERROR\n");
    puts("Failed to find data field 'vf-module-model-name'. Maybe this preload worksheet is older?")
    puts("If on the 'general' tab there is a field called 'vf-module-type' please rename it to 'vf-module-model-name'");
    puts("\n\n");
    process.exit();
  }
  rawJson['generic-vnf-name'] = getParam(csvGeneral, 'field2', 'vnf-name', 'field3');
  rawJson['generic-vnf-type'] = getParam(csvGeneral, 'field2', 'vnf-type', 'field3');
  rawJson['request-id'] = uuid.v1();
  rawJson['source'] = "ADMINPORTAL";
  rawJson['request-action'] = "PreloadVNFRequest";
  rawJson['svc-request-id'] = uuid.v1();
  rawJson['svc-action'] = "reserve";
  puts('rawJson:');
  putd(rawJson);
  puts("\n");
}

function processAvailZones() {
  var newZones = _.map(csvZones, function(x) { return {'availability-zone': x['field2']}; } );
  rawJson['availability-zones'] = newZones;
  puts("Availability zones read:");
  putd(rawJson['availability-zones']);
  puts("\n");
}

function processNetworks() {
  var newNetworks = [];
  csvNetworks.forEach( function(network) {
    var netJson = {};
    netJson["network-role"] = network.field2;
    netJson["network-name"] = network.field3;
    netJson["network-id"] = network.field4;
    netJson["contrail-network-fqdn"] = network.field5;
    netJson["subnet-name"] = network.field6;
    netJson["subnet-id"] = network.field7;
    netJson["ipv6-subnet-name"] = network.field8;
    netJson["ipv6-subnet-id"] = network.field9;
    newNetworks.push(netJson);
    }
  );
  puts("networks:");
  putd(newNetworks);
  rawJson["networks"] = newNetworks;
}

function processVMs() {
  var newVMs = [];
  csvVMs.forEach( function(vm) {
    var vmJson = {};
    vmJson["vm-type"] = vm.field2;
    vmJson["vm-name"] = vm.field3;
    newVMs.push(vmJson);
    }
  );

  puts("VMs:");
  putd(newVMs);

  // OK, now for each type, get count and then build vm-names array of the names
  var vnfvms=[]
  vmTypes = _.uniq(_.pluck(newVMs,'vm-type'));
  vmTypes.forEach( function(vmType) {
    puts(vmType);
    var vmJson={};
    var vmThisType;
    var vmCount;
    var vmNames=[];
    var tmpNames;
    vmThisType=_.select(newVMs, 'vm-type', vmType);
    vmCount=vmThisType.length;
    vmJson["vm-type"] = vmType;
    vmJson["vm-count"] = vmCount;
    tmpNames = _.pluck(vmThisType,'vm-name');
    vmJson["vm-names"] = _.map(tmpNames, function(nam) { return {"vm-name": nam}; } );
    netroles = _.select( rawJson["vm-networks"], "vm-type", vmType );
    newnetroles=[]
    netroles.forEach( function(netrole) {
      tmpNetDetails = {};
      tmpNetDetails["network-role"] = netrole["network-role"];
      tmpNetDetails["use-dhcp"] = netrole["use-dhcp"];

      var tmpipsThisVmType=[];
      tmpipsThisVmType = _.select( rawJson["vm-net-ips"], "vm-type", vmType);
      var tmpips=[];
      tmpips = _.select( tmpipsThisVmType, "network-role", netrole["network-role"]);
      tmpipsJson = _.map(tmpips, function(ip) { return {"ip-address": ip["ip-address"]} } );
      tmpipsJson = _.reject(tmpipsJson, function(o) { return (o["ip-address"]==undefined); } );
      tmpNetDetails["network-ips"] = tmpipsJson;

      var tmpipsv6ThisVmType=[];
      tmpipsv6ThisVmType = _.select( rawJson["vm-net-ips"], "vm-type", vmType);
      var tmpipsv6=[];
      tmpipsv6 = _.select( tmpipsv6ThisVmType, "network-role", netrole["network-role"]);
      tmpipsv6Json = _.map(tmpipsv6, function(ip) { return {"ip-address-ipv6": ip["ipv6-address"]} } );
      tmpipsv6Json = _.reject(tmpipsv6Json, function(o) { return (o["ip-address-ipv6"]==undefined); } );
      tmpNetDetails["network-ips-v6"] = tmpipsv6Json;

      var tmpirpThisVmType=[];
      tmpirpThisVmType = _.select( rawJson["vm-net-ips"], "vm-type", vmType);
      var tmpirp=[];
      tmpirp = _.select( tmpirpThisVmType, "network-role", netrole["network-role"]);
      tmpirpJson = _.map(tmpirp, function(irp) { return {"interface-route-prefix-cidr": irp["interface-route-prefix"]} } );
      tmpirpJson = _.reject(tmpirpJson, function(o) { return (o["interface-route-prefix-cidr"]==undefined); } );
      tmpNetDetails["interface-route-prefixes"] = tmpirpJson;

      var tmpmacsThisVmType=[];
      tmpmacsThisVmType = _.select( rawJson["vm-net-macs"], "vm-type", vmType);
      var tmpmacs=[];
      tmpmacs = _.select( tmpmacsThisVmType, "network-role", netrole["network-role"]);
      tmpmacsJson = _.map(tmpmacs, function(mac) { return {"mac-address": mac["mac-address"]} } );
      tmpNetDetails["network-macs"] = tmpmacsJson;

      var fip='';
      fip = netrole["floating-ip"];
      fip = _.trim(fip);
      if (fip != '') {
        tmpNetDetails["floating-ip"] = netrole["floating-ip"];
      }

      var fipv6='';
      fipv6 = netrole["floating-ip-v6"];
      fipv6 = _.trim(fipv6);
      if (fipv6 != '') {
        tmpNetDetails["floating-ip-v6"] = netrole["floating-ip-v6"];
      }

      newnetroles.push(tmpNetDetails);
      }
    );
    vmJson["vm-networks"] = newnetroles;
    putd(vmJson);
    vnfvms.push(vmJson);
    }
  );
  rawJson["vms"] = vnfvms;
}

function processVMnetworks() {
  // For each VM type, for each Network role, get details like use-dhcp
  var newVMnetworks = [];
  csvVMnetworks.forEach( function(vm) {
    var newvmJson = {};
    newvmJson["vm-type"] = vm.field2;
    newvmJson["network-role"] = vm.field3;
    newvmJson["use-dhcp"] = vm.field4;
    newvmJson["floating-ip"] = vm.field5;
    newvmJson["floating-ip-v6"] = vm.field6;
    newVMnetworks.push(newvmJson);
    }
  );
  rawJson["vm-networks"] = newVMnetworks;
  puts("rawJson for vm-networks...");
  putd( rawJson["vm-networks"] );
}


function processVMnetips() {
  // For each VM type, for each network role, get the set of network IPs
  puts("Processing VM-net-ips");
  var newVMnetips = [];
  csvVMnetworkIPs.forEach( function(vm) {
    var newvmnetipsJson = {};
    newvmnetipsJson["vm-type"] = vm.field2;
    newvmnetipsJson["network-role"] = vm.field3;
    if (_.trim(vm.field4)!="") { 
      newvmnetipsJson["ip-address"] = vm.field4;
    }
    if (_.trim(vm.field5)!="") { 
    newvmnetipsJson["ipv6-address"] = vm.field5;
    }
    if (_.trim(vm.field6)!="") { 
    newvmnetipsJson["interface-route-prefix"] = vm.field6;
    }
    newVMnetips.push(newvmnetipsJson);
    }
  );
  rawJson["vm-net-ips"] = newVMnetips;
  puts("rawJson for vm-net-ips");
  putd(rawJson["vm-net-ips"]);
}

function processVMnetmacs() {
  // For each VM type, for each network role, get the set of MACs 
  puts("Processing VM-net-macs");
  var newVMnetmacs = [];
  csvVMnetworkMACs.forEach( function(vm) {
    var newvmnetmacsJson = {};
    newvmnetmacsJson["vm-type"] = vm.field2;
    newvmnetmacsJson["network-role"] = vm.field3;
    newvmnetmacsJson["mac-address"] = vm.field4;
    newVMnetmacs.push(newvmnetmacsJson);
    }
  );
  rawJson["vm-net-macs"] = newVMnetmacs;
  puts("rawJson for vm-net-macs");
  putd(rawJson["vm-net-macs"]);
}

function processTagValues() {
  var newTagValues = _.map(csvTagValues, function(x) { return {'vnf-parameter-name': x['field2'], 
	  'vnf-parameter-value': x['field3']}; } );
  rawJson['tag-values'] = newTagValues;
  puts("Tag-values read:");
  putd(rawJson['tag-values']);
  puts("\n");
}


