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
package org.oransc.ric.portal.dashboard.config;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.oransc.ric.portal.dashboard.model.ImmutablePolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInfo;
import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.oransc.ric.portal.dashboard.policyagentapi.PolicyAgentApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestClientException;

/**
 * Creates a mock implementation of the policy controller client API.
 */
@TestConfiguration
public class PolicyControllerMockConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	@Bean
	public PolicyAgentApi policyAgentApi() {
		MockPolicyAgentApi apiClient = new MockPolicyAgentApi();
		return apiClient;
	}

	class MockPolicyAgentApi implements PolicyAgentApi {
		private final Database database = new Database();

		@Override
		public String getPolicyInstance(String id) throws RestClientException {
			return database.getInstance(id);
		}

		@Override
		public void putPolicy(String policyTypeIdString, String policyInstanceId, String json)
				throws RestClientException {
			database.putInstance(policyTypeIdString, policyInstanceId, json);
		}

		@Override
		public void deletePolicy(String policyInstanceId) throws RestClientException {
			database.deleteInstance(policyInstanceId);
		}

		@Override
		public PolicyTypes getAllPolicyTypes() throws RestClientException {
			PolicyTypes result = new PolicyTypes();
			result.addAll(database.getTypes());
			return result;
		}

		@Override
		public PolicyInstances getPolicyInstancesForType(String type) {
			PolicyInstances result = new PolicyInstances();
			List<PolicyInfo> inst = database.getInstances(Optional.of(type));
			result.addAll(inst);
			return result;
		}

	}

	class Database {

		Database() {
			String schema = getStringFromFile("anr-policy-schema.json");
			PolicyType policy = new PolicyType("ANR", schema);
			types.put("ANR", policy);

			schema = getStringFromFile("demo-policy-schema-1.json");
			policy = new PolicyType("type2", schema);
			types.put("type2", policy);

			schema = getStringFromFile("demo-policy-schema-2.json");
			policy = new PolicyType("type3", schema);
			types.put("type3", policy);

			schema = getStringFromFile("demo-policy-schema-3.json");
			policy = new PolicyType("type4", schema);
			types.put("type4", policy);
			try {
				putInstance("ANR", "ANR-1", getStringFromFile("anr-policy-instance.json"));
			} catch (Exception e) {
				// Nothing
			}
		}

		private String getStringFromFile(String path) {
			try {
				InputStream inputStream = MethodHandles.lookup().lookupClass().getClassLoader()
						.getResourceAsStream(path);
				return new BufferedReader(new InputStreamReader(inputStream)).lines().collect(Collectors.joining("\n"));
			} catch (Exception e) {
				logger.error("Cannot read file :" + path, e);
				return "";
			}
		}

		String normalize(String str) {
			return str.replace('\n', ' ');
		}

		private String getTimeStampUTC() {
			return java.time.Instant.now().toString();
		}

		void putInstance(String typeId, String instanceId, String instanceData) {
			PolicyInfo i = ImmutablePolicyInfo.builder().json(instanceData).lastModified(getTimeStampUTC())
					.id(instanceId).ric("ricXX").service("service").type(typeId).build();
			instances.put(instanceId, i);
		}

		public void deleteInstance(String instanceId) {
			instances.remove(instanceId);
		}

		String getInstance(String id) throws RestClientException {
			PolicyInfo i = instances.get(id);
			if (i == null) {
				throw new RestClientException("Type not found: " + id);
			}
			return i.json();
		}

		public Collection<PolicyType> getTypes() {
			return types.values();
		}

		public List<PolicyInfo> getInstances(Optional<String> typeId) {
			ArrayList<PolicyInfo> result = new ArrayList<>();
			for (PolicyInfo i : instances.values()) {
				if (typeId.isPresent()) {
					if (i.type().equals(typeId.get())) {
						result.add(i);
					}

				} else {
					result.add(i);
				}
			}
			return result;
		}

		private Map<String, PolicyType> types = new HashMap<>();
		private Map<String, PolicyInfo> instances = new HashMap<>();

	}

}
