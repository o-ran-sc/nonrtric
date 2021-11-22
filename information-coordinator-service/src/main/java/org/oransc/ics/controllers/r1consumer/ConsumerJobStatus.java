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

package org.oransc.ics.controllers.r1consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "consumer_job_status", description = "Status for an Information Job")
public class ConsumerJobStatus {

    @Gson.TypeAdapters
    @Schema(name = "info_job_status_values", description = OPERATIONAL_STATE_DESCRIPTION)
    public enum InfoJobStatusValues {
        ENABLED, DISABLED
    }

    private static final String OPERATIONAL_STATE_DESCRIPTION = "Allowed values: <br/>" //
        + "ENABLED: the A1-Information producer is able to deliver result for the Information Job <br/>" //
        + "DISABLED: the A1-Information producer is unable to deliver result for the Information Job";

    private static final String PRODUCERS_DESCRIPTION = "An array of all registered Information Producer Identifiers.";

    @Schema(name = "info_job_status", description = OPERATIONAL_STATE_DESCRIPTION, required = true)
    @SerializedName("info_job_status")
    @JsonProperty(value = "info_job_status", required = true)
    public InfoJobStatusValues state;

    @Schema(name = "producers", description = PRODUCERS_DESCRIPTION, required = true)
    @SerializedName("producers")
    @JsonProperty(value = "producers", required = true)
    public Collection<String> producers;

    public ConsumerJobStatus() {
    }

    public ConsumerJobStatus(InfoJobStatusValues state, Collection<String> producers) {
        this.state = state;
        this.producers = producers;
    }

}
