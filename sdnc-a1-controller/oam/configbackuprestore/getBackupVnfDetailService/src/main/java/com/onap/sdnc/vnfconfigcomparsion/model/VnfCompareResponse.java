package com.onap.sdnc.vnfconfigcomparsion.model;

import java.util.List;

public class VnfCompareResponse {

	private List<VnfDetails> vnfDetails;

	public List<VnfDetails> getVnfDetails() {
		return vnfDetails;
	}

	public void setVnfDetails(List<VnfDetails> vnfDetails) {
		this.vnfDetails = vnfDetails;
	}
}
