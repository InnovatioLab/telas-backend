package com.telas.infra.exceptions;

import java.io.Serial;

public class ResourceNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5237270577989992579L;

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
