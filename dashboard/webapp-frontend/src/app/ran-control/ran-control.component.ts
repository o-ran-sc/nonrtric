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
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { finalize } from 'rxjs/operators';
import { E2ManagerService } from '../services/e2-mgr/e2-mgr.service';
import { ConfirmDialogService } from '../services/ui/confirm-dialog.service';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { LoadingDialogService } from '../services/ui/loading-dialog.service';
import { NotificationService } from '../services/ui/notification.service';
import { RanControlConnectDialogComponent } from './ran-connection-dialog.component';
import { RANControlDataSource } from './ran-control.datasource';
import { UiService } from '../services/ui/ui.service';

@Component({
  selector: 'rd-ran-control',
  templateUrl: './ran-control.component.html',
  styleUrls: ['./ran-control.component.scss']
})
export class RanControlComponent implements OnInit {

  darkMode: boolean;
  panelClass: string = "";
  displayedColumns: string[] = ['nbId', 'nodeType', 'ranName', 'ranIp', 'ranPort', 'connectionStatus'];
  dataSource: RANControlDataSource;

  constructor(private e2MgrSvc: E2ManagerService,
    private errorDialogService: ErrorDialogService,
    private confirmDialogService: ConfirmDialogService,
    private notificationService: NotificationService,
    private loadingDialogService: LoadingDialogService,
    public dialog: MatDialog,
    public ui: UiService) { }

  ngOnInit() {
    this.dataSource = new RANControlDataSource(this.e2MgrSvc, this.notificationService);
    this.dataSource.loadTable();
    this.ui.darkModeState.subscribe((isDark) => {
      this.darkMode = isDark;
    });
  }

  setupRANConnection() {
    if (this.darkMode) {
      this.panelClass = "dark-theme";
    } else {
      this.panelClass = "";
    }
    const dialogRef = this.dialog.open(RanControlConnectDialogComponent, {
      panelClass: this.panelClass,
      width: '450px'
    });
    dialogRef.afterClosed()
      .subscribe((result: boolean) => {
      if (result) {
        this.dataSource.loadTable();
      }
    });
  }

  disconnectAllRANConnections() {
    const aboutError = 'Disconnect all RAN Connections Failed: ';
    this.confirmDialogService.openConfirmDialog('Are you sure you want to disconnect all RAN connections?')
      .afterClosed().subscribe( (res: boolean) => {
        if (res) {
          this.loadingDialogService.startLoading("Disconnecting");
          this.e2MgrSvc.nodebPut()
            .pipe(
              finalize(() => this.loadingDialogService.stopLoading())
            )
            .subscribe(
            ( body: any ) => {
              this.notificationService.success('Disconnect succeeded!');
              this.dataSource.loadTable();
            },
            (her: HttpErrorResponse) => {
              // the error field should have an ErrorTransport object
              let msg = her.message;
              if (her.error && her.error.message) {
                msg = her.error.message;
              }
              this.errorDialogService.displayError('Disconnect failed: ' + msg);
            }
          );
        }
      });
  }

}
