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
myApp.service('deviceConfigService', ['$http','VNF_API_BASE', function($http, VNF_API_BASE) {

   
	this.getAllVNFFromRc = function() {
        var rctestlist = {};
        return $http.get('/getAllBackupVnfIds')
            .then(function(response) {
                   console.log("---validationTestService::getAllVNF From Restconf::TestResponse---" + JSON.stringify(response));
                    vnflist = response.data;
                    return vnflist;
                },
                function(response) {
                   console.log("validationTestService::getAllVNF From Restconf::Status Code", response.status);
                    return response;
                });

    };
       
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

    this.getAllVnfIds = function() {
        var rctestlist = {};
        return $http.get('/getAllVnfIds')
            .then(function(response) {
                   console.log("---validationTestService::getAllVNF From Restconf::TestResponse---" + JSON.stringify(response));
                    vnflist = response.data;
                    return vnflist;
                },
                function(response) {
                   console.log("validationTestService::getAllVNF From Restconf::Status Code", response.status);
                    return response;
                });

    };
    
    this.getVersions = function(vnfId) {

        var data = {};
        data.vnfId = vnfId;
        var config = {
            params: data,
            headers: {
                'Accept': 'application/json'
            }
        };

        console.log("deviceConfigService::getVersions::config", JSON.stringify(config));

        var baseUrl = VNF_API_BASE;
        var request = {
            method: 'GET',
            url: '/configDetailsById/'+vnfId,
            
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
    
    this.invokeBackup = function() {
        var testlist = {};
        return $http.get('/backup')
            .then(function(response) {
                    console.log("---validationTestService::getAllVNF::TestResponse---" + response);
                    vnflist = response;
                    return vnflist;
                },
                function(response) {
                    console.log("validationTestService::getAllVNF::Status Code", response);
                    return response;
                });

    };
    
    this.getlastupdated = function() {
        var testlist = {};
        return $http.get('/backuptime')
            .then(function(response) {
                    console.log("---validationTestService::getBackuptime::---" + response);         
                    return response;
                })          

    };
    
    this.runApplyconfig = function(vnfid, newConfig) {

 
    	
    	var url='/vnf-list/'+vnfid;
    	
    	
    	
    	var config = {
                 headers : {
                     'Content-Type': 'application/json'
                 }
             }

    	 $http.put(url, newConfig, config)
    	 .success(function(newConfig) {
             console.log("---validationTestService::getAllVNF::TestResponse---" ,response);
             
             return  newConfig;
         },
         function(newConfig) {
             console.log("validationTestService::getAllVNF::Status Code", response);
             return newConfig;
         });
    };
	

}]);