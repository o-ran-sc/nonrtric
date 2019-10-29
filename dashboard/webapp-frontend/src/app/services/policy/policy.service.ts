/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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

import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { PolicyType, PolicyInstance, PolicyInstanceAck } from '../../interfaces/policy.types';
import { DashboardSuccessTransport } from '../../interfaces/dashboard.types';

/**
 * Services for calling the policy endpoints.
 */
@Injectable({
    providedIn: 'root'
})
export class PolicyService {

    private basePath = 'api/policy';
    private policyTypePath = 'policytypes';
    private policyPath = 'policies';

    private buildPath(...args: any[]) {
        let result = this.basePath;
        args.forEach(part => {
            result = result + '/' + part;
        });
        return result;
    }

    constructor(private httpClient: HttpClient) {
        // injects to variable httpClient
    }

    /**
     * Gets version details
     * @returns Observable that should yield a String
     */
    getVersion(): Observable<string> {
        const url = this.buildPath('version');
        return this.httpClient.get<DashboardSuccessTransport>(url).pipe(
            // Extract the string here
            map(res => res['data'])
        );
    }

    getPolicyTypes(): Observable<PolicyType[]> {
        const url = this.buildPath(this.policyTypePath);
        return this.httpClient.get<PolicyType[]>(url);
    }

    getPolicyInstances(policyTypeId: number): Observable<PolicyInstance[]> {
        const url = this.buildPath(this.policyTypePath, policyTypeId, this.policyPath);
        return this.httpClient.get<PolicyInstance[]>(url);
    }

    /**
     * Gets policy parameters.
     * @returns Observable that should yield a policy instance
     */
    getPolicy(policyTypeId: number, policyInstanceId: string): Observable<any> {
        const url = this.buildPath(this.policyTypePath, policyTypeId, this.policyPath, policyInstanceId);
        return this.httpClient.get<any>(url);
    }

    /**
     * Creates or replaces policy instance.
     * @param policyTypeId ID of the policy type that the instance will have
     * @param policyInstanceId ID of the instance
     * @param policyJson Json with the policy content
     * @returns Observable that should yield a response code, no data
     */
    putPolicy(policyTypeId: number, policyInstanceId: string, policyJson: string): Observable<any> {
        const url = this.buildPath(this.policyTypePath, policyTypeId, this.policyPath, policyInstanceId);
        return this.httpClient.put<PolicyInstanceAck>(url, policyJson, { observe: 'response' });
    }

    /**
     * Deletes a policy instance.
     * @param policyTypeId
     * @returns Observable that should yield a response code, no data
     */
    deletePolicy(policyTypeId: number, policyInstanceId: string): Observable<any> {
        const url = this.buildPath(this.policyTypePath, policyTypeId, this.policyPath, policyInstanceId);
        return this.httpClient.delete(url, { observe: 'response' });
    }
}
