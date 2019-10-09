myApp.controller('ApplyConfigCtrl', ['$scope','$window', '$http', 'growl', 'deviceConfigService', 'ObjectDiff', function($scope, $http,$window, growl, deviceConfigService, ObjectDiff) {

    $scope.showResult = false;

    //THIS FUNCTION WILL BE CALLED ON PAGE LOAD
    $scope.getAllVNFFromRc = function() {

        deviceConfigService.getAllVnfIds().then(function(data) {
            if (data != null) {
                console.log(data);
                $scope.objvnfList= data['vnfDisplayList'];
             console.log("CompareConfigCtrl:getAllVNFFromRc called" + $scope.objvnfList);
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
   $scope.getAllVNFFromRc();
   
   
   $scope.ShowResult=false;
   $scope.selectVnf = function(selectedValueVnf) {
	   if (selectedValueVnf != null && selectedValueVnf != "") {
		   $scope.ShowResult=true;
   	var vnfId = selectedValueVnf;
   		$scope.successMessagebool1 = false;
   		$scope.fileContent = '';
   	    $scope.fileSize = 0;
   	    $scope.fileName = '';
   	   
   	    $scope.submit = function () {
   	      var file = document.getElementById("myFileInput").files[0];
   	    $scope.result1={}; 
   	      if (file) {
   	        var aReader = new FileReader();
   	        aReader.readAsText(file, "UTF-8");
   	        aReader.onload = function (evt) {
   	            $scope.fileName = document.getElementById("myFileInput").files[0].name;
   	            $scope.fileSize = document.getElementById("myFileInput").files[0].size;
   	            var id= vnfId;
   	             result1=JSON.parse(aReader.result);
   	            $scope.fileContent = aReader.result.search(id);
   	            $scope.successMessagebool = false;
   	        if(  $scope.fileContent == -1){
   	        	  $scope.errorMessage = "VNF Id is different!!! Select different file and try again";
   	              growl.error($scope.errorMessage, {
   	                  title: 'Error!',
   	                  globalDisableCloseButton: false,
   	                 ttl: 7000,
   	                 disableCountDown: true  
   	              });
   	            }
   	        else{	
   	         $scope.apply();
   	        }
   	           }
   	        aReader.onerror = function (evt) {
   	            $scope.fileContent = "error";
   	        }
   	       
   	      }else{
 	        	$scope.errorMessage = "Please select file!!!!";
	              growl.error($scope.errorMessage, {
	                  title: 'Error!',
	                  globalDisableCloseButton: false,
	                 ttl: 7000,
	                 disableCountDown: true  
	              });
 	        }
   	 
   	       $scope.apply = function() {
   	          if (file) {
   	        	  deviceConfigService.runApplyconfig(vnfId,result1);
   	        	$scope.successMessage = "File uploaded successfully";
	              growl.success($scope.successMessage, {
	                  title: 'Success!',
	                  globalDisableCloseButton: false,
	                  ttl: 7000,
	                  disableCountDown: true  
	             }); 
 	            $scope.successMessagebool1 = true;
   	          }  };
   	    
   	    }; } else {
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
   	    
   	      };
   
   
}]);