/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2021 Nordix Foundation
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
import lombok.EqualsAndHashCode;

import org.immutables.gson.Gson;

@EqualsAndHashCode
@Gson.TypeAdapters
@Schema(name = "consumer_type_subscription_info", description = "Information for an information type subscription")
public class ConsumerTypeSubscriptionInfo {

    @Schema(name = "status_result_uri", description = "The target URI of the subscribed information", required = true)
    @SerializedName("status_result_uri")
    @JsonProperty(value = "status_result_uri", required = true)
    public String statusResultUri = "";

    @Schema(name = "owner", description = "Identity of the owner of the subscription", required = true)
    @SerializedName("owner")
    @JsonProperty(value = "owner", required = true)
    public String owner = "";

    public ConsumerTypeSubscriptionInfo() {
    }

    public ConsumerTypeSubscriptionInfo(String statusResultUri, String owner) {
        this.statusResultUri = statusResultUri;
        this.owner = owner;
    }
}
