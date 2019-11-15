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
import { MatDialog, MatDialogRef } from '@angular/material/dialog';
import { LoadingDialogComponent } from './../../ui/loading-dialog/loading-dialog.component';
import { UiService } from './ui.service';

@Injectable({
  providedIn: 'root'
})
export class LoadingDialogService {

  darkMode: boolean;
  panelClass = '';

  constructor(private dialog: MatDialog,
    private ui: UiService) { }

  private loadingDialogRef: MatDialogRef<LoadingDialogComponent>;

  startLoading(msg: string) {
    this.ui.darkModeState.subscribe((isDark) => {
      this.darkMode = isDark;
    });
    if (this.darkMode) {
      this.panelClass = 'dark-theme';
    } else {
      this.panelClass = '';
    }
    this.loadingDialogRef = this.dialog.open(LoadingDialogComponent, {
      panelClass: this.panelClass,
      disableClose: true,
      width: '480px',
      position: { top: '100px' },
      data: {
        message: msg
      }
    });
  }

  stopLoading() {
    this.loadingDialogRef.close();
  }

}


