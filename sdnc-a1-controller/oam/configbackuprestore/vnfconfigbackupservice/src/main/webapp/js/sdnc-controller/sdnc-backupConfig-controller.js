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
myApp.controller('BackupConfigCtrl', ['$scope', '$http','$window', 'growl', 'deviceConfigService', 'ObjectDiff', '$timeout' , function($scope, $http, $window, growl, deviceConfigService, ObjectDiff, $timeout) {

    $scope.showResult = false;
    $scope.showCompare = false;

    //THIS FUNCTION WILL BE CALLED ON PAGE LOAD
    $scope.getAllVNF = function() {

        deviceConfigService.getAllVNF().then(function(data) {
            if (data != null) {
                console.log(data);
                $scope.objvnfList = data;
                console.log("CompareConfigCtrl:getAllVNF called" + $scope.objvnfList);
            } else {
                $scope.warningMessage = "No VNF is eligible for configuration!!!";
                growl.error($scope.warningMessage, {
                    title: 'Warning!',
                    globalDisableCloseButton: false,
                    ttl: 7000,
                    disableCountDown: true  
                });
            }
        });
    };
    $scope.getAllVNF();

    $scope.selectVnf = function(selectedValueVnf) {

        if (selectedValueVnf != null && selectedValueVnf != "") {
            console.log("selectedvnf Value", selectedValueVnf);
            //selectedItem = selectedValueVnf.split("%");
//            var vnfName = selectedItem[0];
//            var vnfType = selectedItem[1];
            var vnfId = selectedValueVnf;
            $scope.getVersionList(vnfId);
        } else {
            $scope.ShowResult = false;
            $scope.showCompare = false;
            $scope.showResult = false;
            $scope.errorMessage = "Please select a VNF!!!";
            growl.error($scope.errorMessage, {
                title: 'Error!',
                globalDisableCloseButton: false,
                ttl: 7000,
                disableCountDown: true  
            });
        }
        
    }

    //THIS FUNCTION WILL BE CALLED ON SELECTION OF VNF
    $scope.getVersionList = function(vnfId) {

        $scope.ShowResult = false;

        //service call to fetch the version list
        deviceConfigService.getVersions(vnfId).then(function(result) {
                console.log("--CompareConfigCtrl::getVersionList called from controler--", JSON.stringify(result));
                var status = result.status;
                var result = result.data;
                if (status == 200) {
                    if (result.length >= 1) {
                        //in case of success, build the model object to store the service output here
                        $scope.createVersionModel(result);
                    } else {
                        $scope.ShowResult = false;
                        $scope.warningMessage = "No configruation found for the selected VNF !!";
                        growl.warning($scope.warningMessage, {
                            title: 'Warning!',
                            globalDisableCloseButton: false,
                            ttl: 7000,
                            disableCountDown: true  
                        });
                    }
                } else {
                    $scope.ShowResult = false;
                    $scope.warningMessage = "No configruation found for the selected VNF !!";
                    growl.warning($scope.warningMessage, {
                        title: 'Warning!',
                        globalDisableCloseButton: false,
                        ttl: 7000,
                        disableCountDown: true  
                    });
                }
            },
            function(response) {
                $scope.errorMessage = "Something went wrong, Please try again !!";
                growl.error($scope.errorMessage, {
                    title: 'Error!',
                    globalDisableCloseButton: false,
                    ttl: 7000,
                    disableCountDown: true  
                });
                console.log("--CompareConfigCtrl::getVersionList::Error--", response);
            });
    }

    //Function to build the UI model to be shown
    $scope.createVersionModel = function(result) {

        $scope.objVersionModel = result;
        $scope.objVersion = [];

        console.log("--CompareConfigCtrl::createVersionModel::--", JSON.stringify($scope.objVersionModel));
        if ($scope.objVersionModel.length >= 1) {
            $scope.ShowResult = true;
            $scope.showCompare = true;
            for (var i = 0; i < $scope.objVersionModel.length; i++) {
                var objVersionDetail = {};
                objVersionDetail.vnfname = $scope.objVersionModel[i].vnfname;
                objVersionDetail.vnfid = $scope.objVersionModel[i].vnfid;
                objVersionDetail.versionNo = $scope.objVersionModel[i].vnfversion;
                objVersionDetail.createdAt = $scope.objVersionModel[i].creationdate;
                objVersionDetail.configinfo = $scope.objVersionModel[i].configinfo;
                objVersionDetail.selected = false;
                
                $scope.objVersion.push(objVersionDetail);
            }
        }
        console.log("--CompareConfigCtrl::createVersionModel::final VersionModel--" + JSON.stringify($scope.objVersion));

    }

    $scope.CompareConfig = function(objVersion) {
        var count = 0;
        angular.forEach(objVersion, function(item) {
            if (item.selected == true) 
                count++;
        });
        if (count > 2) {
            $scope.errorMessage = "Only two config files can be selected for the comparison!!!";
            growl.error($scope.errorMessage, {
                title: 'Error!',
                globalDisableCloseButton: false,
                ttl: 7000,
                disableCountDown: true  
            });
        } else if (count === 1){
        	 $scope.errorMessage = "At least two config files can be selected for the comparison!!!";
             growl.error($scope.errorMessage, {
                 title: 'Error!',
                 globalDisableCloseButton: false,
                 ttl: 7000,
                 disableCountDown: true  
             });
        }else
            $scope.createCompareModelNew(objVersion);
    };

    $scope.createCompareModelNew = function(objVersion) {

        $scope.objCompareModel1 = {};
        $scope.objCompareModel2 = {};

        $scope.versionsSelected = [];
        angular.forEach(objVersion, function(item) {
            angular.forEach($scope.objVersionModel, function(val, index) {
                if (item.versionNo == val['versionNo'] && item.selected == false) {
                    $scope.objVersionModel.splice(index, 1);
                }
                if (item.selected) {
                    if ($scope.versionsSelected.indexOf(item) == -1)
                        $scope.versionsSelected.push(item);
                }
            })
        });
        console.log("--CompareConfigCtrl::createCompareModel::$scope.objVersionModel", JSON.stringify($scope.objVersionModel));
        angular.forEach($scope.objVersionModel, function(item) {
            var versionObj = {};
            var versionDetails = {};
            versionDetails.versionNo = item['vnfversion'];
            /*versionDetails.vnfName = item['vnfname'];
            versionDetails.vnfid = item['vnfid'];*/
            versionDetails.timeStamp = item.creationdate;
            versionObj.versionDetails = versionDetails;

            //fetch all the other topology/network,opertaion status for the vnf
           // versionObj.topologyInfo = $scope.fetchConfigDetails(item);
            versionObj.topologyInfo = $scope.fetchTopologyInfo(item);
            	
            versionObj.networkTopologyInfo = $scope.fetchNetworkTopologyInfo(item);
            versionObj.operationStatus = $scope.operationStatus(item);
            versionObj.vnfTopologyIdentifier = $scope.vnfTopologyIdentifier(item);

            if ((versionObj.versionDetails.versionNo == $scope.versionsSelected[0].versionNo)) {
                $scope.objCompareModel1 = versionObj;
            } else
                $scope.objCompareModel2 = versionObj;

        });
        $scope.showResult = true;
        console.log("CompareConfigCtrl::createCompareModel::objCompareModel1", JSON.stringify($scope.objCompareModel1));
        console.log("CompareConfigCtrl::createCompareModel::objCompareModel2", JSON.stringify($scope.objCompareModel2));
    }
    
    

    $scope.fetchTopologyInfo = function(item) {
        var topologyInfo = {};
        item = JSON.parse(item.configinfo);
        if (angular.isDefined(item['preload-data']) && angular.isDefined(item['preload-data']['vnf-topology-information'])) {
            var vnfTopologyInfo = item['preload-data']['vnf-topology-information'];
            if (angular.isDefined(vnfTopologyInfo['vnf-parameters'] && vnfTopologyInfo['vnf-parameters'] != null)) {
                var vnfParameters = vnfTopologyInfo['vnf-parameters'];
                for (var i = 0; i < vnfParameters.length; i++) {

                    var key = vnfParameters[i]['vnf-parameter-name'];
                    var value = vnfParameters[i]['vnf-parameter-value'];
                    console.log("CompareConfigCtrl::fetchTopologyInfo::key", key);
                    console.log("CompareConfigCtrl::fetchTopologyInfo::value", value);
                    topologyInfo[key] = value;

                }
                console.log("CompareConfigCtrl::fetchTopologyInfo::", JSON.stringify(topologyInfo));
                return topologyInfo;
            }
        }
    }
    
    $scope.fetchNetworkTopologyInfo = function(item) {
        var networkTopology = {};
        item = JSON.parse(item.configinfo);
        if (angular.isDefined(item['preload-data']) && angular.isDefined(item['preload-data']['network-topology-information'])) {
            var netwrokTopologyInfo = item['preload-data']['network-topology-information'];
            if (angular.isDefined(netwrokTopologyInfo) && netwrokTopologyInfo != null) {
                for (var i = 0; i < netwrokTopologyInfo.length; i++) {

                    var key = netwrokTopologyInfo[i]['vnf-parameter-name'];
                    var value = netwrokTopologyInfo[i]['vnf-parameter-value'];
                    console.log("CompareConfigCtrl::fetchTopologyInfo::key", key);
                    console.log("CompareConfigCtrl::fetchTopologyInfo::value", value);
                    networkTopology[key] = value;
                }
            }
        }
        console.log("CompareConfigCtrl::fetchNetworkTopologyInfo::", JSON.stringify(networkTopology));
        return networkTopology;
    }
    
    $scope.operationStatus = function(item) {
        var operationStatus = {};
        item = JSON.parse(item.configinfo);
        if (angular.isDefined(item['preload-data']) && angular.isDefined(item['preload-data']['oper-status'])) {
            var operStatus = item['preload-data']['oper-status'];
            if (angular.isDefined(operStatus) && operStatus != null) {

                var value = operStatus['order-status'];
                operationStatus['order-status'] = value;

            }
        }
        console.log("CompareConfigCtrl::operationStatus::", JSON.stringify(operationStatus));
        return operationStatus;
    }
    
    $scope.vnfTopologyIdentifier = function(item) {
        var topologyIdnetifier = {};
        item = JSON.parse(item.configinfo);
        if (angular.isDefined(item['preload-data']) && angular.isDefined(item['preload-data']['vnf-topology-information']['vnf-topology-identifier'])) {
            var topologyInfoidentifier = item['preload-data']['vnf-topology-information']['vnf-topology-identifier'];
            if (angular.isDefined(topologyInfoidentifier)) {
                angular.forEach(topologyInfoidentifier, function(value, key) {

                    console.log("CompareConfigCtrl::fetchTopologyInfo::key", key);
                    console.log("CompareConfigCtrl::fetchTopologyInfo::value", value);
                    topologyIdnetifier[key] = value;
                });
            }
        }

        console.log("CompareConfigCtrl::vnfTopologyIdentifier::", JSON.stringify(topologyIdnetifier));
        return topologyIdnetifier;
    }
    
    $scope.invokeBackup = function(){
    	deviceConfigService.invokeBackup().then(function(data) {
    		console.log("response -- data -- "+data)
    		$window.location.reload();
    	});
    }
    
    $scope.getLastModifiedTime=function(){
//    	$timeout(function(result){
//    		console.log("response-data-"+ result);
//    		$scope.lastModifiedTime="Testcode Dushyant"
//    	}
//    ,5000);
    	
    	deviceConfigService.getlastupdated().then(function(result) {
    		$scope.lastModifiedTime= result.data;
    		console.log("response -- getlastupdated -- "+JSON.stringify(result))
    	});
    }
    $scope.getLastModifiedTime()
}]);