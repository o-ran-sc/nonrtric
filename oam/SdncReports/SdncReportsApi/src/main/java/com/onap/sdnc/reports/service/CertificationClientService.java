/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package com.onap.sdnc.reports.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.onap.sdnc.reports.model.CertificationInputs;
import com.onap.sdnc.reports.model.DeviceConfig;
import com.onap.sdnc.reports.model.ODLClientResponse;
import com.onap.sdnc.reports.model.Output;
import com.onap.sdnc.reports.model.PreTestResponse;
import com.onap.sdnc.reports.repository.DeviceRepository;
import com.onap.sdnc.reports.repository.PreTestConfigRepository;

@Service
public class CertificationClientService {

	private static final Logger logger = Logger.getLogger(CertificationClientService.class);

	
	@Autowired
	DeviceRepository deviceRepository;
	
	@Autowired
	PreTestConfigRepository preTestRepo;
	
	ObjectMapper mapper = new ObjectMapper();

	public void restClient(CertificationInputs vnfinfo, List<PreTestResponse> preTestNew, String testType) {

		PreTestResponse preTest = new PreTestResponse();

		Output output = new Output();
		if ("network".equalsIgnoreCase(testType)) {
			output = pingTest(vnfinfo);
		}
		if ("protocol".equalsIgnoreCase(testType)) {
			output = protocolTest(vnfinfo);
		}
		preTest.setStatus(output.getStatus());
		preTest.setIpaddress(output.getIpaddress());
		preTest.setStatistics(output.getStatistics());
		preTest.setAvgTime(output.getAvgTime());
		preTest.setTesttype(testType);
		preTest.setHostname(output.getHostname());
		preTestNew.add(preTest);
		
		ODLClientResponse odlClientResponse=new ODLClientResponse();
		odlClientResponse.setOutput(output);
		
		testSaveResults(preTest, odlClientResponse);		
	}

	public static Output pingTest(CertificationInputs vnfinfo) {
		
		Output output = new Output();
		String pingCmd = "ping " + vnfinfo.getInput().getIpaddress();
		String pingResult = "";
		String testResult = "fail";
		String status = "unreachable";
		String reason = null;
		String timeRes = null;
		String percentile = null;
		boolean flag = false;
		boolean flag1 = false;
		try {
			InetAddress byIpaddress = InetAddress.getByName(vnfinfo.getInput().getIpaddress());
			String byHostName=vnfinfo.getInput().getHostname();			
			flag = byIpaddress.isReachable(5000);

		} catch (UnknownHostException e) {
			logger.info("Network certification Exception : " + e);
		} catch (IOException e) {
			logger.info("Network certification Exception : " + e);
		}
		
		if (flag ) {
			try {
				Runtime r = Runtime.getRuntime();
				Process p = r.exec(pingCmd);
				BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
				String inputLine;
				while ((inputLine = in.readLine()) != null) {
					if (pingResult.equals("")) {
						pingResult = inputLine;
					} else {
						pingResult += "~" + inputLine;
					}
				}
				String[] results = pingResult.split("~");
				for (String res : results) {
					if (res.trim().contains("Packets:")) {
						testResult = "pass";
						status = "reachable";
						String packets = res.trim();
						String[] lossPercentile = packets.split("\\(");
						percentile = lossPercentile[1].replace(")", "").replace(",", "").trim();
					}
					if (res.trim().contains("Minimum")) {
						String timeMs = res.trim();
						String[] time = timeMs.split(",");
						timeRes = time[0];
					}
				}
				in.close();
			} catch (Exception e) {
				logger.info("Network certification Exception : " + e);
				testResult = "fail";
				status = "unreachable";
				reason = e.toString();
			}
		}
		output.setAvgTime(timeRes);
		output.setStatistics(percentile);
		output.setHostname(vnfinfo.getInput().getHostname());
		output.setIpaddress(vnfinfo.getInput().getIpaddress());
		output.setReason(reason);
		output.setTestresult(testResult);
		output.setStatus(status);

		return output;
	}

	public static Output protocolTest(CertificationInputs vnfinfo) {
		Output output = new Output();
		Socket s = null;
		String status = "unreachable";
		String reason = null;
		try {
			s = new Socket(vnfinfo.getInput().getIpaddress(), Integer.parseInt("445"));
			status = "reachable";
		} catch (Exception e) {
			logger.info("Protocol certification Exception : " + e);
			reason = e.toString();
			status = "unreachable";
		} finally {
			if (s != null)
				try {
					s.close();
				} catch (Exception e) {
					logger.info("Protocol certification Exception : " + e);
					reason = e.toString();
					status = "unreachable";
				}
		}
		output.setStatus(status);
		output.setIpaddress(vnfinfo.getInput().getIpaddress());
		output.setReason(reason);
		
		return output;
	}
	
	public void testSaveResults(PreTestResponse preTest,ODLClientResponse output) {
		boolean flag=false;
		long devId = 1;
		
		String timeStamp = new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
		try {
			DeviceConfig devicename = deviceRepository.findDeviceIP(preTest.getIpaddress());
			devId = devicename.getId();			
		} catch (Exception e) {
			flag=true;			
		}
		if(flag) {
			deviceRepository.logDeviceName(preTest.getIpaddress(), timeStamp);	
		}
		
		DeviceConfig devicename = deviceRepository.findDeviceIP(preTest.getIpaddress());
		devId = devicename.getId();
		
		Gson gson = new Gson();
		String testName= preTest.getTesttype();
		String result = preTest.getStatus();
		String execuationDetails = gson.toJson(output);

		preTestRepo.logPreTestReport(testName, result, execuationDetails, timeStamp, devId);
	}

}