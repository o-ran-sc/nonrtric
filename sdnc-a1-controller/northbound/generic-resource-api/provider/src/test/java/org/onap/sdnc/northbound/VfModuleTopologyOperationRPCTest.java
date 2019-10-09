package org.onap.sdnc.northbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.APP_NAME;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NO_SERVICE_LOGIC_ACTIVE;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NULL_OR_EMPTY_ERROR_PARAM;
import static org.onap.sdnc.northbound.util.MDSALUtil.build;
import static org.onap.sdnc.northbound.util.MDSALUtil.exec;
import static org.onap.sdnc.northbound.util.MDSALUtil.requestInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.sdncRequestHeader;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceInformationBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.vnfResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.vfModuleResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.vfModuleInformationBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.vfModuleTopologyOperationInput;
import static org.onap.sdnc.northbound.util.MDSALUtil.vfModuleTopologyOperationOutput;
import static org.onap.sdnc.northbound.util.MDSALUtil.vnfInformationBuilder;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.northbound.util.PropBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.VfModuleTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeader.SvcAction;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class VfModuleTopologyOperationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "vf-module-topology-operation";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_service_info_not_present() throws Exception {

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput());

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("403", output.getResponseCode());
        assertEquals(NULL_OR_EMPTY_ERROR_PARAM, output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_fail_when_invalid_vnf_id() throws Exception {

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput()
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("403", output.getResponseCode());
        assertEquals("invalid input, null or empty vnf-id", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    /*@Test
    public void should_fail_when_invalid_vf_module_id() throws Exception {

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput()
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
            .setVnfInformation(build(vnfInformationBuilder()
                .setVnfId("test-vnf-id")))
        );

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("403", output.getResponseCode());
        assertEquals("invalid input, vf-module-id is null or empty", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }*/


    @Test
    public void should_fail_when_client_execution_failed() throws Exception {

        svcClient.mockHasGraph(true);
        svcClient.mockExecute(new RuntimeException("test exception"));

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput()
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
            .setVnfInformation(build(vnfInformationBuilder()
                .setVnfId("test-vnf-id")
            ))
            .setVfModuleInformation(build(vfModuleInformationBuilder()
                .setVfModuleId("test-vf-module-id")
            ))
        );

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_client_has_no_graph() throws Exception {

        svcClient.mockHasGraph(false);

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput()
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
            .setVnfInformation(build(vnfInformationBuilder()
                .setVnfId("test-vnf-id")
            ))
            .setVfModuleInformation(build(vfModuleInformationBuilder()
                .setVfModuleId("test-vf-module-id")
            ))
        );

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("503", output.getResponseCode());
        assertEquals(NO_SERVICE_LOGIC_ACTIVE + APP_NAME + ": '" + SVC_OPERATION + "'", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_failed_to_update_mdsal() throws Exception {

        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecute(svcResultProp);
        svcClient.mockHasGraph(true);
        WriteTransaction mockWriteTransaction = mock(WriteTransaction.class);
        when(mockWriteTransaction.submit()).thenThrow(new TransactionChainClosedException("test exception"));

        DataBroker spyDataBroker = Mockito.spy(dataBroker);
        when(spyDataBroker.newWriteOnlyTransaction()).thenReturn(mockWriteTransaction);
        genericResourceApiProvider.setDataBroker(spyDataBroker);

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput()
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
            .setVnfInformation(build(vnfInformationBuilder()
                .setVnfId("test-vnf-id")
            ))
            .setVfModuleInformation(build(vfModuleInformationBuilder()
                .setVfModuleId("test-vf-module-id")
            ))
        );

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_complete_with_success_when_no_errors() throws Exception {

        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcClient.mockExecute(svcResultProp);

        VfModuleTopologyOperationInput input = build(vfModuleTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setRequestInformation(build(requestInformation()
                .setRequestId("test-request-id")
                .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
            .setVnfInformation(build(vnfInformationBuilder()
                .setVnfId("test-vnf-id")
            ))
            .setVfModuleInformation(build(vfModuleInformationBuilder()
                .setVfModuleId("test-vf-module-id")
            ))
        );

        VfModuleTopologyOperationOutput output =
            exec(genericResourceApiProvider::vfModuleTopologyOperation, input, RpcResult::getResult);

        assertEquals("200", output.getResponseCode());
        assertEquals("OK", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());

        VfModuleTopologyOperationOutput expectedOutput = createExpectedVMTOO(svcResultProp, input);
        assertEquals(expectedOutput, output);
    }

    private VfModuleTopologyOperationOutput createExpectedVMTOO(PropBuilder propBuilder,
        VfModuleTopologyOperationInput input) {
        return build(vfModuleTopologyOperationOutput()
                .setSvcRequestId(input.getSdncRequestHeader().getSvcRequestId())
                .setResponseCode(propBuilder.get(svcClient.errorCode))
                .setAckFinalIndicator(propBuilder.get(svcClient.ackFinal))
                .setResponseMessage(propBuilder.get(svcClient.errorMessage))
                .setServiceResponseInformation(build(serviceResponseInformation()
                    .setInstanceId(input.getServiceInformation().getServiceInstanceId())
                    .setObjectPath(propBuilder.get(svcClient.serviceObjectPath)))
                )
                .setVnfResponseInformation(build(vnfResponseInformation()
                        .setInstanceId(input.getVnfInformation().getVnfId())
                        .setObjectPath(propBuilder.get(svcClient.vnfObjectPath)))
                    )
                .setVfModuleResponseInformation(build(vfModuleResponseInformation()
                        .setInstanceId(input.getVfModuleInformation().getVfModuleId())
                        .setObjectPath(propBuilder.get(svcClient.vfModuleObjectPath)))
                    )
        );
    }
}
