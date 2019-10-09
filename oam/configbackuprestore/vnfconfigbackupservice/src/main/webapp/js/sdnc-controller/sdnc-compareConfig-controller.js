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
myApp.controller('CompareConfigCtrl', ['$scope','$filter', '$http','$window', 'growl', 'deviceConfigService', 'ObjectDiff', function($scope,$filter, $http,$window, growl, deviceConfigService, ObjectDiff) {

    $scope.showResult = false;
    $scope.showCompare = false;
  $scope.showView=false;
  $scope.pagination = false;
  $scope.gap = 2;
	$scope.filteredItems = [];
	$scope.groupedItems = [];
	$scope.itemsPerPage = 5;
	$scope.pagedItems = [];
	$scope.currentPage = 0;
	$scope.version1=false;
	$scope.version2=false;
	$scope.version3=false;
	$scope.version4=false;
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
                console.log("-----CompareConfigCtrl::getVersionList called from controler--", JSON.stringify(result));
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

        console.log("-----CompareConfigCtrl::createVersionModel::--", JSON.stringify($scope.objVersionModel));
        if ($scope.objVersionModel.length >= 1) {
            $scope.ShowResult = true;
            $scope.showCompare = true;
            $scope.showView =true;
            for (var i = 0; i < $scope.objVersionModel.length; i++) {
                var objVersionDetail = {};
                objVersionDetail.vnfname = $scope.objVersionModel[i].vnfname;
                objVersionDetail.vnfid = $scope.objVersionModel[i].vnfid;
                objVersionDetail.vnfversion = $scope.objVersionModel[i].vnfversion;
                objVersionDetail.createdAt = $scope.objVersionModel[i].creationdate;
                objVersionDetail.updatedAt = $scope.objVersionModel[i].lastupdated;
                objVersionDetail.configinfo = $scope.objVersionModel[i].configinfo;
                objVersionDetail.selected = false;
                $scope.objVersion.push(objVersionDetail);
                
            }
        }
        console.log("-----CompareConfigCtrl::createVersionModel::final VersionModel--" + JSON.stringify($scope.objVersion));
    
     // init
		$scope.sort = {
			sortingOrder : 'vnfversion',
			reverse : true
		};

		var searchMatch = function(haystack, needle) {
			if (!needle) {
				return true;
			}
			return haystack.toLowerCase().indexOf(
					needle.toLowerCase()) !== -1;
		};

		// init the filtered items
		$scope.search = function() {
			$scope.filteredItems = $filter('filter')(
					$scope.objVersion,
					function(item) {
						for ( var attr in item) {
							if (searchMatch(item[attr],
									$scope.query))
								return true;
						}
						return false;
					});
			// take care of the sorting order
			if ($scope.sort.sortingOrder !== '') {
				$scope.filteredItems = $filter(
						'orderBy')(
						$scope.filteredItems,
						$scope.sort.sortingOrder,
						$scope.sort.reverse);
			}
			$scope.currentPage = 0;
			// now group by pages
			$scope.groupToPages();
		};

		// calculate page in place
		$scope.groupToPages = function() {
			$scope.pagedItems = [];

			for (var i = 0; i < $scope.filteredItems.length; i++) {
				if (i % $scope.itemsPerPage === 0) {
					$scope.pagedItems[Math.floor(i
							/ $scope.itemsPerPage)] = [ $scope.filteredItems[i] ];
				} else {
					$scope.pagedItems[Math.floor(i
							/ $scope.itemsPerPage)]
							.push($scope.filteredItems[i]);
				}
			}
		};

		$scope.range = function(size, start, end) {
			var ret = [];
			//console.log(size, start, end);

			if (size < end) {
				end = size;
				start = size - $scope.gap;
			}
			for (var i = start; i < end; i++) {
				ret.push(i);
			}
			//console.log(ret);
			return ret;
		};

		$scope.prevPage = function() {
			if ($scope.currentPage > 0) {
				$scope.currentPage--;
			}
		};

		$scope.nextPage = function() {
			if ($scope.currentPage < $scope.pagedItems.length - 1) {
				$scope.currentPage++;
			}
		};

		$scope.setPage = function() {
			$scope.currentPage = this.n;
		};

		// functions have been describe process the data
		// for display
		$scope.search();
    
    }
  
    
    
    //For apply version
       
 $scope.ApplyConfig = function(objVersion){
	   var count = 0;
       angular.forEach(objVersion, function(item) {
           if (item.selected == true) 
               count++;
       });
       if (count < 1) {
           $scope.errorMessage = "Select a config file to apply !!!";
           growl.error($scope.errorMessage, {
               title: 'Error!',
               globalDisableCloseButton: false,
               ttl: 7000,
               disableCountDown: true  
           });
         }else if (count > 1) {
             $scope.errorMessage = "Only one config file can be applyed at a time !!!";
             growl.error($scope.errorMessage, {
                 title: 'Error!',
                 globalDisableCloseButton: false,
                 ttl: 7000,
                 disableCountDown: true  
             });
         }else
        	 $scope.applyModelNew(objVersion);
   };
   
   
   $scope.applyModelNew = function(objVersion){
	   
	   $scope.objCompareModel1 = {};

       $scope.versionsSelected = [];
       angular.forEach(objVersion, function(item) {
           angular.forEach($scope.objVersionModel, function(val, index) {
               if (item.selected) {
                   if ($scope.versionsSelected.indexOf(item) == -1)
                       $scope.versionsSelected.push(item);
               }
           })
       });
       console.log("--CompareConfigCtrl::createCompareModel::$scope.objVersionModel", JSON.stringify($scope.objVersionModel));
     
       angular.forEach($scope.versionsSelected, function(item) {
           var versionObj = {};
           var versionDetails = {};
           versionDetails.vnfversion = item['vnfversion'];
           versionDetails.vnfName = item['vnfname'];
           var vnfid = item['vnfid'];
           var config = item['configinfo'];
           var config1=JSON.parse(config);
           console.log("CompareConfigCtrl::createCompareModel::objCompareModel1", config1);
           deviceConfigService.runApplyconfig(vnfid,config1);
           $scope.showResult = false;
           $scope.successMessage = "File uploaded successfully";
           growl.success($scope.successMessage, {
               title: 'Success!',
               globalDisableCloseButton: false,
               ttl: 7000,
               disableCountDown: true  
          }); 
       });
   } 
   
//View Configuration
   
   $scope.ViewConfig = function(objVersion){
	   var elmnt1 = document.getElementById("view");
	
       elmnt1.style.display = "block";
	   var count = 0;
       angular.forEach(objVersion, function(item) {
           if (item.selected == true) 
               count++;
       });
       if (count < 1) {
           $scope.showResult = false;
           $scope.errorMessage = "Select a config file to view !!!";
           growl.error($scope.errorMessage, {
               title: 'Error!',
               globalDisableCloseButton: false,
               ttl: 7000,
               disableCountDown: true  
           });
         }else if (count > 1) {
             $scope.showResult = false;
             $scope.errorMessage = "Only one config file can be viewed at a time !!!";
             growl.error($scope.errorMessage, {
                 title: 'Error!',
                 globalDisableCloseButton: false,
                 ttl: 7000,
                 disableCountDown: true  
             });
         }else
        	 $scope.ViewCompareModelNew(objVersion); 
   	};
   
    $scope.ViewCompareModelNew = function(objVersion) {
    	$scope.objCompareModel1 = {};

        $scope.versionsSelected = [];
        angular.forEach(objVersion, function(item) {
            angular.forEach($scope.objVersionModel, function(val, index) {
                if (item.selected) {
                    if ($scope.versionsSelected.indexOf(item) == -1)
                        $scope.versionsSelected.push(item);
                }
            })
        });
        console.log("--CompareConfigCtrl::createCompareModel::$scope.objVersionModel", JSON.stringify($scope.objVersionModel));
      
        angular.forEach($scope.versionsSelected, function(item) {
            var versionObj = {};
            var versionDetails = {};
            versionDetails.vnfversion = item['vnfversion'];
            versionDetails.vnfName = item['vnfname'];
            var vnfid = item['vnfid'];
            
            
            versionDetails.timeStamp = item.createdAt;
            versionObj.versionDetails = versionDetails;

            //fetch all the other topology/network,opertaion status for the vnf
            versionObj.topologyInfo = $scope.fetchTopologyInfo(item);
            versionObj.vnfIdInfo = $scope.fetchVnfId(item);	
            versionObj.serviceStatusInfo= $scope.fetchServiceStatus(item);
            versionObj.vnfTopologyIdentifier = $scope.vnfTopologyIdentifier(item);
            versionObj.operationStatus = $scope.operationStatus(item);
            versionObj.vnfRequestInfo=$scope.fetchVnfRequestInfo(item);
            versionObj.serviceInfo= $scope.fetchServiceInfo(item);
            versionObj.requestHeader= $scope.serviceRequestHeader(item);
            versionObj.requestInfo=$scope.fetchRequestInfo(item);
           
            if ((versionObj.versionDetails.vnfversion == $scope.versionsSelected[0].vnfversion)) {
                $scope.objCompareModel1 = versionObj;
            } else
            	{console.log("CompareConfigCtrl::createCompareModel::objCompareModel1");

        }
       document.getElementById("compare").style.display = "none";
    } );
        $scope.showView=true;
        $scope.showResult = true;
        var elmnt1 = document.getElementById("view");
        elmnt1.scrollIntoView();
        console.log("CompareConfigCtrl::createCompareModel::objCompareModel1", JSON.stringify($scope.objCompareModel1));
    }
 
   
    //compare
    
    
    $scope.CompareConfig = function(objVersion) {
    	 var elmnt = document.getElementById("compare");
    	elmnt.style.display = "block";
        var count = 0;
        angular.forEach(objVersion, function(item) {
            if (item.selected == true) 
                count++;
        });
        if (count > 4) {
        	$scope.showResult = false;
            $scope.errorMessage = "Four or lessthan four and gretterthan two config files can be selected for the comparison!!!";
            growl.error($scope.errorMessage, {
                title: 'Error!',
                globalDisableCloseButton: false,
                ttl: 7000,
                disableCountDown: true  
            });
        } else if (count < 2){
        	$scope.showResult = false;
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
                if (item.selected) {
                    if ($scope.versionsSelected.indexOf(item) == -1)
                        $scope.versionsSelected.push(item);
                }
            })
        });
        console.log("--CompareConfigCtrl::createCompareModel::$scope.objVersionModel", JSON.stringify($scope.objVersionModel));
        console.log("--CompareConfigCtrl::createCompareModel::$scope.objVersionModel", JSON.stringify($scope.versionsSelected));
      
        angular.forEach($scope.versionsSelected, function(item) {
            var versionObj = {};
            var versionDetails = {};
            versionDetails.vnfversion = item.vnfversion;
            versionDetails.timeStamp = item.createdAt;
            versionObj.versionDetails = versionDetails;

            //fetch all the other topology/network,opertaion status for the vnf
            versionObj.topologyInfo = $scope.fetchTopologyInfo(item);
            versionObj.vnfIdInfo = $scope.fetchVnfId(item);	
            versionObj.serviceStatusInfo= $scope.fetchServiceStatus(item);
            versionObj.vnfTopologyIdentifier = $scope.vnfTopologyIdentifier(item);
            versionObj.operationStatus = $scope.operationStatus(item);
            versionObj.vnfRequestInfo=$scope.fetchVnfRequestInfo(item);
            versionObj.serviceInfo= $scope.fetchServiceInfo(item);
            versionObj.requestHeader= $scope.serviceRequestHeader(item);
            versionObj.requestInfo=$scope.fetchRequestInfo(item);
           
            if ((versionObj.versionDetails.vnfversion == $scope.versionsSelected[0].vnfversion)) {
                $scope.objCompareModel1 = versionObj;
                $scope.version1=true;
            } else if ((versionObj.versionDetails.vnfversion == $scope.versionsSelected[1].vnfversion))
                {$scope.objCompareModel2 = versionObj;
                $scope.version2=true;
                $scope.version3=false;
                $scope.version4=false;
        }else if((versionObj.versionDetails.vnfversion == $scope.versionsSelected[2].vnfversion)){
        	$scope.objCompareModel3 = versionObj;
        	$scope.version3=true;
        	$scope.version4=false;
        }else if((versionObj.versionDetails.vnfversion == $scope.versionsSelected[3].vnfversion)){
        	$scope.objCompareModel4 = versionObj;
        	$scope.version4=true;
        	}
      document.getElementById("view").style.display = "none";
        } );
        $scope.showResult = true;
        var elmnt = document.getElementById("compare");
        elmnt.scrollIntoView();
        console.log("CompareConfigCtrl::createCompareModel::objCompareModel1", JSON.stringify($scope.objCompareModel1));
        console.log("CompareConfigCtrl::createCompareModel::objCompareModel2", JSON.stringify($scope.objCompareModel2));
        
    }
    
    //1'st comparison for vnf topology info vnf-parameters
   $scope.fetchTopologyInfo = function(item) {
        var topologyInfo = {};
        item = JSON.parse(item.configinfo);
        var item= item['vnf-list'][0];
        if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['vnf-topology-information'])) {
            var vnfTopologyInfo = item['service-data']['vnf-topology-information'];
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
    
 
   
   //2nd comparison for vnf id
   
  $scope.fetchVnfId = function(item){
	  var vnfIdInfo = {}; 
	  item = JSON.parse(item.configinfo);
	  var item= item['vnf-list'][0];
	  if (angular.isDefined(item['vnf-id'])) {
	  var key=item['vnf-id'];
	  vnfIdInfo['vnf-id']=key;
	  }
	console.log("CompareConfigCtrl::fetchVnfId::", JSON.stringify( vnfIdInfo));
	  return vnfIdInfo;
  }  
  
 
  //3rd comparison for service status
  
  $scope.fetchServiceStatus = function(item) {
      var serviceStatusInfo = {};
      
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      
      if (angular.isDefined(item['service-status'])) {
          var serviceStatus = item['service-status'];
          if (angular.isDefined(serviceStatus)) {
              angular.forEach(serviceStatus, function(value, key) {

                 console.log("CompareConfigCtrl::fetchServiceStatus::key", key);
               console.log("CompareConfigCtrl::fetchServiceStatus::value", value);
                  serviceStatusInfo[key] = value;
              });
          }
      }

     console.log("CompareConfigCtrl::fetchServiceStatus::", JSON.stringify(serviceStatusInfo));
      return serviceStatusInfo;
  }
  
  // 4th comparison for vnf topology identifire
  
  $scope.vnfTopologyIdentifier = function(item) {
      var topologyIdnetifier = {};
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['vnf-topology-information']['vnf-topology-identifier'])) {
          var topologyInfoidentifier = item['service-data']['vnf-topology-information']['vnf-topology-identifier'];
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
 
  // 5th comparison for vnf request information
  
  $scope.fetchVnfRequestInfo = function(item) {
      var vnfRequestInfo = {};
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['vnf-request-information'])) {
          var vnfRequest = item['service-data']['vnf-request-information'];
          if (angular.isDefined(vnfRequest) && vnfRequest != null) {

        	  angular.forEach(vnfRequest, function(value, key) {

             console.log("CompareConfigCtrl::fetchVnfRequestInfo::key", key);
              console.log("CompareConfigCtrl::fetchVnfRequestInfo::value", value);
                  vnfRequestInfo[key] = value;
              });
          }
      }
