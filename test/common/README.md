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



## Descriptions of functions in testcase_common.sh ##

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
Tests if a variable value in the Callback Receiver (CR) simulator is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'cr' dir for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the CR  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

#### Function: mr_equal ####
Tests if a variable value in the Message Router (MR) simulator is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the MR  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

#### Function: mr_greater ####
Tests if a variable value in the Message Router (MR) simulator is greater than a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is greater than the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes greater than the target value or not.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the MR  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to become grater than the target value  |

#### Function: mr_read ####
Reads the value of a variable in the Message Router (MR) simulator. The value is intended to be passed to a env variable in the test script.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the MR  |

#### Function: mr_print ####
Prints the value of a variable in the Message Router (MR) simulator.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the MR  |

## Descriptions of functions in testsuite_common.sh ##
#### Function: suite_setup ####
Sets up the test suite and prints out a heading.
| arg list |
|--|
| None |

#### suite_complete ####
Print out the overall result of the executed test cases.
| arg list |
|--|
| None |

## Descriptions of functions in agent_api_function.sh ##
#### Function: api_equal() ####

Tests if the array length of a json array in the Policy Agent simulator is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the array length is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the array length becomes equal to the target value or not.
See the 'cr' dir for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Relative url. Example 'json:policy_types' - checks the json array length of the url /policy_types  |
| `<target-value>` | Target value for the length  |
| `<timeout-in-sec>` | Max time to wait for the length to reach the target value  |

#### Function: api_get_policies() ####
Test of GET '/policies' and optional check of the array of returned policies.
To test the response code only, provide the response code parameter as well as the following three parameters.
To also test the response payload add the 'NOID' for an expected empty array or repeat the last five parameters for each expected policy.
| arg list |
|--|
| `<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <template-file>]*]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` | Id of the ric  |
| `NORIC` | Indicator that no ric is provided  |
| `<service-id>` | Id of the service  |
| `NOSERVICE` | Indicator that no service id is provided  |
| `<policy-type-id>` |  Id of the policy type |
| `NOTYPE` | Indicator that no type id is provided  |
| `NOID` |  Indicator that no policy id is provided - indicate empty list of policies|
| `<policy-id>` |  Id of the policy |
| `EMPTY` |  Indicate for the special empty policy type |
| `<template-file>` |  Path to the template file for the policy (same template used when creating the policy) |


