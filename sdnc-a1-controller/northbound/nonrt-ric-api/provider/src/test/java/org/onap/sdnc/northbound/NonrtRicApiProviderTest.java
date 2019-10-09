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
import org.oransc.ric.a1med.client.model.PolicyTypeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.ListenableFuture;

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
  public void testCreatePolicyType() throws InterruptedException, ExecutionException {
    CreatePolicyTypeInputBuilder inputBuilder = new CreatePolicyTypeInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri =
        nearRicUrlProvider.getPolicyTypeId(String.valueOf(inputBuilder.build().getPolicyTypeId()));
    Optional<Object> createPolicyTyperesponse = null;
    when(restAdapter.put(eq(uri), anyObject())).thenReturn(createPolicyTyperesponse);
    ListenableFuture<RpcResult<CreatePolicyTypeOutput>> result =
        nonrtRicApiProvider.createPolicyType(inputBuilder.build());
    Assert.assertEquals("Success", result.get().getResult().getStatus());
  }

  @Test
  public void testGetPolicyType() throws InterruptedException, ExecutionException {
    GetPolicyTypeInputBuilder inputBuilder = new GetPolicyTypeInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri =
        nearRicUrlProvider.getPolicyTypeId(String.valueOf(inputBuilder.build().getPolicyTypeId()));
    PolicyTypeSchema policyTypeSchema = new PolicyTypeSchema();
    policyTypeSchema.setName("AdmissionControlPolicy");
    policyTypeSchema.setCreateSchema("{}");
    when(restAdapter.get(eq(uri), anyObject())).thenReturn(Optional.of(policyTypeSchema));
    ListenableFuture<RpcResult<GetPolicyTypeOutput>> result =
        nonrtRicApiProvider.getPolicyType(inputBuilder.build());
    Assert.assertEquals(policyTypeSchema.getName(), result.get().getResult().getName());
  }

  @Test
  public void testGetPolicyTypes() throws InterruptedException, ExecutionException {
    GetPolicyTypesInputBuilder inputBuilder = new GetPolicyTypesInputBuilder();
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyTypes();
    List<Long> policyTypes = new ArrayList<>();
    policyTypes.add(20001L);
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(Optional.of(policyTypes));
    ListenableFuture<RpcResult<GetPolicyTypesOutput>> result =
        nonrtRicApiProvider.getPolicyTypes(inputBuilder.build());
    Assert.assertEquals(policyTypes, result.get().getResult().getPolicyTypeIdList());
  }

  @Test
  public void testDeletePolicyType() throws InterruptedException, ExecutionException {
    DeletePolicyTypeInputBuilder inputBuilder = new DeletePolicyTypeInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri =
        nearRicUrlProvider.getPolicyTypeId(String.valueOf(inputBuilder.build().getPolicyTypeId()));
    Optional<Object> deletePolicyTyperesponse = null;
    when(restAdapter.delete(uri)).thenReturn(deletePolicyTyperesponse);
    ListenableFuture<RpcResult<DeletePolicyTypeOutput>> result =
        nonrtRicApiProvider.deletePolicyType(inputBuilder.build());
  }

  @Test
  public void testCreatePolicyInstance() throws InterruptedException, ExecutionException {
    CreatePolicyInstanceInputBuilder inputBuilder = new CreatePolicyInstanceInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceId(
        String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    Optional<Object> createPolicyInstanceresponse = null;
    when(restAdapter.put(eq(uri), anyObject())).thenReturn(createPolicyInstanceresponse);
    ListenableFuture<RpcResult<CreatePolicyInstanceOutput>> result =
        nonrtRicApiProvider.createPolicyInstance(inputBuilder.build());
  }

  @Test
  public void testDeletePolicyInstance() throws InterruptedException, ExecutionException {
    DeletePolicyInstanceInputBuilder inputBuilder = new DeletePolicyInstanceInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceId(
        String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    Optional<Object> deletePolicyInstanceresponse = null;
    when(restAdapter.delete(uri)).thenReturn(deletePolicyInstanceresponse);
    ListenableFuture<RpcResult<DeletePolicyInstanceOutput>> result =
        nonrtRicApiProvider.deletePolicyInstance(inputBuilder.build());
  }

  @Test
  public void testGetPolicyInstance() throws InterruptedException, ExecutionException {
    GetPolicyInstanceInputBuilder inputBuilder = new GetPolicyInstanceInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceId(
        String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    String getPolicyInstanceresponse = "{}";
    when(restAdapter.get(eq(uri), eq(String.class)))
        .thenReturn(Optional.of(getPolicyInstanceresponse));
    ListenableFuture<RpcResult<GetPolicyInstanceOutput>> result =
        nonrtRicApiProvider.getPolicyInstance(inputBuilder.build());
  }

  @Test
  public void testGetPolicyInstances() throws InterruptedException, ExecutionException {
    GetPolicyInstancesInputBuilder inputBuilder = new GetPolicyInstancesInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider
        .getPolicyInstances(String.valueOf(inputBuilder.build().getPolicyTypeId()));
    List<String> getPolicyInstances = new ArrayList<>();
    getPolicyInstances.add("3d2157af-6a8f-4a7c-810f-38c2f824bf12");
    when(restAdapter.get(eq(uri), eq(List.class))).thenReturn(Optional.of(getPolicyInstances));
    ListenableFuture<RpcResult<GetPolicyInstancesOutput>> result =
        nonrtRicApiProvider.getPolicyInstances(inputBuilder.build());
  }

  @Test
  public void testGetStatus() throws InterruptedException, ExecutionException {
    GetStatusInputBuilder inputBuilder = new GetStatusInputBuilder();
    inputBuilder.setPolicyTypeId(policyTypeId);
    inputBuilder.setPolicyInstanceId(policyTypeInstanceId);
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getPolicyInstanceIdStatus(
        String.valueOf(inputBuilder.build().getPolicyTypeId()), inputBuilder.getPolicyInstanceId());
    List<String> getPolicyInstanceIdStatus = new ArrayList<>();
    getPolicyInstanceIdStatus.add("");
    when(restAdapter.get(eq(uri), eq(List.class)))
        .thenReturn(Optional.of(getPolicyInstanceIdStatus));
    ListenableFuture<RpcResult<GetStatusOutput>> result =
        nonrtRicApiProvider.getStatus(inputBuilder.build());
    // TODO: Define the proper response message for get policy instance status
    Assert.assertEquals("Success", result.get().getResult().getStatus());
  }

  @Test
  public void testHealthCheck() throws InterruptedException, ExecutionException {
    GetHealthCheckInputBuilder inputBuilder = new GetHealthCheckInputBuilder();
    Whitebox.setInternalState(nonrtRicApiProvider, "restAdapter", restAdapter);
    String uri = nearRicUrlProvider.getHealthCheck();
    String healthCheckStatus = "";
    when(restAdapter.get(eq(uri), eq(String.class))).thenReturn(Optional.of(healthCheckStatus));
    ListenableFuture<RpcResult<GetHealthCheckOutput>> result =
        nonrtRicApiProvider.getHealthCheck(inputBuilder.build());
    Assert.assertEquals(true, result.get().getResult().isHealthStatus());
  }
}
