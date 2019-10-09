package com.onap.sdnc.vnfreportsservice.service;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.onap.sdnc.vnfreportsservice.dao.VnfReportsServiceRepo;
import com.onap.sdnc.vnfreportsservice.model.VnfConfigDetailsDB;
@Service
public class VnfReportsServiceImpl implements Vnfreportsservice {
	
	private static final Logger logger = LogManager.getLogger(VnfReportsServiceImpl.class);

	@Autowired
	VnfReportsServiceRepo vnfRepo;

	List<VnfConfigDetailsDB> config = null;

	@Override
	public List<VnfConfigDetailsDB> getVnfConfigDetailsBetweenDates(Date startDate, Date endDate) {

		logger.info("Start Date" + startDate + "End Date" + endDate);
		try {
			config = vnfRepo.findByLastupdatedBetween(startDate, endDate);
		} catch (Exception e) {
			logger.error("Exception Occered Not able to get details from DB : " + e);
		}

		return config;
	}

	@Override
	public List<VnfConfigDetailsDB> getVnfIdDetailsBetweenDates(String vnfid, Date startDate, Date endDate) {

		logger.info("Vnf ID: " + vnfid + "Start Date: " + startDate + "End Date: " + endDate);
		try {
			config = vnfRepo.findByVnfidAndLastupdatedBetween(vnfid, startDate, endDate);
		} catch (Exception e) {
			logger.error("Exception Occered Not able to get details from DB : " + e);
		}
		return config;
	}

}
