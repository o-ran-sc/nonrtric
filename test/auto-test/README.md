
## Automated test Description
This auto-test repo stores test script for automated test cases for Policy Agent.
Each of the testcase script will bring up a containerized test enviroment for Policy Agent,
CBS, consul, and simulator(TBD).

### Overview

Right now, test cases are written in bash scripts. \
Each test case script(ex. `FTC1.sh)` will call functions defined in `../common`. \
The environment vriables are set in`test_env.sh`. \
The automated test support both local build Policy Agent image testing and remote image stored in Nexus.
```
# Local image
export POLICY_AGENT_LOCAL_IMAGE=o-ran-sc/nonrtric-policy-agent
# Remote image
export POLICY_AGENT_REMOTE_IMAGE=nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-policy-agent
```
### Test Cases Description(more TBD)
`FTC1.sh`: Test policy-agent can refresh configurations from consul

### Logs
All log files are stored at `logs/<testcase id>`. \
The logs include the application.log and the container log from Policy Agent, the container logs from each simulator and the
test case log (same as the screen output). \
In the test cases the logs are stored with a prefix so the logs can be stored at different steps during the test.
All test cases contains an entry to save all logs with prefix 'END' at the end of each test case.

### Manual
Test case command:
```
./<testcase-id>.sh local | remote

Discription:
local: test image: POLICY_AGENT_LOCAL_IMAGE=o-ran-sc/nonrtric-policy-agent
remote: test image: nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-policy-agent

```

### Test case categories
1-99 - Basic sanity tests

100-199 - API tests

300-399 - Config changes and sync

800-899 - Stability and capacity test

900-999 - Misc test

Suites

### Test case file
A test case file contains a number of steps to verify a certain functionality.
A description of the test case should be given to the ``TC_ONELINE_DESCR`` var. The description will be printed in
the test result.

The empty template for a test case files looks like this:

(Only the parts noted with < and > shall be changed.)

-----------------------------------------------------------
```
#!/usr/bin/env bash

TC_ONELINE_DESCR="<test case description>"

. ../common/testcase_common.sh $1

#### TEST BEGIN ####


<tests here>


#### TEST COMPLETE ####

store_logs          END

```
-----------------------------------------------------------

The ../common/testcase_common.sh contains all functions needed for the test case file. See the README.md file in
the ../common dir for a description of all available functions.

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