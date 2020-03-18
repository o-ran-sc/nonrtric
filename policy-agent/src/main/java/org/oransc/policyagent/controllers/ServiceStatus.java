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

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import org.immutables.gson.Gson;

@Gson.TypeAdapters
@ApiModel(value = "ServiceStatus")
public class ServiceStatus {

    @ApiModelProperty(value = "identity of the service")
    public final String serviceName;

    @ApiModelProperty(value = "policy keep alive timeout")
    public final long keepAliveIntervalSeconds;

    @ApiModelProperty(value = "time since last invocation by the service")
    public final long timeSinceLastActivitySeconds;

    @ApiModelProperty(value = "callback for notifying of RIC synchronization")
    public String callbackUrl;

    ServiceStatus(String name, long keepAliveIntervalSeconds, long timeSincePingSeconds, String callbackUrl) {
        this.serviceName = name;
        this.keepAliveIntervalSeconds = keepAliveIntervalSeconds;
        this.timeSinceLastActivitySeconds = timeSincePingSeconds;
        this.callbackUrl = callbackUrl;
    }

}
