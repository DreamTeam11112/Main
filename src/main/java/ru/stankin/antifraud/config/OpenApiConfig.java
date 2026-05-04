package ru.stankin.antifraud.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI antifraudOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Antifraud REST API")
                        .description("API для проверки транзакций, управления антифрод-правилами и просмотра решений.")
                        .version("v1")
                        .contact(new Contact().name("Stankin Antifraud Backend"))
                        .license(new License().name("Internal Use")));
    }
}