console.log("CompareConfigCtrl::fetchVnfRequestInfo::", JSON.stringify(vnfRequestInfo));
      return vnfRequestInfo;
  }
  
  // 6th comparison for service info
  

  $scope.fetchServiceInfo = function(item) {
      var serviceInfo = {};
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['service-information'])) {
          var service= item['service-data']['service-information'];
          if (angular.isDefined(service) && service != null) {

        	  angular.forEach(service, function(value, key) {

           console.log("CompareConfigCtrl::fetchServiceInfo::key", key);
           console.log("CompareConfigCtrl::fetchServiceInfo::value", value);
                  serviceInfo[key] = value;
              });
          }
      }
 console.log("CompareConfigCtrl::fetchServiceInfo::", JSON.stringify(serviceInfo));
      return serviceInfo;
  }
  
  // 7th comparison for sdnc request header
  
  $scope.serviceRequestHeader = function(item) {
      var requestHeader = {};
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['sdnc-request-header'])) {
          var requestHeaderInfo = item['service-data']['sdnc-request-header'];
          if (angular.isDefined(requestHeaderInfo) && requestHeaderInfo != null) {

        	  angular.forEach(requestHeaderInfo, function(value, key) {

                 console.log("CompareConfigCtrl::serviceRequestHeader::key", key);
             console.log("CompareConfigCtrl::serviceRequestHeader::value", value);
                  requestHeader[key] = value;
              });

          }
      }
  console.log("CompareConfigCtrl::serviceRequestHeader::", JSON.stringify(requestHeader));
      return requestHeader;
  }
  
  // 8th comparison for oper status
  $scope.operationStatus = function(item) {
      var operationStatus = {};
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['oper-status'])) {
          var operStatus = item['service-data']['oper-status'];
          if (angular.isDefined(operStatus) && operStatus != null) {

        	  angular.forEach(operStatus, function(value, key) {

                 console.log("CompareConfigCtrl::operationStatus::key", key);
                console.log("CompareConfigCtrl::operationStatus::value", value);
                  operationStatus[key] = value;
              });

          }
      }
     console.log("CompareConfigCtrl::operationStatus::", JSON.stringify(operationStatus));
      return operationStatus;
  }
  // 9th comparison for request info
 
  $scope.fetchRequestInfo = function(item) {
      var requestInfo = {};
      item = JSON.parse(item.configinfo);
      var item= item['vnf-list'][0];
      if (angular.isDefined(item['service-data']) && angular.isDefined(item['service-data']['request-information'])) {
          var request = item['service-data']['request-information'];
          if (angular.isDefined(request) && request != null) {

        	  angular.forEach(request, function(value, key) {

                 console.log("CompareConfigCtrl::fetchRequestInfo::key", key);
                console.log("CompareConfigCtrl::fetchRequestInfo::value", value);
                  requestInfo[key] = value;
              });

          }
      }
     console.log("CompareConfigCtrl::fetchRequestInfo::", JSON.stringify(requestInfo));
      return requestInfo;
  }
  
}]);