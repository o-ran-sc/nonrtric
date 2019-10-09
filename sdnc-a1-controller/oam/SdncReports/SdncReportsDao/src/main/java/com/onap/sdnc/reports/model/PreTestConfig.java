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
package com.onap.sdnc.reports.model;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Table(name = "pretestconfig")
public class PreTestConfig implements Serializable{
	
	private static final long serialVersionUID = -3009157732242241606L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "testid")
	private long testId ;

	@Column(name = "testname")
	private String testName;
		
	@Column(name = "result")
	private String result;
	
	@Column(name = "execuationdetails")
	private String execuationDetails;
	
	@Column(name = "timestamp")
	@Temporal(TemporalType.DATE)
	private Date timestamp;
	
	@ManyToOne
	@JoinColumn(name="deviceid",nullable=false)
	private DeviceConfig device;
	
	public PreTestConfig() 
	{
	}

	public long getTestId() {
		return testId;
	}

	public void setTestId(long testId) {
		this.testId = testId;
	}

	public String getTestName() {
		return testName;
	}

	public void setTestName(String testName) {
		this.testName = testName;
	}

	public DeviceConfig getDevice() {
		return device;
	}

	public void setDevice(DeviceConfig device) {
		this.device = device;
	}

	public String getResult() {
		return result;
	}

	public void setResult(String result) {
		this.result = result;
	}

	public String getExecuationDetails() {
		return execuationDetails;
	}

	public void setExecuationDetails(String execuationDetails) {
		this.execuationDetails = execuationDetails;
	}

	public Date getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(Date timestamp) {
		this.timestamp = timestamp;
	}

	@Override
	public String toString() {
		return "PreTestConfig [testId=" + testId + ", testName=" + testName + ", result=" + result
				+ ", execuationDetails=" + execuationDetails + ", timestamp=" + timestamp + ", device=" + device + "]";
	}		
}
