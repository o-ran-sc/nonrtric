/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 AT&T Intellectual Property
 * %%
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
 * ========================LICENSE_END===================================
 */
package org.oransc.ric.portal.dashboard.portalapi;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.onap.portalsdk.core.onboarding.util.PortalApiConstants;
import org.oransc.ric.portal.dashboard.DashboardUserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
public class PortalAuthManagerTest {

    @Value("${portalapi.decryptor}")
    private String decryptor;

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Test
    public void testPortalStuff() throws ClassNotFoundException, InstantiationException, IllegalAccessException,
        InvocationTargetException, NoSuchMethodException, IOException, ServletException {

        PortalAuthManager m = new PortalAuthManager("app", "user", "secret", decryptor, "cookie");
        Assert.assertNotNull(m.getAppCredentials());
        String s = null;

        MockHttpServletRequest request = new MockHttpServletRequest();
        s = m.validateEcompSso(request);
        logger.debug("validateEcompSso answers {}", s);
        Assert.assertNull(s);

        Cookie cookie = new Cookie(PortalApiConstants.EP_SERVICE, "bogus");
        request.setCookies(cookie);
        s = m.validateEcompSso(request);
        logger.debug("validateEcompSso answers {}", s);
        Assert.assertNull(s);

        DashboardUserManager dum = new DashboardUserManager(true);
        PortalAuthenticationFilter filter = new PortalAuthenticationFilter(false, m, dum);
        filter.init(null);
        filter.destroy();
        MockHttpServletResponse response = new MockHttpServletResponse();
        try {
            filter.doFilter(request, response, null);
        } catch (NullPointerException ex) {
            logger.debug("chain is null");
        }

        filter = new PortalAuthenticationFilter(true, m, dum);
        try {
            filter.doFilter(request, response, null);
        } catch (NullPointerException ex) {
            logger.debug("chain is null");
        }
    }

}
