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

executor.logger.info("Task Selection Execution: '"+executor.subject.id+
    "'. InputFields: '"+executor.inFields+"'");

var linkFailureInput = executor.inFields.get("LinkFailureInput");
var commonEventHeader = linkFailureInput.get("event").get("commonEventHeader");
var domain = commonEventHeader.get("domain");

taskFailure = executor.subject.getTaskKey("CreateLinkFailureOutfieldsTask");
taskCleared = executor.subject.getTaskKey("CreateLinkClearedOutfieldsTask");
taskDefault = executor.subject.getDefaultTaskKey();

if (domain == "fault") {
    var faultFields = linkFailureInput.get("event").get("faultFields");
    var alarmCondition = faultFields.get("alarmCondition");
    var eventSeverity = faultFields.get("eventSeverity");
    if (alarmCondition == "28" && eventSeverity != "NORMAL") {
        taskFailure.copyTo(executor.selectedTask);
    } else if (alarmCondition == "28" && eventSeverity == "NORMAL") {
        taskCleared.copyTo(executor.selectedTask);
    } else {
        taskDefault.copyTo(executor.selectedTask);
    }
} else {
    taskDefault.copyTo(executor.selectedTask);
}

true;
