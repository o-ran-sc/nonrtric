# Introduction #
This dir contains most scripts needed for the auto-test environment. There are scripts with functions to adapt to the apis of the components of the Non-RT RIC; Policy Agent, A1 Controller and Ric (A1) simulator. The test environment supports both test with docker and kubernetes(still experimental)
Some of the scripts can also be used for other kinds of tests, for example basic tests.

## Overview for common test scripts and files ##

`agent_api_functions.sh` \
Contains functions for adapting towards the Policy Management Service (PMS) API, also via dmaap (using a message-router stub interface)

`api_curl.sh` \
A common curl based function for the agent and ecs apis. Also partly used for the Callback receiver and RAPP Catalogue apis.

`clean-kube.sh` \
Cleans all services, deployments, pods, replica set etc started by the test environment in kubernetes.

`compare_json.py` \
A python script to compare two json obects for equality. Note that the comparsion always sort json-arrays before comparing (that is, it does not care about the order of items within the array). In addition, the target json object may specify individual parameter values where equality is 'dont care'.

`consul_cbs_function.sh` \
Contains functions for managing Consul and CBS as well as create the configuration for the PMS.

`control_panel_api_function.sh` \
Contains functions for managing Control Panel.

`controller_api_functions.sh` \
Contains functions for adaping towards the A1-controller API.

`count_json_elements.py` \
A python script returning the number of items in a json array.

`cr_api_functions.sh` \
Contains functions for adapting towards the Callback receiver for checking received callback event.

`create_policies_process.py` \
A python script to create a batch of policies. The script is intended to run in a number of processes to create policies in parallel.

`create_rics_json.py` \
A python script to create a json file from a formatted string of ric info. Helper for the test enviroment.

`delete_policies_process.py` \
A python script to delete a batch of policies. The script is intended to run in a number of processes to delete policies in parallel.

`do_curl_function.sh`
A script for executing a curl call with a specific url and optional payload. It also compare the response with an expected result in terms of response code and optional returned payload. Intended to be used by test script (for example basic test scripts of other components)

`ecs_api_functions.sh` \
Contains functions for adapting towards the ECS API

`extract_sdnc_reply.py` \
A python script to extract the information from an sdnc (A1 Controller) reply json. Helper for the test environment.

`gateway_api_functions.sh` \
Contains functions for managing the Non-RT RIC Gateway

`http_proxy_api_functions.sh` \
Contains functions for managing the Http Proxy

`kube_proxy_api_functions.sh` \
Contains functions for managing the Kube Proxy - to gain access to all services pod inside a kube cluster.

`mr_api_functions.sh` \
Contains functions for managing the MR Stub and the Dmaap Message Router

`prodstub_api_functions.sh` \
Contains functions for adapting towards the Producer stub interface - simulates a producer.

`rapp_catalogue_api_functions.sh` \
Contains functions for adapting towards the RAPP Catalogue.

`ricsimulator_api_functions.sh` \
Contains functions for adapting towards the RIC (A1) simulator admin API.

`test_env*.sh` \
Common env variables for test in the auto-test dir. All configuration of port numbers, image names and version etc shall be made in this file.
Used by the auto test scripts/suites but could be used for other test script as well. The test cases shall be started with the file for the intended target using command line argument '--env-file'.

`testcase_common.sh` \
Common functions for auto test cases in the auto-test dir. This script is the foundation of test auto environment which sets up images and enviroment variables needed by this script as well as the script adapting to the APIs.
The included functions are described in detail further below.

`testsuite_common.sh` \
Common functions for running two or more auto test scripts as a suite.

## Integration of a new applicaton

Integration a new application to the test environment involves the following steps.

* Choose a short name for the application. Should be a uppcase name. For example, the NonRTRIC Gateway has NGW as short name.
This short name shall be added to the testengine_config.sh. See that file for detailed instructions.

* Create a file in this directory using the pattern `<application-name>_api_functions.sh`.
This file must implement the following functions used by the test engine. Note that functions must include the application short name in the function name. If the application does not run in kubernetes, then the last three functions in the list can be omitted.

| Function |
|--|
| __<app-short_name>_imagesetup |
| __<app-short_name>_imagepull |
| __<app-short_name>_imagebuild |
| __<app-short_name>_image_data |
| __<app-short_name>_kube_scale_zero |
| __<app-short_name>_kube_scale_zero_and_wait |
| __<app-short_name>_kube_delete_all |

In addition, all other functions used for testing of the application shall also be added to the file. For example functions to start the application, setting interface parameters as well as functions to send rest call towards the api of the application and validating the result.

* Add the application variables to api_curl.sh. This file contains a generic function to make rest calls to an api. It also supports switching betweeen direct rest calls or rest calls via messsage router.

* Create a directory beneath in the simulator-group dir. This new directory shall contain docker-compose files, config files (with or without variable substitutions) and kubernetes resource files.

All docker-compose files and all kubernetes resource files need to defined special lables. These lables are used by the test enginge to identify containers and resources started and used by the test engine.

| Label for docker compose | Description |
|--|--|
| nrttest_app | shall contain the application short name |
| nrttest_dp  | shall be set by a variable containing the display name, a short textual description of the applicaion |

| Label for kubernetes resource | Description |
|--|--|
| autotest | shall contain the application short name |

* Add mandatory image(s) and image tag(s) to the appropriate environment files for each release in the file(s) `test_env_<system>-<release-name>`.
In addition, all other needed environment shall also be defined in these file.


# Description of functions in testcase_common.sh #

