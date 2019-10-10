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
import com.fasterxml.jackson.annotation.JsonProperty;


  public class PolicyInstance implements Serializable {
    /**
     * 
     */
    private static final long serialVersionUID = -4903894058377154039L;
	private String policyInstanceId;
	private Object jsonObject;
	
	public PolicyInstance(String policyInstanceId, Object jsonObject) {
		this.policyInstanceId = policyInstanceId;
		this.jsonObject = jsonObject;
	}

	public Object getJson() {
		return jsonObject;
	}
	
	public String getInstanceId() {
		return policyInstanceId;
	}
}
