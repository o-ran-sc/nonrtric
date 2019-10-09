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

import static org.junit.Assert.assertEquals;

import java.sql.Timestamp;

import org.junit.Test;

import com.onap.sdnc.vnfbackupservice.model.VnfConfigDetailsDB;

public class VnfConfigDetailsDBTest {

	@Test
	public void configDetailsDbTest() {

		long millis = System.currentTimeMillis();
		Timestamp date = new java.sql.Timestamp(millis);
		String configinfo = "configifo";
		Timestamp creationDate = date;
		Timestamp lastupdated = date;
		int status = 1;
		String vnfid = "vnfId";
		String vnfname = "vnfname";
		String vnfversion = "Version-1";

		VnfConfigDetailsDB vnfConfigDetailsDB = new VnfConfigDetailsDB();
		vnfConfigDetailsDB.setVnfid(vnfid);
		vnfConfigDetailsDB.setConfiginfo(configinfo);
		vnfConfigDetailsDB.setCreationdate(creationDate);
		vnfConfigDetailsDB.setLastupdated(lastupdated);
		vnfConfigDetailsDB.setStatus(status);
		vnfConfigDetailsDB.setVnfname(vnfname);
		vnfConfigDetailsDB.setVnfversion(vnfversion);

		assertEquals(vnfid, vnfConfigDetailsDB.getVnfid());
		assertEquals(configinfo, vnfConfigDetailsDB.getConfiginfo());
		assertEquals(creationDate, vnfConfigDetailsDB.getCreationdate());
		assertEquals(lastupdated, vnfConfigDetailsDB.getLastupdated());
		assertEquals(status, vnfConfigDetailsDB.getStatus());
		assertEquals(vnfname, vnfConfigDetailsDB.getVnfname());
		assertEquals(vnfversion, vnfConfigDetailsDB.getVnfversion());
	}
}
