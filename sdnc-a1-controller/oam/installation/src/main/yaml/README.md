The SDNC-A1 controller uses the default keystore and truststore that are built into the container.

The paths and passwords for these stores are located in a properties file:
nonrtric/sdnc-a1-controller/oam/installation/src/main/properties/https-props.properties

The default truststore includes the a1simulator cert as a trusted cert which is located here:
https://gerrit.o-ran-sc.org/r/gitweb?p=sim/a1-interface.git;a=tree;f=near-rt-ric-simulator/certificate;h=172c1e5aacd52d760e4416288dc5648a5817ce65;hb=HEAD

The default keystore, truststore, and https-props.properties files can be overridden by mounting new files using the "volumes" field of docker-compose. Uncommment the following lines in docker-compose to do this, and provide paths to the new files:

#volumes:
    #  - <path_to_keystore>:/etc/ssl/certs/java/keystore.jks:ro
    #  - <path_to_truststore>:/etc/ssl/certs/java/truststore.jks:ro
    #  - <path_to_https-props>:/opt/onap/sdnc/data/properties/https-props.properties:ro

The target paths in the container should not be modified.

For example, assuming that the keystore, truststore, and https-props.properties files are located in the same directory as docker-compose:

volumes:
      - ./new_keystore.jks:/etc/ssl/certs/java/keystore.jks:ro
      - ./new_truststore.jks:/etc/ssl/certs/java/truststore.jks:ro
      - ./new_https-props.properties:/opt/onap/sdnc/data/properties/https-props.properties:ro


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


