var express = require('express');
var router = express.Router();
var exec = require('child_process').exec;
var util = require('util');
var fs = require('fs');
var dbRoutes = require('./dbRoutes');
var csp = require('./csp');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var sax = require('sax'),strict=true,parser = sax.parser(strict);
var async = require('async');
var csrf = require('csurf');

var csrfProtection = csrf({cookie: true});
router.use(cookieParser());


// GET
router.get('/getParameters', csp.checkAuth, dbRoutes.checkDB, function(req,res) {
    dbRoutes.getParameters(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
});
router.get('/deleteParameter', csp.checkAuth, dbRoutes.checkDB, csrfProtection, function(req,res) {

	var privilegeObj = req.session.loggedInAdmin;
	var tasks = [];
	tasks.push(function(callback) { dbRoutes.deleteParameter(req,res,callback); });
	async.series(tasks, function(err,result){
		var msgArray = new Array();
		if(err){
			msgArray.push(err);
			dbRoutes.getParameters(req,res,{code:'failure', msg:msgArray},privilegeObj);
			return;
		}
		else {
			msgArray.push('Row successfully deleted from PARAMETERS table.');
			dbRoutes.getParameters(req,res,{code:'success', msg:msgArray},privilegeObj);
			return;
		}
	});
});


// POST
router.post('/addParameter', csp.checkAuth, dbRoutes.checkDB, csrfProtection, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.addParameter(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getParameters(req,res,{code:'failure', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated PARAMETERS.');
            dbRoutes.getParameters(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});

// gamma - updateAicSite
router.post('/updateParameter', csp.checkAuth, dbRoutes.checkDB, csrfProtection, function(req,res){

    var privilegeObj = req.session.loggedInAdmin;
    var tasks = [];
    tasks.push( function(callback) { dbRoutes.updateParameter(req,res,callback); } );
    async.series(tasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            dbRoutes.getParameters(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
        else {
            msgArray.push('Successfully updated PARAMETERS.');
            dbRoutes.getParameters(req,res,{code:'success', msg:msgArray},privilegeObj);
            return;
        }
    });
});


module.exports = router;
