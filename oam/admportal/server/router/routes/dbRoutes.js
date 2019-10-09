var express = require('express'),
    app = express();
var mysql = require('mysql');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');
var fs = require('fs.extra');
var util = require('util');
var os = require('os');
var async = require('async');
var l_ = require('lodash');
var dns = require('dns');
var dnsSync = require('dns-sync');

var pool = '';
var currentDB = '';
var currentDbName = '';
var fabricDB = properties.dbFabricServer;
var dbArray = properties.databases;
var enckey = properties.passwordKey;

console.log('dbFabric=' + properties.dbFabric);

if ( properties.dbFabric == 'true' )
{
	connectFabric();
}
else
{
    initDB();
}


exports.dbConnect = function(){

	console.log('fabric=' + fabricDB);
	if ( properties.dbFabric == 'true' )
	{
		connectFabric();
	}
	else
	{
        initDB();
	}
}

function setCurrentDbName(){
	
	function createFunction(dbentry)
	{
		return function(callback) { findCurrentDbIP(dbentry,callback); }
	}

	var tasks = [];
	for (var x=0; x<dbArray.length; x++){
		var dbElement = dbArray[x];
		var dbElementArray = dbElement.split("|");

		tasks.push( createFunction(dbElement) );
	}
	async.series(tasks, function(err,result){
	
		if(err){
			currentDbName = err;
			console.log('currentDbName: ' + err);
            return;
        }
        else {
			console.log('not found');
            return;
        }
	});
}


function findCurrentDbIP(dbElement, callback){

	var dbElementArray = dbElement.split("|");

	dns.lookup( dbElementArray[0], function onLookup(err, addresses, family) {

        if ( currentDB == addresses ){
        	callback(dbElementArray[1]);
            return;
        }
		else {
        	callback(null);
        	return;
		}
    });
}


exports.getCurrentDB = function(){
	return currentDbName;
}
		
	
exports.testdb = function(req,res,callback){
console.log('testdb');

	osObj = {
        'hostname' : os.hostname(),
        'type'     : os.type(),
        'platform' : os.platform(),
        'arch'     : os.arch(),
        'release'  : os.release(),
        'uptime'   : os.uptime(),
        'totalmem' : os.totalmem(),
        'dbhealth' : ''
    };

    pool.getConnection(function(err,connection)
	{
        if(err){
            callback(err);
            return;
        }

		// http://stackoverflow.com/questions/10982281/mysql-connection-validity-test-in-datasource-select-1-or-something-better
		connection.query("/* pint */ SELECT 1", function(err,result){

            connection.release();
            if(err) {
				callback(err);
                return;
            }
			callback(null,'Database Connectivity to ' + currentDB + ' is working.');
            return;
        }); //end query
    }); // end getConnection
}

/*
exports.checkSvcLogic = function(req,res){

	if ( DBmasterHost.length > 0 && currentHost != DBmasterHost )
	{
		 // need to copy file so SLA functionality works
         var source = process.env.SDNC_CONFIG_DIR
    	     + "/svclogic.properties." + currentHost;
         var target = process.env.SDNC_CONFIG_DIR
             + "/svclogic.properties";
         fs.copy(source,target,{replace:true}, function(err){
         	if(err){
            	res.render("pages/err",
                	{result:{code:'error',
                              msg:"Unable to copy svclogic.properties. "+ String(err) }});
                return;
            }
         });
	}
}
*/
function initDB( next ) {


	var tasks = [];
	for (var x=0; x<properties.databases.length; x++){

		var db = properties.databases[x];
		var dbArray = db.split("|");
		var _dbIP = dnsSync.resolve(dbArray[0]);
		var _dbName = dbArray[1];

        tasks.push( createFindMasterFunctionObj(_dbIP, _dbName) );
	}
    async.series(tasks, function(err,result)
    {
        if(err){
			if ( err == 'found' ){
				if ( typeof next != 'undefined'){
					next();
				}
				else {
					return;
				}
			}
			else {
            	console.error( String(err) ); // ALARM
            	return;
			}
        }
		console.log('result=' + result);
	});
	return;
}


