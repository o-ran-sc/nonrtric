/*-
z * ============LICENSE_START=======================================================
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

package org.onap.sdnc.northbound;

import org.onap.ccsdk.sli.core.sli.SvcLogicException;
import org.onap.ccsdk.sli.core.sli.provider.SvcLogicService;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.preload.data.PreloadDataBuilder;
import org.opendaylight.yang.gen.v1.org.onap.sdnc.northbound.generic.resource.rev170824.service.data.ServiceDataBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

public class GenericResourceApiSvcLogicServiceClient {
    static final String FAILURE_RESULT = "failure";
    static final String SVC_LOGIC_STATUS_KEY = "SvcLogic.status";
    static final String SKIP_MDSAL_UPDATE_PROP = "skip-mdsal-update";

    private final Logger LOG = LoggerFactory
            .getLogger(GenericResourceApiSvcLogicServiceClient.class);

    private SvcLogicService svcLogic = null;

    public GenericResourceApiSvcLogicServiceClient(SvcLogicService svcLogic)
    {
        this.svcLogic = svcLogic;
    }

    public boolean hasGraph(String module, String rpc, String version, String mode) throws SvcLogicException
    {
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

        Properties props = GenericResourceApiUtil.toProperties(properties, serviceData);
        printPropsDebugLogs(props, "Parameters passed to SLI");

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printPropsDebugLogs(respProps, "Parameters returned by SLI");
        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return respProps;
        }

        String skipMdsalUpdate = respProps.getProperty(SKIP_MDSAL_UPDATE_PROP);
        if ((skipMdsalUpdate == null) || !skipMdsalUpdate.equals("Y")) {
            GenericResourceApiUtil.toBuilder(respProps, serviceData);
        } else {
            LOG.debug("Skipping call to MdsalHelper.toBuilder");
        }

        return respProps;
    }


    public Properties execute(String module,
                              String rpc,
                              String version,
                              String mode,
                              PreloadDataBuilder serviceData,
                              Properties properties)
            throws SvcLogicException {

        Properties props = GenericResourceApiUtil.toProperties(properties, serviceData);
        printPropsDebugLogs(props, "Parameters passed to SLI");

        Properties respProps = svcLogic.execute(module, rpc, version, mode, props);
        printPropsDebugLogs(respProps, "Parameters returned by SLI");
        if (respProps == null
                || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
            return (respProps);
        }

        GenericResourceApiUtil.toBuilder(respProps, serviceData);

        return respProps;
    }

	public Properties execute(String module, String rpc, String version, String mode, Properties properties)
			throws SvcLogicException {

		printPropsDebugLogs(properties, "Parameters passed to SLI");

		Properties respProps = svcLogic.execute(module, rpc, version, mode, properties);
		printPropsDebugLogs(respProps, "Parameters returned by SLI");
		if (respProps == null || FAILURE_RESULT.equalsIgnoreCase(respProps.getProperty(SVC_LOGIC_STATUS_KEY))) {
			return (respProps);
		}

		return respProps;
	}


    private void printPropsDebugLogs(Properties properties, String msg) {
        if (!LOG.isDebugEnabled()) {
            return;
        }
        if (properties == null) {
            LOG.debug(msg, "properties is null");
            return;
        }

        LOG.debug(msg);
        for (Object key : properties.keySet()) {
            String paramName = (String) key;
            LOG.debug(paramName + " = " + properties.getProperty(paramName));
        }
    }
}
