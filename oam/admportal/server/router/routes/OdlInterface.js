// OdlInterface.js
var https = require('https');
var http = require('http');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');


var OdlInterface = function() {
    // Call ODL page
    //  get(uri,res)
    //  post(uri,data, res)

	var _healthcheck = function(options, callback)
	{
		// Setup request to ODL side
		console.log('options:' + JSON.stringify(options,null,2));
		var request = https.request(options, function(response) {
			
			var response_str = '';
	    	// Read the response from ODL side
	    	response.on('data', function(chunk) {
				response_str += chunk;
	    	});
			
			response.on('end', function() 
			{
				//logger.debug(response_str);
				if(response.statusCode == 200)
				{
					console.log('200 OK');
					callback(null, response_str);
					return;
				}
				else{
					console.log('not ok status=' + response.statusCode);
					callback(response_str, 'statusCode=' + response.statusCode + '\n' + response_str);
					return;
				}
			});
		});
		request.end()

		request.on('error', function(err) {
			console.error('err:' + err);
			callback(err, err);
			return;
		});
	}

    var _get = function(uri,options,callback,req,res) 
	{

		options.method = "GET";
		options.path = uri;

		// Setup request to ODL side
		console.log('options:' + JSON.stringify(options,null,2));
		var request = https.request(options, function(response) {
	    	// console.dir(response);
  	    	console.log('STATUS: ' + response.statusCode);
	    	var response_str = '';
	    	// Read the response from ODL side
	    	response.on('data', function(chunk) {
				response_str += chunk;
	    	});

	    	response.on('end', function() 
			{
				console.log(response_str);

				if(response.statusCode == 200)
				{
				// make sure response starts with JSON string
        		if (response_str && response_str.indexOf('{') == 0) {
            		//console.log("response: ", result);
						
					req.session.l3sdnPageInfo = undefined;
					var x=0;
					var pageList = [];
					var obj_rows = [];
					var rows = [];
					var robj = JSON.parse(response_str);

					if ( typeof robj['services']['layer3-service-list'] != 'undefined' )
					{
						for( var i=0; i<robj['services']['layer3-service-list'].length; i++)
						{
							obj_rows.push( robj['services']['layer3-service-list'][i] );
						}
					}
					else
					{
            			res.render('pages/err', {result:{code:'failure', msg:'no data Error: ' + String(err)}, header:process.env.MAIN_MENU});
						callback(null,response_str,res);
						return;
					}
 
        			var rows = [];
					var l3sdnPageInfo =
        			{
            			'totalRows'   : obj_rows.length,
            			'pageSize'    : 18,
            			'pageCount'   : parseInt(obj_rows.length/18),
            			'currentPage' : 1
        			}

					while (obj_rows.length > 0){
            			pageList.push(obj_rows.splice(0,l3sdnPageInfo.pageSize));
        			}
					l3sdnPageInfo.rows = pageList[0]; // first page
					l3sdnPageInfo.pages = pageList; // all pages


					req.session.l3sdnPageInfo = l3sdnPageInfo;
					var privObj = req.session.loggedInAdmin;

            		res.render('odl/listWklst', 
					{
            			pageInfo  : l3sdnPageInfo,
            			result    : {code:'', msg:''},
						privilege : privObj, header:process.env.MAIN_MENU
    				});


        		} else {
            		res.render('pages/err', {result:{code:'failure', msg:'no data Error: ' + String(err)}, header:process.env.MAIN_MENU});
        		}
	        	callback(null, response_str, res);
			}
			else
			{
				callback(response_str,response_str,res);
				//res.render('pages/err', {result:{code:'failure', msg:'Failed to retrieve worklist. ' + response_str}});
			}
	    	});
		});

		request.end()

		request.on('error', function(err) {
	    	callback(err,null, res);
		});
    }

    var _getid = function(uri,options,res) {
	options.method = "GET";
	options.path = uri;

	// Setup request to ODL side
	var request = https.request(options, function(response) {
	    // console.dir(response);
  	    //console.log('STATUS: ' + response.statusCode);
	    var response_str = '';
	    // Read the response from ODL side
	    response.on('data', function(chunk) {
			response_str += chunk;
	    });

	    response.on('end', function() {
			if(response.statusCode == 200){
				if(response_str){
					//console.log("response: ", response_str);
					res.render('odl/view', {response_obj: JSON.parse(response_str), header:process.env.MAIN_MENU});
				}
				else{
					//console.log("no data");
					res.render('pages/err', {result:{code:'failure', msg:'Failed to retrieve worklist'}, header:process.env.MAIN_MENU});
				}
			}
			else {
				//console.log("bad status code:", response.statusCode);
				res.render('pages/err', {result:{code:'failure', msg:'Failed to retrieve worklist. Status Code:' + response.statusCode}, header:process.env.MAIN_MENU});
			}
	    });
	});

	request.end()

	request.on('error', function(err) {
		//console.log(err);
		res.render('pages/err', {result:{code:'failure', msg:'Failed to get worklist item. ' + String(err)}, header:process.env.MAIN_MENU});
	});
    }

	var _getvrlan = function(uri,options,callback) {
    options.method = "GET";
    options.path = uri;


	//callback(null,'');	
    // Setup request to ODL side
    var request = https.request(options, function(response) {
        // console.dir(response);
        //console.log('STATUS: ' + response.statusCode);
        var response_str = '';
        // Read the response from ODL side
        response.on('data', function(chunk) {
            response_str += chunk;
        });


        response.on('end', function() {
            if(response.statusCode == 200){
                if(response_str){
					callback(null,response_str);
					return;
                }
                else{
					callback('err','no data');
					return;
                }
            }
            else {
				callback('error',response.statusCode);
				return;
            }
        });
    });

    request.end()

    request.on('error', function(err) {
		callback(err,String(err));
		return;
    });
    }


	var _getvnf = function(uri,options,req,res) {
    options.method = "GET";
    options.path = uri;

    // Setup request to ODL side
    var request = https.request(options, function(response) {
        // console.dir(response);
        //console.log('STATUS: ' + response.statusCode);
        var response_str = '';
        // Read the response from ODL side
        response.on('data', function(chunk) {
            response_str += chunk;
        });

        response.on('end', function() {
            if(response.statusCode == 200){
                if(response_str){
                    //console.log("response: ", response_str);
                    res.render('odl/viewvnf', { vnf_obj: JSON.parse(response_str),
											   request_id: req.query['request'], header:process.env.MAIN_MENU });
                }
                else{
                    //console.log("no data");
					res.render('pages/err', {result:{code:'failure', msg:'Failed to retrieve worklist item.'}, header:process.env.MAIN_MENU});
                }
            }
            else {
                //console.log("bad status code:", response.statusCode);
				res.render('pages/err', {result:{code:'failure', msg:'Failed to retrieve worklist. Status Code:' + response.statusCode}, header:process.env.MAIN_MENU});
            }
        });
    });

    request.end()

    request.on('error', function(err) {
        //console.log(err);
        res.render('pages/err', {result:{code:'failure', msg:'Failed getting VNF information. Error: '+ String(err)}, header:process.env.MAIN_MENU});
    });
    }

	var _getPreloadVnfData = function(uri,options,res,callback) {

    options.method = "GET";
    options.path = uri;


    // Setup request to ODL side
	var protocol;
	if ( process.env.NODE_ENV != 'production' ){
    	protocol = http;
	}else{
    	protocol = https;
	}

console.log('NODE_ENV:' + process.env.NODE_ENV);
console.log('GET: ' + JSON.stringify(options,null,4));

    var request = protocol.request(options, function(response) {

        var response_str = '';

        // Read the response from ODL side
        response.on('data', function(chunk) {
            response_str += chunk;
        });

        response.on('end', function() {
console.log('response_str: ' + response_str);
console.log('response.statusCode: ' + response.statusCode);
            if(response.statusCode == 200){
                if(response_str){
						callback(null,response_str);
						return;
				}
                else{
					callback('Error - No data returned.');
					return;
                }
            }
            else {
				if ( response.statusCode == 404 )
				{
					callback('HTTP Status Code:' + response.statusCode + '.  Not Found.');
					return;
				}
				else if ( response_str.length > 0 )
				{
					callback('HTTP Status Code:' + response.statusCode + '.  ' + response_str);
					return;
				}
				else
				{
					callback('HTTP Status Code:' + response.statusCode + '.  No data returned.');
					return;
				}
            }
        });
    });

    request.end()

    request.on('error', function(err) {
		callback(err);
		return;
    });
    }

var _getClusterStatus = function(options,callback) {
    //options.method = "GET";
    //options.path = uri;

    console.log('URI='+options.path);

    // Setup request to ODL side
    var protocol = properties.odlProtocol;
console.log('protocol=' + protocol);
    if ( protocol == 'http' || protocol == 'HTTP' )
    {
console.log('http request');
	var request = http.request(options, function(response) {
        	var response_str = '';

        	// Read the response from ODL side
        	response.on('data', function(chunk) {
            		response_str += chunk;
        	});


        	response.on('end', function() {
console.log('HTTP StatusCode='+response.statusCode);
            		if(response.statusCode == 200){
                		if(response_str){
console.log('response_str='+response_str);
                    			callback(null,JSON.parse(response_str));
                    			return;
                		}
                		else{
                    			callback(null,'no data');
                    			return;
                		}
            		}
            		else {
                		callback(null,response.statusCode);
                		return;
            		}
        	});
    		});

    	request.end()

    	request.on('error', function(err) {
        	callback(null,String(err));
        	return;
    	});
    }
    else {
	var request = https.request(options, function(response) {
                var response_str = '';

                // Read the response from ODL side
                response.on('data', function(chunk) {
                        response_str += chunk;
                });


                response.on('end', function() {
                        if(response.statusCode == 200){
                                if(response_str){
console.log('response_str='+response_str);
                                        callback(null,JSON.parse(response_str));
                                        return;
                                }
                                else{
                                        callback(null,'no data');
                                        return;
                                }
                        }
                        else {
                                callback(null,response.statusCode);
                                return;
                        }
                });
                });

        request.end()

        request.on('error', function(err) {
                callback(null,String(err));
                return;
        });
    }
}

    var _delete = function(uri,options,res,callback) {
		options.method = 'DELETE';
		options.path = uri;


        // Setup request to ODL side
        //var request = https.request(options, function(response) {
        var request = http.request(options, function(response) {
            //console.log('STATUS: ' + response.statusCode);
            var response_str = '';
            // Read the response from ODL side
            response.on('data', function(chunk) {
                response_str += chunk;
            });

            response.on('end', function() {
				if(response.statusCode == 200){
                    callback(null);
                    return;
                }
                else {
                    callback('Error:' + response_str);
                    return;
                }
            });
        });
		request.on('error', function(err) {
	    	callback(err);
			return;
		});
        request.end()
    }

    var _post = function(uri,options,data,res,callback)
	{
		options.method = 'POST';
		options.path = uri;


		// Setup request to ODL side
		var protocol;
		//if ( process.env.NODE_ENV != 'production' ){
    		protocol = http;
		//}else{
    		//protocol = https;
		//}
		var request = protocol.request(options, function(response)
		{
	    	var response_str = '';
	    	// Read the response from ODL side
	    	response.on('data', function(chunk) {
				response_str += chunk;
                //logger.debug('chunk:' + chunk);
	    	});

	    	// end of request, check response
	    	response.on('end', function() {
				console.log('post status code:'+response.statusCode);
				if(response.statusCode == 200 ){
                    try {
				        var respObj = JSON.parse(response_str);
                        console.log('response_str.length:' + response_str.length);
					    if(response_str){
	        			    console.log("post response-code:" + respObj['output']['response-code']);
						
                    	    if ( respObj['output']['response-code'] == 200 ){
                        	    callback(null,response_str);
                        	    return;
                    	    }else{
                        	    callback('Error - response-code:' + respObj['output']['response-code'] + '  response-message:' + respObj['output']['response-message']);
                        	    return;
                    	    }
					    }else{
						    // success
	            		    callback(null);
						    return;
					    }
                    }
                    catch (error) {
                        callback('Error parsing response: ' + error);
                        return;
                    }
				}
            	else {
				    var respObj = null; 
                    if ( response_str.length > 0 ) {
                        console.log('response_str:' + response_str);
				        try {
                            respObj = JSON.parse(response_str);
                            if ( typeof respObj['errors'] != 'undefined' )
					        {
                                console.log('Error' + JSON.stringify(respObj));
						        if ( typeof respObj['errors']['error'][0]['error-message'] != 'undefined' )
						        {
							        callback('HTTP Status Code:' + response.statusCode + '. Message:' 
								        + respObj['errors']['error'][0]['error-message']);
                			        return;
						        }
						        else
						        {
							        callback('Error - HTTP Status Code:' + response.statusCode + '.');
                			        return;
						        }
					        }
					        else
					        {
						        callback('Error - HTTP Status Code:' + response.statusCode + '.');
                		        return;
					        }
                        }
                        catch (error) {
						    callback('Error - HTTP Status Code:' + response.statusCode + '.');
                		    return;
                        }
                    }
                    else{
						callback('Error - HTTP Status Code:' + response.statusCode + '.');
                		return;
					}
            	}
	    	});
		});

		request.on('error', function(err) {
	    	callback(err);
			return;
		});

    	request.write(data);
		request.end()
    }

    var _postSubnetAllocated = function(uri,options,data,callback,res) {
        options.method = 'POST';
        options.path = uri;

        // Setup request to ODL side
        var request = https.request(options, function(response) {
        var response_str = '';
        // Read the response from ODL side
        response.on('data', function(chunk) {
        response_str += chunk;
        });

        // end of request, check response
        response.on('end', function() {
            //console.log("post: response: " + response_str);
             callback(response_str,null,res);
        });
    });
    request.on('error', function(err) {
        callback(err,null, res);
    });
    request.write(data);
    request.end()
    }

    return {
	GetClusterStatus: _getClusterStatus,
	Get: _get,
	GetID: _getid,
	GetVNF: _getvnf,
	GetVRlan: _getvrlan,
	GetPreloadVnfData: _getPreloadVnfData,
	Post: _post,
	PostSubnetAllocated: _postSubnetAllocated,
	Delete: _delete,
	Healthcheck: _healthcheck
    };
}();

module.exports = OdlInterface;

