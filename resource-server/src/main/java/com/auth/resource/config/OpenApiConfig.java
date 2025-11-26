package com.auth.resource.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Auth Platform Resource Server API")
                .version("1.0.0")
                .description(
                    "OAuth2 Resource Server API. "
                        + "Provides protected resources and user information endpoints."))
        .servers(
            List.of(
                new Server().url("http://localhost:8082").description("OAuth2 Resource Server")));
  }
}

