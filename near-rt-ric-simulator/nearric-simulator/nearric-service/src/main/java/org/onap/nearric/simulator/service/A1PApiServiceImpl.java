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

package org.onap.nearric.simulator.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.beanutils.BeanUtils;
import org.onap.nearric.simulator.model.PolicyType;
import org.onap.nearric.simulator.model.PolicyInstance;
import org.oransc.ric.a1med.api.model.PolicyTypeSchema;
import org.oransc.ric.a1med.api.model.InlineResponse200;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * This class provides the service implementation of all the A1 operation
 * 
 * @author lathishbabu.ganesan@est.tech
 *
 */

@Service
public class A1PApiServiceImpl { // implements A1PApiService {

	private static final Logger log = LoggerFactory.getLogger(A1PApiServiceImpl.class);

	private HashMap<String, PolicyType> policyTypes = new HashMap<String, PolicyType>();

	private ObjectMapper objectMapper = null;

	private HttpServletRequest request = null;

	public boolean validateSchema(String jsonData, String jsonSchema) {
		ProcessingReport report = null;
		boolean result = false;
		try {
			System.out.println("Applying schema: @<@<" + jsonSchema + ">@>@ to data: #<#<" + jsonData + ">#>#");
			JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
			JsonNode data = JsonLoader.fromString(jsonData);
			JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
			JsonSchema schema = factory.getJsonSchema(schemaNode);
			report = schema.validate(data);
		} catch (JsonParseException jpex) {
			System.out.println("Error. Something went wrong trying to parse json data: #<#<" + jsonData
					+ ">#># or json schema: @<@<" + jsonSchema + ">@>@. Are the double quotes included? "
					+ jpex.getMessage());
			// jpex.printStackTrace();
		} catch (ProcessingException pex) {
			System.out.println("Error. Something went wrong trying to process json data: #<#<" + jsonData
					+ ">#># with json schema: @<@<" + jsonSchema + ">@>@ " + pex.getMessage());
			// pex.printStackTrace();
		} catch (IOException e) {
			System.out.println("Error. Something went wrong trying to read json data: #<#<" + jsonData
					+ ">#># or json schema: @<@<" + jsonSchema + ">@>@");
			// e.printStackTrace();
		}
		if (report != null) {
			Iterator<ProcessingMessage> iter = report.iterator();
			while (iter.hasNext()) {
				ProcessingMessage pm = iter.next();
				System.out.println("Processing Message: " + pm.getMessage());
			}
			result = report.isSuccess();
		}
		System.out.println(" Result=" + result);
		return result;
	}

	public A1PApiServiceImpl() {
	}

	public void set(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}


	public ResponseEntity<Void> createReplaceType(Integer policyTypeId, PolicyTypeSchema body) {

			System.out.println("createReplaceType - policyTypeId: " + policyTypeId);
			System.out.println("createReplaceType - body: " + body);

			String accept = request.getHeader("Accept");

			if (body != null && body.getName() != null) {
				if (body.getPolicyTypeId().intValue() != policyTypeId.intValue()) {
					System.out.println("createReplaceType - policytype mismatch between request and body");
					return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
				}

				if (policyTypes.containsKey(policyTypeId.toString())) {
					System.out.println("createReplaceType - policytype already exists");
					return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
				}

				PolicyType policyType = new PolicyType(policyTypeId, body);
				policyTypes.put(policyTypeId.toString(), policyType);
			}
			System.out.println("createReplaceType - created ok");
			return new ResponseEntity<Void>(HttpStatus.CREATED);

	}

	public ResponseEntity<Void> deleteType(Integer policyTypeId) {

			System.out.println("deleteType - policyTypeId: " + policyTypeId);

			String accept = request.getHeader("Accept");

			PolicyType policyType = policyTypes.get(policyTypeId.toString());

			if (policyType == null) {
				System.out.println("deleteType - policytype does not exists");
				return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
			}

			if (policyType.getNumberInstances() > 0) {
				System.out.println("deleteType - cannot delete, instances exists");
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			policyTypes.remove(policyTypeId.toString());

			System.out.println("deleteType - deleted ok");
			return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}

	public ResponseEntity<Void> deleteInstance(Integer policyTypeId, String policyInstanceId) {

			System.out.println("deleteInstance - policyTypeId: " + policyTypeId);
			System.out.println("deleteInstance - policyInstanceId: " + policyInstanceId);

			PolicyType policyType = policyTypes.get(policyTypeId.toString());

			if (policyType == null) {
				System.out.println("deleteType - policytype does not exists");
				return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
			}
			PolicyInstance policyInstance = policyType.getInstance(policyInstanceId);
			if (policyInstance == null) {
				System.out.println("deleteType - instance does not exists");
				return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
			}
			policyType.delete(policyInstanceId);

			System.out.println("deleteInstance - deleted ok");
			return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);

	}

