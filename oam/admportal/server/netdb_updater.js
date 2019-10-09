var log4js = require('log4js');
var http = require('http');
var async = require('async');
var properties = require(process.env.SDNC_CONFIG_DIR + '/netdb-updater.json');
var admProperties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');
var csvtojson = require('csvtojson');
var mysql = require('mysql');
var moment = require('moment');
var os = require('os');
var fs = require('fs.extra');

// Check to make sure SDNC_CONFIG_DIR is set
var sdnc_config_dir = process.env.SDNC_CONFIG_DIR;
if ( typeof sdnc_config_dir == 'undefined' )
{
    console.log('ERROR the SDNC_CONFIG_DIR environmental variable is not set.');
    return;
}

// SETUP LOGGER
log4js.configure(process.env.SDNC_CONFIG_DIR + '/netdb.log4js.json');
var logger = log4js.getLogger('netdb');
logger.setLevel(properties.netdbLogLevel);

var yargs = require('yargs')
  .usage("\nUsage: node netdb_updater -t link_master|router_master")
  .demand('t')
  .alias('t', 'table')
  .example("Example: node netdb_updater -t link_master","Update SDNC LINK_MASTER table from NetDB.")
  .argv;

var dbTable = yargs.table;
var debug = properties.netdbDebug;
var env = properties.netdbEnv; 
var retSuccess = false;

// DB Setup
var currentDB = '';
var dbConnection = '';
var db01 = '';
var db02 = '';
var count = 0;
var errorMsg = [];

var dbtasks = [];
dbtasks.push( function(callback) { checkParams(callback); } );
dbtasks.push( function(callback) { dbConnect(callback); } );
//dbtasks.push( function(callback) { netdb(callback); } );

logger.debug('\n\n********** START PROCESSING - Env=' + env + ' Debug=' + debug + ' **********');

async.series(dbtasks, function(err,result){
    if(err) {
		logger.error(err + ' COUNT: ' + count);
    }
    else {
		if ( errorMsg.length > 0 ){
			logger.error(errorMsg);
		}
	}
});


function checkParams(scb){
	if ( dbTable != 'link_master' && dbTable != 'router_master' ){
		scb("Invalid parameter passed in '" + dbTable + " ' exiting.'");
	}
	else{
		scb(null);
	}
}


async.whilst(
    	function () { return count < properties.netdbRetryInterval },
    	function (callback) {
			if ( dbTable == 'link_master' ){
				getLinkMaster(callback);
			}
			else if (dbTable == 'router_master'){
				getRouterMaster(callback);
			}
			else{ // should never hit this condition
				logger.debug("Invalid parameter passed in '" + dbTable + " ' exiting.'");
			}
    	},
    	function (err) {
logger.debug('whilst err function errorMsg = ' + errorMsg);
			// report error
			if ( errorMsg.length > 0 ){
				logger.debug(errorMsg + ' COUNT: ' + count);
				process.exit(1);
			}
			else{
				logger.debug('success');
				process.exit(0);
			}
    	}
);


function returnError(emsg, cb){
	retSuccess=false;
	errorMsg.push(emsg);
	if ( count == properties.netdbRetryInterval ) { logger.error(errorMsg); }
    setTimeout( function(){
    		cb(null);
		}, properties.netdbWaitTime);
}

function returnSuccess(cb){
logger.debug('inside returnSuccess');
	errorMsg = '';
	//var cnt = properties.netdbRetryInterval;
	//logger.debug('b4 inc returnSuccess count=' + count);
	//count = ++cnt;
	//logger.debug('after inc returnSuccess count=' + count);
	//cb(null);
	retSuccess=true;
process.exit(0);
} 

