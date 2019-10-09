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

// app.js This is first entry point for the application
// =============================================================================
var myApp = angular.module('myApp', ['ngRoute', 'ngSanitize', 'ngCsv', 'angularUtils.directives.dirPagination', 'angular-growl','ng-ip-address']);

// configuring our routes 
// =============================================================================
myApp.config(['$routeProvider', function($routeProvider) {

	//Called when user select pre validation test from the UI
    $routeProvider.
    when('/testValidation', {
        templateUrl: 'form-validationTest.html',
        controller: 'TestController'
    }).
    when('/', {
        templateUrl: 'form-validationTest.html',
        controller: 'TestController'
    }).
  //Called when user select view test report from the UI
    when('/testReports', {
        templateUrl: 'form-viewReport.html',
        controller: 'ReportController'
    }).
    otherwise({
        redirectTo: 'form-validationTest.html'
    });
}]);