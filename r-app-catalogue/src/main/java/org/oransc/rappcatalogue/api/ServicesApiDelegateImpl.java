package org.oransc.rappcatalogue.api;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@org.springframework.stereotype.Service
public class ServicesApiDelegateImpl implements ServicesApiDelegate {

    @Override
    public ResponseEntity<Void> deleteIndividualServiceUsingDELETE(String serviceName) {
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

    // @Override
    // public ResponseEntity<Service> getIndividualServiceUsingGET(String serviceName) {
    //     return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    // }

    @Override
    public ResponseEntity<List<String>> getServiceNamesUsingGET() {
        List<String> services = Arrays.asList("a", "b");
        return ResponseEntity.ok(services);
    }

    // @Override
    // public ResponseEntity<Void> putIndividualServiceUsingPUT(String serviceName, Service service) {
    //     return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);

    // }
}
