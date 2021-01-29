/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2020 Nordix Foundation
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

package org.oransc.enrichment.controllers.producer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Collection;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "producer_registration_info", description = "Information for an EI producer")
public class ProducerRegistrationInfo {

    @Schema(name = "Supported EI type IDs", required = true)
    @SerializedName("supported_ei_types")
    @JsonProperty(value = "supported_ei_types", required = true)
    public Collection<String> supportedTypeIds;

    @Schema(name = "callback for EI job", required = true)
    @SerializedName("ei_job_callback_url")
    @JsonProperty(value = "ei_job_callback_url", required = true)
    public String jobCallbackUrl;

    @Schema(name = "callback for producer supervision", required = true)
    @SerializedName("ei_producer_supervision_callback_url")
    @JsonProperty(value = "ei_producer_supervision_callback_url", required = true)
    public String producerSupervisionCallbackUrl;

    public ProducerRegistrationInfo(Collection<String> types, String jobCallbackUrl,
        String producerSupervisionCallbackUrl) {
        this.supportedTypeIds = types;
        this.jobCallbackUrl = jobCallbackUrl;
        this.producerSupervisionCallbackUrl = producerSupervisionCallbackUrl;
    }

    public ProducerRegistrationInfo() {
    }

}
