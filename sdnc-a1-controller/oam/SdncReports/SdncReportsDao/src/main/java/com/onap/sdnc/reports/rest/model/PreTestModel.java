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
package com.onap.sdnc.reports.rest.model;

import java.io.Serializable;
import java.util.Date;

public class PreTestModel implements Serializable{

	private static final long serialVersionUID = -3009157732242241606L;
	
	private long testid;
	
	private long deviceid;
	
	private String testName;
	
	private String deviceIP,execuationDetails,result;
	
	private Date timeStamp;

	public PreTestModel(long testid, long deviceid, String testName, String deviceIP, String execuationDetails,
			String result, Date timeStamp) {
		super();
		this.testid = testid;
		this.deviceid = deviceid;
		this.testName = testName;
		this.deviceIP = deviceIP;
		this.execuationDetails = execuationDetails;
		this.result = result;
		this.timeStamp = timeStamp;
	}

	@Override
	public String toString() {
		return "PreTestModel [testid=" + testid + ", deviceid=" + deviceid + ", testName=" + testName + ", deviceName="
				+ deviceIP + ", execuationDetails=" + execuationDetails + ", result=" + result + ", timeStamp="
				+ timeStamp + "]";
	}

	public long getTestid() {
		return testid;
	}

	public void setTestid(long testid) {
		this.testid = testid;
	}

	public long getDeviceid() {
		return deviceid;
	}

	public void setDeviceid(long deviceid) {
		this.deviceid = deviceid;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public String getDeviceIP() {
		return deviceIP;
	}

	public void setDeviceName(String deviceName) {
		this.deviceIP = deviceName;
	}

	public String getExecuationDetails() {
		return execuationDetails;
	}

	public void setExecuationDetails(String execuationDetails) {
		this.execuationDetails = execuationDetails;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public Date getTimeStamp() {
		return timeStamp;
	}

	public void setTimeStamp(Date timeStamp) {
		this.timeStamp = timeStamp;
	}
}