## Script args ##
The script can be started with these arguments

| arg list |
|--|
| `remote|remote-remove docker|kube --env-file <environment-filename> [release] [auto-clean] [--stop-at-error] [--ricsim-prefix <prefix> ] [--use-local-image <app-nam>+]  [--use-snapshot-image <app-nam>+] [--use-staging-image <app-nam>+] [--use-release-image <app-nam>+] [--image-repo <repo-address] [--repo-policy local|remote] [--cluster-timeout <timeout-in seconds>]` |

| parameter | description |
|-|-|
| `remote` | Use images from remote repositories. Can be overridden for individual images using the '--use_xxx' flags |
| `remote-remove` | Same as 'remote' but will also try to pull fresh images from remote repositories |
| `docker` | Use docker environment for test |
| `kuber` | Use kubernetes environment for test. Requires a kubernetes minikube installation |
| `--env-file` | The script will use the supplied file to read environment variables from |
| `release` | If this flag is given the script will use release version of the images |
| `auto-clean` | If the function 'auto_clean_containers' is present in the end of the test script then all containers will be stopped and removed. If 'auto-clean' is not given then the function has no effect |
| `--stop-at-error` | The script will stop when the first failed test or configuration |
| `--ricsim-prefix <prefix>` | The a1 simulator will use the supplied string as container prefix instead of 'ricsim'. Note that the testscript has to read and use the env var `$RIC_SIM_PREFIX` instead of a hardcoded name of the ric(s). |
| `--use-local-image` | The script will use local images for the supplied apps, space separated list of app short names |
| `--use-snapshot-image` | The script will use images from the nexus snapshot repo for the supplied apps, space separated list of app short names |
| `--use-staging-image` | The script will use images from the nexus staging repo for the supplied apps, space separated list of app short names |
| `--use-release-image` | The script will use images from the nexus release repo for the supplied apps, space separated list of app short names |
| `--image-repo` |  Url to optional image repo. Only locally built images will be re-tagged and pushed to this repo |
| `-repo-policy ` |  Policy controlling which images to re-tag and push to image repo in param --image-repo. Can be set to 'local' (push on locally built images) or 'remote' (push locally built images and images from nexus repo). Default is 'local' |
| `--cluster-timeout` |  Optional timeout for cluster where it takes time to obtain external ip/host-name. Timeout in seconds |
| `help` | Print this info along with the test script description and the list of app short names supported |

## Function: setup_testenvironment
Main function to setup the test environment before any tests are started.
Must be called right after sourcing all component scripts.
| arg list |
|--|
| None |

## Function: indent1 ##
Indent every line of a command output with one space char.
| arg list |
|--|
| None |

## Function: indent2 ##
Indent every line of a command output with two space chars.
| arg list |
|--|
| None |

## Function: print_result ##
Print a test report of an auto-test script.
| arg list |
|--|
| None |

## Function: start_timer ##
Start a timer for time measurement. Only one timer can be running.
| arg list |
|--|
| None - but any args will be printed (It is good practice to use same args for this function as for the `print_timer`) |

## Function: print_timer ##
Print the value of the timer (in seconds) previously started by 'start_timer'. (Note that timer is still running after this function). The result of the timer as well as the args to the function will also be printed in the test report.
| arg list |
|--|
| `<timer-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `<timer-message-to-print>` | Any text message to be printed along with the timer result.(It is good practice to use same args for this function as for the `start_timer`) |

## Function: print_and_reset_timer ##
Print the value of the timer (in seconds) previously started by 'start_timer'. Also reset the timer to 0. The result of the timer as well as the args to the function will also be printed in the test report.
| arg list |
|--|
| `<timer-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `<timer-message-to-print>` | Any text message to be printed along with the timer result.(It is good practice to use same args for this function as for the `start_timer`) |

