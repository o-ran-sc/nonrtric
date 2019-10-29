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
package org.oransc.ric.portal.dashboard.config;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.InvocationTargetException;
import org.onap.portalsdk.core.onboarding.util.PortalApiConstants;
import org.oransc.ric.portal.dashboard.DashboardUserManager;
import org.oransc.ric.portal.dashboard.controller.A1Controller;
import org.oransc.ric.portal.dashboard.controller.SimpleErrorController;
import org.oransc.ric.portal.dashboard.portalapi.PortalAuthManager;
import org.oransc.ric.portal.dashboard.portalapi.PortalAuthenticationFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@Profile("!test")
public class WebSecurityConfiguration extends WebSecurityConfigurerAdapter {

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	// Although constructor arguments are recommended over field injection,
	// this results in fewer lines of code.
	@Value("${portalapi.security}")
	private Boolean portalapiSecurity;
	@Value("${portalapi.appname}")
	private String appName;
	@Value("${portalapi.username}")
	private String userName;
	@Value("${portalapi.password}")
	private String password;
	@Value("${portalapi.decryptor}")
	private String decryptor;
	@Value("${portalapi.usercookie}")
	private String userCookie;

	@Autowired
	DashboardUserManager userManager;

	@Override
    protected void configure(HttpSecurity http) throws Exception {
		logger.debug("configure: portalapi.username {}", userName);
		// A chain of ".and()" always baffles me
		http.authorizeRequests().anyRequest().authenticated();
		http.headers().frameOptions().disable();
		http.csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
		http.addFilterBefore(portalAuthenticationFilterBean(), BasicAuthenticationFilter.class);
	}

	/**
	 * Resource paths that do not require authentication, especially including
	 * Swagger-generated documentation.
	 */
	public static final String[] OPEN_PATHS = { //
			"/v2/api-docs", //
			"/swagger-resources/**", //
			"/swagger-ui.html", //
			"/webjars/**", //
			PortalApiConstants.API_PREFIX + "/**", //
			A1Controller.CONTROLLER_PATH + "/" + A1Controller.VERSION_METHOD, //					
			SimpleErrorController.ERROR_PATH };

	@Override
	public void configure(WebSecurity web) throws Exception {
		// This disables Spring security, but not the app's filter.
		web.ignoring().antMatchers(OPEN_PATHS);
	}

	@Bean
	public PortalAuthManager portalAuthManagerBean()
			throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		return new PortalAuthManager(appName, userName, password, decryptor, userCookie);
	}

	/*
	 * If this is annotated with @Bean, it is created automatically AND REGISTERED,
	 * and Spring processes annotations in the source of the class. However, the
	 * filter is added in the chain apparently in the wrong order. Alternately, with
	 * no @Bean and added to the chain up in the configure() method in the desired
	 * order, the ignoring() matcher pattern configured above causes Spring to
	 * bypass this filter, which seems to me means the filter participates
	 * correctly.
	 */
	public PortalAuthenticationFilter portalAuthenticationFilterBean()
			throws ClassNotFoundException, InstantiationException, IllegalAccessException, IOException,
			IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
		PortalAuthenticationFilter portalAuthenticationFilter = new PortalAuthenticationFilter(portalapiSecurity,
				portalAuthManagerBean(), this.userManager);
		return portalAuthenticationFilter;
	}

}
