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

myApp.service('viewReportService', ['$http', function($http) {
    
this.getAllVNF = function() {
        var testlist = {};
        return $http.get('/getAllBackupVnfIds')
            .then(function(response) {
                    console.log("---validationTestService::getAllVNF::TestResponse---" + JSON.stringify(response));
                    vnflist = response.data;
                    return vnflist;
                },
                function(response) {
                    console.log("validationTestService::getAllVNF::Status Code", response.status);
                    return response;
                });

    };

	this.getData = function(startDate, endDate) {
		
		var data = {};
		if (startDate != null && endDate != null) {

            data.startdate = startDate;
            data.enddate = endDate;

        }
		var request = {
		            method: 'GET',
		            url: '/getVnfDetBetDates/'+startDate+'/'+endDate+'/',
		           
		             headers: {
		                'Content-Type': 'application/json',
		            }
		        };
		 
		 return $http(request)
	     .then(function(response) {
	             console.log("---deviceConfigService::getVersions::Response---" + JSON.stringify(response));
	             return response;
	         },
	         function(response) {
	             console.log("--deviceConfigService::getVersions::Status Code--", response.status);
	             return response;
	         });

		}
	
	
	this.getDataById = function(selectedValueVnf,startDate, endDate) {
		
		var data = {};
		if (startDate != null && endDate != null) {

            data.startdate = startDate;
            data.enddate = endDate;

        }
		var request = {
		            method: 'GET',
		            url: '/getVnfDetByVnfidBetDates/'+selectedValueVnf+'/'+startDate+'/'+endDate+'/',
		           
		             headers: {
		                'Content-Type': 'application/json',
		            }
		        };
		 
		 return $http(request)
	     .then(function(response) {
	             console.log("---deviceConfigService::getVersions::Response---" + JSON.stringify(response));
	             return response;
	         },
	         function(response) {
	             console.log("--deviceConfigService::getVersions::Status Code--", response.status);
	             return response;
	         });

		}
	
	
	
}]);