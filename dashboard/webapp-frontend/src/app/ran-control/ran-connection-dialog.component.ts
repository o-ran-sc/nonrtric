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
import { Component, OnInit } from '@angular/core';
import { FormControl, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef } from '@angular/material/dialog';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { E2SetupRequest, RanDialogFormData } from '../interfaces/e2-mgr.types';
import { E2ManagerService } from '../services/e2-mgr/e2-mgr.service';
import { ErrorDialogService } from '../services/ui/error-dialog.service';
import { LoadingDialogService } from '../services/ui/loading-dialog.service';
import { NotificationService } from '../services/ui/notification.service';

@Component({
  selector: 'rd-ran-control-connect-dialog',
  templateUrl: './ran-connection-dialog.component.html',
  styleUrls: ['./ran-connection-dialog.component.scss']
})

export class RanControlConnectDialogComponent implements OnInit {

  public ranDialogForm: FormGroup;
  public processing = false;

  constructor(
    private dialogRef: MatDialogRef<RanControlConnectDialogComponent>,
    private service: E2ManagerService,
    private errorService: ErrorDialogService,
    private loadingDialogService: LoadingDialogService,
    private notifService: NotificationService) {
    // opens with empty fields; accepts no data to display
  }

  ngOnInit() {
    const namePattern = /^([A-Z0-9])+$/;
    // tslint:disable-next-line:max-line-length
    const ipPattern = /((((([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5]))$)|(^((([0-9A-Fa-f]{1,4}:){7}([0-9A-Fa-f]{1,4}|:))|(([0-9A-Fa-f]{1,4}:){6}(:[0-9A-Fa-f]{1,4}|((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){5}(((:[0-9A-Fa-f]{1,4}){1,2})|:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3})|:))|(([0-9A-Fa-f]{1,4}:){4}(((:[0-9A-Fa-f]{1,4}){1,3})|((:[0-9A-Fa-f]{1,4})?:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){3}(((:[0-9A-Fa-f]{1,4}){1,4})|((:[0-9A-Fa-f]{1,4}){0,2}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){2}(((:[0-9A-Fa-f]{1,4}){1,5})|((:[0-9A-Fa-f]{1,4}){0,3}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(([0-9A-Fa-f]{1,4}:){1}(((:[0-9A-Fa-f]{1,4}){1,6})|((:[0-9A-Fa-f]{1,4}){0,4}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:))|(:(((:[0-9A-Fa-f]{1,4}){1,7})|((:[0-9A-Fa-f]{1,4}){0,5}:((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)){3}))|:)))(%.+)?$))/;
    const portPattern = /^([0-9]{1,4}|[1-5][0-9]{4}|6[0-4][0-9]{3}|65[0-4][0-9]{2}|655[0-2][0-9]|6553[0-5])$/;
    this.ranDialogForm = new FormGroup({
      ranType: new FormControl('endc'),
      ranName: new FormControl('', [Validators.required, Validators.pattern(namePattern)]),
      ranIp: new FormControl('', [Validators.required, Validators.pattern(ipPattern)]),
      ranPort: new FormControl('', [Validators.required, Validators.pattern(portPattern)])
    });
  }

  onCancel() {
    this.dialogRef.close(false);
  }

  setupConnection = (ranFormValue: RanDialogFormData) => {
    if (!this.ranDialogForm.valid) {
      // should never happen
      return;
    }
    this.processing = true;
    const setupRequest: E2SetupRequest = {
      ranName: ranFormValue.ranName.trim(),
      ranIp: ranFormValue.ranIp.trim(),
      ranPort: ranFormValue.ranPort.trim()
    };
    this.loadingDialogService.startLoading('Setting up connection');
    let observable: Observable<HttpResponse<Object>>;
    if (ranFormValue.ranType === 'endc') {
      observable = this.service.endcSetup(setupRequest);
    } else {
      observable = this.service.x2Setup(setupRequest);
    }
    observable
      .pipe(
        finalize(() => this.loadingDialogService.stopLoading())
      )
      .subscribe(
        (response: any) => {
          this.processing = false;
          this.notifService.success('Connect request sent!');
          this.dialogRef.close(true);
        },
        ((her: HttpErrorResponse) => {
          this.processing = false;
          // the error field carries the server's response
          let msg = her.message;
          if (her.error && her.error.message) {
            msg = her.error.message;
          }
          this.errorService.displayError('Connect request failed: ' + msg);
          // keep the dialog open
        })
      );
  }

  hasError(controlName: string, errorName: string) {
    if (this.ranDialogForm.controls[controlName].hasError(errorName)) {
      return true;
    }
    return false;
  }

  validateControl(controlName: string) {
    if (this.ranDialogForm.controls[controlName].invalid && this.ranDialogForm.controls[controlName].touched) {
      return true;
    }
    return false;
  }

}
