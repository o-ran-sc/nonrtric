package org.onap.sdnc.northbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PolicyUpdateNotifyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.PolicyUpdateNotifyOperationOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;

import static org.junit.Assert.assertEquals;
import static org.onap.sdnc.northbound.util.MDSALUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class PolicyUpdateNotifyOperationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "policy-update-notify-operation";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_fail_when_invalid_vnf_topology() throws Exception {

        PolicyUpdateNotifyOperationInput input = build(PolicyUpdateNotifyOperationInput());

        PolicyUpdateNotifyOperationOutput output =
                exec(genericResourceApiProvider::policyUpdateNotifyOperation, input, RpcResult::getResult);

        assertEquals("404", output.getErrorCode());
        assertEquals("Invalid input, missing input data", output.getErrorMsg());
    }

    @Test
    public void should_fail_when_valid_vnf_topology() throws Exception {

        PolicyUpdateNotifyOperationInput input = build(PolicyUpdateNotifyOperationInput().setPolicyName("PolicyName").setUpdateType("UpdateType").setVersionId("vID"));

        PolicyUpdateNotifyOperationOutput output =
                exec(genericResourceApiProvider::policyUpdateNotifyOperation, input, RpcResult::getResult);

        assertEquals("503", output.getErrorCode());
        assertEquals("No service logic active for generic-resource-api: 'policy-update-notify-operation'", output.getErrorMsg());
    }
}
