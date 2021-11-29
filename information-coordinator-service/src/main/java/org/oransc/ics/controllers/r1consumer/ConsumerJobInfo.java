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

package org.oransc.ics.controllers.r1consumer;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import io.swagger.v3.oas.annotations.media.Schema;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@Schema(name = "consumer_job", description = "Information for an Information Job")
public class ConsumerJobInfo {

    @Schema(
        name = "info_type_id",
        description = "Information type Idenitifier of the subscription job",
        required = true)
    @SerializedName("info_type_id")
    @JsonProperty(value = "info_type_id", required = true)
    public String infoTypeId = "";

    @Schema(name = "job_owner", description = "Identity of the owner of the job", required = true)
    @SerializedName("job_owner")
    @JsonProperty(value = "job_owner", required = true)
    public String owner = "";

    @Schema(name = "job_definition", description = "Information type specific job data", required = true)
    @SerializedName("job_definition")
    @JsonProperty(value = "job_definition", required = true)
    public Object jobDefinition;

    @Schema(name = "job_result_uri", description = "The target URI of the subscribed information", required = true)
    @SerializedName("job_result_uri")
    @JsonProperty(value = "job_result_uri", required = true)
    public String jobResultUri = "";

    @Schema(
        name = "status_notification_uri",
        description = "The target of Information subscription job status notifications",
        required = false)
    @SerializedName("status_notification_uri")
    @JsonProperty(value = "status_notification_uri", required = false)
    public String statusNotificationUri = "";

    public ConsumerJobInfo() {
    }

    public ConsumerJobInfo(String infoTypeId, Object jobData, String owner, String targetUri,
        String statusNotificationUri) {
        this.infoTypeId = infoTypeId;
        this.jobDefinition = jobData;
        this.owner = owner;
        this.jobResultUri = targetUri;
        this.statusNotificationUri = statusNotificationUri;
    }
}
