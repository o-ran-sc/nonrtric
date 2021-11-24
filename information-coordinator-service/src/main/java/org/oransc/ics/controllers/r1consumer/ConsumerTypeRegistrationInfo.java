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
@Schema(name = "consumer_type_registration_info", description = "Information for an Information type")
public class ConsumerTypeRegistrationInfo {

    @Schema(name = "info_type_id", description = "Information type identifier", required = true)
    @SerializedName("info_type_id")
    @JsonProperty(value = "info_type_id", required = true)
    public String infoTypeId;

    @Schema(name = "job_data_schema", description = "Json schema for the job data", required = true)
    @SerializedName("job_data_schema")
    @JsonProperty(value = "job_data_schema", required = true)
    public Object jobDataSchema;

    @Gson.TypeAdapters
    @Schema(name = "consumer_type_registration_values", description = REGISTRATION_DESCRIPTION)
    public enum ConsumerTypeStatusValues {
        REGISTERED, DEREGISTERED
    }

    private static final String REGISTRATION_DESCRIPTION = "Allowed values: <br/>" //
        + "REGISTERED: the information type has been registered <br/>" //
        + "DEREGISTERED: the information type has been removed";

    @Schema(name = "status", description = REGISTRATION_DESCRIPTION, required = true)
    @SerializedName("status")
    @JsonProperty(value = "status", required = true)
    public ConsumerTypeStatusValues state;

    public ConsumerTypeRegistrationInfo(Object jobDataSchema, ConsumerTypeStatusValues state, String infoTypeId) {
        this.jobDataSchema = jobDataSchema;
        this.state = state;
        this.infoTypeId = infoTypeId;
    }

    public ConsumerTypeRegistrationInfo() {
    }

}
