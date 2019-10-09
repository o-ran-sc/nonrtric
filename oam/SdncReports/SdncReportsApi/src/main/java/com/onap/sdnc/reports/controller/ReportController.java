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
package com.onap.sdnc.reports.controller;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.onap.sdnc.reports.rest.model.PreTestModel;
import com.onap.sdnc.reports.service.IReportService;

@RestController
public class ReportController {

	private static final Logger logger = LogManager.getLogger(ReportController.class);
	
	@Autowired
	IReportService reportService;
	@RequestMapping(value="/findReportByDeviceIP/{startDate}/{endDate}/{deviceIP:.+}", produces = "application/json",method=RequestMethod.GET)
	public List<PreTestModel> findReportByDeviceIP(@PathVariable("startDate") Date startDate,@PathVariable("endDate") Date endDate,@PathVariable("deviceIP") String deviceIP) {

		try{
			logger.info("findReportByDeviceIP Started Working..");
			if(logger.isDebugEnabled())
				logger.debug("Received StartDate : "+startDate+" ,EndDate : "+endDate+"  ,DeviceIP : "+deviceIP);

			return reportService.findReportByDeviceIP(startDate,endDate,deviceIP);
		}
		catch(Exception ex)
		{
			logger.info("Exception Occured : "+ex.getLocalizedMessage());
			return java.util.Collections.emptyList();
		}		
	}
}
