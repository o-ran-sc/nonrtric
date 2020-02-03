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

import java.util.Collection;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "RicInfo")
class RicInfo {
    @ApiModelProperty(value = "identity of the ric")
    public final String name;

    @ApiModelProperty(value = "O1 identities for managed entities")
    public final Collection<String> managedElementIds;

    @ApiModelProperty(value = "supported policy types")
    public final Collection<String> policyTypes;

    RicInfo(String name, Collection<String> managedElementIds, Collection<String> policyTypes) {
        this.name = name;
        this.managedElementIds = managedElementIds;
        this.policyTypes = policyTypes;
    }
}
