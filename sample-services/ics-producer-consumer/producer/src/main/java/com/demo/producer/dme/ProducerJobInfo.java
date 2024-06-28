/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 *
 * Copyright (C) 2024: OpenInfra Foundation Europe
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

package com.demo.producer.dme;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.gson.annotations.SerializedName;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
public class ProducerJobInfo {

    @SerializedName("info_job_identity")
    @JsonProperty("info_job_identity")
    public String id = "";

    @SerializedName("info_type_identity")
    @JsonProperty("info_type_identity")
    public String typeId = "";

    @SerializedName("info_job_data")
    @JsonProperty("info_job_data")
    public Object jobData;

    @SerializedName("owner")
    @JsonProperty("owner")
    public String owner = "";

    @SerializedName("last_updated")
    @JsonProperty("last_updated")
    public String lastUpdated = "";

}
