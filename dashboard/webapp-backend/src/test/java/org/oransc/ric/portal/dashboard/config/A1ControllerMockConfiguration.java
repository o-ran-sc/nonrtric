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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.oransc.ric.a1controller.client.api.A1ControllerApi;
import org.oransc.ric.a1controller.client.invoker.ApiClient;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidPISchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidSchema;
import org.oransc.ric.a1controller.client.model.OutputCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputCodeSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputDescNamePTCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputDescNamePTCodeSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPICodeSchema;
import org.oransc.ric.a1controller.client.model.OutputPICodeSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPIidsListCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputPIidsListCodeSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPTidsListCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputPTidsListCodeSchemaOutput;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;

/**
 * Creates a mock implementation of the A1 controller client API.
 */
@Profile("test")
@Configuration
public class A1ControllerMockConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// A "control" is an element in the XApp descriptor
	public static final String AC_CONTROL_NAME = "admission_control_policy";

	// Simulate remote method delay for UI testing
	@Value("${mock.config.delay:0}")
	private int delayMs;

	public A1ControllerMockConfiguration() {
		logger.info("Configuring mock A1 Mediator");
	}

	private ApiClient apiClient() {
		ApiClient mockClient = mock(ApiClient.class);
		when(mockClient.getStatusCode()).thenReturn(HttpStatus.OK);
		return mockClient;
	}

	@Bean
	// Use the same name as regular configuration
	public A1ControllerApi a1ControllerApi() {
		ApiClient apiClient = apiClient();
		A1ControllerApi mockApi = mock(A1ControllerApi.class);

		when(mockApi.getApiClient()).thenReturn(apiClient);

		doAnswer(inv -> {
			if (delayMs > 0) {
				logger.debug("a1ControllerGetHandler sleeping {}", delayMs);
				Thread.sleep(delayMs);
			}
			List<Integer> types = database.getTypes();
			OutputPTidsListCodeSchemaOutput output = new OutputPTidsListCodeSchemaOutput();
			output.setPolicyTypeIdList(types);
			output.setCode(String.valueOf(HttpStatus.OK.value()));
			OutputPTidsListCodeSchema outputSchema = new OutputPTidsListCodeSchema();
			outputSchema.setOutput(output);
			return outputSchema;
		}).when(mockApi).a1ControllerGetAllPolicyTypes(any(InputNRRidSchema.class));

		doAnswer(inv -> {
			if (delayMs > 0) {
				logger.debug("a1ControllerGetPolicyType sleeping {}", delayMs);
				Thread.sleep(delayMs);
			}
			InputNRRidPTidSchema input = inv.<InputNRRidPTidSchema>getArgument(0);
			PolicyType policyType = database.getPolicyType(input.getInput().getPolicyTypeId());
			OutputDescNamePTCodeSchemaOutput type = new OutputDescNamePTCodeSchemaOutput();
			type.setName(policyType.getName());
			type.setPolicyType(database.normalize(policyType.getSchema()));
			type.setCode(String.valueOf(HttpStatus.OK.value()));
			OutputDescNamePTCodeSchema outputSchema = new OutputDescNamePTCodeSchema();
			outputSchema.setOutput(type);
			return outputSchema;
		}).when(mockApi).a1ControllerGetPolicyType(any(InputNRRidPTidSchema.class));

		doAnswer(inv -> {
			if (delayMs > 0) {
				logger.debug("a1ControllerGetHandler sleeping {}", delayMs);
				Thread.sleep(delayMs);
			}
			InputNRRidPTidSchema input = inv.<InputNRRidPTidSchema>getArgument(0);
			List<String> instances = database.getInstances(Optional.of(input.getInput().getPolicyTypeId()));
			OutputPIidsListCodeSchemaOutput instancesOutput = new OutputPIidsListCodeSchemaOutput();
			instancesOutput.setPolicyInstanceIdList(instances);
			instancesOutput.setCode(String.valueOf(HttpStatus.OK.value()));
			OutputPIidsListCodeSchema outputSchema = new OutputPIidsListCodeSchema();
			outputSchema.setOutput(instancesOutput);
			return outputSchema;
		}).when(mockApi).a1ControllerGetAllInstancesForType(any(InputNRRidPTidSchema.class));

		doAnswer(inv -> {
			if (delayMs > 0) {
				logger.debug("a1ControllerGetHandler sleeping {}", delayMs);
				Thread.sleep(delayMs);
			}
			InputNRRidPTidPIidSchema input = inv.<InputNRRidPTidPIidSchema>getArgument(0);
			Integer polcyTypeId = input.getInput().getPolicyTypeId();
			String instanceId = input.getInput().getPolicyInstanceId();
			String instance = database.normalize(database.getInstance(polcyTypeId, instanceId));
			OutputPICodeSchemaOutput instanceOutput = new OutputPICodeSchemaOutput();
			instanceOutput.setPolicyInstance(instance);
			instanceOutput.setCode(String.valueOf(HttpStatus.OK.value()));
			OutputPICodeSchema outputSchema = new OutputPICodeSchema();
			outputSchema.setOutput(instanceOutput);
			return outputSchema;
		}).when(mockApi).a1ControllerGetPolicyInstance(any(InputNRRidPTidPIidSchema.class));

		doAnswer(inv -> {
			if (delayMs > 0) {
				logger.debug("a1ControllerGetHandler sleeping {}", delayMs);
				Thread.sleep(delayMs);
			}
			InputNRRidPTidPIidPISchema input = inv.<InputNRRidPTidPIidPISchema>getArgument(0);
			Integer polcyTypeId = input.getInput().getPolicyTypeId();
			String instanceId = input.getInput().getPolicyInstanceId();
			String instance = input.getInput().getPolicyInstance();
			database.putInstance(polcyTypeId, instanceId, instance);
			OutputCodeSchemaOutput outputCodeSchemaOutput = new OutputCodeSchemaOutput();
			outputCodeSchemaOutput.setCode(String.valueOf(HttpStatus.CREATED.value()));
			OutputCodeSchema outputCodeSchema = new OutputCodeSchema();
			outputCodeSchema.setOutput(outputCodeSchemaOutput);
			return outputCodeSchema;
		}).when(mockApi).a1ControllerCreatePolicyInstance(any(InputNRRidPTidPIidPISchema.class));

		doAnswer(inv -> {
			if (delayMs > 0) {
				logger.debug("a1ControllerGetHandler sleeping {}", delayMs);
				Thread.sleep(delayMs);
			}
			InputNRRidPTidPIidSchema input = inv.<InputNRRidPTidPIidSchema>getArgument(0);
			Integer polcyTypeId = input.getInput().getPolicyTypeId();
			String instanceId = input.getInput().getPolicyInstanceId();
			database.deleteInstance(polcyTypeId, instanceId);
			OutputCodeSchemaOutput outputCodeSchemaOutput = new OutputCodeSchemaOutput();
			outputCodeSchemaOutput.setCode(String.valueOf(HttpStatus.NO_CONTENT.value()));
			OutputCodeSchema outputCodeSchema = new OutputCodeSchema();
			outputCodeSchema.setOutput(outputCodeSchemaOutput);
			return outputCodeSchema;
		}).when(mockApi).a1ControllerDeletePolicyInstance(any(InputNRRidPTidPIidSchema.class));

		return mockApi;
	}

	class Database {

		public class PolicyException extends Exception {

			private static final long serialVersionUID = 1L;

			public PolicyException(String message) {
				super(message);
				System.out.println("**** Exception " + message);
			}
		}

		private class PolicyTypeHolder {
			PolicyTypeHolder(PolicyType pt) {
				this.policyType = pt;
			}

			String getInstance(String instanceId) throws PolicyException {
				String instance = instances.get(instanceId);
				if (instance == null) {
					throw new PolicyException("Instance not found: " + instanceId);
				}
				return instance;
			}

			PolicyType getPolicyType() {
				return policyType;
			}

			void putInstance(String id, String data) {
				instances.put(id, data);
			}

			void deleteInstance(String id) {
				instances.remove(id);
			}

			List<String> getInstances() {
				return new ArrayList<>(instances.keySet());
			}

			private final PolicyType policyType;
			private Map<String, String> instances = new HashMap<>();
		}

		Database() {
			String schema = getStringFromFile("anr-policy-schema.json");
			PolicyType policy = new PolicyType(1, "ANR", schema);
			types.put(1, new PolicyTypeHolder(policy));

			schema = getStringFromFile("demo-policy-schema-1.json");
			policy = new PolicyType(2, "type2", schema);
			types.put(2, new PolicyTypeHolder(policy));

			schema = getStringFromFile("demo-policy-schema-2.json");
			policy = new PolicyType(3, "type3", schema);
			types.put(3, new PolicyTypeHolder(policy));

			schema = getStringFromFile("demo-policy-schema-3.json");
			policy = new PolicyType(4, "type4", schema);
			types.put(4, new PolicyTypeHolder(policy));
			try {
				putInstance(1, "ANR-1", getStringFromFile("anr-policy-instance.json"));
			} catch (JsonProcessingException | PolicyException e) {
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

		void putInstance(Integer typeId, String instanceId, String instanceData)
				throws JsonProcessingException, PolicyException {
			PolicyTypeHolder type = getTypeHolder(typeId);
			type.putInstance(instanceId, instanceData);
		}

		void deleteInstance(Integer typeId, String instanceId) throws JsonProcessingException, PolicyException {
			PolicyTypeHolder type = getTypeHolder(typeId);
			type.deleteInstance(instanceId);
		}

		String getInstance(Integer typeId, String instanceId) throws JsonProcessingException, PolicyException {
			return getTypeHolder(typeId).getInstance(instanceId);
		}

		List<Integer> getTypes() {
			return new ArrayList<>(types.keySet());
		}

		List<String> getInstances(Optional<Integer> typeId) throws PolicyException {
			if (typeId.isPresent()) {
				return getTypeHolder(typeId.get()).getInstances();
			} else {
				Set<String> res = new HashSet<String>();
				for (Iterator<PolicyTypeHolder> i = types.values().iterator(); i.hasNext();) {
					res.addAll(i.next().getInstances());
				}
				return new ArrayList<>(res);
			}
		}

		private PolicyTypeHolder getTypeHolder(Integer typeId) throws PolicyException {
			PolicyTypeHolder typeHolder = types.get(typeId);
			if (typeHolder == null) {
				throw new PolicyException("Type not found: " + typeId);
			}
			return typeHolder;
		}

		private PolicyType getPolicyType(Integer typeId) throws PolicyException {
			PolicyTypeHolder typeHolder = getTypeHolder(typeId);
			return typeHolder.getPolicyType();
		}

		private Map<Integer, PolicyTypeHolder> types = new HashMap<>();

	}

	private final Database database = new Database();
}
