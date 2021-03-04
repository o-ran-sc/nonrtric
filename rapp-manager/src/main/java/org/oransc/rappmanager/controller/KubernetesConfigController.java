package org.oransc.rappmanager.controller;

import io.kubernetes.client.openapi.ApiClient;
import io.kubernetes.client.openapi.apis.CoreV1Api;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.oransc.rappmanager.configuration.ApplicationConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController("KubernetesConfigController")
@RequestMapping("rms")
@Api(tags = {"kubeconfig"})
public class KubernetesConfigController {

    @Autowired
    private ApplicationConfig appConfig;

    @Autowired
    ResourceLoader resourceLoader;

    private ApiClient client;

    private CoreV1Api api;

    private static final Logger logger = LoggerFactory.getLogger(KubernetesConfigController.class);

    @GetMapping(path = "/config", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Return all Connected Kubernetes Cluster")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Kubernetes Cluster List")})
    public ResponseEntity<Object> getAllClusters() {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }

    @GetMapping(path = "/config/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ApiOperation(value = "Return a Kubernetes Cluster")
    @ApiResponses(value = {@ApiResponse(code = 200, message = "Kubernetes Cluster")})
    public ResponseEntity<Object> getCluster(@PathVariable("id") String clusterId) {
        return new ResponseEntity<>(null, HttpStatus.OK);
    }
}
