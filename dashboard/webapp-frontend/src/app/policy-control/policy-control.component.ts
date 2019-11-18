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
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';
import { animate, state, style, transition, trigger } from '@angular/animations';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';

import { PolicyService } from '../services/policy/policy.service';
import { PolicyType } from '../interfaces/policy.types';
import { PolicyTypeDataSource } from './policy-type.datasource';
import { PolicyInstanceDataSource } from './policy-instance.datasource';
import { getPolicyDialogProperties } from './policy-instance-dialog.component';
import { PolicyInstanceDialogComponent } from './policy-instance-dialog.component';
import { PolicyInstance } from '../interfaces/policy.types';
import { NotificationService } from '../services/ui/notification.service';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { ConfirmDialogService } from './../services/ui/confirm-dialog.service';
import { Subject } from 'rxjs';
import { UiService } from '../services/ui/ui.service';

class PolicyTypeInfo {
    constructor(public type: PolicyType, public isExpanded: boolean) { }

    isExpandedObservers: Subject<boolean> = new Subject<boolean>();
}

@Component({
    selector: 'rd-policy-control',
    templateUrl: './policy-control.component.html',
    styleUrls: ['./policy-control.component.scss'],
    animations: [
        trigger('detailExpand', [
            state('collapsed', style({ height: '0px', minHeight: '0', visibility: 'hidden' })),
            state('expanded', style({ height: '*', visibility: 'visible' })),
            transition('expanded <=> collapsed', animate('225ms cubic-bezier(0.4, 0.0, 0.2, 1)')),
        ]),
    ],
})
export class PolicyControlComponent implements OnInit {

    policyTypesDataSource: PolicyTypeDataSource;
    @ViewChild(MatSort, { static: true }) sort: MatSort;

    expandedTypes = new Map<string, PolicyTypeInfo>();
    darkMode: boolean;

    constructor(
        private policySvc: PolicyService,
        private dialog: MatDialog,
        private errorDialogService: ErrorDialogService,
        private notificationService: NotificationService,
        private confirmDialogService: ConfirmDialogService,
        private ui: UiService) { }

    ngOnInit() {
        this.policyTypesDataSource = new PolicyTypeDataSource(this.policySvc, this.sort, this.notificationService);
        this.policyTypesDataSource.loadTable();
        this.ui.darkModeState.subscribe((isDark) => {
            this.darkMode = isDark;
        });
    }

    createPolicyInstance(policyType: PolicyType): void {
        const dialogRef = this.dialog.open(PolicyInstanceDialogComponent, getPolicyDialogProperties(policyType, null, this.darkMode));
        const info: PolicyTypeInfo = this.getPolicyTypeInfo(policyType);
        dialogRef.afterClosed().subscribe(
            (result: any) => {
                info.isExpandedObservers.next(info.isExpanded);
            }
        );
    }

    toggleListInstances(policyType: PolicyType): void {
        const info = this.getPolicyTypeInfo(policyType);
        info.isExpanded = !info.isExpanded;
        info.isExpandedObservers.next(info.isExpanded);
    }

    getPolicyTypeInfo(policyType: PolicyType): PolicyTypeInfo {
        let info: PolicyTypeInfo = this.expandedTypes.get(policyType.name);
        if (!info) {
            info = new PolicyTypeInfo(policyType, false);
            this.expandedTypes.set(policyType.name, info);
        }
        return info;
    }

    isInstancesShown(policyType: PolicyType): boolean {
        return this.getPolicyTypeInfo(policyType).isExpanded;
    }

    getPolicyTypeName(type: PolicyType): string {
        const schema = JSON.parse(type.create_schema);
        if (schema.title) {
            return schema.title;
        }
        return type.name;
    }

    getObservable(policyType: PolicyType): Subject<boolean> {
        return this.getPolicyTypeInfo(policyType).isExpandedObservers;
    }
}
