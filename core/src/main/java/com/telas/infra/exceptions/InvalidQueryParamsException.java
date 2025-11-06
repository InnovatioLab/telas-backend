package com.telas.infra.exceptions;

import java.io.Serial;

public class InvalidQueryParamsException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5237270577989992579L;

    public InvalidQueryParamsException(String message) {
        super(message);
    }

    public InvalidQueryParamsException(String message, Throwable cause) {
        super(message, cause);
    }
}
