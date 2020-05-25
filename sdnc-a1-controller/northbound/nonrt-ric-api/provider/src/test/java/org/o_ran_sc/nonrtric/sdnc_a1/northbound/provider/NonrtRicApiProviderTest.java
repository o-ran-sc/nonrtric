/*-
 * ============LICENSE_START=======================================================
 *  Copyright (C) 2019 Nordix Foundation.
 * ================================================================================
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
 *
 * SPDX-License-Identifier: Apache-2.0
 * ============LICENSE_END=========================================================
 */

package org.o_ran_sc.nonrtric.sdnc_a1.northbound.provider;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import java.util.concurrent.ExecutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.o_ran_sc.nonrtric.sdnc_a1.northbound.restadapter.RestAdapter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.DeleteA1PolicyInputBuilder;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.DeleteA1PolicyOutput;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.GetA1PolicyInputBuilder;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.GetA1PolicyOutput;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.GetA1PolicyStatusInputBuilder;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.GetA1PolicyStatusOutput;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.GetA1PolicyTypeInputBuilder;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.GetA1PolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.PutA1PolicyInputBuilder;
import org.opendaylight.yang.gen.v1.org.o_ran_sc.nonrtric.sdnc_a1.northbound.a1.adapter.rev200122.PutA1PolicyOutput;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientResponseException;

/**
 * This class Tests all the methods in NonrtRicApiProvider
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class NonrtRicApiProviderTest extends AbstractConcurrentDataBrokerTest {

  protected static final Logger LOG = LoggerFactory.getLogger(NonrtRicApiProviderTest.class);

  private static final Integer HTTP_OK_AS_INTEGER = HttpStatus.OK.value();

  protected NonrtRicApiProvider nonrtRicApiProvider;
  protected DataBroker dataBroker;
  @Mock
  protected NotificationPublishService mockNotificationPublishService;
  @Mock
  protected RpcProviderRegistry mockRpcProviderRegistry;
  @Mock
  private RestAdapter restAdapter;
  private static Uri nearRtRicUrl = new Uri("http://ric1:8085");

  @Before
  public void setUp() throws Exception {
    dataBroker = getDataBroker();
    nonrtRicApiProvider = new NonrtRicApiProvider(dataBroker, mockNotificationPublishService, mockRpcProviderRegistry);
  }

  @Test
  public void testGetA1PolicySuccess() throws InterruptedException, ExecutionException {
    GetA1PolicyInputBuilder inputBuilder = new GetA1PolicyInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1PolicyOutput result = nonrtRicApiProvider.getA1Policy(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, result.getBody());
    assertEquals(HTTP_OK_AS_INTEGER, result.getHttpStatus());
  }

  @Test
  public void testGetA1PolicyTypeSuccess() throws InterruptedException, ExecutionException {
    GetA1PolicyTypeInputBuilder inputBuilder = new GetA1PolicyTypeInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1PolicyTypeOutput result = nonrtRicApiProvider.getA1PolicyType(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, result.getBody());
    assertEquals(HTTP_OK_AS_INTEGER, result.getHttpStatus());
  }

  @Test
  public void testGetA1PolicyStatusSuccess() throws InterruptedException, ExecutionException {
    GetA1PolicyStatusInputBuilder inputBuilder = new GetA1PolicyStatusInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1PolicyStatusOutput result = nonrtRicApiProvider.getA1PolicyStatus(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, result.getBody());
    assertEquals(HTTP_OK_AS_INTEGER, result.getHttpStatus());
  }

  @Test
  public void testGetA1Failure() throws InterruptedException, ExecutionException {
    GetA1PolicyInputBuilder inputBuilder = new GetA1PolicyInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "GET failed";
    Integer notFoundStatusCode = HttpStatus.NOT_FOUND.value();
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class)))
    .thenThrow(new RestClientResponseException(null, notFoundStatusCode, null, null, returnedBody.getBytes(), null));
    GetA1PolicyOutput result = nonrtRicApiProvider.getA1(inputBuilder.build());
    assertEquals(returnedBody, result.getBody());
    assertEquals(notFoundStatusCode, result.getHttpStatus());
  }

  @Test
  public void testPutA1PolicySuccess() throws InterruptedException, ExecutionException {
    PutA1PolicyInputBuilder inputBuilder = new PutA1PolicyInputBuilder();
    String testPolicy = "{}";
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setBody(testPolicy);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    Integer createdStatusCode = HttpStatus.CREATED.value();
    ResponseEntity<String> putResponse = new ResponseEntity<>(returnedBody, HttpStatus.CREATED);
    when(restAdapter.put(eq(nearRtRicUrl.getValue()), eq(testPolicy), eq(String.class))).thenReturn(putResponse);
    PutA1PolicyOutput result = nonrtRicApiProvider.putA1Policy(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, result.getBody());
    assertEquals(createdStatusCode, result.getHttpStatus());
  }

  @Test
  public void testPutA1PolicyFailure() throws InterruptedException, ExecutionException {
    PutA1PolicyInputBuilder inputBuilder = new PutA1PolicyInputBuilder();
    String testPolicy = "{}";
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setBody(testPolicy);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "PUT failed";
    Integer badRequestStatusCode = HttpStatus.BAD_REQUEST.value();
    when(restAdapter.put(eq(nearRtRicUrl.getValue()), eq(testPolicy), eq(String.class)))
    .thenThrow(new RestClientResponseException(null, badRequestStatusCode, null, null, returnedBody.getBytes(), null));
    PutA1PolicyOutput result = nonrtRicApiProvider.putA1Policy(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, result.getBody());
    assertEquals(badRequestStatusCode, result.getHttpStatus());
  }

  @Test
  public void testDeleteA1Success() throws InterruptedException, ExecutionException {
    DeleteA1PolicyInputBuilder inputBuilder = new DeleteA1PolicyInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    ResponseEntity<Object> getResponseNoContent = new ResponseEntity<>(HttpStatus.NO_CONTENT);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponseOk = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.delete(nearRtRicUrl.getValue())).thenReturn(getResponseNoContent).thenReturn(getResponseOk);
    DeleteA1PolicyOutput resultNoContent = nonrtRicApiProvider.deleteA1Policy(inputBuilder.build()).get().getResult();
    assertEquals(Integer.valueOf(HttpStatus.NO_CONTENT.value()), resultNoContent.getHttpStatus());
    DeleteA1PolicyOutput resultOk = nonrtRicApiProvider.deleteA1Policy(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, resultOk.getBody());
    assertEquals(HTTP_OK_AS_INTEGER, resultOk.getHttpStatus());
  }

  @Test
  public void testDeleteA1Failure() throws InterruptedException, ExecutionException {
    DeleteA1PolicyInputBuilder inputBuilder = new DeleteA1PolicyInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "DELETE failed";
    Integer notFoundStatusCode = HttpStatus.NOT_FOUND.value();
    when(restAdapter.delete(nearRtRicUrl.getValue()))
    .thenThrow(new RestClientResponseException(null, notFoundStatusCode, null, null, returnedBody.getBytes(), null));
    DeleteA1PolicyOutput result = nonrtRicApiProvider.deleteA1Policy(inputBuilder.build()).get().getResult();
    assertEquals(returnedBody, result.getBody());
    assertEquals(notFoundStatusCode, result.getHttpStatus());
  }

}
