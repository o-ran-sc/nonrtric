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

#App names to include in the test, space separated list
INCLUDED_IMAGES="ECS PRODSTUB"

. ../common/testcase_common.sh  $@
. ../common/ecs_api_functions.sh
. ../common/prodstub_api_functions.sh

#### TEST BEGIN ####

clean_containers

use_ecs_rest_http

use_prod_stub_http

start_ecs

start_prod_stub

set_ecs_debug

set_ecs_trace

# Setup prodstub sim to accept calls for producers, types and jobs
prodstub_arm_supervision 200 prod-a
prodstub_arm_supervision 200 prod-b
prodstub_arm_supervision 200 prod-c

prodstub_arm_type 200 prod-a type1
prodstub_arm_type 200 prod-b type2
prodstub_arm_type 200 prod-b type3

prodstub_disarm_type 200 prod-b type3
prodstub_arm_type 200 prod-b type1
prodstub_disarm_type 200 prod-b type1

prodstub_arm_create 200 prod-a job1
prodstub_arm_create 200 prod-a job2
prodstub_arm_create 200 prod-b job3

prodstub_arm_delete 200 prod-a job1
prodstub_arm_delete 200 prod-a job2
prodstub_arm_delete 200 prod-b job3

prodstub_arm_create 200 prod-b job4
prodstub_arm_create 200 prod-a job4

prodstub_arm_create 200 prod-b job5
prodstub_arm_create 200 prod-a job5
prodstub_arm_delete 200 prod-a job5

prodstub_arm_create 200 prod-b job6

# ecs status
ecs_api_service_status 200

# Initial tests - no config made
ecs_api_a1_get_type_ids 200 EMPTY
ecs_api_a1_get_type 404 test-type

ecs_api_edp_get_type_ids 200 EMPTY
ecs_api_edp_get_type 404 test-type

ecs_api_edp_get_producer_ids 200 EMPTY
ecs_api_edp_get_producer 404 test-prod

ecs_api_edp_get_producer_status 404 test-prod

ecs_api_edp_delete_producer 404 test-prod

ecs_api_a1_get_job_ids 404 test-type NOWNER
ecs_api_a1_get_job_ids 404 test-type test-owner

ecs_api_a1_get_job 404 test-type test-job

ecs_api_a1_get_job_status 404 test-type test-job

ecs_api_a1_delete_job 404 test-type test-job

ecs_api_edp_get_producer_jobs 404 test-prod


# Setup of producer/job and test apis
#prod-a
ecs_api_edp_put_producer 201 prod-a http://producer-stub:8092/callbacks/create/prod-a http://producer-stub:8092/callbacks/delete/prod-a http://producer-stub:8092/callbacks/supervision/prod-a type1 testdata/ecs/ei-type-1.json
ecs_api_edp_put_producer 200 prod-a http://producer-stub:8092/callbacks/create/prod-a http://producer-stub:8092/callbacks/delete/prod-a http://producer-stub:8092/callbacks/supervision/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_a1_get_type_ids 200 type1
ecs_api_a1_get_type 200 type1 testdata/ecs/ei-type-1.json

ecs_api_edp_get_type_ids 200 type1
ecs_api_edp_get_type 200 type1 testdata/ecs/ei-type-1.json prod-a

ecs_api_edp_get_producer_ids 200 prod-a
ecs_api_edp_get_producer 200 prod-a http://producer-stub:8092/callbacks/create/prod-a http://producer-stub:8092/callbacks/delete/prod-a http://producer-stub:8092/callbacks/supervision/prod-a type1 testdata/ecs/ei-type-1.json

ecs_api_edp_get_producer_status 200 prod-a ENABLED

ecs_api_a1_get_job_ids 200 type1 NOWNER EMPTY
ecs_api_a1_get_job_ids 200 type1 test-owner EMPTY

ecs_api_a1_get_job 404 type1 test-job

ecs_api_a1_get_job_status 404 type1 test-job

ecs_api_edp_get_producer_jobs 200 prod-a EMPTY


#job1 - prod-a
ecs_api_a1_put_job 201 type1 job1 http://localhost:80/target1 ric1 testdata/ecs/job-template.json

prodstub_check_jobdata 200 prod-a job1 type1 http://localhost:80/target1 testdata/ecs/job-template.json

ecs_api_a1_get_job_ids 200 type1 NOWNER job1
ecs_api_a1_get_job_ids 200 type1 ric1 job1

ecs_api_a1_get_job 200 type1 job1 http://localhost:80/target1 ric1 testdata/ecs/job-template.json

ecs_api_a1_get_job_status 200 type1 job1 ENABLED

ecs_api_edp_get_producer_jobs 200 prod-a job1 type1 http://localhost:80/target1 testdata/ecs/job-template.json


#job2 - prod-a
ecs_api_a1_put_job 201 type1 job2 http://localhost:80/target2 ric2 testdata/ecs/job-template.json