function createFindMasterFunctionObj(dbIP,dbName){
	return function(callback) { findMaster(dbIP, dbName, callback); }
}

function findMaster (ldbIP, ldbName, callback){
var dbIP = ldbIP;
var dbName = ldbName;

	console.log('checking dbIP:' + dbIP);

    pool = mysql.createPool({
        connectionLimit : properties.dbConnLimit,
        host            : dbIP,
        user            : properties.dbUser,
        password        : properties.dbPassword,
        database        : properties.dbName,
        multipleStatements: true,
        debug           : false
    });

	pool.getConnection(function(err,connection){

        if(err){
			callback( String(err) ); 
            return;
        }
        var sql = 'select @@read_only';
        connection.query(sql, function(err,result){
            connection.release();

            // @@read_only=0 means db is writeable
            console.log('@@read_only=' + result[0]['@@read_only']);
            if ( result[0]['@@read_only'] == '0' )
            { // writeable
				// if this is not the current DB, make it since its writeable
					currentDB = dbIP;
					currentDbName = dbName;
					console.log('currentDB=' + currentDB + "|" + currentDbName);
                	var newpool = mysql.createPool({
                   		connectionLimit : properties.dbConnLimit,
                   		host            : currentDB,
                   		user            : properties.dbUser,
                   		password        : properties.dbPassword,
                   		database        : properties.dbName,
                   		multipleStatements: true,
                   		debug           : false
                	}); // end create
                	pool = newpool;
					callback('found', currentDB);
					return;
			}
		// otherwise this is the current db and its writeable, just return
		callback(null, currentDB);
		return;
        });
    });
}

exports.checkDB = function(req,res,next){

console.log('checkDB');


	if ( properties.dbFabric == 'true' )
	{
		connectFabric();
		next();
	}
	else
	{
		initDB( next );
	}
}


exports.saveUser = function(req,res){

console.log('b4 sani');
	var email = req.sanitize(req.body.nf_email);
	var pswd = req.sanitize(req.body.nf_password);
console.log('after sani');

	pool.getConnection(function(err,connection)
	{
		if(err){
			console.error( String(err) ); // ALARM
			res.render("pages/signup", {result:{code:'error', msg:"Unable to get database connection. " + String(err)},header:process.env.MAIN_MENU});
			return;
		}
		var sql = "SELECT email FROM PORTAL_USERS WHERE email='" + email + "'";

		connection.query(sql, function(err,result)
		{
			if(err){
				connection.release();
				res.render("pages/signup", {result:{code:'error', msg:"Unable to get database connection. " + String(err)},header:process.env.MAIN_MENU});
				return;
			}
			if (result.length == 1 || result.length > 1)
			{
				connection.release();
				res.render("pages/signup", {result:{code:'error', msg:'User Information already exists.'},header:process.env.MAIN_MENU});
				return;
			}

			sql = "INSERT INTO PORTAL_USERS (email,password,privilege) VALUES ("
            +"'"+ email + "',"
            + "AES_ENCRYPT('" + pswd + "','" + enckey + "'),"
            +"'A')";

			connection.query(sql, function(err,result)
			{
				connection.release();
				
				if(err){
					res.render("pages/signup", {result:{ code:'error', msg:String(err) },header:process.env.MAIN_MENU});;
					return;
				}
				res.render('pages/signup', {result:{code:'success', msg:'User created.  Please login.'},header:process.env.MAIN_MENU});
				return;
			});
		});
	});
}

