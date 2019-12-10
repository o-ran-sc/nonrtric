/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 * %%
 * Copyright (C) 2019 Nordix Foundation
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
package org.oransc.policyagent.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.gson.JsonIOException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Properties;
import java.util.Vector;

import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableEnvProperties;
import org.oransc.policyagent.exceptions.ServiceException;
import org.oransc.policyagent.utils.LoggingUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

public class ApplicationConfigTest {

    private ApplicationConfig appConfigUnderTest;
    CbsClient cbsClient = mock(CbsClient.class);

    public static final ImmutableRicConfig CORRECT_RIC_CONIFG =
        ImmutableRicConfig.builder().name("ric1").baseUrl("http://localhost:8080/")
            .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))).build();

    private static EnvProperties properties() {
        return ImmutableEnvProperties.builder() //
            .consulHost("host") //
            .consulPort(123) //
            .cbsName("cbsName") //
            .appName("appName") //
            .build();
    }

    @Test
    public void whenTheConfigurationFits() throws IOException, ServiceException {

        appConfigUnderTest = spy(ApplicationConfig.class);
        appConfigUnderTest.systemEnvironment = new Properties();
        // When
        doReturn(getCorrectJson()).when(appConfigUnderTest).createInputStream(any());
        appConfigUnderTest.initialize();

        // Then
        verify(appConfigUnderTest, times(1)).loadConfigurationFromFile(any());

        Vector<RicConfig> ricConfigs = appConfigUnderTest.getRicConfigs();
        RicConfig ricConfig = ricConfigs.firstElement();
        assertThat(ricConfigs).isNotNull();
        assertThat(ricConfig).isEqualTo(CORRECT_RIC_CONIFG);
    }

    @Test
    public void whenFileIsExistsButJsonIsIncorrect() throws IOException, ServiceException {

        appConfigUnderTest = spy(ApplicationConfig.class);
        appConfigUnderTest.systemEnvironment = new Properties();

        // When
        doReturn(getIncorrectJson()).when(appConfigUnderTest).createInputStream(any());
        appConfigUnderTest.loadConfigurationFromFile(any());

        // Then
        verify(appConfigUnderTest, times(1)).loadConfigurationFromFile(any());
        Assertions.assertNull(appConfigUnderTest.getRicConfigs());
    }

    @Test
    public void whenPeriodicConfigRefreshNoEnvironmentVariables() {

        appConfigUnderTest = spy(ApplicationConfig.class);
        appConfigUnderTest.systemEnvironment = new Properties();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ApplicationConfig.class);
        Flux<ApplicationConfig> task = appConfigUnderTest.createRefreshTask();

        StepVerifier.create(task).expectSubscription().verifyComplete();

        assertTrue(logAppender.list.toString().contains("$CONSUL_HOST environment has not been defined"));
    }

    @Test
    public void whenPeriodicConfigRefreshNoConsul() {
        appConfigUnderTest = spy(ApplicationConfig.class);
        appConfigUnderTest.systemEnvironment = new Properties();

        EnvProperties props = properties();
        doReturn(Mono.just(props)).when(appConfigUnderTest).getEnvironment(any());

        doReturn(Mono.just(cbsClient)).when(appConfigUnderTest).createCbsClient(props);
        Flux<JsonObject> err = Flux.error(new IOException());
        doReturn(err).when(cbsClient).updates(any(), any(), any());

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(ApplicationConfig.class);
        Flux<ApplicationConfig> task = appConfigUnderTest.createRefreshTask();

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .verifyComplete();

        assertTrue(
            logAppender.list.toString().contains("Could not refresh application configuration java.io.IOException"));
    }

    @Test
    public void whenPeriodicConfigRefreshSuccess() throws JsonIOException, JsonSyntaxException, IOException {
        appConfigUnderTest = spy(ApplicationConfig.class);
        appConfigUnderTest.systemEnvironment = new Properties();

        EnvProperties props = properties();
        doReturn(Mono.just(props)).when(appConfigUnderTest).getEnvironment(any());
        doReturn(Mono.just(cbsClient)).when(appConfigUnderTest).createCbsClient(props);

        Flux<JsonObject> json = Flux.just(getJsonRootObject());
        doReturn(json).when(cbsClient).updates(any(), any(), any());

        Flux<ApplicationConfig> task = appConfigUnderTest.createRefreshTask();

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .expectNext(appConfigUnderTest) //
            .verifyComplete();

        Assertions.assertNotNull(appConfigUnderTest.getRicConfigs());
    }

    private JsonObject getJsonRootObject() throws JsonIOException, JsonSyntaxException, IOException {
        JsonObject rootObject = (new JsonParser()).parse(new InputStreamReader(getCorrectJson())).getAsJsonObject();
        return rootObject;
    }

    private static InputStream getCorrectJson() throws IOException {
        URL url = ApplicationConfigParser.class.getClassLoader().getResource("test_application_configuration.json");
        String string = Resources.toString(url, Charsets.UTF_8);
        return new ByteArrayInputStream((string.getBytes(StandardCharsets.UTF_8)));
    }

    private static InputStream getIncorrectJson() {
        String string = "{" + //
            "    \"config\": {" + //
            "        \"ric\": {"; //
        return new ByteArrayInputStream((string.getBytes(StandardCharsets.UTF_8)));
    }

}
