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

package com.demo.consumer.dme;

import com.google.gson.annotations.SerializedName;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

import com.demo.consumer.repository.Job.Parameters.KafkaDeliveryInfo;
import com.google.gson.Gson;

@Data
public class JobDataSchema {

    public enum InfoJobStatusValues {
        REGISTERED, UNREGISTERED
    }
    @SerializedName("info_type_id")
    private String info_type_id;
    @SerializedName("job_data_schema")
    private DataSchema job_data_schema;
    @SerializedName("status")
    private InfoJobStatusValues status;

    @Override
    public String toString() {
        return new Gson().toJson(this);
    }

    @Getter
    @Setter
    public class DataSchema {
        private String title;
        private String description;
        @SerializedName("topic")
        private String topic;
        @SerializedName("bootStrapServers")
        private String bootStrapServers;
    }
}
