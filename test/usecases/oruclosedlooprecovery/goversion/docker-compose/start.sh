#  Copyright (C) 2022 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================
#

SHELL_FOLDER=$(cd "$(dirname "$0")";pwd)
docker stop $(docker ps -aq)
docker system prune -f

# build o-ru application image
cd ${SHELL_FOLDER}/../
docker build -t oru-app .

# build simulator image of ics
docker build -t ics-sim -f Dockerfile-ics .

# build simulator image of producer
docker build -t producer-sim -f Dockerfile-producer .

# build simulator image of sdnr
docker build -t sdnr-sim -f Dockerfile-sdnr .

# start up oru, producer, ics and sdnr-simulator
cd ${SHELL_FOLDER}
docker-compose up -d

# create the job in ICS
curl -X POST http://localhost:8086/admin/start