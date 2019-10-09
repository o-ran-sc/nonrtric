package org.onap.sdnc.northbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GetpathsegmentTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GetpathsegmentTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.information.ServiceInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.onap.model.information.OnapModelInformationBuilder;
import org.opendaylight.yangtools.yang.common.RpcResult;

import static org.junit.Assert.assertEquals;
import static org.onap.sdnc.northbound.util.MDSALUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class GetpathsegmentTopologyOperationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "getpathsegment-topology-operation";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_invalid_vnf_topology() throws Exception {

        GetpathsegmentTopologyOperationInput input = build(GetpathsegmentTopologyOperationInput());

        GetpathsegmentTopologyOperationOutput output =
                exec(genericResourceApiProvider::getpathsegmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getResponseCode());
        assertEquals("invalid input, null or empty service-instance-id", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_valid_vnf_topology() throws Exception {

        GetpathsegmentTopologyOperationInput input = build(GetpathsegmentTopologyOperationInput()
                .setServiceInformation(new ServiceInformationBuilder().setServiceInstanceId("ServiceInstanceID").build()));

        GetpathsegmentTopologyOperationOutput output =
                exec(genericResourceApiProvider::getpathsegmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getResponseCode());
        assertEquals("invalid input, no model-uuid provided", output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }

    @Test
    public void should_fail_when_valid_serice_excep_vnf_topology() throws Exception {

        GetpathsegmentTopologyOperationInput input = build(GetpathsegmentTopologyOperationInput()
                .setServiceInformation(new ServiceInformationBuilder().setServiceInstanceId("ServiceInstanceID")
                        .setOnapModelInformation(new OnapModelInformationBuilder().setModelUuid("moduleUUID").build()).build()));

        GetpathsegmentTopologyOperationOutput output =
                exec(genericResourceApiProvider::getpathsegmentTopologyOperation, input, RpcResult::getResult);

        assertEquals("503", output.getResponseCode());
        assertEquals("No service logic active for generic-resource-api: 'getpathsegment-topology-operation'",
                output.getResponseMessage());
        assertEquals("Y", output.getAckFinalIndicator());
    }
}
