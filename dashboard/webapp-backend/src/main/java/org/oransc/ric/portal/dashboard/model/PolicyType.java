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
package org.oransc.ric.portal.dashboard.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PolicyType {

	@JsonProperty("policy_type_id")
	String policyTypeId;

	@JsonProperty("name")
	String name;

	@JsonProperty("schema")
	String schema;

	public PolicyType(String policyId, String name, String schema) {
		this.policyTypeId = policyId;
		this.name = name;
		this.schema = schema;
	}

	public PolicyType(String name, String schema) {
		this.policyTypeId = name;
		this.name = name;
		this.schema = schema;
	}

	public String getPolicyTypeId() {
		return policyTypeId;
	}

	public void setPolicyTypeId(String policyTypeId) {
		this.policyTypeId = policyTypeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getSchema() {
		return schema;
	}

	public void setSchema(String schema) {
		this.schema = schema;
	}

	@Override
	public String toString() {
		return "[policy_type_id:" + policyTypeId + ", name:" + name + ", schema:" + schema + "]";
	}
}
