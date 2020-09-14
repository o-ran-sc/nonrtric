## Overview
The bash scripts in the this dir are intended for function test of the Non-RT RIC in different configurations, using simulators when needed for the external interfaces.
A few of the bash scripts are so called 'suites', These suite scripts calls a sequence of the other bash scripts.

## Automated test scrips
There are two types of scripts, filenames in the format FTCXXX.sh test one or more components of the Non-RT RIC. Filenames in the format SuiteZZZZ.sh tests a number of FTCXXX.sh script as one suite. (XXX is an integer selected from the categories described further below).
FTS is short for Function Test Case.

The requirements, in terms of the execution enviroment, to run a script or a suite is to have docker, docker-compose and python3 is installed (the scripts warns if not installed).
The scripts has been tested to work on both MacOS and Ubuntu. They should work also in git bash on windows but not yet verified.

## Configuration
The test scripts uses configuration from a single file, found in `../common/test_env.sh`, which contains all needed configuration in terms of image names, image tags, ports, file paths, passwords etc. This file can be modified if needed.  See the README.md in  `../common/` for all details of the config file.

## How to run
A test script, for example FTC1, is executed from the cmd line using the script filename and one or more parameters:

 ./FTC1.sh remote.

See the README.md in  `../common/` for all details about available parameters and there meaning.

Each test script prints out the overall result of the tests in the end of the execution.

The test scripts produces quite a number of logs; all container logs, a log of all http/htps calls from the test scripts including the payload, some configuration created during test and also a test case log (same as what is printed on the screen during execution). All these logs are stored in `logs/FTCXXX/`. So each test script is using is own log directory.

## Test case categories
The test script are number using these basic categories.

1-99 - Basic sanity tests

100-199 - API tests

300-399 - Config changes and sync

800-899 - Stability and capacity test

900-999 - Misc test

Suites

To get an overview of the available test scripts, use the following command to print the test script description:
'grep ONELINE *.sh' in the dir of the test scripts.

## Test case file - template
A test script contains a number of steps to verify a certain functionality.
The empty template for a test case files looks like this.
Only the parts noted with < and > shall be changed.
It is strongly suggested to look at the existing test scripts, it is probably easier to copy an existing test script instead of creating one from scratch. The README.md in  `../common/` describes the functions available in the test script in detail.

-----------------------------------------------------------
```
#!/bin/bash

TC_ONELINE_DESCR="<test case description>"

. ../common/testcase_common.sh  $@
< other scripts need to sourced for specific interfaces>

#### TEST BEGIN ####


<tests here>


#### TEST COMPLETE ####

store_logs          END

```
-----------------------------------------------------------


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