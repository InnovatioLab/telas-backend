package com.telas.infra.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.json.JSONObject;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Configuration
@OpenAPIDefinition
public class SpringDocConfig {
  @Value("classpath:springdoc-responses/responses.json")
  private Resource jsonFile;

  @Value("${swagger.server.url}")
  private String serverUrl;

  @Bean
  public OpenAPI customOpenAPI() throws IOException {
    var key = "default";

    ApiResponse badRequest = new ApiResponse().content(
            new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType().addExamples(key,
                            new Example().value(read("badRequestResponse"))))
    ).description("BAD REQUEST");

    ApiResponse notFound = new ApiResponse().content(
            new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType().addExamples(key,
                            new Example().value(read("notFoundResponse"))))
    ).description("NOT FOUND");

    ApiResponse unauthorized = new ApiResponse().content(
            new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType().addExamples(key,
                            new Example().value(read("unauthorizedResponse"))))
    ).description("UNAUTHORIZED");

    ApiResponse forbidden = new ApiResponse().content(
            new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType().addExamples(key,
                            new Example().value(read("forbiddenResponse"))))
    ).description("FORBIDDEN");

    ApiResponse internalServerError = new ApiResponse().content(
            new Content().addMediaType(MediaType.APPLICATION_JSON_VALUE,
                    new io.swagger.v3.oas.models.media.MediaType().addExamples(key,
                            new Example().value(read("internalServerErrorResponse"))))
    ).description("INTERNAL SERVER ERROR");

    var components = new Components();

    components.addResponses("badRequest", badRequest);
    components.addResponses("notFound", notFound);
    components.addResponses("unauthorized", unauthorized);
    components.addResponses("forbidden", forbidden);
    components.addResponses("internalServerError", internalServerError);

    components.addSecuritySchemes("jwt", new SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT"));

    return new OpenAPI()
            .info(new Info()
                    .title("Análise Vínculos - Gerenciador de Configurações - API")
                    .description("Esta API fornece acesso a serviço de Gerenciamento de Configurações do sistema Análise de Vínculos.")
                    .version("v1.0.0"))
            .components(components)
            .addServersItem(new io.swagger.v3.oas.models.servers.Server().url(serverUrl));

  }

  @Bean
  public GroupedOpenApi producersGroup() {
    String[] paths = {"/**"};
    return GroupedOpenApi.builder().group("Geral").pathsToMatch(paths).build();
  }

  private String read(String key) throws IOException {
    try (var inputStream = jsonFile.getInputStream()) {
      String content = StreamUtils.copyToString(inputStream, StandardCharsets.UTF_8);
      return new JSONObject(content).get(key).toString();
    }
  }
}
