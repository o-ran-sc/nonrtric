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

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.servlet.http.HttpServletRequest;

import org.onap.nearric.simulator.model.PolicyInstance;
import org.onap.nearric.simulator.model.PolicyType;
import org.oransc.ric.a1med.api.model.InlineResponse200;
import org.oransc.ric.a1med.api.model.PolicyTypeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * This class provides the service implementation of all the A1 operation
 * 
 * @author lathishbabu.ganesan@est.tech
 *
 */

@Service
public class A1PApiServiceImpl {

	private static final Logger log = LoggerFactory.getLogger(A1PApiServiceImpl.class);

	private HashMap<String, PolicyType> policyTypes = new HashMap<String, PolicyType>();

	private ObjectMapper objectMapper = null;

	private HttpServletRequest request = null;

	public boolean validateSchema(String jsonData, String jsonSchema) {
		ProcessingReport report = null;
		boolean result = false;
		try {
			log.info("Applying schema: @<@<" + jsonSchema + ">@>@ to data: #<#<" + jsonData + ">#>#");
			JsonNode schemaNode = JsonLoader.fromString(jsonSchema);
			JsonNode data = JsonLoader.fromString(jsonData);
			JsonSchemaFactory factory = JsonSchemaFactory.byDefault();
			JsonSchema schema = factory.getJsonSchema(schemaNode);
			report = schema.validate(data);
		} catch (JsonParseException jpex) {
			log.info("Error. Something went wrong trying to parse json data: #<#<" + jsonData
					+ ">#># or json schema: @<@<" + jsonSchema + ">@>@. Are the double quotes included? "
					+ jpex.getMessage());
		} catch (ProcessingException pex) {
			log.info("Error. Something went wrong trying to process json data: #<#<" + jsonData
					+ ">#># with json schema: @<@<" + jsonSchema + ">@>@ " + pex.getMessage());
		} catch (IOException e) {
			log.info("Error. Something went wrong trying to read json data: #<#<" + jsonData
					+ ">#># or json schema: @<@<" + jsonSchema + ">@>@");
		}
		if (report != null) {
			Iterator<ProcessingMessage> iter = report.iterator();
			while (iter.hasNext()) {
				ProcessingMessage pm = iter.next();
				log.info("Processing Message: " + pm.getMessage());
			}
			result = report.isSuccess();
		}
		log.info("Result=" + result);
		return result;
	}

	public A1PApiServiceImpl() {
	}

	public void set(ObjectMapper objectMapper, HttpServletRequest request) {
		this.objectMapper = objectMapper;
		this.request = request;
	}
	
    public void reset() {
    	log.info("Resetting db");
    	policyTypes.clear();
    }

	public ResponseEntity<Void> createReplaceType(Integer policyTypeId, PolicyTypeSchema policyTypeSchema) {
		log.info("createReplaceType - policyTypeId: " + policyTypeId);
		log.info("createReplaceType - policyTypeSchema: " + policyTypeSchema);

		if (policyTypeId == null || policyTypeSchema == null || policyTypeSchema.getName() == null) {
			log.info("createReplaceType - bad parameters");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}

		if (policyTypeSchema.getPolicyTypeId().intValue() != policyTypeId.intValue()) {
			log.info("createReplaceType - policytype id mismatch between request and policyTypeSchema");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}

		if (policyTypes.containsKey(policyTypeId.toString())) {
			log.info("createReplaceType - policytype already exists");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}

		PolicyType policyType = new PolicyType(policyTypeId, policyTypeSchema);
		policyTypes.put(policyTypeId.toString(), policyType);
		log.info("createReplaceType - created ok");

		return new ResponseEntity<Void>(HttpStatus.CREATED);
	}

