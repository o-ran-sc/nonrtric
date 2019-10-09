package com.onap.sdnc.vnfconfigcomparsion.model;

public class VnfDetails {

	private String vnfId;
	private String vnfDeatils;
	private String vnfversion;
	
	public String getVnfversion() {
		return vnfversion;
	}
	public void setVnfversion(String vnfversion) {
		this.vnfversion = vnfversion;
	}
	public String getVnfDeatils() {
		return vnfDeatils;
	}
	public void setVnfDeatils(String vnfDeatils) {
		this.vnfDeatils = vnfDeatils;
	}
	public String getVnfId() {
		return vnfId;
	}
	public void setVnfId(String vnfId) {
		this.vnfId = vnfId;
	}
}
