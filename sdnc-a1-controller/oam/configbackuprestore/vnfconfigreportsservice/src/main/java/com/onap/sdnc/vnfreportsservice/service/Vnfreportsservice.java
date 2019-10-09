package com.onap.sdnc.vnfreportsservice.service;

import java.util.Date;
import java.util.List;
import com.onap.sdnc.vnfreportsservice.model.VnfConfigDetailsDB;

public interface Vnfreportsservice {
	public List<VnfConfigDetailsDB> getVnfConfigDetailsBetweenDates(Date startDate, Date endDate);
	public List<VnfConfigDetailsDB> getVnfIdDetailsBetweenDates(String vnfId, Date startDate, Date endDate);

}
