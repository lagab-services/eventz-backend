package com.lagab.eventz.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import lombok.Getter;
import lombok.Setter;

@Getter
@ConfigurationProperties(prefix = "common")
public class CommonProperties {
    private final CorsConfiguration cors = new CorsConfiguration();
    private final Swagger swagger = new Swagger();

    @Getter
    @Setter
    public static class Swagger {

        private String title = "API";
        private String description = "API documentation";
        private String version = "0.0.1";
        private String termsOfServiceUrl;
        private String contactName;
        private String contactUrl;
        private String contactEmail;
        private String license;
        private String licenseUrl;
    }
}
