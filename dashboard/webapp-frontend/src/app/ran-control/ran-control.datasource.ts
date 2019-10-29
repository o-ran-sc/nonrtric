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
import { Observable } from 'rxjs/Observable';
import { BehaviorSubject } from 'rxjs/BehaviorSubject';
import { of } from 'rxjs/observable/of';
import { catchError, finalize } from 'rxjs/operators';
import { E2RanDetails } from '../interfaces/e2-mgr.types';
import { E2ManagerService } from '../services/e2-mgr/e2-mgr.service';
import { NotificationService } from '../services/ui/notification.service';

export class RANControlDataSource extends DataSource<E2RanDetails> {

  private ranControlSubject = new BehaviorSubject<E2RanDetails[]>([]);

  private loadingSubject = new BehaviorSubject<boolean>(false);

  public loading$ = this.loadingSubject.asObservable();

  public rowCount = 1; // hide footer during intial load

  constructor(private e2MgrSvcservice: E2ManagerService,
    private notificationService: NotificationService) {
    super();
  }

  loadTable() {
    this.loadingSubject.next(true);
    this.e2MgrSvcservice.getRan()
      .pipe(
        catchError( (her: HttpErrorResponse) => {
          console.log('RANControlDataSource failed: ' + her.message);
          this.notificationService.error('Failed to get RAN details: ' + her.message);
          return of([]);
        }),
        finalize( () =>  this.loadingSubject.next(false) )
      )
      .subscribe( (ranControl: E2RanDetails[] ) => {
        this.rowCount = ranControl.length;
        this.ranControlSubject.next(ranControl);
      });
  }

  connect(collectionViewer: CollectionViewer): Observable<E2RanDetails[]> {
    return this.ranControlSubject.asObservable();
  }

  disconnect(collectionViewer: CollectionViewer): void {
    this.ranControlSubject.complete();
    this.loadingSubject.complete();
  }

}
