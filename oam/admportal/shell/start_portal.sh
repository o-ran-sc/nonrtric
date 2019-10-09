#. ./set-https-env.sh
#cd ../bin
#cp ../config/config.json.https ../config/config.json
export PROJECT_HOME=/opt/onap/sdnc
export PROJECT_RUNTIME_BASE=/opt/onap/sdnc
#export JAVA_HOME=/usr/lib/jvm/java-7-oracle
export JAVA_HOME=/usr/lib/jvm/java-7-openjdk-amd64
export SSL_ENABLED=false
export SDNC_CONFIG_DIR=${PROJECT_HOME}/data/properties
export NODE_ENV=production
export CLASSPATH=$PROJECT_HOME/admportal/lib:$CLASSPATH

PATH=${PATH}:${JAVA_HOME}/bin

#. ${PROJECT_HOME}/etc/default.env

MAIN_MENU=`python $PROJECT_HOME/admportal/shell/getAdmPortalProp.py MainMenu | sed -e 's|['\'']||g'` 
export MAIN_MENU
SSL_ENABLED=`python $PROJECT_HOME/admportal/shell/getAdmPortalProp.py sslEnabled | sed -e 's|['\'']||g'` 
export SSL_ENABLED

if [ ! -d /opt/onap/sdnc/admportal/node_modules ]; then
    echo "ERROR: missing node modules: /opt/onap/sdnc/admportal/node_modules"
    exit 1
fi

cd /opt/onap/sdnc/admportal
exec node shell/www
#if [ "true" == "${SSL_ENABLED}" ]; then
	#pm2 startOrRestart process.https.json
#else
	#pm2 startOrRestart process.http.json
#fi
