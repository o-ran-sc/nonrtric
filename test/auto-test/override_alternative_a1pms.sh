#!/bin/bash
################################################################################
#   Copyright (c) 2021 Nordix Foundation.                                      #
#                                                                              #
#   Licensed under the Apache License, Version 2.0 (the "License");            #
#   you may not use this file except in compliance with the License.           #
#   You may obtain a copy of the License at                                    #
#                                                                              #
#       http://www.apache.org/licenses/LICENSE-2.0                             #
#                                                                              #
#   Unless required by applicable law or agreed to in writing, software        #
#   distributed under the License is distributed on an "AS IS" BASIS,          #
#   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.   #
#   See the License for the specific language governing permissions and        #
#   limitations under the License.                                             #
################################################################################

# Override file for running the latest alternative a1 pms image

# NOTE: This image is aussmed to be located in a different image repo (other than oran and onap)
# NOTE: Begin with manually pulling the image to your local docker image reqistry
# NOTE: Re-tag the image to: alternative-a1pms:<image-tag>     (use same tage is specified in var A1PMS_IMAGE_TAG_LOCAL below)
# NOTE: Run the test using the flags "--use-local-image A1PMS --override override_alternative_a1pms.sh"

A1PMS_IMAGE_BASE="alternative-a1pms"
A1PMS_IMAGE_TAG_LOCAL="1.4.0-SNAPSHOT"
