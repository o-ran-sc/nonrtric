/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
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
 * ============LICENSE_END=========================================================
 */ 
package com.onap.sdnc.reports.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.stereotype.Service;
import com.onap.sdnc.reports.model.CertificationInputs;
import com.onap.sdnc.reports.model.Input;
import com.onap.sdnc.reports.model.PreTestResponse;
import com.onap.sdnc.reports.model.Request;
import com.onap.sdnc.reports.model.Response;
import com.onap.sdnc.reports.model.ValidationTestType;
import com.onap.sdnc.reports.model.VnfList;
import com.onap.sdnc.reports.repository.DeviceRepository;
import com.onap.sdnc.reports.repository.PreTestConfigRepository;


@EnableJpaRepositories("com.onap.sdnc.reports.repository")
@EntityScan("com.onap.sdnc.*")
@EnableAutoConfiguration
@Service
public class LayerTestServiceImpl implements LayerTestService {

	private static final Logger logger = Logger.getLogger(CertificationClientService.class);
	
	@Autowired
	CertificationClientService certificationClientservice;
	
	@Autowired
	DeviceRepository deviceRepository;
	
	@Autowired
	PreTestConfigRepository preTestRepo;
	
	@Override
	public Response networkCertification(Request restReq) {
		
		String testType = "network";		
		
		VnfList[] vnf = restReq.getVnfList();
		
		ValidationTestType[] validationType = restReq.getValidationTestType();

		CertificationInputs vnfRequestParams = new CertificationInputs();

		Response resOutput = new Response();
		
		Input input = new Input();

		List<PreTestResponse> preTestNew = new ArrayList<PreTestResponse>();
		for (ValidationTestType validationTestType : validationType) {
			if (validationTestType.getValidationType().equalsIgnoreCase("Network Layer")) {
				testType = "network";
			}
			if (validationTestType.getValidationType().equalsIgnoreCase("Protocol Layer")) {
				testType = "protocol";
			}
			for (VnfList vnfList : vnf) {
				input.setIpaddress(vnfList.getIpAddress());
				input.setHostname(vnfList.getHostName());
				vnfRequestParams.setInput(input);
				certificationClientservice.restClient(vnfRequestParams, preTestNew, testType);
			}
		}		
		resOutput.setPreTestResponse(preTestNew);		
		return resOutput;
	}

}
