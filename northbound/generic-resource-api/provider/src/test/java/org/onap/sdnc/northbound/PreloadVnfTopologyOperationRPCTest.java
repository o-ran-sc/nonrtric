package org.onap.sdnc.northbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.APP_NAME;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NO_SERVICE_LOGIC_ACTIVE;
import static org.onap.sdnc.northbound.util.MDSALUtil.build;
import static org.onap.sdnc.northbound.util.MDSALUtil.exec;
import static org.onap.sdnc.northbound.util.MDSALUtil.preloadVfModuleTopologyOperationInput;
import static org.onap.sdnc.northbound.util.MDSALUtil.preloadVfModuleTopologyOperationOutput;
import static org.onap.sdnc.northbound.util.MDSALUtil.requestInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.sdncRequestHeader;
import static org.onap.sdnc.northbound.util.MDSALUtil.vnfTopologyIdentifierStructureBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.preloadVfModuleTopologyInformationBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.vfModuleTopologyBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.vfModuleTopologyIdentifierBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.northbound.util.PropBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadVfModuleTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeader.SvcAction;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class PreloadVnfTopologyOperationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "preload-vf-module-topology-operation";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_invalid_vnf_topology() throws Exception {

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput());

        PreloadVfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("403", output.getResponseCode());
        assertEquals("invalid input, null or empty preload-vf-module-topology-information.vf-module-topology.vf-module-topology-identifier.vf-module-name", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_fail_when_invalid_preload_data() throws Exception {

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput()
            .setPreloadVfModuleTopologyInformation(build(preloadVfModuleTopologyInformationBuilder()
                .setVnfTopologyIdentifierStructure(build(vnfTopologyIdentifierStructureBuilder()
                    .setVnfName("test-vnf-name")))))
        );

        PreloadVfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("403", output.getResponseCode());
        assertEquals("invalid input, null or empty preload-vf-module-topology-information.vf-module-topology.vf-module-topology-identifier.vf-module-name", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_fail_when_client_execution_failed() throws Exception {

        svcClient.mockHasGraph(true);
        svcClient.mockExecuteWoServiceDataPreload(new RuntimeException("test exception"));

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput()
                .setPreloadVfModuleTopologyInformation(build(preloadVfModuleTopologyInformationBuilder()
                    .setVnfTopologyIdentifierStructure(build(vnfTopologyIdentifierStructureBuilder()
                        .setVnfName("test-vnf-name")
                        .setVnfType("test-vnf-type")))
                    .setVfModuleTopology(build(vfModuleTopologyBuilder()
                        .setVfModuleTopologyIdentifier(build(vfModuleTopologyIdentifierBuilder()
                            .setVfModuleName("vf-module-name"))
                )))))
                .setSdncRequestHeader(build(sdncRequestHeader()
                    .setSvcRequestId("test-svc-request-id")
                    .setSvcAction(SvcAction.Assign)
                ))
                .setRequestInformation(build(requestInformation()
                    .setRequestId("test-request-id")
                    .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
                ))
            );

        PreloadVfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_client_has_no_graph() throws Exception {

        svcClient.mockHasGraph(false);

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput()
                .setPreloadVfModuleTopologyInformation(build(preloadVfModuleTopologyInformationBuilder()
                    .setVnfTopologyIdentifierStructure(build(vnfTopologyIdentifierStructureBuilder()
                        .setVnfName("test-vnf-name")
                        .setVnfType("test-vnf-type")))
                    .setVfModuleTopology(build(vfModuleTopologyBuilder()
                        .setVfModuleTopologyIdentifier(build(vfModuleTopologyIdentifierBuilder()
                            .setVfModuleName("vf-module-name"))
                )))))
                .setSdncRequestHeader(build(sdncRequestHeader()
                    .setSvcRequestId("test-svc-request-id")
                    .setSvcAction(SvcAction.Assign)
                ))
                .setRequestInformation(build(requestInformation()
                    .setRequestId("test-request-id")
                    .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
                ))
            );

        PreloadVfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("503", output.getResponseCode());
        assertEquals(NO_SERVICE_LOGIC_ACTIVE + APP_NAME + ": '" + SVC_OPERATION + "'", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_failed_to_update_mdsal() throws Exception {

        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecuteWoServiceData(svcResultProp);
        svcClient.mockHasGraph(true);
        WriteTransaction mockWriteTransaction = mock(WriteTransaction.class);
        when(mockWriteTransaction.submit()).thenThrow(new TransactionChainClosedException("test exception"));

        DataBroker spyDataBroker = Mockito.spy(dataBroker);
        when(spyDataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTransaction);
        genericResourceApiProvider.setDataBroker(spyDataBroker);

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput()
                .setPreloadVfModuleTopologyInformation(build(preloadVfModuleTopologyInformationBuilder()
                    .setVnfTopologyIdentifierStructure(build(vnfTopologyIdentifierStructureBuilder()
                        .setVnfName("test-vnf-name")
                        .setVnfType("test-vnf-type")))
                    .setVfModuleTopology(build(vfModuleTopologyBuilder()
                        .setVfModuleTopologyIdentifier(build(vfModuleTopologyIdentifierBuilder()
                            .setVfModuleName("vf-module-name"))
                )))))
                .setSdncRequestHeader(build(sdncRequestHeader()
                    .setSvcRequestId("test-svc-request-id")
                    .setSvcAction(SvcAction.Assign)
                ))
                .setRequestInformation(build(requestInformation()
                    .setRequestId("test-request-id")
                    .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
                ))
            );

        PreloadVfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_complete_with_success_when_no_errors() throws Exception {

        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecute(svcResultProp);

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput()
            .setPreloadVfModuleTopologyInformation(build(preloadVfModuleTopologyInformationBuilder()
                .setVnfTopologyIdentifierStructure(build(vnfTopologyIdentifierStructureBuilder()
                    .setVnfName("test-vnf-name")
                    .setVnfType("test-vnf-type")))
                .setVfModuleTopology(build(vfModuleTopologyBuilder()
                    .setVfModuleTopologyIdentifier(build(vfModuleTopologyIdentifierBuilder()
                        .setVfModuleName("vf-module-name"))
            )))))
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setRequestInformation(build(requestInformation()
                .setRequestId("test-request-id")
                .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
            ))
        );

        PreloadVfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("200", output.getResponseCode());
        assertEquals("Y", output.getAckFinalIndicator());

        PreloadVfModuleTopologyOperationOutput expectedOutput = createExpectedOutput(svcResultProp, input);
        assertEquals(expectedOutput, output);
    }


    @Test
    public void delete_complete_with_success_when_no_errors() throws Exception {

        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecute(svcResultProp);

        PreloadVfModuleTopologyOperationInput input = build(preloadVfModuleTopologyOperationInput()
                .setPreloadVfModuleTopologyInformation(build(preloadVfModuleTopologyInformationBuilder()
                        .setVnfTopologyIdentifierStructure(build(vnfTopologyIdentifierStructureBuilder()
                                .setVnfName("test-vnf-name")
                                .setVnfType("test-vnf-type")))
                        .setVfModuleTopology(build(vfModuleTopologyBuilder()
                                .setVfModuleTopologyIdentifier(build(vfModuleTopologyIdentifierBuilder()
                                        .setVfModuleName("vf-module-name"))
                                )))))
                .setSdncRequestHeader(build(sdncRequestHeader()
                        .setSvcRequestId("test-svc-request-id")
                        .setSvcAction(SvcAction.Delete)
                ))
                .setRequestInformation(build(requestInformation()
                        .setRequestId("test-request-id")
                        .setRequestAction(RequestInformation.RequestAction.DeleteServiceInstance)
                ))
        );

        PreloadVfModuleTopologyOperationOutput output =
                exec(genericResourceApiProvider::preloadVfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("200", output.getResponseCode());
        assertEquals("Y", output.getAckFinalIndicator());

        PreloadVfModuleTopologyOperationOutput expectedOutput = createExpectedOutput(svcResultProp, input);
        assertEquals(expectedOutput, output);
    }



    private PreloadVfModuleTopologyOperationOutput createExpectedOutput(PropBuilder svcResultProp,
        PreloadVfModuleTopologyOperationInput input) {
        return build(preloadVfModuleTopologyOperationOutput()
            .setSvcRequestId(input.getSdncRequestHeader().getSvcRequestId())
            .setResponseCode(svcResultProp.get(svcClient.errorCode))
            .setAckFinalIndicator(svcResultProp.get(svcClient.ackFinal))
        );
    }

}
