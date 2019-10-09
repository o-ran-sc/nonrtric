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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

public class VnfSdnUtilTest {

    private VnfSdnUtil vnfSdnUtil;

    @Before public void setUp() throws Exception {
        vnfSdnUtil = new VnfSdnUtil();
    }

    @Test public void loadProperties() throws Exception {
        vnfSdnUtil.loadProperties();
        Object properties = Whitebox.getInternalState(VnfSdnUtil.class, "properties");
        Assert.assertNotNull(properties);
    }
}