// delete User
exports.deleteUser = function(req,res){

	var rows={};
	var resultObj = { code:'', msg:'' };
	var privilegeObj = req.session.loggedInAdmin;

    pool.getConnection(function(err,connection) {
        if(err){
			console.error( String(err) ); // ALARM
            res.render("user/list", {rows: null, result:{code:'error', msg:"Unable to get database connection. Error:" + String(err), 
				privilege:privilegeObj },header:process.env.MAIN_MENU});
			return;
        }

        var sqlUpdate = "DELETE FROM PORTAL_USERS WHERE email='" + req.query.email + "'";

		console.log(sqlUpdate);

        connection.query(sqlUpdate,function(err,result){

            if(err){
                 resultObj = {code:'error', msg:'Delete of user failed Error: '+ String(err) };
            }

            // Need DB lookup logic here
            connection.query("SELECT email,password,privilege FROM PORTAL_USERS", function(err, rows) {
            	connection.release();
                if(!err) {
                    if ( rows.length > 0 )
                    {
						resultObj = {code:'success',msg:'Successfully deleted user.'};
                        res.render('user/list', { rows: rows, result:resultObj, privilege:privilegeObj,header:process.env.MAIN_MENU } );
						return;
                    }else{
                        res.render("user/list", { rows: null, result:{code:'error', msg:'Unexpected no rows returned from database, please try again.',
							privilege:privilegeObj },header:process.env.MAIN_MENU});
						return;
                    }
                } else {
                    res.render("user/list", { rows: null, result:{code:'error', msg:'Unexpected no rows returned from database. Error: ' + String(err),
							privilege:privilegeObj },header:process.env.MAIN_MENU});
					return;
                }
            }); //end query
        });
    }); // end of getConnection
}

// add User
exports.addUser = function(req,res){
	
	var rows={};
	var resultObj = { code:'', msg:'' };
	var privilegeObj = req.session.loggedInAdmin;
	var privilege = req.sanitize(req.body.nf_privilege);
	var email = req.sanitize(req.body.nf_email);
  var pswd = req.sanitize(req.body.nf_password);


	pool.getConnection(function(err,connection) 
	{
		if(err)
		{
			console.error( String(err) ); // ALARM
			res.render("user/list", {rows: null, result:{code:'error', msg:"Unable to get database connection. "+ String(err),
			privilege:privilegeObj },header:process.env.MAIN_MENU});
			return;
		}

		if( privilege == "admin" ){
			var char_priv = 'A';
		}else if(privilege == 'readonly'){
			var char_priv = 'R';
		}else{
			var char_priv = 'R';
		}

		//connection.query(sqlRequest, function(err,result)
		var sqlUpdate = "INSERT INTO PORTAL_USERS (email, password, privilege) VALUES ("
			+"'"+ email + "',"
			+ "AES_ENCRYPT('" + pswd + "','" + enckey + "'),"
			+"'"+ char_priv + "')";


		connection.query(sqlUpdate,function(err,result)
		{
			if(err){
				resultObj = {code:'error', msg:'Add of user failed Error: '+err};
			}
			// Need DB lookup logic here
			connection.query("SELECT email,AES_DECRYPT(password, '" + enckey + "') password,privilege FROM PORTAL_USERS", function(err, rows)
			{
				connection.release();
				if(!err)
				{
					if ( rows.length > 0 )
					{
						resultObj = {code:'success',msg:'Successfully added user.'};
						res.render('user/list', { rows: rows, result:resultObj, privilege:privilegeObj,header:process.env.MAIN_MENU } );
						return;
					}else{
						res.render("user/list", {rows: null, result:{code:'error', msg:'Unexpected no rows returned from database, please try again.',
							privilege:privilegeObj },header:process.env.MAIN_MENU});
						return;
					}
				}
				else {
					res.render("user/list", {rows: null, result:{code:'error', msg:'Unexpected no rows returned from database. Error: '+ err ,
						privilege:privilegeObj },header:process.env.MAIN_MENU});
					return;
				}
			}); //end query
		});
	}); // end of getConnection
}

