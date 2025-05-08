package com.telas.dtos.response;

import org.springframework.http.HttpStatus;

import java.io.Serial;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

public class ResponseDto<T> implements Serializable {

    @Serial
    private static final long serialVersionUID = -6746702443721295550L;

    private transient T data;
    private List<String> errors = new ArrayList<>();
    private String message;
    private URI uri;
    private Integer status;

    ResponseDto() {
    }

    public static <T> ResponseDto<T> fromData(T data, HttpStatus status, String message) {
        return new ResponseDto<T>()
                .setData(data)
                .setStatus(status)
                .setMessage(message);
    }

    public static <T> ResponseDto<T> fromData(T data, HttpStatus status, String message, List<String> errors) {
        return new ResponseDto<T>()
                .setData(data)
                .setStatus(status)
                .setMessage(message)
                .setErrors(errors);
    }

    public String getMensagem() {
        return message;
    }

    public ResponseDto<T> setMessage(String message) {
        this.message = message;
        return this;
    }

    public T getData() {
        return data;
    }

    public ResponseDto<T> setData(T data) {
        this.data = data;
        return this;
    }

    public Integer getStatus() {
        return status;
    }

    public ResponseDto<T> setStatus(HttpStatus status) {
        this.status = status.value();
        return this;
    }

    public URI getUri() {
        return uri;
    }

    public List<String> getErrors() {
        return errors;
    }

    public ResponseDto<T> setErrors(List<String> errors) {
        this.errors = errors;
        return this;
    }
}

