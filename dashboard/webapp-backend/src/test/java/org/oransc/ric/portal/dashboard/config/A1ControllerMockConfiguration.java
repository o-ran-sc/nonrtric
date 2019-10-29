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

import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.oransc.ric.a1controller.client.api.A1ControllerApi;
import org.oransc.ric.a1controller.client.invoker.ApiClient;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidPISchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidSchema;
import org.oransc.ric.a1controller.client.model.OutputDescNamePTSchema;
import org.oransc.ric.a1controller.client.model.OutputDescNamePTSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPISchema;
import org.oransc.ric.a1controller.client.model.OutputPISchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPIidsListSchema;
import org.oransc.ric.a1controller.client.model.OutputPIidsListSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPTidsListSchema;
import org.oransc.ric.a1controller.client.model.OutputPTidsListSchemaOutput;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;

import com.fasterxml.jackson.core.JsonProcessingException;

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
			OutputPTidsListSchemaOutput output = new OutputPTidsListSchemaOutput();
			output.setPolicyTypeIdList(types);
			OutputPTidsListSchema outputSchema = new OutputPTidsListSchema();
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
			OutputDescNamePTSchemaOutput type = new OutputDescNamePTSchemaOutput();
			type.setName(policyType.getName());
			type.setDescription(policyType.getDescription());
			type.setPolicyType(database.normalize(policyType.getCreateSchema()));
			OutputDescNamePTSchema outputSchema = new OutputDescNamePTSchema();
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
			OutputPIidsListSchemaOutput instancesOutput = new OutputPIidsListSchemaOutput();
			instancesOutput.setPolicyInstanceIdList(instances);
			OutputPIidsListSchema outputSchema = new OutputPIidsListSchema();
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
			OutputPISchemaOutput instanceOutput = new OutputPISchemaOutput();
			instanceOutput.setPolicyInstance(instance);
			OutputPISchema outputSchema = new OutputPISchema();
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
			return null;
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
			return null;
		}).when(mockApi).a1ControllerDeletePolicyInstance(any(InputNRRidPTidPIidSchema.class));

		return mockApi;
	}

	class Database {

		private String schema1 = "{\"$schema\": " //
				+ "\"http://json-schema.org/draft-07/schema#\"," //
				+ "\"title\": \"ANR\"," //
				+ "\"description\": \"ANR Neighbour Cell Relation Policy\"," //
				+ "\"type\": \"object\"," //
				+ "\"properties\": " //
				+ "{ \"servingCellNrcgi\": {" //
				+ "\"type\": \"string\"," //
				+ "\"description\" : \"Serving Cell Identifier (NR CGI)\"}," //
				+ "\"neighborCellNrpci\": {" //
				+ "\"type\": \"string\"," //
				+ "\"description\": \"Neighbor Cell Identifier (NR PCI)\"}," //
				+ "\"neighborCellNrcgi\": {" //
				+ "\"type\": \"string\"," //
				+ "\"description\": \"Neighbor Cell Identifier (NR CGI)\"}," //
				+ "\"flagNoHo\": {" //
				+ "\"type\": \"boolean\"," //
				+ "\"description\": \"Flag for HANDOVER NOT ALLOWED\"}," //
				+ "\"flagNoXn\": {" //
				+ "\"type\": \"boolean\"," //
				+ "\"description\": \"Flag for Xn CONNECTION NOT ALLOWED\"}," //
				+ "\"flagNoRemove\": {" //
				+ "\"type\": \"boolean\"," //
				+ "\"description\": \"Flag for DELETION NOT ALLOWED\"}}, " //
				+ "\"required\": [ \"servingCellNrcgi\",\"neighborCellNrpci\",\"neighborCellNrcgi\",\"flagNoHo\",\"flagNoXn\",\"flagNoRemove\" ]}";
		private PolicyType policy1 = new PolicyType(1, "ANR", "ANR Neighbour Cell Relation Policy", schema1);

		private String policyInstance1 = "{\"servingCellNrcgi\": \"Cell1\",\r\n" + //
				"\"neighborCellNrpci\": \"NCell1\",\r\n" + //
				"\"neighborCellNrcgi\": \"Ncell1\",\r\n" + //
				"\"flagNoHo\": true,\r\n" + //
				"\"flagNoXn\": true,\r\n" + //
				"\"flagNoRemove\": true}";

		private String schema2 = "{\n" + "          \"type\": \"object\",\n" + //
				"          \"title\": \"Car\",\n" + //
				"          \"properties\": {\n" + //
				"            \"make\": {\n" + //
				"              \"type\": \"string\",\n" + //
				"              \"enum\": [\n" + //
				"                \"Toyota\",\n" + //
				"                \"BMW\",\n" + //
				"                \"Honda\",\n" + //
				"                \"Ford\",\n" + //
				"                \"Chevy\",\n" + //
				"                \"VW\"\n" + //
				"              ]\n" + //
				"            },\n" + //
				"            \"model\": {\n" + //
				"              \"type\": \"string\"\n" + //
				"            },\n" + //
				"            \"year\": {\n" + //
				"              \"type\": \"integer\",\n" + //
				"              \"enum\": [\n" + //
				"                1995,1996,1997,1998,1999,\n" + //
				"                2000,2001,2002,2003,2004,\n" + //
				"                2005,2006,2007,2008,2009,\n" + //
				"                2010,2011,2012,2013,2014\n" + //
				"              ],\n" + //
				"              \"default\": 2008\n" + //
				"            },\n" + //
				"            \"safety\": {\n" + //
				"              \"type\": \"integer\",\n" + //
				"              \"format\": \"rating\",\n" + //
				"              \"maximum\": 5,\n" + //
				"              \"exclusiveMaximum\": false,\n" + //
				"              \"readonly\": false\n" + //
				"            }\n" + //
				"          }\n" + //
				"        }\n";
		private PolicyType policy2 = new PolicyType(2, "type2", "Type2 description", schema2);

		private String schema3 = "{\n" + //
				"  \"$id\": \"https://example.com/person.schema.json\",\n" + //
				"  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" + //
				"  \"title\": \"Person\",\n" + //
				"  \"type\": \"object\",\n" + //
				"  \"properties\": {\n" + //
				"    \"firstName\": {\n" + //
				"      \"type\": \"string\",\n" + //
				"      \"description\": \"The person's first name.\"\n" + //
				"    },\n" + //
				"    \"lastName\": {\n" + //
				"      \"type\": \"string\",\n" + //
				"      \"description\": \"The person's last name.\"\n" + //
				"    },\n" + //
				"    \"age\": {\n" + //
				"      \"description\": \"Age in years which must be equal to or greater than zero.\",\n" + //
				"      \"type\": \"integer\",\n" + //
				"      \"minimum\": 0\n" + //
				"    }\n" + //
				"  }\n" + //
				"}";
		private PolicyType policy3 = new PolicyType(3, "type3", "Type3 description", schema3);

		private String schema4 = "{" + //
				"		  \"$id\": \"https://example.com/arrays.schema.json\"," + //
				"		  \"$schema\": \"http://json-schema.org/draft-07/schema#\"," + //
				"		  \"description\": \"A representation of a person, company, organization, or place\"," + //
				"		  \"type\": \"object\"," + //
				"		  \"properties\": {" + //
				"		    \"fruits\": {" + //
				"		      \"type\": \"array\"," + //
				"		      \"items\": {" + //
				"		        \"type\": \"string\"" + //
				"		      }" + //
				"		    }," + //
				"		    \"vegetables\": {" + //
				"		      \"type\": \"array\"," + //
				"		      \"items\": { \"$ref\": \"#/definitions/veggie\" }" + //
				"		    }" + //
				"		  }," + //
				"		  \"definitions\": {" + //
				"		    \"veggie\": {" + //
				"		      \"type\": \"object\"," + //
				"		      \"required\": [ \"veggieName\", \"veggieLike\" ]," + //
				"		      \"properties\": {" + //
				"		        \"veggieName\": {" + //
				"		          \"type\": \"string\"," + //
				"		          \"description\": \"The name of the vegetable.\"" + //
				"		        }," + //
				"		        \"veggieLike\": {" + //
				"		          \"type\": \"boolean\"," + //
				"		          \"description\": \"Do I like this vegetable?\"" + //
				"		        }" + //
				"		      }" + //
				"		    }" + //
				"		  }" + //
				"		}";
		private PolicyType policy4 = new PolicyType(4, "type4", "Type4 description", schema4);

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
			types.put(1, new PolicyTypeHolder(policy1));
			types.put(2, new PolicyTypeHolder(policy2));
			types.put(3, new PolicyTypeHolder(policy3));
			types.put(4, new PolicyTypeHolder(policy4));
			try {
				putInstance(1, "ANR-1", policyInstance1);
			} catch (JsonProcessingException | PolicyException e) {
				// Nothing
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
