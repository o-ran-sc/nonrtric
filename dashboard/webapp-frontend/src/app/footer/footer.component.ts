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
import { Component, OnInit } from '@angular/core';
import { DashboardSuccessTransport } from '../interfaces/dashboard.types';
import { DashboardService } from '../services/dashboard/dashboard.service';
import { UiService } from '../services/ui/ui.service';

@Component({
  selector: 'rd-footer',
  templateUrl: './footer.component.html',
  styleUrls: ['./footer.component.scss']
})

/**
 * Fetches the version on load for display in the footer
 */
export class FooterComponent implements OnInit {
  darkMode: boolean;
  dashboardVersion: string;

  // Inject the service
  constructor(private dashboardService: DashboardService,
              public ui: UiService ) { }

  ngOnInit() {
    this.dashboardService.getVersion().subscribe((res: DashboardSuccessTransport) => this.dashboardVersion = res.data);
    this.ui.darkModeState.subscribe((isDark) => {
      this.darkMode = isDark;
    });
  }

}
