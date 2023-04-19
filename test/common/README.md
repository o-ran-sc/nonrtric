# Introduction #

This dir contains most scripts needed for the auto-test environment. There are scripts with functions to adapt to the apis of the components of the Non-RT RIC; Policy Management Service, Information Coordinator Service , A1 simulator as well as other components and simulators. The test environment supports both test with docker and kubernetes.
Some of the scripts can also be used for other kinds of tests, for example basic tests.

## Overview for common test scripts and files ##

`a1pms_api_functions.sh` \
Contains functions for adapting towards the Policy Management Service (A1PMS) API.

`api_curl.sh` \
A common curl based function for the a1pms and ics apis. Also partly used for the Callback receiver and RAPP Catalogue apis.

`authsidecar_api_functions.sh` \
Image handling functions for the auth_token_fetch image.

`chartmus_api_functions.sh` \
Contains functions for managing a Chartmuseum instance.

`clean-docker.sh` \
Cleans all containers started by the test environment in docker.

`clean-kube.sh` \
Cleans all services, deployments, pods, replica set etc started by the test environment in kubernetes.

`clean-kube_ns.sh` \
Cleans all namespaces created by the test environment in kubernetes.

`compare_json.py` \
A python script to compare two json objects for equality. Note that the comparison always sort json-arrays before comparing (that is, it does not care about the order of items within the array). In addition, the target json object may specify individual parameter values where equality is 'don't care'.

`count_json_elements.py` \
A python script calculate the length of json array or size of a json dictionary'.

`count_json_elements.py` \
A python script returning the number of items in a json array.

`cp_api_function.sh` \
Contains functions for managing the Control Panel.

`cr_api_functions.sh` \
Contains functions for adapting towards the Callback receiver for checking received callback events.

`create_policies_process.py` \
A python script to create a batch of policies. The script is intended to run in a number of processes to create policies in parallel.

`create_rics_json.py` \
A python script to create a json file from a formatted string of ric info. Helper for the test environment.

`delete_policies_process.py` \
A python script to delete a batch of policies. The script is intended to run in a number of processes to delete policies in parallel.

`dmaapadp_api_function.sh`
Contains functions for managing the Dmaap Adapter.

`dmaapmed_api_function.sh`
Contains functions for managing the Dmaap Mediator Service.

`dmaapmr_api_function.sh`
All functions are implemented in `mr_api_functions.sh`.

`do_curl_function.sh`
A script for executing a curl call with a specific url and optional payload. It also compare the response with an expected result in terms of response code and optional returned payload. Intended to be used by test script (for example basic test scripts of other components)

`extract_sdnc_reply.py` \
A python script to extract the information from an sdnc (A1 Controller) reply json. Helper for the test environment.

`format_endpoint_stats.sh` \
This script formats API statistics into a test report - data is collected only if the flag `--endpoint-stats` is given to the test script .

`genstat.sh` \
This script collects container statistics to a file. Works both in docker and kubernetes (only for docker runtime).

`helmmanager_api_functions.sh` \
Contains functions for managing and testing of the Helm Manager.

`http_proxy_api_functions.sh` \
Contains functions for managing the Http Proxy.

`ics_api_functions.sh` \
Contains functions for adapting towards the Information Coordinator Service API.

`istio_api_functions.sh` \
Contains functions for istio configurations.

`kafkapc_api_functions.sh` \
Contains functions for managing the kafka producer/consumer. Kafka is started by the dmaap message router component.

`keycloak_api_functions.sh` \
Contains functions for keycloak configuration.

`kube_proxy_api_functions.sh` \
Contains functions for managing the Kube Proxy - to gain access to all services pod inside a kube cluster or all containers in a private docker network.

`localhelm_api_functions.sh` \
Contains functions for helm access on localhost.

`mr_api_functions.sh` \
Contains functions for managing the MR Stub and the Dmaap Message Router

`ngw_api_functions.sh` \
Contains functions for managing the Non-RT RIC Gateway

`prodstub_api_functions.sh` \
Contains functions for adapting towards the Producer stub interface - simulates a producer.

`pvccleaner_api_functions.sh` \
Contains functions for managing the PVC Cleaner (used for reset mounted volumes in kubernetes).

`rc_api_functions.sh` \
Contains functions for adapting towards the RAPP Catalogue.

`ricsim_api_functions.sh` \
Contains functions for adapting towards the RIC (A1) simulator admin API.

`ricmediatorsim_api_functions.sh` \
Contains functions for adapting towards the ricmediator simulator (A1) API.

`sdnc_api_functions.sh` \
Contains functions for adaping towards the SDNC (used as an A1 controller).

`test_env*.sh` \
Common env variables for test in the auto-test dir. All configuration of port numbers, image names and version etc shall be made in this file.
Used by the auto test scripts/suites but could be used for other test script as well. The test cases shall be started with the file for the intended target using command line argument '--env-file'.

`testcase_common.sh` \
Common functions for auto test cases in the auto-test dir. This script is the foundation of test auto environment which sets up images and environment variables needed by this script as well as the script adapting to the APIs.
The included functions are described in detail further below.

`testengine_config.sh` \
Configuration file to setup the applications (components and simulators) the test environment handles.

`testsuite_common.sh` \
Common functions for running two or more auto test scripts as a suite.

## Integration of a new applicaton ##

Integration a new application to the test environment involves the following steps.

* Choose a short name for the application. Should be a uppercase name. For example, the NonRTRIC Gateway has NGW as short name.
This short name shall be added to the testengine_config.sh. See that file for detailed instructions.

Depending if the image is a locally built simulator image or an official image, the following env vara may need to be updated with the app short name: `PROJECT_IMAGES_APP_NAMES  ORAN_IMAGES_APP_NAMES  ORAN_IMAGES_APP_NAMES`.

* Create a file in this directory using the pattern `<application-name>_api_functions.sh`.
This file must implement the following functions used by the test engine. Note that functions must include the application short name in the function name. If the application does not run in kubernetes, then the functions with kube prefix does not need to be implemented.

| Function |
|--|
| __<app-short_name>_imagesetup |
| __<app-short_name>_imagepull |
| __<app-short_name>_imagebuild |
| __<app-short_name>_image_data |
| __<app-short_name>_kube_scale_zero |
| __<app-short_name>_kube_scale_zero_and_wait |
| __<app-short_name>_kube_delete_all |
| __<app-short_name>_store_docker_logs |
| __<app-short_name>_initial_setup |
| __<app-short_name>_statistics_setup |
| __<app-short_name>_test_requirements |

In addition, all other functions used for testing of the application shall also be added to the file. For example functions to start the application, setting interface parameters as well as functions to send rest call towards the api of the application and validating the result.

* Add the application variables to api_curl.sh. This file contains a generic function to make rest calls to an api. It also supports switching between direct rest calls or rest calls via message router.

* Create a directory beneath in the simulator-group dir. This new directory shall contain docker-compose files, config files (with or without variable substitutions) and kubernetes resource files.

All docker-compose files and all kubernetes resource files need to defined special labels. These labels are used by the test engine to identify containers and resources started and used by the test engine.

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
| `remote\|remote-remove `
` docker\|kube `
` --env-file <environment-filename> `
` [release] `
` [auto-clean]`
` [--stop-at-error]`
` [--ricsim-prefix <prefix> ]`
` [--use-local-image <app-nam>+]`
` [--use-snapshot-image <app-nam>+]`
` [--use-staging-image <app-nam>+]`
` [--use-release-image <app-nam>+]`
` [--use-external-image <app-nam>+]`
` [--image-repo <repo-address>]`
` [--repo-policy local\|remote]`
` [--cluster-timeout <timeout-in-seconds>]`
` [--print-stats]`
` [--override <override-environment-filename>]`
` [--pre-clean]`
` [--gen-stats]`
` [--delete-namespaces]`
` [--delete-containers]`
` [--endpoint-stats]`
` [--kubeconfig <config-file>]`
` [--host-path-dir <local-host-dir>]`
` [--kubecontext <context-name>]`
` [--docker-host <docker-host-url>]`
` [--docker-proxy <host-or-ip>] `
` ["--target-platform <platform> ]` |