	public ResponseEntity<PolicyTypeSchema> getPolicyTypeSchema(Integer policyTypeId) {
		System.out.println("getPolicyTypeSchema - policyTypeId: " + policyTypeId);
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			String res = null;
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());

				if (policyType == null) {
					System.out.println("getPolicyTypeSchema - policytype does not exists");
					return new ResponseEntity<PolicyTypeSchema>(HttpStatus.NOT_FOUND);
				}

				String json = "{}";
				PolicyTypeSchema schema = policyType.getSchema();
				String createSchema = "{}";
				try {
					// Convert Map to JSON
					json = objectMapper.writeValueAsString(schema);
					// Print JSON output
					System.out.println("getPolicyTypeSchema - schema: " + json);

					createSchema = objectMapper.writeValueAsString(schema.getCreateSchema());
					System.out.println("getPolicyTypeSchema - createSchema: " + createSchema);
				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("getPolicyTypeSchema - schema corrupt");
					return new ResponseEntity<PolicyTypeSchema>(HttpStatus.INTERNAL_SERVER_ERROR);
				}
				res = "{\n  \"name\" : \"" + schema.getName() + "\",\n  \"description\" : \"" + schema.getDescription()
						+ "\",\n  \"create_schema\" : " + createSchema + ",\n  \"policy_type_id\" : "
						+ schema.getPolicyTypeId().intValue() + "\n}";
				System.out.println("getPolicyTypeSchema - json schema: " + res);
				objectMapper.configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);
				return new ResponseEntity<PolicyTypeSchema>(objectMapper.readValue(res, PolicyTypeSchema.class),
						HttpStatus.ACCEPTED);
			} catch (Exception e) {
				e.printStackTrace();
				System.out
						.println("getPolicyTypeSchema - Couldn't serialize response for content type application/json");
				return new ResponseEntity<PolicyTypeSchema>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		System.out.println("getPolicyTypeSchema - not implemented");
		return new ResponseEntity<PolicyTypeSchema>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<List<Integer>> getAllTypes() {
		System.out.println("getAllTypes");
		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				Set<String> types = policyTypes.keySet();
				String res = "";
				for (Iterator<String> iterator = types.iterator(); iterator.hasNext();) {
					String tid = (String) iterator.next();
					if (res.length() > 0) {
						res = res + ",";
					}
					res = res + tid;
				}
				return new ResponseEntity<List<Integer>>(objectMapper.readValue("[" + res + "]", List.class),
						HttpStatus.ACCEPTED);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("getAllTypes - Couldn't serialize response for content type application/json");
				return new ResponseEntity<List<Integer>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		System.out.println("getAllTypes - not implemented");
		return new ResponseEntity<List<Integer>>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<Void> createReplaceInstance(Integer policyTypeId, String policyInstanceId, Object body) {
			System.out.println("createReplaceInstance -  policyTypeId:" + policyTypeId);
			System.out.println("createReplaceInstance -  policyInstanceId:" + policyInstanceId);
			System.out.println("createReplaceInstance -  body:" + body);
			System.out.println("createReplaceInstance -  bodyclass:" + body.getClass().toString());

			String accept = request.getHeader("Accept");

			PolicyType policyType = policyTypes.get(policyTypeId.toString());

			if (policyType == null) {
				System.out.println("createReplaceInstance - policytype does not exists");
				return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
			}

			// Create json string from schema
			String createSchema = null;
			try {
				PolicyTypeSchema schema = policyType.getSchema();
				// Convert Map to JSON
				String json = objectMapper.writeValueAsString(schema);
				// Print JSON output
				System.out.println("createReplaceInstance - schema - json: " + json);
				createSchema = objectMapper.writeValueAsString(schema.getCreateSchema());
				System.out.println("createReplaceInstance - createSchema - string: " + createSchema);
			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("createReplaceInstance - schema corrupt");
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			// Create json string from instance
			String jsonInstance = null;
			try {
				System.out.println("createReplaceInstance - raw: " + body);
				// Convert Map to JSON
				jsonInstance = objectMapper.writeValueAsString(body);
				// Print JSON output
				System.out.println("createReplaceInstance - instance: " + jsonInstance);

			} catch (Exception e) {
				e.printStackTrace();
				System.out.println("createReplaceInstance - instancce corrupt");
				return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
			}

			if (!validateSchema(jsonInstance, createSchema)) {
				System.out.println("createReplaceInstance - schema validation failed");
				return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
			}
			PolicyInstance policyInstance = new PolicyInstance(policyInstanceId, body);
			policyType.createReplaceInstance(policyInstanceId, policyInstance);

			System.out.println("createReplaceInstance - created/replaced ok");
			return new ResponseEntity<Void>(HttpStatus.CREATED);

	}

	public ResponseEntity<List<String>> getAllInstanceForType(Integer policyTypeId) {
		System.out.println("getAllInstanceForType -  policyTypeId:" + policyTypeId);

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());
				if (policyType == null) {
					System.out.println("getAllInstanceForType - policytype does not exists");
					return new ResponseEntity<List<String>>(HttpStatus.NOT_FOUND);
				}
				Set<String> instances = policyType.getInstances();
				String res = "";
				for (Iterator iterator = instances.iterator(); iterator.hasNext();) {
					String iid = (String) iterator.next();
					iid = "\"" + iid + "\"";
					if (res.length() > 0) {
						res = res + ",";
					}
					res = res + iid;
				}
				System.out.println("getAllInstanceForType - " + res);
				return new ResponseEntity<List<String>>(objectMapper.readValue("[" + res + "]", List.class),
						HttpStatus.ACCEPTED);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println(
						"getAllInstanceForType - Couldn't serialize response for content type application/json");
				return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		System.out.println("getAllInstanceForType - not implemented");
		return new ResponseEntity<List<String>>(HttpStatus.NOT_IMPLEMENTED);

	}

	public ResponseEntity<Object> getPolicyInstance(Integer policyTypeId, String policyInstanceId) {
		System.out.println("getPolicyInstance -  policyTypeId:" + policyTypeId);
		System.out.println("getPolicyInstance -  policyInstanceId:" + policyInstanceId);

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());
				if (policyType == null) {
					System.out.println("getPolicyInstance - policytype does not exists");
					return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
				}
				PolicyInstance policyInstance = policyType.getInstance(policyInstanceId);
				if (policyInstance == null) {
					System.out.println("getPolicyInstance - policyinstance does not exists");
					return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
				}

				String json = "{}";
				try {
					System.out.println("getPolicyInstance - rawschema: " + policyInstance.getJson());
					// Convert Map to JSON
					json = objectMapper.writeValueAsString(policyInstance.getJson());
					// Print JSON output
					System.out.println("getPolicyInstance - schema: " + json);

				} catch (Exception e) {
					e.printStackTrace();
					System.out.println("getPolicyInstance - schema corrupt");
					return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
				}

				return new ResponseEntity<Object>(objectMapper.readValue(json, Object.class), HttpStatus.ACCEPTED);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("getPolicyInstance - policyinstance corrupt");
				return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<List<InlineResponse200>> getStatus(Integer policyTypeId, String policyInstanceId) {
		System.out.println("getStatus -  policyTypeId:" + policyTypeId);
		System.out.println("getStatus -  policyInstanceId:" + policyInstanceId);

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());
				if (policyType == null) {
					System.out.println("getStatus - policytype does not exists");
					return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_FOUND);
				}
				PolicyInstance policyInstance = policyType.getInstance(policyInstanceId);
				if (policyInstance == null) {
					System.out.println("getStatus - policyinstance does not exists");
					return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_FOUND);
				}


				return new ResponseEntity<List<InlineResponse200>>(
						objectMapper.readValue("[ {\n  \"handler_id\" : \"X-APP-1\",\n  \"status\" : \"enforced\"\n} ]",
								List.class),
						HttpStatus.ACCEPTED);
			} catch (IOException e) {
				e.printStackTrace();
				System.out.println("getStatus - Couldn't serialize response for content type application/json");
				return new ResponseEntity<List<InlineResponse200>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_IMPLEMENTED);
	}

}
