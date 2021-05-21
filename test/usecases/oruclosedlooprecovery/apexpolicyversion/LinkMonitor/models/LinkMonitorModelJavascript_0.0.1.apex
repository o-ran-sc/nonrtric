#-------------------------------------------------------------------------------
# ============LICENSE_START=======================================================
# Copyright (C) 2021 Nordix Foundation.
# ================================================================================
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
# SPDX-License-Identifier: Apache-2.0
# ============LICENSE_END=========================================================
#-------------------------------------------------------------------------------

##### Model #####

model create name=LinkMonitorModel version=0.0.1 uuid=540226fb-55ee-4f0e-a444-983a0494818e description="This is the Apex Policy Model for link monitoring."


##### Schemas #####

schema create name=LinkFailureInputSchema flavour=Avro schema=LS
#MACROFILE:"examples/LinkMonitor/schemas/LinkFailureInputSchema.avsc"
LE
schema create name=LinkFailureOutputSchema flavour=Avro schema=LS
#MACROFILE:"examples/LinkMonitor/schemas/LinkFailureOutputSchema.avsc"
LE
schema create name=MessageSchema description="A schema for messages from apex" flavour=Java schema=java.lang.String
schema create name=OruIdSchema description="A schema for O-RU-ID" flavour=Java schema=java.lang.String
schema create name=OduIdSchema description="A schema for O-DU-ID" flavour=Java schema=java.lang.String


##### Tasks #####

task create name=LinkFailureTask version=0.0.1 uuid=3351b0f4-cf06-4fa2-8823-edf67bd30223 description=LS
This task updates the config for O-RU when link failure is detected.
LE
task inputfield create name=LinkFailureTask fieldName=OruId schemaName=OruIdSchema
task inputfield create name=LinkFailureTask fieldName=OduId schemaName=OduIdSchema
task outputfield create name=LinkFailureTask fieldName=LinkFailureOutput schemaName=LinkFailureOutputSchema
task logic create name=LinkFailureTask logicFlavour=JAVASCRIPT logic=LS
#MACROFILE:"examples/LinkMonitor/models/LinkFailureLogic.js"
LE

task create name=LinkClearedTask version=0.0.1 uuid=eecfde90-896c-4343-8f9c-2603ced94e2d description=LS
This task sends a message to the output when link failure is cleared.
LE
task inputfield create name=LinkClearedTask fieldName=OruId schemaName=OruIdSchema
task outputfield create name=LinkClearedTask fieldName=message schemaName=MessageSchema
task logic create name=LinkClearedTask logicFlavour=JAVASCRIPT logic=LS
#MACROFILE:"examples/LinkMonitor/models/LinkClearedLogic.js"
LE

task create name=NoPolicyDefinedTask version=0.0.1 uuid=d48b619e-d00d-4008-b884-02d76ea4350b description=LS
This task sends a message to the output when an event is received for which no policy has been defined.
LE
task inputfield create name=NoPolicyDefinedTask fieldName=LinkFailureInput schemaName=LinkFailureInputSchema
task outputfield create name=NoPolicyDefinedTask fieldName=message schemaName=MessageSchema
task logic create name=NoPolicyDefinedTask logicFlavour=JAVASCRIPT logic=LS
#MACROFILE:"examples/LinkMonitor/models/NoPolicyDefinedLogic.js"
LE

task create name=CreateLinkClearedOutfieldsTask version=0.0.1 uuid=fd594e88-411d-4a94-b2be-697b3a0d7adf description=LS
This task creates the output fields when link failure is cleared.
LE
task inputfield create name=CreateLinkClearedOutfieldsTask fieldName=LinkFailureInput schemaName=LinkFailureInputSchema
task outputfield create name=CreateLinkClearedOutfieldsTask fieldName=OruId schemaName=OruIdSchema
task logic create name=CreateLinkClearedOutfieldsTask logicFlavour=JAVASCRIPT logic=LS
#MACROFILE:"examples/LinkMonitor/models/CreateLinkClearedOutfieldsLogic.js"
LE

task create name=CreateLinkFailureOutfieldsTask version=0.0.1 uuid=ac3d9842-80af-4a98-951c-bd79a431c613 description=LS
This task the output fields when link failure is detected.
LE
task inputfield create name=CreateLinkFailureOutfieldsTask fieldName=LinkFailureInput schemaName=LinkFailureInputSchema
task outputfield create name=CreateLinkFailureOutfieldsTask fieldName=OruId schemaName=OruIdSchema
task outputfield create name=CreateLinkFailureOutfieldsTask fieldName=OduId schemaName=OduIdSchema
task logic create name=CreateLinkFailureOutfieldsTask logicFlavour=JAVASCRIPT logic=LS
#MACROFILE:"examples/LinkMonitor/models/CreateLinkFailureOutfieldsLogic.js"
LE