## Function: deviation ##
Mark a test as a deviation from the requirements. The list of deviations will be printed in the test report.
| arg list |
|--|
| `<deviation-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `<deviation-message-to-print>` | Any text message describing the deviation. The text will also be printed in the test report. The intention is to mark known deviations, compared to required functionality |

## Function: clean_environment ##
Stop and remove all containers (docker) or resources (kubernetes). Containers not part of the test are not affected (docker only). Removes all resources started by previous kube tests (kube only).
| arg list |
|--|
| None |

## Function: auto_clean_containers ##
Same function as 'clean_environment'. This function has effect only if the test script is started with arg `auto-clean`. This intention is to use this function as the last step in an auto-test script.
| arg list |
|--|
| None |

## Function: sleep_wait ##
Make the script sleep for a number of seconds.
| arg list |
|--|
| `<sleep-time-in-sec> [<any-text-in-quotes-to-be-printed>]` |

| parameter | description |
| --------- | ----------- |
| `<sleep-time-in-sec> ` | Number of seconds to sleep |
| `<any-text-in-quotes-to-be-printed>` | Optional. The text will be printed, if present |

## Function: check_control_panel_logs ##
Check the Control Panel log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

## Function: store_logs ##
Take a snap-shot of all logs for all running containers and stores them in `./logs/<ATC-id>`. All logs will get the specified prefix in the file name. In general, one of the last steps in an auto-test script shall be to call this function. If logs shall be taken several times during a test script, different prefixes shall be used each time.
| arg list |
|--|
| `<logfile-prefix>` |

| parameter | description |
| --------- | ----------- |
| `<logfile-prefix>` | Log file prefix  |


# Description of functions in testsuite_common.sh #

## Function: suite_setup ##
Sets up the test suite and prints out a heading.
| arg list |
|--|
| None |

## suite_complete ##
Print out the overall result of the executed test cases.
| arg list |
|--|
| None |

# Description of functions in agent_api_functions.sh #

## General ##
Both PMS version 1 and 2 are supported. The version is controlled by the env variable `$PMS_VERSION` set in the test env file.
For api function in version 2, an url prefix is added if configured.

## Function: use_agent_rest_http ##
Use http for all API calls to the Policy Agent. This is the default.
| arg list |
|--|
| None |

## Function: use_agent_rest_https ##
Use https for all API calls to the Policy Agent.
| arg list |
|--|
| None |

## Function: use_agent_dmaap_http ##
Send and recieve all API calls to the Policy Agent over Dmaap via the MR over http.
| arg list |
|--|
| None |

## Function: use_agent_dmaap_https ##
Send and recieve all API calls to the Policy Agent over Dmaap via the MR over https.
| arg list |
|--|
| None |

## Function: start_policy_agent ##
Start the Policy Agent container or corresponding kube resources depending on docker/kube mode.
| arg list |
| `<logfile-prefix>` |
| (docker) `PROXY|NOPROXY <config-file>` |
| (kube) `PROXY|NOPROXY <config-file> [ <data-file> ]` |
| parameter | description |
| --------- | ----------- |
| `PROXY` | Configure with http proxy, if proxy is started  |
| `NOPROXY` | Configure without http proxy  |
| <config-file>` | Path to application.yaml  |
|  <data-file>` | Optional path to application_configuration.json  |

## Function: agent_load_config ##
Load the config into a config map (kubernetes only).
| arg list |
|  <data-file> ]` |
| parameter | description |
| --------- | ----------- |
|  <data-file>` | Path to application_configuration.json  |

## Function: set_agent_debug ##
Configure the Policy Agent log on debug level. The Policy Agent must be running.
| arg list |
|--|
| None |

## Function: set_agent_trace ##
Configure the Policy Agent log on trace level. The Policy Agent must be running.
| arg list |
|--|
| None |

## Function: use_agent_retries ##
Configure the Policy Agent to make upto 5 retries if an API calls return any of the specified http return codes.
| arg list |
|--|
| `[<response-code>]*` |


## Function: check_policy_agent_logs ##
Check the Policy Agent log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

## Function: api_equal() ##

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

## Function: api_get_policies() ##
Test of GET '/policies' or V2 GET '/v2/policy-instances' and optional check of the array of returned policies.
To test the response code only, provide the response code parameter as well as the following three parameters.
To also test the response payload add the 'NOID' for an expected empty array or repeat the last five/seven parameters for each expected policy.

| arg list |
|--|
| `<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <template-file>]*]` |

| arg list V2 |
|--|
| `<response-code> <ric-id>|NORIC <service-id>|NOSERVICE <policy-type-id>|NOTYPE [ NOID | [<policy-id> <ric-id> <service-id> EMPTY|<policy-type-id> <transient> <notification-url> <template-file>]*]` |

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
| `transient` |  Transient, true or false |
| `notification-url` |  Url for notifications |
| `<template-file>` |  Path to the template file for the policy (same template used when creating the policy) |


## Function: api_get_policy() ##
Test of GET '/policy' or V2 GET '/v2/policies/{policy_id}' and optional check of the returned json payload.
To test the the response code only, provide the expected response code and policy id.
To test the contents of the returned json payload, add a path to the template file used when creating the policy.

| arg list |
|--|
| `<response-code>  <policy-id> [<template-file>]` |

| arg list V2|
|--|
| `<response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>|NOTYPE <transient> <notification-url>|NOURL ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `<template-file>` |  Path to the template file for the policy (same template used when creating the policy) |
| `<service-id>` | Id of the service  |
| `<ric-id>` | Id of the ric  |
| `<policy-type-id>` |  Id of the policy type |
| `NOTYPE` | Indicator that no type id is provided  |
| `transient` |  Transient, true or false |
| `notification-url` |  Url for notifications |

## Function: api_put_policy() ##
Test of PUT '/policy' or V2 PUT '/policies'.
If more than one policy shall be created, add a count value to indicate the number of policies to create. Note that if more than one policy shall be created the provided policy-id must be numerical (will be used as the starting id).

| arg list |
|--|
| `<response-code> <service-name> <ric-id> <policytype-id> <policy-id> <transient> <template-file> [<count>]` |

| arg list V2 |
|--|
| `<response-code> <service-name> <ric-id> <policytype-id>|NOTYPE <policy-id> <transient>|NOTRANSIENT <notification-url>|NOURL <template-file> [<count>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<ric-id>` | Id of the ric  |
| `<policy-type-id>` |  Id of the policy type |
| `<policy-id>` |  Id of the policy. This value shall be a numeric value if more than one policy shall be created |
| `transient>` |  Transient 'true' or 'false'. 'NOTRANSIENT' can be used to indicate using the default value (no transient value provided) |
| `notification-url` |  Url for notifications |
|`NOURL`| Indicator for no url |
| `<template-file>` |  Path to the template file for the policy |
| `<count>` |  An optional count (default is 1). If a value greater than 1 is given, the policy ids will use the given policy id as the first id and add 1 to that id for each new policy |

## Function: api_put_policy_batch() ##
This tests the same as function 'api_put_policy' except that all put requests are sent to dmaap in one go and then the responses are polled one by one.
If the agent api is not configured to use dmaap (see 'use_agent_dmaap', 'use_agent_rest_http' and 'use_agent_rest_https'), an error message is printed.
For arg list and parameters, see 'api_put_policy'.

