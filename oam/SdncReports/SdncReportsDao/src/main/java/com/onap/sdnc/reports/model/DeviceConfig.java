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
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;

@Entity
@Table(name = "deviceconfig")
public class DeviceConfig implements Serializable 
{
	private static final long serialVersionUID = -3009157732242241606L;
	@Id
	@GeneratedValue(strategy = GenerationType.AUTO)
	@Column(name = "deviceid")
	private long id;

	@Column(name = "deviceIP")
	private String deviceIP;
	
	@Column(name = "createdon")
	private String createdon;
	
	@OneToMany(mappedBy="device")
	private Set<PreTestConfig> preTestConfig;
	
	public DeviceConfig() {
	}

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}
	
	public String getCreationDate() {
		return createdon;
	}

	public void setCreationDate(String creationDate) {
		this.createdon = creationDate;
	}
	
	public Set<PreTestConfig> getPreTestConfig() {
		return preTestConfig;
	}

	public void setPreTestConfig(Set<PreTestConfig> preTestConfig) {
		this.preTestConfig = preTestConfig;
	}
	
	public String getDeviceIP() {
		return deviceIP;
	}

	public void setDeviceIP(String deviceIP) {
		this.deviceIP = deviceIP;
	}

	@Override
	public String toString() {
		return "DeviceConfig [id=" + id + ", deviceName=" +  ", createdon=" + createdon
				+  ", deviceIP=" + deviceIP + "]";
	}
}
