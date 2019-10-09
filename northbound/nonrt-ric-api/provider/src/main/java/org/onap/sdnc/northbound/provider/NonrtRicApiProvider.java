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

package org.onap.sdnc.northbound.provider;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.StringUtils;
import org.onap.sdnc.northbound.restadpter.NearRicUrlProvider;
import org.onap.sdnc.northbound.restadpter.RestAdapter;
import org.onap.sdnc.northbound.restadpter.RestAdapterImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.A1ADAPTERAPIService;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyInstanceInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyInstanceOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyInstanceOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyTypeInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.CreatePolicyTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyInstanceInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyInstanceOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyInstanceOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyTypeInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.DeletePolicyTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetHealthCheckInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetHealthCheckOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetHealthCheckOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetNearRTRICsInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetNearRTRICsOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstanceInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstanceOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstanceOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstancesInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstancesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyInstancesOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypeInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypesInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetPolicyTypesOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetStatusInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetStatusOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetStatusOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.oransc.ric.a1med.client.model.PolicyTypeSchema;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

/**
 * Defines a base implementation for your provider. This class overrides the generated interface
 * from the YANG model and implements the request model for the A1 interface. This class identifies
 * the Near-RIC throught the IP passed over the payload and calls the corresponding Near-RIC over
 * Rest API
 * 
 * <pre>
 * 
 * @author lathishbabu.ganesan@est.tech
 *
 */

public class NonrtRicApiProvider implements AutoCloseable, A1ADAPTERAPIService {

  protected static final String APP_NAME = "nonrt-ric-api";
  protected static final String NO_SERVICE_LOGIC_ACTIVE = "No service logic active for ";
  private static final String NON_NULL_PARAM = "non-null";
  private static final String NULL_PARAM = "null";
  private static final String RESPONSE_SUCCESS = "Success";
  private static final String RESPONSE_CODE_SUCCESS = "200";

  private final Logger log = LoggerFactory.getLogger(NonrtRicApiProvider.class);
  private final ExecutorService executor;

  protected DataBroker dataBroker;
  protected NotificationPublishService notificationService;
  protected RpcProviderRegistry rpcRegistry;
  protected BindingAwareBroker.RpcRegistration<?> rpcRegistration;
  private RestAdapter restAdapter;
  private NearRicUrlProvider nearRicUrlProvider;

  public NonrtRicApiProvider(DataBroker dataBroker,
      NotificationPublishService notificationPublishService,
      RpcProviderRegistry rpcProviderRegistry) {
    log.info("Creating provider for {}", APP_NAME);
    executor = Executors.newFixedThreadPool(1);
    setDataBroker(dataBroker);
    setNotificationService(notificationPublishService);
    setRpcRegistry(rpcProviderRegistry);
    initialize();

  }

  public void initialize() {
    log.info("Initializing provider for {}", APP_NAME);
    createContainers();
    restAdapter = new RestAdapterImpl();
    nearRicUrlProvider = new NearRicUrlProvider();
    log.info("Initialization complete for {}", APP_NAME);
  }

  protected void initializeChild() {
    // Override if you have custom initialization intelligence
  }

  @Override
  public void close() throws Exception {
    log.info("Closing provider for {}", APP_NAME);
    executor.shutdown();
    rpcRegistration.close();
    log.info("Successfully closed provider for {}", APP_NAME);
  }

  private static class Iso8601Util {

    private static TimeZone timeZone = TimeZone.getTimeZone("UTC");
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    private Iso8601Util() {}

    static {
      dateFormat.setTimeZone(timeZone);
    }

    private static String now() {
      return dateFormat.format(new Date());
    }
  }

  public void setDataBroker(DataBroker dataBroker) {
    this.dataBroker = dataBroker;
    if (log.isDebugEnabled()) {
      log.debug("DataBroker set to {}", dataBroker == null ? NULL_PARAM : NON_NULL_PARAM);
    }
  }

  public void setNotificationService(NotificationPublishService notificationService) {
    this.notificationService = notificationService;
    if (log.isDebugEnabled()) {
      log.debug("Notification Service set to {}",
          notificationService == null ? NULL_PARAM : NON_NULL_PARAM);
    }
  }

