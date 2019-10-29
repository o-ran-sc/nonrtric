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

package org.oransc.ric.portal.dashboard.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Disables and enables certificate and host-name checking in
 * HttpsURLConnection, the default JVM implementation of the HTTPS/TLS protocol.
 * Has no effect on implementations such as Apache Http Client, Ok Http.
 * 
 * https://stackoverflow.com/questions/23504819/how-to-disable-ssl-certificate-checking-with-spring-resttemplate/58291331#58291331
 */
public final class HttpsURLConnectionUtils {

	private static final HostnameVerifier jvmHostnameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();

	private static final HostnameVerifier trivialHostnameVerifier = new HostnameVerifier() {
		public boolean verify(String hostname, SSLSession sslSession) {
			return true;
		}
	};

	private static final TrustManager[] UNQUESTIONING_TRUST_MANAGER = new TrustManager[] { new X509TrustManager() {
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			return null;
		}

		public void checkClientTrusted(X509Certificate[] certs, String authType) {
		}

		public void checkServerTrusted(X509Certificate[] certs, String authType) {
		}
	} };

	public static void turnOffSslChecking() throws NoSuchAlgorithmException, KeyManagementException {
		HttpsURLConnection.setDefaultHostnameVerifier(trivialHostnameVerifier);
		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, UNQUESTIONING_TRUST_MANAGER, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	public static void turnOnSslChecking() throws KeyManagementException, NoSuchAlgorithmException {
		HttpsURLConnection.setDefaultHostnameVerifier(jvmHostnameVerifier);
		// Return it to the initial state (discovered by reflection, now hardcoded)
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, null, null);
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
	}

	private HttpsURLConnectionUtils() {
		throw new UnsupportedOperationException("Do not instantiate libraries.");
	}
}
