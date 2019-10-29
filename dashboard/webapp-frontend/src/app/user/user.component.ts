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
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { MatSort } from '@angular/material/sort';
import { DashboardService } from '../services/dashboard/dashboard.service';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { EcompUser } from './../interfaces/dashboard.types';
import { NotificationService } from './../services/ui/notification.service';
import { UserDataSource } from './user.datasource';
import { AddDashboardUserDialogComponent } from './add-dashboard-user-dialog/add-dashboard-user-dialog.component';
import { EditDashboardUserDialogComponent } from './edit-dashboard-user-dialog/edit-dashboard-user-dialog.component';
import { UiService } from '../services/ui/ui.service';

@Component({
  selector: 'rd-user',
  templateUrl: './user.component.html',
  styleUrls: ['./user.component.scss']
})

export class UserComponent implements OnInit {

  darkMode: boolean;
  panelClass: string = "";
  displayedColumns: string[] = ['loginId', 'firstName', 'lastName', 'active', 'action'];
  dataSource: UserDataSource;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private dashboardSvc: DashboardService,
    private errorService: ErrorDialogService,
    private notificationService: NotificationService,
    public dialog: MatDialog,
    public ui: UiService) { }

  ngOnInit() {
    this.dataSource = new UserDataSource(this.dashboardSvc, this.sort, this.notificationService);
    this.dataSource.loadTable();
    this.ui.darkModeState.subscribe((isDark) => {
      this.darkMode = isDark;
    });
  }

  editUser(user: EcompUser) {
    if (this.darkMode) {
      this.panelClass = "dark-theme"
    } else {
      this.panelClass = "";
    }
    const dialogRef = this.dialog.open(EditDashboardUserDialogComponent, {
      panelClass: this.panelClass,
      width: '450px',
      data: user
    });
    dialogRef.afterClosed().subscribe(result => {
      this.dataSource.loadTable();
    });
  }

  deleteUser() {
    const aboutError = 'Not implemented (yet).';
    this.errorService.displayError(aboutError);
  }

  addUser() {
    if (this.darkMode) {
      this.panelClass = "dark-theme"
    } else {
      this.panelClass = "";
    }
    const dialogRef = this.dialog.open(AddDashboardUserDialogComponent, {
      panelClass: this.panelClass,
      width: '450px'
    });
    dialogRef.afterClosed().subscribe(result => {
      this.dataSource.loadTable();
    });
  }
}

