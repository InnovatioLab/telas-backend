package com.telas.shared.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.telas.infra.exceptions.BusinessRuleException;
import com.telas.infra.exceptions.InvalidQueryParamsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
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

    public <T> T makePostRequest(String url, Object body, Class<T> responseType, Map<String, String> queryParams) {
        return makePostRequest(url, body, responseType, queryParams, null);
    }

    public <T> T makePostRequest(String url, Object body, Class<T> responseType, Map<String, String> queryParams, Map<String, String> headers) {
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

    public <T> T makePostRequest(String url, Object body, TypeReference<T> responseType, Map<String, String> queryParams, Map<String, String> headers) {
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

            String response = requestBodySpec
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return objectMapper.readValue(response, responseType);
        } catch (Exception e) {
            log.error("Error during POST request to: {}, error: {}", url, e.getMessage());
            throw new BusinessRuleException("Error during HTTP POST request, please try again later.");
        }
    }

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
}