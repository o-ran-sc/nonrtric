package org.oransc.rappcatalogue.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
class ServicesApiDelegateImplTest {

    @Test
    void putValidService_shouldBeOk() {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        ResponseEntity<List<String>> response = delegateUnderTest.getServiceNamesUsingGET();
    }

    @Test
    void getServices_shouldProvideArrayOfServices() throws Exception {
        ServicesApiDelegateImpl delegateUnderTest = new ServicesApiDelegateImpl();

        ResponseEntity<List<String>> response = delegateUnderTest.getServiceNamesUsingGET();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(Arrays.asList("a", "b"));
    }
}
