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

import { CollectionViewer, DataSource } from '@angular/cdk/collections';
import { HttpErrorResponse } from '@angular/common/http';
import { MatSort } from '@angular/material/sort';
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { merge } from 'rxjs';
import { of } from 'rxjs/observable/of';
import { catchError, finalize, map } from 'rxjs/operators';
import { XappControlRow, XMDeployedApp, XMXappInstance } from '../interfaces/app-mgr.types';
import { AppMgrService } from '../services/app-mgr/app-mgr.service';
import { NotificationService } from '../services/ui/notification.service';

export class AppControlDataSource extends DataSource<XappControlRow> {

  private appControlSubject = new BehaviorSubject<XappControlRow[]>([]);

  private loadingSubject = new BehaviorSubject<boolean>(false);

  public loading$ = this.loadingSubject.asObservable();

  public rowCount = 1; // hide footer during intial load

  private emptyInstances: XMXappInstance =
    { ip: null,
      name: null,
      port: null,
      status: null,
      rxMessages: [],
      txMessages: [],
    };

  constructor(private appMgrSvc: AppMgrService,
    private sort: MatSort,
    private notificationService: NotificationService) {
    super();
  }

  loadTable() {
    this.loadingSubject.next(true);
    this.appMgrSvc.getDeployed()
      .pipe(
        catchError( (her: HttpErrorResponse) => {
          console.log('AppControlDataSource failed: ' + her.message);
          this.notificationService.error('Failed to get applications: ' + her.message);
          return of([]);
        }),
        finalize(() => this.loadingSubject.next(false))
      )
      .subscribe( (xApps: XMDeployedApp[]) => {
        this.rowCount = xApps.length;
        const flattenedApps = this.flatten(xApps);
        this.appControlSubject.next(flattenedApps);
      });
  }

  connect(collectionViewer: CollectionViewer): Observable<XappControlRow[]> {
    const dataMutations = [
      this.appControlSubject.asObservable(),
      this.sort.sortChange
    ];
    return merge(...dataMutations).pipe(map(() => {
      return this.getSortedData([...this.appControlSubject.getValue()]);
    }));
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.appControlSubject.complete();
    this.loadingSubject.complete();
  }

  private flatten(allxappdata: XMDeployedApp[]): XappControlRow[]  {
    const xAppInstances: XappControlRow[] = [];
    for (const xapp of allxappdata) {
      if (!xapp.instances) {
        const row: XappControlRow = {
          xapp: xapp.name,
          instance: this.emptyInstances
        };
        xAppInstances.push(row);
      } else {
        for (const ins of xapp.instances) {
          const row: XappControlRow = {
            xapp: xapp.name,
            instance: ins
          };
          xAppInstances.push(row);
        }
      }
    }
    return xAppInstances;
  }

  private getSortedData(data: XappControlRow[]) {
    if (!this.sort.active || this.sort.direction === '') {
      return data;
    }

    return data.sort((a, b) => {
      const isAsc = this.sort.direction === 'asc';
      switch (this.sort.active) {
        case 'xapp': return compare(a.xapp, b.xapp, isAsc);
        case 'name': return compare(a.instance.name, b.instance.name, isAsc);
        case 'status': return compare(a.instance.status, b.instance.status, isAsc);
        case 'ip': return compare(a.instance.ip, b.instance.ip, isAsc);
        case 'port': return compare(a.instance.port, b.instance.port, isAsc);
        default: return 0;
      }
    });
  }
}

function compare(a: any, b: any, isAsc: boolean) {
  return (a < b ? -1 : 1) * (isAsc ? 1 : -1);
}
