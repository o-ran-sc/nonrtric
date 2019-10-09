package com.onap.sdnc.testapi.model;

import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import com.onap.sdnc.reports.model.Input;

public class InputTest {	
	
	private String hostname = "host";
	private String ipaddress = "0.0.0.0";
	private String network = "Network Layer";
	
	Input input = new Input();
	
	@Before
	public void setUp() throws Exception {
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void TestInput() {		
		
		input.setHostname(hostname);		
		input.setIpaddress(ipaddress);
		input.setNetwork(network);

		assertEquals(input.getHostname(), hostname);
		assertEquals(input.getIpaddress(), ipaddress);
		assertEquals(input.getNetwork(), network);

	}
}