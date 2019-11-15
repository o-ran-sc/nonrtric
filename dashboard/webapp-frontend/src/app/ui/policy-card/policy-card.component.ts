/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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
import { Component, OnDestroy, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { UiService } from '../../services/ui/ui.service';

@Component({
  selector: 'rd-policy-card',
  templateUrl: './policy-card.component.html',
  styleUrls: ['./policy-card.component.scss']
})
export class PolicyCardComponent implements OnInit, OnDestroy {
  darkMode: boolean;

  constructor(public router: Router, private ui: UiService) { }

  ngOnInit() {
    this.ui.darkModeState.subscribe((isDark) => {
      this.darkMode = isDark;
    });
  }

  ngOnDestroy() { }

  openDetails() {
    this.router.navigateByUrl('../../policy');
  }

}
