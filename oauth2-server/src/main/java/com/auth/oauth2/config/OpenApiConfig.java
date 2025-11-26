package com.auth.oauth2.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Slf4j
@Configuration
public class OpenApiConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    // Create base OpenAPI object
    // Note: springdoc will merge code scan results with this bean
    // If we set paths here, code scan results will be ignored
    // So we only set metadata (info, servers, components) and let springdoc handle paths
    OpenAPI openAPI = new OpenAPI()
        .info(
            new Info()
                .title("Auth Platform API")
                .version("1.0.0")
                .description(
                    "Centralized authentication and authorization platform API. "
                        + "This API provides OAuth2/OIDC-compliant authentication services for multiple side projects."))
        .servers(
            List.of(
                new Server().url("http://localhost:8081").description("OAuth2 Authorization Server"),
                new Server().url("http://localhost:8082").description("OAuth2 Resource Server")));

    // Load components (schemas, securitySchemes) from YAML
    // Paths will be added via GroupedOpenApi or manual merge
    loadComponentsFromYaml(openAPI);

    return openAPI;
  }

  @Bean
  public GroupedOpenApi yamlApiGroup() {
    // Load paths from YAML and merge them with code scan results
    Paths yamlPaths = loadPathsFromYaml();
    
    if (yamlPaths != null && !yamlPaths.isEmpty()) {
      log.info("Successfully loaded {} paths from openapi.yaml", yamlPaths.size());
      
      return GroupedOpenApi.builder()
          .group("default")
          .addOpenApiCustomizer(openApi -> {
            // Merge YAML paths with code scan results
            if (openApi.getPaths() == null) {
              openApi.setPaths(new Paths());
            }
            yamlPaths.forEach((path, pathItem) -> {
              openApi.getPaths().addPathItem(path, pathItem);
            });
          })
          .build();
    } else {
      log.warn("Failed to load paths from openapi.yaml");
      return GroupedOpenApi.builder()
          .group("default")
          .build();
    }
  }

  private Paths loadPathsFromYaml() {
    try {
      ClassPathResource resource = new ClassPathResource("static/openapi.yaml");
      if (!resource.exists()) {
        log.warn("openapi.yaml file not found in static resources");
        return null;
      }

      ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
      try (InputStream inputStream = resource.getInputStream()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> yamlMap = yamlMapper.readValue(inputStream, Map.class);
        
        // Extract paths from YAML
        @SuppressWarnings("unchecked")
        Map<String, Object> pathsMap = (Map<String, Object>) yamlMap.get("paths");
        if (pathsMap != null) {
          Paths paths = yamlMapper.convertValue(pathsMap, Paths.class);
          log.debug("Loaded {} paths from YAML", paths.size());
          return paths;
        } else {
          log.warn("No paths found in openapi.yaml");
          return null;
        }
      }
    } catch (Exception e) {
      log.error("Failed to load paths from openapi.yaml file", e);
      return null;
    }
  }

  private void loadComponentsFromYaml(OpenAPI openAPI) {
    try {
      ClassPathResource resource = new ClassPathResource("static/openapi.yaml");
      if (!resource.exists()) {
        log.warn("openapi.yaml file not found in static resources");
        return;
      }

      ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
      try (InputStream inputStream = resource.getInputStream()) {
        @SuppressWarnings("unchecked")
        Map<String, Object> yamlMap = yamlMapper.readValue(inputStream, Map.class);
        
        // Extract components from YAML
        @SuppressWarnings("unchecked")
        Map<String, Object> componentsMap = (Map<String, Object>) yamlMap.get("components");
        if (componentsMap != null) {
          io.swagger.v3.oas.models.Components components =
              yamlMapper.convertValue(componentsMap, io.swagger.v3.oas.models.Components.class);
          if (openAPI.getComponents() != null) {
            // Merge components
            if (components.getSchemas() != null) {
              openAPI.getComponents().setSchemas(components.getSchemas());
            }
            if (components.getSecuritySchemes() != null) {
              openAPI.getComponents().setSecuritySchemes(components.getSecuritySchemes());
            }
          } else {
            openAPI.setComponents(components);
          }
          log.info("Successfully loaded components from openapi.yaml");
        }
      }
    } catch (Exception e) {
      log.error("Failed to load components from openapi.yaml file", e);
    }
  }
}

