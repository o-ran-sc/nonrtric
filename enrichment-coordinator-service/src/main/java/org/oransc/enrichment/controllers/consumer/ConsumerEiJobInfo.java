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

package org.oransc.enrichment.controllers.consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "ei_job_info", description = "Information for a Enrichment Information Job")
public class ConsumerEiJobInfo {

    @ApiModelProperty(value = "Identity of the owner of the job", required = true)
    @SerializedName("job_owner")
    @JsonProperty(value = "job_owner", required = true)
    public String owner;

    @ApiModelProperty(value = "EI Type specific job data", required = true)
    @SerializedName("job_data")
    @JsonProperty(value = "job_data", required = true)
    public Object jobData;

    public ConsumerEiJobInfo() {
    }

    public ConsumerEiJobInfo(Object jobData, String owner) {
        this.jobData = jobData;
        this.owner = owner;
    }
}
