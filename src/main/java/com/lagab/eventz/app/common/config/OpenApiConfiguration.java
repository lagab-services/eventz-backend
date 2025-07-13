package com.lagab.eventz.app.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityScheme;
import lombok.RequiredArgsConstructor;

@Configuration
@RequiredArgsConstructor
public class OpenApiConfiguration {

    private final CommonProperties commonProperties;

    @Bean
    @Profile("swagger")
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title(commonProperties.getSwagger().getTitle())
                        .description(commonProperties.getSwagger().getDescription())
                        .version(commonProperties.getSwagger().getVersion())
                        .contact(new Contact()
                                .name(commonProperties.getSwagger().getContactName())
                                .email(commonProperties.getSwagger().getContactEmail())
                                .url(commonProperties.getSwagger().getContactUrl())
                        )
                        .license(new License()
                                .name(commonProperties.getSwagger().getLicense())
                                .url(commonProperties.getSwagger().getLicenseUrl())
                        )
                        .termsOfService(commonProperties.getSwagger().getTermsOfServiceUrl()))
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .in(SecurityScheme.In.HEADER)
                                .name("Authorization")
                        )
                );
    }
}
