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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.onap.sdnc.northbound.restadapter.RestAdapter;
import org.onap.sdnc.northbound.restadapter.RestAdapterImpl;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.A1ADAPTERAPIService;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeleteA1PolicyInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeleteA1PolicyOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.DeleteA1PolicyOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyInputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyStatusInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyStatusOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyStatusOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyTypeInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyTypeOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.GetA1PolicyTypeOutputBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutA1PolicyInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutA1PolicyOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.a1.adapter.rev200122.PutA1PolicyOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.ietf.params.xml.ns.yang.ietf.inet.types.rev130715.Uri;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;

/**
 * Defines a base implementation for your provider. This class overrides the
 * generated interface from the YANG model and implements the request model for
 * the A1 interface. This class identifies the Near-RT RIC throught the IP passed
 * over the payload and calls the corresponding Near-RT RIC over Rest API
 *
 * <pre>
 *
 * @author lathishbabu.ganesan@est.tech
 *
 */

@SuppressWarnings("squid:S1874") // "@Deprecated" code should not be used
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

  public NonrtRicApiProvider(DataBroker dataBroker, NotificationPublishService notificationPublishService,
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

  public void setDataBroker(DataBroker dataBroker) {
    this.dataBroker = dataBroker;
    if (log.isDebugEnabled()) {
      log.debug("DataBroker set to {}", dataBroker == null ? NULL_PARAM : NON_NULL_PARAM);
    }
  }

  public void setNotificationService(NotificationPublishService notificationService) {
    this.notificationService = notificationService;
    if (log.isDebugEnabled()) {
      log.debug("Notification Service set to {}", notificationService == null ? NULL_PARAM : NON_NULL_PARAM);
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
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public ListenableFuture<RpcResult<PutA1PolicyOutput>> putA1Policy(PutA1PolicyInput input) {
    log.info("Start of putPolicy");
    final Uri uri = input.getNearRtRicUrl();

    log.info("PUT Request input.GetA1Policy() : {} ", uri);
    ResponseEntity<String> response = restAdapter.put(uri.getValue(), input.getBody(), String.class);
    PutA1PolicyOutputBuilder responseBuilder = new PutA1PolicyOutputBuilder();
    if (response.hasBody()) {
      log.info("Response PutA1Policy : {} ", response.getBody());
      responseBuilder.setBody(response.getBody());
    }
    responseBuilder.setHttpStatus(response.getStatusCodeValue());
    log.info("End of PutA1Policy");
    RpcResult<PutA1PolicyOutput> rpcResult = RpcResultBuilder.<PutA1PolicyOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<DeleteA1PolicyOutput>> deleteA1Policy(DeleteA1PolicyInput input) {
    log.info("Start of DeleteA1Policy");
    final Uri uri = input.getNearRtRicUrl();
    ResponseEntity<Object> response = restAdapter.delete(uri.getValue());
    log.info("End of DeleteA1Policy");
    DeleteA1PolicyOutputBuilder responseBuilder = new DeleteA1PolicyOutputBuilder();
    if (response.hasBody()) {
      log.info("Response PutA1Policy : {} ", response.getBody());
      responseBuilder.setBody(response.getBody().toString());
    }
    responseBuilder.setHttpStatus(response.getStatusCodeValue());
    RpcResult<DeleteA1PolicyOutput> rpcResult = RpcResultBuilder.<DeleteA1PolicyOutput>status(true)
        .withResult(responseBuilder.build()).build();
    return Futures.immediateFuture(rpcResult);
  }

  private GetA1PolicyOutput getA1(GetA1PolicyInput input) {
    log.info("Start of getA1");
    final Uri uri = input.getNearRtRicUrl();
    ResponseEntity<String> response = restAdapter.get(uri.getValue(), String.class);
    log.info("End of getA1");
    GetA1PolicyOutputBuilder responseBuilder = new GetA1PolicyOutputBuilder();
    if (response.hasBody()) {
      log.info("Response getA1 : {} ", response.getBody());
      responseBuilder.setBody(response.getBody());
    }
    responseBuilder.setHttpStatus(response.getStatusCodeValue());
    return responseBuilder.build();
  }

  @Override
  public ListenableFuture<RpcResult<GetA1PolicyOutput>> getA1Policy(GetA1PolicyInput input) {
    GetA1PolicyOutput output = getA1(input);
    RpcResult<GetA1PolicyOutput> rpcResult = RpcResultBuilder.<GetA1PolicyOutput>status(true).withResult(output)
        .build();
    return Futures.immediateFuture(rpcResult);
  }

  @Override
  public ListenableFuture<RpcResult<GetA1PolicyStatusOutput>> getA1PolicyStatus(GetA1PolicyStatusInput input) {
    GetA1PolicyInputBuilder getInputBuilder = new GetA1PolicyInputBuilder();
    getInputBuilder.setNearRtRicUrl(input.getNearRtRicUrl());
    GetA1PolicyOutput getOutput = getA1(getInputBuilder.build());

    GetA1PolicyStatusOutputBuilder outputBuilder = new GetA1PolicyStatusOutputBuilder();
    outputBuilder.setBody(getOutput.getBody());
    outputBuilder.setHttpStatus(getOutput.getHttpStatus());

    return Futures.immediateFuture(RpcResultBuilder.<GetA1PolicyStatusOutput>status(true) //
        .withResult(outputBuilder.build()) //
        .build());
  }

  @Override
  public ListenableFuture<RpcResult<GetA1PolicyTypeOutput>> getA1PolicyType(GetA1PolicyTypeInput input) {
    GetA1PolicyInputBuilder getInputBuilder = new GetA1PolicyInputBuilder();
    getInputBuilder.setNearRtRicUrl(input.getNearRtRicUrl());
    GetA1PolicyOutput getOutput = getA1(getInputBuilder.build());

    GetA1PolicyTypeOutputBuilder outputBuilder = new GetA1PolicyTypeOutputBuilder();
    outputBuilder.setBody(getOutput.getBody());
    outputBuilder.setHttpStatus(getOutput.getHttpStatus());

    return Futures.immediateFuture(RpcResultBuilder.<GetA1PolicyTypeOutput>status(true) //
        .withResult(outputBuilder.build()) //
        .build());
  }

}
