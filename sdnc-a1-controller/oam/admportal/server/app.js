var express = require('express');
var app = express();
var path = require('path');
var session = require('express-session');
var cookieParser = require('cookie-parser');
var bodyParser = require('body-parser');
var PropertiesReader = require('properties-reader');
var properties = PropertiesReader(process.argv[2]); //property file passed
var morgan = require('morgan');
var _ = require('lodash');
var expressSanitizer = require('express-sanitizer');
//var multer = require('multer');
//var done=false;

// Check to make sure SDNC_CONFIG_DIR is set
var sdnc_config_dir = process.env.SDNC_CONFIG_DIR;
if ( typeof sdnc_config_dir == 'undefined' )
{
	console.log('ERROR the SDNC_CONFIG_DIR environmental variable is not set.');
	return;
}
	

var moptions = { "stream": 
{
	write: function(str)
	{
		if ( str.indexOf("/javascript") == -1 && str.indexOf("/stylesheets") == -1)
		{
			console.log(str); 
		}
	}
}
};
var accesslog = morgan( "|:method|HTTP/:http-version|:status|:url - requestIP-:remote-addr", moptions);

//var favicon = require('serve-favicon');

// initialize session objects
app.use(session({
	secret:'SDN7C',
	resave: false,
	saveUninitialized: false
}));

app.use(cookieParser());
app.use(bodyParser.urlencoded({
  extended: true
}));

// mount express-sanitizer here
app.use(expressSanitizer()); // this line needs to follow bodyParser

app.use(accesslog); // http access log
app.use(express.static(process.cwd() + '/public')); // static files


//app.use('trust proxy', true);
app.enable('trust proxy');

// view engine setup
app.set('views', path.join(__dirname, '../views'));
app.set('view engine', 'ejs');


var router = require('./router')(app);

// Error Handling
app.use(function(err,req,res,next) {
	res.status(err.status || 500);
});

module.exports = app;
