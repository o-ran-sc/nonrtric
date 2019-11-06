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

import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
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
import org.onap.sdnc.northbound.restadpter.NearRicUrlProvider;
import org.onap.sdnc.northbound.restadpter.RestAdapter;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.test.AbstractConcurrentDataBrokerTest;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyInstanceInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyInstanceOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyTypeInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyInstanceInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyInstanceOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyTypeInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetHealthCheckInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetHealthCheckOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetNearRTRICsInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetNearRTRICsOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstanceInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstanceOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstancesInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstancesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypeInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypesInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetStatusInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetStatusOutput;
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
  private static String nearRtRicId = "NearRtRic1";
  private static Long policyTypeId = 11L;
  private static String policyTypeInstanceId = "12";


  @Before
  public void setUp() throws Exception {
    nearRicUrlProvider = new NearRicUrlProvider();
    dataBroker = getDataBroker();
    nonrtRicApiProvider = new NonrtRicApiProvider(dataBroker, mockNotificationPublishService,
        mockRpcProviderRegistry);
  }

  @Test
  public void testCreatePolicyInstance() throws InterruptedException, ExecutionException {
    CreatePolicyInstanceInputBuilder inputBuilder = new CreatePolicyInstanceInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceId(inputBuilder.build().getNearRtRicId(),
            String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    ResponseEntity<Object> createPolicyInstanceResponse = new ResponseEntity<>(HttpStatus.CREATED);
    when(restAdapter.put(eq(uri), anyObject())).thenReturn(createPolicyInstanceResponse);
    ListenableFuture<RpcResult<CreatePolicyInstanceOutput>> result =
        nonrtRicApiProvider.createPolicyInstance(inputBuilder.build());
    Assert.assertEquals(String.valueOf(HttpStatus.CREATED.value()), result.get().getResult().getCode());
  }

  @Test
  public void testDeletePolicyInstance() throws InterruptedException, ExecutionException {
    DeletePolicyInstanceInputBuilder inputBuilder = new DeletePolicyInstanceInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceId(inputBuilder.build().getNearRtRicId(),
            String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    ResponseEntity<Object> deletePolicyInstanceResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
    when(restAdapter.delete(eq(uri))).thenReturn(deletePolicyInstanceResponse);
    ListenableFuture<RpcResult<DeletePolicyInstanceOutput>> result =
        nonrtRicApiProvider.deletePolicyInstance(inputBuilder.build());
    Assert.assertEquals(String.valueOf(HttpStatus.NO_CONTENT.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetNearRTRICs() throws InterruptedException, ExecutionException {
    GetNearRTRICsInputBuilder inputBuilder = new GetNearRTRICsInputBuilder();
    ListenableFuture<RpcResult<GetNearRTRICsOutput>> result =
        nonrtRicApiProvider.getNearRTRICs(inputBuilder.build());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }

  @Test
  public void testCreatePolicyType() throws InterruptedException, ExecutionException {
    CreatePolicyTypeInputBuilder inputBuilder = new CreatePolicyTypeInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyTypeId(inputBuilder.build().getNearRtRicId(),
                String.valueOf(inputBuilder.build().getPolicyTypeId()));
    ResponseEntity<Object> createPolicyTypeResponse = new ResponseEntity<>(HttpStatus.CREATED);
    when(restAdapter.put(eq(uri), anyObject())).thenReturn(createPolicyTypeResponse);
    ListenableFuture<RpcResult<CreatePolicyTypeOutput>> result =
        nonrtRicApiProvider.createPolicyType(inputBuilder.build());
    Assert.assertEquals(String.valueOf(HttpStatus.CREATED.value()), result.get().getResult().getCode());
  }

  @Test
  public void testDeletePolicyType() throws InterruptedException, ExecutionException {
    DeletePolicyTypeInputBuilder inputBuilder = new DeletePolicyTypeInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyTypeId(inputBuilder.build().getNearRtRicId(),
                String.valueOf(inputBuilder.build().getPolicyTypeId()));
    ResponseEntity<Object> deletePolicyTypeResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
    when(restAdapter.delete(eq(uri))).thenReturn(deletePolicyTypeResponse);
    ListenableFuture<RpcResult<DeletePolicyTypeOutput>> result =
        nonrtRicApiProvider.deletePolicyType(inputBuilder.build());
    Assert.assertEquals(String.valueOf(HttpStatus.NO_CONTENT.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetPolicyType() throws InterruptedException, ExecutionException {
    GetPolicyTypeInputBuilder inputBuilder = new GetPolicyTypeInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyTypeId(inputBuilder.build().getNearRtRicId(),
            String.valueOf(inputBuilder.build().getPolicyTypeId()));
    String policyType =
            "{\"name\":\"Policy type 1\",\"description\":\"PT 1\",\"policy_type_id\":1,\"create_schema\":{}}";
    ResponseEntity<Object> getPolicyTypeResponse = new ResponseEntity<>(policyType, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(String.class))).thenReturn(getPolicyTypeResponse);
    ListenableFuture<RpcResult<GetPolicyTypeOutput>> result =
        nonrtRicApiProvider.getPolicyType(inputBuilder.build());
    Assert.assertEquals("Policy type 1", result.get().getResult().getName());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetPolicyTypes() throws InterruptedException, ExecutionException {
    GetPolicyTypesInputBuilder inputBuilder = new GetPolicyTypesInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyTypes(inputBuilder.build().getNearRtRicId());
    List<Integer> policyTypesInteger = new ArrayList<>();
    policyTypesInteger.add(20001);
    List<Long> policyTypesLong = new ArrayList<>();
    policyTypesLong.add(20001L);
    ResponseEntity<Object> getPolicyTypesResponse = new ResponseEntity<>(policyTypesInteger, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(getPolicyTypesResponse);
    ListenableFuture<RpcResult<GetPolicyTypesOutput>> result =
        nonrtRicApiProvider.getPolicyTypes(inputBuilder.build());
    Assert.assertEquals(policyTypesLong, result.get().getResult().getPolicyTypeIdList());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetPolicyInstance() throws InterruptedException, ExecutionException {
    GetPolicyInstanceInputBuilder inputBuilder = new GetPolicyInstanceInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceId(inputBuilder.build().getNearRtRicId(),
        String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    String policyInstance =
            "{\"scope\":{\"ue_id\":\"2\"},\"statement\":{\"priority_level\":\"1\"},\"policy_id\":\"pi12\"}";
    ResponseEntity<Object> getPolicyInstanceResponse = new ResponseEntity<>(policyInstance, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(String.class))).thenReturn(getPolicyInstanceResponse);
    ListenableFuture<RpcResult<GetPolicyInstanceOutput>> result =
        nonrtRicApiProvider.getPolicyInstance(inputBuilder.build());
    Assert.assertEquals(policyInstance, result.get().getResult().getPolicyInstance());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetPolicyInstances() throws InterruptedException, ExecutionException {
    GetPolicyInstancesInputBuilder inputBuilder = new GetPolicyInstancesInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstances(inputBuilder.build().getNearRtRicId(),
            String.valueOf(inputBuilder.build().getPolicyTypeId()));
    List<String> policyInstances = new ArrayList<>();
    policyInstances.add("3d2157af-6a8f-4a7c-810f-38c2f824bf12");
    ResponseEntity<Object> getPolicyInstancesResponse = new ResponseEntity<>(policyInstances, HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(getPolicyInstancesResponse);
    ListenableFuture<RpcResult<GetPolicyInstancesOutput>> result =
        nonrtRicApiProvider.getPolicyInstances(inputBuilder.build());
    Assert.assertEquals(policyInstances, result.get().getResult().getPolicyInstanceIdList());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetStatus() throws InterruptedException, ExecutionException {
    GetStatusInputBuilder inputBuilder = new GetStatusInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceIdStatus(inputBuilder.build().getNearRtRicId(),
        String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    String policyInstanceStatus = "[{\"handler_id\":\"NearRTRIC-1\",\"status\":\"enforced\"}]";
    ResponseEntity<Object> getStatusResponse =
            new ResponseEntity<>(new Gson().fromJson(policyInstanceStatus, List.class), HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(getStatusResponse);
    ListenableFuture<RpcResult<GetStatusOutput>> result =
        nonrtRicApiProvider.getStatus(inputBuilder.build());
    Assert.assertEquals("enforced", result.get().getResult().getStatus());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }

  @Test
  public void testGetHealthCheck() throws InterruptedException, ExecutionException {
    GetHealthCheckInputBuilder inputBuilder = new GetHealthCheckInputBuilder();
    inputBuilder.setNearRtRicId(nearRtRicId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getHealthCheck(inputBuilder.build().getNearRtRicId());
    ResponseEntity<Object> getHealthCheckResponse = new ResponseEntity<>(HttpStatus.OK);
    when(restAdapter.get(eq(uri), eq(Object.class))).thenReturn(getHealthCheckResponse);
    ListenableFuture<RpcResult<GetHealthCheckOutput>> result =
        nonrtRicApiProvider.getHealthCheck(inputBuilder.build());
    Assert.assertEquals(true, result.get().getResult().isHealthStatus());
    Assert.assertEquals(String.valueOf(HttpStatus.OK.value()), result.get().getResult().getCode());
  }
}
