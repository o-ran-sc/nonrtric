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

package org.oransc.policyagent.tasks;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import ch.qos.logback.classic.Level;
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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.api.CbsClient;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.EnvProperties;
import org.onap.dcaegen2.services.sdk.rest.services.cbs.client.model.ImmutableEnvProperties;
import org.oransc.policyagent.configuration.ApplicationConfig;
import org.oransc.policyagent.configuration.ApplicationConfigParser;
import org.oransc.policyagent.configuration.ImmutableRicConfig;
import org.oransc.policyagent.configuration.RicConfig;
import org.oransc.policyagent.utils.LoggingUtils;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@ExtendWith(MockitoExtension.class)
public class RefreshConfigTaskTest {

    private RefreshConfigTask refreshTaskUnderTest;

    @Spy
    ApplicationConfig appConfig;

    @Mock
    CbsClient cbsClient;

    private static final String RIC_1_NAME = "ric1";
    public static final ImmutableRicConfig CORRECT_RIC_CONIFG = ImmutableRicConfig.builder() //
        .name(RIC_1_NAME) //
        .baseUrl("http://localhost:8080/") //
        .managedElementIds(new Vector<String>(Arrays.asList("kista_1", "kista_2"))) //
        .build();

    private static EnvProperties properties() {
        return ImmutableEnvProperties.builder() //
            .consulHost("host") //
            .consulPort(123) //
            .cbsName("cbsName") //
            .appName("appName") //
            .build();
    }

    @Test
    public void whenTheConfigurationFits_thenConfiguredRicsArePutInRepository() throws Exception {
        refreshTaskUnderTest = spy(new RefreshConfigTask(appConfig));
        refreshTaskUnderTest.systemEnvironment = new Properties();
        // When
        doReturn(getCorrectJson()).when(refreshTaskUnderTest).createInputStream(any());
        doReturn("fileName").when(appConfig).getLocalConfigurationFilePath();
        refreshTaskUnderTest.start();

        // Then
        verify(refreshTaskUnderTest, times(1)).loadConfigurationFromFile();

        Iterable<RicConfig> ricConfigs = appConfig.getRicConfigs();
        RicConfig ricConfig = ricConfigs.iterator().next();
        assertThat(ricConfigs).isNotNull();
        assertThat(ricConfig).isEqualTo(CORRECT_RIC_CONIFG);
    }

    @Test
    public void whenFileExistsButJsonIsIncorrect_thenNoRicsArePutInRepository() throws Exception {
        refreshTaskUnderTest = spy(new RefreshConfigTask(appConfig));
        refreshTaskUnderTest.systemEnvironment = new Properties();

        // When
        doReturn(getIncorrectJson()).when(refreshTaskUnderTest).createInputStream(any());
        doReturn("fileName").when(appConfig).getLocalConfigurationFilePath();
        refreshTaskUnderTest.loadConfigurationFromFile();

        // Then
        verify(refreshTaskUnderTest, times(1)).loadConfigurationFromFile();
        assertThat(appConfig.getRicConfigs().size()).isEqualTo(0);
    }

    @Test
    public void whenPeriodicConfigRefreshNoEnvironmentVariables_thenErrorIsLogged() {
        refreshTaskUnderTest = spy(new RefreshConfigTask(appConfig));
        refreshTaskUnderTest.systemEnvironment = new Properties();

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(RefreshConfigTask.class);
        Flux<ApplicationConfig> task = refreshTaskUnderTest.createRefreshTask();

        StepVerifier.create(task).expectSubscription().verifyComplete();

        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logAppender.list.toString().contains("$CONSUL_HOST environment has not been defined")).isTrue();
    }

    @Test
    public void whenPeriodicConfigRefreshNoConsul_thenErrorIsLogged() {
        refreshTaskUnderTest = spy(new RefreshConfigTask(appConfig));
        refreshTaskUnderTest.systemEnvironment = new Properties();

        EnvProperties props = properties();
        doReturn(Mono.just(props)).when(refreshTaskUnderTest).getEnvironment(any());

        doReturn(Mono.just(cbsClient)).when(refreshTaskUnderTest).createCbsClient(props);
        Flux<JsonObject> err = Flux.error(new IOException());
        doReturn(err).when(cbsClient).updates(any(), any(), any());

        final ListAppender<ILoggingEvent> logAppender = LoggingUtils.getLogListAppender(RefreshConfigTask.class);
        Flux<ApplicationConfig> task = refreshTaskUnderTest.createRefreshTask();

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .verifyComplete();

        assertThat(logAppender.list.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(
            logAppender.list.toString().contains("Could not refresh application configuration. java.io.IOException"))
                .isTrue();
    }

    @Test
    public void whenPeriodicConfigRefreshSuccess_thenNewConfigIsCreated() throws Exception {
        refreshTaskUnderTest = spy(new RefreshConfigTask(appConfig));
        refreshTaskUnderTest.systemEnvironment = new Properties();

        EnvProperties props = properties();
        doReturn(Mono.just(props)).when(refreshTaskUnderTest).getEnvironment(any());
        doReturn(Mono.just(cbsClient)).when(refreshTaskUnderTest).createCbsClient(props);

        JsonObject configAsJson = getJsonRootObject();
        String newBaseUrl = "newBaseUrl";
        modifyTheRicConfiguration(configAsJson, newBaseUrl);
        Flux<JsonObject> json = Flux.just(configAsJson);
        doReturn(json).when(cbsClient).updates(any(), any(), any());

        Flux<ApplicationConfig> task = refreshTaskUnderTest.createRefreshTask();

        StepVerifier //
            .create(task) //
            .expectSubscription() //
            .expectNext(appConfig) //
            .verifyComplete();

        assertThat(appConfig.getRicConfigs()).isNotNull();
        assertThat(appConfig.getRic(RIC_1_NAME).baseUrl()).isEqualTo(newBaseUrl);
    }

    private void modifyTheRicConfiguration(JsonObject configAsJson, String newBaseUrl) {
        ((JsonObject) configAsJson.getAsJsonObject("config").getAsJsonArray("ric").get(0)).addProperty("baseUrl",
            newBaseUrl);
    }

    private JsonObject getJsonRootObject() throws JsonIOException, JsonSyntaxException, IOException {
        JsonObject rootObject = JsonParser.parseReader(new InputStreamReader(getCorrectJson())).getAsJsonObject();
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
