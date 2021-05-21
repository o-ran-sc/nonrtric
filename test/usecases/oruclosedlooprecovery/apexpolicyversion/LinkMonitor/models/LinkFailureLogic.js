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

var linkFailureOutput = executor.subject.getOutFieldSchemaHelper("LinkFailureOutput").createNewInstance();

var oruId = executor.inFields.get("OruId");
var oduId = executor.inFields.get("OduId");

var unlockMessageArray = new java.util.ArrayList();
for (var i = 0; i < 1; i++) {
    unlockMessageArray.add({
        "name" : oruId,
        "administrative_DasH_state" : "UNLOCKED"
    });
}

linkFailureOutput.put("o_DasH_ran_DasH_sc_DasH_du_DasH_hello_DasH_world_ColoN_du_DasH_to_DasH_ru_DasH_connection", unlockMessageArray);
executor.outFields.put("LinkFailureOutput", linkFailureOutput.toString());

executor.getExecutionProperties().setProperty("OduId", oduId);
executor.getExecutionProperties().setProperty("OruId", oruId);

executor.logger.info(executor.outFields);

true;
