/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * 
 * Copyright (C) 2024 Nordix Foundation
 * 
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

package com.demo.consumer.dme;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

public class ConsumerJobInfo {

    @SerializedName("info_type_id")
    @JsonProperty(value = "info_type_id", required = true)
    public String infoTypeId = "";

    @SerializedName("job_owner")
    @JsonProperty(value = "job_owner", required = true)
    public String owner = "";

    @SerializedName("job_definition")
    @JsonProperty(value = "job_definition", required = true)
    public Object jobDefinition;

    @SerializedName("job_result_uri")
    @JsonProperty(value = "job_result_uri", required = true)
    public String jobResultUri = "";

    @SerializedName("status_notification_uri")
    @JsonProperty(value = "status_notification_uri", required = false)
    public String statusNotificationUri = "";

    public ConsumerJobInfo() {
    }

    public ConsumerJobInfo(String infoTypeId, Object jobData, String owner, String statusNotificationUri) {
        this.infoTypeId = infoTypeId;
        this.jobDefinition = jobData;
        this.owner = owner;
        this.statusNotificationUri = statusNotificationUri;
    }
}
