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
package com.onap.sdnc.vnfbackupservice.model;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Entity
@Table(name = "vnfconfigdetails", schema = "testreports")
@JsonIgnoreProperties(value = {"createdAt", "updatedAt"}, 
	allowGetters = true)
public class VnfConfigDetailsDB implements Serializable{

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private int id;
	
	@Column(name = "vnfid")
	private String vnfid;

	@Column(name = "vnfversion")
	private String vnfversion;

	@Column(name = "vnfname")
	private String vnfname;

	@Column(name = "configinfo")
	@Lob
	private String configinfo;

	@Column(nullable = false, updatable = false,columnDefinition="DATETIME")
    @CreatedDate
	private Timestamp creationdate;

	@Column(nullable = false,columnDefinition="DATETIME")
    @LastModifiedDate
	private Timestamp lastupdated;
	
	@Column(name = "status")
	private int status;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getVnfid() {
		return vnfid;
	}

	public void setVnfid(String vnfid) {
		this.vnfid = vnfid;
	}

	public String getVnfversion() {
		return vnfversion;
	}

	public void setVnfversion(String vnfversion) {
		this.vnfversion = vnfversion;
	}

	public String getVnfname() {
		return vnfname;
	}

	public void setVnfname(String vnfname) {
		this.vnfname = vnfname;
	}

	public String getConfiginfo() {
		return configinfo;
	}

	public void setConfiginfo(String configinfo) {
		this.configinfo = configinfo;
	}

	public Timestamp getCreationdate() {
		return creationdate;
	}

	public void setCreationdate(Timestamp creationdate) {
		this.creationdate = creationdate;
	}
	
	public int getStatus() {
		return status;
	}

	public void setStatus(int status) {
		this.status = status;
	}	

	public Timestamp getLastupdated() {
		return lastupdated;
	}

	public void setLastupdated(Timestamp lastupdated) {
		this.lastupdated = lastupdated;
	}

}
