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

import java.lang.invoke.MethodHandles;

import org.oransc.ric.portal.dashboard.DashboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@EnableWebSecurity
@EnableGlobalMethodSecurity(securedEnabled = true)
@Profile("test")
public class WebSecurityMockConfiguration extends WebSecurityConfigurerAdapter {

	public static final String TEST_CRED_ADMIN = "admin";
	public static final String TEST_CRED_STANDARD = "standard";

	private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

	public WebSecurityMockConfiguration(@Value("${userfile}") final String userFilePath) {
		logger.debug("ctor: user file path {}", userFilePath);
	}

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		PasswordEncoder encoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
		auth.inMemoryAuthentication() //
				.passwordEncoder(encoder) //
				// The admin user has the admin AND standard roles
				.withUser(TEST_CRED_ADMIN) //
				.password(encoder.encode(TEST_CRED_ADMIN))
				.roles(DashboardConstants.ROLE_NAME_ADMIN, DashboardConstants.ROLE_NAME_STANDARD)//
				.and()//
				// The standard user has only the standard role
				.withUser(TEST_CRED_STANDARD) //
				.password(encoder.encode(TEST_CRED_STANDARD)) //
				.roles(DashboardConstants.ROLE_NAME_STANDARD);
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests().anyRequest().authenticated()//
				.and().httpBasic() //
				.and().csrf().disable();
	}

	@Override
	public void configure(WebSecurity web) throws Exception {
		// This disables Spring security, but not the app's filter.
		web.ignoring().antMatchers(WebSecurityConfiguration.OPEN_PATHS);
		web.ignoring().antMatchers("/", "/csrf"); // allow swagger-ui to load
	}

}
