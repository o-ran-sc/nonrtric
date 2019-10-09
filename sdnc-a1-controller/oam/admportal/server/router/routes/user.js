var express = require('express');
var router = express.Router();
var exec = require('child_process').exec;
var util = require('util');
var fs = require('fs');
var dbRoutes = require('./dbRoutes');
var csp = require('./csp');
var cookieParser = require('cookie-parser');
var csrf = require('csurf');
var bodyParser = require('body-parser');
//var sax = require('sax'),strict=true,parser = sax.parser(strict);

var csrfProtection = csrf({cookie: true});
router.use(cookieParser());

// SVC_LOGIC table columns
var _module=''; // cannot use module its a reserved word
var version='';
var rpc='';
var mode='';
var xmlfile='';


//router.use(bodyParser());
router.use(bodyParser.urlencoded({ extended: true }));


// GET
router.get('/listUsers', csp.checkAuth, function(req,res) {
	dbRoutes.listUsers(req,res, {user:req.session.loggedInAdmin,code:'', msg:''} );
});
// POST
router.post('/updateUser', csp.checkAuth, csrfProtection, function(req,res,next){
	dbRoutes.updateUser(req,res,{code:'',msg:''});
});
router.post('/addUser', csp.checkAuth, csrfProtection, function(req,res) {
	dbRoutes.addUser(req,res, {code:'', msg:''} );
});
router.get('/deleteUser', csp.checkAuth, csrfProtection, function(req,res) {
	dbRoutes.deleteUser(req,res, {code:'', msg:''} );
});

//router.get('/activate', csp.checkAuth, function(req,res){

	//var _module = req.query.module;
	//var rpc = req.query.rpc;
	//var version = req.query.version;
	//var mode = req.query.mode;

	//dbRoutes.activate(req,res,_module,rpc,version,mode);
//});

//router.get('/deactivate', csp.checkAuth, function(req,res){

	//var _module = req.query.module;
	//var rpc = req.query.rpc;
	//var version = req.query.version;
	//var mode = req.query.mode;
//
	//dbRoutes.deactivate(req,res,_module,rpc,version,mode);
//});

//router.get('/deleteDG', csp.checkAuth, function(req,res){

	//var _module = req.query.module;
	//var rpc = req.query.rpc;
	//var version = req.query.version;
	//var mode = req.query.mode;

	//dbRoutes.deleteDG(req,res,_module,rpc,version,mode);
//});
/*
// SAX
parser.onerror = function (e) {
	logger.debug('onerror');
  // an error happened.
};
parser.ontext = function (t) {
  // got some text.  t is the string of text.
	logger.debug('ontext:'+t);
};
parser.onopentag = function (node) {
  // opened a tag.  node has "name" and "attributes"
	if ( node.name == 'service-logic' )
	{
		_module = node.attributes.module;
		version = node.attributes.version;
	}
	if ( node.name == 'method' )
	{
		rpc = node.attributes.rpc;
		mode = node.attributes.mode;
	}
};
parser.onattribute = function (attr) {
  // an attribute.  attr has "name" and "value"
	logger.debug('onattribute:'+attr);
};
parser.onend = function () {
  // parser stream is done, and ready to have more stuff written to it.
	logger.debug('onend:');
};
*/



//router.post('/upload', csp.checkAuth, function(req, res, next){

/*
logger.debug("upload");
	if(req.files.filename){
        if (req.files.filename.size == 0) {
			resultObj = 
				{code:'danger', msg:'There was an error uploading the file, please try again.'};
			dbRoutes.listSLA(req,res, resultObj);
        }
        fs.exists(req.files.filename.path, function(exists) {
            if(exists) {
				resultObj = {code:'success', msg:'File sucessfully uploaded.'};

				// parse xml
				try {
    				var file_buf = fs.readFileSync(req.files.filename.path, "utf8");
logger.debug('file '+req.files.filename);

					
					// call Dan's svclogic shell script from here
					 var commandToExec = process.cwd()
            			+ "/shell/svclogic.sh load "
						+ req.files.filename.path + " "
						+ process.cwd()
						+ "/config/svclogic.properties";

        			logger.debug("commandToExec:" + commandToExec);
        			child = exec(commandToExec ,function (error,stdout,stderr){
            			if(error){
                			logger.info("error:" + error);
            			}
            			if(stderr){
                			logger.info("stderr:" + stderr);
            			}
            			if(stdout){
							logger.info("OUTPUT:" + stdout);
							dbRoutes.listSLA(req,res, resultObj);
						}

						// remove the grave accents, the sax parser does not like them
    					//parser.write(file_buf.replace(/\`/g,'').toString('utf8')).close();
						//dbRoutes.addDG(_module,version,rpc,mode,file_buf,req,res);
						//dbRoutes.listSLA(req,res, resultObj);
					});
				} catch(ex) {
    				// keep 'em silent
					logger.debug('sax error:'+ex);
				}

            } else {
				resultObj = 
					{code:'danger', msg:'There was an error uploading the file, please try again.'};
				dbRoutes.listSLA(req,res, resultObj);
            }
        });
	}
	else {
		resultObj = 
			{code:'danger', msg:'There was an error uploading the file, please try again.'};
		dbRoutes.listSLA(req,res, resultObj);
	}
});
*/

module.exports = router;
