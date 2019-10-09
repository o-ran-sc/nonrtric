<!-- /*
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
    -->
<!-- index.html -->
<!DOCTYPE html>
<html ng-app="myApp">
   <head>
      <meta charset="utf-8">
      <!-- CSS -->
      <script src="./node_modules/jquery.min.js"></script>
      <script src="./node_modules/tether.min.js" ></script>
      <link rel="stylesheet" href="./webjars/bootstrap/3.3.7/css/bootstrap.min.css">
      <script src="./webjars/bootstrap/3.3.7/js/bootstrap.min.js"></script>
      <link rel="stylesheet" href="/static/style/sdnc-style.css">
      <!-- JS -->
      <!-- load angular,date-time picker,pagination,growl and ui-router -->
      <script src = "./node_modules/angular.min.js"></script>
      <script src = "./node_modules/angular-route.min.js"></script>
      <script src="./node_modules/angular-utils-pagination/dirPagination.js"></script>
      <script src="./node_modules/ng-csv/build/ng-csv.min.js"></script>     
      <script src="./node_modules/angular-sanitize.min.js"></script>
      <link rel="stylesheet" href="./node_modules/angularjs-datetime-picker/angularjs-datetime-picker.css" />
      <script src="./node_modules/angularjs-datetime-picker/angularjs-datetime-picker.js"></script>
      <script data-require="jquery@*" data-semver="2.1.4" src="https://code.jquery.com/jquery-2.1.4.js"></script>
      <script src="./node_modules/angular-utils-pagination/dirPagination.js"></script>
      <script src="./js/app.js"></script>
      <script src="./js/sdnc-controller/sdnc-viewreport-controller.js"></script>
      <script src="./js/sdnc-controller/sdnc-validationTest-controller.js"></script>
      <script src="./js/sdnc-services/sdnc-viewReport-service.js"></script>
      <script src="./js/sdnc-services/sdnc-validationTest-service.js"></script>
      <script src="./js/sdnc-controller/sdnc-backupConfig-controller.js"></script>
      <script src="./js/sdnc-controller/sdnc-applyConfig-controller.js"></script>
      <script src="./js/sdnc-controller/sdnc-compareConfig-controller.js"></script>
      <script src="./js/sdnc-services/sdnc-compareConfig-service.js"></script>
      <script src="./node_modules/angular-growl-v2/build/angular-growl.min.js" ></script>
      <script src="./node_modules/ng-ip-address/ngIpAddress.min.js" ></script>
      <link rel="stylesheet" type="text/css" href="./node_modules/angular-growl-v2/build/angular-growl.css">
      <link rel="stylesheet" href="/static/style/w3.css">
      <link type="text/css" href="./webjars/angular-object-diff/1.0.1/dist/angular-object-diff.css" rel='stylesheet'>
      <script type="text/javascript" src="/webjars/angular-object-diff/1.0.1/dist/angular-object-diff.js"></script>
   </head>
   <!-- apply our angular app -->
   <body>
   <div class="container">
      <!-- header page -->
   <div ng-include="'static/views/header.html'"></div>
      <div style="border-style:ridge;height:auto;">          
         <div ng-include="'static/views/tabs.html'" ></div>
         <!-- views will be injected here -->
         <div ng-view></div>
      </div>
      <!-- footer page -->
       <div ng-include="'static/views/footer.html'" ></div>
       </div>
   </body>
</html>