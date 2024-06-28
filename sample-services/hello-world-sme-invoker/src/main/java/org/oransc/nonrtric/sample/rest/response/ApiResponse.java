/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2024 OpenInfra Foundation Europe.
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

package org.oransc.nonrtric.sample.rest.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class ApiResponse {

    @JsonProperty("serviceAPIDescriptions")
    private List<ServiceAPIDescription> serviceAPIDescriptions;

    public List<ServiceAPIDescription> getServiceAPIDescriptions() {
        return serviceAPIDescriptions;
    }

    public void setServiceAPIDescriptions(List<ServiceAPIDescription> serviceAPIDescriptions) {
        this.serviceAPIDescriptions = serviceAPIDescriptions;
    }

    public static class ServiceAPIDescription {
        @JsonProperty("apiName")
        private String apiName;

        @JsonProperty("apiId")
        private String apiId;

        @JsonProperty("description")
        private String description;

        @JsonProperty("aefProfiles")
        private List<AefProfile> aefProfiles;

        public String getApiName() {
            return apiName;
        }

        public void setApiName(String apiName) {
            this.apiName = apiName;
        }

        public String getApiId() {
            return apiId;
        }

        public void setApiId(String apiId) {
            this.apiId = apiId;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public List<AefProfile> getAefProfiles() {
            return aefProfiles;
        }

        public void setAefProfiles(List<AefProfile> aefProfiles) {
            this.aefProfiles = aefProfiles;
        }
    }

    public static class AefProfile {
        private String aefId;
        private String domainName;
        private List<ApiVersion> versions;
        private String protocol;
        private List<InterfaceDescription> interfaceDescriptions;

        public String getAefId() {
            return aefId;
        }

        public String getDomainName() {
            return domainName;
        }

        public void setDomainName(String domainName) {
            this.domainName = domainName;
        }

        public void setAefId(String aefId) {
            this.aefId = aefId;
        }

        public List<ApiVersion> getVersions() {
            return versions;
        }

        public void setVersions(List<ApiVersion> versions) {
            this.versions = versions;
        }

        public String getProtocol() {
            return protocol;
        }

        public void setProtocol(String protocol) {
            this.protocol = protocol;
        }

        public List<InterfaceDescription> getInterfaceDescriptions() {
            return interfaceDescriptions;
        }

        public void setInterfaceDescriptions(
            List<InterfaceDescription> interfaceDescriptions) {
            this.interfaceDescriptions = interfaceDescriptions;
        }
    }

    public static class ApiVersion {
        private String apiVersion;
        private List<Resource> resources;

        public String getApiVersion() {
            return apiVersion;
        }

        public void setApiVersion(String apiVersion) {
            this.apiVersion = apiVersion;
        }

        public List<Resource> getResources() {
            return resources;
        }

        public void setResources(List<Resource> resources) {
            this.resources = resources;
        }
    }

    public static class Resource {
        private String resourceName;
        private String commType;
        private String uri;
        private List<String> operations;

        public String getResourceName() {
            return resourceName;
        }

        public void setResourceName(String resourceName) {
            this.resourceName = resourceName;
        }

        public String getCommType() {
            return commType;
        }

        public void setCommType(String commType) {
            this.commType = commType;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public List<String> getOperations() {
            return operations;
        }

        public void setOperations(List<String> operations) {
            this.operations = operations;
        }
    }

    public static class InterfaceDescription {
        private String ipv4Addr;
        private int port;

        public String getIpv4Addr() {
            return ipv4Addr;
        }

        public void setIpv4Addr(String ipv4Addr) {
            this.ipv4Addr = ipv4Addr;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }
}

