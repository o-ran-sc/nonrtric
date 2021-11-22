/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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

package org.oransc.ics.controllers.r1consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "consumer_information_type", description = "Information for an Information type")
public class ConsumerInfoTypeInfo {

    @Schema(name = "job_data_schema", description = "Json schema for the job data", required = true)
    @SerializedName("job_data_schema")
    @JsonProperty(value = "job_data_schema", required = true)
    public Object jobDataSchema;

    @Gson.TypeAdapters
    @Schema(name = "consumer_type_status_values", description = STATUS_DESCRIPTION)
    public enum ConsumerTypeStatusValues {
        ENABLED, DISABLED
    }

    private static final String STATUS_DESCRIPTION = "Allowed values: <br/>" //
        + "ENABLED: one or several producers for the information type are available <br/>" //
        + "DISABLED: no producers for the information type are available";

    @Schema(name = "type_status", description = STATUS_DESCRIPTION, required = true)
    @SerializedName("type_status")
    @JsonProperty(value = "type_status", required = true)
    public ConsumerTypeStatusValues state;

    @Schema(name = "no_of_producers", description = "The number of registered producers for the type", required = true)
    @SerializedName("no_of_producers")
    @JsonProperty(value = "no_of_producers", required = true)
    public int noOfProducers;

    public ConsumerInfoTypeInfo(Object jobDataSchema, ConsumerTypeStatusValues state, int noOfProducers) {
        this.jobDataSchema = jobDataSchema;
        this.state = state;
        this.noOfProducers = noOfProducers;
    }

    public ConsumerInfoTypeInfo() {
    }

}
