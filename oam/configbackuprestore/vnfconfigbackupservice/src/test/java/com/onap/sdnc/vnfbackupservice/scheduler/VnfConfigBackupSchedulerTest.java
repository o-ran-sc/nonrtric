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
package com.onap.sdnc.vnfbackupservice.scheduler;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.onap.sdnc.vnfbackupservice.model.VnfDisplayParams;
import com.onap.sdnc.vnfbackupservice.model.VnfServiceResponse;
import com.onap.sdnc.vnfbackupservice.scheduler.VnfConfigBackupScheduler;
import com.onap.sdnc.vnfbackupservice.service.VnfbackupServiceImpl;

public class VnfConfigBackupSchedulerTest {

	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}

	@Mock
	VnfbackupServiceImpl vnfbackupServiceImpl;
	
	@InjectMocks
	VnfConfigBackupScheduler vnfconfigBackScheduler;

	@Test
	public void initiateBackupServiceTest() {
		
		String backuptime="timeee";		
		String vnfId = "vnfid";
		String vnfName = "vnfname";
		VnfDisplayParams vnfDisplayParams = new VnfDisplayParams();
		vnfDisplayParams.setVnfId(vnfId);
		vnfDisplayParams.setVnfName(vnfName);
		
		VnfServiceResponse vnfServiceResponse = new VnfServiceResponse();

		List<VnfDisplayParams> vnfdisplaylist = new ArrayList<VnfDisplayParams>();
		vnfdisplaylist.add(vnfDisplayParams);
		vnfServiceResponse.setVnfDisplayList(vnfdisplaylist);
		
		when(vnfbackupServiceImpl.getAllVnfDetails()).thenReturn(vnfServiceResponse);
		when(vnfconfigBackScheduler.initiateBackupService()).thenReturn(backuptime);
		
		vnfconfigBackScheduler.initiateBackupService();
	}
	
	@Test
	public void invokebackupTest() {
		
		String vnfId="vnfid";	
		VnfConfigBackupScheduler vnfConBackSch= mock(VnfConfigBackupScheduler.class);
		when(vnfConBackSch.invokeDetails(vnfId)).thenReturn(vnfId);
		assertEquals(vnfId, vnfConBackSch.invokeDetails(vnfId));
		vnfConBackSch.invokeDetails(vnfId);
	}
	
}
