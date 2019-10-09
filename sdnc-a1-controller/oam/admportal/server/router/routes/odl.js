var express = require('express');
var router = express.Router();
var csp = require('./csp');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');
var async = require('async');


// pass host, username and password to ODL
var username = properties.odlUser;
var password = properties.odlPasswd;
var auth = 'Basic ' + new Buffer(username + ':' + password).toString('base64');

// target host for ODL request
var host = properties.odlHost;
var port = properties.odlPort;
var header = {'Host': host, 'Authorization': auth, 'Content-Type': 'application/yang.data+json'};
var options = {
        host: host,
        headers           : header,
        port              : port,
		rejectUnauthorized: false,
		strictSSL         : false
};

// Connection to OpenDaylight
OdlInterface = require('./OdlInterface');

function handleResult(err, response_str, res) {
    if (err) {
        console.error( String(err) );
        res.render('pages/err', {result:{code:'failure', msg:String(err)}, header:process.env.MAIN_MENU});
    } else {
        // make sure response starts with JSON string
        if (response_str && response_str.indexOf('{') == 0) {
            //console.log("response: ", result);
            res.render('odl/listWklst', { response_obj: JSON.parse(response_str), header:process.env.MAIN_MENU });
        } else {
			res.render('pages/err', {result:{code:'failure', msg:String(err) }, header:process.env.MAIN_MENU});
        }
    }
}

// / index page
// calls restconf to get information
router.get('/listWklst', csp.checkAuth, function(req, res) {
    options.strictSSL = true;   // used to test SSL certificate
    OdlInterface.Get('/restconf/config/L3SDN-API:services',options, handleResult,req,res);
});

router.get('/pageWklst', csp.checkAuth, function(req,res) {
    pageWklst(req,res, {code:'', msg:''}, req.session.loggedInAdmin);
});


function pageWklst(req,res,resultObj,privilegeObj)
{
     if(req.session == null || req.session == undefined
            || req.session.l3sdnPageInfo == null || req.session.l3sdnPageInfo == undefined)
     {
        res.render("pages/err",
            { result: {code:'error', msg:"Unable to read session information. "+ String(err) }, header:process.env.MAIN_MENU});
            return;
     }

    var l3sdnPageInfo = req.session.l3sdnPageInfo;
    var currentPage=1;
    if (typeof req.query.page != 'undefined')
    {
        currentPage = +req.query.page;
    }
    l3sdnPageInfo.currentPage = currentPage;
    l3sdnPageInfo.rows = l3sdnPageInfo.pages[currentPage-1];
    req.session.l3sdnPageInfo = l3sdnPageInfo;
	res.render('odl/listWklst',
    {
    	pageInfo  : l3sdnPageInfo,
        result    : resultObj,
        privilege : privilegeObj, header:process.env.MAIN_MENU
    });
    return;
}


router.post('/update_vr_lan_interface', function(req,res){
	var svc_instance_id = encodeURIComponent(req.body.svc_instance_id);

	// format msg
    var msgRsp = 
	{
    	"vr-lan-interface" : 
		[
			{
				"vr-designation"         : req.body.uf_vr_designation,
				"v6-vr-lan-prefix"       : req.body.uf_vr_lan_prefix,
				"v6-vr-lan-prefix-length": req.body.uf_vr_lan_prefix_length,
				"v6-vce-wan-address"     : req.body.uf_vce_wan_address,
				"v4-vr-lan-prefix"       : req.body.uf_vr_lan_prefix,
				"v4-vr-lan-prefix-length": req.body.uf_vr_lan_prefix_length,
				"v4-vce-loopback-address": req.body.uf_vce_loopback_address
			}
		]
	};
	var tasks = [];
	tasks.push(function(callback){
		OdlInterface.put_vr_lan_interface('/restconf/config/L3SDN-API:services/layer3-service-list/'
			+ svc_instance_id
			+ '/service-data/vr-lan/', options, callback);
	});
	async.series(tasks, function(err,result){
		
		if(err){
		}
		else{
    		var msgArray = new Array();
			//result:{code:'error', msg:"got vr-lan information: "+ String(result)}
            msgArray.push('vr-lan-interface successfully updated.');
			res.render("odl/listVRlan", 
			{
				svc_instance_id: req.body.svc_instance_id,
				response_obj   : JSON.parse(result), header:process.env.MAIN_MENU
			});
            return;
		}	
	});
});


