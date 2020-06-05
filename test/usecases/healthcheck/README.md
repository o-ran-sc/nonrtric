# Use case Health Check test
# General

The Health Check use case test provides a python script that regularly creates, reads, updates, and deletes a policy
in all Near-RT RICs that support the type used by the script. A self refreshing web page provides a view of statistics
for these regular checks.

# Prerequisits
To run this script Python3 needs to be installed. To install the script's dependencies, run the following command from
the `src` folder: `pip install -r requirements.txt`

# How to run
Go to the `src/` folder and run `python3 main.py`. The script will start and run until stopped. Use the `-h` option to
see the options available for the script.

As default, the script uses the "Hello World" policy type with ID "2". To create the instances it uses the body file
`nonrtric/test/autotest/testdata/OSC/pihw_template.json`. The body file contains the string "XXX" as a parameter value.
This string vill be replaced with dynamic data during creation. It is possible to provide a custom policy type and
body file to the script at startup.

To see the web page, navigate to `localhost:9990/stats`. The page refreshes itself with the same interval as the script
uses.

## License

Copyright (C) 2020 Nordix Foundation.
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