function getRouterMaster(cb){

	logger.info('getRouterMaster debug=' + debug + ' count=' + count);

	// setup connection
    var netdbEnv = properties.netdbEnv;
    var auth_param = '';
    if ( netdbEnv == 'e2e' || netdbEnv == 'prod' ){
        // conexus network
        auth_param = '?auth=' + admProperties['ip-addresses']['eth2'] + ';'
    }else{
        // app network
        auth_param = '?auth=' + admProperties['ip-addresses']['eth1:0'] + ';'
    }
    var username = properties.netdbUser;;
    var password = properties.netdbPassword;
	var date = moment().format('YYYYMMDD');
    var auth = 'Basic ' + new Buffer(username + ':' + password).toString('base64');
    var host = properties.netdbHost;
    var port = properties.netdbPort;
    var path = '/' + properties.netdbPath
                + '/' + properties.netdbNetwork
                + '/' + properties.netdbApiName
                + auth_param
                + 'client=' + properties.netdbClientName + ';'
                + 'date=' + date + ';'
                + 'reportName=' + dbTable + ';'
                + 'type=' + properties.netdbType;

    var header = { 'Content-Type': 'text/csv' };
    //var header = {'Host': host, 'Authorization': auth, 'Content-Type': 'text/csv' };
    var options = {
        method            : "GET",
        path              : path,
        host              : host,
        port              : port,
        headers           : header
    };

	logger.debug('options:\n' + JSON.stringify(options,null,2));

    var request = http.request(options, function(response) {

    	var response_str = '';
		if ( retSuccess == true ){
			var cnt = properties.netdbRetryInterval;
			count = ++cnt;
		}
		else{
			count++;
		}

        logger.debug('STATUS: ' + response.statusCode + ' content-type=' + response.headers['content-type']);

        // Read the response from ODL side
        response.on('data', function(chunk) {
            response_str += chunk;
        });

        response.on('end', function() {

			logger.debug('HEADERS:' + JSON.stringify(response.headers));

            if(response.statusCode == 200){

                if(response_str.length > 0){

					// save the upload
					try{
						fs.writeFileSync('/sdncvar/sdnc/files/netdb-updater/' + moment().unix() + ".netdb." + dbTable + '.csv', response_str);
					}
					catch(e){
						// this is not in reqs, if it fails keep on going.
						logger.error('Error writing NetDB file:' + e);
					}

					if (response.headers['content-type'].indexOf('html') > 0){
						returnError('Error:Unexpected content-type:' + response.headers['content-type'] + ' returned.\n', cb);
						return;
					}
                    // need to parse csv file
                    var Converter=csvtojson.Converter;
                    var csvConverter = new Converter({
                        noheader:true
                    });
                    var routerMasterSQL = '';

                    // end_parsed will be emitted once parsing is finished
                    csvConverter.on("end_parsed", function(respObj){

                        routerMasterSQL = routerMasterSQL.concat("INSERT INTO ROUTER_MASTER (crs_name, loopback_ip)");
                        for ( var x=0; x < respObj.length; x++ ){

                           	if ( respObj[x].field1.length == 0 ){
                               	returnError('Required field [crs_name] is null.', cb);
                           	}

							if (x!=0){
								routerMasterSQL = routerMasterSQL.concat(' union ');
							}
							routerMasterSQL = routerMasterSQL.concat(" SELECT " 
								+ "'" + respObj[x].field1 + "',"
                               	+ "'" + respObj[x].field2 + "' FROM DUAL ");
                        }
                        //logger.debug('SQL: ' + routerMasterSQL);
	
			if (debug != 'debug' && env != 'dev'){

                        	var tasks = [];
                        	tasks.push( function(callback) { updateRouterMaster(routerMasterSQL,callback); } );
                        	async.series(tasks, function(err,result){
                            	if(err) {
                                	returnError(err,cb);
					return;
                            	}
                            	else {
					logger.info('*** Router Master Table Replaced ***');
                                	returnSuccess(cb);
					return;
                            	}
                        	});
			}
			else{
logger.debug('*** debug ***');
                            	returnSuccess(cb);
					return;
			}

                    });
                    csvConverter.on("error",function(errMsg,errData){
                        returnError(errMsg,cb);
						return;
                    });
                    csvConverter.fromString(response_str, function(err,result){
                        if(err){
							returnError(err,cb);
							return;
                        }
                    });
                }
                else{
                    //logger.debug("no data");
					returnError('no data',cb);
					return;
                }
            }
            else if(response.statusCode == 404){
				returnError('Router Master Table for ' + date + ' is not Available.',cb);
				return;
			}
            else {
				returnError('Status Code:' + response.statudCode + ' returned for Router Master Table query.',cb);
				return;
            }
        });
    });
    request.on('error', function(e) {
	    if ( retSuccess == true ){
			var cnt = properties.netdbRetryInterval;
			count = ++cnt;
		}
		else{
			count++;
		}	
        returnError(e,cb);
		return;
    });
    request.end();
}

