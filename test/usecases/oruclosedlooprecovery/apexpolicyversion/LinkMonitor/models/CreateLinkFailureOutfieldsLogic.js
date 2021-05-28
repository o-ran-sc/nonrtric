/*
 * ============LICENSE_START=======================================================
 * Copyright (C) 2021 Nordix Foundation.
 * ================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

executor.logger.info("Task Execution: '"+executor.subject.id+"'. Input Fields: '"+executor.inFields+"'");

var returnValue = true;
var linkFailureInput = executor.inFields.get("LinkFailureInput");
var oruId = linkFailureInput.get("event").get("commonEventHeader").get("sourceName");
var oruOduMap = JSON.parse(executor.parameters.get("ORU-ODU-Map"));

if (oruId in oruOduMap) {
    var oduId = oruOduMap[oruId];
    executor.outFields.put("OruId", oruId);
    executor.outFields.put("OduId", oduId);
    executor.logger.info(executor.outFields);
} else {
    executor.message = "No O-RU found in the config with this ID: " + oruId;
    returnValue = false;
}

returnValue;
