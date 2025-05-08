package com.telas.infra.exceptions;

public class GlobalExceptionConstants {
    public static final String PSQL_ERROR_MESSAGE = "An error occurred during the operation. Please try again later.";
    public static final String ATTACHMENT = "attachment";
    public static final String FILE_MAX_SIZE = "The file must be at most 10MB";
    public static final String CHECK_FIELDS_MESSAGE = "Please check all fields with validation";
    public static final String INVALID_VALUE_MESSAGE = "Invalid value for type %s: %s. Accepted values are: %s";
    public static final String MANDATORY_PARAMETER_NOT_PROVIDED = "Mandatory parameter not provided: ";
    public static final String INVALID_DATE_FORMAT_MESSAGE = "Invalid date format: ";
    public static final String EXPECTED_DATE_FORMAT_MESSAGE = "The expected format is YYYY-MM-DD";
    public static final String USE_APPROPRIATE_FORMATS_MESSAGE = "Use appropriate formats for date and time.";
    public static final String AUTHENTICATION_ERROR_MESSAGE = "Unauthorized, authentication is required!";
    public static final String RESOURCE_NOT_FOUND_MESSAGE = "Resource not found";
    public static final String INVALID_PARAMETER_TYPE_MESSAGE = "Invalid parameter type: ";

    private GlobalExceptionConstants() {
    }
}
