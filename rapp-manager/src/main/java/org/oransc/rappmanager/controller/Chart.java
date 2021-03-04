/*-
 * ========================LICENSE_START=================================
 * Copyright (C) 2021 Nordix Foundation. All rights reserved.
 * ======================================================================
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

package org.oransc.rappmanager.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import org.immutables.gson.Gson;
import org.springframework.web.multipart.MultipartFile;

@Gson.TypeAdapters
@ApiModel(value = "Chart", description = "Chart Information")
public class Chart {

    public String name;
    public String version;
    public MultipartFile chart;
    @JsonProperty(required = false, defaultValue = "false")
    public boolean upgrade;

    Chart() {}

}
