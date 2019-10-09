/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 * 							reserved.
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

import com.google.common.util.concurrent.CheckedFuture;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.Mockito;
import org.onap.ccsdk.sli.core.sli.SvcLogicException;
import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.binding.impl.BindingDOMDataBrokerAdapter;
import org.opendaylight.controller.md.sal.binding.impl.BindingToNormalizedNodeCodec;
import org.opendaylight.controller.md.sal.binding.test.AbstractDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.binding.test.ConcurrentDataBrokerTestCustomizer;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.md.sal.dom.api.DOMDataBroker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationInput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationOutput;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.request.information.RequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.request.information.RequestInformation.RequestAction;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.request.information.RequestInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.request.information.VfModuleRequestInformation;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.request.information.VfModuleRequestInformationBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.service.data.VfModuleServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.VfModuleTopologyOperationInputBuilder;

import java.util.Properties;
import java.util.concurrent.ExecutionException;

class VfModuleExceptionThrowerConfigurator {
    private boolean shouldThrowExceptionDuringTransactionPut;

    public VfModuleExceptionThrowerConfigurator() {
        this.shouldThrowExceptionDuringTransactionPut = true;
    }

    public boolean shouldThrowExceptionDuringTransactionPut() {
        return shouldThrowExceptionDuringTransactionPut;
    }

    public void setShouldThrowExceptionDuringTransactionPut(boolean shouldThrowExceptionDuringTransactionPut) {
        this.shouldThrowExceptionDuringTransactionPut = shouldThrowExceptionDuringTransactionPut;
    }
}

class VfModuleDataBrokerErrorMsgConfigurator {
    static public String JAVA_LANG_RUNTIME_EXCEPTION = "java.lang.RuntimeException: ";
    static public String TRANSACTION_WRITE_ERROR = "transaction-write-error";
}

class VfModuleDataBrokerStab extends BindingDOMDataBrokerAdapter {
    VfModuleExceptionThrowerConfigurator exceptionThrowerConfigurator;

    public VfModuleDataBrokerStab(final DOMDataBroker domDataBroker,
        final BindingToNormalizedNodeCodec codec, VfModuleExceptionThrowerConfigurator exceptionThrowerConfigurator) {
        super(domDataBroker, codec);
        this.exceptionThrowerConfigurator = exceptionThrowerConfigurator;

    }

    @Override
    public WriteTransaction newWriteOnlyTransaction() {
        if (exceptionThrowerConfigurator.shouldThrowExceptionDuringTransactionPut()) {
            return newThrowingExceptionWriteOnlyTransaction();
        }
        return newNotThrowingExceptionWriteOnlyTransaction();
    }

    private WriteTransaction newThrowingExceptionWriteOnlyTransaction() {
        WriteTransaction mockWriteTransaction = Mockito.mock(WriteTransaction.class);
        Mockito.doThrow(new RuntimeException(VfModuleDataBrokerErrorMsgConfigurator.TRANSACTION_WRITE_ERROR))
                .when(mockWriteTransaction).put(Mockito.any(), Mockito.any(), Mockito.any());
        return mockWriteTransaction;
    }

    private WriteTransaction newNotThrowingExceptionWriteOnlyTransaction() {
         WriteTransaction mockWriteTransaction = Mockito.mock(WriteTransaction.class);
        CheckedFuture<Void, TransactionCommitFailedException> mockCheckedFuture =
                Mockito.mock(CheckedFuture.class);
        Mockito.doNothing().when(mockWriteTransaction).put(Mockito.any(), Mockito.any(), Mockito.any());
        Mockito.doReturn(mockCheckedFuture).when(mockWriteTransaction).submit();
        return mockWriteTransaction;
    }
}

class VfModuleProviderDataBrokerTestCustomizer  extends ConcurrentDataBrokerTestCustomizer {
    VfModuleExceptionThrowerConfigurator exceptionThrowerConfigurator;

    public VfModuleProviderDataBrokerTestCustomizer(VfModuleExceptionThrowerConfigurator exceptionThrowerConfigurator) {
        super(false);
        this.exceptionThrowerConfigurator = exceptionThrowerConfigurator;
    }

    public DataBroker createDataBroker() {
        return new VfModuleDataBrokerStab(createDOMDataBroker(),
            super.getBindingToNormalized(), this.exceptionThrowerConfigurator);
    }
}

@RunWith(MockitoJUnitRunner.class)
public class VfModuleTopologyOperationTest extends VnfApiProviderTest {
    protected static final Logger LOG = LoggerFactory.getLogger(VfModuleTopologyOperationTest.class);

    private static final String INVALID_VFMODULEID = "invalid input, null or empty vf-module-id";
    private static final String INVALID_VNFINSTANCEID = "invalid input, null or empty vnf-instance-id";
    private static final String NO_SERVICE_LOGIC = "No service logic active for VNF-API: \'vf-module-topology-operation\'";
    private static final String VF_MODULE_ID = "vfModule1";
    private static final String VNF_INSTANCE_ID = "vnfInstance1";
    private static final String PRELOAD_NAME = "preloadName";
    private static final String PRELOAD_TYPE = "preloadType";
    private static final String ERROR_CODE = "error-code";
    