function getLinkMaster(cb){

	logger.info('getLinkMaster debug=' + debug + ' count=' + count);

    // setup connection
    var netdbEnv = properties.netdbEnv;
    var auth_param = '';
    if ( netdbEnv == 'e2e' || netdbEnv == 'prod' ){
        // conexus network
        auth_param = '?auth=' + admProperties['ip-addresses']['eth2'] + ';'
    }else{
        // app network
        auth_param = '?auth=' + admProperties['ip-addresses']['eth1:0'] + ';'
    }
    var username = properties.netdbUser;;
    var password = properties.netdbPassword;
    var auth = 'Basic ' + new Buffer(username + ':' + password).toString('base64');
    var host = properties.netdbHost;
    var port = properties.netdbPort;
	var date = moment().format('YYYYMMDD');
    var path = '/' + properties.netdbPath
                + '/' + properties.netdbNetwork
                + '/' + properties.netdbApiName
                + auth_param
                + 'client=' + properties.netdbClientName + ';'
                + 'date=' + date + ';'
                + 'reportName=' + dbTable + ';'
                + 'type=' + properties.netdbType;

    var header = { 'Content-Type': 'text/csv' };
    //var header = {'Host': host, 'Authorization': auth, 'Content-Type': 'text/csv' };
    var options = {
        method            : "GET",
        path              : path,
        host              : host,
        port              : port,
        headers           : header
    };

    logger.debug('options:\n' + JSON.stringify(options,null,2));

	var request = http.request(options, function(response) {

        logger.debug('STATUS: ' + response.statusCode + ' content-type=' + response.headers['content-type']);

		if ( retSuccess == true ){
        	var cnt = properties.netdbRetryInterval;
        	count = ++cnt;
    	}
		else{
    		count++
		}

        var response_str = '';

        // Read the response from ODL side
        response.on('data', function(chunk) {
            response_str += chunk;
        });

        response.on('end', function() {

			logger.debug('HEADERS:' + JSON.stringify(response.headers));

            if(response.statusCode == 200){

				if(response_str.length > 0){

					//logger.debug('response_str=' + response_str);
					// save the upload
                    try{
						fs.writeFileSync('/sdncvar/sdnc/files/netdb-updater/' + moment().unix() + ".netdb." + dbTable + '.csv', response_str);
                    }
                    catch(e){
                        // this is not in reqs, if it fails keep on going.
                        logger.error('Error writing NetDB file:' + e);
                    }

					if (response.headers['content-type'].indexOf('html') > 0){
						returnError('Error:Unexpected content-type:' + response.headers['content-type'] + ' returned.\n', cb);
						return;
					}
					// need to parse csv file
					var Converter=csvtojson.Converter;
					var csvConverter = new Converter({
						noheader:true
					});

					var linkMasterSQL = '';

					// end_parsed will be emitted once parsing is finished
					csvConverter.on("end_parsed", function(jsonObj){

						linkMasterSQL = linkMasterSQL.concat("INSERT INTO LINK_MASTER (link_interface_ip, source_crs_name, destination_crs_name, link_speed, default_cost, bundle_name, shutdown)"); 
						for ( var x=0; x < jsonObj.length; x++ ){
							if ( jsonObj[x].field1.length == 0 ){
								returnError('Required field [link_interface_ip] is null.', cb);
								return;
							}
							if ( jsonObj[x].field2.length == 0 ){
								returnError('Required field [source_crs_name] is null.', cb);
								return;
							}
							if ( jsonObj[x].field3.length == 0 ){
								returnError('Required field [destination_crs_name] is null.', cb);
								return;
							}
							if (x!=0){
								linkMasterSQL = linkMasterSQL.concat(' union ');
							}

							linkMasterSQL = linkMasterSQL.concat(" SELECT " 
								+ "'" + jsonObj[x].field1 + "',"
								+ "'" + jsonObj[x].field2 + "',"
								+ "'" + jsonObj[x].field3 + "',"
								+ jsonObj[x].field4 + ","
								+ jsonObj[x].field5 + ","
								+ "'" + jsonObj[x].field6 + "',"
								+ "'" + jsonObj[x].field7 + "' FROM DUAL");
						}
						//logger.debug('SQL: ' + linkMasterSQL);

						if (debug != 'debug' && env != 'dev'){
                    		// update db
							var tasks = [];
							tasks.push( function(callback) { updateLinkMaster(linkMasterSQL,callback); } );
    						async.series(tasks, function(err,result){
								if(err)
                            	{
                                	returnError(err,cb);
									return;
                            	}
                            	else
                            	{
									logger.info('*** Link Master Table Replaced ***');
                                	returnSuccess(cb);
									return;
                            	}
    						});
						}
						else{
                        	returnSuccess(cb);
							return;
						}
					});
					csvConverter.on("error",function(errMsg,errData){
    					returnError(errMsg,cb);
						return;
  					});
					csvConverter.fromString(response_str, function(err,result){
						if(err){
    						returnError(errMsg,cb);
							return;
						}
					});
                }
                else{
    				returnError('no data',cb);
					return;
                }
            }
            else if(response.statusCode == 404){
				returnError('Link Master Table for ' + date + ' is not Available.',cb);
				return;
			}
            else {
				returnError('Status Code:' + response.statudCode + ' returned for Link Master Table query.',cb);
				return;
            }
    	});
	});
	request.on('error', function(e) {
		if ( retSuccess == true ){
        	var cnt = properties.netdbRetryInterval;
        	count = ++cnt;
    	}
		else{
    		count++
		}
        returnError(e,cb);
        return;
	});
	request.end();
}
			

