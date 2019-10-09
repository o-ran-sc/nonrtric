var express = require('express');
var router = express.Router();
var exec = require('child_process').exec;
var util = require('util');
var fs = require('fs.extra');
var dbRoutes = require('./dbRoutes');
var csp = require('./csp');
var multer = require('multer');
var bodyParser = require('body-parser');
var sax = require('sax'),strict=true,parser = sax.parser(strict);
var async = require('async');
var l_ = require('lodash');
var dateFormat = require('dateformat');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');
var vnf = require('./vnf');
var network = require('./network');
var moment = require('moment');

// pass host, username and password to ODL
// target host for ODL request
var username = properties.odlUser;
var password = properties.odlPasswd;
var auth = 'Basic ' + new Buffer(username + ':' + password).toString('base64');
var host = properties.odlHost;
var port = properties.odlPort;

var header = {'Host': host, 'Authorization': auth, 'Content-Type': 'application/json'};
var options = {
        host    : host,
        headers : header,
        port    : port,
        rejectUnauthorized:false,
        strictSSL: false
};

// multer 
var unixTime = moment().unix();
var storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, process.cwd() + '/uploads/')
		return;
  },
  filename: function (req, file, cb) {
console.log('filename');
    cb(null, unixTime + "." + file.originalname )
		return;
  }
});

var upload = multer({
	storage: storage,
	fileFilter: function(req,file,cb) {
		var type = file.mimetype;
		if ( type.indexOf('ms-excel') == -1 ) {
			return cb(null,false);
		}
		cb(null,true);
		return;
	}
});

router.post('/uploadVnfCsv', csp.checkAuth, upload.array('filename'), function(req, res)
{
  var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;
	var tasks = [];

	tasks.push ( function(callback) { vnf.go(req,res,callback,''); } );
	tasks.push ( function(arg1,arg2,callback) { formatVnfInsertStatement(arg1,arg2,req,res,callback); } );
  tasks.push( function(arg1, callback) { dbRoutes.addRow(arg1,req,res,callback); } );
	async.waterfall(tasks, function(err,result)
	{	
		if(err){
			msgArray.push(err);
			dbRoutes.getVnfData(req,res,{code:'failure', msg:msgArray},privilegeObj);
			return;
		}
		else {
			msgArray.push('Successfully uploaded file.' );
			dbRoutes.getVnfData(req,res,{code:'success', msg:msgArray},privilegeObj);
			return;
		}
	});
});

router.post('/uploadNetworkCsv', csp.checkAuth, upload.array('filename'), function(req, res)
{
	console.log('uploadNetworkCsv');

	var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;
	var tasks = [];

	tasks.push ( function(callback) { network.go(req,res,callback,''); } );
	tasks.push ( function(arg1,arg2,callback) { formatNetworkInsertStatement(arg1,arg2,req,res,callback); } );
	tasks.push( function(arg1, callback) { dbRoutes.addRow(arg1,req,res,callback); } );
	async.waterfall(tasks, function(err,result)
	{
		if(err){
			console.log('ERROR:' + err);
			msgArray.push(err);
			dbRoutes.getVnfNetworkData(req,res,{code:'failure', msg:msgArray},privilegeObj);
		}
		else {
			msgArray.push('Successfully uploaded file.' );
			dbRoutes.getVnfNetworkData(req,res,{code:'success', msg:msgArray},privilegeObj);
		}
	});
});


function formatVnfInsertStatement(content,filename,req,res,callback)
{
	var newstr = JSON.stringify(content);
	var enc_str = encodeURI(newstr);
	var sql = "INSERT INTO PRE_LOAD_VNF_DATA "
		+ "(filename,preload_data) VALUES ("
		+ "'"+ filename + "',"
		+ "'" + enc_str + "')";

	callback(null,sql);
	return;
}

function formatNetworkInsertStatement(content,filename,req,res,callback)
{
	var newstr = JSON.stringify(content);
	var enc_str = encodeURI(newstr);
	var sql = "INSERT INTO PRE_LOAD_VNF_NETWORK_DATA "
		+ "(filename,preload_data) VALUES ("
		+ "'"+ filename + "',"
		+ "'" + enc_str + "')";

	callback(null,sql);
	return;
}

module.exports = router;
