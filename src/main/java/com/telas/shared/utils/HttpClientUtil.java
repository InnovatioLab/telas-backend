package com.telas.shared.utils;

import com.telas.infra.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
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

    public HttpClientUtil(WebClient.Builder webClientBuilder) {
        webClient = webClientBuilder.build();
    }

    public <T> void makePostRequest(String url, Object body, Class<T> responseType, Map<String, String> queryParams, Map<String, String> headers) {
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

            requestBodySpec
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(responseType)
                    .block();
        } catch (WebClientResponseException | HttpClientErrorException exception) {
            log.error("Error during POST request to: {}, error: {}", url, exception.getMessage());
            handleException(exception);
        }
    }

    public <T> T makePostRequestWithReturn(String url, Object body, Class<T> responseType, Map<String, String> queryParams, Map<String, String> headers) {
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
            log.error("Error during POST request to: {}, error: {}", url, exception.getMessage());
            handleException(exception);
            return null;
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

    @Async
    public void makeDeleteRequest(String url, Map<String, String> queryParams, Map<String, String> headers) {
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

            requestSpec
                    .retrieve()
                    .bodyToMono(Void.class)
                    .block();
        } catch (WebClientResponseException | HttpClientErrorException exception) {
            log.error("Error during DELETE request to: {}, error: {}", url, exception.getMessage());
            handleException(exception);
        }
    }

    private void handleException(Exception exception) {
        if (exception instanceof WebClientResponseException webClientException) {
            handleWebClientResponseException(webClientException);
        } else {
            throw new BusinessRuleException(exception.getMessage());
        }
    }

    private void handleWebClientResponseException(WebClientResponseException exception) {
        int statusCode = exception.getStatusCode().value();

        switch (statusCode) {
            case 401 -> throw new UnauthorizedException(exception.getMessage());
            case 403 -> throw new ForbiddenException(exception.getMessage());
            case 404 -> throw new ResourceNotFoundException(exception.getMessage());
            default -> throw new BusinessRuleException(exception.getMessage());
        }
    }
}