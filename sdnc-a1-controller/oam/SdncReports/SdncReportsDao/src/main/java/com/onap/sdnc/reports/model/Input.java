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

public class Input {
	
	    private String ipaddress;
	    private String hostname;
	    private String network;
	    
	    public String getHostname() {
			return hostname;
		}

		public void setHostname(String hostname) {
			this.hostname = hostname;
		}	
		
	    public String getIpaddress ()
	    {
	        return ipaddress;
	    }

	    public void setIpaddress (String ipaddress)
	    {
	        this.ipaddress = ipaddress;
	    }

		public String getNetwork() {
			return network;
		}

		public void setNetwork(String network) {
			this.network = network;
		}
}
