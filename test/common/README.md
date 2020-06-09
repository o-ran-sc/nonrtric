## Introduction ##
This dir contains most scripts needed for the auto-test environment. There are scripts with functions to adapt to the apis of the components of the Non-RT RIC; Policy Agent, A1 Controller and Ric (A1) simulator.
Some of the scripts can also be used for other kinds of tests, for example basic tests.

## Overview for common test scripts and files ##

`test_env.sh` \
Common env variables for test in the auto-test dir. All configuration of port numbers, image names and version etc shall be made in this file.
Used by the auto test scripts/suites but could be used for other test script as well.

`testcase_common.sh` \
Common functions for auto test cases in the auto-test dir. This script is the foundation of test auto environment which sets up images and enviroment variables needed by this script as well as the script adapting to the APIs.
The included functions are described in detail further below.

`testsuite_common.sh` \
Common functions for running two or more auto test scripts as a suite.

`agent_api_functions.sh` \
Contains functions for adapting towards the Policy Agent API, also via dmaap (using a message-router stub interface)

`controller_api_functions.sh` \
Contains functions for adaping towards the A1-controller API.

`ricsimulator_api_functions.sh` \
Contains functions for adapting towards the RIC (A1) simulator admin API.

`compare_json.py` \
A python script to compare two json obects for equality. Note that the comparsion always sort json-arrays before comparing (that is, it does not care about the order of items within the array). In addition, the target json object may specify individual parameter values where equality is 'dont care'.

`count_json_elements.py` \
A python script returning the number of items in a json array.

`create_policies_process.py` \
A python script to create a batch of policies. The script is intended to run in a number of processes to create policies in parallel.

`create_rics_json.py` \
A python script to create a json file from a formatted string of ric info. Helper for the test enviroment.

`delete_policies_process.py` \
A python script to delete a batch of policies. The script is intended to run in a number of processes to delete policies in parallel.

`extract_sdnc_reply.py` \
A python script to extract the information from an sdnc (A1 Controller) reply json. Helper for the test environment.

`do_curl_function.sh`
A script for executing a curl call with a specific url and optional payload. It also compare the response with an expected result in terms of response code and optional returned payload. Intended to be used by test script (for example basic test scripts of other components)



### Descriptions of functions in testcase_common.sh ###

#### Script args ####
The script can be started with these arguments

| arg list |
|--|
| `local|remote|remote-remove [auto-clean] [--stop-at-error] [--use-local-image <app-nam> [<app-name>]*]` |

| parameter | description |
|-|-|
| `local` | only locally built images (in local docker respository) will be used for the Non-RT RIC components. CBS, Consul, DB will still use remote nexus images |
| `remote` | only remote images from nexus will be used. Images pulled if not present in local docker repository |
| `remote-remove` | same as remote but all images are removed first so that fresh images are pulled when running |
| `auto-clean` | all containers will be automatically stopped and removed when the test case is complete. Requires the function 'auto_clean_containers' to be included last in the applicable auto-test script |
| `--stop-at-error` | intended for debugging and make the script stop at first 'FAIL' and save all logs with a prefix 'STOP_AT_ERROR' |
| `--use-local-image <app-nam> [<app-name>]*` | nnly applicable when running as 'remote' or 'remote-remove'. Mainly for debugging when a locally built image shall be used together with other remote images from nexus.Accepts a space separated list of PA, CP, RICSIM, SDNC for Policy Agent, Control Panel, A1-controller and the Ric simulator |
| `--ricsim-prefix <prefix>` | use another prefix for the ric simulator container name than the standard 'ricsim'. Note that the testscript has to read and use the env var `$RIC_SIM_PREFIX` instead of a hardcoded name of the ric(s). |

#### Function: print_result ####
Print a test report of an auto-test script.
| arg list |
|--|
| None |

