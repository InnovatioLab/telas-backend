package com.telas.infra.exceptions;

import java.io.Serial;

public class BusinessRuleException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5237270577989992579L;

    public BusinessRuleException(String message) {
        super(message);
    }
}
