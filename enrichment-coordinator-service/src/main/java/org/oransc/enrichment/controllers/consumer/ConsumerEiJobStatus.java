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

package org.oransc.enrichment.controllers.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "EiJobStatus", description = "Status for an EI Job")
public class ConsumerEiJobStatus {

    @Gson.TypeAdapters
    @ApiModel(value = "OperationalState", description = "Represents the operational states for a EI Job")
    public enum OperationalState {
        ENABLED, DISABLED
    }

    private static final String OPERATIONAL_STATE_DESCRIPTION = "Operational state, values:\n" //
        + "ENABLED: TBD\n" //
        + "DISABLED: TBD.";

    @ApiModelProperty(value = OPERATIONAL_STATE_DESCRIPTION, name = "operational_state", required = true)
    @SerializedName("operationalState")
    @JsonProperty(value = "operationalState", required = true)
    public final OperationalState state;

    public ConsumerEiJobStatus(OperationalState state) {
        this.state = state;
    }

}