#### Function: start_timer ####
Start a timer for time measurement. Only one timer can be running.
| arg list |
|--|
| None - but any args will be printed (It is good practice to use same args for this function as for the `print_timer`) |

#### Function: print_timer ####
Print the value of the timer (in seconds) previously started by 'start_timer'. (Note that timer is still running after this function). The result of the timer as well as the args to the function will also be printed in the test report.
| arg list |
|--|
| `<timer-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `<timer-message-to-print>` | Any text message to be printed along with the timer result.(It is good practice to use same args for this function as for the `start_timer`) |

#### Function: print_and_reset_timer ####
Print the value of the timer (in seconds) previously started by 'start_timer'. Also reset the timer to 0. The result of the timer as well as the args to the function will also be printed in the test report.
| arg list |
|--|
| `<timer-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `<timer-message-to-print>` | Any text message to be printed along with the timer result.(It is good practice to use same args for this function as for the `start_timer`) |

#### Function: deviation ####
Mark a test as a deviation from the requirements. The list of deviations will be printed in the test report.
| arg list |
|--|
| `<deviation-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `<deviation-message-to-print>` | Any text message describing the deviation. The text will also be printed in the test report. The intention is to mark known deviations, compared to required functionality |

#### Function: clean_containers ####
Stop and remove all containers. Containers not part of the test are not affected.
| arg list |
|--|
| None |

#### Function: auto_clean_containers ####
Stop and remove all containers. Containers not part of the test are not affected. This function has effect only if the test script is started with arg `auto-clean`. This intention is to use this function as the last step in an auto-test script.
| arg list |
|--|
| None |

#### Function: sleep_wait ####
Make the script sleep for a number of seconds.
| arg list |
|--|
| `<sleep-time-in-sec> [<any-text-in-quotes-to-be-printed>]` |

| parameter | description |
| --------- | ----------- |
| `<sleep-time-in-sec> ` | Number of seconds to sleep |
| `<any-text-in-quotes-to-be-printed>` | Optional. The text will be printed, if present |

#### Function: generate_uuid ####
Geneate a UUID prefix to use along with the policy instance number when creating/deleting policies. Sets the env var UUID.
UUID is then automatically added to the policy id in GET/PUT/DELETE.
| arg list |
|--|
| None |

#### Function: consul_config_app ####
Function to load a json config from a file into consul for the Policy Agent

| arg list |
|--|
| `<json-config-file>` |

| parameter | description |
| --------- | ----------- |
| `<json-config-file>` | The path to the json file to be loaded to Consul/CBS |

#### Function: prepare_consul_config ####
Function to prepare a Consul config based on the previously configured (and started simulators). Note that all simulator must be running and the test script has to configure if http or https shall be used for the components (this is done by the functions 'use_simulator_http', 'use_simulator_https', 'use_sdnc_http', 'use_sdnc_https', 'use_mr_http', 'use_mr_https')
| arg list |
|--|
| `<deviation-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `SDNC|SDNC_ONAP|NOSDNC` | Configure based on a1-controller (SNDC), a1-adapter (SDNC_ONAP) or without a controller/adapter (NOSDNC) |
| `<output-file>` | The path to the json output file containing the prepared config. This file is used in 'consul_config_app'  |

#### Function: start_consul_cbs ####
Start the Consul and CBS containers
| arg list |
|--|
| None |

#### Function: use_simulator_http ####
Use http for all API calls (A1) toward the simulator. This is the default. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

#### Function: use_simulator_https ####
Use https for all API calls (A1) toward the simulator. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

#### Function: start_ric_simulators ####
Start a group of simulator where a group may contain 1 more simulators.
| arg list |
|--|
| `ricsim_g1|ricsim_g2|ricsim_g3 <count> <interface-id>` |

