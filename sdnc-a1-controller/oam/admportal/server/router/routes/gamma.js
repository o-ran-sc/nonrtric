var express = require('express');
var router = express.Router();
var exec = require('child_process').exec;
var util = require('util');
var fs = require('fs');
var dbRoutes = require('./dbRoutes');
var csp = require('./csp');
var multer = require('multer');
var bodyParser = require('body-parser');
var sax = require('sax'),strict=true,parser = sax.parser(strict);
var async = require('async');
var l_ = require('lodash');


// used for file upload button, retain original file name
//router.use(bodyParser());
router.use(bodyParser.urlencoded({
  extended: true
}));

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


//router.use(express.json());
//router.use(express.urlencoded());
//router.use(multer({ dest: './uploads/' }));

// 1604
var selectNetworkProfile = "SELECT network_type,technology FROM NETWORK_PROFILE ORDER BY network_type";

var selectNbVlanRange = "SELECT vlan_plan_id,plan_type,purpose,LPAD(range_start,4,0) range_start,LPAD(range_end,4,0) range_end,generated from VLAN_RANGES ORDER BY vlan_plan_id";

var selectNbVlanPool = "SELECT aic_site_id,availability_zone,vlan_plan_id,plan_type,purpose,LPAD(vlan_id,4,0) vlan_id,status FROM VLAN_POOL ORDER BY aic_site_id,availability_zone,vlan_plan_id,vlan_id";

router.get('/getNetworkProfile', csp.checkAuth, dbRoutes.checkDB, function(req,res) {
	dbRoutes.getTable(req,res,selectNetworkProfile,'gamma/networkProfile',{code:'', msg:''}, req.session.loggedInAdmin);
});
router.get('/getNbVlanRange', csp.checkAuth, dbRoutes.checkDB, function(req,res) {
		dbRoutes.getTable(req,res,selectNbVlanRange,'gamma/nbVlanRange',{code:'', msg:''}, req.session.loggedInAdmin);
});

