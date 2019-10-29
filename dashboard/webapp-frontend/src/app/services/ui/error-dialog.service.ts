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
import { ErrorDialogComponent } from '../../ui/error-dialog/error-dialog.component';
import { HttpErrorResponse } from '@angular/common/http';
import { MatDialog } from '@angular/material/dialog';
import { Injectable } from '@angular/core';
import { UiService } from './ui.service';

@Injectable()
export class ErrorDialogService {

  darkMode: boolean;
  panelClass: string = "";
  public errorMessage: string = '';

  constructor(private dialog: MatDialog,
    public ui: UiService) { }

  public displayError(error: string) {
    this.ui.darkModeState.subscribe((isDark) => {
      this.darkMode = isDark;
    });
    if (this.darkMode) {
      this.panelClass = "dark-theme";
    } else {
      this.panelClass = "";
    }
    return this.dialog.open(ErrorDialogComponent, {
      panelClass: this.panelClass,
      width: '400px',
      position: { top: '100px' },
      disableClose: true,
      data: { 'errorMessage': error }
    });
  }
}
