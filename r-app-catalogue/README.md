# O-RAN-SC Non-RT RIC rAPP Catalogue

The O-RAN Non-RT RIC rApp Catalogue provides an OpenApi 3.0 REST API for services to register themselves and discover
other services.

**NOTE!** The definition of the REST API is done in the `api/rac-api.json` file. The yaml version of the file is
generated during compilation.

The application is a SpringBoot application generated using the openapitools openapi-generator-maven-plugin.

To start the application run:
`mvn spring-boot:run`

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