  public void setRpcRegistry(RpcProviderRegistry rpcRegistry) {
    this.rpcRegistry = rpcRegistry;
    if (log.isDebugEnabled()) {
      log.debug("RpcRegistry set to {}", rpcRegistry == null ? NULL_PARAM : NON_NULL_PARAM);
    }
  }

  private void createContainers() {

    final WriteTransaction t = dataBroker.newReadWriteTransaction();

    try {
      CheckedFuture<Void, TransactionCommitFailedException> checkedFuture = t.submit();
      checkedFuture.get();
      log.info("Create containers succeeded!");

    } catch (InterruptedException | ExecutionException e) {
      log.error("Create containers failed: ", e);
    }
  }

  @Override
  public ListenableFuture<RpcResult<CreatePolicyInstanceOutput>> createPolicyInstance(
      CreatePolicyInstanceInput input) {
    log.debug("Start of createPolicyInstance");
    String uri = nearRicUrlProvider.getPolicyInstanceId(String.valueOf(input.getPolicyTypeId()),
        String.valueOf(input.getPolicyInstanceId()));
    restAdapter.put(uri, input.getPolicyInstance());
    CreatePolicyInstanceOutputBuilder responseBuilder = new CreatePolicyInstanceOutputBuilder();
    log.debug("End of createPolicyInstance");
    RpcResult<CreatePolicyInstanceOutput> rpcResult = RpcResultBuilder
        .<CreatePolicyInstanceOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<CreatePolicyTypeOutput>> createPolicyType(
      CreatePolicyTypeInput input) {
    log.debug("Start of createPolicyType");
    String uri = nearRicUrlProvider.getPolicyTypeId(String.valueOf(input.getPolicyTypeId()));
    PolicyTypeSchema policyTypeSchema = new PolicyTypeSchema();
    restAdapter.put(uri, policyTypeSchema);
    CreatePolicyTypeOutputBuilder responseBuilder = new CreatePolicyTypeOutputBuilder();
    responseBuilder.setCode(RESPONSE_CODE_SUCCESS);
    responseBuilder.setStatus(RESPONSE_SUCCESS);
    log.debug("End of createPolicyType");
    RpcResult<CreatePolicyTypeOutput> rpcResult = RpcResultBuilder
        .<CreatePolicyTypeOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<DeletePolicyInstanceOutput>> deletePolicyInstance(
      DeletePolicyInstanceInput input) {
    log.debug("Start of deletePolicyInstance");
    String uri = nearRicUrlProvider.getPolicyInstanceId(String.valueOf(input.getPolicyTypeId()),
        String.valueOf(input.getPolicyInstanceId()));
    restAdapter.delete(uri);
    DeletePolicyInstanceOutputBuilder responseBuilder = new DeletePolicyInstanceOutputBuilder();
    log.debug("End of deletePolicyInstance");
    RpcResult<DeletePolicyInstanceOutput> rpcResult = RpcResultBuilder
        .<DeletePolicyInstanceOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<DeletePolicyTypeOutput>> deletePolicyType(
      DeletePolicyTypeInput input) {
    log.debug("Start of deletePolicyType");
    String uri = nearRicUrlProvider.getPolicyTypeId(String.valueOf(input.getPolicyTypeId()));
    restAdapter.delete(uri);
    DeletePolicyTypeOutputBuilder responseBuilder = new DeletePolicyTypeOutputBuilder();
    log.debug("End of deletePolicyType");
    RpcResult<DeletePolicyTypeOutput> rpcResult = RpcResultBuilder
        .<DeletePolicyTypeOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetHealthCheckOutput>> getHealthCheck(
      GetHealthCheckInput input) {
    log.debug("Start of getHealthCheck");
    String uri = nearRicUrlProvider.getHealthCheck();
    Optional<String> heathCheckStatus = restAdapter.get(uri, String.class);
    GetHealthCheckOutputBuilder responseBuilder = new GetHealthCheckOutputBuilder();
    if (heathCheckStatus.get().equals("")) {
      responseBuilder.setHealthStatus(true);
    }
    log.debug("End of getHealthCheck");
    RpcResult<GetHealthCheckOutput> rpcResult = RpcResultBuilder.<GetHealthCheckOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetNearRTRICsOutput>> getNearRTRICs(GetNearRTRICsInput input) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyInstanceOutput>> getPolicyInstance(
      GetPolicyInstanceInput input) {
    log.debug("Start of getPolicyInstance");
    String uri = nearRicUrlProvider.getPolicyInstanceId(String.valueOf(input.getPolicyTypeId()),
        String.valueOf(input.getPolicyInstanceId()));
    Optional<String> policyInstance = restAdapter.get(uri, String.class);
    GetPolicyInstanceOutputBuilder responseBuilder = new GetPolicyInstanceOutputBuilder();
    responseBuilder
        .setPolicyInstance(policyInstance.isPresent() ? policyInstance.get() : StringUtils.EMPTY);
    log.debug("End of getPolicyInstance");
    RpcResult<GetPolicyInstanceOutput> rpcResult = RpcResultBuilder
        .<GetPolicyInstanceOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyInstancesOutput>> getPolicyInstances(
      GetPolicyInstancesInput input) {
    log.debug("Start of getPolicyInstances");
    String uri = nearRicUrlProvider.getPolicyInstances(String.valueOf(input.getPolicyTypeId()));
    Optional<List<String>> policyInstances = restAdapter.get(uri, List.class);
    GetPolicyInstancesOutputBuilder responseBuilder = new GetPolicyInstancesOutputBuilder();
    if (policyInstances.isPresent()) {
      responseBuilder.setPolicyInstanceIdList(policyInstances.get());
    }
    log.debug("End of getPolicyInstances");
    RpcResult<GetPolicyInstancesOutput> rpcResult = RpcResultBuilder
        .<GetPolicyInstancesOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyTypeOutput>> getPolicyType(GetPolicyTypeInput input) {
    log.debug("Start of getPolicyType");
    log.debug("Policy Type Id : ", input.getPolicyTypeId());
    String uri = nearRicUrlProvider.getPolicyTypeId(String.valueOf(input.getPolicyTypeId()));
    Optional<PolicyTypeSchema> policyTypeSchema = restAdapter.get(uri, PolicyTypeSchema.class);
    GetPolicyTypeOutputBuilder responseBuilder = new GetPolicyTypeOutputBuilder();
    if (policyTypeSchema.isPresent()) {
      responseBuilder.setDescription(policyTypeSchema.get().getDescription());
      responseBuilder.setName(policyTypeSchema.get().getName());
      responseBuilder.setPolicyType(policyTypeSchema.get().getCreateSchema().toString());
    }
    log.debug("End of getPolicyType");
    RpcResult<GetPolicyTypeOutput> rpcResult = RpcResultBuilder.<GetPolicyTypeOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyTypesOutput>> getPolicyTypes(
      GetPolicyTypesInput input) {
    log.debug("Start of getPolicyTypes");
    String uri = nearRicUrlProvider.getPolicyTypes();
    Optional<List<Long>> policyTypes = restAdapter.get(uri, List.class);
    GetPolicyTypesOutputBuilder responseBuilder = new GetPolicyTypesOutputBuilder();
    responseBuilder.setPolicyTypeIdList(policyTypes.get());
    log.debug("End of getPolicyTypes");
    RpcResult<GetPolicyTypesOutput> rpcResult = RpcResultBuilder.<GetPolicyTypesOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetStatusOutput>> getStatus(GetStatusInput input) {
    log.debug("Start of getStatus");
    String uri = nearRicUrlProvider.getPolicyInstanceIdStatus(
        String.valueOf(input.getPolicyTypeId()), String.valueOf(input.getPolicyInstanceId()));
    Optional<List<String>> policyTypes = restAdapter.get(uri, List.class);
    GetStatusOutputBuilder responseBuilder = new GetStatusOutputBuilder();
    // TODO:
    /*
     * No Schema defined for the response so Identify a way to parse the schema or create the java
     * object
     */
    responseBuilder.setStatus(RESPONSE_SUCCESS);
    log.debug("End of getStatus");
    RpcResult<GetStatusOutput> rpcResult =
        RpcResultBuilder.<GetStatusOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }
}