    private static final String ERROR_MESSAGE = "error-message";
    private static final String ACK_FINAL = "ack-final";
    private static final String SVC_OPERATION = "vf-module-topology-operation";

    private VfModuleExceptionThrowerConfigurator exceptionThrowerConfigurator;

    @Override
    protected AbstractDataBrokerTestCustomizer createDataBrokerTestCustomizer() {
        this.exceptionThrowerConfigurator = new VfModuleExceptionThrowerConfigurator();
        return new VfModuleProviderDataBrokerTestCustomizer(this.exceptionThrowerConfigurator);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        svcClient.setScvOperation(SVC_OPERATION);
    }

    @Test
    public void vfModuleTopologyOperationInputIsNull() throws Exception {
        VfModuleTopologyOperationInput input = null;
        checkVfModuleTopologyOperation(input, "403", INVALID_VFMODULEID);
    }

    @Test
    public void vfModuleTopologyOperationInput_VfModuleRequestInformationIsNull() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = new VfModuleTopologyOperationInputBuilder();
        builder.setVfModuleRequestInformation(null);
        VfModuleTopologyOperationInput input = builder.build();
        checkVfModuleTopologyOperation(input, "403", INVALID_VFMODULEID);
    }

    @Test
    public void vfModuleTopologyOperationInput_getVfModuleRequestInformationVfModuleIdIsNull() throws Exception {
        RequestInformation reqInfo = createRequestInformation(RequestAction.PreloadVfModuleRequest);
        VfModuleRequestInformation vfModuleRequestInformation = createVfModuleRequestInformation(VNF_INSTANCE_ID, null);
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(vfModuleRequestInformation, reqInfo);
        VfModuleTopologyOperationInput input = builder.build();
        checkVfModuleTopologyOperation(input, "403", INVALID_VFMODULEID);
    }

    @Test
    public void vfModuleTopologyOperationInput_VfModuleRequestInformationVfModuleIdIsZero() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID,""), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();
        checkVfModuleTopologyOperation(input, "403", INVALID_VFMODULEID);
    }
    
    @Test
    public void vfModuleTopologyOperationInput_getVfModuleRequestInformationVnfInstanceIdIsNull() throws Exception {
        RequestInformation reqInfo = createRequestInformation(RequestAction.PreloadVfModuleRequest);
        VfModuleRequestInformation vfModuleRequestInformation = createVfModuleRequestInformation(null, VF_MODULE_ID);
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(vfModuleRequestInformation, reqInfo);
        VfModuleTopologyOperationInput input = builder.build();
        checkVfModuleTopologyOperation(input, "403", INVALID_VNFINSTANCEID);
    }

    @Test
    public void vfModuleTopologyOperationInput_VfModuleRequestInformationVnfInstanceIdIsZero() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation("",VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();
        checkVfModuleTopologyOperation(input, "403", INVALID_VNFINSTANCEID);
    }

    @Test
    public void vfModuleTopologyOperationInput_svcLogicClientHasGrapheReturnFalse() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();
        setReturnForSvcLogicServiceClientHasGraph(false);
        checkVfModuleTopologyOperation(input, "503", NO_SERVICE_LOGIC);
    }

    @Test
    public void vfModuleTopologyOperationInput_svcLogicClientExecuteThrowsSvcLogicException() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();
        setReturnForSvcLogicServiceClientHasGraph(true);
        setMockVNFSDNSvcLogicServiceClientToThrowException(SvcLogicException.class);
        checkVfModuleTopologyOperation(input, "500", null);
    }

    @Test
    public void vfModuleTopologyOperationInput_svcLogicClientExecuteThrowsException() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setMockVNFSDNSvcLogicServiceClientToThrowException(Exception.class);
        checkVfModuleTopologyOperation(input, "500", null);
    }

    @Test
    public void vfModuleTopologyOperationInput_svcLogicClientExecuteReturnsNotNull() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();

        Properties properties = prop().set(ERROR_CODE, "500")
                .set(ERROR_MESSAGE, ERROR_MESSAGE)
                .set(ACK_FINAL, "Y")
                .build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(properties);
        checkVfModuleTopologyOperation(input, "500", ERROR_MESSAGE);
    }

    @Test
    public void vfModuleTopologyOperationInput_svcLogicClientExecuteReturnsNull() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(null);
        String expectedErrorMsg = VfModuleDataBrokerErrorMsgConfigurator.JAVA_LANG_RUNTIME_EXCEPTION
                + VfModuleDataBrokerErrorMsgConfigurator.TRANSACTION_WRITE_ERROR;
        checkVfModuleTopologyOperation(input, "500", expectedErrorMsg);
    }

    @Test
    public void vfModuleTopologyOperationInput_ActivateNoErrorDuringTransactionWriting() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.VfModuleActivateRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(null);
        exceptionThrowerConfigurator.setShouldThrowExceptionDuringTransactionPut(false);
        checkVfModuleTopologyOperation(input, "200", null);
    }
    
    @Test
    public void vfModuleTopologyOperationInput_ChangeNoErrorDuringTransactionWriting() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.ChangeVfModuleActivateRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(null);
        exceptionThrowerConfigurator.setShouldThrowExceptionDuringTransactionPut(false);
        checkVfModuleTopologyOperation(input, "200", null);
    }
    
    @Test
    public void vfModuleTopologyOperationInput_DisconnectNoErrorDuringTransactionWriting() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.DisconnectVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(null);
        exceptionThrowerConfigurator.setShouldThrowExceptionDuringTransactionPut(false);
        checkVfModuleTopologyOperation(input, "200", null);
    }

    @Test
    public void vfModuleTopologyOperationInput_PreloadNoErrorDuringTransactionWriting() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.PreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(null);
        exceptionThrowerConfigurator.setShouldThrowExceptionDuringTransactionPut(false);
        checkVfModuleTopologyOperation(input, "200", null);
    }
    
    @Test
    public void vfModuleTopologyOperationInput_DeletePreloadNoErrorDuringTransactionWriting() throws Exception {
        VfModuleTopologyOperationInputBuilder builder = createVfModuleTopologyOperationInputBuilder(createVfModuleRequestInformation(VNF_INSTANCE_ID, VF_MODULE_ID), createRequestInformation(RequestAction.DeletePreloadVfModuleRequest));
        VfModuleTopologyOperationInput input = builder.build();

        setReturnForSvcLogicServiceClientHasGraph(true);
        setReturnForSvcLogicServiceClientExecute(null);
        exceptionThrowerConfigurator.setShouldThrowExceptionDuringTransactionPut(false);
        checkVfModuleTopologyOperation(input, "200", null);
    }
    private void checkVfModuleTopologyOperation(VfModuleTopologyOperationInput input,
        String expectedResponseCode, String expectedResponseMessage) throws ExecutionException, InterruptedException {

        VfModuleTopologyOperationOutput output = executeVfModuleTopologyOperation(input);
        checkVfModuleTopologyOperationOutput(output, expectedResponseCode, expectedResponseMessage);
    }

    private VfModuleTopologyOperationOutput executeVfModuleTopologyOperation(
        VfModuleTopologyOperationInput input) throws ExecutionException, InterruptedException {
        return vnfapiProvider
                .vfModuleTopologyOperation(input)
                .get()
                .getResult();
    }

    private void checkVfModuleTopologyOperationOutput(VfModuleTopologyOperationOutput result,
        String expectedResponseCode, String expectedResponseMessage) {

        String expectedAckFinalIndicator = "Y";

        Assert.assertEquals(expectedResponseCode , result.getResponseCode());
        Assert.assertEquals(expectedResponseMessage, result.getResponseMessage());
        Assert.assertEquals(expectedAckFinalIndicator, result.getAckFinalIndicator());
    }

    private void setReturnForSvcLogicServiceClientHasGraph(Boolean returnValue) throws Exception{
        Mockito.when(mockVNFSDNSvcLogicServiceClient
                .hasGraph(Mockito.any(),Mockito.any(), Mockito.any(),Mockito.any()))
                .thenReturn(returnValue);
    }

    private void setReturnForSvcLogicServiceClientExecute(Properties properties) throws Exception{
        Mockito.when(mockVNFSDNSvcLogicServiceClient
                .execute(Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(VfModuleServiceDataBuilder.class),
                        Mockito.any()))
                .thenReturn(properties);
    }

    private void setMockVNFSDNSvcLogicServiceClientToThrowException(Class exceptionClass) throws Exception {
        Mockito.when(mockVNFSDNSvcLogicServiceClient
                .execute(Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(),
                        Mockito.any(VfModuleServiceDataBuilder.class),
                        Mockito.any()))
                .thenThrow(exceptionClass.asSubclass(Throwable.class));
    }

    private VfModuleTopologyOperationInputBuilder createVfModuleTopologyOperationInputBuilder(VfModuleRequestInformation vfModuleRequestInformation, RequestInformation reqInfo) {
        VfModuleTopologyOperationInputBuilder builder = new VfModuleTopologyOperationInputBuilder();
        builder.setVfModuleRequestInformation(vfModuleRequestInformation);
        builder.setRequestInformation(reqInfo);
        return builder;
    }



    private VfModuleRequestInformation createVfModuleRequestInformation(String vnfInstanceId, String vfModuleId) {
        return new VfModuleRequestInformationBuilder()
                .setVnfInstanceId(vnfInstanceId)
                .setVfModuleId(vfModuleId)
                .setVfModuleName(PRELOAD_NAME)
                .setVfModuleModelId(PRELOAD_TYPE)
                .build();
    }
    
    private RequestInformation createRequestInformation(RequestAction action) {
        return new RequestInformationBuilder()
                .setRequestAction(action)
                .build();
    }
    
    
}
