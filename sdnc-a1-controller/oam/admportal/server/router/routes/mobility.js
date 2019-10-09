var express = require('express');
var router = express.Router();
var exec = require('child_process').exec;
var util = require('util');
var fs = require('fs.extra');
var dbRoutes = require('./dbRoutes');
var csp = require('./csp');
var multer = require('multer');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var sax = require('sax'),strict=true,parser = sax.parser(strict);
var async = require('async');
var l_ = require('lodash');
var dateFormat = require('dateformat');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');
var crypto = require('crypto');
var csrf = require('csurf');

var csrfProtection = csrf({cookie: true});
router.use(cookieParser())

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

// Connection to OpenDaylight
OdlInterface = require('./OdlInterface');

// used for file upload button, retain original file name
//router.use(bodyParser());
//router.use(bodyParser.urlencoded({
  //extended: true
//}));

//var upload = multer({ dest: process.cwd() + '/uploads/', rename: function(fieldname,filename){ return filename; } });

// multer 1.1
var storage = multer.diskStorage({
  destination: function (req, file, cb) {
    cb(null, process.cwd() + '/uploads/')
  },
  filename: function (req, file, cb) {
    cb(null, file.originalname )
  }
});

var upload = multer({
    storage: storage
});


// GET
router.get('/getVnfData', csp.checkAuth, csrfProtection, function(req,res) {
	dbRoutes.getVnfData(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
});
router.get('/getVnfNetworkData', csp.checkAuth, csrfProtection, function(req,res) {
	dbRoutes.getVnfNetworkData(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
});
router.get('/getVnfProfile', csp.checkAuth, csrfProtection, function(req,res) {
	dbRoutes.getVnfProfile(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
});
//router.get('/getVmNetworks', csp.checkAuth, function(req,res) {
//	dbRoutes.getVmNetworks(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
//});
//router.get('/getVnfNetworks', csp.checkAuth, function(req,res) {
//	dbRoutes.getVnfNetworks(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
//});
//router.get('/getVmProfile', csp.checkAuth, function(req,res) {
//	dbRoutes.getVmProfile(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
//});
////////

router.get('/viewVnfNetworkData', csp.checkAuth, csrfProtection, function(req,res)
{
    var privilegeObj = req.session.loggedInAdmin;
    var resp_msg = '';
    var network_name = req.query.network_name;
    var network_type = req.query.network_type;
    var tasks = [];

    tasks.push(function(callback){
		OdlInterface.GetPreloadVnfData('/restconf/config/VNF-API:preload-vnfs/vnf-preload-list/'
            + encodeURIComponent(network_name) + '/' + encodeURIComponent(network_type) + '/', options,res,callback);

    });
    async.series(tasks, function(err,result)
    {
        var msgArray = new Array();
        if(err){
            resp_msg = err;
            res.render('mobility/displayVnfNetworkData', {result:{code:'failure', msg:resp_msg}, header:process.env.MAIN_MENU});
            return;
        }
        else{
            resp_msg = JSON.stringify(JSON.parse(result[0],null,4));
            res.render('mobility/displayVnfNetworkData', {result:{code:'success', msg:JSON.parse(result[0])}, header:process.env.MAIN_MENU});
            return;
        }
    });

});

router.get('/viewVnfData', csp.checkAuth, csrfProtection, function(req,res) 
{
    var privilegeObj = req.session.loggedInAdmin;
    var resp_msg = '';
	var vnf_name = req.query.vnf_name;
	var vnf_type = req.query.vnf_type;
	var tasks = [];

	tasks.push(function(callback){
		OdlInterface.GetPreloadVnfData('/restconf/config/VNF-API:preload-vnfs/vnf-preload-list/'
			+ encodeURIComponent(vnf_name) + '/' + encodeURIComponent(vnf_type) + '/', options,res,callback);

	});
	async.series(tasks, function(err,result)
    {
        var msgArray = new Array();
        if(err){
            resp_msg = err;
			res.render('mobility/displayVnfData', {result:{code:'failure', msg:resp_msg}, header:process.env.MAIN_MENU});
            return;
        }
        else{
			resp_msg = JSON.stringify(JSON.parse(result[0],null,4));
			res.render('mobility/displayVnfData', {result:{code:'success', msg:JSON.parse(result[0])}, header:process.env.MAIN_MENU});
            return;
        }
    });

});

router.get('/loadVnfNetworkData', csp.checkAuth, csp.checkPriv, function(req,res)
{
	var privilegeObj = req.session.loggedInAdmin;
	var msgArray = new Array();

	if ( req.query.status != 'pending' )
	{
		msgArray.push("Upload Status must be in 'pending' state.");
		dbRoutes.getVnfNetworkData(req,res, {code:'failure', msg:msgArray}, privilegeObj);
		return;
	}

	// build request-id
	var now = new Date();
	var df = dateFormat(now,"isoDateTime");
	const rnum = crypto.randomBytes(4);
	var svc_req_id = req.query.id + "-" + df + "-" + rnum.toString('hex');;
	var tasks = [];

	// first get the contents of the file from the db
	tasks.push(function(callback){
		dbRoutes.getVnfPreloadData(req,res,"PRE_LOAD_VNF_NETWORK_DATA",callback);
	});

	// then format the request and send it using the arg1 parameter
	// which is the contents of the file returned from the previous function
	// call in the tasks array
	tasks.push(function(arg1,callback){

		var s_file = JSON.stringify(arg1);

		// remove the last two braces, going to add the headers there
		// will add them back later.
		s_file = s_file.substring(0, (s_file.length-2));

		// add the request-information header
		s_file = s_file.concat(',"request-information": {"request-action": "PreloadNetworkRequest"}');

		// add the sdnc-request-header
		s_file = s_file.concat(',"sdnc-request-header": {"svc-request-id":"');
		s_file = s_file.concat(svc_req_id);
		s_file = s_file.concat('","svc-action": "reserve"}');

		// add the two curly braces at the end that we stripped off
		s_file = s_file.concat('}}');

		OdlInterface.Post('/restconf/operations/VNF-API:preload-network-topology-operation', 
			options,s_file,res,callback);
	});

	// if successful then update the status
	tasks.push(function(arg1,callback){
		dbRoutes.executeSQL("UPDATE PRE_LOAD_VNF_NETWORK_DATA SET status='uploaded',svc_request_id='"
            + svc_req_id + "',svc_action='reserve' WHERE id="+req.query.id,req,res,callback);
	});

	// use the waterfall method of making calls
	async.waterfall(tasks, function(err,result)
	{
		var msgArray = new Array();
		if(err){
			msgArray.push("Error posting pre-load data to ODL: "+err);
			dbRoutes.getVnfNetworkData(req,res, {code:'failure', msg:msgArray}, privilegeObj);
			return;
		}
		else{
			msgArray.push('Successfully loaded VNF pre-loaded data.');
			dbRoutes.getVnfNetworkData(req,res,{code:'success', msg:msgArray},privilegeObj);
			return;
		}
	});
});


router.get('/loadVnfData', csp.checkAuth, csp.checkPriv, function(req,res) 
{
	var privilegeObj = req.session.loggedInAdmin;
	var full_path_file_name = process.cwd() + "/uploads/" + req.query.filename
  var msgArray = new Array();

	if ( req.query.status != 'pending' )
	{
		msgArray.push("Upload Status must be in 'pending' state.");
		dbRoutes.getVnfData(req,res, {code:'failure', msg:msgArray}, privilegeObj);
		return;
	}

	// build request-id
	var now = new Date();
	var df = dateFormat(now,"isoDateTime");
	const rnum = crypto.randomBytes(4);
	var svc_req_id = req.query.id + "-" + df + "-" + rnum.toString('hex');
	var tasks = [];

	// first get the contents of the file from the db
	tasks.push(function(callback){
		dbRoutes.getVnfPreloadData(req,res,"PRE_LOAD_VNF_DATA",callback);
  });

	// then format the request and send it using the arg1 parameter
	// which is the contents of the file returned from the previous function
	// call in the tasks array
	tasks.push(function(arg1,callback){

		var s1_file = JSON.stringify(arg1);
		var s_file = decodeURI(s1_file);


		// remove the last two braces, going to add the headers there
   	// will add them back later.
    s_file = s_file.substring(0, (s_file.length-2));

		// add the request-information header
		s_file = s_file.concat(',"request-information": {"request-action": "PreloadVNFRequest"}');

		// add the sdnc-request-header
		s_file = s_file.concat(',"sdnc-request-header": {"svc-request-id":"');
		s_file = s_file.concat(svc_req_id);
		s_file = s_file.concat('","svc-action": "reserve"}');

		// add the two curly braces at the end that we stripped off
		s_file = s_file.concat('}}');

		OdlInterface.Post('/restconf/operations/VNF-API:preload-vnf-topology-operation',
			options,s_file,res,callback);
	});

	// if successful then update the status
	tasks.push(function(arg1,callback){
		dbRoutes.executeSQL("UPDATE PRE_LOAD_VNF_DATA SET status='uploaded',svc_request_id='"
			+ svc_req_id + "',svc_action='reserve' WHERE id="+req.query.id,req,res,callback);
	});

	// use the waterfall method of making calls
	async.waterfall(tasks, function(err,result)
	{
		var msgArray = new Array();
		if(err){
			msgArray.push("Error posting pre-load data to ODL: "+err);
      dbRoutes.getVnfData(req,res, {code:'failure', msg:msgArray}, privilegeObj);
      return;
		}
		else{
			msgArray.push('Successfully loaded VNF pre-loaded data.');
      dbRoutes.getVnfData(req,res,{code:'success', msg:msgArray},privilegeObj);
      return;
    }
	});
});


router.get('/deleteVnfNetworkData', csp.checkAuth, csp.checkPriv, csrfProtection,  function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    var sql = 'DELETE FROM PRE_LOAD_VNF_NETWORK_DATA WHERE id=' + req.query.id;

    // if status is pending, then we do not have to call
    // ODL, just remove from db
    if (req.query.status == 'pending'){
        tasks.push(function(callback) {
            dbRoutes.executeSQL(sql,req,res,callback);
        });
    } else {
		// format the request to ODL
        var inputString = '{"input":{"network-topology-information":{"network-topology-identifier":{"service-type":"SDN-MOBILITY","network-name": "';
        inputString = inputString.concat(req.query.network_name);
        inputString = inputString.concat('","network-type":"');
        inputString = inputString.concat(req.query.network_type);
        inputString = inputString.concat('"}},');

        // add the request-information header
        inputString = inputString.concat('"request-information": {"request-action": "DeletePreloadNetworkRequest"},');

		// add the sdnc-request-header
		inputString = inputString.concat('"sdnc-request-header": {"svc-request-id":"');
		inputString = inputString.concat(req.query.svc_request_id);
		inputString = inputString.concat('","svc-action": "delete"}}}');
	
        tasks.push(function(callback) {
            OdlInterface.Post('/restconf/operations/VNF-API:preload-network-topology-operation',
                    options,inputString,res,callback);
        });
        tasks.push(function(callback) {
            dbRoutes.executeSQL(sql,req,res,callback);
        });
    }
    async.series(tasks, function(err,result){

        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVnfNetworkData(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from PRE_LOAD_VNF_NETWORK_DATA table and ODL.');
            dbRoutes.getVnfNetworkData(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


router.get('/deleteVnfData', csp.checkAuth, csp.checkPriv, csrfProtection, function(req,res) {

console.log('deleteVnfData');

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    var sql = 'DELETE FROM PRE_LOAD_VNF_DATA WHERE id=' + req.query.id;

    // if status is pending, then we do not have to call
    // ODL, just remove from db
    if (req.query.status == 'pending'){
        tasks.push(function(callback) {
            dbRoutes.executeSQL(sql,req,res,callback);
        });
    } else {
			var inputString = '{"input":{"vnf-topology-information":{"vnf-topology-identifier":{"service-type":"SDN-MOBILITY","vnf-name": "';
			inputString = inputString.concat(req.query.vnf_name);
			inputString = inputString.concat('","vnf-type":"');
			inputString = inputString.concat(req.query.vnf_type);
			inputString = inputString.concat('"}},');
		
      // add the request-information header
      inputString = inputString.concat('"request-information": {"request-action": "DeletePreloadVNFRequest"},');

    	// add the request-information header
    	//inputString = inputString.concat('"request-information": {"request-id": "259c0f93-23cf-46ad-84dc-162ea234fff1",');
		//inputString = inputString.concat('"source": "ADMINPORTAL",');
		//inputString = inputString.concat('"order-version": "1",');
		//inputString = inputString.concat('"notification-url": "notused-this would be infrastructure portal",');
		//inputString = inputString.concat('"order-number": "1",');
		//inputString = inputString.concat('"request-action": "DeletePreloadVNFRequest"},');

		// add the sdnc-request-header
		inputString = inputString.concat('"sdnc-request-header": {"svc-request-id":"');
		inputString = inputString.concat(req.query.svc_request_id);
		inputString = inputString.concat('","svc-action": "delete"}}}');

		//inputString = inputString.concat('"sdnc-request-header":{');
		//inputString = inputString.concat('"svc-request-id": "2015-01-15T14:34:54.st1101a",');
		//inputString = inputString.concat('"svc-notification-url": "not used",');
		//inputString = inputString.concat('"svc-action": "delete"}}}');
		
        tasks.push(function(callback) {
        	OdlInterface.Post('/restconf/operations/VNF-API:preload-vnf-topology-operation',
                    options,inputString,res,callback);
        });
        tasks.push(function(callback) {
            dbRoutes.executeSQL(sql,req,res,callback);
        });
    }
    async.series(tasks, function(err,result){

        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVnfData(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from PRE_LOAD_VNF_DATA table and ODL.');
            dbRoutes.getVnfData(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


router.get('/deleteVnfNetwork', csp.checkAuth, csp.checkPriv, csrfProtection, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    var sql = '';

    sql = "DELETE FROM VNF_NETWORKS WHERE vnf_type='" + req.query.vnf_type + "'"
		+ " AND network_role='" + req.query.network_role + "'";

    tasks.push(function(callback) {
        dbRoutes.executeSQL(sql,req,res,callback);
    });
    async.series(tasks, function(err,result)
    {
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVnfNetwork(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from VNF_NETWORKS table.');
            dbRoutes.getVnfNetworks(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

router.get('/deleteVnfProfile', csp.checkAuth, csp.checkPriv, csrfProtection, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    var sql = '';

    sql = "DELETE FROM VNF_PROFILE WHERE vnf_type='" + req.query.vnf_type + "'";

    tasks.push(function(callback) {
        dbRoutes.executeSQL(sql,req,res,callback);
    });
    async.series(tasks, function(err,result)
    {
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVnfProfile(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from VNF_PROFILE table.');
            dbRoutes.getVnfProfile(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// POST
router.post('/addVnfProfile', csp.checkAuth, csp.checkPriv, csrfProtection, function(req,res){

  var privilegeObj = req.session.loggedInAdmin;
	var vnf_type = req.sanitize(req.body.nf_vnf_type);
	var availability_zone_count = req.sanitize(req.body.nf_availability_zone_count);
  var equipment_role = req.sanitize(req.body.nf_equipment_role);
  var tasks = [];
	var sql;

  sql = "INSERT INTO VNF_PROFILE (vnf_type,availability_zone_count,equipment_role) VALUES ("
       	+ "'" + vnf_type + "'," + availability_zone_count + ",'" + equipment_role + "')"; 

console.log(sql);

	tasks.push( function(callback) { dbRoutes.executeSQL(sql,req,res,callback); } );
	async.series(tasks, function(err,result){
		var msgArray = new Array();
		if(err){
			msgArray.push(err);
			dbRoutes.getVnfProfile(req,res,{code:'failure', msg:msgArray},privilegeObj);
			return;
		}
		else {
			msgArray.push('Successfully added VNF Profile');
			dbRoutes.getVnfProfile(req,res,{code:'success', msg:msgArray},privilegeObj);
			return;
		}
	});
});

// POST
router.post('/uploadVnfData', csp.checkAuth, csp.checkPriv, upload.single('filename'), function(req, res)
{
console.log('filename:'+ JSON.stringify(req.file.originalname));
    var msgArray = new Array();
    var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname)
	{
        if (req.file.originalname.size == 0) {
			msgArray.push('There was an error uploading the file.');
            dbRoutes.getVnfData(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        fs.exists(req.file.path, function(exists) 
		{
            if(exists) 
			{
                var str = req.file.originalname;
				var content;
                var enc_content;
			
				try{
                    content = fs.readFileSync(req.file.path);
                    enc_content = encodeURI(content);


                    var sql = "INSERT INTO PRE_LOAD_VNF_DATA "
                        + "(filename,preload_data) VALUES ("
                        + "'"+ str + "'," + "'" + enc_content + "')";

                	var privilegeObj = req.session.loggedInAdmin;
                	var tasks = [];
                	tasks.push( function(callback) { dbRoutes.addRow(sql,req,res,callback); } );
                	async.series(tasks, function(err,result)
                	{
                    	if(err){
                        	msgArray.push(err);
                        	dbRoutes.getVnfData(req,res,{code:'failure', msg:msgArray},privilegeObj);
                        	return;
                    	}
                    	else {
                        	msgArray.push('Successfully uploaded ' + str);
                        	dbRoutes.getVnfData(req,res,{code:'success', msg:msgArray},privilegeObj);
                        	return;
                    	}
                	});
				}
				catch(error){
						fs.removeSync(req.file.path); // remove bad file that was uploaded
						console.error("There was an error reading the file '"+str+"'. Error: " + error);
						msgArray.push("There was an error reading the file '"+str+"'. Error: " + error);
            			dbRoutes.getVnfData(req,res,{code:'failure', msg:msgArray},privilegeObj);
            			return;
				}
            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getVnfData(req,res,{code:'danger', msg:msgArray},privilegeObj);
                return;
            }
        });
 	}
    else 
	{
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getVnfData(req,res,{code:'danger', msg:msgArray},privilegeObj);
        return;
    }

} );

router.post('/uploadVnfNetworkData', csp.checkAuth, csp.checkPriv, upload.single('filename'), function(req, res)
{
    var msgArray = new Array();
    var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname)
    {
        if (req.file.originalname.size == 0) {
            msgArray.push('There was an error uploading the file.');
            dbRoutes.getVnfData(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        fs.exists(req.file.path, function(exists)
        {
            if(exists)
            {
                var str = req.file.originalname;
                var content;
                var enc_content;

                try{
                    content = fs.readFileSync(req.file.path);
                    enc_content = encodeURI(content);

                    var sql = "INSERT INTO PRE_LOAD_VNF_NETWORK_DATA "
                        + "(filename,preload_data) VALUES ("
                        + "'"+ str + "'," + "'" + enc_content + "')";

                    var privilegeObj = req.session.loggedInAdmin;
                    var tasks = [];
                    tasks.push( function(callback) { dbRoutes.addRow(sql,req,res,callback); } );
                    async.series(tasks, function(err,result)
                    {
                        if(err){
                            msgArray.push(err);
                            dbRoutes.getVnfNetworkData(req,res,{code:'failure', msg:msgArray},privilegeObj);
                            return;
                        }
                        else {
                            msgArray.push('Successfully uploaded ' + str);
                            dbRoutes.getVnfNetworkData(req,res,{code:'success', msg:msgArray},privilegeObj);
                            return;
                        }
                    });
                }
                catch(error){
                        fs.removeSync(req.file.path); // remove bad file that was uploaded
                        msgArray.push("There was an error reading the file '"+str+"'. Error: " + error);
                        dbRoutes.getVnfNetworkData(req,res,{code:'failure', msg:msgArray},privilegeObj);
                        return;
                }
            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getVnfNetworkData(req,res,{code:'danger', msg:msgArray},privilegeObj);
                return;
            }
        });
    }
	else
    {
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getVnfNetworkData(req,res,{code:'danger', msg:msgArray},privilegeObj);
        return;
    }

} );


router.post('/uploadVnfProfile', csp.checkAuth, csp.checkPriv, upload.single('filename'), function(req, res){

    var msgArray = new Array();
    var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname)
	{
        if (req.file.originalname.size == 0) {
            dbRoutes.getVnfProfile(req,res,{code:'failure', msg:'There was an error uploading the file, please try again.'},privilegeObj);
            return;
        }
        fs.exists(req.file.path, function(exists) {

            if(exists) {

                var str = req.file.originalname;

                try {
                    var csv = require('csv');

                    // the job of the parser is to convert a CSV file
                    // to a list of rows (array of rows)
                    var parser = csv.parse({
                        columns: function(line) {
                            // By defining this callback, we get handed the
                            // first line of the spreadsheet. Which we'll
                            // ignore and effectively skip this line from processing
                        },
                        skip_empty_lines: true
                    });

                    var row = 0;
                    var f = new Array();
                    var transformer = csv.transform(function(data){
                        // this will get row by row data, so for example,
                        //logger.debug(data[0]+','+data[1]+','+data[2]);

                        // build an array of rows
                        f[row] = new Array();
                        for ( col=0; col<data.length; col++ )
                        {
                            f[row][col] = data[col];
                        }
                        row++;
                    });

                    // called when done with processing the CSV
                    transformer.on("finish", function() {

                        var funcArray = new Array();

                        function createFunction(lrow,res)
                        {
                            return function(callback) { dbRoutes.addVnfProfile(lrow,res,callback); }
                        }
                        // loop for each row and create an array of callbacks for async.parallelLimit
                        // had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
                            funcArray.push( createFunction(f[x],res) );
                        }

                        // make db calls in parrallel
                        async.series(funcArray, function(err,result){

                            if ( err ) {
                                dbRoutes.getVnfProfile(req,res,result,privilegeObj);
                                return;
                            }
                            else {
                                // result array has an entry in it, success entries are blank, figure out
                                // how many are not blank, aka errors.
                                var rowError = 0;
                                for(var i=0;i<result.length;i++){
                                    if ( result[i].length > 0 )
                                    {
                                        rowError++;
                                    }
                                }
console.log('rowError='+rowError);
                                var rowsProcessed = f.length - rowError;
console.log('rowsProcessed='+rowsProcessed);
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
console.log('result='+JSON.stringify(result));
                                dbRoutes.getVnfProfile(req,res,result,privilegeObj);
                                return;
                            }
                        });
                    });

                    var stream = fs.createReadStream(req.file.path, "utf8");
                    stream.pipe(parser).pipe(transformer);

                } catch(ex) {
                    msgArray.length = 0;
                    msgArray.push('There was an error uploading the file. '+ex);
                    console.error('There was an error uploading the file. '+ex);
                    dbRoutes.getVnfProfile(req,res,{code:'danger', msg:msgArray},privilegeObj);
                    return;
                }
            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getVnfProfile(req,res,{code:'danger', msg:msgArray},privilegeObj);
                return;
            }
        });
        }
    else {
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getVnfProfile(req,res,{code:'danger', msg:msgArray},privilegeObj);
        return;
    }
} );

module.exports = router;
