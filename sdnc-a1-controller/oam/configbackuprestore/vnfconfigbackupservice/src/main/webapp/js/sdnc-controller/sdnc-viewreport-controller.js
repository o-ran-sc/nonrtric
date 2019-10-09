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

myApp.controller(
				'ReportController',
				[
						'$scope',
						'$http',
						'$filter',
						'viewReportService',
						'growl',
						function($scope, $http, $filter, viewReportService,
								growl) {
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
							$scope.showData = false;

							$scope.gap = 2;
							$scope.filteredItems = [];
							$scope.groupedItems = [];
							$scope.itemsPerPage = 5;
							$scope.pagedItems = [];
							$scope.currentPage = 0;

							// THIS FUNCTION WILL BE CALLED ON PAGE LOAD
							$scope.getAllVNF = function() {

								viewReportService
										.getAllVNF()
										.then(
												function(data) {
													if (data != null) {
														console.log(data);
														$scope.objvnfList = data;
														console
																.log("ViewConfigCtrl:getAllVNF called"
																		+ $scope.objvnfList);
													} else {
														$scope.warningMessage = "No VNF is eligible for configuration!!!";
														growl
																.error(
																		$scope.warningMessage,
																		{
																			title : 'Warning!',
																			globalDisableCloseButton : false,
																			ttl : 7000,
																			disableCountDown : true
																		});
													}
												});
							};
							$scope.getAllVNF();

							$scope.selectVnf = function(selectedValueVnf) {

								if (selectedValueVnf != null
										&& selectedValueVnf != "") {
									console.log("selectedvnf Value",
											selectedValueVnf);
									var vnfId = selectedValueVnf;
									$scope.ShowResult = true;

								} else {
									$scope.ShowResult = false;
									$scope.showCompare = false;
									$scope.showResult = false;
									$scope.errorMessage = "Please select a VNF!!!";
									growl.error($scope.errorMessage, {
										title : 'Error!',
										globalDisableCloseButton : false,
										ttl : 7000,
										disableCountDown : true
									});
								}
							}

							// THIS FUNCTION WILL BE CALLED WHEN USER CLICK
							// SUBMIT FROM UI
							$scope.getReportsById = function(selectedValueVnf,
									startdate, enddate) {

								$scope.ShowResult = true;
								if (new Date(startdate) > new Date(enddate)) {
									$scope.dateValidation = true;
									$scope.showError = true;
									$scope.errorMessage = "Start date cannot be greated than End date";
									growl.error($scope.errorMessage, {
										title : 'Error!',
										globalDisableCloseButton : false,
										ttl : 7000,
										disableCountDown : true
									});
									return false;
								}
								var date = new Date(startdate);
								if (angular.isDefined(startdate)) {
									$scope.startDate = startdate;
								}
								if (angular.isDefined(enddate)) {
									$scope.endDate = enddate;
								}

								$scope.startdate1 = $filter('date')(
										$scope.startDate, 'dd-MM-yyyy');
								$scope.enddate1 = $filter('date')(
										$scope.endDate, 'dd-MM-yyyy');
								if (startdate != null && enddate != null) {

									// service call to fetch the reports start
									// date,end date,test name
									viewReportService
											.getDataById(selectedValueVnf,
													$scope.startdate1,
													$scope.enddate1)
											.then(
													function(result) {
														console
																.log(
																		"-----------------------------------ReportController::getdata called from controler--",
																		JSON
																				.stringify(result.data));
														if (result.status == 200) {
															if (result.data != null
																	&& result.data.length >= 1) {

																// in case of
																// success,
																// build the
																// model object
																// to store the
																// service
																// output here
																$scope
																		.createTestReportModel(result.data);// result.data.data
															} else {
																$scope.ShowResult = false;
																$scope.showWarning = true;
																$scope.warningMessage = "No result found for specified Date !!";
																growl
																		.warning(
																				$scope.warningMessage,
																				{
																					title : 'Warning!',
																					globalDisableCloseButton : false,
																					ttl : 7000,
																					disableCountDown : true
																				});
															}
														} else {
															$scope.ShowResult = false;
															$scope.showWarning = true;
															$scope.warningMessage = "No result found for specified Date !!";
															growl
																	.warning(
																			$scope.warningMessage,
																			{
																				title : 'Warning!',
																				globalDisableCloseButton : false,
																				ttl : 7000,
																				disableCountDown : true
																			});
														}
													},
													function(response) {
														$scope.ShowError = true;
														$scope.errorMessage = "Something went wrong, Please try again !!";
														growl
																.error(
																		$scope.errorMessage,
																		{
																			title : 'Error!',
																			globalDisableCloseButton : false,
																			ttl : 7000,
																			disableCountDown : true
																		});
														console
																.log(
																		"--ReportController::getdata::Error--",
																		response);
													});
								}
							}

							// FUNCTION WILL BE CALLED WHEN USER CLICK DOWNLOAD
							// FROM UI
							$scope.exportToExcel = function(tableId) { // ex:
																		// '#my-table'
								var exportHref = Excel.tableToExcel(tableId,
										'export');
								$timeout(function() {
									location.href = exportHref;
								}, 100); // trigger download

								console
										.log("--ReportController::exportToexcel--");
							}

							$scope.createTestReportModel = function(result) {

								$scope.showError = false;
								$scope.showWarning = false;
								$scope.objTestReportModel = result;
								$scope.objTestModel = [];

								if ($scope.objTestReportModel.length >= 1) {
									for (var i = 0; i < $scope.objTestReportModel.length; i++) {
										var objTestReport = {};
										objTestReport.vnfname = $scope.objTestReportModel[i].vnfname;
										objTestReport.vnfid = $scope.objTestReportModel[i].vnfid;
										objTestReport.versionNo = $scope.objTestReportModel[i].vnfversion;
										objTestReport.createdAt = $scope.objTestReportModel[i].creationdate;
										objTestReport.updatedAt = $scope.objTestReportModel[i].lastupdated;
										objTestReport.status = $scope.objTestReportModel[i].status;
										objTestReport.Id = $scope.objTestReportModel[i].id;
										// objTestReport.configinfo =
										// $scope.objVersionModel[i].configinfo;
										$scope.objTestModel.push(objTestReport);
										console
												.log(
														"--ReportController::CreateTestReportModel--",
														JSON
																.stringify($scope.objTestModel));

									}
									$scope.showresult = true;
									$scope.pagination = true;
								}
								console
										.log("--ReportController::createTestReportModel::final TestReportModel--"
												+ JSON
														.stringify($scope.objTestModel));
								$scope.csvOrder = [ 'testname', 'timeStamp',
										'status', 'statistics', 'avgTime',
										'result' ];

							}

							// THIS FUNCTION WILL BE CALLED WHEN USER CLICK
							// SUBMIT FROM UI
							$scope.getReports = function(startdate, enddate) {

								$scope.ShowResult = false;
								if (new Date(startdate) > new Date(enddate)) {
									$scope.dateValidation = true;
									$scope.showError = true;
									$scope.errorMessage = "Start date cannot be greated than End date";
									growl.error($scope.errorMessage, {
										title : 'Error!',
										globalDisableCloseButton : false,
										ttl : 7000,
										disableCountDown : true
									});
									return false;
								}
								var date = new Date(startdate);
								/*
								 * if (angular.isDefined(deviceName)) {
								 * $scope.DeviceName = deviceName; }
								 */
								if (angular.isDefined(startdate)) {
									$scope.startDate = startdate;
								}
								if (angular.isDefined(enddate)) {
									$scope.endDate = enddate;
								}

								$scope.startdate1 = $filter('date')(
										$scope.startDate, 'dd-MM-yyyy');
								$scope.enddate1 = $filter('date')(
										$scope.endDate, 'dd-MM-yyyy');
								if (startdate != null && enddate != null) {

									// service call to fetch the reports start
									// date,end date,test name
									viewReportService
											.getData($scope.startdate1,
													$scope.enddate1)
											.then(
													function(result) {
														console
																.log(
																		"-----------------------------------ReportController::getdata called from controler--",
																		JSON
																				.stringify(result.data));
														if (result.status == 200) {
															if (result.data != null
																	&& result.data.length >= 1) {

																// in case of
																// success,
																// build the
																// model object
																// to store the
																// service
																// output here
																$scope
																		.createTestReportModel(result.data);// result.data.data
															} else {
																$scope.ShowResult = false;
																$scope.showWarning = true;
																$scope.warningMessage = "No result found for specified Date !!";
																growl
																		.warning(
																				$scope.warningMessage,
																				{
																					title : 'Warning!',
																					globalDisableCloseButton : false,
																					ttl : 7000,
																					disableCountDown : true
																				});
															}
														} else {
															$scope.ShowResult = false;
															$scope.showWarning = true;
															$scope.warningMessage = "No result found for specified Date !!";
															growl
																	.warning(
																			$scope.warningMessage,
																			{
																				title : 'Warning!',
																				globalDisableCloseButton : false,
																				ttl : 7000,
																				disableCountDown : true
																			});
														}
													},
													function(response) {
														$scope.ShowError = true;
														$scope.errorMessage = "Something went wrong, Please try again !!";
														growl
																.error(
																		$scope.errorMessage,
																		{
																			title : 'Error!',
																			globalDisableCloseButton : false,
																			ttl : 7000,
																			disableCountDown : true
																		});
														console
																.log(
																		"--ReportController::getdata::Error--",
																		response);
													});
								}
							}

							// FUNCTION WILL BE CALLED WHEN USER CLICK DOWNLOAD
							// FROM UI
							$scope.exportToExcel = function(tableId) { // ex:
																		// '#my-table'
								var exportHref = Excel.tableToExcel(tableId,
										'export');
								$timeout(function() {
									location.href = exportHref;
								}, 100); // trigger download

								console
										.log("--ReportController::exportToexcel--");
							}

							$scope.createTestReportModel = function(result) {

								$scope.showError = false;
								$scope.showWarning = false;
								$scope.objTestReportModel = result;
								$scope.objTestModel = [];

								if ($scope.objTestReportModel.length >= 1) {
									for (var i = 0; i < $scope.objTestReportModel.length; i++) {
										var objTestReport = {};
										objTestReport.vnfname = $scope.objTestReportModel[i].vnfname;
										objTestReport.vnfid = $scope.objTestReportModel[i].vnfid;
										objTestReport.versionNo = $scope.objTestReportModel[i].vnfversion;
										objTestReport.createdAt = $scope.objTestReportModel[i].creationdate;
										objTestReport.updatedAt = $scope.objTestReportModel[i].lastupdated;
										objTestReport.status = $scope.objTestReportModel[i].status;
										objTestReport.Id = $scope.objTestReportModel[i].id;
										// objTestReport.configinfo =
										// $scope.objVersionModel[i].configinfo;
										$scope.objTestModel.push(objTestReport);
										console
												.log(
														"--ReportController::CreateTestReportModel--",
														JSON
																.stringify($scope.objTestModel));

									}
									$scope.showresult = true;
									$scope.pagination = true;
								}
								console
										.log("--ReportController::createTestReportModel::final TestReportModel--"
												+ JSON
														.stringify($scope.objTestModel));
								$scope.csvOrder = [ 'testname', 'timeStamp',
										'status', 'statistics', 'avgTime',
										'result' ];

								// init
								$scope.sort = {
									sortingOrder : 'createdAt',
									reverse : false
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
											$scope.objTestModel,
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
									console.log(size, start, end);

									if (size < end) {
										end = size;
										start = size - $scope.gap;
									}
									for (var i = start; i < end; i++) {
										ret.push(i);
									}
									console.log(ret);
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

						} ]);