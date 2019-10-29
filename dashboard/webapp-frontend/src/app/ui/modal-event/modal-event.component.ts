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
import { Component, Input, OnInit , Output, EventEmitter  } from '@angular/core';
import { FormControl, Validators } from '@angular/forms';

@Component({
  selector: 'rd-modal-event',
  templateUrl: './modal-event.component.html',
  styleUrls: ['./modal-event.component.scss']
})
export class ModalEventComponent implements OnInit {

    public renderValue;

    @Input() value;
    @Input() rowData: any;
    @Output() save: EventEmitter<any> = new EventEmitter();
    contactFormModalHelm = new FormControl('', Validators.required);
    onOpened(event: any) {
    console.log(event);
    }


    constructor() {  }

    ngOnInit() {
        this.renderValue = this.value;
    }

    example() {
        alert(this.renderValue);
    }

    onDeployxApp() {
        this.save.emit(this.rowData);
    }

}
