/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2023 Nordix Foundation
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

package org.oran.pmlog.configuration;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

public class ConsumerJobInfo {

    @Getter
    public static class PmFilterData {

        public static class MeasTypeSpec {
            @Setter
            @Getter
            @Expose
            String measuredObjClass;

            @Getter
            @Expose
            final Set<String> measTypes = new HashSet<>();
        }

        @Expose
        final Set<String> sourceNames = new HashSet<>();

        @Expose
        final Set<String> measObjInstIds = new HashSet<>();

        @Expose
        final Collection<MeasTypeSpec> measTypeSpecs = new ArrayList<>();

        @Expose
        final Set<String> measuredEntityDns = new HashSet<>();
    }

    @Builder
    public static class KafkaDeliveryInfo {
        @Getter
        @Expose
        private String topic;

        @Getter
        @Expose
        private String bootStrapServers;
    }

    @Builder
    public static class PmJobParameters {

        public static final String PM_FILTER_TYPE = "pmdata";

        @Getter
        @Builder.Default
        @Expose
        private String filterType = PM_FILTER_TYPE;

        @Getter
        @Expose
        private PmFilterData filter;

        @Getter
        @Expose
        private KafkaDeliveryInfo deliveryInfo;

    }

    @SerializedName("info_type_id")
    @Expose
    public String infoTypeId = "";

    @SerializedName("job_owner")
    @Expose
    public String owner = "";

    @SerializedName("job_definition")
    @Expose
    public PmJobParameters jobDefinition;

    public ConsumerJobInfo() {}

    public ConsumerJobInfo(String infoTypeId, PmJobParameters jobData, String owner) {
        this.infoTypeId = infoTypeId;
        this.jobDefinition = jobData;
        this.owner = owner;
    }
}
