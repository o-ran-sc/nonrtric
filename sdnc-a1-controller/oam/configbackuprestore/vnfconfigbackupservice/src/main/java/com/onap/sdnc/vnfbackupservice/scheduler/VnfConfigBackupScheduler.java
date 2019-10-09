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

import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.onap.sdnc.vnfbackupservice.dao.VnfBackupServiceRepo;
import com.onap.sdnc.vnfbackupservice.model.VnfDisplayParams;
import com.onap.sdnc.vnfbackupservice.model.VnfServiceResponse;
import com.onap.sdnc.vnfbackupservice.service.VnfbackupService;
import com.onap.sdnc.vnfbackupservice.service.VnfbackupServiceImpl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@EnableScheduling
public class VnfConfigBackupScheduler {
	
	@Autowired
	VnfbackupService vnfConfigBackService;

	@Autowired
	VnfBackupServiceRepo vnfBackupServiceDao;
	
	private static final Logger logger = Logger.getLogger(VnfbackupServiceImpl.class);
	
	@Scheduled(cron = "0 0 * * * *")
	public String initiateBackupService() {
		
		String lastupdatedtime = null;
		VnfServiceResponse s = vnfConfigBackService.getAllVnfDetails();
		List<VnfDisplayParams> displayParams = s.getVnfDisplayList();
		for (VnfDisplayParams params : displayParams) {
			lastupdatedtime = invokeDetails(params.getVnfId());
		}
		return lastupdatedtime;
	}

	public String invokeDetails(String vnfId) {
		String formatDateTime = null;
		try {
			LocalDateTime now = LocalDateTime.now();
			DateTimeFormatter format = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
			formatDateTime = now.format(format);
			String dbschedulertime = vnfBackupServiceDao.getvnfschedulertime();
			if (dbschedulertime == null) {
				int id = 1;
				vnfBackupServiceDao.insertSchedulerTime(id, formatDateTime);
			}
			vnfBackupServiceDao.updateSchedulerTime(formatDateTime);
		} catch (Exception e) {
			logger.error(":::::::::exception is at vackupVnfconfig()::::   " + e);
		}
		vnfConfigBackService.backupVnfconfig(vnfId);
		return formatDateTime;
	}

}
