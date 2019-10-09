. /etc/attappl.env
. ${PROJECT_HOME}/etc/default.env

SSL_ENABLED=`python /opt/admportal/shell/getAdmPortalProp.py sslEnabled | sed -e 's|['\'']||g'` 
export SSL_ENABLED

cd ..
if [ "true" == "${SSL_ENABLED}" ]; then
    pm2 stop admportal8443
else
    pm2 stop http_admportal
fi
pm2 kill
