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

package com.demo.producer.repository;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

@ToString
public class Job {
    @Builder
    public static class Parameters {

        @Builder
        @EqualsAndHashCode
        public static class KafkaDeliveryInfo {
            @Getter
            private String topic;

            @Getter
            private String bootStrapServers;

            @JsonProperty(value = "numberOfMessages")
            @Getter
            private int numberOfMessages;
        }

        @Getter
        private KafkaDeliveryInfo deliveryInfo;
    }

    @Getter
    private final String id;

    @Getter
    private final InfoType type;

    @Getter
    private final String owner;

    @Getter
    private final Parameters parameters;

    public Job(String id, InfoType type, String owner, Parameters parameters) {
        this.id = id;
        this.type = type;
        this.owner = owner;
        this.parameters = parameters;
    }
}