| parameter | description |
|-|-|
| `remote` | Use images from remote repositories. Can be overridden for individual images using the '--use_xxx' flags |
| `remote-remove` | Same as 'remote' but will also try to pull fresh images from remote repositories |
| `docker` | Use docker environment for test |
| `kube` | Use kubernetes environment for test. Requires access to a local or remote kubernetes cluster (or or more nodes). For example docker desktop, minikube, external kubernetes cluster, google cloud etc |
| `--env-file` | The script will use the supplied file to read environment variables from |
| `release` | If this flag is given the script will use release version of the images |
| `auto-clean` | If the function 'auto_clean_containers' is present in the end of the test script then all containers will be stopped and removed. If 'auto-clean' is not given then the function has no effect |
| `--stop-at-error` | The script will stop when the first failed test or configuration |
| `--ricsim-prefix <prefix>` | The a1 simulator will use the supplied string as container prefix instead of 'ricsim'. Note that the testscript has to read and use the env var `$RIC_SIM_PREFIX` instead of a hardcoded name of the ric(s). |
| `--use-local-image` | The script will use local images for the supplied apps, space separated list of app short names |
| `--use-snapshot-image` | The script will use images from the nexus snapshot repo for the supplied apps, space separated list of app short names |
| `--use-staging-image` | The script will use images from the nexus staging repo for the supplied apps, space separated list of app short names |
| `--use-release-image` | The script will use images from the nexus release repo for the supplied apps, space separated list of app short names |
| `--use-external-image` | The script will use images from an external repo for the supplied apps, space separated list of app short names |
| `--image-repo` |  Url to optional image repo. Only locally built images will be re-tagged and pushed to this repo |
| `--repo-policy` |  Policy controlling which images to re-tag and push to image repo in param --image-repo. Can be set to 'local' (push only locally built images) or 'remote' (push locally built images and images from nexus repo). Default is 'local' |
| `--cluster-timeout` |  Optional timeout for cluster where it takes time to obtain external ip/host-name. Timeout in seconds |
| `--print-stats` |  Prints the number of tests, failed tests, failed configuration and deviations after each individual test or config |
| `--override <file>` |  Override setting from the file supplied by --env-file |
| `--pre-clean` |  Clean kube resources when running docker and vice versa |
| `--gen-stats`  | Collect container/pod runtime statistics |
| `--delete-namespaces`  | Delete kubernetes namespaces before starting tests - but only those created by the test scripts. Kube mode only. Ignored if running with pre-started apps. |
| `--delete-containers`  | Delete docker containers before starting tests - but only those created by the test scripts. Docker mode only. |
| `--endpoint-stats`  | Collect http endpoint statistics |
| `--kubeconfig`  | Kubernetes config file for kubectl, when running with non local kubernetes |
| `--host-path-dir`  | Path for storing persistent data in kubernetes. Must be available on all nodes. Default is `/tmp` |
| `--kubecontext`  | Kubernetes context name for kubectl, when running with non local kubernetes  |
| `--docker-host`  | Url to docker host, host and port. For non local docker |
| `--docker-proxy`  | Host or IP of the docker instance, for non local docker |
| `--target-platform` | Build and pull images for this target platform |


| `help` | Print this info along with the test script description and the list of app short names supported |

## Function: setup_testenvironment ##

Main function to setup the test environment before any tests are started.
Must be called right after sourcing all component scripts.
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
| `<timer-message-to-print>` |
| None - but any args will be printed (It is good practice to use same args for this function as for the `print_timer`) |

## Function: print_timer ##

Print the value of the timer (in seconds) previously started by 'start_timer'. (Note that timer is still running after this function). The result of the timer as well as the arg to 'start_timer' will also be printed in the test report.
| arg list |
|--|
| None |

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
| `<sleep-time-in-sec>` | Number of seconds to sleep |
| `<any-text-in-quotes-to-be-printed>` | Optional. The text will be printed, if present |

## Function: store_logs ##

Take a snap-shot of all logs for all running containers/pods and stores them in `./logs/<ATC-id>`. All logs will get the specified prefix in the file name. In general, one of the last steps in an auto-test script shall be to call this function. If logs shall be taken several times during a test script, different prefixes shall be used each time.
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

# Description of functions in a1pms_api_functions.sh #

## General ##

Both A1PMS version 1 and 2 are supported. The version is controlled by the env variable `$A1PMS_VERSION` set in the test env file.
For api function in version 2, an url prefix is added if configured.

## Function: use_a1pms_rest_http ##

Use http for all API calls to the A1PMS. This is the default.
| arg list |
|--|
| None |

## Function: use_a1pms_rest_https ##

Use https for all API calls to the A1PMS.
| arg list |
|--|
| None |

## Function: use_a1pms_dmaap_http ##

Send and receive all API calls to the A1PMS over Dmaap via the MR over http.
| arg list |
|--|
| None |

## Function: use_a1pms_dmaap_https ##

Send and receive all API calls to the A1PMS over Dmaap via the MR over https.
| arg list |
|--|
| None |

## Function: start_a1pms ##

Start the A1PMS container or corresponding kube resources depending on docker/kube mode.
| arg list |
|--|
| `<logfile-prefix>` |
| (docker) `PROXY\|NOPROXY <config-file>` |
| (kube) `PROXY\|NOPROXY <config-file> [ <data-file> ]` |

| parameter | description |
| --------- | ----------- |
| `PROXY` | Configure with http proxy, if proxy is started  |
| `NOPROXY` | Configure without http proxy  |
| `<config-file>`| Path to application.yaml  |
| `<data-file>` | Optional path to application_configuration.json  |

## Function: stop_a1pms ##

Stop the a1pms container (docker) or scale it to zero (kubernetes).
| arg list |
|--|
|  None |

## Function: start_stopped_a1pms ##

Start a previously stopped a1pms container (docker) or scale it to 1 (kubernetes).
| arg list |
|--|
|  None |

## Function: prepare_a1pms_config ##

Function to prepare an a1pms config based on the previously configured (and started simulators). Note that all simulator must be running and the test script has to configure if http or https shall be used for the components (this is done by the functions 'use_simulator_http', 'use_simulator_https', 'use_sdnc_http', 'use_sdnc_https', 'use_mr_http', 'use_mr_https')
| arg list |
|--|
| `SDNC|NOSDNC <output-file>` |

| parameter | description |
| --------- | ----------- |
| `SDNC` | Configure with controller |
| `NOSDNC` | Configure without controller |
| `<output-file>` | The path to the json output file containing the prepared config. This file is used in 'a1pms_load_config'  |

## Function: a1pms_load_config ##

Load the config into a config map (kubernetes only).
| arg list |
|--|
|  `<data-file>` |

| parameter | description |
| --------- | ----------- |
|  `<data-file>` | Path to application_configuration.json  |

## Function: set_a1pms_debug ##

Configure the A1PMS log on debug level. The A1PMS must be running.
| arg list |
|--|
| None |

## Function: set_a1pms_trace ##

Configure the A1PMS log on trace level. The A1PMS must be running.
| arg list |
|--|
| None |

## Function: use_a1pms_retries ##

Configure the A1PMS to make up-to 5 retries if an API call returns any of the specified http return codes.
| arg list |
|--|
| `[<response-code>]*` |

## Function: check_a1pms_logs ##

Check the A1PMS log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

## Function: a1pms_equal ##

Tests if the array length of a json array in the A1PMS simulator is equal to a target value.
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

## Function: a1pms_api_get_policies ##

Test of GET '/policies' or V2 GET '/v2/policy-instances' and optional check of the array of returned policies.
To test the response code only, provide the response code parameter as well as the following three parameters.
To also test the response payload add the 'NOID' for an expected empty array or repeat the last five/seven parameters for each expected policy.

| arg list |
|--|
| `<response-code> <ric-id>\|NORIC <service-id>\|NOSERVICE <policy-type-id>\|NOTYPE [ NOID \| [<policy-id> <ric-id> <service-id> EMPTY\|<policy-type-id> <template-file>]*]` |

| arg list V2 |
|--|
| `<response-code> <ric-id>\|NORIC <service-id>\|NOSERVICE <policy-type-id>\|NOTYPE [ NOID \| [<policy-id> <ric-id> <service-id> EMPTY\|<policy-type-id> <transient> <notification-url> <template-file>]*]` |

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

## Function: a1pms_api_get_policy ##

Test of GET '/policy' or V2 GET '/v2/policies/{policy_id}' and optional check of the returned json payload.
To test the the response code only, provide the expected response code and policy id.
To test the contents of the returned json payload, add a path to the template file used when creating the policy.

| arg list |
|--|
| `<response-code>  <policy-id> [<template-file>]` |

| arg list V2|
|--|
| `<response-code> <policy-id> [ <template-file> <service-name> <ric-id> <policytype-id>\|NOTYPE <transient> <notification-url>\|NOURL ]` |

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

## Function: a1pms_api_put_policy ##

Test of PUT '/policy' or V2 PUT '/policies'.
If more than one policy shall be created, add a count value to indicate the number of policies to create. Note that if more than one policy shall be created the provided policy-id must be numerical (will be used as the starting id).

| arg list |
|--|
| `<response-code> <service-name> <ric-id> <policytype-id> <policy-id> <transient> <template-file> [<count>]` |

| arg list V2 |
|--|
| `<response-code> <service-name> <ric-id> <policytype-id>\|NOTYPE <policy-id> <transient>\|NOTRANSIENT <notification-url>\|NOURL <template-file> [<count>]` |

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

## Function: a1pms_api_put_policy_batch ##

