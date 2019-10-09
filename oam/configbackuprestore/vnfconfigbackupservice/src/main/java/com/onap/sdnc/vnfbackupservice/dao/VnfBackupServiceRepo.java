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
package com.onap.sdnc.vnfbackupservice.dao;

import java.sql.Timestamp;
import java.util.List;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;


import com.onap.sdnc.vnfbackupservice.model.VnfConfigDetailsDB;

@Repository
public interface VnfBackupServiceRepo extends JpaRepository<VnfConfigDetailsDB, Long> {

	@Query(value = "Select * from vnfconfigdetails where vnfid = :vnfid", nativeQuery = true)
	List<VnfConfigDetailsDB> getVnfDetails(@Param("vnfid") String vnfid);
	
	@Query(value = "Select * from vnfconfigdetails where vnfid = :vnfid ORDER BY vnfversion DESC LIMIT 1", nativeQuery = true)
	VnfConfigDetailsDB getVnfDetail(@Param("vnfid") String vnfid);

	@Modifying
	@Query(value = "insert into vnfconfigdetails (configinfo,creationdate,lastupdated,status,vnfid,vnfname,vnfversion) VALUES (:configinfo,:creationdate,:lastupdated,:status,:vnfid,:vnfname,:vnfversion)", nativeQuery = true)
	@Transactional
	void saveVnfDetails(@Param("configinfo") String configinfo, @Param("creationdate") Timestamp creationDate,
			@Param("lastupdated") Timestamp lastupdated, @Param("status") int status, @Param("vnfid") String vnfid,
			@Param("vnfname") String vnfname,@Param("vnfversion") String vnfversion);

	@Query(value = "Select configinfo && vnfversion from vnfconfigdetails where vnfid = :vnfid", nativeQuery = true)
	List<VnfConfigDetailsDB> getVnfDetailhavingAllVersion(@Param("vnfid") String vnfid);

	@Query(value = "Select backuptime from vnfschedulertime where id=1", nativeQuery = true)
	String getvnfschedulertime();

	@Modifying
	@Query(value = "insert into vnfschedulertime(id, backuptime) VALUES (:id, :backuptime)", nativeQuery = true)
	@Transactional
	void insertSchedulerTime(@Param("id") int id, @Param("backuptime") String backuptime);

	@Modifying
	@Query(value = "UPDATE vnfschedulertime SET backuptime =:formatDateTime WHERE id = 1", nativeQuery = true)
	@Transactional
	void updateSchedulerTime(@Param("formatDateTime") String formatDateTime);
	
}