// updateUser
exports.updateUser= function(req,res){

	var rows={};
	var resultObj = { code:'', msg:'' };
	var privilegeObj = req.session.loggedInAdmin;
	var email = req.sanitize(req.body.uf_email);
	var key_email = req.sanitize(req.body.uf_key_email)
  var pswd = req.sanitize(req.body.uf_password);
  var privilege = req.sanitize(req.body.uf_privilege);

	pool.getConnection(function(err,connection)
	{
		if(err){
			console.error( String(err) ); // ALARM
			res.render("user/list", {rows: null, result:{code:'error', msg:"Unable to get database connection. " + String(err),
				privilege:privilegeObj },header:process.env.MAIN_MENU});
			return;
		}

		if( privilege == "admin" ){
			var char_priv = 'A';
		}else if(privilege == 'readonly'){
			var char_priv = 'R';
		}else{
			var char_priv = 'R';
		}

		var sqlUpdate = "UPDATE PORTAL_USERS SET "
			+ "email = '" + email + "',"
			+ "password = " + "AES_ENCRYPT('" + pswd + "','" + enckey + "'), "
			+ "privilege = '"+ char_priv + "'"
			+ " WHERE email = '" + key_email + "'";

		connection.query(sqlUpdate,function(err,result)
		{
			if(err){
				resultObj = {code:'error', msg:'Update of user failed Error: '+err};
			}
			// Need DB lookup logic here
			connection.query("SELECT email, AES_DECRYPT(password,'" + enckey + "') password, privilege FROM PORTAL_USERS", function(err, rows)
			{
				connection.release();
				if(!err)
				{
					if ( rows.length > 0 )
					{
						resultObj = {code:'success',msg:'Successfully updated user.'};
						res.render('user/list', { rows: rows, result:resultObj, privilege:privilegeObj,header:process.env.MAIN_MENU} );
						return;
					}else{
						res.render("user/list", {rows: null, result:{ code:'error', msg:'Unexpected no rows returned from database.',
							privilege:privilegeObj },header:process.env.MAIN_MENU});
						return;
					}
				} else {
					res.render("user/list", {rows: null, result:{code:'error', msg:'Unexpected no rows returned from database. ' + String(err),
						privilege:privilegeObj },header:process.env.MAIN_MENU});
					return;
				}
			}); //end query
		});
	}); // end of getConnection
}

exports.listUsers = function(req,res,resultObj){

	var privilegeObj = req.session.loggedInAdmin;
	var rows={};
	pool.getConnection(function(err,connection)
	{
    
		if(err){
			console.error( String(err) ); // ALARM
			res.render("pages/list", 
			{
				rows: null, 
				result:{
					code:'error', 
					msg:"Unable to get database connection. " + String(err), 
					privilege:privilegeObj },
					header:process.env.MAIN_MENU
			});
			return;
		}

		// Need DB lookup logic here
		var selectUsers = "SELECT email, AES_DECRYPT(password,'" 
			+ enckey + "') password, privilege from PORTAL_USERS";

		connection.query(selectUsers, function(err, rows) {

		connection.release();
		if(err){
			resultObj = {code:'error', msg:'Unable to SELECT users Error: '+err};
		}
		if(!err)
		{
			if ( rows.length > 0 )
			{
				console.log(JSON.stringify(rows));
				res.render('user/list', 
				{
					rows: rows, 
					result:resultObj, 
					privilege:privilegeObj,
					header:process.env.MAIN_MENU 
				});
				return;
			}
			else{
				res.render("user/list", 
				{
					rows: null, 
					result:{
						code:'error', 
						msg:'Unexpected no rows returned from database.',
						privilege:privilegeObj },
						header:process.env.MAIN_MENU
				});
				return;
			}
		}
		else
		{
			res.render("user/list", 
			{
				rows: null, 
				result:{
					code:'error', 
					msg:'Unexpected no rows returned from database. ' + String(err),
					privilege:privilegeObj },header:process.env.MAIN_MENU
			});
			return;
		}
		}); //end query
	}); // end getConnection
}