## Function: api_put_policy_parallel() ##
This tests the same as function 'api_put_policy' except that the policy create is spread out over a number of processes and it only uses the agent rest API. The total number of policies created is determined by the product of the parameters 'number-of-rics' and 'count'. The parameter 'number-of-threads' shall be selected to be not evenly divisible by the product of the parameters 'number-of-rics' and 'count' - this is to ensure that one process does not handle the creation of all the policies in one ric.

| arg list |
|--|
| `<response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>`

| arg list |
|--|
| `<response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>|NOURL <template-file> <count-per-ric> <number-of-threads>`

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<ric-id-base>` | The base id of the rics, ie ric id without the sequence number. The sequence number is added during processing  |
| `<number-of-rics>` | The number of rics, assuming the first index is '1'. The index is added to the 'ric-id-base' id  |
| `<policy-type-id>` |  Id of the policy type |
| `<policy-start-id>` |  Id of the policy. This value shall be a numeric value and will be the id of the first policy |
| `transient>` |  Transient 'true' or 'false'. 'NOTRANSIENT' can be used to indicate using the default value (no transient value provide) |
| `notification-url` |  Url for notifications |
| `<template-file>` |  Path to the template file for the policy |
| `<count-per-ric>` |  Number of policies per ric |
| `<number-of-threads>` |  Number of threads (processes) to run in parallel |

## Function: api_delete_policy() ##
This tests the DELETE '/policy' or V2 DELETE '/v2/policies/{policy_id}'. Removes the indicated policy or a 'count' number of policies starting with 'policy-id' as the first id.

| arg list |
|--|
| `<response-code> <policy-id> [<count>]`

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `<count>` |  An optional count of policies to delete. The 'policy-id' will be the first id to be deleted. |

## Function: api_delete_policy_batch() ##
This tests the same as function 'api_delete_policy' except that all delete requests are sent to dmaap in one go and then the responses are polled one by one.
If the agent api is not configured to used dmaap (see 'use_agent_dmaap', 'use_agent_rest_http' and 'use_agent_rest_https'), an error message is printed.
For arg list and parameters, see 'api_delete_policy'.

## Function: api_delete_policy_parallel() ##
This tests the same as function 'api_delete_policy' except that the policy delete is spread out over a number of processes and it only uses the agent rest API. The total number of policies deleted is determined by the product of the parameters 'number-of-rics' and 'count'. The parameter 'number-of-threads' shall be selected to be not evenly divisible by the product of the parameters 'number-of-rics' and 'count' - this is to ensure that one process does not handle the deletion of all the policies in one ric.

| arg list |
|--|
| `<response-code> <ric-id-base> <number-of-rics> <policy-start-id> <count-per-ric> <number-of-threads>`

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id-base>` | The base id of the rics, ie ric id without the sequence number. The sequence number is added during processing  |
| `<number-of-rics>` | The number of rics, assuming the first index is '1'  |
| `<policy-start-id>` |  Id of the policy. This value shall be a numeric value and will be the id of the first policy |
| `<count-per-ric>` |  Number of policies per ric |
| `<number-of-threads>` |  Number of threads (processes) to run in parallel |


## Function: api_get_policy_ids() ##

Test of GET '/policy_ids' or V2 GET '/v2/policies'.
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

## Function: api_get_policy_schema() ##
Test of V2 GET '/v2/policy-types/{policyTypeId}' and optional check of the returned json schema.
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

## Function: api_get_policy_schema() ##
Test of GET '/policy_schema' and optional check of the returned json schema.
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

## Function: api_get_policy_schemas() ##
Test of GET '/policy_schemas' and optional check of the returned json schemas.
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

## Function: api_get_policy_status() ##
Test of GET '/policy_status' or V2 GET '/policies/{policy_id}/status'.

