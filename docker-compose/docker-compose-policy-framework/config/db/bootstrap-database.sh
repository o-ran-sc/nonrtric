#!/bin/sh

###
# ============LICENSE_START=======================================================
# ONAP CLAMP
# ================================================================================
# Copyright (C) 2017 AT&T Intellectual Property. All rights reserved.
# Modifications Copyright (C) 2021 Nordix Foundation. All rights reserved.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
# ============LICENSE_END============================================
# ===================================================================
#
###

mysql -uroot -p$MYSQL_ROOT_PASSWORD -f < /docker-entrypoint-initdb.d/create-db.sql
mysql -uroot -p$MYSQL_ROOT_PASSWORD --execute "CREATE USER 'policy_user'@'%' IDENTIFIED BY 'policy_user';"
mysql -uroot -p$MYSQL_ROOT_PASSWORD --execute "GRANT ALL PRIVILEGES ON controlloop.* TO 'policy_user'@'%';"