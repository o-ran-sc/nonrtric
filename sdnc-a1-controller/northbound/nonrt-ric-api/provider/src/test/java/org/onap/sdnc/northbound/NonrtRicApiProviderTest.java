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
import com.google.common.util.concurrent.ListenableFuture;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.northbound.provider.NonrtRicApiProvider;
import org.onap.sdnc.northbound.restadapter.NearRicUrlProvider;
import org.onap.sdnc.northbound.restadapter.RestAdapter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyIdentitiesInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyIdentitiesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyStatusInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyStatusOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeIdentitiesInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeIdentitiesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutPolicyInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutPolicyOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
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
  private NearRicUrlProvider nearRicUrlProvider;
  private static String nearRtRicUrl = "http://ric1:8085";
  private static String policyTypeId = "STD_QoSNudging_0.1.0";
  private static String policyId = "3d2157af-6a8f-4a7c-810f-38c2f824bf12";

  @Before
  public void setUp() throws Exception {
    nearRicUrlProvider = new NearRicUrlProvider();
    dataBroker = getDataBroker();
    nonrtRicApiProvider = new NonrtRicApiProvider(dataBroker, mockNotificationPublishService, mockRpcProviderRegistry);
  }

  @Test
  public void testGetPolicyTypeIdentities() throws InterruptedException, ExecutionException {
    GetPolicyTypeIdentitiesInputBuilder inputBuilder = new GetPolicyTypeIdentitiesInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.policyTypesUrl(inputBuilder.build().getNearRtRicUrl());
    List<String> policyTypeIdentities = new ArrayList<>();
    policyTypeIdentities.add(policyTypeId);
    ResponseEntity<Object> getPolicyTypeIdentitiesResponse = new ResponseEntity<>(policyTypeIdentities, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(getPolicyTypeIdentitiesResponse);
    ListenableFuture<RpcResult<GetPolicyTypeIdentitiesOutput>> result = nonrtRicApiProvider
        .getPolicyTypeIdentities(inputBuilder.build());
    Assert.assertEquals(policyTypeIdentities, result.get().getResult().getPolicyTypeIdList());
  }

  @Test
  public void testGetPolicyIdentities() throws InterruptedException, ExecutionException {
    GetPolicyIdentitiesInputBuilder inputBuilder = new GetPolicyIdentitiesInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.policiesUrl(inputBuilder.build().getNearRtRicUrl());
    List<String> policyIdentities = new ArrayList<>();
    policyIdentities.add(policyId);
    ResponseEntity<Object> getPolicyIdentitiesResponse = new ResponseEntity<>(policyIdentities, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(getPolicyIdentitiesResponse);
    ListenableFuture<RpcResult<GetPolicyIdentitiesOutput>> result = nonrtRicApiProvider
        .getPolicyIdentities(inputBuilder.build());
    Assert.assertEquals(policyIdentities, result.get().getResult().getPolicyIdList());
  }

  @Test
  public void testGetPolicyType() throws InterruptedException, ExecutionException {
    GetPolicyTypeInputBuilder inputBuilder = new GetPolicyTypeInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyTypeUrl(inputBuilder.build().getNearRtRicUrl(),
        String.valueOf(inputBuilder.build().getPolicyTypeId()));
    String testPolicyType = "{}";
    ResponseEntity<Object> getPolicyTypeResponse = new ResponseEntity<>(testPolicyType, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(String.class))).thenReturn(getPolicyTypeResponse);
    ListenableFuture<RpcResult<GetPolicyTypeOutput>> result = nonrtRicApiProvider.getPolicyType(inputBuilder.build());
    Assert.assertEquals(testPolicyType, result.get().getResult().getPolicyType());
  }

  @Test
  public void testPutPolicy() throws InterruptedException, ExecutionException {
    PutPolicyInputBuilder inputBuilder = new PutPolicyInputBuilder();
    String testPolicy = "{}";
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setPolicyId(policyId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicy(testPolicy);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.putPolicyUrl(inputBuilder.build().getNearRtRicUrl(), inputBuilder.getPolicyId(),
        inputBuilder.getPolicyTypeId());
    ResponseEntity<String> putPolicyResponse = new ResponseEntity<>(testPolicy, HttpStatus.CREATED);
    when(restAdapter.put(eq(uri), eq(testPolicy), eq(String.class))).thenReturn(putPolicyResponse);
    ListenableFuture<RpcResult<PutPolicyOutput>> result = nonrtRicApiProvider.putPolicy(inputBuilder.build());
    Assert.assertEquals(testPolicy, result.get().getResult().getReturnedPolicy());
  }

  @Test
  public void testGetPolicyStatus() throws InterruptedException, ExecutionException {
    GetPolicyStatusInputBuilder inputBuilder = new GetPolicyStatusInputBuilder();
    inputBuilder.setNearRtRicUrl(nearRtRicUrl);
    inputBuilder.setPolicyId(policyId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyStatusUrl(nearRtRicUrl, policyId);
    String testPolicyStatus = "STATUS";
    ResponseEntity<Object> getPolicyStatusResponse = new ResponseEntity<>(testPolicyStatus, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(String.class))).thenReturn(getPolicyStatusResponse);
    ListenableFuture<RpcResult<GetPolicyStatusOutput>> result = nonrtRicApiProvider
        .getPolicyStatus(inputBuilder.build());
    Assert.assertEquals(testPolicyStatus, result.get().getResult().getPolicyStatus());
  }

}
