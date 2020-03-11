/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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

package org.oransc.policyagent.controllers;

import com.google.gson.annotations.SerializedName;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "ServiceRegistrationInfo")
public class ServiceRegistrationInfo {

    @ApiModelProperty(value = "identity of the service", required = true, allowEmptyValue = false)
    @SerializedName(value = "serviceName", alternate = {"name"})

    public String serviceName = "";

    @ApiModelProperty(
        value = "keep alive interval for the service. This is a heartbeat supervision of the service, "
            + "which in regular intevals must invoke a 'keepAlive' REST call. "
            + "When a service does not invoke this call within the given time, it is considered unavailble. "
            + "An unavailable service will be automatically deregistered and its policies will be deleted. "
            + "Value 0 means no timeout supervision." + " When a ")
    @SerializedName("keepAliveIntervalSeconds")
    public long keepAliveIntervalSeconds = 0;

    @ApiModelProperty(value = "callback for notifying of RIC recovery", required = false, allowEmptyValue = true)
    @SerializedName("callbackUrl")
    public String callbackUrl = "";

    public ServiceRegistrationInfo() {
    }

    public ServiceRegistrationInfo(String name, long keepAliveIntervalSeconds, String callbackUrl) {
        this.serviceName = name;
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
        this.callbackUrl = callbackUrl;
    }

}