function dbConnect(callback){

        var l_db01 = admProperties['databases']['0'];
	var db01Array = l_db01.split("|");
	db01 = db01Array[0];

        var l_db02 = admProperties['databases']['1'];
	var db02Array = l_db02.split("|");
	db02 = db02Array[0];

	if ( admProperties.dbFabric == 'true' )
	{
		logger.debug('connectFabric()');

    // testing 
    var fabric_connection = mysql.createConnection({
        host            : admProperties.dbFabricServer,
        user            : admProperties.dbFabricUser,
        password        : admProperties.dbFabricPassword,
        database        : admProperties.dbFabricDB,
        port            : admProperties.dbFabricPort
    });


    fabric_connection.connect( function(err) {

        if (err) {
            callback(err);
            return;
        }
        fabric_connection.query('CALL dump.servers()', function(err,rows) {

            var masterDB = '';

            if (err) {
                callback(err);
                return;
            }
            fabric_connection.end();
            logger.debug('rows: ' + JSON.stringify(rows,null,2));

            // fabric servers
            for ( var x=0; x<rows.length; x++)
            {
                // database servers
                for ( var y=0; y<rows[x].length; y++)
                {
                    var row = rows[x][y];
                    if (row.group_id == admProperties.dbFabricGroupId)
                    {
                        if (row.status == '3' && row.mode == '3'){
                            masterDB = row.host;
                        }
                    }
                }
            }
            logger.debug('currentDB: ' + currentDB);
            logger.debug('masterDB: ' + masterDB);

            if (masterDB.length <=0)
            {
                logger.debug('no writable master db');
                callback('no writable master db');
                return;
            }

            if ( currentDB != masterDB )
            {
                currentDB = masterDB;
                dbConnection = mysql.createConnection({
                    connectionLimit   : admProperties.dbConnLimit,
                    host              : currentDB,
                    user              : admProperties.dbUser,
                    password          : admProperties.dbPassword,
                    database          : admProperties.dbName,
                    multipleStatements: true,
                    debug             : false
                });
            }
            logger.debug('new currentDB: ' + currentDB);
            logger.debug('new masterDB: ' + masterDB);
            callback(null);
            return;
        });
        fabric_connection.on('error', function(err){
             logger.debug(err.code);
             callback(err);
		     return;
        });
    });
	}
	else
	{
    	currentDB = db01;

    	var dbConn = mysql.createConnection({
        	connectionLimit : admProperties.dbConnLimit,
        	host            : currentDB,
        	user            : admProperties.dbUser,
        	password        : admProperties.dbPassword,
        	database        : admProperties.dbName,
        	multipleStatements: true,
        	debug           : false
    	});
		logger.debug('initDB currentDB=' + currentDB);

    	dbConn.connect(function(err,connection){

        	if(err){
            	logger.debug( String(err) ); // ALARM
            	callback(err);
            	return;
        	}
        	var sql = 'select @@read_only';
        	dbConn.query(sql, function(err,result){
            	dbConn.end();

            	// @@read_only=0 means db is writable
            	logger.debug('@@read_only=' + result[0]['@@read_only']);
            	if ( result[0]['@@read_only'] != '0' )
            	{
                	if (currentDB == db01)
                	{
                    	currentDB = db02;
                	}
                	else
                	{
                    	currentDB = db01;
                	}
logger.debug('initDB reconnect to currentDB '+ currentDB);
                	var newConnection = mysql.createConnection({
                    	connectionLimit : admProperties.dbConnLimit,
                    	host            : currentDB,
                    	user            : admProperties.dbUser,
                    	password        : admProperties.dbPassword,
                    	database        : admProperties.dbName,
                    	multipleStatements: true,
                    	debug           : false
                	}); // end create
                	dbConnection = newConnection;
                	callback(null);
					return;
            	}
                dbConnection = dbConn;
            	callback(null);
				return;
        	});
    	});
	}
}

