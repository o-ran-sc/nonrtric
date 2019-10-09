
// Helper functions for processing a NETWORK worksheet

var helpers = require('./helpers.js');
var _ = require('lodash');
var csvtojson = require('csvtojson');
var async = require('async');
var uuid = require('node-uuid');   // generate a uuid with "uuid.v1()"
var path = require('path');
var fs = require("fs");
var moment = require("moment");

var network = module.exports;
var getParam = helpers.getParam;

var indir;
var csvGeneral, csvSubnets, csvVpnBindings, csvPolicies, csvNetRoutes;
var rawJson={}
var finalJson={};  
var platform;
var req,res;
var preloadVersion;  // 1607, 1610, etc...
var proc_error = false;
var filename;

puts = helpers.puts;
putd = helpers.putd;

network.go = function(lreq,lres,cb,dir) {
  puts("Processing NETWORK workbook");
	proc_error = false;
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
    puts('general file is missing from upload.');
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
  doSubnets();
}

// READ WORKSHEET: SUBNETS

function doSubnets() {
  puts("Reading Subnets worksheet");
  var csvFilename="Subnets.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotSubnets);
  }
  else {
		puts('subnets file is missing from upload.');
    proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
		return;
  }
}

function gotSubnets(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('Subnets.csv file is missing from upload.');
    return;
  }
  csvSubnets = jsonObj;
  csvSubnets = _.reject(csvSubnets, 'field2', 'Subnets');
  csvSubnets = _.reject(csvSubnets, 'field2', 'start-address');
  csvSubnets = _.reject(csvSubnets, 'field2', '');
  puts("\nRead this: ");
  putd(csvSubnets);
  puts("\n");
  doVpnBindings();
}

// READ WORKSHEET: VPN-BINDINGS

function doVpnBindings() {
  puts("Reading VPN-Bindings worksheet");
  var csvFilename="VPN-Bindings.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotVpnBindings);
  }
  else {
		puts('vnp-bindings file is missing from upload.');
    proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
		return;
  }
}

function gotVpnBindings(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('VPN-Bindings.csv file is missing from upload.');
    return;
  }
  csvVpnBindings = jsonObj;
  csvVpnBindings = _.reject(csvVpnBindings, 'field2', 'VPN-Bindings');
  csvVpnBindings = _.reject(csvVpnBindings, 'field2', 'vpn-binding-id');
  csvVpnBindings = _.reject(csvVpnBindings, function(o) { return (_.trim(o.field2)=="") && (_.trim(o.field3)==""); } );
  puts("\nRead this: ");
  putd(csvVpnBindings);
  puts("\n");
  doPolicies();
}


// READ WORKSHEET: POLICIES

function doPolicies() {
  puts("Reading Policies worksheet");
  var csvFilename="Policies.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotPolicies);
  }
  else {
		puts('policies file is missing from upload.');
    proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
		return;
  }
}

function gotPolicies(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('Policies.csv file is missing from upload.');
    return;
  }
  csvPolicies = jsonObj;
  csvPolicies = _.reject(csvPolicies, 'field2', 'Policies');
  csvPolicies = _.reject(csvPolicies, 'field2', 'network-policy-fqdn');
  csvPolicies = _.reject(csvPolicies, 'field2', '');
  puts("\nRead this: ");
  putd(csvPolicies);
  puts("\n");
  doNetRoutes();
}


// READ WORKSHEET: NETWORK-ROUTES

function doNetRoutes() {
  puts("Reading Network-Routes worksheet");
  var csvFilename="Network-Routes.csv";
  var newFileName = helpers.getFileName(req, csvFilename);
  preloadVersion = getParam(csvGeneral, 'field2', 'preload-version', 'field3');
  if ( preloadVersion == '1607' ) {
    puts("This is a 1607 spreadsheet. Skipping Network-Routes.csv.");
    gotNetRoutes(null,{});
    return;
  }
  if ( newFileName != null ) {
    helpers.readCsv(indir, newFileName, gotNetRoutes);
  }
  else {
		puts('network-routes file is missing from upload.');
    proc_error=true;
    callback(csvFilename + ' file is missing from upload.');
		return;
  }
}

function gotNetRoutes(err, jsonObj) {
  if (err) {
    puts("\nError!");
    putd(err);
		proc_error=true;
    callback('Network-Routes.csv file is missing from upload.');
    return;
  }
  csvNetRoutes = jsonObj;
  csvNetRoutes = _.reject(csvNetRoutes, 'field2', 'Network-Routes');
  csvNetRoutes = _.reject(csvNetRoutes, 'field2', 'route-table-reference-fqdn');
  csvNetRoutes = _.reject(csvNetRoutes, 'field2', '');
  puts("\nRead this: ");
  putd(csvNetRoutes);
  puts("\n");
  doneReadingFiles();
}


// DONE READING FILES

function doneReadingFiles() {
  puts("\n");
  puts("DONE READING FILES!");
  puts("\n");
  processJson();
}


