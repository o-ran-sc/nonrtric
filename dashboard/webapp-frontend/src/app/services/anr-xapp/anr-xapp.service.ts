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
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { DashboardSuccessTransport } from '../../interfaces/dashboard.types';
import { ANRNeighborCellRelation, ANRNeighborCellRelationMod } from '../../interfaces/anr-xapp.types';

@Injectable({
  providedIn: 'root'
})
export class ANRXappService {

  // Trailing slashes are confusing so omit them here
  private basePath = 'api/xapp/anr';
  private ncrtPath = 'ncrt';
  private servingPath = 'servingcells';
  private neighborPath = 'neighborcells';

  constructor(private httpClient: HttpClient) {
    // injects to variable httpClient
  }

  private buildPath(...args: any[]) {
    let result = this.basePath;
    args.forEach(part => {
      result = result + '/' + part;
    });
    return result;
  }

  /**
   * Gets ANR xApp client version details
   * @returns Observable that should yield a String
   */
  getVersion(): Observable<string> {
    const url = this.buildPath('version');
    return this.httpClient.get<DashboardSuccessTransport>(url).pipe(
      // Extract the string here
      map(res => res['data'])
    );
  }

  /**
   * Performs a liveness probe
   * @returns Observable that should yield a response code (no data)
   */
  getHealthAlive(): Observable<any> {
    const url = this.buildPath('health/alive');
    return this.httpClient.get(url, { observe: 'response' });
  }

  /**
   * Performs a readiness probe
   * @returns Observable that should yield a response code (no data)
   */
  getHealthReady(): Observable<any> {
    const url = this.buildPath('health/ready');
    return this.httpClient.get(url, { observe: 'response' });
  }

/**
 * Gets array of gNodeB IDs
 * @returns Observable that should yield a string array
 */
  getgNodeBs(): Observable<string[]> {
    const url = this.buildPath('gnodebs');
    return this.httpClient.get<string[]>(url).pipe(
      // Extract the array of IDs here
      map(res => res['gNodeBIds'])
    );
  }

  /**
   * Gets the neighbor cell relation table for all gNodeBs or based on query parameters
   * @param ggnbId Optional parameter for the gNB ID
   * @param servingCellNrcgi Serving cell NRCGI
   * @param neighborCellNrpci Neighbor cell NRPCI
   * @returns Observable of ANR neighbor cell relation array
   */
  getNcrtInfo(ggnodeb: string = '', servingCellNrcgi: string = '', neighborCellNrpci: string = ''): Observable<ANRNeighborCellRelation[]> {
    const url = this.buildPath(this.ncrtPath);
    return this.httpClient.get<ANRNeighborCellRelation[]>(url, {
      params: new HttpParams()
        .set('ggnodeb', ggnodeb)
        .set('servingCellNrcgi', servingCellNrcgi)
        .set('neighborCellNrpci', neighborCellNrpci)
    }).pipe(
      // Extract the array of relations here
      map(res => res['ncrtRelations'])
    );
  }

  /**
   * Modify neighbor cell relation based on Serving Cell NRCGI and Neighbor Cell NRPCI
   * @param servingCellNrcgi Serving cell NRCGI
   * @param neighborCellNrpci Neighbor cell NRPCI
   * @param mod Values to store in the specified relation
   * @returns Observable that yields a response code only, no data
   */
  modifyNcr(servingCellNrcgi: string, neighborCellNrpci: string, mod: ANRNeighborCellRelationMod): Observable<Object> {
    const url = this.buildPath(this.ncrtPath, this.servingPath, servingCellNrcgi, this.neighborPath, neighborCellNrpci);
    return this.httpClient.put(url, mod, { observe: 'response' });
  }

  /**
   * Deletes neighbor cell relation based on Serving Cell NRCGI and Neighbor Cell NRPCI
   * @param servingCellNrcgi Serving cell NRCGI
   * @param neighborCellNrpci Neighbor cell NRPCI
   * @returns Observable that yields a response code only, no data
   */
  deleteNcr(servingCellNrcgi: string, neighborCellNrpci: string): Observable<Object> {
    const url = this.buildPath(this.ncrtPath, this.servingPath, servingCellNrcgi, this.neighborPath, neighborCellNrpci);
    return this.httpClient.delete(url, { observe: 'response' });
  }

}
