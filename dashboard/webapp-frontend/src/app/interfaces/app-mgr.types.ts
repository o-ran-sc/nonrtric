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

// Models of data used by the App Manager

export interface XMSubscription {
  eventType: string;
  id: string;
  maxRetries: number;
  retryTimer: number;
  targetUrl: string;
}

/**
 * Name is the only required field
 */
export interface XMXappInfo {
  name: string;
  configName?: string;
  namespace?: string;
  serviceName?: string;
  imageRepo?: string;
  hostname?: string;
}

export interface XMXappInstance {
  ip: string;
  name: string;
  port: number;
  status: string;
  rxMessages: Array<string>;
  txMessages: Array<string>;
}

export interface XMDeployableApp {
  name: string;
  version: string;
}

export interface XMDeployedApp {
  name: string;
  status: string;
  version: string;
  instances: Array<XMXappInstance>;
}

export interface XappControlRow {
  xapp: string;
  instance: XMXappInstance;
}
