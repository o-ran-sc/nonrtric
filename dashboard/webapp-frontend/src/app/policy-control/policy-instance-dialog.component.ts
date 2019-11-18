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
import { animate, state, style, transition, trigger } from '@angular/animations';
import { AfterViewInit, Component, Inject, OnInit, ViewChild } from '@angular/core';
import { MatDialogConfig, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { MatMenuTrigger } from '@angular/material/menu';
import { JsonPointer } from 'angular6-json-schema-form';
import * as uuid from 'uuid';
import { PolicyInstance, PolicyType } from '../interfaces/policy.types';
import { PolicyService } from '../services/policy/policy.service';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { NotificationService } from './../services/ui/notification.service';
import { UiService } from '../services/ui/ui.service';


@Component({
    selector: 'rd-policy-instance-dialog',
    templateUrl: './policy-instance-dialog.component.html',
    styleUrls: ['./policy-instance-dialog.component.scss'],
    animations: [
        trigger('expandSection', [
            state('in', style({ height: '*' })),
            transition(':enter', [
                style({ height: 0 }), animate(100),
            ]),
            transition(':leave', [
                style({ height: '*' }),
                animate(100, style({ height: 0 })),
            ]),
        ]),
    ],
})
export class PolicyInstanceDialogComponent implements OnInit, AfterViewInit {

    formActive = false;
    isVisible = {
        form: true,
        json: false,
        schema: false
    };

    jsonFormStatusMessage = 'Loading form...';
    jsonSchemaObject: any = {};
    jsonObject: any = {};


    jsonFormOptions: any = {
        addSubmit: false, // Add a submit button if layout does not have one
        debug: false, // Don't show inline debugging information
        loadExternalAssets: true, // Load external css and JavaScript for frameworks
        returnEmptyFields: false, // Don't return values for empty input fields
        setSchemaDefaults: true, // Always use schema defaults for empty fields
        defautWidgetOptions: { feedback: true }, // Show inline feedback icons
    };

    liveFormData: any = {};
    formValidationErrors: any;
    formIsValid = false;


    @ViewChild(MatMenuTrigger, { static: true }) menuTrigger: MatMenuTrigger;

    public policyInstanceId: string;
    public policyTypeName: string;
    darkMode: boolean;
    private policyTypeId: number;


    constructor(
        private dataService: PolicyService,
        private errorService: ErrorDialogService,
        private notificationService: NotificationService,
        @Inject(MAT_DIALOG_DATA) private data,
        private dialogRef: MatDialogRef<PolicyInstanceDialogComponent>,
        private ui: UiService) {
        this.formActive = false;
        this.policyInstanceId = this.data.instanceId;
        this.policyTypeName = this.data.name;
        this.policyTypeId = this.data.policyTypeId;
        this.parseJson(data.createSchema, data.instanceJson);
    }

    ngOnInit() {
        this.jsonFormStatusMessage = 'Init';
        this.formActive = true;
        this.ui.darkModeState.subscribe((isDark) => {
            this.darkMode = isDark;
        });
    }

    ngAfterViewInit() {
    }

    onSubmit() {
        if (this.policyInstanceId == null) {
            this.policyInstanceId = uuid.v4();
        }
        const policyJson: string = this.prettyLiveFormData;
        const self: PolicyInstanceDialogComponent = this;
        this.dataService.putPolicy(this.policyTypeId, this.policyInstanceId, policyJson).subscribe(
            {
                next(value) {
                    self.notificationService.success('Policy ' + self.policyTypeName + ':' + self.policyInstanceId + ' submitted');
                },
                error(error) {
                    self.errorService.displayError('updatePolicy failed: ' + error.message);
                },
                complete() { }
            });
    }

    onClose() {
        this.dialogRef.close();
    }

    public onChanges(data: any) {
        this.liveFormData = data;
    }

    get prettyLiveFormData() {
        return JSON.stringify(this.liveFormData, null, 2);
    }

    get schemaAsString() {
        return JSON.stringify(this.jsonSchemaObject, null, 2);
    }

    get jsonAsString() {
        return JSON.stringify(this.jsonObject, null, 2);
    }

    isValid(isValid: boolean): void {
        this.formIsValid = isValid;
    }

    validationErrors(data: any): void {
        this.formValidationErrors = data;
    }

    get prettyValidationErrors() {
        if (!this.formValidationErrors) { return null; }
        const errorArray = [];
        for (const error of this.formValidationErrors) {
            const message = error.message;
            const dataPathArray = JsonPointer.parse(error.dataPath);
            if (dataPathArray.length) {
                let field = dataPathArray[0];
                for (let i = 1; i < dataPathArray.length; i++) {
                    const key = dataPathArray[i];
                    field += /^\d+$/.test(key) ? `[${key}]` : `.${key}`;
                }
                errorArray.push(`${field}: ${message}`);
            } else {
                errorArray.push(message);
            }
        }
        return errorArray.join('<br>');
    }

    private parseJson(createSchema: string, instanceJson: string): void {
        try {
            this.jsonSchemaObject = JSON.parse(createSchema);
            if (this.data.instanceJson != null) {
                this.jsonObject = JSON.parse(instanceJson);
            }
        } catch (jsonError) {
            this.jsonFormStatusMessage =
                'Invalid JSON\n' +
                'parser returned:\n\n' + jsonError;
            return;
        }
    }

    public toggleVisible(item: string) {
        this.isVisible[item] = !this.isVisible[item];
    }
}

export function getPolicyDialogProperties(policyType: PolicyType, instance: PolicyInstance, darkMode: boolean): MatDialogConfig {
    const policyTypeId = policyType.policy_type_id;
    const createSchema = policyType.create_schema;
    const instanceId = instance ? instance.instanceId : null;
    const instanceJson = instance ? instance.instance : null;
    const name = policyType.name;
    return {
        maxWidth: '1200px',
        maxHeight: '900px',
        width: '900px',
        role: 'dialog',
        disableClose: false,
        panelClass: darkMode ? 'dark-theme' : '',
        data: {
            policyTypeId,
            createSchema,
            instanceId,
            instanceJson,
            name
        }
    };
}

