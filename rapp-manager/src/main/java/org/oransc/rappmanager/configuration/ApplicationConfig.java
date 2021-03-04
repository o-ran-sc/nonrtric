package org.oransc.rappmanager.configuration;

import javax.validation.constraints.NotEmpty;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties()
@EnableConfigurationProperties
public class ApplicationConfig {

    @NotEmpty
    @Getter
    @Value("${app.certpath}")
    private String certPath;
    
    @NotEmpty
    @Getter
    @Value("${app.tokenpath}")
    private String tokenPath;
    
    @NotEmpty
    @Getter
    @Value("${app.basepath}")
    private String basePath;

}
