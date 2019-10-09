console.log("index.js");

module.exports = function(app) {

//console.log ( 'index port ' + server.address().port );

	app.use('/', require('./routes/root'));
	//app.use('/login', require('./routes/login'));
	app.use('/odl', require('./routes/odl'));
	app.use('/sla', require('./routes/sla'));
	app.use('/user', require('./routes/user'));
	//app.use('/gamma', require('./routes/gamma'));
	app.use('/mobility', require('./routes/mobility'));
	//app.use('/admin', require('./routes/admin'));
	app.use('/preload', require('./routes/preload'));
	//app.use('/svc-topology-operation', require('./routes/odl'));
	//app.use('/wklist-delete', require('./routes/odl'));
};
