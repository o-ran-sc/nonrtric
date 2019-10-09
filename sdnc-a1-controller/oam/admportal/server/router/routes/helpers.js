
var _ = require('lodash');
var csvtojson = require('csvtojson');
var async = require('async');
var uuid = require('node-uuid');   // generate a uuid with "uuid.v1()"
var path = require('path');
var fs = require("fs");
var moment = require('moment');

var helpers = module.exports;

function puts(obj) { console.log(obj); } 
function putd(obj) { console.log(obj); } 
helpers.puts = puts;
helpers.putd = putd;


helpers.readCsv = function(filedir, filename, callback) {


  var Converter=csvtojson.Converter;
  var csvFileName=path.join(filedir,filename);
  var fileStream=fs.createReadStream(csvFileName);
  fileStream.on('error', function(err){
	callback(err, null);
  });
  var param={noheader:true, checkType:false};
  var csvConverter=new Converter(param);
  csvConverter.on("end_parsed",function(jsonObj){
     var returnValue = jsonObj;
     callback(null, returnValue);
  });
  fileStream.on('error', function(err) {
    putd(err);
    callback(err,"");
  });
  fileStream.pipe(csvConverter);
}

helpers.getParam = function(csv, matchField, matchValue, returnField) {
  dataRow=_.find(csv, matchField, matchValue);
  dataValue=dataRow[returnField];
  return dataValue;
}

helpers.writeOutput = function(req, filename, jsonOutput, callback) {
  try {
  	fs.writeFileSync(filename, jsonOutput);
  }
  catch(err){
	callback(err);
  }
}

helpers.getFileName = function(req, defFilename) {
  
  var fileObj = null;
  for (var x=0; x < req.files.length; x++)
  {
	var fileObj = req.files[x];
	if ( fileObj.filename.indexOf(defFilename) != -1 ){
  		return fileObj.filename;
	}
  }
  return null;
}
