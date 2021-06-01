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

# Starts a helm manager container

docker run \
    --rm  \
    -it \
    -p 8112:8083  \
    --name helmmanagerservice \
    --network nonrtric-docker-net \
    -v $(pwd)/mnt/database:/var/helm-manager/database \
    -v ~/.kube:/root/.kube \
    -v ~/.helm:/root/.helm \
    -v ~/.config/helm:/root/.config/helm \
    -v ~/.cache/helm:/root/.cache/helm \
    -v $(pwd)/config/KubernetesParticipantConfig.json:/opt/app/helm-manager/src/main/resources/config/KubernetesParticipantConfig.json \
    -v $(pwd)/config/application.yaml:/opt/app/helm-manager/src/main/resources/config/application.yaml \
    nexus3.o-ran-sc.org:10004/o-ran-sc/nonrtric-helm-manager:1.0.0-SNAPSHOT