This tests the same as function 'a1pms_api_put_policy' except that all put requests are sent to dmaap in one go and then the responses are polled one by one.
If the a1pms api is not configured to use dmaap (see 'use_a1pms_dmaap', 'use_a1pms_rest_http' and 'use_a1pms_rest_https'), an error message is printed.
For arg list and parameters, see 'a1pms_api_put_policy'.

## Function: a1pms_api_put_policy_parallel ##

This tests the same as function 'a1pms_api_put_policy' except that the policy created is spread out over a number of processes and it only uses the a1pms rest API. The total number of policies created is determined by the product of the parameters 'number-of-rics' and 'count'. The parameter 'number-of-threads' should not be evenly divisible by the product of the parameters 'number-of-rics' and 'count' - this is to ensure that one process does not handle the creation of all the policies in one ric.

| arg list |
|--|
| `<response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <template-file> <count-per-ric> <number-of-threads>`

| arg list |
|--|
| `<response-code> <service-name> <ric-id-base> <number-of-rics> <policytype-id> <policy-start-id> <transient> <notification-url>\|NOURL <template-file> <count-per-ric> <number-of-threads>`

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

## Function: a1pms_api_delete_policy ##

This tests the DELETE '/policy' or V2 DELETE '/v2/policies/{policy_id}'. Removes the indicated policy or a 'count' number of policies starting with 'policy-id' as the first id.

| arg list |
|--|
| `<response-code> <policy-id> [<count>]`

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-id>` |  Id of the policy |
| `<count>` |  An optional count of policies to delete. The 'policy-id' will be the first id to be deleted. |

## Function: a1pms_api_delete_policy_batch ##

This tests the same as function 'a1pms_api_delete_policy' except that all delete requests are sent to dmaap in one go and then the responses are polled one by one.
If the a1pms api is not configured to used dmaap (see 'use_a1pms_dmaap', 'use_a1pms_rest_http' and 'use_a1pms_rest_https'), an error message is printed.
For arg list and parameters, see 'a1pms_api_delete_policy'.

## Function: a1pms_api_delete_policy_parallel ##

This tests the same as function 'a1pms_api_delete_policy' except that the policy deleted is spread out over a number of processes and it only uses the a1pms rest API. The total number of policies deleted is determined by the product of the parameters 'number-of-rics' and 'count'. The parameter 'number-of-threads' shall be selected to be not evenly divisible by the product of the parameters 'number-of-rics' and 'count' - this is to ensure that one process does not handle the deletion of all the policies in one ric.

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

## Function: a1pms_api_get_policy_ids ##

Test of GET '/policy_ids' or V2 GET '/v2/policies'.
To test response code only, provide the response code parameter as well as the following three parameters.
To also test the response payload add the 'NOID' for an expected empty array or repeat the 'policy-instance-id' for each expected policy id.

| arg list |
|--|
| `<response-code> <ric-id>\|NORIC <service-id>\|NOSERVICE <type-id>\|NOTYPE ([<policy-instance-id]*\|NOID)` |

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

## Function: a1pms_api_get_policy_schema ##

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

## Function: a1pms_api_get_policy_schema ##

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

## Function: a1pms_api_get_policy_schemas ##

Test of GET '/policy_schemas' and optional check of the returned json schemas.
To test the response code only, provide the expected response code and ric id (or NORIC if no ric is given).
To test the contents of the returned json schema, add a path to a schema file to compare with (or NOFILE to represent an empty '{}' type)

| arg list |
|--|
| `<response-code>  <ric-id>\|NORIC [<schema-file>\|NOFILE]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `NORIC` |  No ric id given |
| `<schema-file>` |  Path to the schema file for the policy type |
| `NOFILE` |  Indicate the template for an empty type |

## Function: a1pms_api_get_policy_status ##

Test of GET '/policy_status' or V2 GET '/policies/{policy_id}/status'.

| arg list |
|--|
| `<response-code> <policy-id> (STD\|STD2 <enforce-status>\|EMPTY [<reason>\|EMPTY])\|(OSC <instance-status> <has-been-deleted>)` |

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

## Function: a1pms_api_get_policy_types ##

Test of GET '/policy_types' or  V2 GET '/v2/policy-types' and optional check of the returned ids.
To test the response code only, provide the expected response code and ric id (or NORIC if no ric is given).
To test the contents of the returned json payload, add the list of expected policy type id (or 'EMPTY' for the '{}' type)

