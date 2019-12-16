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

import { MatSort } from '@angular/material';
import { Component, OnInit, ViewChild, Input, AfterViewInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { PolicyType } from '../interfaces/policy.types';
import { PolicyInstanceDataSource } from './policy-instance.datasource';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { NotificationService } from '../services/ui/notification.service';
import { PolicyService } from '../services/policy/policy.service';
import { ConfirmDialogService } from './../services/ui/confirm-dialog.service';
import { PolicyInstance } from '../interfaces/policy.types';
import { PolicyInstanceDialogComponent } from './policy-instance-dialog.component';
import { getPolicyDialogProperties } from './policy-instance-dialog.component';
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UiService } from '../services/ui/ui.service';

@Component({
    selector: 'rd-policy-instance',
    templateUrl: './policy-instance.component.html',
    styleUrls: ['./policy-instance.component.scss']
})


export class PolicyInstanceComponent implements OnInit, AfterViewInit {
    instanceDataSource: PolicyInstanceDataSource;
    @Input() policyType: PolicyType;
    @Input() expanded: Observable<boolean>;
    @ViewChild(MatSort, { static: true }) sort: MatSort;
    darkMode: boolean;

    constructor(
        private policySvc: PolicyService,
        private dialog: MatDialog,
        private errorDialogService: ErrorDialogService,
        private notificationService: NotificationService,
        private confirmDialogService: ConfirmDialogService,
        private ui: UiService) {
    }

    ngOnInit() {
        this.instanceDataSource = new PolicyInstanceDataSource(this.policySvc, this.sort, this.notificationService, this.policyType);
        this.expanded.subscribe((isExpanded: boolean) => this.onExpand(isExpanded));
        this.ui.darkModeState.subscribe((isDark) => {
            this.darkMode = isDark;
        });
    }

    ngAfterViewInit() {
        this.instanceDataSource.sort = this.sort;
    }

    private onExpand(isExpanded: boolean) {
        if (isExpanded) {
            this.instanceDataSource.loadTable();
        }
    }

    modifyInstance(instance: PolicyInstance): void {
        this.policySvc.getPolicy(this.policyType.name, instance.id).subscribe(
            (refreshedJson: any) => {
                instance.json = JSON.stringify(refreshedJson);
                this.dialog.open(PolicyInstanceDialogComponent, getPolicyDialogProperties(this.policyType, instance, this.darkMode));
            },
            (httpError: HttpErrorResponse) => {
                this.notificationService.error('Could not refresh instance ' + httpError.message);
                this.dialog.open(PolicyInstanceDialogComponent, getPolicyDialogProperties(this.policyType, instance, this.darkMode));
            }
        );
    }

    hasInstances(): boolean {
        return this.instanceDataSource.rowCount > 0;
    }

    toLocalTime(utcTime: string): string {
        const date = new Date(utcTime);
        const toutc = date.toUTCString();
        return new Date(toutc + " UTC").toLocaleString();

    }

    deleteInstance(instance: PolicyInstance): void {
        this.confirmDialogService
            .openConfirmDialog('Are you sure you want to delete this policy instance?')
            .afterClosed().subscribe(
                (res: any) => {
                    if (res) {
                        this.policySvc.deletePolicy(this.policyType.name, instance.id)
                            .subscribe(
                                (response: HttpResponse<Object>) => {
                                    switch (response.status) {
                                        case 200:
                                            this.notificationService.success('Delete succeeded!');
                                            this.instanceDataSource.loadTable();
                                            break;
                                        default:
                                            this.notificationService.warn('Delete failed.');
                                    }
                                },
                                (error: HttpErrorResponse) => {
                                    this.errorDialogService.displayError(error.message);
                                });
                    }
                });
    }



}