function updateLinkMaster(linkMasterSQL,callback){

logger.debug('updateLinkMaster');

    dbConnection.connect(function(err,connection)
    {
        if(err){
            logger.debug( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.');
            return;
        }
    }); // end connection
    dbConnection.beginTransaction(function(err) {
    	if(err){
            //dbConnection.release();
            callback(err,String(err));
            return;
        }
        var sql = "DELETE FROM LINK_MASTER";
        dbConnection.query(sql,function(err,result)
        {
            if(err){
                //dbConnection.release();
                dbConnection.rollback( {rollback: 'NO RELEASE'},function(){
                    callback(err,String(err));
                    return;
                });
            }
            dbConnection.query(linkMasterSQL,function(err,result)
            {
                if(err){
                    //dbConnection.release();
                    dbConnection.rollback( {rollback: 'NO RELEASE'},function(){
                        callback(err,String(err));
                        return;
                    });
                }
                dbConnection.commit(function(err){
                    if(err){
                        //dbConnection.release();
                		dbConnection.rollback( {rollback: 'NO RELEASE'},function(){
                            callback(err,String(err));
                            return;
                        });
                    }
                    //dbConnection.release();
                    callback(null);
                });
            })
        });
    }); // end transaction
}

function updateRouterMaster(routerMasterSQL,callback){

logger.debug('updateRouterMaster');

    dbConnection.connect(function(err,connection)
    {
        if(err){
            logger.debug( String(err) ); // ALARM
            callback(err, 'Unable to get database connection.');
            return;
        }
    }); // end connection
    dbConnection.beginTransaction(function(err) {
        if(err){
            //dbConnection.release();
            callback(err,String(err));
            return;
        }
        var sql = "DELETE FROM ROUTER_MASTER";
        dbConnection.query(sql,function(err,result)
        {
            if(err){
                //dbConnection.release();
                dbConnection.rollback( function(){
                    callback(err,String(err));
                    return;
                });
            }
            dbConnection.query(routerMasterSQL,function(err,result)
            {
                if(err){
                    //dbConnection.release();
                	dbConnection.rollback( function(){
                        callback(err,String(err));
                        return;
                    });
                }
                dbConnection.commit(function(err){
                    if(err){
                        //dbConnection.release();
                		dbConnection.rollback( function(){
                            callback(err,String(err));
                            return;
                        });
                    }
                    //dbConnection.release();
                    callback(null);
                });
            })
        });
    }); // end transaction
}

/*

	logger.debug('getLinkMaster - count=' + count);
	if ( true ) {
		//fail
		setTimeout( function(){
			cb(null);}, properties.netdbWaitTime);
		return;
	}
	// success
	count = 10;
	cb(null);
}
*/
