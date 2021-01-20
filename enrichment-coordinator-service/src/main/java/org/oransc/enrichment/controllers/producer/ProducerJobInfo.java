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

import org.immutables.gson.Gson;
import org.oransc.enrichment.repository.EiJob;

@Gson.TypeAdapters
@ApiModel(
    value = "producer_ei_job_request",
    description = "The body of the EI producer callbacks for EI job creation and deletion")
public class ProducerJobInfo {

    @ApiModelProperty(value = "Idenitity of the EI job", required = true)
    @SerializedName("ei_job_identity")
    @JsonProperty("ei_job_identity")
    public String id;

    @ApiModelProperty(value = "Type idenitity for the job")
    @SerializedName("ei_type_identity")
    @JsonProperty("ei_type_identity")
    public String typeId;

    @ApiModelProperty(value = "Json for the job data")
    @SerializedName("ei_job_data")
    @JsonProperty("ei_job_data")
    public Object jobData;

    @ApiModelProperty(value = "URI for the target of the EI")
    @SerializedName("target_uri")
    @JsonProperty("target_uri")
    public String targetUri;

    @ApiModelProperty(value = "The owner of the job")
    @SerializedName("owner")
    @JsonProperty("owner")
    public String owner;

    @ApiModelProperty(value = "The time when the job was last updated or created (ISO-8601)")
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
