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
package org.oransc.ric.portal.dashboard.controller;

import java.lang.invoke.MethodHandles;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.oransc.ric.a1controller.client.api.A1ControllerApi;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidPISchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidPISchemaInput;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidPIidSchemaInput;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidPTidSchemaInput;
import org.oransc.ric.a1controller.client.model.InputNRRidSchema;
import org.oransc.ric.a1controller.client.model.InputNRRidSchemaInput;
import org.oransc.ric.a1controller.client.model.OutputCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputDescNamePTCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputDescNamePTCodeSchemaOutput;
import org.oransc.ric.a1controller.client.model.OutputPICodeSchema;
import org.oransc.ric.a1controller.client.model.OutputPIidsListCodeSchema;
import org.oransc.ric.a1controller.client.model.OutputPTidsListCodeSchema;
import org.oransc.ric.portal.dashboard.DashboardApplication;
import org.oransc.ric.portal.dashboard.DashboardConstants;
import org.oransc.ric.portal.dashboard.exceptions.HttpBadRequestException;
import org.oransc.ric.portal.dashboard.exceptions.HttpInternalServerErrorException;
import org.oransc.ric.portal.dashboard.exceptions.HttpNotFoundException;
import org.oransc.ric.portal.dashboard.exceptions.HttpNotImplementedException;
import org.oransc.ric.portal.dashboard.model.PolicyInstance;
import org.oransc.ric.portal.dashboard.model.PolicyInstances;
import org.oransc.ric.portal.dashboard.model.PolicyType;
import org.oransc.ric.portal.dashboard.model.PolicyTypes;
import org.oransc.ric.portal.dashboard.model.SuccessTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.annotation.Secured;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.swagger.annotations.ApiOperation;

/**
 * Proxies calls from the front end to the A1 Controller via the A1 Mediator
 * API.
 *
 * If a method throws RestClientResponseException, it is handled by
 * {@link CustomResponseEntityExceptionHandler#handleProxyMethodException(Exception,
 * org.springframework.web.context.request.WebRequest)}
 * which returns status 502. All other exceptions are handled by Spring which
 * returns status 500.
 */
