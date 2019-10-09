package org.onap.sdnc.northbound;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.APP_NAME;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.INVALID_INPUT_ERROR_MESSAGE;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NO_SERVICE_LOGIC_ACTIVE;
import static org.onap.sdnc.northbound.GenericResourceApiProvider.NULL_OR_EMPTY_ERROR_PARAM;
import static org.onap.sdnc.northbound.util.MDSALUtil.build;
import static org.onap.sdnc.northbound.util.MDSALUtil.connectionAttachmentResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.connectionAttachmentTopologyOperationInput;
import static org.onap.sdnc.northbound.util.MDSALUtil.connectionAttachmentTopologyOperationOutput;
import static org.onap.sdnc.northbound.util.MDSALUtil.exec;
import static org.onap.sdnc.northbound.util.MDSALUtil.requestInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.sdncRequestHeader;
import static org.onap.sdnc.northbound.util.MDSALUtil.service;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceData;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceInformationBuilder;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceLevelOperStatus;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceResponseInformation;
import static org.onap.sdnc.northbound.util.MDSALUtil.serviceStatus;

import java.time.Instant;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.onap.sdnc.northbound.util.PropBuilder;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.TransactionChainClosedException;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ConnectionAttachmentTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.ConnectionAttachmentTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.OperStatusData.LastAction;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.OperStatusData.LastOrderStatus;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.OperStatusData.LastRpcAction;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.OperStatusData.OrderStatus;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.sdnc.request.header.SdncRequestHeader.SvcAction;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceData;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.information.ServiceInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.model.infrastructure.Service;
import org.opendaylight.yangtools.yang.common.RpcResult;

@RunWith(MockitoJUnitRunner.class)
public class ConnectionAttachmentTopologyOperationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "connection-attachment-topology-operation";


    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_service_instance_id_not_present() throws Exception {

        ConnectionAttachmentTopologyOperationInput input = build(connectionAttachmentTopologyOperationInput());

        ConnectionAttachmentTopologyOperationOutput output =
            exec(genericResourceApiProvider::connectionAttachmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getResponseCode());
        assertEquals(NULL_OR_EMPTY_ERROR_PARAM, output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_fail_when_invalid_service_data() throws Exception {

        ConnectionAttachmentTopologyOperationInput input = build(connectionAttachmentTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        ConnectionAttachmentTopologyOperationOutput output =
            exec(genericResourceApiProvider::connectionAttachmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getResponseCode());
        assertEquals(INVALID_INPUT_ERROR_MESSAGE, output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_fail_when_client_execution_failed() throws Exception {

        svcClient.mockHasGraph(true);
        svcClient.mockExecute(new RuntimeException("test exception"));

        ConnectionAttachmentTopologyOperationInput input = build(connectionAttachmentTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        persistServiceInDataBroker(input);

        ConnectionAttachmentTopologyOperationOutput output =
            exec(genericResourceApiProvider::connectionAttachmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_client_has_no_graph() throws Exception {

        svcClient.mockHasGraph(false);

        ConnectionAttachmentTopologyOperationInput input = build(connectionAttachmentTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        persistServiceInDataBroker(input);

        ConnectionAttachmentTopologyOperationOutput output =
            exec(genericResourceApiProvider::connectionAttachmentTopologyOperation, input, RpcResult::getResult);

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

        ConnectionAttachmentTopologyOperationInput input = build(connectionAttachmentTopologyOperationInput()
            .setSdncRequestHeader(build(sdncRequestHeader()
                .setSvcRequestId("test-svc-request-id")
                .setSvcAction(SvcAction.Assign)
            ))
            .setServiceInformation(build(serviceInformationBuilder()
                .setServiceInstanceId("test-service-instance-id")
            ))
        );

        persistServiceInDataBroker(input);

        ConnectionAttachmentTopologyOperationOutput output =
            exec(genericResourceApiProvider::connectionAttachmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("500", output.getResponseCode());
        assertEquals("test exception", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }


    @Test
    public void should_success_when_no_errors_encountered() throws Exception {

        svcClient.mockHasGraph(true);
        PropBuilder svcResultProp = svcClient.createExecuteOKResult();
        svcResultProp.set("contrail-route-object-path", "connectionAttachmentObjectPath: XYZ");
        svcClient.mockExecute(svcResultProp);

        ConnectionAttachmentTopologyOperationInput input = build(connectionAttachmentTopologyOperationInput()
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
        );

        Service service = persistServiceInDataBroker(input);

        ConnectionAttachmentTopologyOperationOutput output =
            exec(genericResourceApiProvider::connectionAttachmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("200", output.getResponseCode());
        assertEquals("OK", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());

        ConnectionAttachmentTopologyOperationOutput expectedOutput = createExpectedOutput(svcResultProp, input);
        assertEquals(expectedOutput, output);

        Service actualService = db
            .read(input.getServiceInformation().getServiceInstanceId(), LogicalDatastoreType.CONFIGURATION);

        Service expectedService = createExpectedService(input, service.getServiceData());
        assertEquals(expectedService, actualService);
    }

    private Service persistServiceInDataBroker(ConnectionAttachmentTopologyOperationInput input) throws Exception {

        Service service = build(service()
            .setServiceInstanceId(input.getServiceInformation().getServiceInstanceId())
            .setServiceData(build(serviceData()
                .setServiceLevelOperStatus(build(serviceLevelOperStatus()
                    .setOrderStatus(OrderStatus.Created)
                    .setModifyTimestamp(Instant.now().toString())
                    .setLastSvcRequestId(input.getSdncRequestHeader().getSvcRequestId())
                    .setLastRpcAction(LastRpcAction.Activate)
                    .setLastOrderStatus(LastOrderStatus.PendingAssignment)
                    .setLastAction(LastAction.ActivateNetworkInstance)
                    .setCreateTimestamp(Instant.now().toString())
                ))
            ))
        );
        db.write(true, service, LogicalDatastoreType.CONFIGURATION);
        return service;
    }

    private ConnectionAttachmentTopologyOperationOutput createExpectedOutput(PropBuilder propBuilder,
        ConnectionAttachmentTopologyOperationInput input) {

        return build(connectionAttachmentTopologyOperationOutput()
            .setConnectionAttachmentResponseInformation(build(connectionAttachmentResponseInformation()
                .setObjectPath(propBuilder.get("connection-attachement-object-path"))))
            .setSvcRequestId(input.getSdncRequestHeader().getSvcRequestId())
            .setResponseCode(propBuilder.get(svcClient.errorCode))
            .setAckFinalIndicator(propBuilder.get(svcClient.ackFinal))
            .setResponseMessage(propBuilder.get(svcClient.errorMessage))
            .setServiceResponseInformation(build(serviceResponseInformation()
                .setInstanceId(input.getServiceInformation().getServiceInstanceId())
                .setObjectPath(propBuilder.get(svcClient.serviceObjectPath))
            ))
        );
    }

    private Service createExpectedService(
        ConnectionAttachmentTopologyOperationInput expectedInput, ServiceData expectedServiceData) {

        ServiceInformation expectedServiceInformation = expectedInput.getServiceInformation();

        return build(service()
            .setServiceInstanceId(expectedServiceInformation.getServiceInstanceId())
            .setServiceData(build(serviceData()))
            .setServiceData(expectedServiceData)
            .setServiceStatus(build(serviceStatus()))
        );
    }
}
