## producer stub - a stub interface to simulate data producers ##

The producer stub is intended for function tests to simulate data producers.


# Ports and certificates
TBD

| Port     | Protocol |
| -------- | ----- |
| 8092     | http  |
| 8093     | https |



### Control interface ###

TBD


### Build and start ###

>Build image<br>
```docker build -t producer-stub .```

>Start the image on both http and https<br>
```docker run -it -p 8092:8092 -p 8093:8093 --name producer-stub producer-stub```

It will listen to http 8092 port and https 8093 port(using default certificates) at the same time.

TBD

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