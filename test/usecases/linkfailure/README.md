# Use case Link Failure
# General

The Link Failure use case test provides a python script that regularly polls DMaaP Message Router (MR) for "CUS Link Failure"
messages.

When such a message appears with the "eventSeverity" set to "CRITICAL" a configuration change message with the
"administrative-state" set to "UNLOCKED" will be sent to the O-DU mapped to the O-RU that sent the alarm.

When such a message appears with the "eventSeverity" set to "NORMAL" a printout will be made to signal that the
alarm has been cleared, provided that the verbose option has been used when the test was started.

# Prerequisits
To run this script Python3 needs to be installed. To install the script's dependencies, run the following command from
the `app` folder: `pip install -r requirements.txt`

Also, the MR needs to be up and running with a topic created for the alarms and there must be an endpoint for the
configuration change event that will accept these.

For convenience, a message generator and a change event endpoint simulator are provided.

# How to run
Go to the `app/` folder and run `python3 main.py`. The script will start and run until stopped. Use the `-h` option to
see the options available for the script.


## License

Copyright (C) 2021 Nordix Foundation.
Licensed under the Apache License, Version 2.0 (the "License")
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

For more information about license please see the [LICENSE](LICENSE.txt) file for details.