| parameter | description |
| --------- | ----------- |
| `ricsim_g1|ricsim_g2|ricsim_g3` | Base name of the simulator. Each instance will have an postfix instance id added, starting on '1'. For examplle 'ricsim_g1_1', 'ricsim_g1_2' etc  |
|`<count>`| And integer, 1 or greater. Specifies the number of simulators to start|
|`<interface-id>`| Shall be the interface id of the simulator. See the repo 'a1-interface' for the available ids. |

#### Function: start_control_panel ####
Start the Control Panel container
| arg list |
|--|
| None |

#### Function: start_sdnc ####
Start the SDNC A1 Controller container and its database container
| arg list |
|--|
| None |

#### Function: use_sdnc_http ####
Use http for all API calls towards the SDNC A1 Controller. This is the default. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

#### Function: use_sdnc_http ####
Use https for all API calls towards the SDNC A1 Controller. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

#### Function: start_sdnc_onap ####
Start the SDNC A1 Adapter container and its database container
| arg list |
|--|
| None |

#### Function: config_sdnc_onap ####
Configure the SDNC A1 adapter - Not implemented
| arg list |
|--|
| None |

#### Function: start_mr ####
Start the Message Router stub interface container
| arg list |
|--|
| None |

#### Function: use_mr_http ####
Use http for all Dmaap calls to the MR. This is the default. The admin API is not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

#### Function: use_mr_https ####
Use https for all Dmaap call to the MR. The admin API is not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

#### Function: start_cr ####
Start the Callback Receiver container
| arg list |
|--|
| None |

#### Function: start_policy_agent ####
Start the Policy Agent container. If the test script is configured to use a stand alone Policy Agent (for example other container or stand alone app) the script will prompt for starting the stand alone Policy Agent.
| arg list |
|--|
| None |

#### Function: use_agent_stand_alone ####
Configure to run the Policy Agent as a stand alone container or app. See also 'start_policy_agent'
| arg list |
|--|
| None |

#### Function: use_agent_rest_http ####
Use http for all API calls to the Policy Agent. This is the default.
| arg list |
|--|
| None |

#### Function: use_agent_rest_https ####
Use https for all API calls to the Policy Agent.
| arg list |
|--|
| None |

#### Function: use_agent_dmaap ####
Send and recieve all API calls to the Policy Agent over Dmaap via the MR.
| arg list |
|--|
| None |

#### Function: set_agent_debug ####
Configure the Policy Agent log on debug level. The Policy Agent must be running.
| arg list |
|--|
| None |

#### Function: set_agent_trace ####
Configure the Policy Agent log on trace level. The Policy Agent must be running.
| arg list |
|--|
| None |

#### Function: use_agent_retries ####
Configure the Policy Agent to make upto 5 retries if an API calls return any of the specified http return codes.
| arg list |
|--|
| `[<response-code>]*` |

| parameter | description |
| --------- | ----------- |
| `[<response-code>]*` | A space separated list of http response codes, may be empty to reset to 'no codes'.  |

#### Function: check_policy_agent_logs ####
Check the Policy Agent log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

#### Function: check_control_panel_logs ####
Check the Control Panel log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

#### Function: store_logs ####
Take a snap-shot of all logs for all running containers and stores them in `./logs/<ATC-id>`. All logs will get the specified prefix in the file name. In general, one of the last steps in an auto-test script shall be to call this function. If logs shall be taken several times during a test script, different prefixes shall be used each time.
| arg list |
|--|
| `<logfile-prefix>` |

| parameter | description |
| --------- | ----------- |
| `<logfile-prefix>` | Log file prefix  |


#### Function: cr_equal ####
TBD

#### Function: mr_equal ####
TBD

#### Function: mr_greater ####
TBD

#### Function: mr_read ####
TBD

#### Function: mr_print ####
TBD

### Descriptions of functions in testsuite_common.sh ###
TBD
### Descriptions of functions in agent_api_function.sh ###
TBD
### Descriptions of functions in ricsimulator_api_functions.sh ###
TBD
### Descriptions of functions in controller_api_functions.sh ###
TBD

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