#### Function: api_get_policy() ####
Test of GET /policy and optional check of the returned json payload.
To test the the response code only, provide the expected response code and policy id.
To test the contents of the returned json payload, add a path to the template file used when creating the policy.
| arg list |
|--|
| `<response-code>  <policy-id> [<template-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `<template-file>` |  Path to the template file for the policy (same template used when creating the policy) |

#### Function: api_put_policy() ####
Test of PUT '/policy'.
To test the response code only, provide the response code parameter as well as the following three parameters.
To also test the response payload add the 'NOID' for an expected empty array or repeat the last five parameters for each expected policy.
| arg list |
|--|
| `<response-code> <service-name> <ric-id> <policytype-id> <policy-id> <transient> <template-file> [<count>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<ric-id>` | Id of the ric  |
| `<policy-type-id>` |  Id of the policy type |
| `<policy-id>` |  Id of the policy. This value shall be a numeric value if more than one policy shall be created |
| `transient>` |  Transient 'true' or 'false'. 'NOTRANSIENT' can be used to indicate using the default value (no transient value provided) |
| `<template-file>` |  Path to the template file for the policy |
| `<count>` |  An optional count (default is 1). If a value greater than 1 is given, the policy ids will use the given policy id as the first id and add 1 to that id for each new policy |

#### Function: api_put_policy_batch() ####
This tests the same as function 'api_put_policy' except that all put requests are sent to dmaap in one go and then the responses are polled one by one.
If the agent api is not configured to use dmaap (see 'use_agent_dmaap', 'use_agent_rest_http' and 'use_agent_rest_https'), an error message is printed.
For arg list and parameters, see 'api_put_policy'.

#### Function: api_put_policy_parallel() ####
This tests the same as function 'api_put_policy' except that the policy create is spread out over a number of processes and it only uses the agent rest API. The total number of policies created is determined by the product of the parameters 'number-of-rics' and 'count'. The parameter 'number-of-threads' shall be selected to be not evenly divisible by the product of the parameters 'number-of-rics' and 'count' - this is to ensure that one process does not handle the creation of all the policies in one ric.
| arg list |
|--|
| `<response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>`

| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<ric-id-base>` | The base id of the rics, ie ric id without the sequence number. The sequence number is added during processing  |
| `<number-of-rics>` | The number of rics, assuming the first index is '1'. The index is added to the 'ric-id-base' id  |
| `<policy-type-id>` |  Id of the policy type |
| `<policy-start-id>` |  Id of the policy. This value shall be a numeric value and will be the id of the first policy |
| `transient>` |  Transient 'true' or 'false'. 'NOTRANSIENT' can be used to indicate using the default value (no transient value provide) |
| `<template-file>` |  Path to the template file for the policy |
| `<count-per-ric>` |  Number of policies per ric |
| `<number-of-threads>` |  Number of threads (processes) to run in parallel |

#### Function: api_delete_policy() ####
This tests the DELETE /policy. Removes the indicated policy or a 'count' number of policies starting with 'policy-id' as the first id.
| arg list |
|--|
| `<response-code> <policy-id> [<count>]`

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `<count>` |  An optional count of policies to delete. The 'policy-id' will be the first id to be deleted. |

#### Function: api_delete_policy_batch() ####
This tests the same as function 'api_delete_policy' except that all delete requests are sent to dmaap in one go and then the responses are polled one by one.
If the agent api is not configured to used dmaap (see 'use_agent_dmaap', 'use_agent_rest_http' and 'use_agent_rest_https'), an error message is printed.
For arg list and parameters, see 'api_delete_policy'.

#### Function: api_delete_policy_parallel() ####
This tests the same as function 'api_delete_policy' except that the policy delete is spread out over a number of processes and it only uses the agent rest API. The total number of policies deleted is determined by the product of the parameters 'number-of-rics' and 'count'. The parameter 'number-of-threads' shall be selected to be not evenly divisible by the product of the parameters 'number-of-rics' and 'count' - this is to ensure that one process does not handle the deletion of all the policies in one ric.
| arg list |
|--|
| `<response-code> <ric-id-base> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>`

| `<response-code>` | Expected http response code |
| `<ric-id-base>` | The base id of the rics, ie ric id without the sequence number. The sequence number is added during processing  |
| `<number-of-rics>` | The number of rics, assuming the first index is '1'  |
| `<policy-start-id>` |  Id of the policy. This value shall be a numeric value and will be the id of the first policy |
| `<count-per-ric>` |  Number of policies per ric |
| `<number-of-threads>` |  Number of threads (processes) to run in parallel |


#### Function: api_get_policy_ids() ####

Test of GET '/policy_ids'.
To test response code only, provide the response code parameter as well as the following three parameters.
To also test the response payload add the 'NOID' for an expected empty array or repeat the 'policy-instance-id' for each expected policy id.
| arg list |
|--|
| `<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <type-id>|NOTYPE ([<policy-instance-id]*|NOID)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` | Id of the ric  |
| `NORIC` | Indicator that no ric is provided  |
| `<service-id>` | Id of the service  |
| `NOSERVICE` | Indicator that no service id is provided  |
| `type-id>` |  Id of the policy type |
| `NOTYPE` | Indicator that no type id is provided  |
| `NOID` |  Indicator that no policy id is provided - indicate empty list of policies|
| `<policy-instance-id>` |  Id of the policy |

#### Function: api_get_policy_schema() ####
Test of GET /policy_schema and optional check of the returned json schema.
To test the response code only, provide the expected response code and policy type id.
To test the contents of the returned json schema, add a path to a schema file to compare with.
| arg list |
|--|
| `<response-code> <policy-type-id> [<schema-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-type-id>` |  Id of the policy type |
| `<schema-file>` |  Path to the schema file for the policy type |

#### Function: api_get_policy_schemas() ####
Test of GET /policy_schemas and optional check of the returned json schemas.
To test the response code only, provide the expected response code and ric id (or NORIC if no ric is given).
To test the contents of the returned json schema, add a path to a schema file to compare with (or NOFILE to represent an empty '{}' type)
| arg list |
|--|
| `<response-code>  <ric-id>|NORIC [<schema-file>|NOFILE]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `NORIC` |  No ric id given |
| `<schema-file>` |  Path to the schema file for the policy type |
| `NOFILE` |  Indicate the template for an empty type |

#### Function: api_get_policy_status() ####
Test of GET /policy_status.
| arg list |
|--|
| `<response-code> <policy-id> (STD <enforce-status> [<reason>])|(OSC <instance-status> <has-been-deleted>)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `STD` |  Indicator of status of Standarized A1 |
| `<enforce-status>` |  Enforcement status |
| `<reason>` |  Optional reason |
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `<instance-status>` |  Instance status |
| `<has-been-deleted>` |  Deleted status, true or false |

#### Function: api_get_policy_types() ####
Test of GET /policy_types and optional check of the returned ids.
To test the response code only, provide the expected response code and ric id (or NORIC if no ric is given).
To test the contents of the returned json payload, add the list of expected policy type id (or 'EMPTY' for the '{}' type)

| arg list |
|--|
| `<response-code> [<ric-id>|NORIC [<policy-type-id>|EMPTY [<policy-type-id>]*]]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `NORIC` |  No ric id given |
| `<policy-type-id>` |  Id of the policy type |
| `EMPTY` |  Indicate the empty type |

#### Function: api_get_status() ####
Test of GET /status
| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

#### Function: api_get_ric() ####
Test of GET /ric
To test the response code only, provide the expected response code and managed element id.
To test the returned ric id, provide the expected ric id.
| arg list |
|--|
| `<reponse-code> <managed-element-id> [<ric-id>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<managed-element-id>` |  Id of the managed element |
| `<ric-id>` |  Id of the ric |

#### Function: api_get_rics() ####
Test of GET /rics and optional check of the returned json payload (ricinfo).
To test the response code only, provide the expected response code and policy type id (or NOTYPE if no type is given).
To test also the returned payload, add the formatted string of info in the returned payload.
Format of ricinfo: '<ric-id>:<list-of-mes>:<list-of-policy-type-ids>'
Example `<space-separate-string-of-ricinfo> = "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2,4 ricsim_g1_1:me2_........."`
| arg list |
|--|
| `<reponse-code> <policy-type-id>|NOTYPE [<space-separate-string-of-ricinfo>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-type-id>` |  Policy type id of the ric |
| `NOTYPE>` |  No type given |
| `<space-separate-string-of-ricinfo>` |  A space separated string of ric info - needs to be quoted |

#### Function: api_put_service() ####
Test of PUT /service
| arg list |
|--|
| `<response-code>  <service-name> <keepalive-timeout> <callbackurl>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |
| `<keepalive-timeout>` |  Timeout value |
| `<callbackurl>` |  Callback url |

#### Function: api_get_services() ####
Test of GET /service and optional check of the returned json payload.
To test only the response code, omit all parameters except the expected response code.
To test the returned json, provide the parameters after the response code.
| arg list |
|--|
| `<response-code> [ (<query-service-name> <target-service-name> <keepalive-timeout> <callbackurl>) | (NOSERVICE <target-service-name> <keepalive-timeout> <callbackurl> [<target-service-name> <keepalive-timeout> <callbackurl>]* )]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| <query-service-name>` |  Service name for the query |
| <target-service-name>` |  Target service name|
| `<keepalive-timeout>` |  Timeout value |
| `<callbackurl>` |  Callback url |
| `NOSERVICE` |  Indicator of no target service name |

#### Function: api_get_service_ids() ####
Test of GET /services
| arg list |
|--|
| `<response-code> [<service-name>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

#### Function: api_delete_services() ####
Test of DELETE /services
| arg list |
|--|
| `<response-code> [<service-name>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

#### Function: api_put_services_keepalive() ####
Test of PUT /services/keepalive
| arg list |
|--|
| <response-code> <service-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Descriptions of functions in ricsimulator_api_functions.sh ##
The functions below only use the admin interface of the simulator, no usage of the A1 interface.

#### Function: sim_equal ####
Tests if a variable value in the RIC simulator is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'a1-interface' repo for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the ric simulator  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

#### Function: sim_print ####
Prints the value of a variable in the RIC simulator.
See the 'a1-interface' repo for more details.
| arg list |
|--|
| `<variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the RIC simulator  |


#### Function: sim_contains_str ####
Tests if a variable value in the RIC simulator contains a target string.
Without the timeout, the test sets pass or fail immediately depending on if the variable contains the target string or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value contains the target string or not.
See the 'a1-interface' repo for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the ric simulator  |
| `<target-value>` | Target substring for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

#### Function: sim_put_policy_type ####
Loads a policy type to the simulator
| arg list |
|--|
| `<response-code> <ric-id> <policy-type-id> <policy-type-file>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `<policy-type-id>` |  Id of the policy type |
| `<policy-type-file>` |  Path to the schema file of the policy type |

#### Function: sim_delete_policy_type ####
Deletes a policy type from the simulator
| arg list |
|--|
| `<response-code> <ric-id> <policy_type_id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `<policy-type-id>` |  Id of the policy type |

#### Function: sim_post_delete_instances ####
Deletes all instances (and status), for one ric
| arg list |
|--|
| `<response-code> <ric-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |


#### Function: sim_post_delete_all ####
Deletes all types, instances (and status), for one ric
| arg list |
|--|
| `<response-code> <ric-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |

#### Function: sim_post_forcedresponse ####
Sets (or resets) response code for next (one) A1 message, for one ric.
The intention is to simulate error response on the A1 interface.
| arg list |
|--|
| `<response-code> <ric-id> [<forced_response_code>]`|

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `<forced_response_code>` |  Http response code to send |

#### Function: sim_post_forcedelay ####
Sets (or resets) A1 response delay, for one ric
The intention is to delay responses on the A1 interface. Setting remains until removed.
| arg list |
|--|
| `<response-code> <ric-id> [<delay-in-seconds>]`|

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `<delay-in-seconds>` |  Delay in seconds. If omitted, the delay is removed |


## Descriptions of functions in controller_api_functions.sh ##
The file contains a selection of the possible API tests towards the a1-controller

#### Function: controller_api_get_A1_policy_ids ####
Test of GET policy ids towards OSC or STD type simulator.
To test response code only, provide the response code, 'OSC' + policy type or 'STD'
To test the response payload, include the ids of the expexted response.
| arg list |
|--|
| `<response-code> (OSC <ric-id> <policy-type-id> [ <policy-id> [<policy-id>]* ]) | ( STD <ric-id> [ <policy-id> [<policy-id>]* ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `<ric-id>` | Id of the ric  |
| `policy-type-id>` |  Id of the policy type |
| `<policy-id>` |  Id of the policy |
| `STD` |  Indicator of status of Standarized A1 |


#### Function: controller_api_get_A1_policy_type ####
Test of GET a policy type (OSC only)
| arg list |
|--|
| `<response-code> OSC <ric-id> <policy-type-id> [<policy-type-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `<ric-id>` | Id of the ric  |
| `policy-type-id>` |  Id of the policy type |
| `policy-type-file>` |  Optional schema file to compare the returned type with |

#### Function: controller_api_delete_A1_policy ####
Deletes a policy instance
| arg list |
|--|
| `(STD <ric-id> <policy-id>) | (OSC <ric-id> <policy-type-id> <policy-id>)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `STD` |  Indicator of status of Standarized A1 |
| `<ric-id>` | Id of the ric  |
| `<policy-id>` |  Id of the policy |
| `policy-type-id>` |  Id of the policy type |
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `policy-type-file>` |  Optional schema file to compare the returned type with |

#### Function: controller_api_put_A1_policy ####
Creates a policy instance
| arg list |
|--|
| `<response-code> (STD <ric-id> <policy-id> <template-file> ) | (OSC <ric-id> <policy-type-id> <policy-id> <template-file>)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `STD` |  Indicator of status of Standarized A1 |
| `<ric-id>` | Id of the ric  |
| `<policy-id>` |  Id of the policy |
| `<template-file>` |  Path to the template file of the policy|
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `<policy-type-id>` |  Id of the policy type |

#### Function: controller_api_get_A1_policy_status ####
Checks the status of a policy
 arg list |
|--|
| `<response-code> (STD <ric-id> <policy-id> <enforce-status> [<reason>]) | (OSC <ric-id> <policy-type-id> <policy-id> <instance-status> <has-been-deleted>)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `STD` |  Indicator of status of Standarized A1 |
| `<ric-id>` | Id of the ric  |
| `<policy-id>` |  Id of the policy |
| `<enforce-status>` |  Enforcement status |
| `<reason>` |  Optional reason |
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `<policy-type-id>` |  Id of the policy type |
| `<instance-status>` |  Instance status |
| `<has-been-deleted>` |  Deleted status, true or false |

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