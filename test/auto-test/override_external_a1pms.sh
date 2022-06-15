#!/bin/bash
################################################################################
#   Copyright (c) 2022 Nordix Foundation.                                      #
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

# Override file for running external a1 pms image

# NOTE: This image is assumed to be located in a different image repo (other than oran and onap)
# NOTE: The image tag "EXTERNAL" indicate that the IMAGE_BASE var contains the full image repo path

A1PMS_IMAGE_BASE="<full image repo path - except the image tag>"
A1PMS_IMAGE_TAG_EXTERNAL="<tag>"
