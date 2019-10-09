var express = require('express');
var router = express.Router();
var csp = require('./csp.js');
var dbRoutes = require('./dbRoutes.js');
var sla = require('./sla');
var os = require('os');
var async = require('async');
var OdlInterface = require('./OdlInterface');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');
var cookieParser = require('cookie-parser')
var csrf = require('csurf')
var bodyParser = require('body-parser')

var csrfProtection = csrf({cookie:true});
var parseForm = bodyParser.urlencoded({ extended: false })



router.use('/healthcheck', function(req,res){
	res.render('pages/healthcheck');
});
router.get('/test', function(req,res){

//console.log('port='+ req.socket.localPort);
//console.log('port='+ req.protocol);

	// pass host, username and password to ODL
	var username = properties.odlUser;
	var password = properties.odlPasswd;
	var auth = 'Basic ' + new Buffer(username + ':' + password).toString('base64');

	// target host for ODL request
	var host = properties.odlHost;
	var header = {'Host': host, 'Authorization': auth, 'Content-Type': 'application/yang.data+json'};
	var c_header = {'Host': properties.odlConexusHost, 'Authorization': auth, 'Content-Type': 'application/yang.data+json'};

// path = '/restconf/config/SLI-API:healthcheck',
	var _options = {
		method			  : 'POST',
        host              : host,
        headers           : header,
        port              : '8443',
		path			  : '/restconf/operations/SLI-API:healthcheck',
        rejectUnauthorized: false,
        strictSSL         : false
	};
	var c_options = {
		method			  : 'POST',
        host              : properties.odlConexusHost,
        headers           : c_header,
        port              : '8543',
		path			  : '/restconf/operations/SLI-API:healthcheck',
        rejectUnauthorized: false,
        strictSSL         : false
	};


    var tasks = [];
    //tasks.push( function(callback) { dbRoutes.testdb(req,res,callback); } );

	tasks.push ( createFunctionObj(_options) );

	tasks.push ( createFunctionObj(c_options) );

    async.series(tasks, function(err,result){
    	if(err) {
			res.status(400).send(err);
			return;
    	}
		res.status(200).send(result);
		return;
	});
});

function createFunctionObj( loptions ) {
	return function(callback) { OdlInterface.Healthcheck(loptions,callback); };
}

//router.get('/mytree', function(req,res) {
//	res.render('pages/tree');
//});
//router.get('/setuplogin', function(req,res) {
//	res.render('pages/setuplogin');
//});
//router.post('/formSetupLogin', function(req,res) {
//	dbRoutes.saveSetupLogin(req,res);
//});

router.get('/login', csrfProtection, function(req,res) {
	var tkn = req.csrfToken();
	res.render('pages/login', {csrfToken:tkn});
	return;
});
router.post('/formlogin', csrfProtection, function(req,res) {
	csp.login(req,res);
});

router.get('/signup', csrfProtection, function(req,res) {
	var tkn = req.csrfToken();
	res.render('pages/signup', {csrfToken:tkn});
});
router.post('/formSignUp', csrfProtection, function(req,res) {
	dbRoutes.saveUser(req,res);
});

router.get('/info', function(req,res) {
	// handle get
	res.send("login info");
});
router.get('/logout', csp.logout, function(req,res) {
    // handle get
});
router.get('/csplogout', function(req,res) {
    // handle get
	res.render("pages/csplogout", {result:{code:'success', msg:'You have been successfylly logged out.'},header:process.env.MAIN_MENU});
});
router.get('/getuser', function(req,res) {
    // handle get
    res.render("pages/home");
});

module.exports = router;