// sendData submits form data to ODL
// Data is read from URL params and converted to JSON
router.get('/svc-topology-operation', function(req, res) {
    var formData = '{"input":{'
             + '"svc-request-id":"'+ new Date().toISOString() + '"' +','
             + '"svc-notification-url":"'+ req.query['svc-notification-url']+ '"' + ','
             + '"svc-action":"'+ req.query['svc-action']+ '"' + ','
             + '"svc-vnf-type":"'+ req.query['svc-vnf-type']+ '"' + ','
             + '"svc-instance-id":"'+ req.query['svc-instance-id']+ '"' + ','
             + '"svc-aic-site-id":"'+ req.query['svc-aic-site-id']+ '"'
             +' } }';
    OdlInterface.Post('/restconf/operations/L3SDN-API:svc-topology-operation', options, formData, handleResult, res);
});

// delete request
router.get('/wklist-delete', function(req, res) {
    //console.dir(req.query);
    OdlInterface.Delete('/restconf/config/L3SDN-API:l3sdn-api-worklist/requests/'+req.query['request'], options, handleResult, res);
});

// get request
router.get('/getid',function(req, res) {
    //console.dir(req.query);
    OdlInterface.GetID('/restconf/config/L3SDN-API:l3sdn-api-worklist/requests/'+req.query['request'], options, res);
});

router.get('/getvnf', function(req,res) {
    //console.log("/getvnf "+req.query);
    OdlInterface.GetVNF('/restconf/config/L3SDN-API:l3sdn-api-worklist/requests/'+req.query['request']+'/vnf/',options,req,res);
});
router.get('/getvrlan', function(req,res) {
	var vrtasks = [];
	var reqstr = encodeURIComponent(req.query['request']);
    vrtasks.push(function(callback) {
		OdlInterface.GetVRlan('/restconf/config/L3SDN-API:services/layer3-service-list/'+reqstr+'/service-data/vr-lan/',options,callback);
    });
	async.series(vrtasks, function(err,result){
    	var msgArray = new Array();
        if(err){
            msgArray.push(err);
			OdlInterface.Get('/restconf/config/L3SDN-API:services',options, handleResult,res);
			//res.render("pages/err",
                //{result:{code:'error', msg:"Unable to get vr-lan information: "+ String(err) }});
            return;
        }
        else {
            msgArray.push('Row successfully deleted from AIC_SITE table.');
			res.render("odl/listVRlan", 
			{
				svc_instance_id: req.query['request'],
				response_obj   : JSON.parse(result), header:process.env.MAIN_MENU
			});
            return;
        }
    });
});
router.get('/getClusterStatus', function(req,res) {


    var urltasks = [];
    var _header = {'Host': host, 'Authorization': auth, 'Content-Type': 'application/yang.data+json'};
    var _options = null;

	var slist = properties.shard_list;
	var hlist = properties.hostnameList;
	var port = properties.clusterPort;
	var prefix_url = properties.clusterPrefixURL;
	var mid_url = properties.clusterMidURL;
	var suffix_url = properties.clusterSuffixURL;
	var urlArray = new Array();
	var url_request='';
	var shard=null, hostname=null;

	// build array of urls from properties
    for(var x=0; x<slist.length; x++)
    {
       	shard = slist[x];
		for(var y=0; y<hlist.length; y++)
		{
    		hostname = hlist[y];

        	url_request = properties.odlProtocol + '://'
            	+ hostname.hname + ':'
            	+ port
            	+ prefix_url
            	+ (y+1)
            	+ mid_url
            	+ shard.shard_name
            	+ suffix_url;

    		_options = {
				method 			  : "GET",
				path			  : url_request,
        		host			  : hostname.hname,
        		headers           : _header,
        		port              : port,
        		rejectUnauthorized: false,
        		strictSSL         : false
			};
        	urlArray.push(_options);
    	}
	}

	urlArray.forEach(function(request){
    	urltasks.push(function(callback) {
        	OdlInterface.GetClusterStatus(request,callback);
		});
    });
    async.series(urltasks, function(err,result){
        var msgArray = new Array();
        if(err){
            msgArray.push(err);
            res.render("pages/err",
                {result:{code:'error', msg:"Unable to get status: "+ String(err) }, header:process.env.MAIN_MENU});
            return;
        }
        else {
    		var msgArray = new Array();
            msgArray.push('Sucess');
            res.render("odl/cluster_status",
            {
				result         : {code:'success', msg:msgArray}, 
                response_obj   : result, header:process.env.MAIN_MENU
            });
            return;
        }
    });
});


module.exports = router;
