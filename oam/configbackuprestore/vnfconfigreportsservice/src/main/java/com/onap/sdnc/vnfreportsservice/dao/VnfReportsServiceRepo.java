package com.onap.sdnc.vnfreportsservice.dao;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.data.jpa.repository.JpaRepository;

import com.onap.sdnc.vnfreportsservice.controller.VnfReportsServiceController;
import com.onap.sdnc.vnfreportsservice.model.VnfConfigDetailsDB;


public interface VnfReportsServiceRepo extends JpaRepository<VnfConfigDetailsDB, Long>  {
	
	List<VnfConfigDetailsDB> findByLastupdatedBetween(Date startDate, Date endDate);
	List<VnfConfigDetailsDB> findByVnfidAndLastupdatedBetween(String vnfid,Date startDate,Date endDate);
	
}


