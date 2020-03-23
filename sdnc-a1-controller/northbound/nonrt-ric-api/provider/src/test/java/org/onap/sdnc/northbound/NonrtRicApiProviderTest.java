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

package org.onap.sdnc.northbound;

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
import org.onap.sdnc.northbound.provider.NonrtRicApiProvider;
import org.onap.sdnc.northbound.restadapter.RestAdapter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeleteA1InputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeleteA1Output;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1InputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1Output;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutA1InputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutA1Output;
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
  public void testGetA1() throws InterruptedException, ExecutionException {
    GetA1InputBuilder inputBuilder = new GetA1InputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<Object> getResponse = new ResponseEntity<>(returnedBody, HttpStatus.OK);
    when(restAdapter.get(eq(nearRtRicUrl.getValue()), eq(String.class))).thenReturn(getResponse);
    GetA1Output result = nonrtRicApiProvider.getA1(inputBuilder.build()).get().getResult();
    Assert.assertEquals(returnedBody, result.getBody());
    Assert.assertTrue(HttpStatus.OK.value() == result.getHttpStatus());
  }

  @Test
  public void testPutA1() throws InterruptedException, ExecutionException {
    PutA1InputBuilder inputBuilder = new PutA1InputBuilder();
    String testPolicy = "{}";
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setBody(testPolicy);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String returnedBody = "returned body";
    ResponseEntity<String> putResponse = new ResponseEntity<>(returnedBody, HttpStatus.CREATED);
    when(restAdapter.put(eq(nearRtRicUrl.getValue()), eq(testPolicy), eq(String.class))).thenReturn(putResponse);
    PutA1Output result = nonrtRicApiProvider.putA1(inputBuilder.build()).get().getResult();
    Assert.assertEquals(returnedBody, result.getBody());
    Assert.assertTrue(HttpStatus.CREATED.value() == result.getHttpStatus());
  }

  @Test
  public void testDeleteA1() throws InterruptedException, ExecutionException {
    DeleteA1InputBuilder inputBuilder = new DeleteA1InputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);

    ResponseEntity<Object> getResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
    when(restAdapter.delete(nearRtRicUrl.getValue())).thenReturn(getResponse);
    DeleteA1Output result = nonrtRicApiProvider.deleteA1(inputBuilder.build()).get().getResult();
    Assert.assertTrue(HttpStatus.NO_CONTENT.value() == result.getHttpStatus());
  }

}
