/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
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
 * ============LICENSE_END=========================================================
 */
package org.onap.sdnc.vnfapi;

import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.DataObjectModification;
import org.opendaylight.controller.md.sal.binding.api.DataTreeModification;
import org.opendaylight.controller.md.sal.binding.api.NotificationPublishService;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.ReadWriteTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker;
import org.opendaylight.controller.sal.binding.api.RpcProviderRegistry;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.NetworkTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.NetworkTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadNetworkTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadNetworkTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModuleTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModuleTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVfModules;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstanceTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstanceTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfInstances;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.PreloadVnfs;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VNFAPIService;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModules;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstanceTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstanceTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfInstances;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VnfTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.Vnfs;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.request.information.NetworkRequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.topology.identifier.NetworkTopologyIdentifier;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.network.topology.information.NetworkTopologyInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.data.PreloadData;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.data.PreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.model.information.VnfPreloadList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.vf.module.model.information.VfModulePreloadList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.vnf.instance.model.information.VnfInstancePreloadList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.sdnc.request.header.SdncRequestHeader;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceData;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.information.ServiceInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.status.ServiceStatus;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.status.ServiceStatusBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.identifiers.VfModuleIdentifiers;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.model.infrastructure.VfModuleList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.preload.data.VfModulePreloadData;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.preload.data.VfModulePreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.request.information.VfModuleRequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.service.data.VfModuleServiceData;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.service.data.VfModuleServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.topology.information.VfModuleTopologyInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.identifiers.VnfInstanceIdentifiers;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.model.infrastructure.VnfInstanceList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.preload.data.VnfInstancePreloadData;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.preload.data.VnfInstancePreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.request.information.VnfInstanceRequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.service.data.VnfInstanceServiceData;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.service.data.VnfInstanceServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.topology.information.VnfInstanceTopologyInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfList;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.model.infrastructure.VnfListKey;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.request.information.VnfRequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.topology.identifier.VnfTopologyIdentifier;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.topology.information.VnfTopologyInformation;
import org.opendaylight.yangtools.yang.binding.DataObject;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.powermock.reflect.Whitebox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class VnfapiProviderTestBase {
    @Rule public MockitoRule rule = MockitoJUnit.rule();

    @Mock private DataBroker dataBroker;
    @Mock private NotificationPublishService notificationPublishService;
    @Mock private RpcProviderRegistry rpcProviderRegistry;
    @Mock private VNFSDNSvcLogicServiceClient vnfsdnSvcLogicServiceClient;
    @Mock private ReadWriteTransaction readWriteTransactionInCreateContainer;
    @Mock private ReadWriteTransaction readWriteTransactionInDataChanged;
    @Mock private CheckedFuture<Void, TransactionCommitFailedException> checkedFuture;

    private VnfApiProvider vnfapiProvider;
    private String vfModuleName;
    private String vfModuleModelId;
    private String vnfInstanceId;
    private String vnfInstanceName;
    private String vnfModelId;
    private String svcRequestId;
    private String serviceInstanceId;
    private String vnfName;
    private String vnfType;
    private String vfModuleId;

    @Before public void setUp() throws Exception {
        doReturn(readWriteTransactionInCreateContainer).when(dataBroker).newReadWriteTransaction();
        doReturn(checkedFuture).when(readWriteTransactionInCreateContainer).submit();
        // mock readOnlyTransaction
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        doReturn(readOnlyTransaction).when(dataBroker).newReadOnlyTransaction();
        doReturn(checkedFuture).when(readOnlyTransaction).read(any(), any());
        // mock writeTransaction
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        doReturn(writeTransaction).when(dataBroker).newWriteOnlyTransaction();
        doReturn(checkedFuture).when(writeTransaction).submit();

        vnfapiProvider = spy(new VnfApiProvider(dataBroker, notificationPublishService, rpcProviderRegistry,
            vnfsdnSvcLogicServiceClient));
    }

    @After public void tearDown() throws Exception {

    }

    @Test public void close() throws Exception {
        ExecutorService executor = Whitebox.getInternalState(vnfapiProvider, "executor");
        BindingAwareBroker.RpcRegistration<VNFAPIService> vnfapiServiceRpcRegistration =
            mock(BindingAwareBroker.RpcRegistration.class);
        vnfapiProvider.rpcRegistration = vnfapiServiceRpcRegistration;

        vnfapiProvider.close();

        Assert.assertTrue(executor.isShutdown());
        verify(vnfapiServiceRpcRegistration, times(1)).close();
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getVnfInstanceRequestInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void vnfInstanceTopologyOperationErrorOne() throws Exception {
        VnfInstanceTopologyOperationInput vnfInstanceTopologyOperationInput =
            mock(VnfInstanceTopologyOperationInput.class);
        doReturn(null).when(vnfInstanceTopologyOperationInput).getVnfInstanceRequestInformation();

        Future<RpcResult<VnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfInstanceTopologyOperation(vnfInstanceTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vnf-instance-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>viid == null</code>
     *
     * @throws Exception
     */
    @Test public void vnfInstanceTopologyOperationErrorTwo() throws Exception {
        vnfInstanceId = "";
        vnfInstanceName = "vnf-instance-name";
        vnfModelId = "vnf-model-id";
        VnfInstanceTopologyOperationInput vnfInstanceTopologyOperationInput =
            mock(VnfInstanceTopologyOperationInput.class);
        VnfInstanceRequestInformation vnfInstanceRequestInformation = mock(VnfInstanceRequestInformation.class);
        doReturn(vnfInstanceName).when(vnfInstanceRequestInformation).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceRequestInformation).getVnfModelId();
        doReturn(vnfInstanceId).when(vnfInstanceRequestInformation).getVnfInstanceId();
        doReturn(vnfInstanceRequestInformation).when(vnfInstanceTopologyOperationInput)
            .getVnfInstanceRequestInformation();

        Future<RpcResult<VnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfInstanceTopologyOperation(vnfInstanceTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vnf-instance-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API",SVC_OPERATION,null,"sync" = false</code>
     *
     * @throws Exception
     */
    @Test public void vnfInstanceTopologyOperationErrorThree() throws Exception {
        vnfInstanceId = "vnf-instance-id";
        vnfInstanceName = "vnf-instance-name";
        vnfModelId = "vnf-model-id";
        svcRequestId = "svc-request-id";

        VnfInstanceTopologyOperationInput vnfInstanceTopologyOperationInput =
            mock(VnfInstanceTopologyOperationInput.class);
        VnfInstanceRequestInformation vnfInstanceRequestInformation = mock(VnfInstanceRequestInformation.class);
        doReturn(vnfInstanceId).when(vnfInstanceRequestInformation).getVnfInstanceId();
        doReturn(vnfInstanceName).when(vnfInstanceRequestInformation).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceRequestInformation).getVnfModelId();
        doReturn(vnfInstanceRequestInformation).when(vnfInstanceTopologyOperationInput)
            .getVnfInstanceRequestInformation();

        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(vnfInstanceTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();

        Future<RpcResult<VnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfInstanceTopologyOperation(vnfInstanceTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void vnfInstanceTopologyOperationSuccess() throws Exception {
        vnfInstanceId = "vnf-instance-id";
        vnfInstanceName = "vnf-instance-name";
        vnfModelId = "vnf-model-id";
        svcRequestId = "svc-request-id";

        VnfInstanceTopologyOperationInput vnfInstanceTopologyOperationInput =
            mock(VnfInstanceTopologyOperationInput.class);
        VnfInstanceRequestInformation vnfInstanceRequestInformation = mock(VnfInstanceRequestInformation.class);
        doReturn(vnfInstanceId).when(vnfInstanceRequestInformation).getVnfInstanceId();
        doReturn(vnfInstanceName).when(vnfInstanceRequestInformation).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceRequestInformation).getVnfModelId();
        doReturn(vnfInstanceRequestInformation).when(vnfInstanceTopologyOperationInput)
            .getVnfInstanceRequestInformation();

        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(vnfInstanceTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();
        doReturn(SdncRequestHeader.SvcAction.Activate).when(sdncRequestHeader).getSvcAction();
        ReadOnlyTransaction readOnlyTransaction = mock(ReadOnlyTransaction.class);
        doReturn(readOnlyTransaction).when(dataBroker).newReadOnlyTransaction();
        doReturn(checkedFuture).when(readOnlyTransaction).read(any(), any());
        WriteTransaction writeTransaction = mock(WriteTransaction.class);
        doReturn(writeTransaction).when(dataBroker).newWriteOnlyTransaction();
        doReturn(checkedFuture).when(writeTransaction).submit();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<VnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfInstanceTopologyOperation(vnfInstanceTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getVfModuleTopologyInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void vfModuleTopologyOperationErrorOne() throws Exception {
        VfModuleTopologyOperationInput vfModuleTopologyOperationInput = mock(VfModuleTopologyOperationInput.class);
        doReturn(null).when(vfModuleTopologyOperationInput).getVfModuleRequestInformation();
        Future<RpcResult<VfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vfModuleTopologyOperation(vfModuleTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vf-module-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>vfid = ""</code>
     * And
     * <p>
     * <code>viid = ""</code>
     *
     * @throws Exception
     */
    @Test public void vfModuleTopologyOperationErrorTwo() throws Exception {
        // vifd = ""
        vfModuleName = "vfModuleName";
        vfModuleModelId = "vfModuleModelId";
        vfModuleId = "";
        vnfInstanceId = "";
        VfModuleTopologyOperationInput vfModuleTopologyOperationInput = mock(VfModuleTopologyOperationInput.class);
        VfModuleRequestInformation vfModuleRequestInformation = mock(VfModuleRequestInformation.class);
        doReturn(vfModuleRequestInformation).when(vfModuleTopologyOperationInput).getVfModuleRequestInformation();
        VfModuleIdentifiers vfModuleIdentifiers = mock(VfModuleIdentifiers.class);
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();
        doReturn(vfModuleId).when(vfModuleRequestInformation).getVfModuleId();

        Future<RpcResult<VfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vfModuleTopologyOperation(vfModuleTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vf-module-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());

        // viid = ""
        vfModuleId = "vfModuleId";
        doReturn(vfModuleId).when(vfModuleRequestInformation).getVfModuleId();
        doReturn(vnfInstanceId).when(vfModuleRequestInformation).getVnfInstanceId();
        rpcResultFuture = vnfapiProvider.vfModuleTopologyOperation(vfModuleTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vnf-instance-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API", SVC_OPERATION, null, "sync") = false</code>
     *
     * @throws Exception
     */
    @Test public void vfModuleTopologyOperationErrorThree() throws Exception {
        // vifd = ""
        vfModuleName = "vfModuleName";
        vfModuleModelId = "vfModuleModelId";
        vfModuleId = "vfModuleId";
        vnfInstanceId = "vnfInstanceId";
        VfModuleTopologyOperationInput vfModuleTopologyOperationInput = mock(VfModuleTopologyOperationInput.class);
        VfModuleRequestInformation vfModuleRequestInformation = mock(VfModuleRequestInformation.class);
        doReturn(vfModuleRequestInformation).when(vfModuleTopologyOperationInput).getVfModuleRequestInformation();
        VfModuleIdentifiers vfModuleIdentifiers = mock(VfModuleIdentifiers.class);
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();
        doReturn(vfModuleId).when(vfModuleRequestInformation).getVfModuleId();
        doReturn(vnfInstanceId).when(vfModuleRequestInformation).getVnfInstanceId();
        // mock sdncRequestHeader
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(vfModuleTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();

        Future<RpcResult<VfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vfModuleTopologyOperation(vfModuleTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void vfModuleTopologyOperationSuccess() throws Exception {
        vfModuleName = "vfModuleName";
        vfModuleModelId = "vfModuleModelId";
        vfModuleId = "vfModuleId";
        vnfInstanceId = "vnfInstanceId";
        VfModuleTopologyOperationInput vfModuleTopologyOperationInput = mock(VfModuleTopologyOperationInput.class);
        VfModuleRequestInformation vfModuleRequestInformation = mock(VfModuleRequestInformation.class);
        doReturn(vfModuleRequestInformation).when(vfModuleTopologyOperationInput).getVfModuleRequestInformation();
        VfModuleIdentifiers vfModuleIdentifiers = mock(VfModuleIdentifiers.class);
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();
        doReturn(vfModuleId).when(vfModuleRequestInformation).getVfModuleId();
        doReturn(vnfInstanceId).when(vfModuleRequestInformation).getVnfInstanceId();
        // mock sdncRequestHeader
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(vfModuleTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<VfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vfModuleTopologyOperation(vfModuleTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getServiceInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void vnfTopologyOperationErrorOne() throws Exception {
        VnfTopologyOperationInput vnfTopologyOperationInput = mock(VnfTopologyOperationInput.class);
        doReturn(null).when(vnfTopologyOperationInput).getServiceInformation();

        Future<RpcResult<VnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfTopologyOperation(vnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty service-instance-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getVnfRequestInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void vnfTopologyOperationErrorTwo() throws Exception {
        serviceInstanceId = "serviceInstanceId";
        VnfTopologyOperationInput vnfTopologyOperationInput = mock(VnfTopologyOperationInput.class);
        ServiceInformation serviceInformation = mock(ServiceInformation.class);
        doReturn(serviceInformation).when(vnfTopologyOperationInput).getServiceInformation();
        doReturn(serviceInstanceId).when(serviceInformation).getServiceInstanceId();

        Future<RpcResult<VnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfTopologyOperation(vnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vf-module-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API", SVC_OPERATION, null, "sync") == false</code>
     *
     * @throws Exception
     */
    @Test public void vnfTopologyOperationErrorThree() throws Exception {
        serviceInstanceId = "serviceInstanceId";
        String vnfId = "vnfId";
        VnfTopologyOperationInput vnfTopologyOperationInput = mock(VnfTopologyOperationInput.class);
        ServiceInformation serviceInformation = mock(ServiceInformation.class);
        doReturn(serviceInformation).when(vnfTopologyOperationInput).getServiceInformation();
        doReturn(serviceInstanceId).when(serviceInformation).getServiceInstanceId();

        VnfRequestInformation vnfRequestInformation = mock(VnfRequestInformation.class);
        doReturn(vnfRequestInformation).when(vnfTopologyOperationInput).getVnfRequestInformation();
        doReturn(vnfId).when(vnfRequestInformation).getVnfId();

        Future<RpcResult<VnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfTopologyOperation(vnfTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void vnfTopologyOperationSuccess() throws Exception {
        serviceInstanceId = "serviceInstanceId";
        String vnfId = "vnfId";
        VnfTopologyOperationInput vnfTopologyOperationInput = mock(VnfTopologyOperationInput.class);
        ServiceInformation serviceInformation = mock(ServiceInformation.class);
        doReturn(serviceInformation).when(vnfTopologyOperationInput).getServiceInformation();
        doReturn(serviceInstanceId).when(serviceInformation).getServiceInstanceId();

        VnfRequestInformation vnfRequestInformation = mock(VnfRequestInformation.class);
        doReturn(vnfRequestInformation).when(vnfTopologyOperationInput).getVnfRequestInformation();
        doReturn(vnfId).when(vnfRequestInformation).getVnfId();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<VnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfTopologyOperation(vnfTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getServiceInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void networkTopologyOperationErrorOne() throws Exception {
        VnfTopologyOperationInput vnfTopologyOperationInput = mock(VnfTopologyOperationInput.class);
        doReturn(null).when(vnfTopologyOperationInput).getServiceInformation();

        Future<RpcResult<VnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.vnfTopologyOperation(vnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty service-instance-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getNetworkRequestInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void networkTopologyOperationErrorTwo() throws Exception {
        serviceInstanceId = "serviceInstanceId";
        NetworkTopologyOperationInput networkTopologyOperation = mock(NetworkTopologyOperationInput.class);
        ServiceInformation serviceInformation = mock(ServiceInformation.class);
        doReturn(serviceInstanceId).when(serviceInformation).getServiceInstanceId();
        doReturn(serviceInformation).when(networkTopologyOperation).getServiceInformation();

        Future<RpcResult<NetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.networkTopologyOperation(networkTopologyOperation);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty service-instance-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API", SVC_OPERATION, null, "sync") == false</code>
     *
     * @throws Exception
     */
    @Test public void networkTopologyOperationErrorThree() throws Exception {
        serviceInstanceId = "serviceInstanceId";
        NetworkTopologyOperationInput networkTopologyOperationInput = mock(NetworkTopologyOperationInput.class);
        ServiceInformation serviceInformation = mock(ServiceInformation.class);
        doReturn(serviceInstanceId).when(serviceInformation).getServiceInstanceId();
        doReturn(serviceInformation).when(networkTopologyOperationInput).getServiceInformation();

        NetworkRequestInformation networkRequestInformation = mock(NetworkRequestInformation.class);
        doReturn(networkRequestInformation).when(networkTopologyOperationInput).getNetworkRequestInformation();
        doReturn("NetworkName").when(networkRequestInformation).getNetworkName();

        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(SdncRequestHeader.SvcAction.Assign).when(sdncRequestHeader).getSvcAction();
        doReturn(sdncRequestHeader).when(networkTopologyOperationInput).getSdncRequestHeader();

        Future<RpcResult<NetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.networkTopologyOperation(networkTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void networkTopologyOperationSuccess() throws Exception {
        serviceInstanceId = "serviceInstanceId";
        NetworkTopologyOperationInput networkTopologyOperationInput = mock(NetworkTopologyOperationInput.class);
        ServiceInformation serviceInformation = mock(ServiceInformation.class);
        doReturn(serviceInstanceId).when(serviceInformation).getServiceInstanceId();
        doReturn(serviceInformation).when(networkTopologyOperationInput).getServiceInformation();
        //mock networkRequestInformation
        NetworkRequestInformation networkRequestInformation = mock(NetworkRequestInformation.class);
        doReturn(networkRequestInformation).when(networkTopologyOperationInput).getNetworkRequestInformation();
        doReturn("NetworkName").when(networkRequestInformation).getNetworkName();
        //mock sdncRequestHeader
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(SdncRequestHeader.SvcAction.Assign).when(sdncRequestHeader).getSvcAction();
        doReturn(sdncRequestHeader).when(networkTopologyOperationInput).getSdncRequestHeader();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<NetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.networkTopologyOperation(networkTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getVnfTopologyInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void preloadVnfTopologyOperationErrorOne() throws Exception {
        PreloadVnfTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVnfTopologyOperationInput.class);
        doReturn(null).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();

        Future<RpcResult<PreloadVnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vnf-name or vnf-type",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>preload_name.length() == 0</code>
     * And
     * <code>preload_type.length() == 0</code>
     *
     * @throws Exception
     */
    @Test public void preloadVnfTopologyOperationErrorTwo() throws Exception {
        // preload_name.length() == 0
        vnfName = "";
        vnfType = "vfModuleModelId";
        PreloadVnfTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVnfTopologyOperationInput.class);
        doReturn(null).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();

        VnfTopologyInformation vnfTopologyInformation = mock(VnfTopologyInformation.class);
        doReturn(vnfTopologyInformation).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();
        VnfTopologyIdentifier vnfTopologyIdentifier = mock(VnfTopologyIdentifier.class);
        doReturn(vnfName).when(vnfTopologyIdentifier).getVnfName();
        doReturn(vnfType).when(vnfTopologyIdentifier).getVnfType();
        doReturn(vnfTopologyIdentifier).when(vnfTopologyInformation).getVnfTopologyIdentifier();

        Future<RpcResult<PreloadVnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, invalid preload-name",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());

        // preload_type.length() == 0
        vnfName = "vnfName";
        vnfType = "";
        doReturn(vnfName).when(vnfTopologyIdentifier).getVnfName();
        doReturn(vnfType).when(vnfTopologyIdentifier).getVnfType();

        rpcResultFuture = vnfapiProvider.preloadVnfTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, invalid preload-type",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <code>svcLogicClient.hasGraph("VNF-API", SVC_OPERATION, null, "sync") = false</code>
     *
     * @throws Exception
     */
    @Test public void preloadVnfTopologyOperationErrorThree() throws Exception {
        // preload_name.length() == 0
        vnfName = "vnfName";
        vnfType = "vfModuleModelId";
        PreloadVnfTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVnfTopologyOperationInput.class);
        doReturn(null).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();

        VnfTopologyInformation vnfTopologyInformation = mock(VnfTopologyInformation.class);
        doReturn(vnfTopologyInformation).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();
        VnfTopologyIdentifier vnfTopologyIdentifier = mock(VnfTopologyIdentifier.class);
        doReturn(vnfName).when(vnfTopologyIdentifier).getVnfName();
        doReturn(vnfType).when(vnfTopologyIdentifier).getVnfType();
        doReturn(vnfTopologyIdentifier).when(vnfTopologyInformation).getVnfTopologyIdentifier();

        Future<RpcResult<PreloadVnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void preloadVnfTopologyOperationSuccess() throws Exception {
        // preload_name.length() == 0
        vnfName = "vnfName";
        vnfType = "vfModuleModelId";
        PreloadVnfTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVnfTopologyOperationInput.class);
        doReturn(null).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();

        VnfTopologyInformation vnfTopologyInformation = mock(VnfTopologyInformation.class);
        doReturn(vnfTopologyInformation).when(preloadVnfTopologyOperationInput).getVnfTopologyInformation();
        VnfTopologyIdentifier vnfTopologyIdentifier = mock(VnfTopologyIdentifier.class);
        doReturn(vnfName).when(vnfTopologyIdentifier).getVnfName();
        doReturn(vnfType).when(vnfTopologyIdentifier).getVnfType();
        doReturn(vnfTopologyIdentifier).when(vnfTopologyInformation).getVnfTopologyIdentifier();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<PreloadVnfTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getVnfInstanceTopologyInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void preloadVnfInstanceTopologyOperationErrorOne() throws Exception {
        PreloadVnfInstanceTopologyOperationInput preloadVnfInstanceTopologyOperationInput =
            mock(PreloadVnfInstanceTopologyOperationInput.class);
        doReturn(null).when(preloadVnfInstanceTopologyOperationInput).getVnfInstanceTopologyInformation();
        Future<RpcResult<PreloadVnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfInstanceTopologyOperation(preloadVnfInstanceTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vnf-instance-name or vnf-model-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>preload_name = ""</code>
     * And
     * <code>preload_type = ""</code>
     *
     * @throws Exception
     */
    @Test public void preloadVnfInstanceTopologyOperationErrorTwo() throws Exception {
        // preload_type = ""
        vnfInstanceName = "vnfInstanceName";
        vnfModelId = "";
        PreloadVnfInstanceTopologyOperationInput preloadVnfInstanceTopologyOperationInput =
            mock(PreloadVnfInstanceTopologyOperationInput.class);
        VnfInstanceTopologyInformation vnfInstanceTopologyInformation = mock(VnfInstanceTopologyInformation.class);
        doReturn(vnfInstanceTopologyInformation).when(preloadVnfInstanceTopologyOperationInput)
            .getVnfInstanceTopologyInformation();
        VnfInstanceIdentifiers vnfInstanceIdentifiers = mock(VnfInstanceIdentifiers.class);
        doReturn(vnfInstanceName).when(vnfInstanceIdentifiers).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceIdentifiers).getVnfModelId();
        doReturn(vnfInstanceIdentifiers).when(vnfInstanceTopologyInformation).getVnfInstanceIdentifiers();

        Future<RpcResult<PreloadVnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfInstanceTopologyOperation(preloadVnfInstanceTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, invalid preload-type",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());

        //preload_name == ""
        vnfInstanceName = "";
        vnfModelId = "vnfModelId";
        doReturn(vnfInstanceName).when(vnfInstanceIdentifiers).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceIdentifiers).getVnfModelId();

        rpcResultFuture = vnfapiProvider.preloadVnfInstanceTopologyOperation(preloadVnfInstanceTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, invalid preload-name",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API", SVC_OPERATION, null, "sync") = false</code>
     *
     * @throws Exception
     */
    @Test public void preloadVnfInstanceTopologyOperationErrorThree() throws Exception {
        // preload_type = ""
        vnfInstanceName = "vnfInstanceName";
        vnfModelId = "vnfModelId";
        PreloadVnfInstanceTopologyOperationInput preloadVnfInstanceTopologyOperationInput =
            mock(PreloadVnfInstanceTopologyOperationInput.class);
        VnfInstanceTopologyInformation vnfInstanceTopologyInformation = mock(VnfInstanceTopologyInformation.class);
        doReturn(vnfInstanceTopologyInformation).when(preloadVnfInstanceTopologyOperationInput)
            .getVnfInstanceTopologyInformation();
        VnfInstanceIdentifiers vnfInstanceIdentifiers = mock(VnfInstanceIdentifiers.class);
        doReturn(vnfInstanceName).when(vnfInstanceIdentifiers).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceIdentifiers).getVnfModelId();
        doReturn(vnfInstanceIdentifiers).when(vnfInstanceTopologyInformation).getVnfInstanceIdentifiers();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(preloadVnfInstanceTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();

        Future<RpcResult<PreloadVnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfInstanceTopologyOperation(preloadVnfInstanceTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void preloadVnfInstanceTopologyOperationSuccess() throws Exception {
        // preload_type = ""
        vnfInstanceName = "vnfInstanceName";
        vnfModelId = "vnfModelId";
        PreloadVnfInstanceTopologyOperationInput preloadVnfInstanceTopologyOperationInput =
            mock(PreloadVnfInstanceTopologyOperationInput.class);
        VnfInstanceTopologyInformation vnfInstanceTopologyInformation = mock(VnfInstanceTopologyInformation.class);
        doReturn(vnfInstanceTopologyInformation).when(preloadVnfInstanceTopologyOperationInput)
            .getVnfInstanceTopologyInformation();
        VnfInstanceIdentifiers vnfInstanceIdentifiers = mock(VnfInstanceIdentifiers.class);
        doReturn(vnfInstanceName).when(vnfInstanceIdentifiers).getVnfInstanceName();
        doReturn(vnfModelId).when(vnfInstanceIdentifiers).getVnfModelId();
        doReturn(vnfInstanceIdentifiers).when(vnfInstanceTopologyInformation).getVnfInstanceIdentifiers();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(preloadVnfInstanceTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<PreloadVnfInstanceTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVnfInstanceTopologyOperation(preloadVnfInstanceTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getVfModuleTopologyInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void preloadVfModuleTopologyOperationErrorOne() throws Exception {
        PreloadVfModuleTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVfModuleTopologyOperationInput.class);
        doReturn(null).when(preloadVnfTopologyOperationInput).getVfModuleTopologyInformation();

        Future<RpcResult<PreloadVfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVfModuleTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, null or empty vf-module-name or vf-module-model-id",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>preload_name = ""</code>
     * And
     * <code>preload_type = ""</code>
     *
     * @throws Exception
     */
    @Test public void preloadVfModuleTopologyOperationErrorTwo() throws Exception {
        // preload_name = ""
        vfModuleName = "";
        vfModuleModelId = "vfModuleModelId";
        PreloadVfModuleTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVfModuleTopologyOperationInput.class);
        VfModuleTopologyInformation vfModuleTopologyInformation = mock(VfModuleTopologyInformation.class);
        doReturn(vfModuleTopologyInformation).when(preloadVnfTopologyOperationInput).getVfModuleTopologyInformation();
        VfModuleIdentifiers vfModuleIdentifiers = mock(VfModuleIdentifiers.class);
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();
        doReturn(vfModuleIdentifiers).when(vfModuleTopologyInformation).getVfModuleIdentifiers();

        Future<RpcResult<PreloadVfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVfModuleTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, invalid preload-name",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());

        // preload_type = ""
        vfModuleName = "vfModuleName";
        vfModuleModelId = "";
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();

        rpcResultFuture = vnfapiProvider.preloadVfModuleTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("invalid input, invalid preload-type",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API", SVC_OPERATION, null, "sync") = false</code>
     *
     * @throws Exception
     */
    @Test public void preloadVfModuleTopologyOperationErrorThree() throws Exception {
        // preload_name = ""
        vfModuleName = "vfModuleName";
        vfModuleModelId = "vfModuleModelId";
        PreloadVfModuleTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVfModuleTopologyOperationInput.class);
        VfModuleTopologyInformation vfModuleTopologyInformation = mock(VfModuleTopologyInformation.class);
        doReturn(vfModuleTopologyInformation).when(preloadVnfTopologyOperationInput).getVfModuleTopologyInformation();
        VfModuleIdentifiers vfModuleIdentifiers = mock(VfModuleIdentifiers.class);
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();
        doReturn(vfModuleIdentifiers).when(vfModuleTopologyInformation).getVfModuleIdentifiers();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(preloadVnfTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();

        Future<RpcResult<PreloadVfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVfModuleTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void preloadVfModuleTopologyOperationSuccess() throws Exception {
        // preload_name = ""
        vfModuleName = "vfModuleName";
        vfModuleModelId = "vfModuleModelId";
        PreloadVfModuleTopologyOperationInput preloadVnfTopologyOperationInput =
            mock(PreloadVfModuleTopologyOperationInput.class);
        VfModuleTopologyInformation vfModuleTopologyInformation = mock(VfModuleTopologyInformation.class);
        doReturn(vfModuleTopologyInformation).when(preloadVnfTopologyOperationInput).getVfModuleTopologyInformation();
        VfModuleIdentifiers vfModuleIdentifiers = mock(VfModuleIdentifiers.class);
        doReturn(vfModuleName).when(vfModuleIdentifiers).getVfModuleName();
        doReturn(vfModuleModelId).when(vfModuleIdentifiers).getVfModuleModelId();
        doReturn(vfModuleIdentifiers).when(vfModuleTopologyInformation).getVfModuleIdentifiers();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(preloadVnfTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<PreloadVfModuleTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadVfModuleTopologyOperation(preloadVnfTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>input.getNetworkTopologyInformation() == null</code>
     *
     * @throws Exception
     */
    @Test public void preloadNetworkTopologyOperationErrorOne() throws Exception {
        PreloadNetworkTopologyOperationInput PreloadNetworkTopologyOperationInput =
            mock(PreloadNetworkTopologyOperationInput.class);
        doReturn(null).when(PreloadNetworkTopologyOperationInput).getNetworkTopologyInformation();

        Future<RpcResult<PreloadNetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadNetworkTopologyOperation(PreloadNetworkTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("input, null or empty network-name or network-type",
            rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>preload_name == ""</code>
     * ANd
     * <code>preload_type == ""ss</code>
     *
     * @throws Exception
     */
    @Test public void preloadNetworkTopologyOperationErrorTwo() throws Exception {
        String networkName = "";
        String networkType = "NetworkType";
        PreloadNetworkTopologyOperationInput PreloadNetworkTopologyOperationInput =
            mock(PreloadNetworkTopologyOperationInput.class);
        NetworkTopologyInformation networkTopologyInformation = mock(NetworkTopologyInformation.class);
        doReturn(networkTopologyInformation).when(PreloadNetworkTopologyOperationInput).getNetworkTopologyInformation();
        NetworkTopologyIdentifier networkTopologyIdentifier = mock(NetworkTopologyIdentifier.class);
        doReturn(networkTopologyIdentifier).when(networkTopologyInformation).getNetworkTopologyIdentifier();
        doReturn(networkName).when(networkTopologyIdentifier).getNetworkName();
        doReturn(networkType).when(networkTopologyIdentifier).getNetworkType();

        Future<RpcResult<PreloadNetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadNetworkTopologyOperation(PreloadNetworkTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("input, invalid preload-name", rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());

        networkName = "NetworkName";
        networkType = "";
        doReturn(networkName).when(networkTopologyIdentifier).getNetworkName();
        doReturn(networkType).when(networkTopologyIdentifier).getNetworkType();

        rpcResultFuture = vnfapiProvider.preloadNetworkTopologyOperation(PreloadNetworkTopologyOperationInput);

        Assert.assertEquals("403", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("input, invalid preload-type", rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Error case:
     * <p>
     * <code>svcLogicClient.hasGraph("VNF-API",SVC_OPERATION,null,"sync" = false</code>
     *
     * @throws Exception
     */
    @Test public void preloadNetworkTopologyOperationErrorThree() throws Exception {
        String networkName = "NetworkName";
        String networkType = "NetworkType";
        PreloadNetworkTopologyOperationInput PreloadNetworkTopologyOperationInput =
            mock(PreloadNetworkTopologyOperationInput.class);
        NetworkTopologyInformation networkTopologyInformation = mock(NetworkTopologyInformation.class);
        doReturn(networkTopologyInformation).when(PreloadNetworkTopologyOperationInput).getNetworkTopologyInformation();
        NetworkTopologyIdentifier networkTopologyIdentifier = mock(NetworkTopologyIdentifier.class);
        doReturn(networkTopologyIdentifier).when(networkTopologyInformation).getNetworkTopologyIdentifier();
        doReturn(networkName).when(networkTopologyIdentifier).getNetworkName();
        doReturn(networkType).when(networkTopologyIdentifier).getNetworkType();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(PreloadNetworkTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();

        Future<RpcResult<PreloadNetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadNetworkTopologyOperation(PreloadNetworkTopologyOperationInput);

        Assert.assertEquals("503", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertTrue(
            rpcResultFuture.get().getResult().getResponseMessage().contains("No service logic active for VNF-API"));
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void preloadNetworkTopologyOperationErrorFour() throws Exception {
        String networkName = "NetworkName";
        String networkType = "NetworkType";
        PreloadNetworkTopologyOperationInput PreloadNetworkTopologyOperationInput =
            mock(PreloadNetworkTopologyOperationInput.class);
        NetworkTopologyInformation networkTopologyInformation = mock(NetworkTopologyInformation.class);
        doReturn(networkTopologyInformation).when(PreloadNetworkTopologyOperationInput).getNetworkTopologyInformation();
        NetworkTopologyIdentifier networkTopologyIdentifier = mock(NetworkTopologyIdentifier.class);
        doReturn(networkTopologyIdentifier).when(networkTopologyInformation).getNetworkTopologyIdentifier();
        doReturn(networkName).when(networkTopologyIdentifier).getNetworkName();
        doReturn(networkType).when(networkTopologyIdentifier).getNetworkType();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(PreloadNetworkTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());
        doReturn(null).when(dataBroker).newWriteOnlyTransaction();

        Future<RpcResult<PreloadNetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadNetworkTopologyOperation(PreloadNetworkTopologyOperationInput);

        Assert.assertEquals("500", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals("java.lang.NullPointerException", rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    /**
     * Test Success case
     *
     * @throws Exception
     */
    @Test public void preloadNetworkTopologyOperationSuccess() throws Exception {
        String networkName = "NetworkName";
        String networkType = "NetworkType";
        PreloadNetworkTopologyOperationInput PreloadNetworkTopologyOperationInput =
            mock(PreloadNetworkTopologyOperationInput.class);
        NetworkTopologyInformation networkTopologyInformation = mock(NetworkTopologyInformation.class);
        doReturn(networkTopologyInformation).when(PreloadNetworkTopologyOperationInput).getNetworkTopologyInformation();
        NetworkTopologyIdentifier networkTopologyIdentifier = mock(NetworkTopologyIdentifier.class);
        doReturn(networkTopologyIdentifier).when(networkTopologyInformation).getNetworkTopologyIdentifier();
        doReturn(networkName).when(networkTopologyIdentifier).getNetworkName();
        doReturn(networkType).when(networkTopologyIdentifier).getNetworkType();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(PreloadNetworkTopologyOperationInput).getSdncRequestHeader();
        doReturn(svcRequestId).when(sdncRequestHeader).getSvcRequestId();
        doReturn(true).when(vnfsdnSvcLogicServiceClient).hasGraph(any(), any(), any(), any());

        Future<RpcResult<PreloadNetworkTopologyOperationOutput>> rpcResultFuture =
            vnfapiProvider.preloadNetworkTopologyOperation(PreloadNetworkTopologyOperationInput);

        Assert.assertEquals("200", rpcResultFuture.get().getResult().getResponseCode());
        Assert.assertEquals(null, rpcResultFuture.get().getResult().getResponseMessage());
        Assert.assertEquals("Y", rpcResultFuture.get().getResult().getAckFinalIndicator());
    }

    @Test public void getVfModuleServiceData() throws Exception {
        // Three parameters: siid, vfModuleServiceDataBuilder, LogicalDatastoreType
        String ssid = "ssid";
        VfModuleServiceDataBuilder vfModuleServiceDataBuilder = spy(new VfModuleServiceDataBuilder());
        Optional<VfModuleList> optional = mock(Optional.class);
        doReturn(optional).when(checkedFuture).get();
        doReturn(true).when(optional).isPresent();
        VfModuleList vfModuleList = mock(VfModuleList.class);
        doReturn(vfModuleList).when(optional).get();
        VfModuleServiceData vfModuleServiceData = mock(VfModuleServiceData.class);
        doReturn(vfModuleServiceData).when(vfModuleList).getVfModuleServiceData();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(vfModuleServiceData).getSdncRequestHeader();

        Whitebox.invokeMethod(vnfapiProvider, "getVfModuleServiceData", ssid, vfModuleServiceDataBuilder,
            LogicalDatastoreType.CONFIGURATION);

        Assert.assertEquals(sdncRequestHeader, vfModuleServiceDataBuilder.getSdncRequestHeader());

        //With Two parameters: siid, vfModuleServiceDataBuilder
        Whitebox.invokeMethod(vnfapiProvider, "getVfModuleServiceData", ssid, vfModuleServiceDataBuilder);

        Assert.assertEquals(sdncRequestHeader, vfModuleServiceDataBuilder.getSdncRequestHeader());
    }

    @Test public void getPreloadData() throws Exception {
        // Four parameters:
        // String preload_name, String preload_type, PreloadDataBuilder preloadDataBuilder, LogicalDatastoreType type
        PreloadDataBuilder preloadDataBuilder = spy(new PreloadDataBuilder());
        // mock optional
        Optional<VnfPreloadList> optional = mock(Optional.class);
        doReturn(optional).when(checkedFuture).get();
        doReturn(true).when(optional).isPresent();

        VnfPreloadList vnfPreloadList = mock(VnfPreloadList.class);
        doReturn(vnfPreloadList).when(optional).get();
        PreloadData preloadData = mock(PreloadData.class);
        doReturn(preloadData).when(vnfPreloadList).getPreloadData();

        VnfTopologyInformation vnfTopologyInformation = mock(VnfTopologyInformation.class);
        doReturn(vnfTopologyInformation).when(preloadData).getVnfTopologyInformation();

        Whitebox.invokeMethod(vnfapiProvider, "getPreloadData", "preloadName", "preloadType", preloadDataBuilder,
            LogicalDatastoreType.CONFIGURATION);

        Assert.assertEquals(vnfTopologyInformation, preloadDataBuilder.getVnfTopologyInformation());

        // Three parameters:
        // String preload_name, String preload_type, PreloadDataBuilder preloadDataBuilder, LogicalDatastoreType type
        Whitebox.invokeMethod(vnfapiProvider, "getPreloadData", "preloadName", "preloadType", preloadDataBuilder);

        Assert.assertEquals(vnfTopologyInformation, preloadDataBuilder.getVnfTopologyInformation());
    }

    @Test public void getVnfInstancePreloadData() throws Exception {
        // Four parameters:
        // String preload_name, String preload_type, VnfInstancePreloadDataBuilder preloadDataBuilder,
        // LogicalDatastoreType type
        VnfInstancePreloadDataBuilder vnfInstancePreloadDataBuilder = spy(new VnfInstancePreloadDataBuilder());
        // mock optional
        Optional<VnfPreloadList> optional = mock(Optional.class);
        doReturn(optional).when(checkedFuture).get();
        doReturn(true).when(optional).isPresent();
        VnfInstancePreloadList vnfInstancePreloadList = mock(VnfInstancePreloadList.class);
        doReturn(vnfInstancePreloadList).when(optional).get();
        VnfInstancePreloadData vnfInstancePreloadData = mock(VnfInstancePreloadData.class);
        doReturn(vnfInstancePreloadData).when(vnfInstancePreloadList).getVnfInstancePreloadData();
        VnfInstanceTopologyInformation vnfInstanceTopologyInformation = mock(VnfInstanceTopologyInformation.class);
        doReturn(vnfInstanceTopologyInformation).when(vnfInstancePreloadData).getVnfInstanceTopologyInformation();

        Whitebox.invokeMethod(vnfapiProvider, "getVnfInstancePreloadData", "preloadName", "preloadType",
            vnfInstancePreloadDataBuilder, LogicalDatastoreType.CONFIGURATION);

        Assert.assertEquals(vnfInstanceTopologyInformation,
            vnfInstancePreloadDataBuilder.getVnfInstanceTopologyInformation());

        // Three Parameters:
        // String preload_name, String preload_type, VnfInstancePreloadDataBuilder preloadDataBuilder
        Whitebox.invokeMethod(vnfapiProvider, "getVnfInstancePreloadData", "preloadName", "preloadType",
            vnfInstancePreloadDataBuilder);

        Assert.assertEquals(vnfInstanceTopologyInformation,
            vnfInstancePreloadDataBuilder.getVnfInstanceTopologyInformation());
    }

    @Test public void getVfModulePreloadData() throws Exception {
        // Four Parameters
        // String preload_name, String preload_type, VfModulePreloadDataBuilder preloadDataBuilder,
        // LogicalDatastoreType type
        VfModulePreloadDataBuilder vfModulePreloadDataBuilder = spy(new VfModulePreloadDataBuilder());
        // mock optional
        Optional<VfModulePreloadList> optional = mock(Optional.class);
        doReturn(optional).when(checkedFuture).get();
        doReturn(true).when(optional).isPresent();
        VfModulePreloadList vfModulePreloadList = mock(VfModulePreloadList.class);
        doReturn(vfModulePreloadList).when(optional).get();
        VfModulePreloadData vfModulePreloadData = mock(VfModulePreloadData.class);
        doReturn(vfModulePreloadData).when(vfModulePreloadList).getVfModulePreloadData();
        VfModuleTopologyInformation vfModuleTopologyInformation = mock(VfModuleTopologyInformation.class);
        doReturn(vfModuleTopologyInformation).when(vfModulePreloadData).getVfModuleTopologyInformation();

        Whitebox.invokeMethod(vnfapiProvider, "getVfModulePreloadData", "preloadName", "preloadType",
            vfModulePreloadDataBuilder, LogicalDatastoreType.CONFIGURATION);

        Assert.assertEquals(vfModuleTopologyInformation, vfModulePreloadDataBuilder.getVfModuleTopologyInformation());

        // Three Parameters:
        // String vnf_name, String vnf_type, VfModulePreloadDataBuilder preloadDataBuilder
        Whitebox.invokeMethod(vnfapiProvider, "getVfModulePreloadData", "preloadName", "preloadType",
            vfModulePreloadDataBuilder);

        Assert.assertEquals(vfModuleTopologyInformation, vfModulePreloadDataBuilder.getVfModuleTopologyInformation());
    }

    /**
     * With ServiceStatusBuilder, RequestInformation
     */
    @Test public void setServiceStatusOne() throws Exception {
        // VNFActivateRequest
        RequestInformation requestInformation = mock(RequestInformation.class);
        ServiceStatusBuilder serviceStatusBuilder = spy(new ServiceStatusBuilder());
        doReturn(RequestInformation.RequestAction.VNFActivateRequest).when(requestInformation).getRequestAction();
        doReturn(RequestInformation.RequestSubAction.SUPP).when(requestInformation).getRequestSubAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.VNFActivateRequest, serviceStatusBuilder.getVnfsdnAction());

        //ChangeVNFActivateRequest
        doReturn(RequestInformation.RequestAction.ChangeVNFActivateRequest).when(requestInformation).getRequestAction();
        doReturn(RequestInformation.RequestSubAction.CANCEL).when(requestInformation).getRequestSubAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert
            .assertEquals(ServiceStatus.VnfsdnAction.ChangeVNFActivateRequest, serviceStatusBuilder.getVnfsdnAction());

        //DisconnectVNFRequest
        doReturn(RequestInformation.RequestAction.DisconnectVNFRequest).when(requestInformation).getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.DisconnectVNFRequest, serviceStatusBuilder.getVnfsdnAction());

        //PreloadVNFRequest
        doReturn(RequestInformation.RequestAction.PreloadVNFRequest).when(requestInformation).getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.PreloadVNFRequest, serviceStatusBuilder.getVnfsdnAction());

        //DeletePreloadVNFRequest
        doReturn(RequestInformation.RequestAction.DeletePreloadVNFRequest).when(requestInformation).getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.DeletePreloadVNFRequest, serviceStatusBuilder.getVnfsdnAction());

        //VnfInstanceActivateRequest
        doReturn(RequestInformation.RequestAction.VnfInstanceActivateRequest).when(requestInformation)
            .getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.VnfInstanceActivateRequest,
            serviceStatusBuilder.getVnfsdnAction());

        //ChangeVnfInstanceActivateRequest
        doReturn(RequestInformation.RequestAction.ChangeVnfInstanceActivateRequest).when(requestInformation)
            .getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.ChangeVnfInstanceActivateRequest,
            serviceStatusBuilder.getVnfsdnAction());

        //DisconnectVnfInstanceRequest
        doReturn(RequestInformation.RequestAction.DisconnectVnfInstanceRequest).when(requestInformation)
            .getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.DisconnectVnfInstanceRequest,
            serviceStatusBuilder.getVnfsdnAction());

        //PreloadVnfInstanceRequest
        doReturn(RequestInformation.RequestAction.PreloadVnfInstanceRequest).when(requestInformation)
            .getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert
            .assertEquals(ServiceStatus.VnfsdnAction.PreloadVnfInstanceRequest, serviceStatusBuilder.getVnfsdnAction());

        //VfModuleActivateRequest
        doReturn(RequestInformation.RequestAction.VfModuleActivateRequest).when(requestInformation).getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.VfModuleActivateRequest, serviceStatusBuilder.getVnfsdnAction());

        //ChangeVfModuleActivateRequest
        doReturn(RequestInformation.RequestAction.ChangeVfModuleActivateRequest).when(requestInformation)
            .getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.ChangeVfModuleActivateRequest,
            serviceStatusBuilder.getVnfsdnAction());

        //DisconnectVfModuleRequest
        doReturn(RequestInformation.RequestAction.DisconnectVfModuleRequest).when(requestInformation)
            .getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert
            .assertEquals(ServiceStatus.VnfsdnAction.DisconnectVfModuleRequest, serviceStatusBuilder.getVnfsdnAction());

        //PreloadVfModuleRequest
        doReturn(RequestInformation.RequestAction.PreloadVfModuleRequest).when(requestInformation).getRequestAction();
        Whitebox.invokeMethod(vnfapiProvider, "setServiceStatus", serviceStatusBuilder, requestInformation);
        Assert.assertEquals(ServiceStatus.VnfsdnAction.PreloadVfModuleRequest, serviceStatusBuilder.getVnfsdnAction());
    }

    @Test public void getServiceData() throws Exception {
        ServiceDataBuilder serviceDataBuilder = spy(new ServiceDataBuilder());
        // mock optional
        Optional<VnfList> optional = mock(Optional.class);
        doReturn(optional).when(checkedFuture).get();
        doReturn(true).when(optional).isPresent();
        VnfList vnfList = mock(VnfList.class);
        doReturn(vnfList).when(optional).get();
        ServiceData serviceData = mock(ServiceData.class);
        doReturn(serviceData).when(vnfList).getServiceData();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(serviceData).getSdncRequestHeader();

        Whitebox.invokeMethod(vnfapiProvider, "getServiceData", "siid", serviceDataBuilder,
            LogicalDatastoreType.CONFIGURATION);

        Assert.assertEquals(sdncRequestHeader, serviceDataBuilder.getSdncRequestHeader());
    }

    @Test public void getVnfInstanceServiceData() throws Exception {
        VnfInstanceServiceDataBuilder vnfInstanceServiceDataBuilder = spy(new VnfInstanceServiceDataBuilder());
        // mock optional
        Optional<VnfList> optional = mock(Optional.class);
        doReturn(optional).when(checkedFuture).get();
        doReturn(true).when(optional).isPresent();
        VnfInstanceList vnfInstanceList = mock(VnfInstanceList.class);
        doReturn(vnfInstanceList).when(optional).get();
        VnfInstanceServiceData vnfInstanceServiceData = mock(VnfInstanceServiceData.class);
        doReturn(vnfInstanceServiceData).when(vnfInstanceList).getVnfInstanceServiceData();
        SdncRequestHeader sdncRequestHeader = mock(SdncRequestHeader.class);
        doReturn(sdncRequestHeader).when(vnfInstanceServiceData).getSdncRequestHeader();

        Whitebox.invokeMethod(vnfapiProvider, "getVnfInstanceServiceData", "siid", vnfInstanceServiceDataBuilder,
            LogicalDatastoreType.CONFIGURATION);

        Assert.assertEquals(sdncRequestHeader, vnfInstanceServiceDataBuilder.getSdncRequestHeader());
    }

    @Test public void DeleteVnfList() throws Exception {
        LogicalDatastoreType configuration = LogicalDatastoreType.CONFIGURATION;
        VnfList vnfList = mock(VnfList.class);
        VnfListKey vnfListKey = mock(VnfListKey.class);
        doReturn(vnfListKey).when(vnfList).key();
        InstanceIdentifier<VnfList> vnfListInstanceIdentifier = mock(InstanceIdentifier.class);
        dataBroker.newWriteOnlyTransaction().put(configuration, vnfListInstanceIdentifier, vnfList);

        Whitebox.invokeMethod(vnfapiProvider, "DeleteVnfList", vnfList, configuration);

        verify(dataBroker, times(2)).newWriteOnlyTransaction();
    }
}
