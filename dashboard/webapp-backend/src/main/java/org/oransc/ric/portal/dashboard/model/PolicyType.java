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
	Integer policyTypeId;

	@JsonProperty("name")
	String name;

	@JsonProperty("description")
	String description;

	@JsonProperty("create_schema")
	String createSchema;

	public PolicyType(Integer policyId, String name, String description, String createSchema) {
		this.policyTypeId = policyId;
		this.name = name;
		this.description = description;
		this.createSchema = createSchema;
	}

	public Integer getPolicyTypeId() {
		return policyTypeId;
	}

	public void setPolicyTypeId(Integer policyTypeId) {
		this.policyTypeId = policyTypeId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCreateSchema() {
		return createSchema;
	}

	public void setCreateSchema(String createSchema) {
		this.createSchema = createSchema;
	}

	@Override
	public String toString() {
		return "[policy_type_id:" + policyTypeId +  ", name:" + name + ", description:" + description + ", create_schema:" + createSchema + "]";
	}
}
