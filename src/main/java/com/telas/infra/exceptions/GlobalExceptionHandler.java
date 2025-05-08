package com.telas.infra.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.telas.dtos.response.ResponseDto;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler({DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<?> handlePSQLException(DataIntegrityViolationException ex, WebRequest request) {
        logger.error(" =========== DataIntegrityViolationException =========== " + ex.getMostSpecificCause().getMessage());
        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.CONFLICT, GlobalExceptionConstants.PSQL_ERROR_MESSAGE, Arrays.asList(ex.getMostSpecificCause().getMessage()));
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.error(" =============== MissingServletRequestParameterException ==========================");

        String field = ex.getParameterName();
        String error = GlobalExceptionConstants.MANDATORY_PARAMETER_NOT_PROVIDED + ex.getParameterName();

        ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error, Arrays.asList(field));
        return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.error(" =============== DTO fields that failed validation ==========================");

        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST,
                GlobalExceptionConstants.CHECK_FIELDS_MESSAGE, erros);

        return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        logger.error(" =============== MaxUploadSizeExceededException ==========================");

        String field = GlobalExceptionConstants.ATTACHMENT;
        String error = GlobalExceptionConstants.FILE_MAX_SIZE;

        ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error, Arrays.asList(field));
        return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof InvalidFormatException) {
            logger.error(" =============== HttpMessageNotReadableException validation ENUM ==========================");

            InvalidFormatException invalidFormatException = (InvalidFormatException) rootCause;
            String invalidValue = invalidFormatException.getValue().toString();
            String enumType = invalidFormatException.getTargetType().getSimpleName();
            List<String> enumValues = Arrays.stream(invalidFormatException.getTargetType().getEnumConstants())
                    .map(Object::toString)
                    .collect(Collectors.toList());

            String errorMessage = String.format(
                    GlobalExceptionConstants.INVALID_VALUE_MESSAGE,
                    enumType, invalidValue, enumValues);

            ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, errorMessage);
            return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
        } else if (rootCause instanceof DateTimeParseException) {
            logger.error(" =============== HttpMessageNotReadableException validation LocalDate or LocalDateTime ==========================");

            DateTimeParseException dateTimeParseException = (DateTimeParseException) rootCause;
            String invalidValue = dateTimeParseException.getParsedString();
            String errorMessage = GlobalExceptionConstants.INVALID_DATE_FORMAT_MESSAGE + invalidValue + ". ";

            if (ex.getLocalizedMessage().contains("LocalDate")) {
                errorMessage += GlobalExceptionConstants.EXPECTED_DATE_FORMAT_MESSAGE;
            } else {
                errorMessage += GlobalExceptionConstants.USE_APPROPRIATE_FORMATS_MESSAGE;
            }

            ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, errorMessage);
            return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
        }

        return handleExceptionInternal(ex, null, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({BusinessRuleException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<?> handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        logger.error(" =============== BusinessRuleException ==========================");

        String field = ex.getMessage();
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.UNPROCESSABLE_ENTITY, error, Arrays.asList(field));
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        logger.error(" =============== ResourceNotFoundException ==========================");

        String field = GlobalExceptionConstants.RESOURCE_NOT_FOUND_MESSAGE;
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.NOT_FOUND, error, Arrays.asList(field));
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({UnauthorizedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        logger.error(" =============== UnauthorizedException ==========================");

        String field = GlobalExceptionConstants.AUTHENTICATION_ERROR_MESSAGE;
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.UNAUTHORIZED, error, Arrays.asList(field));
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<?> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        logger.error(" =============== ForbiddenException ==========================");

        String field = ex.getMessage();
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.FORBIDDEN, error, Arrays.asList(field));
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({InvalidQueryParamsException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleInvalidQueryParamsException(InvalidQueryParamsException ex, WebRequest request) {
        logger.error(" =============== InvalidQueryParamsException ==========================");

        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(
                null,
                HttpStatus.BAD_REQUEST,
                error,
                Arrays.asList("Invalid QueryParams")
        );
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        logger.error(" =============== MethodArgumentTypeMismatchException ==========================");

        String error = GlobalExceptionConstants.INVALID_PARAMETER_TYPE_MESSAGE + ex.getName();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error);

        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }
}
