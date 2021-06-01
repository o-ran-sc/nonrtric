#! /bin/bash

#  ============LICENSE_START===============================================
#  Copyright (C) 2021 Nordix Foundation. All rights reserved.
#  ========================================================================
#  Licensed under the Apache License, Version 2.0 (the "License");
#  you may not use this file except in compliance with the License.
#  You may obtain a copy of the License at
#
#       http://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#  ============LICENSE_END=================================================

# Starts a chartmuseum container


docker run --rm -it \
    -p 8222:8080 \
    --name chartmuseum \
    --network nonrtric-docker-net \
    -e DEBUG=1 \
    -e STORAGE=local \
    -e STORAGE_LOCAL_ROOTDIR=/charts \
    -v $(pwd)/charts:/charts \
    ghcr.io/helm/chartmuseum:v0.13.1