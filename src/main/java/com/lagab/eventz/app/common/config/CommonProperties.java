package com.lagab.eventz.app.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.web.cors.CorsConfiguration;

import lombok.Getter;

@Getter
@ConfigurationProperties(prefix = "common")
public class CommonProperties {
    private final CorsConfiguration cors = new CorsConfiguration();
}
