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

import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { DashboardService } from '../../services/dashboard/dashboard.service';
import { ErrorDialogService } from '../../services/ui/error-dialog.service';

@Component({
  selector: 'add-dashboard-user-dialog',
  templateUrl: './add-dashboard-user-dialog.component.html',
  styleUrls: ['./add-dashboard-user-dialog.component.scss']
})
export class AddDashboardUserDialogComponent implements OnInit {

  public addUserDialogForm: FormGroup;

  constructor(
    private dialogRef: MatDialogRef<AddDashboardUserDialogComponent>,
    private dashSvc: DashboardService,
    private errorService: ErrorDialogService) { }

  ngOnInit() {
    this.addUserDialogForm = new FormGroup({
      firstName: new FormControl('', [Validators.required]),
      lastName: new FormControl('', [Validators.required]),
      status: new FormControl('', [Validators.required])
    });
  }

  onCancel() {
    this.dialogRef.close(false);
  }

  public addUser = (FormValue) => {
    if (this.addUserDialogForm.valid) {
      // send the request to backend when it's ready
      const aboutError = 'Not implemented yet';
      this.errorService.displayError(aboutError);
    }
  }

}
