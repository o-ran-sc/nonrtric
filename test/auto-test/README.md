# Overview

The bash scripts in this dir are intended for function test of the Non-RT RIC in different configurations, using simulators when needed for the external interfaces.
A few of the bash scripts are so called 'suites', These suite scripts calls a sequence of the other bash test scripts.

## Automated test scripts

There are two types of scripts, filenames in the format FTCXXX.sh test one or more components of the Non-RT RIC. Filenames in the format SuiteZZZZ.sh tests a number of FTCXXX.sh script as one suite. (XXX is an integer selected from the categories described further below).
FTC is short for Function Test Case. In addition, there are also other test scripts with other naming format used for demo setup etc (e.g PM_DEMO.sh).

To list all test case files with a s short description, do `grep TC_ONELINE_DESCR *.sh` for a complete list.

The requirements, in terms of the execution environment, to run a script or a suite is to have docker, docker-compose and python3 installed (the scripts warns if not installed). As an option, the scripts can also be executed in a Minikube or Kubernetes installation. The additional requirement is to have a clean minikube/kubernetes installation, preferably with the kube dashboard installed.
The scripts have been tested to work on both MacOS and Ubuntu using docker. They should work also in git-bash on windows (for docker) but only partly verified. Running using minikube has only been verified on Ubuntu and running on kubernetes has been verified on MacOS and Ubuntu. Successful sample tests has been made on google cloud.

## Configuration

The test scripts uses configuration from a single profile file, found in `../common/test_env-*.sh`, which contains all needed configuration in terms of image names, image tags, ports, file paths, passwords etc. There is one profile file for system (ORAN/ONAP) and release.
If temporary changes are needed to the settings in a profile file, use an override file containing only the variable to override.

## How to run

A test script, for example FTC1, is executed from the cmd line using the script filename and one or more parameters:

 >```./FTC1.sh remote docker --env-file ../common/test_env-oran-h-release.sh```

Note that this script will use the staging images. Once the release images are available,add the parameter "release" to run with released images.

See the README.md in  `../common/` for all details about available parameters and their meaning.

Each test script prints out the overall result of the tests in the end of the execution.

The test scripts produce quite a number of logs; all container logs, a log of all http/https calls from the test scripts including the payload, some configuration created during test and also a test case log (same as what is printed on the screen during execution). All these logs are stored in `logs/FTCXXX/` - basically in a dir with the same name as the script. So each test script is using its own log directory. If the same test is started again, any existing logs will be moved to a subdirectory called `previous`.

To test all components on a very basic level, run the demo test script(s) for the desired release.
Note that oran tests only include components from oran (exception is the onap sdnc).
Note that onap test uses components from onap combined with released oran components available at that onap release (e.g. London contains onap images from London and oran images (released images from g-release).

In general, the test scripts support the current ongoing release as well as two previous releases.



ORAN F-RELEASE
=========
>```./PM_EI_DEMO.sh remote-remove  docker  release  --env-file ../common/test_env-oran-f-release.sh  --use-release-image SDNC```

>```./PM_EI_DEMO.sh remote-remove  kube  release  --env-file ../common/test_env-oran-f-release.sh  --use-release-image SDNC```

ORAN G-RELEASE
=========
>```./PM_EI_DEMO.sh remote-remove  docker  release  --env-file ../common/test_env-oran-g-release.sh  --use-release-image SDNC```

>```./PM_EI_DEMO.sh remote-remove  kube  release  --env-file ../common/test_env-oran-g-release.sh  --use-release-image SDNC```

ORAN H-RELEASE - current on master (april 2023)
=========
>```./PM_EI_DEMO.sh remote-remove  docker  --env-file ../common/test_env-oran-h-release.sh```

>```./PM_EI_DEMO.sh remote-remove  kube  --env-file ../common/test_env-oran-h-release.sh```


ONAP JAKARTA
=============
>```./PM_EI_DEMO.sh remote-remove  docker  release  --env-file ../common/test_env-onap-jakarta.sh```

>```./PM_EI_DEMO.sh remote-remove  kube  release  --env-file ../common/test_env-onap-jakarta.sh```


ONAP KOHN
=============
>```./PM_EI_DEMO.sh remote-remove  docker  release  --env-file ../common/test_env-onap-kohn.sh```

>```./PM_EI_DEMO.sh remote-remove  kube  release  --env-file ../common/test_env-onap-kohn.sh```


ONAP LONDON - current on master (april 2023)
=============
>```./PM_EI_DEMO.sh remote-remove  docker  --env-file ../common/test_env-onap-london.sh```

>```./PM_EI_DEMO.sh remote-remove  kube  --env-file ../common/test_env-onap-london.sh```
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

-----------------------------------------------------------

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
