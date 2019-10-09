package com.onap.sdnc.vnfreportsservice.service;

import static org.junit.Assert.*;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.onap.sdnc.vnfreportsservice.dao.VnfReportsServiceRepo;
import com.onap.sdnc.vnfreportsservice.model.VnfConfigDetailsDB;

@RunWith(MockitoJUnitRunner.class)
public class VnfReportsServiceImplTest {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(VnfReportsServiceImplTest.class);

	@Mock
	VnfReportsServiceRepo vnfRepo;

	@InjectMocks
	VnfReportsServiceImpl Vnfreportsservice;

		@Test
		public void getVnfConfigDetailsBetweenDatesTest() throws Exception{
			VnfConfigDetailsDB db = new VnfConfigDetailsDB();
			db.setId(123);
			db.setVnfid("Vnfid");
			db.setVnfname("vnfname");
			db.setVnfversion("vnfversion");
			
			Date sDate = new Date(2018, 5, 7);
			Date eDate = new Date(2018, 7, 7);
			db.setCreationdate(sDate);
			db.setLastupdated(eDate);
			db.setStatus("status");
			db.setConfiginfo("configinfo");

			List<VnfConfigDetailsDB> list = new ArrayList<VnfConfigDetailsDB>();
			list.add(db);
			LOGGER.info("List of vnf config details::" + list);
			
		
			Mockito.when(vnfRepo.findByLastupdatedBetween(sDate, eDate)).thenReturn(list);
			
			Vnfreportsservice.getVnfConfigDetailsBetweenDates(sDate, eDate);
			assertEquals(list, Vnfreportsservice.getVnfConfigDetailsBetweenDates(sDate, eDate));

		}
	
		@Test
		public void getVnfIdDetailsBetweenDatesTest() throws Exception{
			VnfConfigDetailsDB db = new VnfConfigDetailsDB();
			db.setId(123);
			db.setVnfid("Vnfid");
			db.setVnfname("vnfname");
			db.setVnfversion("vnfversion");
			
			Date sDate = new Date(2018, 5, 7);
			Date eDate = new Date(2018, 7, 7);
			db.setCreationdate(sDate);
			db.setLastupdated(eDate);
			db.setStatus("status");
			db.setConfiginfo("configinfo");

			List<VnfConfigDetailsDB> list = new ArrayList<VnfConfigDetailsDB>();
			list.add(db);
			LOGGER.info("List of vnf config details::" + list);
			
		
			Mockito.when(vnfRepo.findByVnfidAndLastupdatedBetween("vnfid",sDate, eDate)).thenReturn(list);
			
			Vnfreportsservice.getVnfIdDetailsBetweenDates("vnfid" ,sDate, eDate);
			assertEquals(list, Vnfreportsservice.getVnfIdDetailsBetweenDates("vnfid",sDate, eDate));

		}
}


