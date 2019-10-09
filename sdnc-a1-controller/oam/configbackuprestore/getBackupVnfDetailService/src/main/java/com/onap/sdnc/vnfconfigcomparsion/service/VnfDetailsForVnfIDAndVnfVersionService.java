/*package com.onap.sdnc.vnfconfigcomparsion.service;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;

import com.onap.sdnc.vnfcomparsion.dao.VnfComparisonRepository;
import com.onap.sdnc.vnfconfigcomparsion.model.VnfCompareResponse;
import com.onap.sdnc.vnfconfigcomparsion.model.VnfConfigDetailsDB;
import com.onap.sdnc.vnfconfigcomparsion.model.VnfDetails;

public class VnfDetailsForVnfIDAndVnfVersionService {
	
	private static final Logger logger = LogManager.getLogger(VnfComparisonService.class);
	
	@Autowired
	VnfComparisonRepository vnfComparisonRepository;

	public VnfCompareResponse getConfigurationDeatils(JSONObject vnfVersionNames, String vnfId) {
		
		VnfCompareResponse vnfCompareResponse = new VnfCompareResponse();
		List<VnfDetails> vnfDetailsList = new ArrayList<VnfDetails>();
		VnfDetails vnfDetails = new VnfDetails();
		try {
			JSONArray vnfIdArray = vnfVersionNames.getJSONArray("versionNames");
				for (int i = 0; i < vnfIdArray.length(); i++) {
					try {
						VnfConfigDetailsDB vnfconfigdetails = vnfComparisonRepository
								.getVnfDetails(vnfIdArray.get(i).toString(), vnfId);
						vnfDetails.setVnfDeatils(vnfconfigdetails.getConfiginfo());
						vnfDetails.setVnfId(vnfconfigdetails.getVnfid());
						vnfDetailsList.add(vnfDetails);
						logger.debug("Versions : " + vnfIdArray.get(i));
					} catch (Exception e) {
						// TODO Auto-generated catch block
						// e.printStackTrace();
					}
				}
		} catch (JSONException jSONException) {

			throw new RuntimeException("Enter atlist 2 versions and maximum 4 versions");
		}
		vnfCompareResponse.setVnfDetails(vnfDetailsList);
		return vnfCompareResponse;
		
	}

}
*/