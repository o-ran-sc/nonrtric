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

import org.immutables.gson.Gson;
import org.oransc.enrichment.repository.EiJob;

@Gson.TypeAdapters
@Schema(
    name = "producer_ei_job_request",
    description = "The body of the EI producer callbacks for EI job creation and deletion")
public class ProducerJobInfo {

    @Schema(name = "ei_job_identity", description = "Idenitity of the EI job", required = true)
    @SerializedName("ei_job_identity")
    @JsonProperty("ei_job_identity")
    public String id;

    @Schema(name = "ei_type_identity", description = "Type idenitity for the job")
    @SerializedName("ei_type_identity")
    @JsonProperty("ei_type_identity")
    public String typeId;

    @Schema(name = "ei_job_data", description = "Json for the job data")
    @SerializedName("ei_job_data")
    @JsonProperty("ei_job_data")
    public Object jobData;

    @Schema(name = "target_uri", description = "URI for the target of the EI")
    @SerializedName("target_uri")
    @JsonProperty("target_uri")
    public String targetUri;

    @Schema(name = "owner", description = "The owner of the job")
    @SerializedName("owner")
    @JsonProperty("owner")
    public String owner;

    @Schema(name = "last_updated", description = "The time when the job was last updated or created (ISO-8601)")
    @SerializedName("last_updated")
    @JsonProperty("last_updated")
    public String lastUpdated;

    public ProducerJobInfo(Object jobData, String id, String typeId, String targetUri, String owner,
        String lastUpdated) {
        this.id = id;
        this.jobData = jobData;
        this.typeId = typeId;
        this.targetUri = targetUri;
        this.owner = owner;
        this.lastUpdated = lastUpdated;
    }

    public ProducerJobInfo(EiJob job) {
        this(job.getJobData(), job.getId(), job.getTypeId(), job.getTargetUrl(), job.getOwner(), job.getLastUpdated());
    }

    public ProducerJobInfo() {
    }

}
