package org.onap.sdnc.northbound;

import static org.junit.Assert.assertEquals;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.APP_NAME;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NO_SERVICE_LOGIC_ACTIVE;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NULL_OR_EMPTY_ERROR_PARAM;
import static org.onap.sdnc.northbound.util.MDSALUtil.build;
import static org.onap.sdnc.northbound.util.MDSALUtil.exec;
import static org.onap.sdnc.northbound.util.MDSALUtil.requestInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.sdncRequestHeader;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceInformationBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.tunnelxconnResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.tunnelxconnTopologyOperationInput;
import static org.onap.sdnc.northbound.util.MDSALUtil.tunnelxconnTopologyOperationOutput;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.northbound.util.PropBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.TunnelxconnTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.TunnelxconnTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeader.SvcAction;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class TunnelxconnTopologyOperationRPCTest extends GenericResourceApiProviderTest {


    private static final String SVC_OPERATION = "tunnelxconn-topology-operation";


    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_service_instance_id_not_present() throws Exception {

        TunnelxconnTopologyOperationInput input = build(tunnelxconnTopologyOperationInput());

        TunnelxconnTopologyOperationOutput output =
            exec(genericResourceApiProvider::tunnelxconnTopologyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getResponseCode());
        assertEquals(NULL_OR_EMPTY_ERROR_PARAM, output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_client_execution_failed() throws Exception {

        svcClient.mockHasGraph(true);
        svcClient.mockExecuteWoServiceData(new RuntimeException("test exception"));

        TunnelxconnTopologyOperationInput input = build(tunnelxconnTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        TunnelxconnTopologyOperationOutput output =
            exec(genericResourceApiProvider::tunnelxconnTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_client_has_no_graph() throws Exception {

        svcClient.mockHasGraph(false);

        TunnelxconnTopologyOperationInput input = build(tunnelxconnTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        TunnelxconnTopologyOperationOutput output =
            exec(genericResourceApiProvider::tunnelxconnTopologyOperation, input, RpcResult::getResult);

        assertEquals("503", output.getResponseCode());
        assertEquals(NO_SERVICE_LOGIC_ACTIVE + APP_NAME + ": '" + SVC_OPERATION + "'", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_success_when_no_errors_encountered() throws Exception {

        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcResultProp.set("security-zone-object-path", "securityZoneObjectPath: XYZ");
        svcClient.mockExecuteWoServiceData(svcResultProp);

        TunnelxconnTopologyOperationInput input = build(tunnelxconnTopologyOperationInput()
            .setRequestInformation(build(requestInformation()
                .setRequestId("test-request-id")
                .setRequestAction(RequestInformation.RequestAction.CreateServiceInstance)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        TunnelxconnTopologyOperationOutput output =
            exec(genericResourceApiProvider::tunnelxconnTopologyOperation, input, RpcResult::getResult);

        assertEquals("200", output.getResponseCode());
        assertEquals("OK", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());

        TunnelxconnTopologyOperationOutput expectedOutput = createExpectedOutput(svcResultProp, input);
        assertEquals(expectedOutput, output);

    }

    private TunnelxconnTopologyOperationOutput createExpectedOutput(PropBuilder propBuilder,
        TunnelxconnTopologyOperationInput input) {

        return build(tunnelxconnTopologyOperationOutput()
            .setTunnelxconnResponseInformation(build(tunnelxconnResponseInformation()
                .setObjectPath(propBuilder.get("tunnelxconn-object-path"))))
            .setResponseCode(propBuilder.get(svcClient.errorCode))
            .setAckFinalIndicator(propBuilder.get(svcClient.ackFinal))
            .setResponseMessage(propBuilder.get(svcClient.errorMessage))
            .setServiceResponseInformation(build(serviceResponseInformation()
                .setInstanceId(input.getServiceInformation().getServiceInstanceId())
                .setObjectPath(propBuilder.get(svcClient.serviceObjectPath))
            ))
        );
    }
}
