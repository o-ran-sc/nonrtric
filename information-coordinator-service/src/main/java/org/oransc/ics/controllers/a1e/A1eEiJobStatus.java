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

package org.oransc.ics.controllers.a1e;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "EiJobStatusObject", description = "Status for an EI job")
public class A1eEiJobStatus {

    @Gson.TypeAdapters
    @Schema(name = "EiJobStatusValues", description = OPERATIONAL_STATE_DESCRIPTION)
    public enum EiJobStatusValues {
        ENABLED, DISABLED
    }

    private static final String OPERATIONAL_STATE_DESCRIPTION = "Allowed values for EI job status: <br/>" //
        + "ENABLED: the A1-EI producer is able to deliver EI result for the EI job <br/>" //
        + "DISABLED: the A1-EI producer is unable to deliver EI result for the EI job";

    @Schema(name = "eiJobStatus", description = OPERATIONAL_STATE_DESCRIPTION, required = true)
    @SerializedName("eiJobStatus")
    @JsonProperty(value = "eiJobStatus", required = true)
    public EiJobStatusValues state;

    public A1eEiJobStatus() {
    }

    public A1eEiJobStatus(EiJobStatusValues state) {
        this.state = state;
    }

}
