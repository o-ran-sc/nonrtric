/*
* ============LICENSE_START=======================================================
* ONAP : SDNC-FEATURES
* ================================================================================
* Copyright 2018 TechMahindra
*=================================================================================
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* ============LICENSE_END=========================================================
*/
package com.onap.sdnc.reports.rest.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigDetailsRestModel {

	@JsonProperty(value = JSONTags.TAG_ROUTER)
	String router;
	
	@JsonProperty(value = JSONTags.TAG_DATE)
	String date;
	
	public ConfigDetailsRestModel() {
		super();
	}
	public String getRouter() {
		return router;
	}
	
	public void setRouter(String router) {
		this.router = router;
	}

	public String getDate() {
		return date;
	}

	public void setDate(String date) {
		this.date = date;
	}
	
}
