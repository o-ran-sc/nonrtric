/*
 * ============LICENSE_START=======================================================
 * ONAP : SDNC
 * ================================================================================
 * Copyright 2019 AMDOCS
 *=================================================================================
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ============LICENSE_END=========================================================
 */
package org.onap.sdnc.oam.datamigrator.common;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.onap.sdnc.oam.datamigrator.exceptions.RestconfException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.util.Base64;

public class RestconfClient {

    private HttpURLConnection httpConn = null;
    private final String host ;
    private final String user ;
    private final String password ;
    private static final String CONFIG_PATH = "/restconf/config/";
    private static final String CONTENT_TYPE_JSON = "application/json";
    private final Logger log = LoggerFactory.getLogger(RestconfClient.class);

    public RestconfClient (String host , String user , String password){
        this.host = host;
        this.user = user;
        this.password = password;
    }

    private class SdncAuthenticator extends Authenticator {

        private final String user;
        private final String passwd;

        SdncAuthenticator(String user, String passwd) {
            this.user = user;
            this.passwd = passwd;
        }
        @Override
        protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication(user, passwd.toCharArray());
        }
    }

    public JsonObject get(String path) throws RestconfException {
            String getResponse = send(path,"GET",CONTENT_TYPE_JSON,"");
            JsonParser parser = new JsonParser();
            return parser.parse(getResponse).getAsJsonObject();
    }

    public void put(String path, String data) throws RestconfException {
            send(path,"PUT",CONTENT_TYPE_JSON, data );
    }

    private String send(String path,String method, String contentType, String msg) throws RestconfException {
        Authenticator.setDefault(new SdncAuthenticator(user, password));
        String url = host + CONFIG_PATH + path;
        try {
            URL sdncUrl = new URL(url);
            log.info("SDNC url: " + url);
            log.info("Method: " + method);
            this.httpConn = (HttpURLConnection) sdncUrl.openConnection();
            String authStr = user + ":" + password;
            String encodedAuthStr = new String(Base64.getEncoder().encode(authStr.getBytes()));
            httpConn.addRequestProperty("Authentication", "Basic " + encodedAuthStr);

            httpConn.setRequestMethod(method);
            httpConn.setRequestProperty("Content-Type", contentType);
            httpConn.setRequestProperty("Accept", contentType);

            httpConn.setDoInput(true);
            httpConn.setDoOutput(true);
            httpConn.setUseCaches(false);

            if (httpConn instanceof HttpsURLConnection) {
                HostnameVerifier hostnameVerifier = (hostname, session) -> true;
                ((HttpsURLConnection) httpConn).setHostnameVerifier(hostnameVerifier);
            }
            if (!method.equals("GET")) {
                log.info("Request payload: " + msg);
                httpConn.setRequestProperty("Content-Length", "" + msg.length());
                DataOutputStream outStr = new DataOutputStream(httpConn.getOutputStream());
                outStr.write(msg.getBytes());
                outStr.close();
            }

            BufferedReader respRdr;
            log.info("Response: " + httpConn.getResponseCode() + " " + httpConn.getResponseMessage());

            if (httpConn.getResponseCode() < 300) {
                respRdr = new BufferedReader(new InputStreamReader(httpConn.getInputStream()));
            } else {
                respRdr = new BufferedReader(new InputStreamReader(httpConn.getErrorStream()));
                log.error("Error during restconf operation: "+ method + ". URL:" + sdncUrl.toString()+". Response:"+respRdr);
                throw new RestconfException(httpConn.getResponseCode(),"Error during restconf operation: "+ method +". Response:"+respRdr);
            }

            StringBuilder respBuff = new StringBuilder();
            String respLn;
            while ((respLn = respRdr.readLine()) != null) {
                respBuff.append(respLn).append("\n");
            }
            respRdr.close();
            String respString = respBuff.toString();

            log.info("Response body :\n" + respString);
            return respString;
        }catch (IOException e){
            throw new RestconfException(500,e.getMessage(),e);
        }finally {
            if (httpConn != null) {
                httpConn.disconnect();
            }
        }
    }


}
