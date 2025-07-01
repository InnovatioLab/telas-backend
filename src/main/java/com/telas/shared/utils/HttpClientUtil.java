package com.telas.shared.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.InvalidQueryParamsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Map;

@Component
public class HttpClientUtil {
  private final Logger log = LoggerFactory.getLogger(HttpClientUtil.class);
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public HttpClientUtil(WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
    webClient = webClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  @Transactional
  public <T> T makePostRequest(String url, Object body, Class<T> responseType, Map<String, String> queryParams) {
    return makePostRequest(url, body, responseType, queryParams, null);
  }

  private <T> T makePostRequest(String url, Object body, Class<T> responseType, Map<String, String> queryParams, Map<String, String> headers) {
    try {
      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);

      if (queryParams != null) {
        queryParams.forEach(uriBuilder::queryParam);
      }

      WebClient.RequestBodySpec requestBodySpec = webClient.post()
              .uri(uriBuilder.build().toUri())
              .contentType(MediaType.APPLICATION_JSON);

      if (headers != null) {
        requestBodySpec.headers(httpHeaders -> headers.forEach(httpHeaders::add));
      }

      return requestBodySpec
              .bodyValue(body)
              .retrieve()
              .bodyToMono(responseType)
              .block();
    } catch (WebClientResponseException | HttpClientErrorException exception) {
      String responseBody = ((WebClientResponseException) exception).getResponseBodyAsString();
      log.error("Error during POST request to: {}, error: {}", url, responseBody);
      throw new BusinessRuleException(responseBody);
    }
  }

  @Transactional
  public <T> T makeGetRequest(String url, Class<T> responseType, Map<String, String> queryParams) {
    try {
      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);

      if (queryParams != null) {
        for (Map.Entry<String, String> entry : queryParams.entrySet()) {
          uriBuilder.queryParam(entry.getKey(), entry.getValue());
        }
      }

      return webClient.get()
              .uri(uriBuilder.build().toUri())
              .retrieve()
              .bodyToMono(responseType)
              .block();
    } catch (WebClientResponseException exception) {
      log.error("Error during GET request to: {}, error: {}", url, exception.getMessage());
      throw new InvalidQueryParamsException("Error during request: " + exception.getMessage(), exception);
    }
  }

  @Transactional
  public <T> T makeDeleteRequest(String url, Class<T> responseType, Map<String, String> queryParams) {
    return makeDeleteRequest(url, responseType, queryParams, null);
  }

  private <T> T makeDeleteRequest(String url, Class<T> responseType, Map<String, String> queryParams, Map<String, String> headers) {
    try {
      UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(url);

      if (queryParams != null) {
        queryParams.forEach(uriBuilder::queryParam);
      }

      WebClient.RequestHeadersSpec<?> requestSpec = webClient.delete()
              .uri(uriBuilder.build().toUri());

      if (headers != null) {
        requestSpec.headers(httpHeaders -> headers.forEach(httpHeaders::add));
      }

      return requestSpec
              .retrieve()
              .bodyToMono(responseType)
              .block();
    } catch (WebClientResponseException | HttpClientErrorException exception) {
      String responseBody = exception instanceof WebClientResponseException
              ? ((WebClientResponseException) exception).getResponseBodyAsString()
              : exception.getMessage();
      log.error("Error during DELETE request to: {}, error: {}", url, responseBody);
      throw new BusinessRuleException(responseBody);
    }
  }
}