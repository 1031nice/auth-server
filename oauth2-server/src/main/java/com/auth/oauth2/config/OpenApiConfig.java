package com.auth.oauth2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    // Try to load from openapi.yaml file first
    OpenAPI openAPI = loadOpenAPIFromYaml();
    
    // If YAML loading failed, create a basic OpenAPI object
    if (openAPI == null) {
      openAPI = new OpenAPI();
      log.warn("Failed to load openapi.yaml, using code-scan only");
    }
    
    return openAPI;
  }

  private OpenAPI loadOpenAPIFromYaml() {
    try {
      ClassPathResource resource = new ClassPathResource("static/openapi.yaml");
      if (!resource.exists()) {
        log.warn("openapi.yaml file not found in static resources");
        return null;
      }

      try (InputStream inputStream = resource.getInputStream()) {
        OpenAPIV3Parser parser = new OpenAPIV3Parser();
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setFlatten(true);
        
        SwaggerParseResult parseResult = parser.readContents(
            new String(inputStream.readAllBytes()), null, options);
        
        if (parseResult.getMessages() != null && !parseResult.getMessages().isEmpty()) {
          log.warn("OpenAPI parsing warnings: {}", parseResult.getMessages());
        }
        
        OpenAPI openAPI = parseResult.getOpenAPI();
        if (openAPI != null) {
          log.info("Successfully loaded OpenAPI specification from openapi.yaml");
          return openAPI;
        } else {
          log.error("Failed to parse openapi.yaml: {}", parseResult.getMessages());
          return null;
        }
      }
    } catch (Exception e) {
      log.error("Failed to load openapi.yaml file", e);
      return null;
    }
  }
}

