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

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONObject;
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
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev191002.GetNearRTRICsOutputBuilder;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    log.info("Start of createPolicyInstance");
    String uri = nearRicUrlProvider.getPolicyInstanceId(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()), String.valueOf(input.getPolicyInstanceId()));
    log.info("PUT Request input.getPolicyInstance() : {} ", input.getPolicyInstance());
    restAdapter.put(uri, input.getPolicyInstance());
    CreatePolicyInstanceOutputBuilder responseBuilder = new CreatePolicyInstanceOutputBuilder();
    log.info("End of createPolicyInstance");
    RpcResult<CreatePolicyInstanceOutput> rpcResult = RpcResultBuilder
        .<CreatePolicyInstanceOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<CreatePolicyTypeOutput>> createPolicyType(
      CreatePolicyTypeInput input) {
    log.info("Start of createPolicyType");
    String uri = nearRicUrlProvider.getPolicyTypeId(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()));
    log.info("PUT Request input.getPolicyType() : {} ", input.getPolicyType());
    restAdapter.put(uri, input.getPolicyType());
    CreatePolicyTypeOutputBuilder responseBuilder = new CreatePolicyTypeOutputBuilder();
    responseBuilder.setCode(RESPONSE_CODE_SUCCESS);
    responseBuilder.setStatus(RESPONSE_SUCCESS);
    log.info("End of createPolicyType");
    RpcResult<CreatePolicyTypeOutput> rpcResult = RpcResultBuilder
        .<CreatePolicyTypeOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<DeletePolicyInstanceOutput>> deletePolicyInstance(
      DeletePolicyInstanceInput input) {
    log.info("Start of deletePolicyInstance");
    String uri = nearRicUrlProvider.getPolicyInstanceId(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()), String.valueOf(input.getPolicyInstanceId()));
    restAdapter.delete(uri);
    DeletePolicyInstanceOutputBuilder responseBuilder = new DeletePolicyInstanceOutputBuilder();
    log.info("End of deletePolicyInstance");
    RpcResult<DeletePolicyInstanceOutput> rpcResult = RpcResultBuilder
        .<DeletePolicyInstanceOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<DeletePolicyTypeOutput>> deletePolicyType(
      DeletePolicyTypeInput input) {
    log.info("Start of deletePolicyType");
    String uri = nearRicUrlProvider.getPolicyTypeId(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()));
    restAdapter.delete(uri);
    DeletePolicyTypeOutputBuilder responseBuilder = new DeletePolicyTypeOutputBuilder();
    log.info("End of deletePolicyType");
    RpcResult<DeletePolicyTypeOutput> rpcResult = RpcResultBuilder
        .<DeletePolicyTypeOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetHealthCheckOutput>> getHealthCheck(
      GetHealthCheckInput input) {
    log.info("Start of getHealthCheck");
    String uri = nearRicUrlProvider.getHealthCheck(String.valueOf(input.getNearRtRicId()));
    restAdapter.get(uri, String.class);
    GetHealthCheckOutputBuilder responseBuilder = new GetHealthCheckOutputBuilder();
    responseBuilder.setHealthStatus(true);
    log.info("End of getHealthCheck");
    RpcResult<GetHealthCheckOutput> rpcResult = RpcResultBuilder.<GetHealthCheckOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetNearRTRICsOutput>> getNearRTRICs(GetNearRTRICsInput input) {
      log.info("Start of getNearRTRICs");
      GetNearRTRICsOutputBuilder responseBuilder = new GetNearRTRICsOutputBuilder();
      responseBuilder.setNearRtRicIdList(nearRicUrlProvider.getNearRTRicIdsList());
      log.info("End of getNearRTRICs");
      RpcResult<GetNearRTRICsOutput> rpcResult = RpcResultBuilder.<GetNearRTRICsOutput>status(true)
          .withResult(responseBuilder.build()).build();
      return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyInstanceOutput>> getPolicyInstance(
      GetPolicyInstanceInput input) {
    log.info("Start of getPolicyInstance");
    log.info("Policy Type Id : {},  Policy Instance Id : {}", input.getPolicyTypeId(), input.getPolicyInstanceId());
    String uri = nearRicUrlProvider.getPolicyInstanceId(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()), String.valueOf(input.getPolicyInstanceId()));
    Optional<String> policyInstance = restAdapter.get(uri, String.class);
    GetPolicyInstanceOutputBuilder responseBuilder = new GetPolicyInstanceOutputBuilder();
    if (policyInstance.isPresent()) {
        log.info("Response policyInstance.get() : {} ", policyInstance.get());
        responseBuilder.setPolicyInstance(policyInstance.get());
    }
    log.info("End of getPolicyInstance");
    RpcResult<GetPolicyInstanceOutput> rpcResult = RpcResultBuilder
        .<GetPolicyInstanceOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyInstancesOutput>> getPolicyInstances(
      GetPolicyInstancesInput input) {
    log.info("Start of getPolicyInstances");
    String uri = nearRicUrlProvider.getPolicyInstances(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()));
    Optional<List<String>> policyInstances = restAdapter.get(uri, List.class);
    GetPolicyInstancesOutputBuilder responseBuilder = new GetPolicyInstancesOutputBuilder();
    if (policyInstances.isPresent()) {
      log.info("Response policyInstances.get() : {} ", policyInstances.get());
      responseBuilder.setPolicyInstanceIdList(policyInstances.get());
    }
    log.info("End of getPolicyInstances");
    RpcResult<GetPolicyInstancesOutput> rpcResult = RpcResultBuilder
        .<GetPolicyInstancesOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyTypeOutput>> getPolicyType(GetPolicyTypeInput input) {
    log.info("Start of getPolicyType");
    log.info("Policy Type Id : {} ", input.getPolicyTypeId());
    String uri = nearRicUrlProvider.getPolicyTypeId(String.valueOf(input.getNearRtRicId()),
            String.valueOf(input.getPolicyTypeId()));
    Optional<String> policyType = restAdapter.get(uri, String.class);
    GetPolicyTypeOutputBuilder responseBuilder = new GetPolicyTypeOutputBuilder();
    if (policyType.isPresent()) {
      log.info("Response policyType.get() : {} ", policyType.get());
      JSONObject policyTypeObj = new JSONObject(policyType.get());
      responseBuilder.setDescription(policyTypeObj.getString("description"));
      responseBuilder.setName(policyTypeObj.getString("name"));
      responseBuilder.setPolicyType(policyTypeObj.getJSONObject("create_schema").toString());
    }
    log.info("End of getPolicyType");
    RpcResult<GetPolicyTypeOutput> rpcResult = RpcResultBuilder.<GetPolicyTypeOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyTypesOutput>> getPolicyTypes(
      GetPolicyTypesInput input) {
    log.info("Start of getPolicyTypes");
    String uri = nearRicUrlProvider.getPolicyTypes(String.valueOf(input.getNearRtRicId()));
    Optional<List<Integer>> policyTypes = restAdapter.get(uri, List.class);
    GetPolicyTypesOutputBuilder responseBuilder = new GetPolicyTypesOutputBuilder();
    if (policyTypes.isPresent()) {
        log.info("Response policyTypes.get() : {} ", policyTypes.get());
        List<Integer> policyTypesListInteger = policyTypes.get();
        List<Long> policyTypesListLong = new ArrayList<>();
        for(Integer i : policyTypesListInteger){
            policyTypesListLong.add(i.longValue());
        }
        responseBuilder.setPolicyTypeIdList(policyTypesListLong);
    }
    log.info("End of getPolicyTypes");
    RpcResult<GetPolicyTypesOutput> rpcResult = RpcResultBuilder.<GetPolicyTypesOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetStatusOutput>> getStatus(GetStatusInput input) {
    log.info("Start of getStatus");
    String uri = nearRicUrlProvider.getPolicyInstanceIdStatus(String.valueOf(input.getNearRtRicId()),
        String.valueOf(input.getPolicyTypeId()), String.valueOf(input.getPolicyInstanceId()));
    Optional<String> status = restAdapter.get(uri, String.class);
    GetStatusOutputBuilder responseBuilder = new GetStatusOutputBuilder();
    if (status.isPresent()) {
        log.info("Response status.get() : {} ", status.get());
        JSONObject statusObj = new JSONObject(status.get());
        responseBuilder.setStatus(statusObj.getString("status"));
    }
    log.info("End of getStatus");
    RpcResult<GetStatusOutput> rpcResult =
        RpcResultBuilder.<GetStatusOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }
}
