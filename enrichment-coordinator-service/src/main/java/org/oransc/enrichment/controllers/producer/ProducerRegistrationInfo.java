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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.util.Collection;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "producer_registration_info", description = "Information for an EI producer")
public class ProducerRegistrationInfo {

    @Gson.TypeAdapters
    @ApiModel(value = "producer_ei_type_registration_info", description = "Information for an EI type")
    public static class ProducerEiTypeRegistrationInfo {

        @ApiModelProperty(value = "EI type identity", required = true)
        @SerializedName("ei_type_identity")
        @JsonProperty(value = "ei_type_identity", required = true)
        public String eiTypeId;

        @ApiModelProperty(value = "Json schema for the job data")
        @SerializedName("ei_job_data_schema")
        @JsonProperty("ei_job_data_schema")
        public Object jobDataSchema;

        public ProducerEiTypeRegistrationInfo(Object jobDataSchema, String eiTypeId) {
            this.jobDataSchema = jobDataSchema;
            this.eiTypeId = eiTypeId;
        }

        public ProducerEiTypeRegistrationInfo() {
        }
    }

    @ApiModelProperty(value = "Supported EI types", required = true)
    @SerializedName("supported_ei_types")
    @JsonProperty(value = "supported_ei_types", required = true)
    public Collection<ProducerEiTypeRegistrationInfo> types;

    @ApiModelProperty(value = "callback for job creation", required = true)
    @SerializedName("ei_job_creation_callback_url")
    @JsonProperty(value = "ei_job_creation_callback_url", required = true)
    public String jobCreationCallbackUrl;

    @ApiModelProperty(value = "callback for job deletion", required = true)
    @SerializedName("ei_job_deletion_callback_url")
    @JsonProperty(value = "ei_job_deletion_callback_url", required = true)
    public String jobDeletionCallbackUrl;

    @ApiModelProperty(value = "callback for producer supervision", required = true)
    @SerializedName("ei_producer_supervision_callback_url")
    @JsonProperty(value = "ei_producer_supervision_callback_url", required = true)
    public String producerSupervisionCallbackUrl;

    public ProducerRegistrationInfo(Collection<ProducerEiTypeRegistrationInfo> types, String jobCreationCallbackUrl,
        String jobDeletionCallbackUrl, String producerSupervisionCallbackUrl) {
        this.types = types;
        this.jobCreationCallbackUrl = jobCreationCallbackUrl;
        this.jobDeletionCallbackUrl = jobDeletionCallbackUrl;
        this.producerSupervisionCallbackUrl = producerSupervisionCallbackUrl;
    }

    public ProducerRegistrationInfo() {
    }

}
