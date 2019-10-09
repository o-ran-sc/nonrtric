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

myApp.controller('ReportController', ['$scope', '$http', 'viewReportService', 'growl', function($scope, $http, viewReportService, growl) {
    $scope.isDisabled = true;
    $scope.pagination = false;
    $scope.selectedDevice;
    $scope.ShowResult = false;
    $scope.dateValidation = false;
    $scope.NoSearchResult = false;
    $scope.showError = false;
    $scope.errorMessage = "";
    $scope.showSuccess = false;
    $scope.successMessage = "";
    $scope.showWarning = false;
    $scope.warningMessage = "";
    $scope.showExecutionDetails = true;

    //THIS FUNCTION WILL BE CALLED WHEN USER CLICK SUBMIT FROM UI
    $scope.getReports = function(deviceIP, startdate, enddate) { 

        $scope.ShowResult = false;
        if (new Date(startdate) > new Date(enddate)) {
            $scope.dateValidation = true;
            $scope.showError = true;
            $scope.errorMessage = "Start date cannot be greated than End date";
            growl.error($scope.errorMessage, {
                title: 'Error!'
            });
            return false;
        }
        var date = new Date(startdate);
        if (angular.isDefined(deviceIP)) {
            $scope.DeviceIP = deviceIP;
        }
        if (angular.isDefined(startdate)) {
            $scope.startDate = startdate;
        }
        if (angular.isDefined(enddate)) {
            $scope.endDate = enddate;
        }

        if (deviceIP != null && startdate != null && enddate != null) {

            //service call to fetch the reports start date,end date,test name
            viewReportService.getData($scope.startDate, $scope.endDate, $scope.DeviceIP).then(function(result) {
                    console.log("--ReportController::getdata called from controler--", JSON.stringify(result.data));
                    if (result.status == 200) {
                        if (result.data != null && result.data.length >= 1) {
                        	
                            //in case of success, build the model object to store the service output here
                            $scope.createTestReportModel(result.data);
                        } else {
                            $scope.ShowResult = false;
                            $scope.showWarning = true;
                            $scope.warningMessage = "No result found for specified Device name !!";
                            growl.warning($scope.warningMessage, {
                                title: 'Warning!'
                            });
                        }
                    } else {
                        $scope.ShowResult = false;
                        $scope.showWarning = true;
                        $scope.warningMessage = "No result found for specified Device name !!";
                        growl.warning($scope.warningMessage, {
                            title: 'Warning!'
                        });
                    }
                },
                function(response) {
                    console.log("--ReportController::getdata::Error--", response);
                });
        }
    }


    //FUNCTION WILL BE CALLED WHEN USER CLICK DOWNLOAD FROM UI
    $scope.exportToExcel = function(tableId) { // ex: '#my-table'
        var exportHref = Excel.tableToExcel(tableId, 'export');
        $timeout(function() {
            location.href = exportHref;
        }, 100); // trigger download

        console.log("--ReportController::exportToexcel--");
    }

    $scope.createTestReportModel = function(result) {

        $scope.showError = false;
        $scope.showWarning = false;
        $scope.objTestReportModel = result;
        $scope.objTestModel = [];


        if ($scope.objTestReportModel.length >= 1) {
            for (var i = 0; i < $scope.objTestReportModel.length; i++) {
                var objTestReport = {};
                objTestReport.testid = $scope.objTestReportModel[i].testid;
                objTestReport.deviceid = $scope.objTestReportModel[i].deviceid;
                objTestReport.deviceIP = $scope.objTestReportModel[i].deviceIP;
                objTestReport.result = $scope.objTestReportModel[i].result;
                objTestReport.timeStamp = $scope.objTestReportModel[i].timeStamp;
                objTestReport.testname = $scope.objTestReportModel[i].testName;

                var executionDetails = {};
                $scope.tmp = angular.fromJson($scope.objTestReportModel[i].execuationDetails); 
                executionDetails = $scope.tmp.output;

                if ($scope.objTestReportModel[i].testName === "Network Layer") {
                	
                    //fetching the statistics to show in progress bar
                    var statistics = executionDetails.statistics;
                    objTestReport.status = executionDetails.status;
                    objTestReport.statistics = executionDetails.statistics;
                    statistics = statistics.split("%");
                    executionDetails.statistics = statistics[0];
                    if (executionDetails.statistics == 0) {
                        executionDetails.statisticPer = parseInt(executionDetails.statistics) + 50;
                    } else
                        executionDetails.statisticPer = executionDetails.statistics;

                    console.log("--ReportController::CreateTestReportModel--", executionDetails.statistics);

                    //fetching the avg time to show in progress bar
                    
                    var avgTime = executionDetails.avgTime;
                    objTestReport.avgTime = executionDetails.avgTime;
                    avgTime = avgTime.split("=");
                    var Testtime = avgTime[1];
                    executionDetails.avgTime = Testtime.slice(0, -2).trim();
                    console.log("--ReportController::CreateTestReportModel--", executionDetails.avgTime);
                    if (executionDetails.avgTime < 50) {
                        executionDetails.avgTimePer = parseInt(executionDetails.avgTime) + 10;
                    } else
                        executionDetails.avgTimePer = executionDetails.avgTime;

                }


                objTestReport.executionDetails = executionDetails;
                $scope.objTestModel.push(objTestReport);
                console.log("--ReportController::CreateTestReportModel--", JSON.stringify($scope.objTestModel));

            }
            $scope.ShowResult = true;
            $scope.pagination = true;
        }
        console.log("--ReportController::createTestReportModel::final TestReportModel--" + JSON.stringify($scope.objTestModel));
        $scope.csvOrder = ['testname', 'deviceIP', 'timeStamp', 'status', 'statistics', 'avgTime', 'result'];

    }
}]);