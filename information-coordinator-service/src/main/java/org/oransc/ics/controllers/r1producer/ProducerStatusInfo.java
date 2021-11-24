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

package org.oransc.ics.controllers.r1producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "producer_status", description = "Status for an Info Producer")
public class ProducerStatusInfo {

    @Gson.TypeAdapters
    @Schema(name = "producer_operational_state", description = "Represents the operational states")
    public enum OperationalState {
        ENABLED, DISABLED
    }

    private static final String OPERATIONAL_STATE_DESCRIPTION = "Operational state, values:\n" //
        + "ENABLED: the producer is operational\n" //
        + "DISABLED: the producer is not operational";

    @Schema(name = "operational_state", description = OPERATIONAL_STATE_DESCRIPTION, required = true)
    @SerializedName("operational_state")
    @JsonProperty(value = "operational_state", required = true)
    public final OperationalState opState;

    public ProducerStatusInfo(OperationalState state) {
        this.opState = state;
    }

}
