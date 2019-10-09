package org.onap.sdnc.northbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PortMirrorTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PortMirrorTopologyOperationOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

import static org.junit.Assert.assertEquals;
import static org.onap.sdnc.northbound.util.MDSALUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class PortMirrorTopologyOperationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "policy-update-notify-operation";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_invalid_vnf_topology() throws Exception {

        PortMirrorTopologyOperationInput input = build(PortMirrorTopologyOperationInput());

        PortMirrorTopologyOperationOutput output =
                exec(genericResourceApiProvider::portMirrorTopologyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getResponseCode());
        assertEquals("invalid input, null or empty service-instance-id", output.getResponseMessage());
    }
}
