package com.marketingproject.infra.exceptions;

import java.io.Serial;

public class ForbiddenException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5237270577989992579L;

    public ForbiddenException(String message) {
        super(message);
    }
}
