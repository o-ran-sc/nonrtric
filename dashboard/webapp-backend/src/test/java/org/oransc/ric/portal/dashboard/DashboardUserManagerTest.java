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
package org.oransc.ric.portal.dashboard;

import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

import org.junit.Assert;
import org.junit.jupiter.api.Test;
import org.onap.portalsdk.core.onboarding.exception.PortalAPIException;
import org.onap.portalsdk.core.restful.domain.EcompRole;
import org.onap.portalsdk.core.restful.domain.EcompUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
public class DashboardUserManagerTest {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static EcompUser createEcompUser(String loginId) {
        EcompUser user = new EcompUser();
        user.setActive(true);
        user.setLoginId(loginId);
        user.setFirstName("First");
        user.setLastName("Last");
        EcompRole role = new EcompRole();
        role.setId(1L);
        role.setName(DashboardConstants.ROLE_NAME_ADMIN);
        Set<EcompRole> roles = new HashSet<>();
        roles.add(role);
        user.setRoles(roles);
        return user;
    }

    @Test
    public void testUserMgr() throws Exception {
        final String loginId = "demo";
        DashboardUserManager dum = new DashboardUserManager(true);
        EcompUser user = createEcompUser(loginId);
        dum.createUser(user);
        logger.info("Created user {}", user);
        try {
            dum.createUser(user);
            throw new Exception("Unexpected success");
        } catch (PortalAPIException ex) {
            logger.info("caught expected exception: {}", ex.toString());
        }
        Assert.assertFalse(dum.getUsers().isEmpty());
        EcompUser fetched = dum.getUser(loginId);
        Assert.assertEquals(fetched, user);
        fetched.setLastName("Lastier");
        dum.updateUser(loginId, fetched);
        EcompUser missing = dum.getUser("foo");
        Assert.assertNull(missing);
        EcompUser unk = createEcompUser("unknown");
        try {
            dum.updateUser("unk", unk);
        } catch (PortalAPIException ex) {
            logger.info("caught expected exception: {}", ex.toString());
        }
    }

}
