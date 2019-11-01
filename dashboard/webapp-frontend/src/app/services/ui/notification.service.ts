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

import { Injectable } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor(public toastr: ToastrService) { }

  successConfig = {
    timeOut: 10000,
    closeButton: true
  };

  warningConfig = {
    disableTimeOut: true,
    closeButton: true
  };

  errorConfig = {
    disableTimeOut: true,
    closeButton: true
  };

  success(msg: string) {
    this.toastr.success(msg, '', this.successConfig);
  }

  warn(msg: string) {
    this.toastr.warning(msg, '', this.warningConfig);
  }

  error(msg: string) {
    this.toastr.error(msg, '', this.errorConfig);
  }

}