@RestController
@RequestMapping(value = A1Controller.CONTROLLER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
public class A1Controller {

    private static final String NEAR_RT_RIC_ID = "NearRtRic1";

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// Publish paths in constants so tests are easy to write
	public static final String CONTROLLER_PATH = DashboardConstants.ENDPOINT_PREFIX + "/policy";
	// Endpoints
	public static final String VERSION_METHOD = DashboardConstants.VERSION_METHOD;
	public static final String POLICY_TYPES_METHOD = "policytypes";
	public static final String POLICY_TYPE_ID_NAME = "policy_type_id";
	public static final String POLICIES_NAME = "policies";
	public static final String POLICY_INSTANCE_ID_NAME = "policy_instance_id";

	// Populated by the autowired constructor
	private final A1ControllerApi a1ControllerApi;

	@Autowired
	public A1Controller(final A1ControllerApi A1ControllerApi) {
		Assert.notNull(A1ControllerApi, "API must not be null");
		this.a1ControllerApi = A1ControllerApi;
		if (logger.isDebugEnabled())
			logger.debug("ctor: configured with client type {}", A1ControllerApi.getClass().getName());
	}

	@ApiOperation(value = "Gets the A1 client library MANIFEST.MF property Implementation-Version.",
	        response = SuccessTransport.class)
	@GetMapping(VERSION_METHOD)
	// No role required
	public SuccessTransport getA1ControllerClientVersion() {
		return new SuccessTransport(200, DashboardApplication.getImplementationVersion(A1ControllerApi.class));
	}

	/*
	 * The fields are defined in the A1Control Typescript interface.
	 */
	@ApiOperation(value = "Gets the policy types from Near Realtime-RIC via the A1 Controller API")
	@GetMapping(POLICY_TYPES_METHOD)
	@Secured({ DashboardConstants.ROLE_ADMIN, DashboardConstants.ROLE_STANDARD })
	public Object getAllPolicyTypes(HttpServletResponse response) {
		logger.debug("getAllPolicyTypes");
		InputNRRidSchemaInput nrrid = new InputNRRidSchemaInput();
		nrrid.setNearRtRicId(NEAR_RT_RIC_ID);
		InputNRRidSchema inputSchema = new InputNRRidSchema();
		inputSchema.setInput(nrrid);
		OutputPTidsListCodeSchema outputPTidsListCodeSchema =
		        a1ControllerApi.a1ControllerGetAllPolicyTypes(inputSchema);
		checkHttpError(outputPTidsListCodeSchema.getOutput().getCode());
		List<Integer> policyTypeIds = outputPTidsListCodeSchema.getOutput().getPolicyTypeIdList();
		PolicyTypes policyTypes = new PolicyTypes();
		InputNRRidPTidSchema typeSchema = new InputNRRidPTidSchema();
		InputNRRidPTidSchemaInput typeId = new InputNRRidPTidSchemaInput();
		typeId.setNearRtRicId(NEAR_RT_RIC_ID);
		for (Integer policyTypeId : policyTypeIds) {
			typeId.setPolicyTypeId(policyTypeId);
			typeSchema.setInput(typeId);
			OutputDescNamePTCodeSchema controllerGetPolicyType =
			        a1ControllerApi.a1ControllerGetPolicyType(typeSchema);
			checkHttpError(controllerGetPolicyType.getOutput().getCode());
			OutputDescNamePTCodeSchemaOutput policyTypeSchema = controllerGetPolicyType.getOutput();
			PolicyType type = new PolicyType(policyTypeId, policyTypeSchema.getName(),
					policyTypeSchema.getDescription(), policyTypeSchema.getPolicyType().toString());
			policyTypes.add(type);
		}
		return policyTypes;
	}

	@ApiOperation(value = "Returns the policy instances for the given policy type.")
	@GetMapping(POLICY_TYPES_METHOD + "/{" + POLICY_TYPE_ID_NAME + "}/" + POLICIES_NAME)
	@Secured({ DashboardConstants.ROLE_ADMIN, DashboardConstants.ROLE_STANDARD })
	public Object getPolicyInstances(@PathVariable(POLICY_TYPE_ID_NAME) String policyTypeIdString) {
		logger.debug("getPolicyInstances {}", policyTypeIdString);
		InputNRRidPTidSchemaInput typeIdInput = new InputNRRidPTidSchemaInput();
		typeIdInput.setNearRtRicId(NEAR_RT_RIC_ID);
		Integer policyTypeId = Integer.decode(policyTypeIdString);
		typeIdInput.setPolicyTypeId(policyTypeId);
		InputNRRidPTidSchema inputSchema = new InputNRRidPTidSchema();
		inputSchema.setInput(typeIdInput);
		OutputPIidsListCodeSchema controllerGetAllInstancesForType =
		        a1ControllerApi.a1ControllerGetAllInstancesForType(inputSchema);
		checkHttpError(controllerGetAllInstancesForType.getOutput().getCode());
		List<String> instancesForType = controllerGetAllInstancesForType.getOutput().getPolicyInstanceIdList();
		PolicyInstances instances = new PolicyInstances();
		InputNRRidPTidPIidSchemaInput instanceIdInput = new InputNRRidPTidPIidSchemaInput();
		instanceIdInput.setNearRtRicId(NEAR_RT_RIC_ID);
		instanceIdInput.setPolicyTypeId(policyTypeId);
		InputNRRidPTidPIidSchema instanceInputSchema = new InputNRRidPTidPIidSchema();
		for (String instanceId : instancesForType) {
			instanceIdInput.setPolicyInstanceId(instanceId);
			instanceInputSchema.setInput(instanceIdInput);
			OutputPICodeSchema policyInstance =
			        a1ControllerApi.a1ControllerGetPolicyInstance(instanceInputSchema);
			checkHttpError(policyInstance.getOutput().getCode());
			PolicyInstance instance =
			        new PolicyInstance(instanceId, policyInstance.getOutput().getPolicyInstance());
			instances.add(instance);
		}
		return instances;
	}

	@ApiOperation(value = "Returns a policy instance of a type")
	@GetMapping(POLICY_TYPES_METHOD + "/{" + POLICY_TYPE_ID_NAME + "}/" + POLICIES_NAME + "/{"
	        + POLICY_INSTANCE_ID_NAME + "}")
	@Secured({ DashboardConstants.ROLE_ADMIN, DashboardConstants.ROLE_STANDARD })
	public Object getPolicyInstance(@PathVariable(POLICY_TYPE_ID_NAME) String policyTypeIdString,
			@PathVariable(POLICY_INSTANCE_ID_NAME) String policyInstanceId) {
		logger.debug("getPolicyInstance {}:{}", policyTypeIdString, policyInstanceId);
		InputNRRidPTidPIidSchemaInput instanceIdInput = new InputNRRidPTidPIidSchemaInput();
		instanceIdInput.setNearRtRicId(NEAR_RT_RIC_ID);
		instanceIdInput.setPolicyTypeId(Integer.decode(policyTypeIdString));
		instanceIdInput.setPolicyInstanceId(policyInstanceId);
		InputNRRidPTidPIidSchema inputSchema = new InputNRRidPTidPIidSchema();
		inputSchema.setInput(instanceIdInput);
		OutputPICodeSchema policyInstance = a1ControllerApi.a1ControllerGetPolicyInstance(inputSchema);
		checkHttpError(policyInstance.getOutput().getCode());
		return policyInstance.getOutput().getPolicyInstance();
	}

	@ApiOperation(value = "Creates the policy instances for the given policy type.")
	@PutMapping(POLICY_TYPES_METHOD + "/{" + POLICY_TYPE_ID_NAME + "}/" + POLICIES_NAME + "/{"
	        + POLICY_INSTANCE_ID_NAME + "}")
	@Secured({ DashboardConstants.ROLE_ADMIN })
	public void putPolicyInstance(@PathVariable(POLICY_TYPE_ID_NAME) String policyTypeIdString,
			@PathVariable(POLICY_INSTANCE_ID_NAME) String policyInstanceId, @RequestBody String instance) {
		logger.debug("putPolicyInstance typeId: {}, instanceId: {}, instance: {}", policyTypeIdString,
		        policyInstanceId, instance);
		InputNRRidPTidPIidPISchemaInput createInstanceInput = new InputNRRidPTidPIidPISchemaInput();
		createInstanceInput.setNearRtRicId(NEAR_RT_RIC_ID);
		createInstanceInput.setPolicyTypeId(Integer.decode(policyTypeIdString));
		createInstanceInput.setPolicyInstanceId(policyInstanceId);
		createInstanceInput.setPolicyInstance(instance);
		InputNRRidPTidPIidPISchema inputSchema = new InputNRRidPTidPIidPISchema();
		inputSchema.setInput(createInstanceInput);
		OutputCodeSchema outputCodeSchema = a1ControllerApi.a1ControllerCreatePolicyInstance(inputSchema);
		checkHttpError(outputCodeSchema.getOutput().getCode());
	}

	@ApiOperation(value = "Deletes the policy instances for the given policy type.")
	@DeleteMapping(POLICY_TYPES_METHOD + "/{" + POLICY_TYPE_ID_NAME + "}/" + POLICIES_NAME + "/{"
			+ POLICY_INSTANCE_ID_NAME + "}")
	@Secured({ DashboardConstants.ROLE_ADMIN })
	public void deletePolicyInstance(@PathVariable(POLICY_TYPE_ID_NAME) String policyTypeIdString,
			@PathVariable(POLICY_INSTANCE_ID_NAME) String policyInstanceId) {
		logger.debug("deletePolicyInstance typeId: {}, instanceId: {}", policyTypeIdString, policyInstanceId);
		InputNRRidPTidPIidSchemaInput instanceIdInput = new InputNRRidPTidPIidSchemaInput();
		instanceIdInput.setNearRtRicId(NEAR_RT_RIC_ID);
		instanceIdInput.setPolicyTypeId(Integer.decode(policyTypeIdString));
		instanceIdInput.setPolicyInstanceId(policyInstanceId);
		InputNRRidPTidPIidSchema inputSchema = new InputNRRidPTidPIidSchema();
		inputSchema.setInput(instanceIdInput);
		OutputCodeSchema outputCodeSchema = a1ControllerApi.a1ControllerDeletePolicyInstance(inputSchema);
		checkHttpError(outputCodeSchema.getOutput().getCode());
	}

	private void checkHttpError(String httpCode) {
	    logger.debug("Http Response Code: {}", httpCode);
	    if (httpCode.equals(String.valueOf(HttpStatus.NOT_FOUND.value()))) {
	        logger.error("Caught HttpNotFoundException");
	        throw new HttpNotFoundException("Not Found Exception");
	    } else if (httpCode.equals(String.valueOf(HttpStatus.BAD_REQUEST.value()))) {
	        logger.error("Caught HttpBadRequestException");
	        throw new HttpBadRequestException("Bad Request Exception");
	    } else if (httpCode.equals(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()))) {
	        logger.error("Caught HttpInternalServerErrorException");
	        throw new HttpInternalServerErrorException("Internal Server Error Exception");
	    } else if (httpCode.equals(String.valueOf(HttpStatus.NOT_IMPLEMENTED.value()))) {
	        logger.error("Caught HttpNotImplementedException");
	        throw new HttpNotImplementedException("Not Implemented Exception");
	    }
	}
}
