package com.webapp.backend.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;

/**
 * OpenAPI Configuration
 * 
 * Configures Swagger/OpenAPI documentation for the REST API.
 * Access documentation at: /api/swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8080}")
    private int serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        Server localServer = new Server();
        localServer.setUrl("http://localhost:" + serverPort + "/api");
        localServer.setDescription("Local development server");

        Contact contact = new Contact();
        contact.setName("Personal Web Application");

        License license = new License();
        license.setName("MIT License");
        license.setUrl("https://opensource.org/licenses/MIT");

        Info info = new Info()
                .title("Personal Web Application API")
                .version("1.0.0")
                .description("REST API for managing games, posts, comments, and user profiles")
                .contact(contact)
                .license(license);

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer));
    }
}
