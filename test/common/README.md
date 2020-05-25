## Description for common test scripts

`test_env.sh` \
Common env variables for test in the auto-test dir.
Used by the auto test cases/suites but could be used for other test script as well.

`testcase_common.sh` \
Common functions for auto test cases in the auto-test dir.
A subset of the functions could be used in other test scripts as well.

###Descriptions of functions in testcase_common.sh

`clean_containers` \
Stop and remove all containers including Policy Agent apps and simulators

`start_simulators` \
Start all simulators in the simulator group

`consul_config_app  ` \
Configure consul with json file with app config for a Policy Agent instance using the Policy Agent
instance id and the json file.

`start_policy_agent` \
Start the Policy Agent application.

`check_policy_agent_logs`
Check the Policy Agent application log for WARN and ERR messages and print the count.
`store_logs`
Store all Policy Agent app and simulators log to the test case log dir. All logs get a prefix to
separate logs stored at different steps in the test script.
If logs need to be stored in several locations, use different prefix to easily identify the location
when the logs where taken.

## License

Copyright (C) 2020 Nordix Foundation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.