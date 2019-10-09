#. ./set-http-env.sh
#cd ../bin
#cp ../config/config.json.http ../config/config.json
. /etc/attappl.env
. ${PROJECT_HOME}/etc/default.env

MAIN_MENU=`python printMainMenu.py | sed -e 's|['\'']||g'`
export MAIN_MENU

cd ..
pm2 startOrRestart process.http.json 
