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

import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.onap.sdnc.northbound.restadapter.NearRicUrlProvider;
import org.onap.sdnc.northbound.restadapter.RestAdapter;
import org.onap.sdnc.northbound.restadapter.RestAdapterImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.A1ADAPTERAPIService;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeletePolicyInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeletePolicyOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeletePolicyOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyIdentitiesInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyIdentitiesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyIdentitiesOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeIdentitiesInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeIdentitiesOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeIdentitiesOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetPolicyTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutPolicyInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutPolicyOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutPolicyOutputBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

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
  public ListenableFuture<RpcResult<GetPolicyTypeIdentitiesOutput>> getPolicyTypeIdentities(
          GetPolicyTypeIdentitiesInput input) {
    log.info("Start of getPolicyTypeIdentities");
    GetPolicyTypeIdentitiesOutputBuilder responseBuilder = new GetPolicyTypeIdentitiesOutputBuilder();
    String uri = nearRicUrlProvider.policyTypesUrl(String.valueOf(input.getNearRtRicUrl()));
    ResponseEntity<List<String>> response = restAdapter.get(uri, List.class);
    if (response.hasBody()) {
      log.info("Response getPolicyTypeIdentities : {} ", response.getBody());
      responseBuilder.setPolicyTypeIdList(response.getBody());
    }
    log.info("End of getPolicyTypeIdentities");
    RpcResult<GetPolicyTypeIdentitiesOutput> rpcResult = RpcResultBuilder.<GetPolicyTypeIdentitiesOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyIdentitiesOutput>> getPolicyIdentities(GetPolicyIdentitiesInput input) {
    log.info("Start of getPolicyIdentities");
    GetPolicyIdentitiesOutputBuilder responseBuilder = new GetPolicyIdentitiesOutputBuilder();
    String uri = nearRicUrlProvider.policiesUrl(String.valueOf(input.getNearRtRicUrl()));
    ResponseEntity<List<String>> response = restAdapter.get(uri, List.class);
    if (response.hasBody()) {
      log.info("Response getPolicyIdentities : {} ", response.getBody());
      responseBuilder.setPolicyIdList(response.getBody());
    }
    log.info("End of getPolicyIdentities");
    RpcResult<GetPolicyIdentitiesOutput> rpcResult = RpcResultBuilder
        .<GetPolicyIdentitiesOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetPolicyTypeOutput>> getPolicyType(GetPolicyTypeInput input) {
    log.info("Start of getPolicyType");
    log.info("Policy Type Id : {} ", input.getPolicyTypeId());
    GetPolicyTypeOutputBuilder responseBuilder = new GetPolicyTypeOutputBuilder();
    String uri = nearRicUrlProvider.getPolicyTypeUrl(String.valueOf(input.getNearRtRicUrl()),
            String.valueOf(input.getPolicyTypeId()));
    ResponseEntity<String> response = restAdapter.get(uri, String.class);
    if (response.hasBody()) {
      log.info("Response getPolicyType : {} ", response.getBody());
      responseBuilder.setPolicyType(response.getBody());
    }
    log.info("End of getPolicyType");
    RpcResult<GetPolicyTypeOutput> rpcResult = RpcResultBuilder.<GetPolicyTypeOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<PutPolicyOutput>> putPolicy(PutPolicyInput input) {
    log.info("Start of putPolicy");
    PutPolicyOutputBuilder responseBuilder = new PutPolicyOutputBuilder();
    String uri = nearRicUrlProvider.putPolicyUrl(String.valueOf(input.getNearRtRicUrl()),
            String.valueOf(input.getPolicyId()), String.valueOf(input.getPolicyTypeId()));
    log.info("PUT Request input.getPolicy() : {} ", input.getPolicy());
    ResponseEntity<String> response = restAdapter.put(uri, input.getPolicy(), String.class);
    if (response.hasBody()) {
      log.info("Response putPolicy : {} ", response.getBody());
      responseBuilder.setReturnedPolicy(response.getBody());
    }
    log.info("End of putPolicy");
    RpcResult<PutPolicyOutput> rpcResult = RpcResultBuilder
        .<PutPolicyOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<DeletePolicyOutput>> deletePolicy(DeletePolicyInput input) {
    log.info("Start of deletePolicy");
    DeletePolicyOutputBuilder responseBuilder = new DeletePolicyOutputBuilder();
    String uri = nearRicUrlProvider.deletePolicyUrl(String.valueOf(input.getNearRtRicUrl()),
            String.valueOf(input.getPolicyId()));
    ResponseEntity<Void> response = restAdapter.delete(uri);
    log.info("End of deletePolicy");
    RpcResult<DeletePolicyOutput> rpcResult = RpcResultBuilder
        .<DeletePolicyOutput>status(true).withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }
}
