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

package org.oransc.enrichment.controllers.r1consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "Job", description = "Information for an Enrichment Information Job")
public class ConsumerJobInfo {

    @Schema(name = "infoTypeId", description = "Information type Idenitifier of the subscription job", required = true)
    @SerializedName("infoTypeId")
    @JsonProperty(value = "infoTypeId", required = true)
    public String infoTypeId;

    @Schema(name = "jobOwner", description = "Identity of the owner of the job", required = true)
    @SerializedName("jobOwner")
    @JsonProperty(value = "jobOwner", required = true)
    public String owner;

    @Schema(name = "jobDefinition", description = "Information type specific job data", required = true)
    @SerializedName("jobDefinition")
    @JsonProperty(value = "jobDefinition", required = true)
    public Object jobDefinition;

    @Schema(name = "jobResultUri", description = "The target URI of the subscribed information", required = true)
    @SerializedName("jobResultUri")
    @JsonProperty(value = "jobResultUri", required = true)
    public String jobResultUri;

    @Schema(
        name = "statusNotificationUri",
        description = "The target of Information subscription job status notifications",
        required = false)
    @SerializedName("jobStatusNotificationUri")
    @JsonProperty(value = "jobStatusNotificationUri", required = false)
    public String statusNotificationUri;

    public ConsumerJobInfo() {
    }

    public ConsumerJobInfo(String eiTypeId, Object jobData, String owner, String targetUri,
        String statusNotificationUri) {
        this.infoTypeId = eiTypeId;
        this.jobDefinition = jobData;
        this.owner = owner;
        this.jobResultUri = targetUri;
        this.statusNotificationUri = statusNotificationUri;
    }
}
