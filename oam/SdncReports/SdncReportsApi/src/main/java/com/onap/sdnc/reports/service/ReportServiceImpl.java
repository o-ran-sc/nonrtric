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
package com.onap.sdnc.reports.service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.onap.sdnc.reports.model.PreTestConfig;
import com.onap.sdnc.reports.repository.DeviceRepository;
import com.onap.sdnc.reports.repository.PreTestConfigRepository;
import com.onap.sdnc.reports.rest.model.PreTestModel;

@Service
public class ReportServiceImpl implements IReportService {

	private static final Logger logger = LogManager.getLogger(ReportServiceImpl.class);
	
	@Autowired
	DeviceRepository deviceRepository;
	
	@Autowired 
	PreTestConfigRepository preTestConfigRepository;
	
	
	@Override
	public List<PreTestModel> findReportByDeviceIP(Date startDate, Date endDate, String deviceIP) throws Exception{
		
		try{

			List<PreTestConfig> resultSet= preTestConfigRepository.findReportByDeviceIP(startDate, endDate, deviceIP);

			if(logger.isDebugEnabled())
				logger.debug("Received Output From Repository Is: "+resultSet);
			
			List<PreTestModel> preTestList=new ArrayList<PreTestModel>();
			for(PreTestConfig config : resultSet)
			{
				try{
					long deviceid=config.getDevice().getId();
					long testid=config.getTestId();
					String testName=config.getTestName();
					String deviceIp=config.getDevice().getDeviceIP();
					String execuationDetails=config.getExecuationDetails();
					String result=config.getResult();
					Date timeStamp=config.getTimestamp();
					
					PreTestModel model=new PreTestModel(testid, deviceid, testName, deviceIp, execuationDetails, result, timeStamp);
					preTestList.add(model);
				}
				catch(Exception ex)
				{
					logger.info("Exception Occured : "+ex.getLocalizedMessage());
					logger.error(ex);
				}				
			}
			logger.info("Final PreTestConfig List Size : "+preTestList.size());		
			logger.info("findReportByDeviceIP Finished Working..");
			return preTestList;
		}
		catch(Exception ex)
		{
			logger.info("Exception Occured : "+ex.getLocalizedMessage());
			logger.error(ex);
			throw new Exception("Exception occurred while processing findReportByDeviceIP ",ex);
		}		
	}
}
