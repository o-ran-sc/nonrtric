package org.onap.sdnc.northbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.APP_NAME;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NO_SERVICE_LOGIC_ACTIVE;
import static org.onap.sdnc.northbound.util.MDSALUtil.build;
import static org.onap.sdnc.northbound.util.MDSALUtil.exec;
import static org.onap.sdnc.northbound.util.MDSALUtil.networkTopologyIdentifierStructureBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.preloadNetworkTopologyInformationBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.preloadNetworkTopologyOperationInput;
import static org.onap.sdnc.northbound.util.MDSALUtil.preloadNetworkTopologyOperationOutput;
import static org.onap.sdnc.northbound.util.MDSALUtil.requestInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.sdncRequestHeader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.northbound.util.PropBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadNetworkTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PreloadNetworkTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeader.SvcAction;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class PreloadNetworkTopologyRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "preload-network-topology-operation";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_invalid_network_topology() throws Exception {

        PreloadNetworkTopologyOperationInput input = build(preloadNetworkTopologyOperationInput());

        PreloadNetworkTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadNetworkTopologyOperation, input, RpcResult::getResult);

        assertEquals("403", output.getResponseCode());
        assertEquals("invalid input, null or empty preload-network-topology-information", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_fail_when_client_execution_failed() throws Exception {

        svcClient.mockHasGraph(true);
        svcClient.mockExecuteWoServiceDataPreload(new RuntimeException("test exception"));

        PreloadNetworkTopologyOperationInput input = build(preloadNetworkTopologyOperationInput()
            .setPreloadNetworkTopologyInformation(build(preloadNetworkTopologyInformationBuilder()
                .setNetworkTopologyIdentifierStructure(build(networkTopologyIdentifierStructureBuilder()
                    .setNetworkName("test-network-name")
                    .setNetworkType("test-network-type")))))
        );

        PreloadNetworkTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadNetworkTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_client_has_no_graph() throws Exception {

        svcClient.mockHasGraph(false);

        PreloadNetworkTopologyOperationInput input = build(preloadNetworkTopologyOperationInput()
            .setPreloadNetworkTopologyInformation(build(preloadNetworkTopologyInformationBuilder()
                .setNetworkTopologyIdentifierStructure(build(networkTopologyIdentifierStructureBuilder()
                    .setNetworkName("test-network-name")
                    .setNetworkType("test-network-type")))))
        );

        PreloadNetworkTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadNetworkTopologyOperation, input, RpcResult::getResult);

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

        PreloadNetworkTopologyOperationInput input = build(preloadNetworkTopologyOperationInput()
            .setPreloadNetworkTopologyInformation(build(preloadNetworkTopologyInformationBuilder()
                .setNetworkTopologyIdentifierStructure(build(networkTopologyIdentifierStructureBuilder()
                    .setNetworkName("test-network-name")
                    .setNetworkType("test-network-type")))))
        );

        PreloadNetworkTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadNetworkTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_complete_with_success_when_no_errors() throws Exception {

        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecute(svcResultProp);

        PreloadNetworkTopologyOperationInput input = build(preloadNetworkTopologyOperationInput()
            .setPreloadNetworkTopologyInformation(build(preloadNetworkTopologyInformationBuilder()
                .setNetworkTopologyIdentifierStructure(build(networkTopologyIdentifierStructureBuilder()
                    .setNetworkName("test-network-name")
                    .setNetworkType("test-network-type")))))
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setRequestInformation(build(requestInformation()
                .setRequestId("test-request-id")
                .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
            ))
        );

        PreloadNetworkTopologyOperationOutput output =
            exec(genericResourceApiProvider::preloadNetworkTopologyOperation, input, RpcResult::getResult);

        assertEquals("200", output.getResponseCode());
        assertEquals("Y", output.getAckFinalIndicator());

        PreloadNetworkTopologyOperationOutput expectedOutput = createExpectedOutput(svcResultProp, input);
        assertEquals(expectedOutput, output);
    }

    private PreloadNetworkTopologyOperationOutput createExpectedOutput(PropBuilder svcResultProp,
        PreloadNetworkTopologyOperationInput input) {
        return build(preloadNetworkTopologyOperationOutput()
            .setSvcRequestId(input.getSdncRequestHeader().getSvcRequestId())
            .setResponseCode(svcResultProp.get(svcClient.errorCode))
            .setAckFinalIndicator(svcResultProp.get(svcClient.ackFinal))
        );
    }

}