| arg list |
|--|
| `<response-code> [<ric-id>\|NORIC [<policy-type-id>\|EMPTY [<policy-type-id>]*]]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `NORIC` |  No ric id given |
| `<policy-type-id>` |  Id of the policy type |
| `EMPTY` |  Indicate the empty type |

## Function: a1pms_api_get_status ##

Test of GET /status or V2 GET /status

| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

## Function: a1pms_api_get_ric ##

Test of GET '/ric' or V2 GET '/v2/rics/ric'
To test the response code only, provide the expected response code and managed element id.
To test the returned ric id, provide the expected ric id.

| arg list |
|--|
| `<reponse-code> <managed-element-id> [<ric-id>]` |

| arg list V2 |
|--|
| `<reponse-code> <management-element-id>\|NOME <ric-id>\|<NORIC> [<string-of-ricinfo>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<managed-element-id>` |  Id of the managed element |
| `NOME` |  Indicator for no ME |
| `ric-id` |  Id of the ric |
| `NORIC` |  Indicator no RIC |
| `string-of-ricinfo` |  String of ric info |

## Function: a1pms_api_get_rics ##

Test of GET '/rics' or V2 GET '/v2/rics' and optional check of the returned json payload (ricinfo).
To test the response code only, provide the expected response code and policy type id (or NOTYPE if no type is given).
To test also the returned payload, add the formatted string of info in the returned payload.
Format of ricinfo: <br>`<ric-id>:<list-of-mes>:<list-of-policy-type-ids>`<br>
Example <br>`<space-separate-string-of-ricinfo> = "ricsim_g1_1:me1_ricsim_g1_1,me2_ricsim_g1_1:1,2,4 ricsim_g1_1:me2_........."`

| arg list |
|--|
| `<reponse-code> <policy-type-id>\|NOTYPE [<space-separate-string-of-ricinfo>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<policy-type-id>` |  Policy type id of the ric |
| `NOTYPE>` |  No type given |
| `<space-separate-string-of-ricinfo>` |  A space separated string of ric info - needs to be quoted |

## Function: a1pms_api_put_service ##

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

## Function: a1pms_api_get_services ##

Test of GET '/service' or V2 GET '/v2/services' and optional check of the returned json payload.
To test only the response code, omit all parameters except the expected response code.
To test the returned json, provide the parameters after the response code.

| arg list |
|--|
| `<response-code> [ (<query-service-name> <target-service-name> <keepalive-timeout> <callbackurl>) \| (NOSERVICE <target-service-name> <keepalive-timeout> <callbackurl> [<target-service-name> <keepalive-timeout> <callbackurl>]* )]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<query-service-name>` |  Service name for the query |
| `<target-service-name>` |  Target service name|
| `<keepalive-timeout>` |  Timeout value |
| `<callbackurl>` |  Callback url |
| `NOSERVICE` |  Indicator of no target service name |

## Function: a1pms_api_get_service_ids ##

Test of GET '/services' or V2 GET /'v2/services'. Only check of service ids.

| arg list |
|--|
| `<response-code> [<service-name>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Function: a1pms_api_delete_services ##

Test of DELETE '/services' or V2 DELETE '/v2/services/{serviceId}'

| arg list |
|--|
| `<response-code> [<service-name>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Function: a1pms_api_put_services_keepalive ##

Test of PUT '/services/keepalive' or V2 PUT '/v2/services/{service_id}/keepalive'

| arg list |
|--|
| `<response-code> <service-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-name>` |  Service name |

## Function: a1pms_api_put_configuration ##

Test of PUT '/v2/configuration'

| arg list |
|--|
| `<response-code> <config-file>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<config-file>` |  Path json config file |

## Function: a1pms_api_get_configuration ##

Test of GET '/v2/configuration'

| arg list |
|--|
| `<response-code> [<config-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<config-file>` |  Path json config file to compare the retrieved config with |

## Function: a1pms_kube_pvc_reset ##
Admin reset to remove all policies and services
All types and instances etc are removed - types and instances in a1 sims need to be removed separately
NOTE - only works in kubernetes and the pod should not be running

| arg list |
|--|
| None |


# Description of functions in authsidecar_api_function.sh #
Only common API functions in this file.


## Function: start_chart_museum ##

Start the Chart Museum
| arg list |
|--|
| None |

## Function: chartmus_upload_test_chart ##

Upload a package chart to chartmusem
| arg list |
|--|
| `<chart-name>` |

| parameter | description |
| --------- | ----------- |
| `<chart-name>` | Name of the chart to upload |

## Function: chartmus_delete_test_chart ##

Delete a chart in chartmusem
| arg list |
|--|
| `<chart-name> [<version>]` |

| parameter | description |
| --------- | ----------- |
| `<chart-name>` | Name of the chart to delete |
| `<version>` | Chart version, default is 0.1.0 |

# Description of functions in cp_api_function.sh #

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
| `<cr-path-id> <variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<cr-path-id>` |  Variable index to CR |
| `<variable-name>` | Variable name in the CR  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: cr_greater_or_equal ##
Tests if a variable value in the Callback Receiver (CR) simulator is equal to or greater than a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to or greater than the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'cr' dir for more details.
| arg list |
|--|
| `<cr-path-id>  <variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<cr-path-id>` |  Variable index to CR |
| `<variable-name>` | Variable name in the CR  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: cr_contains_str ##

Tests if a variable value in the CR contains a target string.
Without the timeout, the test sets pass or fail immediately depending on if the variable contains the target string or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value contains the target string or not.
See the 'a1-interface' repo for more details.

| arg list |
|--|
| `<cr-path-id>  <variable-name> <target-value> [ <timeout-in-sec> ]` |


| parameter | description |
| --------- | ----------- |
| `<cr-path-id>` |  Variable index to CR |
| `<variable-name>` | Variable name in the CR  |
| `<target-value>` | Target substring for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: cr_read ##

Reads the value of a variable in the CR simulator. The value is intended to be passed to a env variable in the test script.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<cr-path-id> <variable-name>` |

| parameter | description |
| --------- | ----------- |
| `<cr-path-id>` |  Variable index to CR |
| `<variable-name>` | Variable name in the CR  |

## Function: cr_delay_callback ##

Function to configure write delay on callbacks. Delay given in seconds. Setting remains until removed.

| arg list |
|--|
| `<response-code> <cr-path-id> [<delay-in-seconds>]`|

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` |  Variable index to CR |
| `<delay-in-seconds>` |  Delay in seconds. If omitted, the delay is removed |

## Function: cr_api_check_all_sync_events ##

Check the contents of all ric events received for a callback id.

| arg list |
|--|
| `<response-code> <cr-path-id>  <id> [ EMPTY \| ( <ric-id> )+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<id>` | Id of the callback destination  |
| `EMPTY` | Indicator for an empty list  |
| `<ric-id>` | Id of the ric  |

## Function: cr_api_check_all_ics_events ##

Check the contents of all current status events for one id from ICS

| arg list |
|--|
| `<response-code> <cr-path-id> <id> [ EMPTY \| ( <status> )+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<id>` | Id of the callback destination  |
| `EMPTY` | Indicator for an empty list  |
| `<status>` | Status string  |

## Function: cr_api_check_all_ics_subscription_events ##

Check the contents of all current subscription events for one id from ICS

| arg list |
|--|
| `<response-code> <cr-path-id>  <id> [ EMPTY | ( <type-id> <schema> <registration-status> )+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<id>` | Id of the callback destination  |
| `EMPTY` | Indicator for an empty list  |
| `<type-id>` | Id of the data type  |
| `<schema>` | Path to typeschema file  |
| `<registration-status>` | Status string  |

## Function: cr_api_reset ##

Reset the callback receiver

| arg list |
|--|
| `<cr-path-id>` |

| parameter | description |
| --------- | ----------- |
| `<cr-path-id>` | Variable index for CR  |

## Function: cr_api_check_all_generic_json_events ##

Check the contents of all json events for path

| arg list |
|--|
| `<response-code> <cr-path-id>  <topic-url> (EMPTY | <json-msg>+ )` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<topic-url>` | Topic url  |
| `EMPTY` | Indicator for an empty list  |
| `json-msg` | Json msg string to compare with  |

## Function: cr_api_check_single_generic_json_event ##

Check a single (oldest) json event (or none if empty) for path

| arg list |
|--|
| `<response-code> <cr-path-id> <topic-url> (EMPTY | <json-msg> )` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<topic-url>` | Topic url  |
| `EMPTY` | Indicator for no msg  |
| `json-msg` | Json msg string to compare with  |

## Function: cr_api_check_single_generic_event_md5 ##

Check a single (oldest) json in md5 format (or none if empty) for path.
Note that if a json message is given, it shall be compact, no ws except inside string.
The MD5 will generate different hash if whitespace is present or not in otherwise equivalent json.

| arg list |
|--|
| `<response-code> <cr-path-id> <topic-url> (EMPTY | <data-msg> )` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<topic-url>` | Topic url  |
| `EMPTY` | Indicator for no msg  |
| `data-msg` | msg string to compare with  |

## Function: cr_api_check_single_generic_event_md5_file ##

Check a single (oldest) event in md5 format (or none if empty) for path.
Note that if a file with json message is given, the json shall be compact, no ws except inside string and not newlines.
The MD5 will generate different hash if ws/newlines is present or not in otherwise equivalent json

| arg list |
|--|
| `<response-code> <cr-path-id> <topic-url> (EMPTY | <data-file> )` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<cr-path-id>` | Variable index for CR  |
| `<topic-url>` | Topic url  |
| `EMPTY` | Indicator for no msg  |
| `data-file` | path to file to compare with  |

# Description of functions in dmaapadp_api_functions.sh #

## Function: use_dmaapadp_http ##

Use http for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.

| arg list |
|--|
| None |

## Function: use_dmaapadp_https ##

Use https for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.

| arg list |
|--|
| None |

## Function: start_dmaapadp ##

Start the dmaap adator service container in docker or kube depending on running mode.

| arg list |
|--|
| (kube) `PROXY\|NOPROXY <config-file> [ <data-file> ]` |

| parameter | description |
| --------- | ----------- |
| `PROXY` | Configure with http proxy, if proxy is started  |
| `NOPROXY` | Configure without http proxy  |
| `<config-file>`| Path to application.yaml  |
| `<data-file>` | Optional path to application_configuration.json  |

## Function: set_dmaapadp_trace ##

Configure the dmaap adaptor service log on trace level. The app must be running.
| arg list |
|--|
| None |

# Description of functions in dmaapmed_api_functions.sh #

## Function: use_dmaapmed_http ##

Use http for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.

| arg list |
|--|
| None |

## Function: use_dmaapmed_https ##

Use https for all proxy requests. Note that this only applicable to the actual proxy request, the proxied protocol can still be http and https.

| arg list |
|--|
| None |

## Function: start_dmaapmed ##

Start the dmaap mediator service container in docker or kube depending on running mode.

| arg list |
|--|
| None |

# Description of functions in helmmanager_api_functions.sh #

## Function: use_helm_manager_http ##

Use http for all API calls to the Helm Manager. This is the default protocol.
| arg list |
|--|
| None |

## Function: use_helm_manager_https ##

Use https for all API calls to the Helm Manager.
| arg list |
|--|
| None |

## Function: start_helm_manager ##

Start the Helm Manager container in docker or kube depending on running mode.
| arg list |
|--|
| None |

## Function: helm_manager_api_get_charts ##

Get all charts and compare the expected contents.
| arg list |
|--|
| `<response-code> [ EMPTY | ( <chart> <version> <namespace> <release> <repo> )+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `EMPTY` | Indicator for empty list  |
| `<chart>`| Name of the chart  |
| `<version>`| Version of the chart  |
| `<namespace>`| Namespace to of the chart  |
| `<release>`| Release name of the chart  |
| `<repo>`| Repository of the chart  |

## Function: helm_manager_api_post_repo ##

Add repo to the helm manager.
| arg list |
|--|
| `<response-code> <repo-name> <repo-protocol> <repo-address> <repo-port>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `<repo-name>` | Name of the repo  |
| `<repo-protocol>`| Protocol http or https  |
| `<repo-address>`| Host name of the repo |
| `<repo-port>`| Host port of the repo  |

## Function: helm_manager_api_post_onboard_chart ##

Onboard a chart to the helm manager.
| arg list |
|--|
| `<response-code> <repo> <chart> <version> <release> <namespace>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `<repo>`| Target repo of the chart  |
| `<chart>`| Name of the chart  |
| `<version>`| Version of the chart  |
| `<namespace>`| Namespace to of the chart  |
| `<release>`| Release name of the chart  |

## Function: helm_manager_api_post_install_chart ##

Install an onboarded chart.
| arg list |
|--|
| `<response-code> <chart> <version>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `<chart>`| Name of the chart  |
| `<version>`| Version of the chart  |

## Function: helm_manager_api_uninstall_chart ##

Uninstall a chart.
| arg list |
|--|
| `<response-code> <chart> <version>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `<chart>`| Name of the chart  |
| `<version>`| Version of the chart  |

## Function: helm_manager_api_delete_chart ##

Delete a chart.
| arg list |
|--|
| `<response-code> <chart> <version>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `<chart>`| Name of the chart  |
| `<version>`| Version of the chart  |

## Function: helm_manager_api_exec_add_repo ##

Add repo in helm manager by helm using exec.
| arg list |
|--|
| `<repo-name> <repo-url>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected response code |
| `<repo-name>`| Name of the repo  |
| `<repo-url>`| Full url to the repo. Url must be accessible by the container  |

# Description of functions in httpproxy_api_functions.sh #

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


# Description of functions in ics_api_functions.sh #

## Function: use_ics_rest_http ##

Use http for all API calls to the ICS. This is the default protocol.
| arg list |
|--|
| None |

## Function: use_ics_rest_https ##

Use https for all API calls to the ICS.
| arg list |
|--|
| None |

## Function: use_ics_dmaap_http ##

Send and recieve all API calls to the ICS over Dmaap via the MR using http.
| arg list |
|--|
| None |

## Function: use_ics_dmaap_https ##

Send and recieve all API calls to the ICS over Dmaap via the MR using https.
| arg list |
|--|
| None |

## Function: start_ics ##

Start the ICS container in docker or kube depending on running mode.
| arg list |
|--|
| `PROXY|NOPROXY <config-file>` |

| parameter | description |
| --------- | ----------- |
| `PROXY` | Configure with http proxy, if proxy is started  |
| `NOPROXY` | Configure without http proxy  |
| `<config-file>`| Path to application.yaml  |

## Function: stop_ics ##

Stop the ICS container.
| arg list |
|--|
| None |

## Function: start_stopped_ics ##

Start a previously stopped ics.
| arg list |
|--|
| None |

## Function: set_ics_debug ##

Configure the ICS log on debug level. The ICS must be running.
| arg list |
|--|
| None |

## Function: set_ics_trace ##

Configure the ICS log on trace level. The ICS must be running.
| arg list |
|--|
| None |

## Function: use_ics_retries ##

Perform curl retries when making direct call to ICS for the specified http response codes
Speace separated list of http response codes
| arg list |
|--|
| `[<response-code>]*` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code to make retry for  |

## Function: check_ics_logs ##

Check the ICS log for any warnings and errors and print the count of each.
| arg list |
|--|
| None |

## Function: ics_equal ##

Tests if a variable value in the ICS is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'a1-interface' repo for more details.

| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in ics  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: ics_api_a1_get_job_ids ##

Test of GET '/A1-EI/v1/eitypes/{eiTypeId}/eijobs' and optional check of the array of returned job ids.
To test the response code only, provide the response code parameter as well as a type id and an owner id.
To also test the response payload add the 'EMPTY' for an expected empty array or repeat the last parameter for each expected job id.

| arg list |
|--|
| `<response-code> <type-id>  <owner-id>\|NOOWNER [ EMPTY \| <job-id>+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<owner-id>` | Id of the job owner  |
| `NOOWNER` | No owner is given  |
| `<job-id>` | Id of the expected job  |
| `EMPTY` | The expected list of job id shall be empty  |

## Function: ics_api_a1_get_type ##

Test of GET '/A1-EI/v1/eitypes/{eiTypeId}' and optional check of the returned schema.
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

## Function: ics_api_a1_get_type_ids ##

Test of GET '/A1-EI/v1/eitypes' and optional check of returned list of type ids.
To test the response code only, provide the response only.
To also test the response payload add the list of expected type ids (or EMPTY if the list is expected to be empty).

| arg list |
|--|
| `<response-code> [ (EMPTY \| [<type-id>]+) ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `EMPTY` | The expected list of type ids shall be empty  |
| `<type-id>` | Id of the EI type  |

## Function: ics_api_a1_get_job_status ##

Test of GET '/A1-EI/v1/eitypes/{eiTypeId}/eijobs/{eiJobId}/status' and optional check of the returned status.
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

## Function: ics_api_a1_get_job ##

Test of GET '/A1-EI/v1/eitypes/{eiTypeId}/eijobs/{eiJobId}' and optional check of the returned job.
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

## Function: ics_api_a1_delete_job ##

Test of DELETE '/A1-EI/v1/eitypes/{eiTypeId}/eijobs/{eiJobId}'.
To test, provide all the specified parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the EI type  |
| `<job-id>` | Id of the job  |

## Function: ics_api_a1_put_job ##

Test of PUT '/A1-EI/v1/eitypes/{eiTypeId}/eijobs/{eiJobId}'.
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

## Function: ics_api_edp_get_type_ids ##

Test of GET '/ei-producer/v1/eitypes' or '/data-producer/v1/info-types' depending on ics version and an optional check of the returned list of type ids.
To test the response code only, provide the response code.
To also test the response payload add list of expected type ids (or EMPTY if the list is expected to be empty).

| arg list |
|--|
| `<response-code> [ EMPTY \| <type-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_edp_get_producer_status ##

Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}/status' or '/data-producer/v1/info-producers/{infoProducerId}/status' depending on ics version and optional check of the returned status.
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

## Function: ics_api_edp_get_producer_ids ##

Test of GET '/ei-producer/v1/eiproducers' and optional check of the returned producer ids.
To test the response code only, provide the response.
To also test the response payload add the list of expected producer-ids (or EMPTY if the list of ids is expected to be empty).

| arg list |
|--|
| `<response-code> [ EMPTY \| <producer-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_edp_get_producer_ids_2 ##

Test of GET '/ei-producer/v1/eiproducers' or '/data-producer/v1/info-producers' depending on ics version and optional check of the returned producer ids.
To test the response code only, provide the response.
To also test the response payload add the type (if any) and a list of expected producer-ids (or EMPTY if the list of ids is expected to be empty).

| arg list |
|--|
| `<response-code> [ ( NOTYPE \| <type-id> ) [ EMPTY \| <producer-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |
| `EMPTY` | No type given  |
| `<producer-id>` | Id of the producer  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_edp_get_type ##

Test of GET '/ei-producer/v1/eitypes/{eiTypeId}' and optional check of the returned type.
To test the response code only, provide the response and the type-id.
To also test the response payload add a path to a job schema file and a list expected producer-id (or EMPTY if the list of ids is expected to be empty).

| arg list |
|--|
| `<response-code> <type-id> [<job-schema-file> (EMPTY \| [<producer-id>]+)]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |
| `<job-schema-file>` | Path to a job schema file  |
| `<producer-id>` | Id of the producer  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_edp_get_type_2 ##

Test of GET '/ei-producer/v1/eitypes/{eiTypeId}' or '/data-producer/v1/info-types/{infoTypeId}' depending on ics version and optional check of the returned type.
To test the response code only, provide the response and the type-id.
To also test the response payload add a path to a job schema file.

| arg list |
|--|
| `<response-code> <type-id> [<job-schema-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |
| `<job-schema-file>` | Path to a job schema file  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_edp_put_type_2 ##

Test of PUT '/ei-producer/v1/eitypes/{eiTypeId}' or '/data-producer/v1/info-types/{infoTypeId}' depending on ics version and optional check of the returned type.

| arg list |
|--|
| `<response-code> <type-id> [<job-schema-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |
| `<job-schema-file>` | Path to a job schema file  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_edp_delete_type_2 ##

Test of DELETE '/ei-producer/v1/eitypes/{eiTypeId}' or '/data-producer/v1/info-types/{infoTypeId}' depending on ics version and optional check of the returned type.

| arg list |
|--|
| `<response-code> <type-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |

## Function: ics_api_edp_get_producer ##

Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}' and optional check of the returned producer.
To test the response code only, provide the response and the producer-id.
To also test the response payload add the remaining parameters defining thee producer.

| arg list |
|--|
| `<response-code> <producer-id> [<create-callback> <delete-callback> <supervision-callback> (EMPTY\| [<type-id> <schema-file>]+) ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<create-callback>` | Callback for create job  |
| `<delete-callback>` | Callback for delete job  |
| `<supervision-callback>` | Callback for producer supervision  |
| `<type-id>` | Id of the type  |
| `<schema-file>` | Path to a schema file  |
| `EMPTY` | The expected list of type schema pairs shall be empty  |

## Function: ics_api_edp_get_producer_2 ##

Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}' or '/data-producer/v1/info-producers/{infoProducerId}' depending on ics version and optional check of the returned producer.
To test the response code only, provide the response and the producer-id.
To also test the response payload add the remaining parameters defining thee producer.

| arg list |
|--|
| `<response-code> <producer-id> [<job-callback> <supervision-callback> (EMPTY \| <type-id>+) ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-callback>` | Callback for the url  |
| `<supervision-callback>` | Callback for producer supervision  |
| `<type-id>` | Id of the type  |
| `EMPTY` | The expected list of types shall be empty  |

## Function: ics_api_edp_delete_producer ##

Test of DELETE '/ei-producer/v1/eiproducers/{eiProducerId}' or '/data-producer/v1/info-producers/{infoProducerId}' depending on ics version.
To test, provide all parameters.

| arg list |
|--|
| `<response-code> <producer-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |

## Function: ics_api_edp_put_producer ##

Test of PUT '/ei-producer/v1/eiproducers/{eiProducerId}'.
To test, provide all parameters. The list of type/schema pair may be empty.

| arg list |
|--|
| `<response-code> <producer-id> <job-callback> <supervision-callback> (EMPTY \| [<type-id> <schema-file>]+)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-callback>` | Callback for create/delete job  |
| `<supervision-callback>` | Callback for producer supervision  |
| `<type-id>` | Id of the type  |
| `<schema-file>` | Path to a schema file  |
| `EMPTY` | The list of type/schema pairs is empty  |

## Function: ics_api_edp_put_producer_2 ##

Test of PUT '/ei-producer/v1/eiproducers/{eiProducerId}' or '/data-producer/v1/info-producers/{infoProducerId}' depending on ics version.
To test, provide all parameters. The list of type/schema pair may be empty.

| arg list |
|--|
| `<response-code> <producer-id> <job-callback> <supervision-callback> NOTYPE\|[<type-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-callback>` | Callback for create/delete job  |
| `<supervision-callback>` | Callback for producer supervision  |
| `<type-id>` | Id of the type  |
| `NOTYPE` | The list of types is empty  |

## Function: ics_api_edp_get_producer_jobs ##

Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}/eijobs' and optional check of the returned producer job.
To test the response code only, provide the response and the producer-id.
To also test the response payload add the remaining parameters.

| arg list |
|--|
| `<response-code> <producer-id> (EMPTY \| [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)` |

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

## Function: ics_api_edp_get_producer_jobs_2 ##

Test of GET '/ei-producer/v1/eiproducers/{eiProducerId}/eijobs' or '/data-producer/v1/info-producers/{infoProducerId}/info-jobs' depending on ics version and optional check of the returned producer job.
To test the response code only, provide the response and the producer-id.
To also test the response payload add the remaining parameters.

| arg list |
|--|
| `<response-code> <producer-id> (EMPTY \| [<job-id> <type-id> <target-url> <job-owner> <template-job-file>]+)` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<job-id>` | Id of the job  |
| `<type-id>` | Id of the type  |
| `<target-url>` | Target url for data delivery  |
| `<job-owner>` | Id of the job owner  |
| `<template-job-file>` | Path to a job template file  |
| `EMPTY` | The list of job/type/target/job-file tuples is empty  |

## Function: ics_api_service_status ##

Test of GET '/status'.

| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

## Function: ics_api_idc_get_type_ids ##

Test of GET '/data-consumer/v1/info-types' and an optional check of the returned list of type ids.
To test the response code only, provide the response code.
To also test the response payload add list of expected type ids (or EMPTY if the list is expected to be empty).

| arg list |
|--|
| `<response-code> [ EMPTY \| <type-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the Info type  |
| `EMPTY` | The expected list of type ids shall be empty  |

## Function: ics_api_idc_get_job_ids ##

Test of GET '/data-consumer/v1/info-jobs' and optional check of the array of returned job ids.
To test the response code only, provide the response code parameter as well as a type id and an owner id.
To also test the response payload add the 'EMPTY' for an expected empty array or repeat the last parameter for each expected job id.

| arg list |
|--|
| `<response-code> <type-id>  <owner-id>\|NOOWNER [ EMPTY \| <job-id>+ ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the Info type  |
| `<owner-id>` | Id of the job owner  |
| `NOOWNER` | No owner is given  |
| `<job-id>` | Id of the expected job  |
| `EMPTY` | The expected list of job id shall be empty  |

## Function: ics_api_idc_get_job ##

Test of GET '/data-consumer/v1/info-jobs/{infoJobId}' and optional check of the returned job.
To test the response code only, provide the response code, type id and job id.
To also test the response payload add the remaining parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id> [<target-url> <owner-id> <template-job-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the Info type  |
| `<job-id>` | Id of the job  |
| `<target-url>` | Expected target url for the job  |
| `<owner-id>` | Expected owner for the job  |
| `<template-job-file>` | Path to a job template for job parameters of the job  |

## Function: ics_api_idc_put_job ##

Test of PUT '/data-consumer/v1/info-jobs/{infoJobId}'.
To test, provide all the specified parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id> <target-url> <owner-id> <template-job-file> [VALIDATE]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the Info type  |
| `<job-id>` | Id of the job  |
| `<target-url>` | Target url for the job  |
| `<owner-id>` | Owner of the job  |
| `<template-job-file>` | Path to a job template for job parameters of the job  |
| `VALIIDATE` | Indicator to preform type validation at creation  |

## Function: ics_api_idc_delete_job ##

Test of DELETE '/A1-EI/v1/eitypes/{eiTypeId}/eijobs/{eiJobId}'.
To test, provide all the specified parameters.

| arg list |
|--|
| `<response-code> <type-id> <job-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the type  |
| `<job-id>` | Id of the job  |

## Function: ics_api_idc_get_type ##

Test of GET '/data-consumer/v1/info-types/{infoTypeId} and optional check of the returned schema.
To test the response code only, provide the response code parameter as well as the type-id.
To also test the response payload add a path to the expected schema file.

| arg list |
|--|
| `<response-code> <type-id> [<schema-file>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type-id>` | Id of the Info type  |
| `<schema-file>` | Path to a schema file to compare with the returned schema  |

## Function: ics_api_idc_get_job_status ##

Test of GET '/data-consumer/v1/info-jobs/{infoJobId}/status' and optional check of the returned status and timeout.
To test the response code only, provide the response code and job id.
To also test the response payload add the expected status.

| arg list |
|--|
| `<response-code> <job-id> [<status> [ <timeout>]]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<job-id>` | Id of the job  |
| `<status>` | Expected status  |
| `<timeout>` | Timeout |

## Function: ics_api_idc_get_job_status2 ##

Test of GET '/data-consumer/v1/info-jobs/{infoJobId}/status' with returned producers and optional check of the returned status and timeout.
To test the response code only, provide the response code and job id.
To also test the response payload add the expected status.

| arg list |
|--|
| `<response-code> <job-id> [<status> EMPTYPROD|( <prod-count> <producer-id>+ ) [<timeout>]]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<job-id>` | Id of the job  |
| `<status>` | Expected status  |
| `<EMPTYPROD>` | Indicated for empty list of producer  |
| `<prod-count>` | Number of expected producer  |
| `<producer-id>` |Id of the producer  |
| `<timeout>` | Timeout |


## Function: ics_api_idc_get_subscription_ids ##
Test of GET '/data-consumer/v1/info-type-subscription' with the returned list of subscription ids

| arg list |
|--|
| `<response-code>  <owner-id>|NOOWNER [ EMPTY | <subscription-id>+]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<owner-id>` | Id of the owner  |
| `<NOOWNER>` | Indicator for empty owner  |
| `<EMPTY>` | Indicated for empty list of subscription ids  |
| `<subscription-id>` |Id of the subscription  |

## Function: ics_api_idc_get_subscription ##
Test of GET '/data-consumer/v1/info-type-subscription/{subscriptionId}' with the subscription information

| arg list |
|--|
| `<response-code>  <subscription-id> [ <owner-id> <status-uri> ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<subscription-id>` |Id of the subscription  |
| `<owner-id>` | Id of the owner  |
| `<status-uri>` | Url for status notifications  |


## Function: ics_api_idc_put_subscription ##
Test of PUT '/data-consumer/v1/info-type-subscription/{subscriptionId}' with the subscription information

| arg list |
|--|
| `<response-code>  <subscription-id> <owner-id> <status-uri>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<subscription-id>` |Id of the subscription  |
| `<owner-id>` | Id of the owner  |
| `<status-uri>` | Url for status notifications  |

## Function: ics_api_idc_delete_subscription ##
Test of DELETE /data-consumer/v1/info-type-subscription/{subscriptionId}

| arg list |
|--|
| `<response-code>  <subscription-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<subscription-id>` |Id of the subscription  |


## Function: ics_api_admin_reset ##

Test of GET '/status'.

| arg list |
|--|
| `<response-code> [ <type> ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<type>` | Type id, if the interface supports type in url |

## Function: ics_kube_pvc_reset ##

Admin reset to remove all data in ics; jobs, producers etc
NOTE - only works in kubernetes and the pod should not be running

| arg list |
|--|
| None |

# Description of functions in istio_api_functions.sh #

## Function: istio_enable_istio_namespace ##
Enable istio on a namespace

| arg list |
|--|
| `<namespace>` |

| parameter | description |
| --------- | ----------- |
| `namespace` | Namespace name to enable istio on |

## Function: istio_req_auth_by_jwksuri ##
Set isto authentication by jwks uri

| arg list |
|--|
| `<app-name> <namespace> <realm>>` |

| parameter | description |
| --------- | ----------- |
| `<app-name>` | App-name, short name |
| `<namespace>` | Namespace of app  |
| `<realm>` | Name of realm  |

## Function: istio_req_auth_by_jwks ##
Set isto authentication by jwks keys (inline)

| arg list |
|--|
| `<app> <namespace> <issuer> <key>` |

| parameter | description |
| --------- | ----------- |
| `<app-name>` | App-name, short name |
| `<namespace>` | Namespace of app  |
| `<issuer>` | Name of issuer |
| `key` | Inline key  |

## Function: istio_auth_policy_by_realm ##
Set isio authorization by realm

| arg list |
|--|
| `<app> <namespace> <realam> [<client-id> <client-role>]` |

| parameter | description |
| --------- | ----------- |
| `<app-name>` | App-name, short name |
| `<namespace>` | Namespace of app  |
| `<realm>` | Name of realm  |
| `<client-id>` | Client id to authorize |
| `<client-role>` | Client role to authorize |


## Function: istio_auth_policy_by_issuer ##
Set isio authorization by issuer

| arg list |
|--|
| `<app> <namespace> <issuer>` |

| parameter | description |
| --------- | ----------- |
| `<app-name>` | App-name, short name |
| `<namespace>` | Namespace of app  |
| `<issuer>` | Name of issuer |


# Description of functions in kafkapc_api_functions.sh #

## Function: use_kafkapc_http ##

Use http for all calls to the KAFKAPC.
| arg list |
|--|
| None |

## Function: use_kafkapc_https ##

Use https for all calls to the KAFKAPC.
| arg list |
|--|
| None |

## Function: start_kafkapc ##

Start the KAFKAPC container in docker or kube depending on start mode
| arg list |
|--|
| None |

## Function: kafkapc_equal ##

Tests if a variable value in the KAFKAPC is equal to a target value.
Without the timeout, the test sets pass or fail immediately depending on if the variable is equal to the target or not.
With the timeout, the test waits up to the timeout seconds before setting pass or fail depending on if the variable value becomes equal to the target value or not.
See the 'mrstub' dir for more details.
| arg list |
|--|
| `<variable-name> <target-value> [ <timeout-in-sec> ]` |

| parameter | description |
| --------- | ----------- |
| `<variable-name>` | Variable name in the KAFKAPC  |
| `<target-value>` | Target value for the variable  |
| `<timeout-in-sec>` | Max time to wait for the variable to reach the target value  |

## Function: kafkapc_api_reset ##

Deep reset of KAFKAPC. Note that kafka itself is not affected, i.e. created topic still exist in kafka.
| arg list |
|--|
| None |

## Function: kafkapc_api_create_topic ##

Create a topic in kafka via kafkapc.
| `<response-code> <topic-name>  <mime-type>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic-name>` | Name of the topic  |
| `<mime-type>` | Mime type of the data to send to the topic. Data on the topic is expected to be of this type  |

## Function: kafkapc_api_get_topic ##

Create a from kafkapc.
| `<response-code> <topic-name>  <mime-type>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic-name>` | Name of the topic  |
| `<mime-type>` | Mime type of the topic  |

## Function: kafkapc_api_start_sending ##

Start sending msg from the msg queue to kafka for a topic.
| `<response-code> <topic-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic-name>` | Name of the topic  |

## Function: kafkapc_api_start_receiving ##

Start receiving msg from a kafka topic to the msg queue in kafkapc.
| `<response-code> <topic-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic-name>` | Name of the topic  |

## Function: kafkapc_api_stop_sending ##

Stop sending msg from the msg queue to kafka for a topic.
| `<response-code> <topic-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic-name>` | Name of the topic  |

## Function: kafkapc_api_stop_receiving ##

Stop receiving msg from a kafka topic to the msg queue in kafkapc.
| `<response-code> <topic-name>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic-name>` | Name of the topic  |

## Function: kafkapc_api_post_msg ##

Send a message on a topic.
| arg list |
|--|
| `<response-code> <topic> <mime-type> <msg>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic>` | Topic name  |
| `<mime-type>` | Mime type of the msg  |
| `<msg>` | String msg to send  |

## Function: kafkapc_api_get_msg ##

Get a message on a topic.
| arg list |
|--|
| `<response-code> <topic>  ([ <mime-type>  <msg> ] | NOMSG )` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic>` | Topic name  |
| `<mime-type>` | Mime type of the msg  |
| `<msg>` | String msg to receive  |
| `NOMSG` | Indicated for no msg  |

## Function: kafkapc_api_post_msg_from_file ##

Send a message in a file on a topic.
| arg list |
|--|
| `<response-code> <topic> <mime-type> <file>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic>` | Topic name  |
| `<mime-type>` | Mime type of the msg  |
| `<file>` | Filepath to the string msg to send  |

## Function: kafkapc_api_get_msg_from_file ##

Get a message on a topic.
| arg list |
|--|
| `<response-code> <topic>  <mime-type>  <file> ` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Http response code  |
| `<topic>` | Topic name  |
| `<mime-type>` | Mime type of the msg  |
| `<file>` | Filepath to the string msg to receive  |

## Function: kafkapc_api_generate_json_payload_file ##

Create json file with dummy data for payload.
| arg list |
|--|
| `<size-in-kb> <filename>` |

| parameter | description |
| --------- | ----------- |
| `<size-in-kb>` | Generated size in kb  |
| `<filename>` | Path to output file  |

## Function: kafkapc_api_generate_text_payload_file ##

Create file with dummy text data for payload.
| arg list |
|--|
| `<size-in-kb> <filename>` |

| parameter | description |
| --------- | ----------- |
| `<size-in-kb>` | Generated size in kb  |
| `<filename>` | Path to output file  |

# Description of functions in keycloak_api_functions.sh #

## Function: start_keycloak ##

Start the KEYCLOAK app in docker or kube depending on start mode
| arg list |
|--|
| None |

## Function: keycloak_api_obtain_admin_token ##

Get the admin token to use for subsequent rest calls to keycloak
| arg list |
|--|
| None |

## Function: keycloak_api_create_realm ##

Create a realm
| arg list |
|--|
| `<realm-name> <enabled> <token-expiry>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `enabled` | Enabled, true or false |
| `token-expiry` | Token lifespan in seconds |

## Function: keycloak_api_update_realm ##

Update a realm
| arg list |
|--|
| `<realm-name> <enabled> <token-expiry>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `enabled` | Enabled, true or false |
| `token-expiry` | Token lifespan in seconds |

## Function: keycloak_api_create_confidential_client ##

Create a client
| arg list |
|--|
| `<realm-name> <client-name>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |

## Function: keycloak_api_generate_client_secret ##

Generate secret for client
| arg list |
|--|
| `<realm-name> <client-name>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |

## Function: keycloak_api_get_client_secret ##

Get a  secret for client
| arg list |
|--|
| `<realm-name> <client-name>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |

## Function: keycloak_api_create_client_roles ##

Create client roles
| arg list |
|--|
| `<realm-name> <client-name> <role>+` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |
| `role` | Name of the role |

## Function: keycloak_api_map_client_roles ##

Map roles to a client
| arg list |
|--|
| `<realm-name> <client-name> <role>+` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |
| `role` | Name of the role |

## Function: keycloak_api_get_client_token ##

Get a client token
| arg list |
|--|
| `<realm-name> <client-name>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |

## Function: keycloak_api_read_client_token ##

Read a client token
| arg list |
|--|
| `<realm-name> <client-name>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |

## Function: keycloak_api_read_client_secret ##

Read the secret for client
| arg list |
|--|
| `<realm-name> <client-name>` |

| parameter | description |
| --------- | ----------- |
| `realm-name` | Name of the realm |
| `client-name` | Name of the client |

# Description of functions in kubeproxy_api_functions.sh #

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

# Description of functions in localhelm_api_functions.sh #

## Function: localhelm_create_test_chart ##

Create a dummy chart using helm
| arg list |
|--|
| `chart-name` |

| parameter | description |
| --------- | ----------- |
| `chart-name` | Name of the chart |

## Function: localhelm_package_test_chart ##

Package a dummy chart using helm
| arg list |
|--|
| `chart-name` |

| parameter | description |
| --------- | ----------- |
| `chart-name` | Name of the chart |

## Function: localhelm_installed_chart_release ##

Check if a chart is installed or not using helm
| arg list |
|--|
| `INSTALLED|NOTINSTALLED <release-name> <name-space> |

| parameter | description |
| --------- | ----------- |
| `INSTALLED` | Expecting installed chart |
| `NOTINSTALLED` | Expecting a not installed chart |
| `release-name` | Name of the release |
| `name-space` | Expected namespace |

# Description of functions in mr_api_functions.sh #

## Function: use_mr_http ##

Use http for all Dmaap calls to the MR. This is the default. The admin API is not affected.

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

## Function: dmaap_api_print_topics ##

Prints the current list of topics in DMAAP MR

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

## Function: mr_api_send_json ##

Send json to topic in mr-stub.
| arg list |
|--|
| `<topic-url> <json-msg>` |

| parameter | description |
| --------- | ----------- |
| `<topic-url>` | Topic url  |
| `<json-msg>` | Json msg as string  |

## Function: mr_api_send_text ##

Send text to topic in mr-stub.
| arg list |
|--|
| `<topic-url> <text-msg>` |

| parameter | description |
| --------- | ----------- |
| `<topic-url>` | Topic url  |
| `<text-msg>` | Text (string) msg  |



## Function: mr_api_send_json_file ##

Send json to topic in mr-stub.
| arg list |
|--|
| `<topic-url> <json-file>` |

| parameter | description |
| --------- | ----------- |
| `<topic-url>` | Topic url  |
| `<json-file>` | Path to file with json msg as string  |

## Function: mr_api_send_text_file ##

Send text to topic in mr-stub.
| arg list |
|--|
| `<topic-url> <text-file>` |

| parameter | description |
| --------- | ----------- |
| `<topic-url>` | Topic url  |
| `<text-file>` | Path to file with text msg as string  |

## Function: mr_api_generate_json_payload_file ##

Create json file with dummy data for payload.
| arg list |
|--|
| `<size-in-kb> <filename>` |

| parameter | description |
| --------- | ----------- |
| `<size-in-kb>` | Generated size in kb  |
| `<filename>` | Path to output file  |

## Function: mr_api_generate_text_payload_file ##

Create file with dummy text data for payload.
| arg list |
|--|
| `<size-in-kb> <filename>` |

| parameter | description |
| --------- | ----------- |
| `<size-in-kb>` | Generated size in kb  |
| `<filename>` | Path to output file  |

# Description of functions in ngw_api_functions.sh #

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

## Function: gateway_a1pms_get_status ##

Sample test of a1pms api (status)
Only response code tested - not payload
| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |

## Function: gateway_ics_get_types ##

Sample test of ics api (get types)
Only response code tested - not payload
| arg list |
|--|
| `<response-code>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |


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

## Function: prodstub_arm_producer ##

Preconfigure the prodstub with a producer. The producer supervision response code is optional, if not given the response code will be set to 200.

| arg list |
|--|
| `<response-code> <producer-id> [<forced_response_code>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<forced_response_code>` | Forced response code for the producer callback url |

## Function: prodstub_arm_job_create ##

Preconfigure the prodstub with a job or update an existing job. Optional create/update job response code, if not given the response code will be set to 200/201 depending on if the job has been previously created or not.

| arg list |
|--|
| `<response-code> <job-id> [<forced_response_code>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<job-id>` | Id of the job  |
| `<forced_response_code>` | Forced response code for the create callback url |

## Function: prodstub_arm_job_delete ##

Preconfigure the prodstub with a job. Optional delete job response code, if not given the response code will be set to 204/404 depending on if the job exists or not.

| arg list |
|--|
| `<response-code> <job-id> [<forced_response_code>]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<job-id>` | Id of the job  |
| `<forced_response_code>` | Forced response code for the delete callback url |

## Function: prodstub_arm_type ##

Preconfigure the prodstub with a type for a producer. Can be called multiple times to add more types.

| arg list |
|--|
| `<response-code> <producer-id> <type-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<type-id>` | Id of the type  |

## Function: prodstub_disarm_type ##

Remove a type for the producer in the rodstub. Can be called multiple times to remove more types.

| arg list |
|--|
| `<response-code> <producer-id> <type-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<producer-id>` | Id of the producer  |
| `<type-id>` | Id of the type  |

## Function: prodstub_check_jobdata ##

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

## Function: prodstub_check_jobdata_2 ##

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

## Function: prodstub_check_jobdata_3 ##

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

## Function: prodstub_delete_jobdata ##

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

# Description of functions in rc_api_function.sh #

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

## Function: rapp_cat_api_get_services ##

Check all registered services.

| arg list |
|--|
| `<response-code> [(<service-id> <version> <display-name> <description>)+ \| EMPTY ]` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |
| `<version>` | Version of the service  |
| `<display-name>` | Dislay name of the service  |
| `<description>` | Description of the service  |
| `EMPTY` | Indicator for an empty list  |

## Function: rapp_cat_api_put_service ##

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

## Function: rapp_cat_api_get_service ##

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

## Function: rapp_cat_api_delete_service ##

Check a registered service.

| arg list |
|--|
| `<response-code> <service-id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<service-id>` | Id of the service  |


# Description of functions in ricmediatorsim_api_functions.sh #

The functions below only use the admin interface of the ricmediator, no usage of the A1 interface.

## Function: use_ricmediator_simulator_http ##

Use http for all API calls (A1) toward the simulator. This is the default. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for A1PMS.
| arg list |
|--|
| None |

## Function: use_ricmediator_simulator_https ##

Use https for all API calls (A1) toward the simulator. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for A1PMS.
| arg list |
|--|
| None |

## Function: start_ricmediator_simulators ##

Start a group of simulator where a group may contain 1 more simulators. Started in docker or kube depending on start mode
| arg list |
|--|
| `ricsim_g1\|ricsim_g2\|ricsim_g3 <count> <interface-id>` |

| parameter | description |
| --------- | ----------- |
| `ricsim_g1\|ricsim_g2\|ricsim_g3` | Base name of the simulator. Each instance will have an postfix instance id added, starting on '1'. For examplle 'ricsim_g1_1', 'ricsim_g1_2' etc  |
|`<count>`| And integer, 1 or greater. Specifies the number of simulators to start|
|`<interface-id>`| Shall be the interface id of the simulator. See the repo 'a1-interface' for the available ids. |

## Function: get_kube_ricmediatorsim_host ##

Translate ric name to kube host name.
| arg list |
|--|
| `<ric-name>` |

| parameter | description |
| --------- | ----------- |
| `<ric-name>` | The name of the ric to translate into a host name (ip) |

## Function: nearsim_generate_policy_uuid ##

Geneate a UUID prefix to use along with the policy instance number when creating/deleting policies. Sets the env var UUID.
UUID is then automatically added to the policy id in GET/PUT/DELETE.
| arg list |
|--|
| None |


## Function: ricmediatorsim_put_policy_type ##

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

## Function: ricmediatorsim_delete_policy_type ##

Deletes a policy type from the simulator

| arg list |
|--|
| `<response-code> <ric-id> <policy_type_id>` |

| parameter | description |
| --------- | ----------- |
| `<response-code>` | Expected http response code |
| `<ric-id>` |  Id of the ric |
| `<policy-type-id>` |  Id of the policy type |


# Description of functions in ricsim_api_functions.sh #

The functions below only use the admin interface of the simulator, no usage of the A1 interface.

## Function: use_simulator_http ##

Use http for all API calls (A1) toward the simulator. This is the default. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for A1PMS.
| arg list |
|--|
| None |

## Function: use_simulator_https ##

Use https for all API calls (A1) toward the simulator. Admin API calls to the simulator are not affected. Note that this function shall be called before preparing the config for A1PMS.
| arg list |
|--|
| None |

## Function: start_ric_simulators ##

Start a group of simulator where a group may contain 1 more simulators. Started in docker or kube depending on start mode
| arg list |
|--|
| `ricsim_g1\|ricsim_g2\|ricsim_g3 <count> <interface-id>` |

| parameter | description |
| --------- | ----------- |
| `ricsim_g1\|ricsim_g2\|ricsim_g3` | Base name of the simulator. Each instance will have an postfix instance id added, starting on '1'. For examplle 'ricsim_g1_1', 'ricsim_g1_2' etc  |
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

## Function: sim_generate_policy_uuid ##

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

# Description of functions in sdnc_api_functions.sh #

The file contains a selection of the possible API tests towards the SDNC (a1-controller)

## Function: use_sdnc_http ##

Use http for all API calls towards the SDNC A1 Controller. This is the default. Note that this function shall be called before preparing the config for A1PMS.
| arg list |
|--|
| None |

## Function: use_sdnc_https ##

Use https for all API calls towards the SDNC A1 Controller. Note that this function shall be called before preparing the config for A1PMS.
| arg list |
|--|
| None |

## Function: start_sdnc ##

Start the SDNC A1 Controller container and its database container
| arg list |
|--|
| None |

## Function: stop_sdnc ##

Stop the SDNC A1 Controller container and its database container
| arg list |
|--|
| None |

## Function: start_stopped_sdnc ##

Start a previously stopped SDNC
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
| `<response-code> (OSC <ric-id> <policy-type-id> [ <policy-id> [<policy-id>]* ]) \| ( STD <ric-id> [ <policy-id> [<policy-id>]* ]` |

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
| `(STD <ric-id> <policy-id>) \| (OSC <ric-id> <policy-type-id> <policy-id>)` |

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
| `<response-code> (STD <ric-id> <policy-id> <template-file> ) \| (OSC <ric-id> <policy-type-id> <policy-id> <template-file>)` |

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
| `<response-code> (STD <ric-id> <policy-id> <enforce-status> [<reason>]) \| (OSC <ric-id> <policy-type-id> <policy-id> <instance-status> <has-been-deleted>)` |

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

Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
