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

import { Component, Inject, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import { ErrorDialogService } from '../../services/ui/error-dialog.service';


@Component({
  selector: 'rd-edit-app-dashboard-user-dialog',
  templateUrl: './edit-dashboard-user-dialog.component.html',
  styleUrls: ['./edit-dashboard-user-dialog.component.scss']
})
export class EditDashboardUserDialogComponent implements OnInit {

  public editUserDialogForm: FormGroup;

  constructor(
    @Inject(MAT_DIALOG_DATA) public data,
    private dialogRef: MatDialogRef<EditDashboardUserDialogComponent>,
    private dashSvc: DashboardService,
    private errorService: ErrorDialogService) { }

  ngOnInit() {
    this.editUserDialogForm = new FormGroup({
      firstName: new FormControl(this.data.firstName , [Validators.required]),
      lastName: new FormControl(this.data.lastName, [Validators.required]),
      status: new FormControl(this.data.status, [Validators.required])
    });
  }

  onCancel() {
    this.dialogRef.close(false);
  }

  public editUser = (FormValue) => {
    if (this.editUserDialogForm.valid) {
      // send the request to backend when it's ready
      const aboutError = 'Not implemented yet';
      this.errorService.displayError(aboutError);
    }
  }

}
