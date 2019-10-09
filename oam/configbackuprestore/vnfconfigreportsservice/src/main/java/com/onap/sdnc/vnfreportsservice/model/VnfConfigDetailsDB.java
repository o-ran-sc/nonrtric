package com.onap.sdnc.vnfreportsservice.model;

import java.sql.Date;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;

@Entity
@Table(name = "vnfconfigdetails", schema = "testreports")
public class VnfConfigDetailsDB {

	private static final long serialVersionUID = 1L;

	@Id
	@GeneratedValue
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

	@Column(name = "creationdate")
	private Date creationdate;

	@Column(name = "lastupdated")
	private Date lastupdated;
	
	@Column(name = "status")
	private String status;
	

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

	public Date getCreationdate() {
		return creationdate;
	}

	public void setCreationdate(Date creationdate) {
		this.creationdate = creationdate;
	}

	
	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public Date getLastupdated() {
		return lastupdated;
	}

	public void setLastupdated(Date lastupdated) {
		this.lastupdated = lastupdated;
	}

}

