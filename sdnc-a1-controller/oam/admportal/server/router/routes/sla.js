var express = require('express');
var router = express.Router();
var exec = require('child_process').exec;
//var util = require('util');
var fs = require('fs');
var dbRoutes = require('./dbRoutes');
var csp = require('./csp');
var multer = require('multer');
var cookieParser = require('cookie-parser');
var csrf = require('csurf');
var bodyParser = require('body-parser');
//var sax = require('sax'),strict=true,parser = sax.parser(strict);
var async = require('async');


// SVC_LOGIC table columns
var _module=''; // cannot use module its a reserved word
var version='';
var rpc='';
var mode='';
var xmlfile='';


// used for file upload button, retain original file name
//router.use(bodyParser());
var csrfProtection = csrf({cookie: true});
router.use(bodyParser.urlencoded({ extended: true }));
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


/*
router.use(multer({
	dest: process.cwd() + '/uploads/',
	rename: function(fieldname,filename){
		return filename;
	}
}));
*/


//router.use(express.json());
//router.use(express.urlencoded());
//router.use(multer({ dest: './uploads/' }));


// GET
router.get('/listSLA', csp.checkAuth, csrfProtection, function(req,res) {
	dbRoutes.listSLA(req,res,{code:'', msg:''} );
});

router.get('/activate', csp.checkAuth, csrfProtection, function(req,res){

	var _module = req.query.module;
	var rpc = req.query.rpc;
	var version = req.query.version;
	var mode = req.query.mode;

	var tasks = [];
    tasks.push( function(callback) { dbRoutes.global_deactivate(req,res,_module,rpc,mode,callback); } );
    tasks.push( function(callback) { dbRoutes.activate(req,res,_module,rpc,version,mode,callback); } );
	async.series(tasks,  function(err,result){

		 if (  err ) {
			 dbRoutes.listSLA(req,res,{code:'failure', msg:'Failed to activate, '+ String(err) });
         }
		 else {
			 dbRoutes.listSLA(req,res,{ code:'success', msg:'Successfully activated directed graph.'});
		 }
	});
});

router.get('/deactivate', csp.checkAuth, csrfProtection, function(req,res){

	var _module = req.query.module;
	var rpc = req.query.rpc;
	var version = req.query.version;
	var mode = req.query.mode;

	var tasks = [];
    tasks.push( function(callback) { dbRoutes.deactivate(req,res,_module,rpc,version,mode,callback); } );
    async.series(tasks,  function(err,result){

         if (  err ) {
             dbRoutes.listSLA(req,res,{code:'failure', msg:'There was an error uploading the file. '+ err });
         }
         else {
             dbRoutes.listSLA(req,res,{ code:'success', msg:'Successfully deactivated directed graph.'});
         }
    });
});

router.get('/deleteDG', csp.checkAuth, csrfProtection, function(req,res){

	var _module = req.query.module;
	var rpc = req.query.rpc;
	var version = req.query.version;
	var mode = req.query.mode;

	var tasks = [];
    tasks.push( function(callback) { dbRoutes.deleteDG(req,res,_module,rpc,version,mode,callback); } );
    async.series(tasks,  function(err,result){

         if (  err ) {
             dbRoutes.listSLA(req,res,{ code:'failure', msg:'There was an error uploading the file. '+ err });
         }
         else {
             dbRoutes.listSLA(req,res,{ code:'success', msg:'Successfully deleted directed graph.'});
         }
    });
});

router.post('/dgUpload', upload.single('filename'), csrfProtection, function(req, res, next){

    if(req.file.originalname){
        if (req.file.originalname == 0) {
			
            dbRoutes.listSLA(req,res,{ code:'danger', msg:'There was an error uploading the file, please try again.'});
        }
        fs.exists(req.file.path, function(exists) {
            if(exists) {

                // parse xml
                try {
					//dbRoutes.checkSvcLogic(req,res);

                    var file_buf = fs.readFileSync(req.file.path, "utf8");

                    // call Dan's svclogic shell script from here
					 var currentDB = dbRoutes.getCurrentDB();
                     var commandToExec = process.cwd()
                        + "/shell/svclogic.sh load "
                        + req.file.path + " "
                        + process.env.SDNC_CONFIG_DIR + "/svclogic.properties." + currentDB;

                    console.log("commandToExec:" + commandToExec);
                    child = exec(commandToExec ,function (error,stdout,stderr){
                        if(error){
                            console.error("error:" + error);
							//res.type('text/html').status(400).send( error);
							//return;
                        }
                        if(stderr){
							res.status(400).send(stderr);
							return;
                        }
                        if(stdout){
							res.status(200).send( new Buffer('Success'));
							return;
                        }

                        // remove the grave accents, the sax parser does not like them
                        //parser.write(file_buf.replace(/\`/g,'').toString('utf8')).close();
                        //dbRoutes.addDG(_module,version,rpc,mode,file_buf,req,res);
                        //dbRoutes.listSLA(req,res, resultObj);
                    });
                } catch(ex) {
                    // keep 'em silent
                    console.error('sax error:'+ex);
					res.status(400).send(ex);
					return;
                }

            } else {
				res.status(400).send(new Buffer('Cannot find file.'));
				return;
			
            }
        });
    }
    else {
		res.status(400).send(new Buffer('file does not exist\n'));
    }
	return;
});


