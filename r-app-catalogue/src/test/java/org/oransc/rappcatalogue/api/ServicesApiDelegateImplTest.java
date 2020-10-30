package org.oransc.rappcatalogue.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.oransc.rappcatalogue.exception.InvalidServiceException;
import org.oransc.rappcatalogue.exception.ServiceNotFoundException;
import org.oransc.rappcatalogue.model.Service;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ServicesApiDelegateImplTest {

    @Test
    void getAddedService_shouldReturnService() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service = new Service();
        service.setDescription("description");
        service.setVersion("1.0");
        service.setDisplayName("Display Name");

        delegateUnderTest.putIndividualService("Service Name", service);

        ResponseEntity<Service> response = delegateUnderTest.getIndividualService("Service Name");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(service);
    }

    @Test
    void getMissingService_shouldThrowException() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Exception exception = assertThrows(ServiceNotFoundException.class, () -> {
            delegateUnderTest.getIndividualService("Service Name");
        });

        String expectedMessage = "Service Service Name not found";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    @Test
    void putNewValidService_shouldBeCreatedAndRegistered() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service = new Service();
        service.setDescription("description");
        service.setVersion("1.0");
        service.setDisplayName("Display Name");

        ResponseEntity<Void> putResponse = delegateUnderTest.putIndividualService("Service Name", service);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        ResponseEntity<Service> getResponse = delegateUnderTest.getIndividualService("Service Name");

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody()).isEqualTo(service);
    }

    @Test
    void putModifiedService_shouldBeModified() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service = new Service();
        service.setDescription("description");
        service.setVersion("1.0");
        service.setDisplayName("Display Name");

        delegateUnderTest.putIndividualService("Service Name", service);

        service.setDescription("New description");
        ResponseEntity<Void> putResponse = delegateUnderTest.putIndividualService("Service Name", service);

        assertThat(putResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

        ResponseEntity<Service> getResponse = delegateUnderTest.getIndividualService("Service Name");

        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().getDescription()).isEqualTo("New description");
    }

    @Test
    void putServiceWithVersionNull_shouldThrowException() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service = new Service();
        service.setDescription("description");
        service.setDisplayName("Display Name");

        Exception exception = assertThrows(InvalidServiceException.class, () -> {
            delegateUnderTest.putIndividualService("Service Name", service);
        });

        String expectedMessage = "Service is missing required property \"version\"";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    @Test
    void putServiceWithBlankVersion_shouldThrowException() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service = new Service();
        service.setVersion("");
        service.setDescription("description");
        service.setDisplayName("Display Name");

        Exception exception = assertThrows(InvalidServiceException.class, () -> {
            delegateUnderTest.putIndividualService("Service Name", service);
        });

        String expectedMessage = "Service is missing required property \"version\"";
        String actualMessage = exception.getMessage();

        assertThat(actualMessage).isEqualTo(expectedMessage);
    }

    @Test
    void getServices_shouldProvideArrayOfAddedServiceNames() throws Exception {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service1 = new Service();
        service1.setDescription("description 1");
        service1.setVersion("1.0");
        service1.setDisplayName("Display Name 1");

        Service service2 = new Service();
        service2.setDescription("description 2");
        service2.setVersion("1.0");
        service2.setDisplayName("Display Name 2");

        delegateUnderTest.putIndividualService("Service Name 1", service1);
        delegateUnderTest.putIndividualService("Service Name 2", service2);

        ResponseEntity<List<String>> response = delegateUnderTest.getServiceNames();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Service Name 1", "Service Name 2");
    }

    @Test
    void deleteService_shouldBeOk() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        Service service = new Service();
        service.setDescription("description");
        service.setVersion("1.0");
        service.setDisplayName("Display Name");

        delegateUnderTest.putIndividualService("Service Name", service);

        ResponseEntity<List<String>> servicesResponse = delegateUnderTest.getServiceNames();

        assertThat(servicesResponse.getBody()).hasSize(1);

        ResponseEntity<Void> deleteResponse = delegateUnderTest.deleteIndividualService("Service Name");

        assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        servicesResponse = delegateUnderTest.getServiceNames();

        assertThat(servicesResponse.getBody()).isEmpty();
}
}
