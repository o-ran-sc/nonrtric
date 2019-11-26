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

import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSort } from '@angular/material';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { merge } from 'rxjs';
import { of } from 'rxjs/observable/of';
import { catchError, finalize, map } from 'rxjs/operators';
import { PolicyType } from '../interfaces/policy.types';
import { PolicyService } from '../services/policy/policy.service';
import { NotificationService } from '../services/ui/notification.service';

export class PolicyTypeDataSource extends DataSource<PolicyType> {

    private policyTypeSubject = new BehaviorSubject<PolicyType[]>([]);

    private loadingSubject = new BehaviorSubject<boolean>(false);

    public loading$ = this.loadingSubject.asObservable();

    public rowCount = 1; // hide footer during intial load

    constructor(private policySvc: PolicyService,
        private sort: MatSort,
        private notificationService: NotificationService) {
        super();
    }

    loadTable() {
        this.loadingSubject.next(true);
        this.policySvc.getPolicyTypes()
            .pipe(
                catchError((her: HttpErrorResponse) => {
                    this.notificationService.error('Failed to get policy types: ' + her.message);
                    return of([]);
                }),
                finalize(() => this.loadingSubject.next(false))
            )
            .subscribe((types: PolicyType[]) => {
                this.rowCount = types.length;
                for (let i = 0; i < types.length; i++) {
                    const policyType = types[i];
                    try {
                        policyType.schemaObject = JSON.parse(policyType.schema);
                    } catch (jsonError) {
                        console.error('Could not parse schema: ' + policyType.schema);
                        policyType.schemaObject = { description: 'Incorrect schema: ' + jsonError };
                    }
                }
                this.policyTypeSubject.next(types);
            });
    }

    connect(collectionViewer: CollectionViewer): Observable<PolicyType[]> {
        const dataMutations = [
            this.policyTypeSubject.asObservable(),
            this.sort.sortChange
        ];
        return merge(...dataMutations).pipe(map(() => {
            return this.getSortedData([...this.policyTypeSubject.getValue()]);
        }));
    }

    disconnect(collectionViewer: CollectionViewer): void {
        this.policyTypeSubject.complete();
        this.loadingSubject.complete();
    }

    private getSortedData(data: PolicyType[]) {
        if (!this.sort.active || this.sort.direction === '') {
            return data;
        }

        return data.sort((a, b) => {
            const isAsc = this.sort.direction === 'asc';
            switch (this.sort.active) {
                case 'name': return compare(a.name, b.name, isAsc);
                default: return 0;
            }
        });
    }
}

function compare(a: any, b: any, isAsc: boolean) {
    return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}