exports.listSLA = function(req,res,resultObj){

	var privilegeObj = req.session.loggedInAdmin;

	pool.getConnection(function(err,connection) {

        if(err){
			console.error( String(err) ); // ALARM
            res.render("pages/err", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
			return;
        }

        // Need DB lookup logic here
		connection.query("SELECT module,rpc,version,mode,active,graph FROM SVC_LOGIC", function(err, rows) {

            connection.release();
            if(err) {
                res.render("pages/err", {result:{code:'error',msg:'Database Error: '+ String(err)},header:process.env.MAIN_MENU});
				return;
			}
			else {
				res.render("sla/list", {rows:rows, result:resultObj, privilege:privilegeObj, header:process.env.MAIN_MENU} );
				return;
            }
        }); //end query
    }); // end getConnection
}

exports.executeSQL = function(sql,req,res,callback){

    console.log(sql);

    pool.getConnection(function(err,connection) {

        if(err){
            console.error( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.' + err);
            return;
        }

        connection.query(sql, function(err,result){
            connection.release();
			if (err) {
				callback(err,'Database operation failed. ' + err );
			}
            else
            {
console.log('affectedRows='+result.affectedRows);
                callback(null, result.affectedRows);
            }
       }); //end query
    }); // end getConnection
}


// gamma - deleteParameter
exports.deleteParameter = function(req,res,callback){

    var sql = "DELETE FROM PARAMETERS WHERE name='" + req.query.name + "'";

    console.log(sql);

    pool.getConnection(function(err,connection) {

        if(err){
            console.log( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.' + err);
            return;
        }
        connection.query(sql, function(err,result){
            connection.release();
               if(err){
                    console.log('Update failed. ' + err );
                    callback(err,'Update failed. ' + err );
               }
               else
               {
                    callback(null,'');
               }
       }); //end query
    }); // end getConnection
}


exports.getTable = function(req,res,sql,rdestination,resultObj,privilegeObj){

console.log('SQL:'+sql);

    pool.getConnection(function(err,connection) {

        if(err){
            console.error( String(err) ); // ALARM
            res.render("pages/err", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
            return;
        }
        connection.query(sql,function(err, rows)
        {
            connection.release();
            if(err) {
                res.render(rdestination, {result:{code:'error',msg:'Database Error: '+ String(err)},header:process.env.MAIN_MENU});
                return;
            }
            else {
                res.render(rdestination, { rows: rows, result:resultObj, privilege:privilegeObj,header:process.env.MAIN_MENU } );
                return;
            }
        }); //end query
    }); // end getConnection
}

exports.getMetaTable = function(req,res,sql,rdestination,resultObj,privilegeObj){

    console.log('SQL:'+ sql);

    var rdata = [];
    var v_tables = [];
    var vtables = properties.viewTables;

	for ( var i in vtables ) {
		v_tables.push(vtables[i]);
	}

    pool.getConnection(function(err,connection) {

        if(err){
            console.error( String(err) ); // ALARM
            res.render("pages/err", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
            return;
        }
        connection.query(sql,function(err, rows, fields)
        {
            console.log('rows:' + JSON.stringify(rows,null,2));
            // http://stackoverflow.com/questions/14528385/how-to-convert-json-object-to-javascript-array
            //logger.debug(Object.keys(rows[0]).map(function(v) { return rows[0][v]; }));
            for ( var i in rows ){
                rdata.push(Object.keys(rows[i]).map(function(v) { return rows[i][v]; }));
                //logger.debug(Object.keys(rows[i]).map(function(v) { return rows[i][v]; }));
                //logger.debug([i, rows[i]]);
            }
            for ( var x in rdata ){
                for ( var j in rdata[x] ){
                    console.log('rdata[' + x + ']: ' + rdata[x][j]);
                }
            }
            console.log('rdata:' + rdata[0]);
            console.log('fields:' + JSON.stringify(fields,null,2));
            connection.release();
            if(err) {
                res.render(rdestination, {result:{code:'error',msg:'Database Error: '+ String(err)},header:process.env.MAIN_MENU});
                return;
            }
            else {
                res.render(rdestination, { displayTable:true, vtables:v_tables, rows:rdata, fields:fields, result:resultObj, privilege:privilegeObj, header:process.env.MAIN_MENU } );
                return;
            }
        }); //end query
    }); // end getConnection
}

exports.getVnfProfile = function(req,res,resultObj,privilegeObj){

	pool.getConnection(function(err,connection)
	{
		if(err){
			console.error( String(err) ); // ALARM
			res.render("pages/err", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
			return;
		}
		var sql = "SELECT vnf_type,availability_zone_count,equipment_role FROM VNF_PROFILE ORDER BY VNF_TYPE";
		console.log(sql);
		connection.query(sql, function(err, rows)
		{
			connection.release();
			if(err) {
				res.render("mobility/vnfProfile", {result:{code:'error',msg:'Database Error: '+ String(err)},header:process.env.MAIN_MENU});
				return;
			}
			else {
				console.log('render vnfProfile');
				res.render('mobility/vnfProfile', { rows: rows, result:resultObj, privilege:privilegeObj,header:process.env.MAIN_MENU } );
				return;
			}
		}); //end query
	}); // end getConnection
}


exports.getVnfPreloadData = function(req,res,dbtable,callback){

    pool.getConnection(function(err,connection) {

		 if(err){
            console.error( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.' + err);
            return;
        }

        // Need DB lookup logic here
        connection.query("SELECT preload_data FROM " + dbtable + " WHERE id="
			+ req.query.id, function(err, rows)
        {
            connection.release();
            if(err) {
                callback(err);
                return;
            }
            else {
				var buffer = rows[0].preload_data;
                var decode_buffer = decodeURI(buffer);
				var content = JSON.parse(decode_buffer);
				callback(null,content);
				return;
            }
        }); //end query
    }); // end getConnection
}



exports.getVnfNetworkData = function(req,res,resultObj,privilegeObj)
{ 
	pool.getConnection(function(err,connection)
	{
		if(err){
			console.error( String(err) ); // ALARM
			res.render("pages/err",
				{result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
			return;
		}
		// Need DB lookup logic here
		var sql = "SELECT id,svc_request_id,svc_action,status,filename,ts,preload_data FROM PRE_LOAD_VNF_NETWORK_DATA ORDER BY id";
		console.log(sql);
		connection.query(sql, function(err, rows)
		{
			var msgArray = new Array();
			connection.release();
			if(err) {
				msgArray = 'Database Error: '+ String(err);
				res.render("mobility/vnfPreloadNetworkData", {
					result:{code:'error',msg:msgArray},
					privilege:privilegeObj,
					preloadImportDirectory: properties.preloadImportDirectory,
					header:process.env.MAIN_MENU
				});
				return;
			}
			else {
				var retData = [];
				for( r=0; r<rows.length; r++)
				{
					var rowObj = {};
					rowObj.row = rows[r];
					if ( rows[r].filename.length > 0 )
					{
						try{
							var buffer = rows[r].preload_data;
							var decode_buffer = decodeURI(buffer);
							var filecontent = JSON.parse(decode_buffer);
							rowObj.filecontent = filecontent;
							rowObj.network_name = filecontent.input["network-topology-information"]["network-topology-identifier"]["network-name"];
							rowObj.network_type = filecontent.input["network-topology-information"]["network-topology-identifier"]["network-type"];
						}
						catch(error){
							msgArray.push('File ' + rows[r].filename + ' has invalid JSON. Error:' + error);
						}
					}
					else {
						rowObj.filecontent = '';
					}
					retData.push(rowObj);
				}//endloop
				if(msgArray.length>0){
					resultObj.code = 'failure';
					resultObj.msg = msgArray;
				}
				res.render('mobility/vnfPreloadNetworkData', { 
					retData:retData, 
					result:resultObj, 
					privilege:privilegeObj,
					preloadImportDirectory: properties.preloadImportDirectory,
					header:process.env.MAIN_MENU 
				});
				return;
			}
		}); //end query
	}); // end getConnection
}

exports.getVnfData = function(req,res,resultObj,privilegeObj)
{
	pool.getConnection(function(err,connection)
	{
		if(err){
			console.error( String(err) ); // ALARM
			res.render("pages/err", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
			return;
		}
		// Need DB lookup logic here
		var sql = "SELECT id,svc_request_id,svc_action,status,filename,ts,preload_data FROM PRE_LOAD_VNF_DATA ORDER BY id";
		console.log(sql);
		connection.query(sql,function(err, rows) 
		{
			var msgArray = new Array();
			connection.release();
			if(err) {
				msgArray = 'Database Error: '+ String(err);
				res.render("mobility/vnfPreloadData", {
					result:{code:'error',msg:msgArray},
					privilege:privilegeObj,
					preloadImportDirectory: properties.preloadImportDirectory,
					header:process.env.MAIN_MENU
				});
				return;
			}
			else {
				var retData = [];
				for( r=0; r<rows.length; r++)
				{
					var rowObj = {};
					rowObj.row = rows[r];
					if ( rows[r].filename.length > 0 )
					{
						try{
							var buffer = rows[r].preload_data;
							var s_buffer = decodeURI(buffer);
							var filecontent = JSON.parse(s_buffer);
							rowObj.filecontent = filecontent;
							rowObj.vnf_name = filecontent.input["vnf-topology-information"]["vnf-topology-identifier"]["vnf-name"];
							rowObj.vnf_type = filecontent.input["vnf-topology-information"]["vnf-topology-identifier"]["vnf-type"];
						}
						catch(error){
							msgArray.push('File ' + rows[r].filename + ' has invalid JSON. Error:' + error);
						}
					}
					else {
						rowObj.filecontent = '';
					}
					retData.push(rowObj);
				}//endloop
				if(msgArray.length>0){
					resultObj.code = 'failure';
					resultObj.msg = msgArray;
				}
				res.render('mobility/vnfPreloadData',{ 
					retData:retData, result:resultObj, 
					privilege:privilegeObj,
					header:process.env.MAIN_MENU, 
					preloadImportDirectory: properties.preloadImportDirectory
				});
				return;
			}
		}); //end query
	}); // end getConnection
}


exports.findAdminUser = function(email,res,callback) {


	var adminUser={};
	pool.getConnection(function(err,connection) {
        if(err){
			console.error( String(err) ); // ALARM
            res.render("pages/login", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
			return;
        }

		// Need DB lookup logic here
		connection.query("SELECT email, AES_DECRYPT(password, '" + enckey + "') password, privilege FROM PORTAL_USERS WHERE email='" + email + "'", function(err, rows) {

			connection.release();
        	if(!err) {
				if ( rows.length > 0 )
            	{
                	rows.forEach(function(row){
                    	adminUser = {
                        	"email" : row.email,
                        	"password" : row.password,
                        	"privilege" : row.privilege };
                	});
                	callback(adminUser);
					return;
            	}else{
                	console.log("no rows returned");
                	res.render("pages/login", {result:{code:'error', msg:'User is not in database.'},header:process.env.MAIN_MENU});
					return;
            	}
            } else {
                    res.render("pages/err", {result:{code:'error',msg:'Unexpected no rows returned from database. '+ String(err)},header:process.env.MAIN_MENU});
					return;
			}
		}); //end query
    }); // end getConnection
}


exports.addRow = function(sql,req,res,callback){

	console.log(sql);

	pool.getConnection(function(err,connection) {

		if(err){
			console.error( String(err) ); // ALARM
			callback(err, 'Unable to get database connection.' + err);
			return;
		}
		connection.query(sql, function(err,result){
			connection.release();
			if(err){
				console.debug('Database operation failed. ' + err );
				callback(err,'Database operation failed. ' + err );
			}
			else
			{
				callback(null, result.affectedRows);
			}
		}); //end query
	}); // end getConnection
}



exports.addVnfProfile = function(row,res,callback){

	var sqlInsert;

    if ( row.length < 3 )
    {
        console.log('Row [' + row + '] does not have enough fields.');
        callback(null, 'Row [' + row + '] does not have enough fields.');
		return;
    }

    sqlInsert = "INSERT INTO VNF_PROFILE ("
        + "vnf_type,availability_zone_count,equipment_role) VALUES ("
        + "'" + row[0] + "',"
		+ row[1] 
        + ",'" + row[2] + "')";

    console.log('SQL='+sqlInsert);

    pool.getConnection(function(err,connection) {

        if(err){
            console.log( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.');
            return;
        }
        connection.query(sqlInsert, function(err,result){
            connection.release();
            if(err){
                console.log('Row [' + row + '] failed to insert. ' + err );
                callback(null,'Row [' + row + '] failed to insert. ' + err );
            }
            else
            {
                callback(null,'');
            }
        }); //end query
    }); // end getConnection
}


// Add to SVC_LOGIC table
exports.addDG = function(_module, version, rpc, mode, xmlfile, req,res){

	var privilegeObj = req.session.loggedInAdmin;
	var rows={};

    pool.getConnection(function(err,connection) {
        if(err){
			console.error( String(err) ); // ALARM
            res.render("pages/err", {result:{code:'error', msg:"Unable to get database connection. "+ String(err)},header:process.env.MAIN_MENU});
			return;
        }

        var post = {
            module  :  _module,
            rpc     : rpc,
            version : version,
            mode    : mode,
            active  : "N",
            graph   : xmlfile
        };

        //logger.debug( JSON.stringify(post));

        //connection.query(sqlRequest, function(err,result){
        connection.query('INSERT INTO SVC_LOGIC SET ?', post, function(err,result){
            // neat!

            // Need DB lookup logic here
            connection.query("SELECT module,rpc,version,mode,active,graph FROM SVC_LOGIC", function(err, rows) {

                if(!err) {
                    if ( rows.length > 0 )
                    {
                        res.render('sla/list', { rows: rows, result:{code:'',msg:''}, privilege:privilegeObj,header:process.env.MAIN_MENU } );
						return;
                    }else{
                        console.log("no rows returned");
                        res.render("pages/home");
						return;
                    }
                }
                connection.on('error', function(err){
                    connection.release();
                    console.log(500, "An error has occurred -- " + err);
                    res.render("pages/home");
					return;
                });
            }); //end query

            connection.release();
        });
        //connection.query('INSERT INTO SVC_LOGIC SET ?', post, function(err,result){
            // neat!
            //logger.debug('inserted rows');
        //});

        //if(err){
            //res.render('pages/home');
        //}
        return;

    }); // end of getConnection
};

exports.activate = function(req,res,_module,rpc,version,mode,callback){

	var sql = "UPDATE SVC_LOGIC SET active=\'Y\' WHERE module=\'"
            + _module + "' AND rpc=\'"
            + rpc + "' AND version=\'"
            +  version + "' AND mode=\'"
            +  mode + "'";

	console.log('SQL='+sql);

    pool.getConnection(function(err,connection) {
    
        if(err){
			console.error( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.' + err);
			return;
        }

        connection.query(sql, function(err,result){

            connection.release();
			if(err){
            	callback(err, 'Unable to get database connection.' + err);
        	}
            else
            {
                 callback(null,'');
            }
       }); //end query
    }); // end getConnection
}


exports.deactivate = function(req,res,_module,rpc,version,mode,callback){

    var sql = "UPDATE SVC_LOGIC SET active=\'N\' WHERE module=\'"
            + _module + "' AND rpc=\'"
            + rpc + "' AND version=\'"
            +  version + "' AND mode=\'"
            +  mode + "'";

	console.log('SQL='+sql);

    pool.getConnection(function(err,connection) {

        if(err){
			console.error( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.' + err);
			return;
        }

        connection.query(sql, function(err,result){

            connection.release();
            if(err){
                callback(err, 'Unable to get database connection.' + err);
            }
            else
            {
                 callback(null,'');
            }
       }); //end query
    }); // end getConnection
}

exports.global_deactivate = function(req,res,_module,rpc,mode,callback){

    var sql = "UPDATE SVC_LOGIC SET active=\'N\' WHERE module=\'"
            + _module + "' AND rpc=\'"
            + rpc + "' AND mode=\'"
            +  mode + "'";


    pool.getConnection(function(err,connection) {

        if(err){
            callback(err, 'Unable to get database connection.' + err);
            return;
        }

        connection.query(sql, function(err,result){

            connection.release();
            if(err){
                callback(err, err);
            }
            else
            {
                 callback(null,'');
            }
       }); //end query
    }); // end getConnection
}


exports.deleteDG = function(req,res,_module,rpc,version,mode,callback){

	var sql = "DELETE FROM SVC_LOGIC WHERE module=\'"
            + _module + "' AND rpc=\'"
            + rpc + "' AND version=\'"
            +  version + "' AND mode=\'"
            +  mode + "'";

	console.log('SQL='+sql);

    pool.getConnection(function(err,connection) {

        if(err){
			console.error( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.' + err);
			return;
        }

        connection.query(sql, function(err,result){

            connection.release();
            if(err){
                callback(err, 'Unable to get database connection.' + err);
            }
            else
            {
                 callback(null,'');
            }
       }); //end query
    }); // end getConnection
}



function padLeft(nr, n, str){
    return Array(n-String(nr).length+1).join(str||'0')+nr;
}

