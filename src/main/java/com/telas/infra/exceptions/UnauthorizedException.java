package com.telas.infra.exceptions;

import java.io.Serial;

public class UnauthorizedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5237270577989992579L;

    public UnauthorizedException(String message) {
        super(message);
    }
}
