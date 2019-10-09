/*-
 * ============LICENSE_START=======================================================
 * openECOMP : SDN-C
 * ================================================================================
 * Copyright (C) 2017 AT&T Intellectual Property. All rights
 *                             reserved.
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

import org.onap.ccsdk.sli.core.sli.SvcLogicException;
import org.onap.ccsdk.sli.core.sli.provider.SvcLogicService;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.preload.data.PreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.service.data.ServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.preload.data.VfModulePreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vf.module.service.data.VfModuleServiceDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.preload.data.VnfInstancePreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnctl.vnf.rev150720.vnf.instance.service.data.VnfInstanceServiceDataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class VNFSDNSvcLogicServiceClient {
    static final String FAILURE_RESULT = "failure";
    static final String SVC_LOGIC_STATUS_KEY = "SvcLogic.status";
    private static final String TO_SLI_MSG = "Parameters passed to SLI";
    private static final String FROM_SLI_MSG = "Parameters returned by SLI";

    private final Logger logger = LoggerFactory.getLogger(VNFSDNSvcLogicServiceClient.class);

    private final SvcLogicService svcLogic;

    public VNFSDNSvcLogicServiceClient(final SvcLogicService svcLogicService) {
        this.svcLogic = svcLogicService;
    }

    public boolean hasGraph(String module, String rpc, String version, String mode) throws SvcLogicException {
        return svcLogic.hasGraph(module, rpc, version, mode);
    }

    public Properties execute(String module, String rpc, String version, String mode, ServiceDataBuilder serviceData)
            throws SvcLogicException {
        return execute(module, rpc, version, mode, serviceData, new Properties());
    }

    public Properties execute(String module, String rpc, String version, String mode, PreloadDataBuilder serviceData)
            throws SvcLogicException {
        return execute(module, rpc, version, mode, serviceData, new Properties());
    }

    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              ServiceDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = VnfSdnUtil.toProperties(properties, serviceData);
        printDebugLog(props, TO_SLI_MSG);

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printDebugLog(respProps, FROM_SLI_MSG);

        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        VnfSdnUtil.toBuilder(respProps, serviceData);

        return respProps;
    }

    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              PreloadDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = VnfSdnUtil.toProperties(properties, serviceData);
        printDebugLog(props, TO_SLI_MSG);

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printDebugLog(respProps, FROM_SLI_MSG);

        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        VnfSdnUtil.toBuilder(respProps, serviceData);

        return respProps;
    }


    //1610 vnf-instance
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VnfInstanceServiceDataBuilder serviceData)
            throws SvcLogicException {
        return execute(module, rpc, version, mode, serviceData, new Properties());
    }

    //1610 vnf-instance
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VnfInstanceServiceDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = VnfSdnUtil.toProperties(properties, serviceData);
        printDebugLog(props, TO_SLI_MSG);

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printDebugLog(respProps, FROM_SLI_MSG);

        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        VnfSdnUtil.toBuilder(respProps, serviceData);

        return respProps;
    }

    //1610 vf-module
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VfModuleServiceDataBuilder serviceData)
            throws SvcLogicException {
        return execute(module, rpc, version, mode, serviceData, new Properties());
    }

    //1610 vf-module
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VfModuleServiceDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = VnfSdnUtil.toProperties(properties, serviceData);
        printDebugLog(props, TO_SLI_MSG);

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printDebugLog(respProps, FROM_SLI_MSG);

        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        VnfSdnUtil.toBuilder(respProps, serviceData);

        return respProps;
    }

    //1610 vnf-instance-preload
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VnfInstancePreloadDataBuilder serviceData)
            throws SvcLogicException {
        return execute(module, rpc, version, mode, serviceData, new Properties());
    }

    //1610 vnf-instance-preload
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VnfInstancePreloadDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = VnfSdnUtil.toProperties(properties, serviceData);
        printDebugLog(props, TO_SLI_MSG);

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printDebugLog(respProps, FROM_SLI_MSG);

        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        VnfSdnUtil.toBuilder(respProps, serviceData);

        return respProps;
    }

    //1610 vf-module-preload
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VfModulePreloadDataBuilder serviceData)
            throws SvcLogicException {
        return execute(module, rpc, version, mode, serviceData, new Properties());
    }

    //1610 vf-module-preload
    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              VfModulePreloadDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = VnfSdnUtil.toProperties(properties, serviceData);
        printDebugLog(props, TO_SLI_MSG);

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printDebugLog(respProps, FROM_SLI_MSG);

        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        VnfSdnUtil.toBuilder(respProps, serviceData);

        return respProps;
    }

    private void printDebugLog(Properties properties, String msg) {
        if (!logger.isDebugEnabled()) {
            return;
        }
        if (properties == null) {
            logger.debug(msg, "properties is null");
            return;
        }

        logger.debug(msg);
        for (Object key : properties.keySet()) {
            String paramName = (String) key;
            logger.debug(paramName, " = ", properties.getProperty(paramName));
        }
    }

}
