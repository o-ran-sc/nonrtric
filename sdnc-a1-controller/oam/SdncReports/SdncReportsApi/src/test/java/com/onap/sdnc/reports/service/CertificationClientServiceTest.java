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

import static org.junit.Assert.assertEquals;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.mockito.Mock;
import com.onap.sdnc.reports.model.CertificationInputs;
import com.onap.sdnc.reports.model.Input;
import com.onap.sdnc.reports.model.Output;
import com.onap.sdnc.reports.model.PreTestResponse;

public class CertificationClientServiceTest {
	
	@Mock
	CertificationClientService cService;
	
	private String hostname = "host";
	private String ipaddress = "10.53.122.25";
	private String statistics = "0% loss";
	private String avgTime = "Minimum = 0ms";
	private String testType = "network";
	
	@Test(expected = NullPointerException.class)
	public void TestRestClient() {

		PreTestResponse pretestResponse = new PreTestResponse();
		pretestResponse.setIpaddress(ipaddress);
		pretestResponse.setAvgTime(avgTime);
		pretestResponse.setHostname(hostname);
		pretestResponse.setStatus("successs");
		pretestResponse.setStatistics(statistics);
		pretestResponse.setTesttype(testType);

		List<PreTestResponse> preTestNew = new ArrayList<PreTestResponse>();
		preTestNew.add(pretestResponse);

		Input input = new Input();
		input.setIpaddress(ipaddress);
		input.setHostname(hostname);
		input.setNetwork(testType);

		CertificationInputs certificationInputs = new CertificationInputs();
		certificationInputs.setInput(input);
		CertificationClientService certificationClientservice = new CertificationClientService();
		certificationClientservice.restClient(certificationInputs, preTestNew, testType);
		
	}

	@Test
	public void pingServiceTest() {
		CertificationInputs vnfinfo = new CertificationInputs();
		Input input = new Input();
		input.setIpaddress("10.53.122.25");
		input.setHostname("hostname");
		vnfinfo.setInput(input);
		Output mockOutput = new Output();
		mockOutput.setIpaddress("10.53.122.25");
		Output output = CertificationClientService.pingTest(vnfinfo);
		assertEquals(output.getIpaddress(), input.getIpaddress());
	}

	@Test
	public void protocolTest() {
		CertificationInputs vnfinfo = new CertificationInputs();
		Input input = new Input();
		input.setIpaddress(ipaddress);
		input.setHostname("hostname");
		vnfinfo.setInput(input);
		Output mockOutput = new Output();
		mockOutput.setIpaddress(ipaddress);
	}
}
