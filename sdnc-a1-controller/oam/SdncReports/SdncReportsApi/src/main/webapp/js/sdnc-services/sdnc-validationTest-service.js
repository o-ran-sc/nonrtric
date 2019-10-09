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
myApp.service('validationTestService', ['$http', function($http) {
   
    this.runPretest = function(vnfList, validationTestType) {


        var data = {};
        data.vnfList = vnfList;
        data.validationTestType = validationTestType;

        var config = {
            params: data,
            headers: {
                'Accept': 'application/json'
            }
        };
        
        console.log("validationTestService::runPretest::config", JSON.stringify(config));

        // Call the pre validation service
        var request = {
            method: 'POST',
            url: '/runtest',
            data: data,
            headers: {
                'Content-Type': "application/json"
            }
        };

        // // SEND VNF FOR VALIDATION
        return $http(request)
            .then(function(response) {
                    console.log("---validationTestService::runPretest::Response---" + JSON.stringify(response));
                    return response;
                },
                function(response) {
                    console.log("--validationTestService::Status Code--", response.status);
                    return response;
                });


    }

}]);