| arg list |
|--|
| `<response-code> <policy-id> (STD|STD2 <enforce-status>|EMPTY [<reason>|EMPTY])|(OSC <instance-status> <has-been-deleted>)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `STD` |  Indicator of status of Standarized A1 |
| `STD2` |  Indicator of status of Standarized A1 version 2 |
| `<enforce-status>` |  Enforcement status |
| `<reason>` |  Optional reason |
| `EMPTY` |  Indicator of empty string status or reason |
| `OSC` |  Indicator of status of Non-Standarized OSC A1 |
| `<instance-status>` |  Instance status |
| `<has-been-deleted>` |  Deleted status, true or false |

## Function: api_get_policy_types() ##
Test of GET '/policy_types' or  V2 GET '/v2/policy-types' and optional check of the returned ids.
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

## Function: api_get_status() ##
Test of GET /status or V2 GET /status

| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

## Function: api_get_ric() ##
Test of GET '/ric' or V2 GET '/v2/rics/ric'
To test the response code only, provide the expected response code and managed element id.
To test the returned ric id, provide the expected ric id.

| arg list |
|--|
| `<reponse-code> <managed-element-id> [<ric-id>]` |

| arg list V2 |
|--|
| `<reponse-code> <management-element-id>|NOME <ric-id>|<NORIC> [<string-of-ricinfo>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<managed-element-id>` |  Id of the managed element |
| `NOME` |  Indicator for no ME |
| `ric-id` |  Id of the ric |
| `NORIC` |  Indicator no RIC |
| `string-of-ricinfo` |  String of ric info |

## Function: api_get_rics() ##
Test of GET '/rics' or V2 GET '/v2/rics' and optional check of the returned json payload (ricinfo).
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

## Function: api_put_service() ##
Test of PUT '/service' or V2 PUT '/service'.
| arg list |
|--|
| `<response-code>  <service-name> <keepalive-timeout> <callbackurl>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |
| `<keepalive-timeout>` |  Timeout value |
| `<callbackurl>` |  Callback url |

## Function: api_get_services() ##
Test of GET '/service' or V2 GET '/v2/services' and optional check of the returned json payload.
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

## Function: api_get_service_ids() ##
Test of GET '/services' or V2 GET /'v2/services'. Only check of service ids.

| arg list |
|--|
| `<response-code> [<service-name>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Function: api_delete_services() ##
Test of DELETE '/services' or V2 DELETE '/v2/services/{serviceId}'

| arg list |
|--|
| `<response-code> [<service-name>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Function: api_put_services_keepalive() ##
Test of PUT '/services/keepalive' or V2 PUT '/v2/services/{service_id}/keepalive'

| arg list |
|--|
| <response-code> <service-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Function: api_put_configuration() ##
Test of PUT '/v2/configuration'

| arg list |
|--|
| <response-code> <config-file>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<config-file>` |  Path json config file |

## Function: api_get_configuration() ##
Test of GET '/v2/configuration'

| arg list |
|--|
| <response-code> [<config-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<config-file>` |  Path json config file to compare the retrieved config with |
| parameter | description |
| --------- | ----------- |
| `[<response-code>]*` | A space separated list of http response codes, may be empty to reset to 'no codes'.  |


# Description of functions in consul_cbs_function.sh #


## Function: consul_config_app ##
Function to load a json config from a file into consul for the Policy Agent

| arg list |
|--|
| `<json-config-file>` |

| parameter | description |
| --------- | ----------- |
| `<json-config-file>` | The path to the json file to be loaded to Consul/CBS |

## Function: prepare_consul_config ##
Function to prepare a Consul config based on the previously configured (and started simulators). Note that all simulator must be running and the test script has to configure if http or https shall be used for the components (this is done by the functions 'use_simulator_http', 'use_simulator_https', 'use_sdnc_http', 'use_sdnc_https', 'use_mr_http', 'use_mr_https')
| arg list |
|--|
| `<deviation-message-to-print>` |

| parameter | description |
| --------- | ----------- |
| `SDNC|NOSDNC` | Configure based on a1-controller (SNDC) or without a controller/adapter (NOSDNC) |
| `<output-file>` | The path to the json output file containing the prepared config. This file is used in 'consul_config_app'  |

## Function: start_consul_cbs ##
Start the Consul and CBS containers
| arg list |
|--|
| None |

# Description of functions in control_panel_api_function.sh #

## Function: use_control_panel_http ##
Set http as the protocol to use for all communication to the Control Panel
| arg list |
|--|
| None |

## Function: use_control_panel_https ##
Set https as the protocol to use for all communication to the Control Panel
| arg list |
|--|
| None |

## Function: start_control_panel ##
Start the Control Panel container
| arg list |
|--|
| None |

# Description of functions in controller_api_functions.sh #
The file contains a selection of the possible API tests towards the a1-controller

## Function: use_sdnc_http ##
Use http for all API calls towards the SDNC A1 Controller. This is the default. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

## Function: use_sdnc_http ##
Use https for all API calls towards the SDNC A1 Controller. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

## Function: start_sdnc ##
Start the SDNC A1 Controller container and its database container
| arg list |
|--|
| None |

## Function: check_sdnc_logs ##
Check the SDNC log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

## Function: controller_api_get_A1_policy_ids ##
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


## Function: controller_api_get_A1_policy_type ##
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

## Function: controller_api_delete_A1_policy ##
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

## Function: controller_api_put_A1_policy ##
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

## Function: controller_api_get_A1_policy_status ##
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


# Description of functions in cr_api_functions.sh #

## Function: use_cr_http ##
Use http for getting event from CR.  The admin API is not affected. This is the default.
| arg list |
|--|
| None |

## Function: use_cr_https ##
Use https for getting event from CR. The admin API is not affected.
Note: Not yet used as callback event is not fully implemented/deciced.
| arg list |
|--|
| None |

## Function: start_cr ##
Start the Callback Receiver container in docker or kube depending on start mode.
| arg list |
|--|
| None |

## Function: cr_equal ##
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

## Function: cr_api_check_all_sync_events() ##
Check the contents of all ric events received for a callback id.

| arg list |
|--|
| `<response-code> <id> [ EMPTY | ( <ric-id> )+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<id>` | Id of the callback destination  |
| `EMPTY` | Indicator for an empty list  |
| `<ric-id>` | Id of the ric  |

# Description of functions in ecs_api_functions.sh #

## Function: use_ecs_rest_http ##
Use http for all API calls to the ECS. This is the default protocol.
| arg list |
|--|
| None |

## Function: use_ecs_rest_https ##
Use https for all API calls to the ECS.
| arg list |
|--|
| None |

## Function: use_ecs_dmaap_http ##
Send and recieve all API calls to the ECS over Dmaap via the MR using http.
| arg list |
|--|
| None |

## Function: use_ecs_dmaap_https ##
Send and recieve all API calls to the ECS over Dmaap via the MR using https.
| arg list |
|--|
| None |

## Function: start_ecs ##
Start the ECS container in docker or kube depending on running mode.
| arg list |
|--|
| None |

## Function: stop_ecs ##
Stop the ECS container.
| arg list |
|--|
| None |

## Function: start_stopped_ecs ##
Start a previously stopped ecs.
| arg list |
|--|
| None |

## Function: set_ecs_debug ##
Configure the ECS log on debug level. The ECS must be running.
| arg list |
|--|
| None |

## Function: set_ecs_trace ##
Configure the ECS log on trace level. The ECS must be running.
| arg list |
|--|
| None |

## Function: check_ecs_logs ##
Check the ECS log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

## Function: ecs_equal ##
Tests if a variable value in the ECS is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'a1-interface' repo for more details.

| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in ecs  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: ecs_api_a1_get_job_ids() ##
Test of GET '/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs' and optional check of the array of returned job ids.
To test the response code only, provide the response code parameter as well as a type id and an owner id.
To also test the response payload add the 'EMPTY' for an expected empty array or repeat the last parameter for each expected job id.

| arg list |
|--|
| `<response-code> <type-id>  <owner-id>|NOOWNER [ EMPTY | <job-id>+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<owner-id>` | Id of the job owner  |
| `NOOWNER` | No owner is given  |
| `<job-id>` | Id of the expected job  |
| `EMPTY` | The expected list of job id shall be empty  |

## Function: ecs_api_a1_get_type() ##
Test of GET '/A1-EI​/v1​/eitypes​/{eiTypeId}' and optional check of the returned schema.
To test the response code only, provide the response code parameter as well as the type-id.
To also test the response payload add a path to the expected schema file.

| arg list |
|--|
| `<response-code> <type-id> [<schema-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<schema-file>` | Path to a schema file to compare with the returned schema  |

## Function: ecs_api_a1_get_type_ids() ##
Test of GET '/A1-EI​/v1​/eitypes' and optional check of returned list of type ids.
To test the response code only, provide the response only.
To also test the response payload add the list of expected type ids (or EMPTY if the list is expected to be empty).

| arg list |
|--|
| `<response-code> [ (EMPTY | [<type-id>]+) ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `EMPTY` | The expected list of type ids shall be empty  |
| `<type-id>` | Id of the EI type  |

## Function: ecs_api_a1_get_job_status() ##
Test of GET '/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}​/status' and optional check of the returned status.
To test the response code only, provide the response code, type id and job id.
To also test the response payload add the expected status.

| arg list |
|--|
| `<response-code> <type-id> <job-id> [<status>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<job-id>` | Id of the job  |
| `<status>` | Expected status  |

## Function: ecs_api_a1_get_job() ##
Test of GET '/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}' and optional check of the returned job.
To test the response code only, provide the response code, type id and job id.
To also test the response payload add the remaining parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id> [<target-url> <owner-id> <template-job-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<job-id>` | Id of the job  |
| `<target-url>` | Expected target url for the job  |
| `<owner-id>` | Expected owner for the job  |
| `<template-job-file>` | Path to a job template for job parameters of the job  |

## Function: ecs_api_a1_delete_job() ##
Test of DELETE '/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}'.
To test, provide all the specified parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id> |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<job-id>` | Id of the job  |

## Function: ecs_api_a1_put_job() ##
Test of PUT '/A1-EI​/v1​/eitypes​/{eiTypeId}​/eijobs​/{eiJobId}'.
To test, provide all the specified parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id> <target-url> <owner-id> <template-job-file>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<job-id>` | Id of the job  |
| `<target-url>` | Target url for the job  |
| `<owner-id>` | Owner of the job  |
| `<template-job-file>` | Path to a job template for job parameters of the job  |

## Function: ecs_api_edp_get_type_ids() ##
Test of GET '/ei-producer/v1/eitypes' and an optional check of the returned list of type ids.
To test the response code only, provide the response code.
To also test the response payload add list of expected type ids (or EMPTY if the list is expected to be empty).

| arg list |
|--|
| `<response-code> [ EMPTY | <type-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ecs_api_edp_get_producer_status() ##
Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}/status' and optional check of the returned status.
To test the response code only, provide the response code and producer id.
To also test the response payload add the expected status.

| arg list |
|--|
| `<response-code> <producer-id> [<status>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<status>` | The expected status string  |

## Function: ecs_api_edp_get_producer_ids() ##
Test of GET '/ei-producer/v1/eiproducers' and optional check of the returned producer ids.
To test the response code only, provide the response.
To also test the response payload add the list of expected producer-ids (or EMPTY if the list of ids is expected to be empty).

| arg list |
|--|
| `<response-code> [ EMPTY | <producer-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ecs_api_edp_get_type() ##
Test of GET '/ei-producer/v1/eitypes/{eiTypeId}' and optional check of the returned type.
To test the response code only, provide the response and the type-id.
To also test the response payload add a path to a job schema file and a list expected producer-id (or EMPTY if the list of ids is expected to be empty).

| arg list |
|--|
| `<response-code> <type-id> [<job-schema-file> (EMPTY | [<producer-id>]+)]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<job-schema-file>` | Path to a job schema file  |
| `<producer-id>` | Id of the producer  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ecs_api_edp_get_producer() ##
Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}' and optional check of the returned producer.
To test the response code only, provide the response and the producer-id.
To also test the response payload add the remaining parameters defining thee producer.

| arg list |
|--|
| `<response-code> <producer-id> [<create-callback> <delete-callback> <supervision-callback> (EMPTY | [<type-id> <schema-file>]+) ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<create-callback>` | Callback for create job  |
| `<delete-callback>` | Callback for delete job  |
| `<supervision-callback>` | Callback for producer supervision  |
| `<type-id>` | Id of the EI type  |
| `<schema-file>` | Path to a schema file  |
| `EMPTY` | The expected list of type schema pairs shall be empty  |

## Function: ecs_api_edp_delete_producer() ##
Test of DELETE '/ei-producer/v1/eiproducers/{eiProducerId}'.
To test, provide all parameters.

| arg list |
|--|
| `<response-code> <producer-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |

## Function: ecs_api_edp_put_producer() ##
Test of PUT '/ei-producer/v1/eiproducers/{eiProducerId}'.
To test, provide all parameters. The list of type/schema pair may be empty.

| arg list |
|--|
| `<response-code> <producer-id> <job-callback> <supervision-callback> (EMPTY | [<type-id> <schema-file>]+)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-callback>` | Callback for create/delete job  |
| `<supervision-callback>` | Callback for producer supervision  |
| `<type-id>` | Id of the EI type  |
| `<schema-file>` | Path to a schema file  |
| `EMPTY` | The list of type/schema pairs is empty  |

## Function: ecs_api_edp_get_producer_jobs() ##
Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}/eijobs' and optional check of the returned producer job.
To test the response code only, provide the response and the producer-id.
To also test the response payload add the remaining parameters.

| arg list |
|--|
| `<response-code> <producer-id> (EMPTY | [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-id>` | Id of the job  |
| `<type-id>` | Id of the EI type  |
| `<target-url>` | Target url for data delivery  |
| `<job-owner>` | Id of the job owner  |
| `<template-job-file>` | Path to a job template file  |
| `EMPTY` | The list of job/type/target/job-file tuples is empty  |

## Function: ecs_api_service_status() ##
Test of GET '/status'.

| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

## Function: ecs_api_admin_reset() ##
Test of GET '/status'.

| arg list |
|--|
| `<response-code> [ <type> ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type>` | Type id, if the interface supports type in url |

# Description of functions in gateway_api_functions.sh #


## Function: use_gateway_http ##
Use http for all calls to the gateway. This is set by default.
| arg list |
|--|
| None |

## Function: use_gateway_https ##
Use https for all calls to the gateway.
| arg list |
|--|
| None |

## Function: set_gateway_debug ##
Set debug level logging in the gateway
| arg list |
|--|
| None |

## Function: set_gateway_trace ##
Set debug level logging in the trace
| arg list |
|--|
| None |

## Function: start_gateway ##
Start the the gateway container in docker or kube depending on start mode
| arg list |
|--|
| None |

## Function: gateway_pms_get_status ##
Sample test of pms api (status)
| arg list |
|--|
| `<response-code> ` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

## Function: gateway_ecs_get_types ##
Sample test of ecs api (get ei type)
Only response code tested - not payload
| arg list |
|--|
| `<response-code> ` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

# Description of functions in http_proxy_api_functions.sh #

## Function: use_http_proxy_http ##
Use http for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.
| arg list |
|--|
| None |

## Function: use_http_proxy_https ##
Use https for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.
| arg list |
|--|
| None |

## Function: start_http_proxy ##
Start the http proxy container in docker or kube depending on running mode.
| arg list |
|--|
| None |

# Description of functions in kube_proxy_api_functions.sh #

## Function: use_kube_proxy_http ##
Use http for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.
| arg list |
|--|
| None |

## Function: use_kube_proxy_https ##
Use https for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.
| arg list |
|--|
| None |

## Function: start_kube_proxy ##
Start the kube proxy container in kube. This proxy enabled the test env to access all services and pods in a kube cluster.
No proxy is started if the function is called in docker mode.
| arg list |
|--|
| None |

# Description of functions in mr_api_functions.sh #

## Function: use_mr_http ##
Use http for all Dmaap calls to the MR. This is the default. The admin API is not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

## Function: use_mr_https ##
Use https for all Dmaap call to the MR. The admin API is not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

## Function: start_mr ##
Start the Message Router stub interface container in docker or kube depending on start mode
| arg list |
|--|
| None |


## Function: mr_equal ##
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

## Function: mr_greater ##
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

## Function: mr_read ##
Reads the value of a variable in the Message Router (MR) simulator. The value is intended to be passed to a env variable in the test script.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the MR  |

## Function: mr_print ##
Prints the value of a variable in the Message Router (MR) simulator.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the MR  |


# Description of functions in prodstub_api_functions.sh #

## Function: use_prod_stub_http ##
Use http for the API.  The admin API is not affected. This is the default protocol.
| arg list |
|--|
| None |

## Function: use_prod_stub_https ##
Use https for the API. The admin API is not affected.
| arg list |
|--|
| None |

## Function: start_prod_stub ##
Start the Producer stub container in docker or kube depending on start mode
| arg list |
|--|
| None |

## Function: prodstub_arm_producer() ##
Preconfigure the prodstub with a producer. The producer supervision response code is optional, if not given the response code will be set to 200.

| arg list |
|--|
| `<response-code> <producer-id> [<forced_response_code>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<forced_response_code>` | Forced response code for the producer callback url |

## Function: prodstub_arm_job_create() ##
Preconfigure the prodstub with a job or update an existing job. Optional create/update job response code, if not given the response code will be set to 200/201 depending on if the job has been previously created or not.

| arg list |
|--|
| `<response-code> <job-id> [<forced_response_code>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<job-id>` | Id of the job  |
| `<forced_response_code>` | Forced response code for the create callback url |

## Function: prodstub_arm_job_delete() ##
Preconfigure the prodstub with a job. Optional delete job response code, if not given the response code will be set to 204/404 depending on if the job exists or not.

| arg list |
|--|
| `<response-code> <job-id> [<forced_response_code>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<job-id>` | Id of the job  |
| `<forced_response_code>` | Forced response code for the delete callback url |

## Function: prodstub_arm_type() ##
Preconfigure the prodstub with a type for a producer. Can be called multiple times to add more types.

| arg list |
|--|
| `<response-code> <producer-id> <type-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<type-id>` | Id of the type  |

## Function: prodstub_disarm_type() ##
Remove a type for the producer in the rodstub. Can be called multiple times to remove more types.

| arg list |
|--|
| `<response-code> <producer-id> <type-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<type-id>` | Id of the type  |

## Function: prodstub_check_jobdata() ##
Check a job in the prodstub towards the list of provided parameters.

| arg list |
|--|
| `<response-code> <producer-id> <job-id> <type-id> <target-url> <job-owner> <template-job-file>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-id>` | Id of the job  |
| `<type-id>` | Id of the type  |
| `<target-url>` | Target url for data delivery  |
| `<job-owner>` | Id of the job owner  |
| `<template-job-file>` | Path to a job template file  |

## Function: prodstub_delete_jobdata() ##
Delete the job parameters, job data, for a job.

| arg list |
|--|
| `<response-code> <producer-id> <job-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-id>` | Id of the job  |


## Function: prodstub_equal ##
Tests if a variable value in the prodstub is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.

| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the prostub  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |


# Description of functions in rapp_catalogue_api_function.sh #

## Function: use_rapp_catalogue_http ##
Use http for the API. This is the default protocol.
| arg list |
|--|
| None |

## Function: use_rapp_catalogue_https ##
Use https for the API.
| arg list |
|--|
| None |

## Function: start_rapp_catalogue ##
Start the rapp catalogue container in docker or kube depending on start mode
| arg list |
|--|
| None |

## Function: rc_equal ##
Tests if a variable value in the RAPP Catalogue is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'cr' dir for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the RC  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: rapp_cat_api_get_services() ##
Check all registered services.

| arg list |
|--|
| `<response-code> [(<service-id> <version> <display-name> <description>)+ | EMPTY ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<version>` | Version of the service  |
| `<display-name>` | Dislay name of the service  |
| `<description>` | Description of the service  |
| `EMPTY` | Indicator for an empty list  |

## Function: rapp_cat_api_put_service() ##
Register a services.

| arg list |
|--|
| `<response-code> <service-id> <version> <display-name> <description>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<version>` | Version of the service  |
| `<display-name>` | Dislay name of the service  |
| `<description>` | Description of the service  |

## Function: rapp_cat_api_get_service() ##
Check a registered service.

| arg list |
|--|
| `<response-code> <service-id> <version> <display-name> <description>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<version>` | Version of the service  |
| `<display-name>` | Dislay name of the service  |
| `<description>` | Description of the service  |

## Function: rapp_cat_api_delete_service() ##
Check a registered service.

| arg list |
|--|
| `<response-code> <service-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |


# Description of functions in ricsimulator_api_functions.sh #
The functions below only use the admin interface of the simulator, no usage of the A1 interface.


## Function: use_simulator_http ##
Use http for all API calls (A1) toward the simulator. This is the default. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

## Function: use_simulator_https ##
Use https for all API calls (A1) toward the simulator. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for Consul.
| arg list |
|--|
| None |

## Function: start_ric_simulators ##
Start a group of simulator where a group may contain 1 more simulators. Started in docker or kube depending on start mode
| arg list |
|--|
| `ricsim_g1|ricsim_g2|ricsim_g3 <count> <interface-id>` |

| parameter | description |
| --------- | ----------- |
| `ricsim_g1|ricsim_g2|ricsim_g3` | Base name of the simulator. Each instance will have an postfix instance id added, starting on '1'. For examplle 'ricsim_g1_1', 'ricsim_g1_2' etc  |
|`<count>`| And integer, 1 or greater. Specifies the number of simulators to start|
|`<interface-id>`| Shall be the interface id of the simulator. See the repo 'a1-interface' for the available ids. |

## Function: get_kube_sim_host ##
Translate ric name to kube host name.
| arg list |
|--|
| `<ric-name>` |

| parameter | description |
| --------- | ----------- |
| `<ric-name>` | The name of the ric to translate into a host name (ip) |

## Function: generate_policy_uuid ##
Geneate a UUID prefix to use along with the policy instance number when creating/deleting policies. Sets the env var UUID.
UUID is then automatically added to the policy id in GET/PUT/DELETE.
| arg list |
|--|
| None |

## Function: sim_equal ##
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

## Function: sim_print ##
Prints the value of a variable in the RIC simulator.
See the 'a1-interface' repo for more details.

| arg list |
|--|
| `<variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the RIC simulator  |


## Function: sim_contains_str ##
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

## Function: sim_put_policy_type ##
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

## Function: sim_delete_policy_type ##
Deletes a policy type from the simulator

| arg list |
|--|
| `<response-code> <ric-id> <policy_type_id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `<policy-type-id>` |  Id of the policy type |

## Function: sim_post_delete_instances ##
Deletes all instances (and status), for one ric

| arg list |
|--|
| `<response-code> <ric-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |


## Function: sim_post_delete_all ##
Deletes all types, instances (and status), for one ric

| arg list |
|--|
| `<response-code> <ric-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |

## Function: sim_post_forcedresponse ##
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

## Function: sim_post_forcedelay ##
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