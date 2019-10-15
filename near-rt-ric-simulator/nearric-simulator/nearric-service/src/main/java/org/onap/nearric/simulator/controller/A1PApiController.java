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

package org.onap.nearric.simulator.controller;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;

import org.apache.commons.beanutils.BeanUtils;
import org.onap.nearric.simulator.model.PolicyType;
import org.onap.nearric.simulator.service.A1PApiServiceImpl;
import org.oransc.ric.a1med.api.model.InlineResponse200;
import org.oransc.ric.a1med.api.model.PolicyTypeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import io.swagger.annotations.ApiParam;

/**
 * This class provides all the operation performed by A1 API.
 * 
 * @author lathishbabu.ganesan@est.tech
 *
 */
@RestController
public class A1PApiController implements A1PApi {

  private static final Logger log = LoggerFactory.getLogger(A1PApiController.class);

  private final ObjectMapper objectMapper;

  private final HttpServletRequest request;

  //@Autowired
  private A1PApiServiceImpl a1pApiService;
  //private A1PApiService a1pApiService;

  @Autowired
  public A1PApiController(ObjectMapper objectMapper, HttpServletRequest request) {
      this.objectMapper = objectMapper;
      this.request = request;
      a1pApiService = new A1PApiServiceImpl();
      a1pApiService.set(objectMapper, request);
  }

  //Reset policy db
  @RequestMapping(value = "reset",
          method = RequestMethod.GET)
  public void reset() {
	  a1pApiService.reset();
  }
  
  public ResponseEntity<Void> a1ControllerCreateOrReplacePolicyInstance(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId,@ApiParam(value = "",required=true) @PathVariable("policy_instance_id") String policyInstanceId,@ApiParam(value = ""  )  @Valid @RequestBody Object body) {
      return a1pApiService.createReplaceInstance(policyTypeId, policyInstanceId, body);
  }

  public ResponseEntity<Void> a1ControllerCreatePolicyType(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId,@ApiParam(value = ""  )  @Valid @RequestBody PolicyTypeSchema body) {
  	return a1pApiService.createReplaceType(policyTypeId, body);
  }

  public ResponseEntity<Void> a1ControllerDeletePolicyInstance(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId,@ApiParam(value = "",required=true) @PathVariable("policy_instance_id") String policyInstanceId) {
  	return a1pApiService.deleteInstance(policyTypeId, policyInstanceId);
  }

  public ResponseEntity<Void> a1ControllerDeletePolicyType(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId) {
  	return a1pApiService.deleteType(policyTypeId);
  }

  public ResponseEntity<List<String>> a1ControllerGetAllInstancesForType(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId) {
      return a1pApiService.getAllInstanceForType(policyTypeId);
  }

  public ResponseEntity<List<Integer>> a1ControllerGetAllPolicyTypes() {
  	return a1pApiService.getAllTypes();
  }

  public ResponseEntity<Void> a1ControllerGetHealthcheck() {
      String accept = request.getHeader("Accept");
      return new ResponseEntity<Void>(HttpStatus.ACCEPTED);
  }

  public ResponseEntity<Object> a1ControllerGetPolicyInstance(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId,@ApiParam(value = "",required=true) @PathVariable("policy_instance_id") String policyInstanceId) {
      return a1pApiService.getPolicyInstance(policyTypeId, policyInstanceId);
  }

  public ResponseEntity<List<InlineResponse200>> a1ControllerGetPolicyInstanceStatus(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId,@ApiParam(value = "",required=true) @PathVariable("policy_instance_id") String policyInstanceId) {
  	return a1pApiService.getStatus(policyTypeId, policyInstanceId);
  }

  public ResponseEntity<PolicyTypeSchema> a1ControllerGetPolicyType(@ApiParam(value = "",required=true) @PathVariable("policy_type_id") Integer policyTypeId) {
      return a1pApiService.getPolicyTypeSchema(policyTypeId);
  }

}
