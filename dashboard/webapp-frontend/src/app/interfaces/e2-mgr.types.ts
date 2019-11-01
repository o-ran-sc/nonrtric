/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 AT&T Intellectual Property
 * %%
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
 * ========================LICENSE_END===================================
 */

// Models of data used by the E2 Manager

export interface E2SetupRequest {
  ranName: string;
  ranIp: string;
  ranPort: string;
}

export interface E2ErrorResponse {
  errorCode: string;
  errorMessage: string;
}

export interface E2NodebIdentityGlobalNbId {
  nbId: string;
  plmnId: string;
}

export interface E2NodebIdentity {
  inventoryName: string;
  globalNbId: E2NodebIdentityGlobalNbId;
}

export interface E2GetNodebResponse {
  connectionStatus: string; // actually one-of, but model as string
  enb: object; // don't model this until needed
  failureType: string; // actually one-of, but model as string
  gnb: object; // don't model this until needed
  ip: string;
  nodeType: string; // actually one-of, but model as string
  port: number; // actually integer
  ranName: string;
  setupFailure: object; // don't model this until needed
}

export interface E2RanDetails {
  nodebIdentity: E2NodebIdentity;
  nodebStatus: E2GetNodebResponse;
}

export interface RanDialogFormData {
  ranIp: string;
  ranName: string;
  ranPort: string;
  ranType: string;
}
