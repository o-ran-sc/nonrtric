# O-RAN-SC Non-RealTime RIC O-DU Closed Loop Usecase Slice Assurance

## Configuration

The consumer takes a number of environment variables, described below, as configuration.

>- MR_HOST              **Required**. The host for Dmaap Message Router.                           Example: `http://mrproducer`
>- MR_PORT              **Required**. The port for the Dmaap Message Router.                       Example: `8095`
>- SDNR_ADDRESS         Optional. The address for SDNR.                                            Defaults to `http://localhost:3904`.
>- SDNR_USER            Optional. The user for the SDNR.                                           Defaults to `admin`.
>- SDNR_PASSWORD        Optional. The password for the SDNR user.                                  Defaults to `Kp8bJ4SXszM0WXlhak3eHlcse2gAw84vaoGGmJvUy2U`.
>- LOG_LEVEL            Optional. The log level, which can be `Error`, `Warn`, `Info` or `Debug`.  Defaults to `Info`.
>- POLLTIME             Optional. Waiting time between one pull request to Dmaap and another.      Defaults to 10 sec

## Functionality

There is a status call provided in a REST API on port 40936.
>- /status  OK

## Development

To make it easy to test during development of the consumer, there is a stub provided in the `stub` folder.

This stub is used to simulate both received VES messages from Dmaap MR with information about performance measurements for the slices in a determinated DU and also SDNR, that sends information about Radio Resource Management Policy Ratio and allows to modify value for RRM Policy Dedicated Ratio from default to higher value.

By default, SDNR stub listens to the port `3904`, but his can be overridden by passing a `--sdnr-port [PORT]` flag when starting the stub. For Dmaap MR stub default port is `3905` but it can be overriden by passing a `--dmaap-port [PORT]` flag when starting the stub.

To build and start the stub, do the following:

>1. cd stub
>2. go build
>3. ./stub [--sdnr-port <portNo>] [--dmaap-port <portNo>]

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