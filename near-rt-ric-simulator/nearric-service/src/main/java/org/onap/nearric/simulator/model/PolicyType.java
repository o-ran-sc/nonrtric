/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.onap.nearric.simulator.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.oransc.ric.a1med.api.model.PolicyTypeSchema;

public class PolicyType implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 8719589957000170141L;
	private Integer policyTypeId;
	private PolicyTypeSchema policyTypeSchema;
	private HashMap<String, PolicyInstance> policyInstances = new HashMap<String, PolicyInstance>();

	public PolicyType(Integer policyTypeId, PolicyTypeSchema policyTypeSchema) {
		this.policyTypeId = policyTypeId;
		this.policyTypeSchema = policyTypeSchema;
	}

	public int getNumberInstances() {
		return policyInstances.size();
	}

	public PolicyInstance getInstance(String policyInstanceId) {
		return policyInstances.get(policyInstanceId);
	}

	public void delete(String policyInstanceId) {
		policyInstances.remove(policyInstanceId);
	}

	public PolicyTypeSchema getSchema() {
		return policyTypeSchema;
	}

	public void createReplaceInstance(String policyTypeId, PolicyInstance policyInstance) {
		policyInstances.put(policyTypeId, policyInstance);
	}

	public Set<String> getInstances() {
		return policyInstances.keySet();
	}

	public Integer getTypeId() {
		return policyTypeId;
	}

}