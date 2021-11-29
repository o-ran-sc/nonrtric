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


## Development

To make it easy to test during development of the consumer, two stubs are provided in the `stub` folder.

One, under the `mrstub` folder, called `mrstub` that stubs the VES message received from Dmaap and pushes messages with information about performance measurements for the slices in a determinated DU. To build and start the stub, do the following:
>1. cd stub/producer
>2. go build
>3. ./mrstub

One, under the `sdnr` folder, called `sdnr` that at startup will listen for REST calls and print the body of them. By default, it listens to the port `3904`, but his can be overridden by passing a `-port [PORT]` flag when starting the stub. To build and start the stub, do the following:
>1. cd stub/sdnr
>2. go build
>3. ./sdnr


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