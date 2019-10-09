package com.onap.sdnc.vnfcomparsion.dao;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.onap.sdnc.vnfconfigcomparsion.model.VnfConfigDetailsDB;


@Repository
public interface VnfComparisonRepository extends JpaRepository<VnfConfigDetailsDB, Serializable>{
	
	
	@Query(value = "Select * from vnfconfigdetails where vnfid = :vnfid", nativeQuery = true)
	List<VnfConfigDetailsDB> getVnfDetailsByVnfID(@Param("vnfid") String vnfid);

	@Query(value = "Select * from vnfconfigdetails where vnfversion = :vnfversion and vnfid = :vnfid", nativeQuery = true)
	VnfConfigDetailsDB getVnfDetails(@Param("vnfversion") String vnfversion, @Param("vnfid") String vnfid);

	public static final String FIND_VNFID = "SELECT * FROM vnfconfigdetails group by vnfid";

	@Query(value= FIND_VNFID,nativeQuery = true) 
	List<VnfConfigDetailsDB> findvnfidvnfname();

}
