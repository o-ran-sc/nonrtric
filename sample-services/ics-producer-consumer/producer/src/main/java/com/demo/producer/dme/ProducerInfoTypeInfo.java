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

public class ProducerInfoTypeInfo {

    @SerializedName("info_job_data_schema")
    @JsonProperty(value = "info_job_data_schema", required = true)
    public Object jobDataSchema;

    @SerializedName("info_type_information")
    @JsonProperty(value = "info_type_information", required = true)
    public Object typeSpecificInformation;

    public ProducerInfoTypeInfo(Object jobDataSchema, Object typeSpecificInformation) {
        this.jobDataSchema = jobDataSchema;
        this.typeSpecificInformation = typeSpecificInformation;
    }

    public ProducerInfoTypeInfo() {
    }

}
