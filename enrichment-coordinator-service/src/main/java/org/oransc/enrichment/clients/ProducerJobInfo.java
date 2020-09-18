/*-
 * ========================LICENSE_START=================================
 * ONAP : ccsdk oran
 * ======================================================================
 * Copyright (C) 2020 Nordix Foundation. All rights reserved.
 * ======================================================================
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

package org.oransc.enrichment.clients;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.immutables.gson.Gson;
import org.oransc.enrichment.repository.EiJob;
import org.oransc.enrichment.repository.EiType;

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

    public ProducerJobInfo(Object jobData, String id, String typeId) {
        this.id = id;
        this.jobData = jobData;
        this.typeId = typeId;
    }

    public ProducerJobInfo(Object jobData, EiJob job, EiType type) {
        this(jobData, job.id(), type.getId());
    }

    public ProducerJobInfo() {
    }

}
