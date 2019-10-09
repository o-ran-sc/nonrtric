package org.onap.sdnc.northbound;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GenericConfigurationNotificationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.GenericConfigurationNotificationOutput;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.information.ServiceInformationBuilder;

import static org.junit.Assert.assertNull;
import static org.onap.sdnc.northbound.util.MDSALUtil.*;

@RunWith(MockitoJUnitRunner.class)
public class GenericConfigurationNotificationRPCTest extends GenericResourceApiProviderTest {

    private static final String SVC_OPERATION = "generic-configuration-notification";

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void should_complete_with_success_when_no_errors() throws Exception {

        GenericConfigurationNotificationInput input = build(GenericConfigurationNotificationInput()
                .setServiceInformation(new ServiceInformationBuilder().setServiceInstanceId("serviceInstanceId").build()));

        GenericConfigurationNotificationOutput output =
                exec(genericResourceApiProvider::genericConfigurationNotification, input, RpcResult::getResult);

        assertNull(output);
    }
}

