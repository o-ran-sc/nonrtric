#  ============LICENSE_START===============================================
#  Copyright (C) 2020-2023 Nordix Foundation. All rights reserved.
#  Copyright (C) 2023-2025 OpenInfra Foundation Europe. All rights reserved.
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

# Overview

The bash scripts in this dir are intended for function test of the Non-RT RIC in different configurations, using simulators when needed for the external interfaces.
A few of the bash scripts are so called 'suites', These suite scripts calls a sequence of the other bash test scripts.

## Automated test scripts

There are two types of scripts, filenames in the format FTCXXX.sh test one or more components of the Non-RT RIC. Filenames in the format SuiteZZZZ.sh tests a number of FTCXXX.sh script as one suite. (XXX is an integer selected from the categories described further below).
FTC is short for Function Test Case. In addition, there are also other test scripts with other naming format used for demo setup etc (e.g PM_DEMO.sh).

To list all test case files with a s short description, do `grep TC_ONELINE_DESCR *.sh` for a complete list.

The requirements, in terms of the execution environment, to run a script or a suite is to have docker, docker-compose (v2+) and python3 installed (the scripts warns if not installed). As an option, the scripts can also be executed in a Minikube or Kubernetes installation. The additional requirement is to have a clean minikube/kubernetes installation, preferably with the kube dashboard installed.
The scripts have been tested to work on both MacOS and Ubuntu using docker. They should work also in git-bash on windows (for docker) but only partly verified. Running using minikube has only been verified on Ubuntu and running on kubernetes has been verified on MacOS and Ubuntu. Successful sample tests has been made on google cloud.

## Configuration

The test scripts uses configuration from a single profile file, found in `../common/test_env-*.sh`, which contains all needed configuration in terms of image names, image tags, ports, file paths, passwords etc. There is one profile file for system (ORAN/ONAP) and release.
If temporary changes are needed to the settings in a profile file, use an override file containing only the variable to override.

## How to run

A test script, for example FTC1, is executed from the cmd line using the script filename and one or more parameters:

 >```./FTC1.sh remote docker --env-file ../common/test_env-oran-l-release.sh```

Note that this script will use the staging images. Once the release images are available,add the parameter "release" to run with released images.

See the README.md in  `../common/` for all details about available parameters and their meaning.

Each test script prints out the overall result of the tests in the end of the execution.

The test scripts produce quite a number of logs; all container logs, a log of all http/https calls from the test scripts including the payload, some configuration created during test and also a test case log (same as what is printed on the screen during execution). All these logs are stored in `logs/FTCXXX/` - basically in a dir with the same name as the script. So each test script is using its own log directory. If the same test is started again, any existing logs will be moved to a subdirectory called `previous`.

