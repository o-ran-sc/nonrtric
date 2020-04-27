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

package org.o_ran_sc.nonrtric.sdnc_a1.northbound;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;

import java.util.concurrent.ExecutionException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.o_ran_sc.nonrtric.sdnc_a1.northbound.provider.NonrtRicApiProvider;
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

/**
 * This class Tests all the methods in NonrtRicApiProvider
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

@RunWith(MockitoJUnitRunner.class)
public class NonrtRicApiProviderTest extends AbstractConcurrentDataBrokerTest {

  protected static final Logger LOG = LoggerFactory.getLogger(NonrtRicApiProviderTest.class);
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
  public void testGetA1Policy() throws InterruptedException, ExecutionException {
    GetA1PolicyInputBuilder inputBuilder = new GetA1PolicyInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1PolicyOutput result = nonrtRicApiProvider.getA1Policy(inputBuilder.build()).get().getResult();
    Assert.assertEquals(returnedBody, result.getBody());
    Assert.assertTrue(HttpStatus.OK.value() == result.getHttpStatus());
  }

  @Test
  public void testGetA1PolicyType() throws InterruptedException, ExecutionException {
    GetA1PolicyTypeInputBuilder inputBuilder = new GetA1PolicyTypeInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1PolicyTypeOutput result = nonrtRicApiProvider.getA1PolicyType(inputBuilder.build()).get().getResult();
    Assert.assertEquals(returnedBody, result.getBody());
    Assert.assertTrue(HttpStatus.OK.value() == result.getHttpStatus());
  }

  @Test
  public void testGetA1PolicyStatus() throws InterruptedException, ExecutionException {
    GetA1PolicyStatusInputBuilder inputBuilder = new GetA1PolicyStatusInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1PolicyStatusOutput result = nonrtRicApiProvider.getA1PolicyStatus(inputBuilder.build()).get().getResult();
    Assert.assertEquals(returnedBody, result.getBody());
    Assert.assertTrue(HttpStatus.OK.value() == result.getHttpStatus());
  }

  @Test
  public void testPutA1Policy() throws InterruptedException, ExecutionException {
    PutA1PolicyInputBuilder inputBuilder = new PutA1PolicyInputBuilder();
    String testPolicy = "{}";
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setBody(testPolicy);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<String> putResponse = new ResponseEntity<>(returnedBody, HttpStatus.CREATED);
    when(restAdapter.put(eq(nearRtRicUrl.getValue()), eq(testPolicy), eq(String.class))).thenReturn(putResponse);
    PutA1PolicyOutput result = nonrtRicApiProvider.putA1Policy(inputBuilder.build()).get().getResult();
    Assert.assertEquals(returnedBody, result.getBody());
    Assert.assertTrue(HttpStatus.CREATED.value() == result.getHttpStatus());
  }

  @Test
  public void testDeleteA1() throws InterruptedException, ExecutionException {
    DeleteA1PolicyInputBuilder inputBuilder = new DeleteA1PolicyInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);

    ResponseEntity<Object> getResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
    when(restAdapter.delete(nearRtRicUrl.getValue())).thenReturn(getResponse);
    DeleteA1PolicyOutput result = nonrtRicApiProvider.deleteA1Policy(inputBuilder.build()).get().getResult();
    Assert.assertTrue(HttpStatus.NO_CONTENT.value() == result.getHttpStatus());
  }

}
