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
docker build -t odu-app .

# build simulator image
docker build -t sdnr-mr-sim -f Dockerfile-simulator .

# start up oru, producer, ics and sdnr-simulator
cd ${SHELL_FOLDER}
docker-compose up -d