prodstub_check_jobdata 200 prod-a job2 type1 http://localhost:80/target2 testdata/ecs/job-template.json

ecs_api_a1_get_job_ids 200 type1 NOWNER job1 job2
ecs_api_a1_get_job_ids 200 type1 ric1 job1
ecs_api_a1_get_job_ids 200 type1 ric2 job2

ecs_api_a1_get_job 200 type1 job2 http://localhost:80/target2 ric2 testdata/ecs/job-template.json

ecs_api_a1_get_job_status 200 type1 job2 ENABLED

ecs_api_edp_get_producer_jobs 200 prod-a job1 type1 http://localhost:80/target1 testdata/ecs/job-template.json job2 type1 http://localhost:80/target2 testdata/ecs/job-template.json


#prod-b
ecs_api_edp_put_producer 201 prod-b http://producer-stub:8092/callbacks/create/prod-b http://producer-stub:8092/callbacks/delete/prod-b http://producer-stub:8092/callbacks/supervision/prod-b type2 testdata/ecs/ei-type-2.json

ecs_api_a1_get_type_ids 200 type1 type2
ecs_api_a1_get_type 200 type1 testdata/ecs/ei-type-1.json
ecs_api_a1_get_type 200 type2 testdata/ecs/ei-type-2.json

ecs_api_edp_get_type_ids 200 type1 type2
ecs_api_edp_get_type 200 type1 testdata/ecs/ei-type-1.json prod-a
ecs_api_edp_get_type 200 type2 testdata/ecs/ei-type-2.json prod-b

ecs_api_edp_get_producer_ids 200 prod-a prod-b
ecs_api_edp_get_producer 200 prod-a http://producer-stub:8092/callbacks/create/prod-a http://producer-stub:8092/callbacks/delete/prod-a http://producer-stub:8092/callbacks/supervision/prod-a type1 testdata/ecs/ei-type-1.json
ecs_api_edp_get_producer 200 prod-b http://producer-stub:8092/callbacks/create/prod-b http://producer-stub:8092/callbacks/delete/prod-b http://producer-stub:8092/callbacks/supervision/prod-b type2 testdata/ecs/ei-type-2.json

ecs_api_edp_get_producer_status 200 prod-b ENABLED


#job3 - prod-b
ecs_api_a1_put_job 201 type2 job3 http://localhost:80/target3 ric3 testdata/ecs/job-template.json

prodstub_check_jobdata 200 prod-b job3 type2 http://localhost:80/target3 testdata/ecs/job-template.json

ecs_api_a1_get_job_ids 200 type1 NOWNER job1 job2
ecs_api_a1_get_job_ids 200 type2 NOWNER job3
ecs_api_a1_get_job_ids 200 type1 ric1 job1
ecs_api_a1_get_job_ids 200 type1 ric2 job2
ecs_api_a1_get_job_ids 200 type2 ric3 job3

ecs_api_a1_get_job 200 type2 job3 http://localhost:80/target3 ric3 testdata/ecs/job-template.json

ecs_api_a1_get_job_status 200 type2 job3 ENABLED

ecs_api_edp_get_producer_jobs 200 prod-a job1 type1 http://localhost:80/target1 testdata/ecs/job-template.json job2 type1 http://localhost:80/target2 testdata/ecs/job-template.json
ecs_api_edp_get_producer_jobs 200 prod-b job3 type2 http://localhost:80/target3 testdata/ecs/job-template.json


#prod-c (no types)
ecs_api_edp_put_producer 201 prod-c http://producer-stub:8092/callbacks/create/prod-c http://producer-stub:8092/callbacks/delete/prod-c http://producer-stub:8092/callbacks/supervision/prod-c NOTYPE

ecs_api_edp_get_producer_ids 200 prod-a prod-b prod-c
ecs_api_edp_get_producer 200 prod-a http://producer-stub:8092/callbacks/create/prod-a http://producer-stub:8092/callbacks/delete/prod-a http://producer-stub:8092/callbacks/supervision/prod-a type1 testdata/ecs/ei-type-1.json
ecs_api_edp_get_producer 200 prod-b http://producer-stub:8092/callbacks/create/prod-b http://producer-stub:8092/callbacks/delete/prod-b http://producer-stub:8092/callbacks/supervision/prod-b type2 testdata/ecs/ei-type-2.json
ecs_api_edp_get_producer 200 prod-c http://producer-stub:8092/callbacks/create/prod-c http://producer-stub:8092/callbacks/delete/prod-c http://producer-stub:8092/callbacks/supervision/prod-c EMPTY

ecs_api_edp_get_producer_status 200 prod-c ENABLED

ecs_api_a1_delete_job 204 type2 job3

ecs_api_edp_delete_producer 204 prod-b


check_sndc_logs

check_ecs_logs

store_logs END

#### TEST COMPLETE ####


print_result

auto_clean_containers
