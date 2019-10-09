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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import com.onap.sdnc.reports.Application;
import com.onap.sdnc.reports.controller.ReportController;
import com.onap.sdnc.reports.rest.model.PreTestModel;
import com.onap.sdnc.reports.service.IReportService;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@ContextConfiguration(classes = { Application.class })
@WebMvcTest(ReportController.class)

public class ReportControllerTest {

	@Autowired
	private MockMvc mvc;

	@MockBean
	private IReportService reportService;

	private Date startDate, endDate;

	@Test
	public void reportServiceAutoWireTest() {
		assertNotNull("Due to Application Context Fail", reportService);
	}

	@Test
	public void whenFindReportByDeviceName_thenReturnThis() throws Exception {
		Calendar calendar = Calendar.getInstance();

		calendar.add(Calendar.DATE, -7);
		calendar.add(Calendar.HOUR_OF_DAY, 00);
		calendar.add(Calendar.MINUTE, 00);
		calendar.add(Calendar.SECOND, 00);
		calendar.add(Calendar.MILLISECOND, 00);
		startDate = calendar.getTime();

		Calendar endDateCalendar = Calendar.getInstance();

		endDateCalendar.add(Calendar.HOUR_OF_DAY, 23);
		endDateCalendar.add(Calendar.MINUTE, 59);
		calendar.add(Calendar.SECOND, 00);
		endDateCalendar.add(Calendar.MILLISECOND, 00);
		endDate = endDateCalendar.getTime();

		Date d = new Date();
		startDate = d;
		endDate = d;
		PreTestModel preTestModel = new PreTestModel(1, 1, "NetWorkTest", "Router", "Ping Got Successful", "Pass",
				endDate);

		List<PreTestModel> allTests = Arrays.asList(preTestModel);

		when(reportService.findReportByDeviceIP(startDate, endDate, "0.0.0.0")).thenReturn(allTests);

		mvc.perform(get("/findReportByDeviceIP/{startDate}/{endDate}/{deviceIP}/", startDate, endDate, "0.0.0.0")
				.contentType(MediaType.APPLICATION_JSON)).andExpect(status().is(400));

	}
}
