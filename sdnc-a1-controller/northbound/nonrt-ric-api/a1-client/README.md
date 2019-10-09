# A1 Mediator Client Generator

This projects generates a REST client library from the OpenAPI specification
file stored here and packages it in a jar.

## Eclipse and STS Users

The Swagger Codegen maven plugin is not supported in Eclipse/STS. You can
limp along by taking these steps:

1. Generate the code using maven:
    mvn install
2. Add this folder to the project build path:
    target/generated-sources/swagger/src/main/java

## License

Copyright (C) 2019 AT&T Intellectual Property & Nokia. All rights reserved.
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