	public ResponseEntity<Void> deleteType(Integer policyTypeId) {
		log.info("deleteType - policyTypeId: " + policyTypeId);

		if (policyTypeId == null) {
			log.info("deleteType - bad parameter");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}

		PolicyType policyType = policyTypes.get(policyTypeId.toString());

		if (policyType == null) {
			log.info("deleteType - policytype does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}

		if (policyType.getNumberInstances() > 0) {
			log.info("deleteType - cannot delete, instances exists");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		policyTypes.remove(policyTypeId.toString());

		log.info("deleteType - deleted ok");
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);
	}

	public ResponseEntity<Void> deleteInstance(Integer policyTypeId, String policyInstanceId) {

		log.info("deleteInstance - policyTypeId: " + policyTypeId);
		log.info("deleteInstance - policyInstanceId: " + policyInstanceId);

		if (policyTypeId == null || policyInstanceId == null) {
			log.info("deleteInstance - bad parameters");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}

		PolicyType policyType = policyTypes.get(policyTypeId.toString());

		if (policyType == null) {
			log.info("deleteType - policytype does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		PolicyInstance policyInstance = policyType.getInstance(policyInstanceId);
		if (policyInstance == null) {
			log.info("deleteType - instance does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}
		policyType.delete(policyInstanceId);

		log.info("deleteInstance - deleted ok");
		return new ResponseEntity<Void>(HttpStatus.NO_CONTENT);

	}

	public ResponseEntity<PolicyTypeSchema> getPolicyTypeSchema(Integer policyTypeId) {
		log.info("getPolicyTypeSchema - policyTypeId: " + policyTypeId);

		if (policyTypeId == null) {
			log.info("getPolicyTypeSchema - bad parameter");
			return new ResponseEntity<PolicyTypeSchema>(HttpStatus.NOT_FOUND);
		}

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			String res = null;
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());

				if (policyType == null) {
					log.info("getPolicyTypeSchema - policytype does not exists");
					return new ResponseEntity<PolicyTypeSchema>(HttpStatus.NOT_FOUND);
				}

				String json = null;
				PolicyTypeSchema schema = policyType.getSchema();
				String createSchema = "{}";
				try {
					// Convert Map to JSON
					json = objectMapper.writeValueAsString(schema);
					// Print JSON output
					log.info("getPolicyTypeSchema - schema: " + json);

					createSchema = objectMapper.writeValueAsString(schema.getCreateSchema());
					log.info("getPolicyTypeSchema - createSchema: " + createSchema);
				} catch (Exception e) {
					e.printStackTrace();
					log.info("getPolicyTypeSchema - schema corrupt");
					return new ResponseEntity<PolicyTypeSchema>(HttpStatus.INTERNAL_SERVER_ERROR);
				}
				res = "{\n  \"name\" : \"" + schema.getName() + "\",\n  \"description\" : \"" + schema.getDescription()
						+ "\",\n  \"create_schema\" : " + createSchema + ",\n  \"policy_type_id\" : "
						+ schema.getPolicyTypeId().intValue() + "\n}";
				log.info("getPolicyTypeSchema - json schema: " + res);
				return new ResponseEntity<PolicyTypeSchema>(objectMapper.readValue(res, PolicyTypeSchema.class),
						HttpStatus.OK);
			} catch (Exception e) {
				e.printStackTrace();
				log.info("getPolicyTypeSchema - Couldn't serialize response for content type application/json");
				return new ResponseEntity<PolicyTypeSchema>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		log.info("getPolicyTypeSchema - not implemented");
		return new ResponseEntity<PolicyTypeSchema>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<List<Integer>> getAllTypes() {
		log.info("getAllTypes");
		
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
						HttpStatus.OK);
			} catch (IOException e) {
				e.printStackTrace();
				log.info("getAllTypes - Couldn't serialize response for content type application/json");
				return new ResponseEntity<List<Integer>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		log.info("getAllTypes - not implemented");
		return new ResponseEntity<List<Integer>>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<Void> createReplaceInstance(Integer policyTypeId, String policyInstanceId, Object body) {
		log.info("createReplaceInstance -  policyTypeId:" + policyTypeId);
		log.info("createReplaceInstance -  policyInstanceId:" + policyInstanceId);
		log.info("createReplaceInstance -  body:" + body);

		if (policyTypeId == null || policyInstanceId == null || body == null) {
			log.info("createReplaceInstance - bad parameter");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}

		log.info("createReplaceInstance -  bodyclass:" + body.getClass().toString());

		PolicyType policyType = policyTypes.get(policyTypeId.toString());

		if (policyType == null) {
			log.info("createReplaceInstance - policytype does not exists");
			return new ResponseEntity<Void>(HttpStatus.NOT_FOUND);
		}

		// Create json string from schema
		String createSchema = null;
		try {
			PolicyTypeSchema schema = policyType.getSchema();
			// Convert Map to JSON
			String json = objectMapper.writeValueAsString(schema);
			// Print JSON output
			log.info("createReplaceInstance - schema - json: " + json);
			createSchema = objectMapper.writeValueAsString(schema.getCreateSchema());
			log.info("createReplaceInstance - createSchema - string: " + createSchema);
		} catch (Exception e) {
			e.printStackTrace();
			log.info("createReplaceInstance - schema corrupt");
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		// Create json string from instance
		String jsonInstance = null;
		try {
			log.info("createReplaceInstance - raw: " + body);
			// Convert Map to JSON
			jsonInstance = objectMapper.writeValueAsString(body);
			// Print JSON output
			log.info("createReplaceInstance - instance: " + jsonInstance);

		} catch (Exception e) {
			e.printStackTrace();
			log.info("createReplaceInstance - instancce corrupt");
			return new ResponseEntity<Void>(HttpStatus.INTERNAL_SERVER_ERROR);
		}

		if (!validateSchema(jsonInstance, createSchema)) {
			log.info("createReplaceInstance - schema validation failed");
			return new ResponseEntity<Void>(HttpStatus.BAD_REQUEST);
		}
		PolicyInstance policyInstance = new PolicyInstance(policyInstanceId, body);
		policyType.createReplaceInstance(policyInstanceId, policyInstance);

		log.info("createReplaceInstance - created/replaced ok");
		return new ResponseEntity<Void>(HttpStatus.CREATED);

	}

	public ResponseEntity<List<String>> getAllInstanceForType(Integer policyTypeId) {
		log.info("getAllInstanceForType -  policyTypeId:" + policyTypeId);

		if (policyTypeId == null) {
			log.info("getAllInstanceForType - bad parameter");
			return new ResponseEntity<List<String>>(HttpStatus.NOT_FOUND);
		}

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());
				if (policyType == null) {
					log.info("getAllInstanceForType - policytype does not exists");
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
				log.info("getAllInstanceForType - " + res);
				return new ResponseEntity<List<String>>(objectMapper.readValue("[" + res + "]", List.class),
						HttpStatus.OK);
			} catch (IOException e) {
				e.printStackTrace();
				log.info("getAllInstanceForType - Couldn't serialize response for content type application/json");
				return new ResponseEntity<List<String>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}
		log.info("getAllInstanceForType - not implemented");
		return new ResponseEntity<List<String>>(HttpStatus.NOT_IMPLEMENTED);

	}

	public ResponseEntity<Object> getPolicyInstance(Integer policyTypeId, String policyInstanceId) {
		log.info("getPolicyInstance -  policyTypeId:" + policyTypeId);
		log.info("getPolicyInstance -  policyInstanceId:" + policyInstanceId);

		if (policyTypeId == null || policyInstanceId == null) {
			log.info("getPolicyInstance - bad parameter");
			return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
		}

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());
				if (policyType == null) {
					log.info("getPolicyInstance - policytype does not exists");
					return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
				}
				PolicyInstance policyInstance = policyType.getInstance(policyInstanceId);
				if (policyInstance == null) {
					log.info("getPolicyInstance - policyinstance does not exists");
					return new ResponseEntity<Object>(HttpStatus.NOT_FOUND);
				}

				String json = null;
				try {
					log.info("getPolicyInstance - rawschema: " + policyInstance.getJson());
					// Convert Map to JSON
					json = objectMapper.writeValueAsString(policyInstance.getJson());
					// Print JSON output
					log.info("getPolicyInstance - schema: " + json);

				} catch (Exception e) {
					e.printStackTrace();
					log.info("getPolicyInstance - schema corrupt");
					return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
				}

				return new ResponseEntity<Object>(objectMapper.readValue(json, Object.class), HttpStatus.OK);
			} catch (IOException e) {
				e.printStackTrace();
				log.info("getPolicyInstance - policyinstance corrupt");
				return new ResponseEntity<Object>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<Object>(HttpStatus.NOT_IMPLEMENTED);
	}

	public ResponseEntity<List<InlineResponse200>> getStatus(Integer policyTypeId, String policyInstanceId) {
		log.info("getStatus -  policyTypeId:" + policyTypeId);
		log.info("getStatus -  policyInstanceId:" + policyInstanceId);

		if (policyTypeId == null || policyInstanceId == null) {
			log.info("getStatus - bad parameters");
			return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_FOUND);
		}

		String accept = request.getHeader("Accept");
		if (accept != null && accept.contains("application/json")) {
			try {
				PolicyType policyType = policyTypes.get(policyTypeId.toString());
				if (policyType == null) {
					log.info("getStatus - policytype does not exists");
					return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_FOUND);
				}
				PolicyInstance policyInstance = policyType.getInstance(policyInstanceId);
				if (policyInstance == null) {
					log.info("getStatus - policyinstance does not exists");
					return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_FOUND);
				}

				return new ResponseEntity<List<InlineResponse200>>(
						objectMapper.readValue("[ {\n  \"handler_id\" : \"X-APP-1\",\n  \"status\" : \"enforced\"\n} ]",
								List.class),
						HttpStatus.OK);
			} catch (IOException e) {
				e.printStackTrace();
				log.info("getStatus - Couldn't serialize response for content type application/json");
				return new ResponseEntity<List<InlineResponse200>>(HttpStatus.INTERNAL_SERVER_ERROR);
			}
		}

		return new ResponseEntity<List<InlineResponse200>>(HttpStatus.NOT_IMPLEMENTED);
	}

}