##### Events #####

event create name=LinkFailureInputEvent version=0.0.1 uuid=c4500941-3f98-4080-a9cc-5b9753ed050b source=DMAAP target=APEX
event parameter create name=LinkFailureInputEvent parName=LinkFailureInput schemaName=LinkFailureInputSchema

event create name=LinkFailureOutputEvent version=0.0.1 uuid=4f04aa98-e917-4f4a-882a-c75ba5a99374 source="APEX" target="OAM"
event parameter create name=LinkFailureOutputEvent parName=LinkFailureOutput schemaName=LinkFailureOutputSchema

event create name=ApexMessageOutputEvent version=0.0.1 uuid=cca47d74-7754-4a61-b163-ca31f66b157b source="APEX" target="APEX"
event parameter create name=ApexMessageOutputEvent parName=message schemaName=MessageSchema

event create name=CreateLinkClearedOutfieldsEvent version=0.0.1 source="APEX" target="APEX"
event parameter create name=CreateLinkClearedOutfieldsEvent parName=OruId schemaName=OruIdSchema

event create name=CreateLinkFailureOutfieldsEvent version=0.0.1 source="APEX" target="APEX"
event parameter create name=CreateLinkFailureOutfieldsEvent parName=OruId schemaName=OruIdSchema
event parameter create name=CreateLinkFailureOutfieldsEvent parName=OduId schemaName=OduIdSchema


##### Policy #####

policy create name=LinkMonitorPolicy version=0.0.1 uuid=6c5e410f-489a-46ff-964e-982ce6e8b6d0 template=Freestyle firstState=LinkFailureOrClearedState


##### States #####

policy state create name=LinkMonitorPolicy stateName=LinkFailureState triggerName=CreateLinkFailureOutfieldsEvent defaultTaskName=LinkFailureTask
policy state output create name=LinkMonitorPolicy stateName=LinkFailureState outputName=LinkFailureLogic_Output_Direct eventName=LinkFailureOutputEvent nextState=NULL
policy state taskref create name=LinkMonitorPolicy stateName=LinkFailureState taskLocalName=LinkFailureTask taskName=LinkFailureTask outputType=DIRECT outputName=LinkFailureLogic_Output_Direct

policy state create name=LinkMonitorPolicy stateName=LinkClearedState triggerName=CreateLinkClearedOutfieldsEvent defaultTaskName=LinkClearedTask
policy state output create name=LinkMonitorPolicy stateName=LinkClearedState outputName=LinkClearedLogic_Output_Direct eventName=ApexMessageOutputEvent nextState=NULL
policy state taskref create name=LinkMonitorPolicy stateName=LinkClearedState taskLocalName=LinkClearedTask taskName=LinkClearedTask outputType=DIRECT outputName=LinkClearedLogic_Output_Direct

policy state create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState triggerName=LinkFailureInputEvent defaultTaskName=NoPolicyDefinedTask
policy state output create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState outputName=CreateLinkFailureOutfieldsLogic_Output_Direct eventName=CreateLinkFailureOutfieldsEvent nextState=LinkFailureState
policy state taskref create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState taskLocalName=CreateLinkFailureOutfieldsTask taskName=CreateLinkFailureOutfieldsTask outputType=DIRECT outputName=CreateLinkFailureOutfieldsLogic_Output_Direct
policy state output create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState outputName=CreateLinkClearedOutfieldsLogic_Output_Direct eventName=CreateLinkClearedOutfieldsEvent nextState=LinkClearedState
policy state taskref create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState taskLocalName=CreateLinkClearedOutfieldsTask taskName=CreateLinkClearedOutfieldsTask outputType=DIRECT outputName=CreateLinkClearedOutfieldsLogic_Output_Direct
policy state output create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState outputName=NoPolicyDefinedLogic_Output_Direct eventName=ApexMessageOutputEvent nextState=NULL
policy state taskref create name=LinkMonitorPolicy stateName=LinkFailureOrClearedState taskLocalName=NoPolicyDefinedTask taskName=NoPolicyDefinedTask outputType=DIRECT outputName=NoPolicyDefinedLogic_Output_Direct
policy state selecttasklogic create name=LinkMonitorPolicy version=0.0.1 stateName=LinkFailureOrClearedState logicFlavour=JAVASCRIPT logic=LS
#MACROFILE:"examples/LinkMonitor/models/TaskSelectionLogic.js"
LE
