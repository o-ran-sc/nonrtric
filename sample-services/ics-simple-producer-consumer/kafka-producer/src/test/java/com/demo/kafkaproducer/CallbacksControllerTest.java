/*-
 * ========================LICENSE_START=================================
 * O-RAN-SC
 *
 * Copyright (C) 2024: OpenInfra Foundation Europe
 *
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
package com.demo.kafkaproducer;

import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.demo.kafkaproducer.controller.CallbacksController;
import com.demo.kafkaproducer.controller.KafkaController;

import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CallbacksControllerTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private KafkaController kafkaController;


    @InjectMocks
    private CallbacksController callbacksController;

    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testGetHealthCheck() {
        ResponseEntity<String> response = callbacksController.getHealthCheck();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Ok");
    }

    @Test
    public void testStartJob() {
        String requestBody = "testJob";
        when(kafkaController.postMessageMono(requestBody)).thenReturn(Mono.just("Message Published Successfully"));

        ResponseEntity<String> response = callbacksController.startJob(requestBody);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Ok");
        verify(kafkaController, times(1)).postMessageMono(requestBody);
    }

    @Test
    public void testActuatorStopJob() {
        String shutdownUrl = "http://localhost:8080/actuator/shutdown";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(null, headers);

        ResponseEntity<String> restTemplateResponse = new ResponseEntity<>("Shutdown successful", HttpStatus.OK);
        when(restTemplate.postForEntity(shutdownUrl, entity, String.class)).thenReturn(restTemplateResponse);

    }
}
