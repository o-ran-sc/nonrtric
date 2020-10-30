package org.oransc.rappcatalogue.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.CREATED;
import static org.springframework.http.HttpStatus.NO_CONTENT;
import static org.springframework.http.HttpStatus.OK;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.oransc.rappcatalogue.exception.InvalidServiceException;
import org.oransc.rappcatalogue.exception.ServiceNotFoundException;
import org.oransc.rappcatalogue.model.InputService;
import org.oransc.rappcatalogue.model.Service;
import org.springframework.http.ResponseEntity;

class ServicesApiDelegateImplTest {

    private static final String INVALID_SERVICE_MESSAGE = "Service is missing required property: version";
    private static final String SERVICE_NAME = "Service Name";
    private static final String SERVICE_DESCRIPTION = "description";
    private static final String SERVICE_VERSION = "1.0";
    private static final String SERVICE_DISPLAY_NAME = "Display Name";

    @Test
    void getAddedService_shouldReturnService() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service = new InputService();
        service.setDescription(SERVICE_DESCRIPTION);
        service.setVersion(SERVICE_VERSION);
        service.setDisplayName(SERVICE_DISPLAY_NAME);

        delegateUnderTest.putIndividualService(SERVICE_NAME, service);

        ResponseEntity<Service> response = delegateUnderTest.getIndividualService(SERVICE_NAME);

        assertThat(response.getStatusCode()).isEqualTo(OK);
        assertThat(response.getBody().getName()).isEqualTo(SERVICE_NAME);
    }

    @Test
    void getMissingService_shouldThrowException() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Exception exception = assertThrows(ServiceNotFoundException.class, () -> {
            delegateUnderTest.getIndividualService(SERVICE_NAME);
        });

        String expectedMessage = "Service " + SERVICE_NAME + " not found";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    @Test
    void putNewValidService_shouldBeCreatedAndRegistered() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service = new InputService();
        service.setDescription(SERVICE_DESCRIPTION);
        service.setVersion(SERVICE_VERSION);
        service.setDisplayName(SERVICE_DISPLAY_NAME);

        ResponseEntity<Void> putResponse = delegateUnderTest.putIndividualService(SERVICE_NAME, service);

        assertThat(putResponse.getStatusCode()).isEqualTo(CREATED);

        ResponseEntity<Service> getResponse = delegateUnderTest.getIndividualService(SERVICE_NAME);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK);
        assertThat(getResponse.getBody().getName()).isEqualTo(SERVICE_NAME);
    }

    @Test
    void putModifiedService_shouldBeModified() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service = new InputService();
        service.setDescription(SERVICE_DESCRIPTION);
        service.setVersion(SERVICE_VERSION);
        service.setDisplayName(SERVICE_DISPLAY_NAME);

        delegateUnderTest.putIndividualService(SERVICE_NAME, service);

        String newDescription = "New description";
        service.setDescription(newDescription);
        ResponseEntity<Void> putResponse = delegateUnderTest.putIndividualService(SERVICE_NAME, service);

        assertThat(putResponse.getStatusCode()).isEqualTo(OK);

        ResponseEntity<Service> getResponse = delegateUnderTest.getIndividualService(SERVICE_NAME);

        assertThat(getResponse.getStatusCode()).isEqualTo(OK);
        assertThat(getResponse.getBody().getDescription()).isEqualTo(newDescription);
    }

    @Test
    void putServiceWithVersionNull_shouldThrowException() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service = new InputService();
        service.setDescription(SERVICE_DESCRIPTION);
        service.setDisplayName(SERVICE_DISPLAY_NAME);

        Exception exception = assertThrows(InvalidServiceException.class, () -> {
            delegateUnderTest.putIndividualService(SERVICE_NAME, service);
        });

        assertThat(exception.getMessage()).isEqualTo(INVALID_SERVICE_MESSAGE);
    }

    @Test
    void putServiceWithBlankVersion_shouldThrowException() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service = new InputService();
        service.setVersion("");
        service.setDescription(SERVICE_DESCRIPTION);
        service.setDisplayName(SERVICE_DISPLAY_NAME);

        Exception exception = assertThrows(InvalidServiceException.class, () -> {
            delegateUnderTest.putIndividualService(SERVICE_NAME, service);
        });

        assertThat(exception.getMessage()).isEqualTo(INVALID_SERVICE_MESSAGE);
    }

    @Test
    void getServices_shouldProvideArrayOfAddedServiceNames() throws Exception {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service1 = new InputService();
        service1.setDescription("description 1");
        service1.setVersion(SERVICE_VERSION);
        service1.setDisplayName("Display Name 1");

        InputService service2 = new InputService();
        service2.setDescription("description 2");
        service2.setVersion(SERVICE_VERSION);
        service2.setDisplayName("Display Name 2");

        String serviceName1 = "Service Name 1";
        delegateUnderTest.putIndividualService(serviceName1, service1);
        String serviceName2 = "Service Name 2";
        delegateUnderTest.putIndividualService(serviceName2, service2);

        ResponseEntity<List<Service>> response = delegateUnderTest.getServices();

        assertThat(response.getStatusCode()).isEqualTo(OK);
        List<Service> services = response.getBody();
        assertThat(services).hasSize(2);
        List<String> expectedServiceNames = Arrays.asList(serviceName1, serviceName2);
        assertThat(expectedServiceNames).contains(services.get(0).getName()) //
                                        .contains(services.get(1).getName());
    }

    @Test
    void deleteService_shouldBeOk() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        InputService service = new InputService();
        service.setDescription(SERVICE_DESCRIPTION);
        service.setVersion(SERVICE_VERSION);
        service.setDisplayName(SERVICE_DISPLAY_NAME);

        delegateUnderTest.putIndividualService(SERVICE_NAME, service);

        ResponseEntity<List<Service>> servicesResponse = delegateUnderTest.getServices();

        assertThat(servicesResponse.getBody()).hasSize(1);

        ResponseEntity<Void> deleteResponse = delegateUnderTest.deleteIndividualService(SERVICE_NAME);

        assertThat(deleteResponse.getStatusCode()).isEqualTo(NO_CONTENT);

        servicesResponse = delegateUnderTest.getServices();

        assertThat(servicesResponse.getBody()).isEmpty();
    }
}
