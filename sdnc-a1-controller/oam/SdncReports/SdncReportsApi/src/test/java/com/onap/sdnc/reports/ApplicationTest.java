package com.onap.sdnc.reports;

import static org.junit.Assert.assertNotNull;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import com.onap.sdnc.reports.controller.ReportController;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class ApplicationTest {
	
	private static final Logger logger = LogManager.getLogger(ApplicationTest.class);
	
	@Autowired
	private ReportController reportController;
	
	@Test
    public void controllerAutoWireTest()
    {
    	assertNotNull("Due to Application Context Fail", reportController);
    }
	
	@Test
	public void contextLoads() throws Exception
	{
		logger.info("Context Load Test Succeded");
	}
}