To test all components on a very basic level, run the demo test script(s) for the desired release.
Note that oran tests only include components from oran (exception is the onap sdnc).
Note that onap test uses components from onap combined with released oran components available at that onap release (e.g. Montreal contains onap images from Montreal and oran images (released images from i-release).

In general, the test scripts support the current ongoing release as well as two previous releases.


ORAN K-RELEASE
=========
>```./PM_EI_DEMO.sh remote-remove  docker  release  --env-file ../common/test_env-oran-k-release.sh  --use-release-image SDNC```

>```./PM_EI_DEMO.sh remote-remove  kube  release  --env-file ../common/test_env-oran-k-release.sh  --use-release-image SDNC```

ORAN L-RELEASE
=========
>```./PM_EI_DEMO.sh remote-remove  docker  --env-file ../common/test_env-oran-l-release.sh --use-release-image SDNC```

>```./PM_EI_DEMO.sh remote-remove  kube  --env-file ../common/test_env-oran-l-release.sh --use-release-image SDNC```

ONAP OSLO
=============
>```./PM_EI_DEMO.sh remote-remove  docker  release  --env-file ../common/test_env-onap-oslo.sh```

>```./PM_EI_DEMO.sh remote-remove  kube  release  --env-file ../common/test_env-onap-oslo.sh```

ONAP PARIS
=============
>```./PM_EI_DEMO.sh remote-remove  docker  --env-file ../common/test_env-onap-paris.sh```

>```./PM_EI_DEMO.sh remote-remove  kube  --env-file ../common/test_env-onap-paris.sh```


## Useful features

There are a fair amount of additional flags that can be used to configure the test setup. See a detailed descriptions of all flags in `test/common/README.md`.


### Stop at first error
In general, the test script will continue to try to make all tests even if there are failures. For debugging it might be needed to stop when the first test fails. In this case, add the flag below to stop test execution at the first failed test:
`--stop-at-error`

### Test released images

Tests are normally carried out on staging images. However, for testing the released images (when the images are available in the nexus report) the flag `release` shall be added to command. See also the below section to further tailor which image version (local/staging/snapshot) to use.

### Use other image builds
As default, all tests of the project images are carried out on the staging images (unless the `release` flag is given -  then released versions of the project images are used).
In some cases, for example a locally built image or a snapshot image for one or more components can be used for testing together with staging or released images.

The following flags can be used to switch image for one more individual components to use local image, remote snapshot image, remote staging image or or remote release image.

`--use-local-image`
`--use-snapshot-image`
`--use-staging-image`
`--use-release-image`

The flag shall be followed by a space separated list of app names.
Example of using a local image for A1 Policy Management Service :
`--use-local-image A1PMS`

The app names that can use other image are listed in the test engine configuration, see env var `AVAILABLE_IMAGES_OVERRIDE`in file `test/common/testengine-config.sh`

### Temporary overriding env vars in a testprofile

Each release has its own test profile, see files `common/test_env-<release-name>.sh`
For debugging it is possible to override one or more settings in the profile given to the test script.
Add the flag and file with env vars to override: `--override <override-environment-filename>`. The desired env vars shall be specified in the same way as in the test profile.

### Use other image builds from external repo for A1PMS or other app
It is possible to replace images with images from other external repos (other than oran and onap nexus repos).
Modify the file `common/override_external_a1pms.sh` with the desired values. A login to the image repo may be required prior to running the test.
Add the flag and the file: `--override common/override_external_a1pms.sh` to the command.
In addition, tell the test script to use the overridden env vars by adding the flag `--use-external-image A1PMS`

Example of running a1pms from external image repo using a test suite (a set of testscripts) and create endpoint statistics.

`./Suite-short-alternative-a1pms.sh remote-remove  docker  --env-file ../common/test_env-onap-london.sh --override common/override_external_a1pms.sh --use-external-image A1PMS --print-stats --endpoint-stats`

When the test suite is executed and all test are "PASS", a test report can be created with the following command - (only A1PMS is currently supported).
The list of IDs shall be same as used in the test suite - in this case: `FTC1 FTC10 FTC100 FTC110 FTC2001`

`./format_endpoint_stats.sh log A1PMS <overall test description> <space separated list of TC-IDs>`

The report is printed to standard out in plain text (can be piped to a file)

## Running test with external or multi node kubernetes
The test script manages the images to use in each test run. When running locally using docker desktop etc, the image registry reside on the local machine and the test script can then pull the configured images to that registry which is then used by the test script.
However, for external or multi node kubernetes clusters then each node in the cluster pull the external (nexus) images directly from the external repos. Images built locally cannot be used since the local registry is not accessible.
The solution is to use another external registry as a temporary registry.
Basically, the test script pulls the images, re-tag them and pushes them to the temporary registry. Locally built images are also pushed to the same report.
All pods started by the script will then pull correct images from the temporary registry (a docker hub registry works fine - requires login to push images though). In this way, the test script has full control over which images are actually used and it is also guaranteed that multiple pod using the same image version actually use the same image.

In addition, the kubernetes config file shall be downloaded from the master node of the cluster prior to the test - the config file is used by the command kubectl to access the cluster.

The following flags shall be added to the command: `--kubeconfig <config-file> --image-repo <repo-address> --repo-policy local\|remote`
The image repo flag shall be the name of the repo (in case of docker hub -  e.g. "myprivateregistry") and repo policy shall be set to `remote` indicating that all images shall use the temporary registry.


### Print test stats
A test script will normally run all test even if there are failures. Detailed info about total test time, number of test, number of failed test etc the flag `--print-stats` can be added. Then the detailed info is printed for each test.


## Test case categories

The test script are number using these basic categories where 0-999 are related to the policy management and 1000-1999 are related to information management. 2000-2999 are for southbound http proxy. There are also demo test cases that test more or less all components. These test scripts does not use the numbering scheme below.

The numbering in each series corresponds to the following groupings
1-99 - Basic sanity tests, A1PMS

100-199 - API tests, A1PMS

300-399 - Config changes and sync, A1PMS

800-899 - Stability and capacity test, A1PMS

900-999 - Misc test, A1PMS

11XX - ICS API Tests

18XX - ICS Stability and capacity test

20XX - Southbound http proxy tests

30XX - rApp tests

40XX - Helm Manager tests

Suites

To get an overview of the available test scripts, use the following command to print the test script description:
'grep ONELINE *.sh' in the dir of the test scripts.



## Test case file - template

A test script contains a number of steps to verify a certain functionality.
The empty template for a test case file looks like this.
Only the parts noted with < and > shall be changed.
It is strongly suggested to look at the existing test scripts, it is probably easier to copy an existing test script instead of creating one from scratch. The README.md in  `../common/` describes the functions available in the test script in detail.

-----------------------------------------------------------

```
#!/bin/bash

<license text>

TC_ONELINE_DESCR="<test case description>"

DOCKER_INCLUDED_IMAGES=<list of used apps in this test case - for docker>

KUBE_INCLUDED_IMAGES=<list of used apps (started by the script) in this test case - for kube>
KUBE_PRESTARTED_IMAGES=<list of used apps (pre-started - i.e. not started by the script) in this test case - for kube>

SUPPORTED_PROFILES=<list of supported profile names>

SUPPORTED_RUNMODES=<List of runmodes, DOCKER and/or KUBE>

CONDITIONALLY_IGNORED_IMAGES=<list of images to exclude if it does not exist in the profile file>

. ../common/testcase_common.sh $@

setup_testenvironment

#### TEST BEGIN ####


<tests here>


#### TEST COMPLETE ####

print_result

store_logs          END

```