// POST
router.post('/upload', csp.checkAuth, upload.single('filename'), csrfProtection, function(req, res, next){

console.log('file:'+ JSON.stringify(req.file));

	if(req.file.originalname)
	{
		if (req.file.originalname.size == 0)
		{
			dbRoutes.listSLA(req,res,
			{ code:'danger', msg:'There was an error uploading the file, please try again.'});
		}
		fs.exists(req.file.path, function(exists)
		{
			if(exists)
			{
				// parse xml
				try 
				{
					//dbRoutes.checkSvcLogic(req,res);

					var currentDB = dbRoutes.getCurrentDB();
					var file_buf = fs.readFileSync(req.file.path, "utf8");

					// call svclogic shell script from here
					var commandToExec = process.cwd() + "/shell/svclogic.sh load "
						+ req.file.path + " "
            + process.env.SDNC_CONFIG_DIR + "/svclogic.properties." + currentDB;

					console.log("commandToExec:" + commandToExec);
					child = exec(commandToExec ,function (error,stdout,stderr)
					{
						if(error)
						{
							console.error("error:" + error);
							dbRoutes.listSLA(req,res,{code:'failure',msg:error} );
							return;
						}
						if(stderr){
							console.error("stderr:" + JSON.stringify(stderr,null,2));
							var s_stderr = JSON.stringify(stderr);
            	if ( s_stderr.indexOf("Saving") > -1 )
            	{
              	dbRoutes.listSLA(req,res,{code:'success', msg:'File sucessfully uploaded.'});
            	}else {
              	dbRoutes.listSLA(req,res,{code:'failure', msg:stderr});
            	}
            	return;
						}
          	if(stdout){
							console.log("stderr:" + stdout);
							dbRoutes.listSLA(req,res,{code:'success', msg:'File sucessfully uploaded.'});
            	return;
						}

						// remove the grave accents, the sax parser does not like them
    					//parser.write(file_buf.replace(/\`/g,'').toString('utf8')).close();
						//dbRoutes.addDG(_module,version,rpc,mode,file_buf,req,res);
						//dbRoutes.listSLA(req,res, resultObj);
				});
			} catch(ex) {
				// keep 'em silent
				console.error("error:" + ex);
				dbRoutes.listSLA(req,res,{code:'failure',msg:ex} );
			}
		}
		else {
			dbRoutes.listSLA(req,res,{ code:'danger', msg:'There was an error uploading the file, please try again.'});
		}
		});
	}
	else {
		dbRoutes.listSLA(req,res,{ code:'danger', msg:'There was an error uploading the file, please try again.'});
	}
});

router.get('/printAsXml', csp.checkAuth, csrfProtection, function(req,res){

	try {
		//dbRoutes.checkSvcLogic(req,res);

		var _module = req.query.module;
    var rpc = req.query.rpc;
    var version = req.query.version;
    var mode = req.query.mode;
		var currentDB = dbRoutes.getCurrentDB();

    // call Dan's svclogic shell script from here
    var commandToExec = process.cwd()
       		+ "/shell/svclogic.sh get-source "
            + _module + " "
            + rpc + " "
            + mode + " "
            + version + " "
            + process.env.SDNC_CONFIG_DIR + "/svclogic.properties." + currentDB;

		console.log("commandToExec:" + commandToExec);

    child = exec(commandToExec , {maxBuffer: 1024*5000}, function (error,stdout,stderr){
	  	if(error){
				console.error("error:" + error);
        dbRoutes.listSLA(req,res,{code:'failure',msg:error} );
				return;
    	}
    	//if(stderr){
    	//logger.info("stderr:" + stderr);
    	//}
    	if(stdout){
      	console.log("OUTPUT:" + stdout);
      	res.render('sla/printasxml', {result:{code:'success', 
				msg:'Module : ' + _module + '\n' + 
						'RPC    : ' + rpc + '\n' + 
						'Mode   : ' + mode + '\n' +
						'Version: ' + version + '\n\n' + stdout}, header:process.env.MAIN_MENU});
   		}

   		// remove the grave accents, the sax parser does not like them
   		//parser.write(file_buf.replace(/\`/g,'').toString('utf8')).close();
   		//dbRoutes.addDG(_module,version,rpc,mode,file_buf,req,res);
   		//dbRoutes.listSLA(req,res, resultObj);
   });
 } catch(ex) {
		console.error("error:" + ex);
		dbRoutes.listSLA(req,res,{code:'failure',msg:ex} );
 }
});


module.exports = router;