// PROCESS THE CSV FILES INTO OBJECTS TO BE ASSEMBLED INTO FINAL OUTPUT
function processJson() {
  processGeneral();
  processSubnets();
  processVpnBindings();
  processPolicies();
  processNetRoutes();
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

  networkTopoID = { "network-name": rawJson['network-name'],
		"network-role": rawJson['network-role'],
		"network-type": rawJson['network-type'],
		"network-technology": rawJson['network-technology'] };

  providerInfo = { "physical-network-name": rawJson['physical-network-name'],
               "is-provider-network": rawJson['is-provider-network'],
               "is-shared-network": rawJson['is-shared-network'],
               "is-external-network": rawJson['is-external-network'] };

  networkSubnets = rawJson['subnets'];

  networkVpnBindings = rawJson['vpn-bindings'];

  networkPolicies = rawJson['network-policy-fqdns'];

  networkRoutes = rawJson['route-table-reference'];

  networkTopo = { "network-topology-identifier": networkTopoID,
	      "provider-network-information": providerInfo,
              "subnets": networkSubnets,
              "vpn-bindings": networkVpnBindings,
              "network-policy": networkPolicies,
              "route-table-reference": networkRoutes};

  networkInput = {'network-topology-information': networkTopo};

  finalJson = {"input": networkInput};

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
    fullpath_filename = process.cwd() + "/uploads/" + unixTime + ".net_worksheet.json";
    filename = unixTime + ".net_worksheet.json.";
  } else {
    fullpath_filename = "./output.json."+unixTime;
    filename = "output.json." + unixTime;
  }
  helpers.writeOutput(req, fullpath_filename, JSON.stringify(finalJson,null,2), callback);
  //callback(null,  finalJson, filename);
}


// Gather functions that actually process data after it is all read

function processGeneral() {
  preloadVersion = getParam(csvGeneral, 'field2', 'preload-version', 'field3');
  rawJson['preload-version'] = preloadVersion;
  puts("Preload version: " + preloadVersion);

  if ( (preloadVersion!='1607') && (preloadVersion!='1610') ) {
    puts("\nError - incorrect version of preload worksheet.");
		proc_error=true;
    //callback('Error - incorrect version of preload worksheet.');
		return;
  }

  rawJson['network-name'] = getParam(csvGeneral, 'field2', 'network-name', 'field3');
  rawJson['network-role'] = getParam(csvGeneral, 'field2', 'network-role', 'field3');
  rawJson['network-type'] = getParam(csvGeneral, 'field2', 'network-type', 'field3');
  rawJson['network-technology'] = getParam(csvGeneral, 'field2', 'network-technology', 'field3');

  if ( preloadVersion!='1607' ) {
    rawJson['physical-network-name'] = getParam(csvGeneral, 'field2', 'physical-network-name', 'field3');
    rawJson['is-provider-network'] = getParam(csvGeneral, 'field2', 'is-provider-network', 'field3');
    rawJson['is-shared-network'] = getParam(csvGeneral, 'field2', 'is-shared-network', 'field3');
    rawJson['is-external-network'] = getParam(csvGeneral, 'field2', 'is-external-network', 'field3');
  }

  rawJson['request-action'] = "PreloadNetworkRequest";
  rawJson['svc-request-id'] = uuid.v1();
  rawJson['svc-action'] = "reserve";
  puts('rawJson:');
  putd(rawJson);
  puts("\n");
}

function processSubnets() {
  var newSubnets = [];
  csvSubnets.forEach( function(subnet) {
    var subnetJson = {};
    subnetJson["start-address"] = subnet.field2;
    if (subnet.field3!='') {
      subnetJson["dhcp-start-address"] = subnet.field3;
    }
    if (subnet.field4!='') {
      subnetJson["dhcp-end-address"] = subnet.field4;
    }
    if (subnet.field5!='') {
      subnetJson["gateway-address"] = subnet.field5;
    }
    subnetJson["cidr-mask"] = subnet.field6;
    subnetJson["ip-version"] = subnet.field7;
    subnetJson["dhcp-enabled"] = subnet.field8;
    subnetJson["subnet-name"] = subnet.field9;
    newSubnets.push(subnetJson);
    }
  );
  puts("subnets:");
  putd(newSubnets);
  puts("\n");
  rawJson["subnets"] = newSubnets;
}

function processVpnBindings() {
  var newVpnBindings = [];

  csvVpnBindings.forEach( function(vpn) {
    var vpnJson = {};
    bindid = _.trim(vpn.field2);
    vpnJson["vpn-binding-id"] = bindid;
    if (bindid!="") {
      newVpnBindings.push(vpnJson);
    }
    });

  puts("VPN-Bindings:");
  putd(newVpnBindings);
  puts("\n");
  rawJson["vpn-bindings"] = newVpnBindings;
}

function processPolicies() {
  var newPolicies = [];

  csvPolicies.forEach( function(policy) {
    var policyJson = {};
    fqdn = _.trim(policy.field2);
    if (fqdn != "") {
      policyJson["network-policy-fqdn"] = fqdn;
      newPolicies.push(policyJson);
    }

    }
  );

  puts("Policies:");
  putd(newPolicies);
  puts("\n");
  rawJson["network-policy-fqdns"] = newPolicies;
}

function processNetRoutes() {
  var newNetRoutes = [];

  csvNetRoutes.forEach( function(netroute) {
    var netrouteJson = {};
    fqdn = _.trim(netroute.field2);
    if (fqdn != "") {
      netrouteJson["route-table-reference-fqdn"] = fqdn;
      newNetRoutes.push(netrouteJson);
    }

    }
  );

  puts("Network-Routes:");
  putd(newNetRoutes);
  puts("\n");
  rawJson["route-table-reference"] = newNetRoutes;
}



