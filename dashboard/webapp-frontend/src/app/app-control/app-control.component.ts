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
import { HttpErrorResponse, HttpResponse } from '@angular/common/http';
import { Component, OnInit, ViewChild } from '@angular/core';
import { MatSort } from '@angular/material/sort';
import { Router } from '@angular/router';
import { XappControlRow } from '../interfaces/app-mgr.types';
import { AppMgrService } from '../services/app-mgr/app-mgr.service';
import { ConfirmDialogService } from '../services/ui/confirm-dialog.service';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { LoadingDialogService } from '../services/ui/loading-dialog.service';
import { NotificationService } from '../services/ui/notification.service';
import { AppControlAnimations } from './app-control.animations';
import { AppControlDataSource } from './app-control.datasource';
import { finalize } from 'rxjs/operators';

@Component({
  selector: 'rd-app-control',
  templateUrl: './app-control.component.html',
  styleUrls: ['./app-control.component.scss'],
  animations: [AppControlAnimations.messageTrigger]
})
export class AppControlComponent implements OnInit {

  displayedColumns: string[] = ['xapp', 'name', 'status', 'ip', 'port', 'action'];
  dataSource: AppControlDataSource;
  @ViewChild(MatSort, {static: true}) sort: MatSort;

  constructor(
    private appMgrSvc: AppMgrService,
    private router: Router,
    private confirmDialogService: ConfirmDialogService,
    private errorDialogService: ErrorDialogService,
    private loadingDialogService: LoadingDialogService,
    private notificationService: NotificationService) { }

  ngOnInit() {
    this.dataSource = new AppControlDataSource(this.appMgrSvc, this.sort, this.notificationService);
    this.dataSource.loadTable();
  }

  controlApp(app: XappControlRow): void {
    // TODO: identify apps without hardcoding to names
    const acAppPattern0 =  /[Aa][Dd][Mm][Ii][Nn]/;
    const acAppPattern1 =  /[Aa][Dd][Mm][Ii][Ss]{2}[Ii][Oo][Nn]/;
    const anrAppPattern0 = /ANR/;
    const anrAppPattern1 = /[Aa][Uu][Tt][Oo][Mm][Aa][Tt][Ii][Cc]/;
    const anrAppPattern2 = /[Nn][Ee][Ii][Gg][Hh][Bb][Oo][Rr]/;
    if (acAppPattern0.test(app.xapp) || acAppPattern1.test(app.xapp)) {
      this.router.navigate(['/ac']);
    } else if (anrAppPattern0.test(app.xapp) || (anrAppPattern1.test(app.xapp) && anrAppPattern2.test(app.xapp))) {
      this.router.navigate(['/anr']);
    } else {
      this.errorDialogService.displayError('No control available for ' + app.xapp + ' (yet)');
    }
  }

  onUndeployApp(app: XappControlRow): void {
    this.confirmDialogService.openConfirmDialog('Are you sure you want to undeploy App ' + app.xapp + '?')
      .afterClosed().subscribe( (res: boolean) => {
        if (res) {
          this.loadingDialogService.startLoading("Undeploying " + app.xapp);
          this.appMgrSvc.undeployXapp(app.xapp)
            .pipe(
              finalize(() => this.loadingDialogService.stopLoading())
            )
            .subscribe(
            ( httpResponse: HttpResponse<Object>) => {
              // Answers 204/No content on success
              this.notificationService.success('App undeployed successfully!');
              this.dataSource.loadTable();
            },
            ( (her: HttpErrorResponse) => {
              // the error field should have an ErrorTransport object
              let msg = her.message;
              if (her.error && her.error.message) {
                msg = her.error.message;
              }
              this.notificationService.warn('App undeploy failed: ' + msg);
            })
          );
        }
      });
  }

}
