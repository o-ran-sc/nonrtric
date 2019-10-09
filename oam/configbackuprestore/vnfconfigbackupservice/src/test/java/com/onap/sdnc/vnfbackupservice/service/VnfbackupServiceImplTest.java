/*
* ============LICENSE_START=======================================================
* ONAP : SDNC-FEATURES
* ================================================================================
* Copyright 2018 TechMahindra
*=================================================================================
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
* ============LICENSE_END=========================================================
*/
package com.onap.sdnc.vnfbackupservice.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.onap.sdnc.vnfbackupservice.dao.VnfBackupServiceRepo;
import com.onap.sdnc.vnfbackupservice.model.VnfConfigDetailsDB;
import com.onap.sdnc.vnfbackupservice.scheduler.VnfRestClient;
import com.onap.sdnc.vnfbackupservice.service.VnfbackupServiceImpl;

public class VnfbackupServiceImplTest {
	
	@Mock
	VnfBackupServiceRepo vrepo;
	
	@Mock
	private RestTemplate rTemplate;
	
	@Mock
    private VnfRestClient vnfclient;
	
	@InjectMocks
	VnfbackupServiceImpl vnfserviceimpl;
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	String json =     "{\r\n" + "  \"vnfs\": {\r\n" + "    \"vnf-list\": [\r\n" + "     {\r\n"
					+ "        \"vnf-id\": \"f24ae4f1-ed6b-4f8b-9ad6-4110a6fe26e7\",\r\n"
					+ "        \"service-data\": {\r\n" + "          \"vnf-request-information\": {\r\n"
					+ "            \"vnf-name\": \"vDNS-Techm_vIMS_vnf_1\"\r\n" + "          },\r\n"
					+ "        \"service-status\": {         \r\n" + "          \"response-code\": \"200\"\r\n"
					+ "        }},\r\n" + "       \"service-status\": {         \r\n"
					+ "          \"response-code\": \"200\"\r\n" + "        }\r\n" + "     }\r\n" + "      \r\n"
					+ "	  ]\r\n" + "  }\r\n" + "	}";
	
	String url = "http://localhost:8023/test/api/foos";
	String vnfId="vnfid";
	String userName = "abc";
	String password = "abc";
	String configfile="config";	
	String vnfversion = "Version-1";
	String jsonInput="jsoninput";
	String vnfName = "vnfname";
	
	long millis = System.currentTimeMillis();
	Timestamp date = new java.sql.Timestamp(millis);
	String configinfo =json;
	Timestamp creationDate = date;
	Timestamp lastupdated = date;
	int status = 1;
	String vnfname = "vnfname";
	
	@Test
	public void parseConfigTest() {	
		VnfbackupServiceImpl vnfbackupServiceImpl = new VnfbackupServiceImpl();
		vnfbackupServiceImpl.parseVnfConfigDetails(json);
		assertTrue(true);
	}			
	
	@Test()
	public void backupVnfconfigTest()
	{		
		HttpHeaders headers = new HttpHeaders();
		HttpHeaders headers1= new HttpHeaders();
		
		headers1.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
		headers1.setContentType(MediaType.APPLICATION_JSON);
		String base64Username = userName + ":" + password;
		byte[] message = base64Username.getBytes();
		headers1.set("Authorization", "Basic " + java.util.Base64.getEncoder().encodeToString(message));		

		when(vnfclient.generateHeaders(headers, userName, password)).thenReturn(headers1);
		
		ResponseEntity<String> entity = new ResponseEntity<String>(HttpStatus.OK);	 
	
		when(rTemplate.exchange(url, HttpMethod.GET, entity, String.class)).thenReturn(entity) ;
		
		when(vnfclient.vnfRestClient(url, userName, password)).thenReturn(json);		
				
		VnfConfigDetailsDB vnfConfigDetailsDB = new VnfConfigDetailsDB();
		vnfConfigDetailsDB.setVnfid(vnfId);
		vnfConfigDetailsDB.setConfiginfo(configinfo);
		vnfConfigDetailsDB.setCreationdate(date);
		vnfConfigDetailsDB.setLastupdated(date);
		vnfConfigDetailsDB.setStatus(status);
		vnfConfigDetailsDB.setVnfname(vnfname);
		vnfConfigDetailsDB.setVnfversion(vnfversion);
			
		when(vrepo.getVnfDetail(vnfId)).thenReturn(vnfConfigDetailsDB);

		List<VnfConfigDetailsDB> listvnfconfdb= new ArrayList<VnfConfigDetailsDB>();
		
		when(vrepo.getVnfDetails(vnfId)).thenReturn(listvnfconfdb);
		 
		doNothing().when(vrepo).saveVnfDetails(configinfo, creationDate, lastupdated, status, vnfId, vnfname, vnfversion);
				
		vnfserviceimpl.backupVnfconfig(vnfId);
		
		assertEquals("success", vnfserviceimpl.backupVnfconfig(vnfId));			
	}	
	
	@Test
	public void updatedBackuptimeTest() {
		String sdtime="12:00";
		when(vrepo.getvnfschedulertime()).thenReturn(sdtime);		
		vnfserviceimpl.updatedBackuptime();		
	}
	
}
