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
package com.onap.sdnc.vnfbackupservice.service;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.onap.sdnc.vnfbackupservice.dao.VnfBackupServiceRepo;
import com.onap.sdnc.vnfbackupservice.model.VnfConfigDetailsDB;
import com.onap.sdnc.vnfbackupservice.model.VnfDisplayParams;
import com.onap.sdnc.vnfbackupservice.model.VnfServiceResponse;

import com.onap.sdnc.vnfbackupservice.scheduler.VnfConfigBackupScheduler;
import com.onap.sdnc.vnfbackupservice.scheduler.VnfRestClient;

@Service
public class VnfbackupServiceImpl implements VnfbackupService {

	@Autowired
	private RestTemplate restTemplate;

	@Autowired
	VnfBackupServiceRepo vnfBackupServiceDao;

	@Autowired
	VnfConfigBackupScheduler vnfConfigBackupScheduler;

	@Value("${sdnc.rest.vnf.api.host}")
	private String host;

	@Value("${sdnc.rest.vnf.api.port}")
	private String port;

	@Value("${sdnc.rest.vnf.api.basepath}")
	private String basePath;

	@Value("${sdnc.rest.vnf.api.username}")
	private String username;

	@Value("${sdnc.rest.vnf.api.password}")
	private String password;

	@Autowired
	VnfRestClient vnfRestClientlocal;

	private static final Logger logger = Logger.getLogger(VnfbackupServiceImpl.class);

	@Override
	public VnfServiceResponse getAllVnfDetails() {
		VnfServiceResponse vnfServiceResponse = new VnfServiceResponse();
		String finalURL = "http://" + host + ":" + port + basePath + "/config/VNF-API:vnfs";
		String response = vnfRestClientlocal.vnfRestClient(finalURL, username, password);
		List<VnfDisplayParams> displayParams = parseVnfConfigDetails(response);
		vnfServiceResponse.setVnfDisplayList(displayParams);
		return vnfServiceResponse;
	}

	public List<VnfDisplayParams> parseVnfConfigDetails(String jsonInput) {
		List<VnfDisplayParams> displayParams = new ArrayList<VnfDisplayParams>();

		try {
			JSONObject vnf = new JSONObject(jsonInput);
			JSONArray vnfList = vnf.getJSONObject("vnfs").getJSONArray("vnf-list");

			for (int i = 0; i < vnfList.length(); i++) {
				VnfDisplayParams vnfDisplayParams = new VnfDisplayParams();
				String responseStatus = vnfList.getJSONObject(i).getJSONObject("service-status").get("response-code")
						.toString();
				if ("200".equalsIgnoreCase(responseStatus)) {
					String vnfId = vnfList.getJSONObject(i).get("vnf-id").toString();
					String vnfName = vnfList.getJSONObject(i).getJSONObject("service-data")
							.getJSONObject("vnf-request-information").get("vnf-name").toString();
					vnfDisplayParams.setVnfId(vnfId);
					vnfDisplayParams.setVnfName(vnfName);
					displayParams.add(vnfDisplayParams);
				}
			}
		} catch (JSONException e) {
			logger.error("Exception is at parseVnfConfigDetails() :  " + e);
		}
		return displayParams;
	}

	@Override
	public String backupVnfconfig(String vnfId) {
		long millis = System.currentTimeMillis();
		Timestamp date = new java.sql.Timestamp(millis);
		String finalURL = "http://" + host + ":" + port + basePath + "/config/VNF-API:vnfs/vnf-list/" + vnfId;
		logger.debug("connecting to restconf device:::" + finalURL);
		String response = vnfRestClientlocal.vnfRestClient(finalURL, username, password);
		String configInfo = response;
		Timestamp creationDate = date;
		Timestamp lastupdated = date;
		int status = 1;
		String vnfid = vnfId;
		String vnfname = "";
		VnfConfigDetailsDB getVnfDetails = null;
		String vnfversion = "Version-1";
		try {
			getVnfDetails = vnfBackupServiceDao.getVnfDetail(vnfId);
		} catch (Exception e) {
			logger.error("exception is at getVnfdetails() :  " + e);
		}
		if (getVnfDetails == null) {
			JSONObject vnf;
			try {
				vnf = new JSONObject(response);
				vnfname = vnf.getJSONArray("vnf-list").getJSONObject(0).getJSONObject("service-data")
						.getJSONObject("vnf-request-information").get("vnf-name").toString();

			} catch (JSONException e) {
				logger.error("exception is at getVnfdetails() :  " + e);
			}
			vnfBackupServiceDao.saveVnfDetails(configInfo, creationDate, lastupdated, status, vnfid, vnfname,
					vnfversion);
		} else {
			try {
				String[] vnfvesionsplit = getVnfDetails.getVnfversion().split("-");
				int tmpVnfversion = Integer.parseInt(vnfvesionsplit[1]) + 1;
				vnfversion = vnfvesionsplit[0] + "-" + String.valueOf(tmpVnfversion);

				ObjectMapper om = new ObjectMapper();
				try {
					Map<String, Object> m1 = (Map<String, Object>) (om.readValue(getVnfDetails.getConfiginfo(),
							Map.class));
					Map<String, Object> m2 = (Map<String, Object>) (om.readValue(response, Map.class));

					JSONObject vnf;
					vnf = new JSONObject(response);
					vnfname = vnf.getJSONArray("vnf-list").getJSONObject(0).getJSONObject("service-data")
							.getJSONObject("vnf-request-information").get("vnf-name").toString();

					if (!m1.equals(m2)) {
						vnfBackupServiceDao.saveVnfDetails(configInfo, creationDate, lastupdated, status, vnfid,
								vnfname, vnfversion);
					}
				} catch (Exception e) {
					logger.error("exception is at getVnfdetails() :  " + e);
				}
			} catch (Exception e) {
				logger.error("exception is at getVnfdetails() :  " + e);
			}
		}
		return "success";
	}

	@Override
	public String putVnfconfig(String configfile, String vnfId) {

		String indented = null;

		if (configfile != null) {
			restTemplate = new RestTemplate();
			String finalURL = "http://" + host + ":" + port + basePath + "/config/VNF-API:vnfs/vnf-list/" + vnfId;

			HttpHeaders headers = new HttpHeaders();

			logger.info("connecting to restconf device:::" + finalURL);
			String response = vnfRestClientlocal.vnfRestClient(finalURL, username, password);
			logger.info(response);
			ObjectMapper mapper = new ObjectMapper();
			try {
				Object json = mapper.readValue(response, Object.class);
				indented = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
				logger.info(indented);
			} catch (JsonParseException e) {
				logger.error("exception occer" + e);
			} catch (JsonMappingException e) {
				logger.error("exception occer" + e);
			} catch (IOException e) {
				logger.error("exception occer" + e);
			}

			if (!(configfile.equals(indented))) {
				HttpEntity<String> requestEntity = new HttpEntity<String>(configfile,
						vnfRestClientlocal.generateHeaders(headers, username, password));

				ResponseEntity<String> uri = restTemplate.exchange(finalURL, HttpMethod.PUT, requestEntity,
						String.class);
				logger.info(uri.getStatusCode());
				if (uri.getStatusCodeValue() == 200) {
					vnfConfigBackupScheduler.initiateBackupService();
				}
				return "ok";
			} else {
				throw new RuntimeException("Both configurations are same");
			}
		}
		return "ok";
	}

	@Override
	public String updatedBackuptime() {
		String sdtime = vnfBackupServiceDao.getvnfschedulertime();
		return sdtime;
	}

}