// GET
router.get('/deleteSite', csp.checkAuth, dbRoutes.checkDB, function(req,res) {

	var privilegeObj = req.session.loggedInAdmin;
	var tasks = [];
	tasks.push(function(callback) {
		dbRoutes.deleteSite(req,res,callback);
	});
	async.series(tasks, function(err,result){
    	var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getAicSite(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from AIC_SITE table.');
            dbRoutes.getAicSite(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// DELETE AIC_SWITCH
router.get('/deleteSwitch', csp.checkAuth, dbRoutes.checkDB, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push(function(callback) {
        dbRoutes.deleteSwitch(req,res,callback);
    });
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getAicSwitch(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from AIC_SWITCH table.');
            dbRoutes.getAicSwitch(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// DELETE AIC_AVAIL_ZONE_POOL
router.get('/deleteZone', csp.checkAuth, dbRoutes.checkDB, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push(function(callback) {
        dbRoutes.deleteZone(req,res,callback);
    });
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getAicAvailZone(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from AIC_AVAIL_ZONE_POOL table.');
            dbRoutes.getAicAvailZone(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// DELETE VLAN_ID_POOL
router.get('/deleteVlanPool', csp.checkAuth, dbRoutes.checkDB, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push(function(callback) {
        dbRoutes.deleteVlanPool(req,res,callback);
    });
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVlanPool(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from VLAN_ID_POOL table.');
            dbRoutes.getVlanPool(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// DELETE VPE_POOL
router.get('/deleteVpePool', csp.checkAuth, dbRoutes.checkDB, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push(function(callback) {
        dbRoutes.deleteVpePool(req,res,callback);
    });
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVpePool(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from VPE_POOL table.');
            dbRoutes.getVpePool(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


// DELETE VPE_POOL
router.get('/deleteVplspePool', csp.checkAuth, dbRoutes.checkDB, function(req,res) {

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push(function(callback) {
        dbRoutes.deleteVplspePool(req,res,callback);
    });
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVplspePool(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Row successfully deleted from VPLSPE_POOL table.');
            dbRoutes.getVplspePool(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// POST
router.post('/addServiceHoming', csp.checkAuth, dbRoutes.checkDB, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.addWebServiceHoming(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getServiceHoming(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully added SERVICE_HOMING');
            dbRoutes.getServiceHoming(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


// gamma - updateProvStatus
router.post('/updateProvStatus', csp.checkAuth, dbRoutes.checkDB, function(req,res){


	var privilegeObj = req.session.loggedInAdmin;
	var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateProvStatus(req,res,callback); } );
    async.series(tasks, function(err,result){
		var msgArray = new Array();
		if(err){
			msgArray.push(err);
            dbRoutes.getVpePool(req,res,{code:'failure', msg:msgArray},privilegeObj);
			return;
        }
        else {
			msgArray.push('Successfully updated Provisioning Status');
            dbRoutes.getVpePool(req,res,{code:'success', msg:msgArray},privilegeObj);
			return;
        }
    });
});

// gamma - updateAicSite
router.post('/updateAicSite', csp.checkAuth, dbRoutes.checkDB, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateAicSite(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getAicSite(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated AIC_SITE table.');
            dbRoutes.getAicSite(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// gamma - updateAicSwitch
router.post('/updateAicSwitch', csp.checkAuth, dbRoutes.checkDB, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateAicSwitch(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getAicSwitch(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated AIC_SWITCH table.');
            dbRoutes.getAicSwitch(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// gamma - updateAicAvailZone
router.post('/updateAicAvailZone', csp.checkAuth, dbRoutes.checkDB, function(req,res){
    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateAicAvailZone(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getAicAvailZone(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated AIC_AVAIL_ZONE_POOL table.');
            dbRoutes.getAicAvailZone(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// gamma - updateVlanPool
router.post('/updateVlanPool', csp.checkAuth, dbRoutes.checkDB, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateVlanPool(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVlanPool(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated VLAN_ID_POOL table.');
            dbRoutes.getVlanPool(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// gamma - updateVpePool
router.post('/updateVpePool', csp.checkAuth, dbRoutes.checkDB, function(req,res){
    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateVpePool(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVpePool(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated VPE_POOL table.');
            dbRoutes.getVpePool(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// gamma - updateVplspePool
router.post('/updateVplspePool', csp.checkAuth, dbRoutes.checkDB, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateVplspePool(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getVplspePool(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated VPLSPE_POOL table.');
            dbRoutes.getVplspePool(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


// gamma - updateServiceHoming
router.post('/updateServiceHoming', csp.checkAuth, dbRoutes.checkDB, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateServiceHoming(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getServiceHoming(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated SERVICE_HOMING table.');
            dbRoutes.getServiceHoming(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


router.post('/uploadVLAN', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res, next){

	var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;

	if(req.file.originalname){
        if (req.file.originalname.size == 0) {
			dbRoutes.getVlanPool(req,res,{code:'danger', msg:'There was an error uploading the file, please try again.'},privilegeObj);
			return;
        }
        fs.exists(req.file.path, function(exists) {
            if(exists) {

				var str = req.file.originalname;
				
				// check for valid filename format
				var tagpos = str.search("_ctag_pool");
				var csvpos = str.search(".csv");
				if(tagpos != 13 || csvpos != 23){
					var msgArray = new Array();
					msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_ctag_pool.csv');
					dbRoutes.getVlanPool(req,res,{code:'failure', msg:msgArray},privilegeObj);
					return;
				}
				
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
                            return function(callback) { dbRoutes.addVLAN(lrow,res,callback); }
                        }

                        // loop for each row and create an array of callbacks for async.parallelLimit
                        // had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
                            funcArray.push( createFunction(f[x],res) );
                        }

                        // make db calls in parrallel
                        //async.parallelLimit(funcArray, 5, function(err,result){
                        async.series(funcArray, function(err,result){

                            if ( err ) {
                                dbRoutes.getVlanPool(req,res, result,privilegeObj);
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

                                var rowsProcessed = f.length - rowError;
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
                                dbRoutes.getVlanPool(req,res,result,privilegeObj);
                                return;
                            }
                        });
                    });


    				var stream = fs.createReadStream(req.file.path, "utf8");
					stream.pipe(parser).pipe(transformer);


				} catch(ex) {
					console.error('error:'+ex);
					msgArray = [];
					msgArray.push('There was an error uploading the file. '+ex);
					dbRoutes.getVlanPool(req,res, {code:'danger', msg:msgArray}, privilegeObj);
					return;
				}

            } else {
				msgArray = [];
				msgArray.push('There was an error uploading the file.');
				dbRoutes.getVlanPool(req,res, {code:'danger', msg:msgArray}, privilegeObj);
				return;
            }
        });
	}
	else {
		msgArray = [];
		msgArray.push('There was an error uploading the file.');
		dbRoutes.getVlanPool(req,res, {code:'danger', msg:msgArray}, privilegeObj);
		return;
	}
	
});


// POST
router.post('/uploadAicSite', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res){

	var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;

	if(req.file.originalname){
        if (req.file.originalname.size == 0) {
			dbRoutes.getAicSite(req,res, {code:'danger', msg:'There was an error uploading the file, please try again.'}, privilegeObj);
			return;
        }
        fs.exists(req.file.path, function(exists) {
            if(exists) {

				var str = req.file.originalname;
				
				// check for valid filename format
				var tagpos = str.search("_site");
				var csvpos = str.search(".csv");
				if(tagpos != 13 || csvpos != 18){
					msgArray.length = 0;
					msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_site.csv');
					dbRoutes.getAicSite(req,res,{code:'failure', msg:msgArray},privilegeObj);
					return;
				}

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
							return function(callback) { dbRoutes.addAicSite(lrow,res,callback); }
						}

						// loop for each row and create an array of callbacks for async.parallelLimit
						// had to create a function above 'createFunction' to get
						for (var x=0; x<f.length; x++)
						{
							funcArray.push( createFunction(f[x],res) );
						}

						// make db calls in parrallel
						async.parallelLimit(funcArray, 50, function(err,result){

							if ( err ) {
								dbRoutes.getAicSite(req,res, result,privilegeObj);
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
								
								var rowsProcessed = f.length - rowError;
								result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
								if ( rowError > 0 )
								{
									result = {code:'failure', msg:result};
								}
								else
								{
									result = {code:'success', msg:result};
								}
								dbRoutes.getAicSite(req,res,result,privilegeObj);
								return;
							}
						});
					});

    				var stream = fs.createReadStream(req.file.path, "utf8");
					stream.pipe(parser).pipe(transformer);


				} catch(ex) {
					msgArray.length = 0;
					msgArray.push('There was an error uploading the file. '+ex);
					dbRoutes.getAicSite(req,res,{code:'danger', msg:msgArray},privilegeObj);
					return;
				}

            } else {
				msgArray.length = 0;
				msgArray.push('There was an error uploading the file.');
				dbRoutes.getAicSite(req,res,{code:'danger', msg:msgArray},privilegeObj);
				return;
            }
        });
	}
	else {
		msgArray.length = 0;
		msgArray.push('There was an error uploading the file.');
		dbRoutes.getAicSite(req,res,{code:'danger', msg:msgArray},privilegeObj);
	}
	
} );

// POST
router.post('/uploadAicSwitch', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res){

    var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname){
        if (req.file.originalname.size == 0) {
            dbRoutes.getAicSwitch(req,res,{code:'danger', msg:'There was an error uploading the file, please try again.'},privilegeObj);
			return;
        }
        fs.exists(req.file.path, function(exists) {

            if(exists) {

                var str = req.file.orignalname;

                // check for valid filename format
                var tagpos = str.search("_switch");
                var csvpos = str.search(".csv");
                if(tagpos != 13 || csvpos != 20){
                    msgArray.length = 0;
                    msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_switch.csv');
                    dbRoutes.getAicSwitch(req,res,{code:'failure', msg:msgArray},privilegeObj);
                    return;
                }

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
                            return function(callback) { dbRoutes.addAicSwitch(lrow,res,callback); }
                        }

                        // loop for each row and create an array of callbacks for async.parallelLimit
                        // had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
                            funcArray.push( createFunction(f[x],res) );
                        }

                        // make db calls in parrallel
                        async.parallelLimit(funcArray, 50, function(err,result){

                            if ( err ) {
                                dbRoutes.getAicSwitch(req,res,result,privilegeObj);
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

                                var rowsProcessed = f.length - rowError;
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
                                dbRoutes.getAicSwitch(req,res,result,privilegeObj);
								return;
                            }
                        });
                    });

                    var stream = fs.createReadStream(req.file.path, "utf8");
                    stream.pipe(parser).pipe(transformer);


                } catch(ex) {
                    msgArray.length = 0;
                    msgArray.push('There was an error uploading the file. '+ex);
                    dbRoutes.getAicSwitch(req,res,{code:'danger', msg:msgArray},privilegeObj);
					return;
                }

            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getAicSwitch(req,res,{code:'danger', msg:msgArray},privilegeObj);
				return;
            }
        });
    }
    else {
	 	msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getAicSwitch(req,res,{code:'danger', msg:msgArray},privilegeObj);
		return;
    }

} );

// POST
router.post('/uploadAicAvailZone', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res){

    var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname){
        if (req.file.originalname.size == 0) {
            dbRoutes.getAicAvailZone(req,res,{code:'failure', msg:'There was an error uploading the file, please try again.'},privilegeObj);
			return;
        }
        fs.exists(req.file.path, function(exists) {

            if(exists) {

                var str = req.file.originalname;

                // check for valid filename format
                var tagpos = str.search("_availabilityzone");
                var csvpos = str.search(".csv");
                if(tagpos != 13 || csvpos != 30){
                    msgArray.length = 0;
                    msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_availabilityzone.csv');
                    dbRoutes.getAicAvailZone(req,res,{code:'failure', msg:msgArray},privilegeObj);
                    return;
                }

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
                            return function(callback) { dbRoutes.addAicAvailZone(lrow,res,callback); }
                        }

                        // loop for each row and create an array of callbacks for async.parallelLimit
                        // had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
                            funcArray.push( createFunction(f[x],res) );
                        }

                        // make db calls in parrallel
                        async.parallelLimit(funcArray, 50, function(err,result){

                            if ( err ) {
                                dbRoutes.getAicAvailZone(req,res,result,privilegeObj);
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

                                var rowsProcessed = f.length - rowError;
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
                                dbRoutes.getAicAvailZone(req,res,result,privilegeObj);
								return;
                            }
                        });
                    });

                    var stream = fs.createReadStream(req.file.path, "utf8");
                    stream.pipe(parser).pipe(transformer);


                } catch(ex) {
                    msgArray.length = 0;
                    msgArray.push('There was an error uploading the file. '+ex);
                    dbRoutes.getAicAvailZone(req,res,{code:'danger', msg:msgArray},privilegeObj);
					return;
                }

            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getAicAvailZone(req,res,{code:'danger', msg:msgArray},privilegeObj);
				return;
            }
        });
 }
    else {
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getAicAvailZone(req,res,{code:'danger', msg:msgArray},privilegeObj);
		return;
    }

} );

// POST
router.post('/uploadVpePool', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res){

    var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname){
        if (req.file.originalname.size == 0) {
            dbRoutes.getVpePool(req,res,{code:'failure', msg:'There was an error uploading the file, please try again.'},privilegeObj);
			return;
        }
        fs.exists(req.file.path, function(exists) {

            if(exists) {

                var str = req.file.originalname;

                // check for valid filename format
                var tagpos = str.search("_vpe");
                var csvpos = str.search(".csv");
                if(tagpos != 13 || csvpos != 17){
                    msgArray.length = 0;
                    msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_vpe.csv');
					var resultObj = {code:'failure', msg:msgArray};
                    dbRoutes.getVpePool(req,res,resultObj,privilegeObj);
					return;
                }

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
                            return function(callback) { dbRoutes.addVpePool(lrow,res,callback); }
                        }

                        // loop for each row and create an array of callbacks for async.parallelLimit
						// had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
                            funcArray.push( createFunction(f[x],res) );
                        }

                        // make db calls in parrallel
                        async.parallelLimit(funcArray, 50, function(err,result){

                            if ( err ) {
                                dbRoutes.getVpePool(req,res,result,privilegeObj);
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

                                var rowsProcessed = f.length - rowError;
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
                                dbRoutes.getVpePool(req,res,result,privilegeObj);
								return;
                            }
                        });
                    });

                    var stream = fs.createReadStream(req.file.path, "utf8");
                    stream.pipe(parser).pipe(transformer);


                } catch(ex) {
                    msgArray.length = 0;
                    msgArray.push('There was an error uploading the file. '+ex);
                    dbRoutes.getVpePool(req,res,{code:'danger', msg:msgArray},privilegeObj);
					return;
                }

            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getVpePool(req,res,{code:'danger', msg:msgArray},privilegeObj);
				return;
            }
        });
 }
    else {
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getVpePool(req,res,{code:'danger', msg:msgArray},privilegeObj);
		return;
    }

} );

// POST
router.post('/uploadVplspePool', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res){

    var msgArray = new Array();
	var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname){
        if (req.file.originalname.size == 0) {
            dbRoutes.getVplspePool(req,res,{code:'failure', msg:'There was an error uploading the file, please try again.'},privilegeObj);
			return;
        }
        fs.exists(req.file.path, function(exists) {

            if(exists) {

                var str = req.file.originalname;

                // check for valid filename format
                var tagpos = str.search("_vpls");
                var csvpos = str.search(".csv");
                if(tagpos != 13 || csvpos != 18){
                    msgArray.length = 0;
                    msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_vpls.csv');
                    dbRoutes.getVplspePool(req,res,{code:'failure', msg:msgArray},privilegeObj);
                    return;
                }

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
                            return function(callback) { dbRoutes.addVplspePool(lrow,res,callback); }
                        }
			 			// loop for each row and create an array of callbacks for async.parallelLimit
                        // had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
                            funcArray.push( createFunction(f[x],res) );
                        }

                        // make db calls in parrallel
                        async.parallelLimit(funcArray, 50, function(err,result){

                            if ( err ) {
                                dbRoutes.getVplspePool(req,res,result,privilegeObj);
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
                                var rowsProcessed = f.length - rowError;
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
                                dbRoutes.getVplspePool(req,res,result,privilegeObj);
								return;
                            }
                        });
                    });

                    var stream = fs.createReadStream(req.file.path, "utf8");
                    stream.pipe(parser).pipe(transformer);


                } catch(ex) {
                    msgArray.length = 0;
                    msgArray.push('There was an error uploading the file. '+ex);
                    dbRoutes.getVplspePool(req,res,{code:'danger', msg:msgArray},privilegeObj);
					return;
                }

            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getVplspePool(req,res,{code:'danger', msg:msgArray},privilegeObj);
				return;
            }
        });
 }
    else {
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getVplspePool(req,res,{code:'danger', msg:msgArray},privilegeObj);
		return;
    }

} );

// POST
router.post('/uploadServiceHoming', csp.checkAuth, dbRoutes.checkDB, upload.single('filename'), function(req, res)
{
    var msgArray = new Array();
    var privilegeObj = req.session.loggedInAdmin;

    if(req.file.originalname)
	{
        if (req.file.originalname.size == 0) {
            dbRoutes.getServiceHoming(req,res,{code:'failure', msg:'There was an error uploading the file, please try again.'},privilegeObj);
            return;
        }
        fs.exists(req.file.path, function(exists) 
		{
            if(exists) 
			{
                var str = req.file.originalname;

                // check for valid filename format
                var csvpos = str.search(".csv");
                if( (l_.startsWith(str,'aichoming_') != true)  || csvpos != 18)
				{
                   	msgArray.length = 0;
                   	msgArray.push('Not a valid filename, format must be aichoming_mmddYYYY.csv');
                   	//msgArray.push('Not a valid filename, format must be pp_YYYYMMDDHH_vpls.csv');
                   	dbRoutes.getServiceHoming(req,res,{code:'failure', msg:msgArray},privilegeObj);
                   	return;
                }

                try 
				{
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
					var csvrows = new Array();
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
                    transformer.on("finish", function() 
					{
                        var funcArray = new Array();

                        function createFunction(lrow,res)
                        {
                            return function(callback) { dbRoutes.addServiceHoming(lrow,req,res,callback); }
                        }
						funcArray.push(function(callback) {
							dbRoutes.saveServiceHoming(req,res,callback);
						});
                        // loop for each row and create an array of callbacks for async.parallelLimit
                        // had to create a function above 'createFunction' to get
                        for (var x=0; x<f.length; x++)
                        {
							funcArray.push( createFunction(f[x],res) );
						}

                        // make db calls in series
                        async.series(funcArray, function(err,result)
						{
                            if ( err ) 
							{
                                result = {code:'failure', msg:result};
                                dbRoutes.getServiceHoming(req,res,result,privilegeObj);
                                return;
                            }
                            else 
							{	// result array has an entry in it, success entries are blank, figure out
                                // how many are not blank, aka errors.
                                var rowError = 0;
                                for(var i=0;i<result.length;i++)
								{
                                    if ( result[i].length > 0 )
                                    {
                                        rowError++;
                                    }
                                }
                                var rowsProcessed = f.length - rowError;
                                result.push(rowsProcessed + ' of ' + f.length + ' rows processed.');
                                if ( rowError > 0 )
                                {
                                    result = {code:'failure', msg:result};
                                }
                                else
                                {
                                    result = {code:'success', msg:result};
                                }
                                dbRoutes.getServiceHoming(req,res,result,privilegeObj);
                                return;
                            }
                        });
                    });

                    var stream = fs.createReadStream(req.file.path, "utf8");
                    stream.pipe(parser).pipe(transformer);


                } catch(ex) {
                    msgArray.length = 0;
                    msgArray.push('There was an error uploading the file. '+ex);
                    dbRoutes.getServiceHoming(req,res,{code:'danger', msg:msgArray},privilegeObj);
                    return;
                }

            } else {
                msgArray.length = 0;
                msgArray.push('There was an error uploading the file.');
                dbRoutes.getServiceHoming(req,res,{code:'danger', msg:msgArray},privilegeObj);
                return;
            }
        });
 	}
    else 
	{
        msgArray.length = 0;
        msgArray.push('There was an error uploading the file.');
        dbRoutes.getServiceHoming(req,res,{code:'danger', msg:msgArray},privilegeObj);
        return;
    }

} );
function removeNL(s) {
  /*
  ** Remove NewLine, CarriageReturn and Tab characters from a String
  **   s  string to be processed
  ** returns new string
  */
  r = "";
  for (i=0; i < s.length; i++)
  {
    if (s.charAt(i) != '\n' &&
        s.charAt(i) != '\r' &&
        s.charAt(i) != '\t')
    {
      r += s.charAt(i);
    }
  }
  return r;
}
function padLeft(nr, n, str){
    return Array(n-String(nr).length+1).join(str||'0')+nr;
}


module.exports = router;
