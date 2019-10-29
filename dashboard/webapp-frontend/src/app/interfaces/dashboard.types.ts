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

// Models of data used by Dashboard admin services

export interface DashboardSuccessTransport {
  status: number;
  data: string;
}

export interface EcompRoleFunction {
  name: string;
  code: string;
  type: string;
  action: string;
}

export interface EcompRole {
  id: number;
  name: string;
  [position: number]: EcompRoleFunction;
}

export interface EcompUser {
  orgId?: number;
  managerId?: string;
  firstName?: string;
  middleInitial?: string;
  lastName?: string;
  phone?: string;
  email?: string;
  hrid?: string;
  orgUserId?: string;
  orgCode?: string;
  orgManagerUserId?: string;
  jobTitle?: string;
  loginId: string;
  active: boolean;
  [position: number]: EcompRole;
}
