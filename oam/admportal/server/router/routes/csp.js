var exec = require('child_process').exec;
var dbRoutes = require('./dbRoutes');
var fs = require('fs.extra');
var properties = require(process.env.SDNC_CONFIG_DIR + '/admportal.json');

var retURL = "";
var noCookieUrl = "";
var logoutUrl = "";

function logout(req,res){
	console.log("logout");
	req.session.loggedInAdmin = undefined;
	res.redirect('/login');
}

function login (req,res) {

console.log('login');
var tkn = req.sanitize(req.body._csrf);
console.log('login:tkn=' + tkn);

	var loggedInAdmin={};
	var email = req.sanitize(req.body.email);
	var pswd = req.sanitize(req.body.password);
	dbRoutes.findAdminUser(email,res,function(adminUser){
		if(adminUser !== null){
			
			// make sure correct password is provided
			if (pswd != adminUser.password) {
				res.render("pages/login", 
				{
					result:
					{
						code:'error',
						msg:'Invalid password entered.'
					},
					header:process.env.MAIN_MENU 
				});
				return;
			}
				
			var loggedInAdmin = {
				email:adminUser.email,
				csrfToken: tkn,
				password:adminUser.password,
				privilege:adminUser.privilege
			}
            req.session.loggedInAdmin = loggedInAdmin;
        	console.log("Login Success"+JSON.stringify(loggedInAdmin));
        	res.redirect('sla/listSLA');
		}else{
			res.render("pages/err", 
			{
				result:
				{
					code:'error',
					msg:'User ' + attuid + ' is not in the database.  Please see an adminstrator to have them added.'
				},
				header:process.env.MAIN_MENU 
			});
		}
	});
}

function checkAuth(req,res,next){

	var host = req.get('host');
	var url = req.url;
	var originalUrl = req.originalUrl;

	console.log("checkAuth");

	var host = req.headers['host'];
	console.log('host=' + host);
	console.log("cookie is not null "+JSON.stringify(req.session.loggedInAdmin));
	if(req.session == null || req.session == undefined 
		|| req.session.loggedInAdmin == null || req.session.loggedInAdmin == undefined)
	{
		// nothing else to do but log them back in, or they may
		// be coming from the graph tool
		console.log("loggedInAdmin not found.session timed out.");
		res.render('pages/login');
		return false;
	}
	next();
}

function checkPriv(req,res,next)
{
  var priv = req.session.loggedInAdmin;
  if(req.session == null || req.session == undefined 
		|| req.session.loggedInAdmin == null || req.session.loggedInAdmin == undefined)
  {
    res.render("pages/err", 
		{
			result: {code:'error', msg:'Unexpected null session.'}, 
			header: process.env.MAIN_MENU
		});
    return;
  }
  else
  {
    if (priv.privilege == 'A')
    {
      next();
      return;
    }
    else
    {
      res.render("pages/err", 
			{
				result: { code:'error', msg:'User does not have permission to run operation.'},
				header: process.env.MAIN_MENU
			});
      return;
    }
  }
}


exports.login = login;
exports.logout = logout;
exports.checkAuth = checkAuth;
exports.checkPriv = checkPriv;
