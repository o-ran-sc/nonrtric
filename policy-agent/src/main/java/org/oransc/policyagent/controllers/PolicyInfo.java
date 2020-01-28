/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ========================LICENSE_END===================================
 */

package org.oransc.policyagent.controllers;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "PolicyInfo")
public class PolicyInfo {

    @ApiModelProperty(value = "identity of the policy")
    public String id;

    @ApiModelProperty(value = "name of the policy type")
    public String type;

    @ApiModelProperty(value = "identity the target NearRT RIC")
    public String ric;

    @ApiModelProperty(value = "the configuration of the policy")
    public String json;

    @ApiModelProperty(value = "the name of the service owning the policy")
    public String service;

    @ApiModelProperty(value = "timestamp, last modification time")
    public String lastModified;

    PolicyInfo() {
    }

    public boolean validate() {
        return id != null && type != null && ric != null && json != null && service != null && lastModified != null;
    }

}
