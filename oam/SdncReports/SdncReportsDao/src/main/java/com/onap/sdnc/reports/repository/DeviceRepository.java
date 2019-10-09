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
package com.onap.sdnc.reports.repository;

import javax.transaction.Transactional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onap.sdnc.reports.model.DeviceConfig;


@Repository
public interface DeviceRepository extends JpaRepository<DeviceConfig, Long> {

		@Query(value= "from DeviceConfig where deviceIP = :deviceIP" )
		DeviceConfig findDeviceIP(@Param("deviceIP") String  deviceIP);
		
		@Modifying
	    @Query(value = "insert into DeviceConfig (deviceIP,createdOn) VALUES (:deviceIP,:createdOn)", nativeQuery = true)
	    @Transactional
	    void logDeviceName(@Param("deviceIP") String deviceIP, @Param("createdOn") String  createdOn);
}
