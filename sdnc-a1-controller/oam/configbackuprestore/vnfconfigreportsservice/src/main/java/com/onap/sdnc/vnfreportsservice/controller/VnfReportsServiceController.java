package com.onap.sdnc.vnfreportsservice.controller;

import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.onap.sdnc.vnfreportsservice.model.VnfConfigDetailsDB;
import com.onap.sdnc.vnfreportsservice.service.VnfReportsServiceImpl;

@RestController
public class VnfReportsServiceController {

	private static final Logger logger = LogManager.getLogger(VnfReportsServiceController.class);

	@Autowired
	VnfReportsServiceImpl Vnfreportsservice;

	@RequestMapping(value = "/getVnfDetBetDates/{startDate}/{endDate}", method = RequestMethod.GET, produces = "application/json")

	public List<VnfConfigDetailsDB> getVnfConfigDetailsBetweenDates(
			@PathVariable("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
			@PathVariable("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
			logger.info("Get VNF Configuration Details Inbetween 2 Dates ");
		return Vnfreportsservice.getVnfConfigDetailsBetweenDates(startDate, endDate);
	}

	@RequestMapping(value = "/getVnfDetByVnfidBetDates/{vnfid}/{startDate}/{endDate}", method = RequestMethod.GET, produces = "application/json")

	public List<VnfConfigDetailsDB> getVnfConfigDetailsByVnfIdBetweenDates(@PathVariable("vnfid") String vnfId,
			@PathVariable("startDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date startDate,
			@PathVariable("endDate") @DateTimeFormat(pattern = "dd-MM-yyyy") Date endDate) {
		logger.info("Get VNF Configuration Details Of a VnfID Inbetween 2 Dates ");
		return Vnfreportsservice.getVnfIdDetailsBetweenDates(vnfId, startDate, endDate);
	}

}
