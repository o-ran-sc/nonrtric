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

package com.onap.sdnc.testapi.model;

import static org.junit.Assert.assertEquals;
import java.util.List;
import org.junit.Test;
import com.onap.sdnc.reports.model.Request;
import com.onap.sdnc.reports.model.ValidationTestType;
import com.onap.sdnc.reports.model.Vnf;
import com.onap.sdnc.reports.model.VnfList;


public class VnfTest {

	private String hostname = "host";
	private String ipaddress = "0.0.0.0";
	private ValidationTestType[] validationTestType;

	Request restReq = new Request();

	@SuppressWarnings("deprecation")
	@Test
	public void TestVnf() {

		VnfList<List> vnfListt = new VnfList<List>();
		vnfListt.setHostName(hostname);
		vnfListt.setIpAddress(ipaddress);
		vnfListt.setPortNo(null);

		VnfList[] vnflist = restReq.getVnfList();
		Vnf testvnf = new Vnf();
		testvnf.setVnfList(vnflist);
		testvnf.setValidationTestType(validationTestType);

		ValidationTestType[] validationTestTypee = restReq.getValidationTestType();
		testvnf.setValidationTestType(validationTestTypee);

		assertEquals(testvnf.getVnfList(), vnflist);
		assertEquals(testvnf.getValidationTestType(), validationTestTypee);
	}
}