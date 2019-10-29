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

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.onap.portalsdk.core.onboarding.exception.PortalAPIException;
import org.onap.portalsdk.core.restful.domain.EcompRole;
import org.onap.portalsdk.core.restful.domain.EcompUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides simple user-management services.
 * 
 * This first implementation serializes user details to a file.
 * 
 * TODO: migrate to a database.
 */
public class DashboardUserManager {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// This default value is only useful for development and testing.
	public static final String USER_FILE_PATH = "dashboard-users.json";

	private final File userFile;
	private final List<EcompUser> users;

	/**
	 * Development/test-only constructor that uses default file path.
	 * 
	 * @param clear
	 *                  If true, start empty and remove any existing file.
	 * 
	 * @throws IOException
	 *                         On file error
	 */
	public DashboardUserManager(boolean clear) throws IOException {
		this(USER_FILE_PATH);
		if (clear) {
			logger.debug("ctor: removing file {}", userFile.getAbsolutePath());
			File f = new File(DashboardUserManager.USER_FILE_PATH);
			if (f.exists())
				f.delete();
			users.clear();
		}
	}

	/**
	 * Constructur that accepts a file path
	 * 
	 * @param userFilePath
	 *                         File path
	 * @throws IOException
	 *                         If file cannot be read
	 */
	public DashboardUserManager(final String userFilePath) throws IOException {
		logger.debug("ctor: userfile {}", userFilePath);
		if (userFilePath == null)
			throw new IllegalArgumentException("Missing or empty user file property");
		userFile = new File(userFilePath);
		logger.debug("ctor: managing users in file {}", userFile.getAbsolutePath());
		if (userFile.exists()) {
			final ObjectMapper mapper = new ObjectMapper();
			users = mapper.readValue(userFile, new TypeReference<List<EcompUser>>() {
			});
		} else {
			users = new ArrayList<>();
		}
	}

	/**
	 * Gets the current users.
	 * 
	 * @return List of EcompUser objects, possibly empty
	 */
	public List<EcompUser> getUsers() {
		return this.users;
	}

	/**
	 * Gets the user with the specified login Id
	 * 
	 * @param loginId
	 *                    Desired login Id
	 * @return User object; null if Id is not known
	 */
	public EcompUser getUser(String loginId) {
		for (EcompUser u : this.users) {
			if (u.getLoginId().equals(loginId)) {
				logger.debug("getUser: match on {}", loginId);
				return u;
			}
		}
		logger.debug("getUser: no match on {}", loginId);
		return null;
	}

	private void saveUsers() throws JsonGenerationException, JsonMappingException, IOException {
		final ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(userFile, users);
	}

	/*
	 * Allow at most one thread to create a user at one time.
	 */
	public synchronized void createUser(EcompUser user) throws PortalAPIException {
		logger.debug("createUser: loginId is " + user.getLoginId());
		if (users.contains(user))
			throw new PortalAPIException("User exists: " + user.getLoginId());
		users.add(user);
		try {
			saveUsers();
		} catch (Exception ex) {
			throw new PortalAPIException("Save failed", ex);
		}
	}

	/*
	 * Allow at most one thread to modify a user at one time. We still have
	 * last-edit-wins of course.
	 */
	public synchronized void updateUser(String loginId, EcompUser user) throws PortalAPIException {
		logger.debug("editUser: loginId is " + loginId);
		int index = users.indexOf(user);
		if (index < 0)
			throw new PortalAPIException("User does not exist: " + user.getLoginId());
		users.remove(index);
		users.add(user);
		try {
			saveUsers();
		} catch (Exception ex) {
			throw new PortalAPIException("Save failed", ex);
		}
	}

	// Test infrastructure
	public static void main(String[] args) throws Exception {
		DashboardUserManager dum = new DashboardUserManager(false);
		EcompUser user = new EcompUser();
		user.setActive(true);
		user.setLoginId("demo");
		user.setFirstName("First");
		user.setLastName("Last");
		EcompRole role = new EcompRole();
		role.setId(1L);
		role.setName(DashboardConstants.ROLE_NAME_ADMIN);
		Set<EcompRole> roles = new HashSet<>();
		roles.add(role);
		user.setRoles(roles);
		dum.createUser(user);
		logger.debug("Created user {}", user);
	}

}
