#!/bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2020 Nordix Foundation. All rights reserved.
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


TC_ONELINE_DESCR="Experimental ECS test case"

#App names to exclude checking pulling images for, space separated list
EXCLUDED_IMAGES="SDNC PA CP CR MR RICSIM CONSUL CBS"

. ../common/testcase_common.sh  $@
. ../common/ecs_api_functions.sh

#### TEST BEGIN ####

clean_containers

use_ecs_rest_https

use_ecs_rest_https

start_ecs

set_ecs_debug




ecs_api_a1_get_job_ids 200
ecs_api_a1_get_type 200
ecs_api_a1_get_type_ids 200
ecs_api_a1_get_job_status 200
ecs_api_a1_get_job 200
ecs_api_a1_delete_job 200
ecs_api_a1_put_job 200
ecs_api_edp_get_type_ids 200
ecs_api_edp_get_producer_status 200
ecs_api_edp_get_producer_ids 200 NOID
ecs_api_edp_get_type 200
ecs_api_edp_get_producer 200
ecs_api_edp_delete_producer 200
ecs_api_edp_put_producer 200
ecs_api_edp_get_producer_jobs 200
ecs_api_sim_post_job_delete_error 200
ecs_api_sim_get_producer_supervision 200
ecs_api_sim_post_job_deleted 200
ecs_api_get_producer_supervision_error 200
ecs_api_sim_get_job_created_error 200
ecs_api_sim_get_job_created 200
ecs_api_service_status 200



store_logs END

#### TEST COMPLETE ####


print_result

auto_clean_containers
