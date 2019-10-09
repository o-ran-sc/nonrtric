/*package com.onap.sdnc.vnfconfigcomparsion.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.onap.sdnc.vnfcomparsion.dao.VnfComparisonRepository;
import com.onap.sdnc.vnfconfigcomparsion.model.VnfCompareResponse;
import com.onap.sdnc.vnfconfigcomparsion.model.VnfConfigDetailsDB;
import com.onap.sdnc.vnfconfigcomparsion.model.VnfDetails;

public class VnfDetailsForVnfIDService {
	
	@Autowired
	VnfComparisonRepository vnfComparisonRepository;

	public VnfCompareResponse getConfigurationDeatils(String vnfId) {
		VnfCompareResponse vnfCompareResponse = new VnfCompareResponse();
		List<VnfDetails> vnfDetailsList = new ArrayList<VnfDetails>();
		VnfDetails vnfDetails = new VnfDetails();
			try {
				VnfConfigDetailsDB vnfconfigdetails = vnfComparisonRepository.getVnfDetailsByVnfID(vnfId);
				vnfDetails.setVnfDeatils(vnfconfigdetails.getConfiginfo());
				vnfDetails.setVnfId(vnfconfigdetails.getVnfid());
				vnfDetailsList.add(vnfDetails);
				} catch (Exception e) {
				// TODO Auto-generated catch block
				// e.printStackTrace();
				}
		vnfCompareResponse.setVnfDetails(vnfDetailsList);
		return vnfCompareResponse;
	}

}
*/