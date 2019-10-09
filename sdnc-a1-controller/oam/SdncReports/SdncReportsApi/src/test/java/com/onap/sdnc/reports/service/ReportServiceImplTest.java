package com.onap.sdnc.reports.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import com.onap.sdnc.reports.Application;
import com.onap.sdnc.reports.config.EmbeddedMariaDbConfig;
import com.onap.sdnc.reports.model.DeviceConfig;
import com.onap.sdnc.reports.model.PreTestConfig;
import com.onap.sdnc.reports.repository.PreTestConfigRepository;
import com.onap.sdnc.reports.rest.model.PreTestModel;

import org.springframework.test.annotation.DirtiesContext;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@ContextConfiguration(classes = EmbeddedMariaDbConfig.class)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ReportServiceImplTest {

	private static final Logger logger = LogManager.getLogger(ReportServiceImplTest.class);

	private Date startDate, endDate;

	@TestConfiguration
	static class ReportServiceImplTestContextConfiguration {
		@Bean
		public IReportService reportService() {
			return new ReportServiceImpl();
		}
	}

	@Autowired
	private IReportService reportService;

	@MockBean
	private PreTestConfigRepository preTestConfigRepository;

	@Test
	public void reportServiceAutoWireTest() {
		assertNotNull("Due to Application Context Fail", reportService);
	}

	@Test
	public void preTestConfigRepositoryAutoWireTest() {
		assertNotNull("Due to Application Context Fail", preTestConfigRepository);
	}

	@Before
	public void setUp() {
		DeviceConfig deviceConfig = new DeviceConfig();
		deviceConfig.setDeviceIP("0.0.0.0");
		deviceConfig.setPreTestConfig(null);
		deviceConfig.setCreationDate(new Date().toLocaleString());

		PreTestConfig obj = new PreTestConfig();
		obj.setDevice(deviceConfig);
		obj.setExecuationDetails("Ping Successful");
		obj.setResult("Pass");

		obj.setTestName("Network Layer");
		obj.setTimestamp(new Date());

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

		System.out.println(
				"Before Call : startDate " + startDate.toLocaleString() + " endDate : " + endDate.toLocaleString());
		List<PreTestConfig> configList = new ArrayList<>();
		configList.add(obj);
		Mockito.when(preTestConfigRepository.findReportByDeviceIP(startDate, endDate, "0.0.0.0"))
				.thenReturn(configList);
	}

	@Test
	public void whenFindByDeviceName_thenReturPreTest() {
		int expectedTestId = 0;
		System.out.println(
				"Test Call : startDate " + startDate.toLocaleString() + " endDate : " + endDate.toLocaleString());

		List<PreTestModel> testList;
		try {
			testList = reportService.findReportByDeviceIP(startDate, endDate, "10.0.0.0");
			assertThat(testList.get(0).getTestid()).isEqualTo(expectedTestId);
		} catch (Exception e) {

			e.printStackTrace();
		}
	}
}
