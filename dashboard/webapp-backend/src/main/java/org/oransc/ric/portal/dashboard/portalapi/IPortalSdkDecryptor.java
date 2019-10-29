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

import org.onap.portalsdk.core.onboarding.exception.CipherUtilException;

/**
 * Supports an upgrade path among methods in CipherUtil because the PortalSDK is
 * changing encryption methods.
 */
public interface IPortalSdkDecryptor {

	/**
	 * Decrypts the specified value using a known key.
	 * 
	 * @param cipherText
	 *                       Encrypted value
	 * @return Clear text on success, null otherwise.
	 * @throws CipherUtilException
	 *                                 if any decryption step fails
	 */
	String decrypt(String cipherText) throws CipherUtilException;

}
