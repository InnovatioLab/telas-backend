package com.telas.infra.exceptions;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.stripe.exception.StripeException;
import com.telas.dtos.response.ResponseDto;
import jakarta.persistence.OptimisticLockException;
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
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler({DataIntegrityViolationException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<?> handlePSQLException(DataIntegrityViolationException ex, WebRequest request) {
        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.CONFLICT, GlobalExceptionConstants.PSQL_ERROR_MESSAGE, Arrays.asList(ex.getMostSpecificCause().getMessage()));
        logException("DataIntegrityViolationException", ex, obj);
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = ex.getParameterName();
        String error = GlobalExceptionConstants.MANDATORY_PARAMETER_NOT_PROVIDED + ex.getParameterName();

        ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error, Arrays.asList(field));
        logException("MissingServletRequestParameterException", ex, res);
        return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<String> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(DefaultMessageSourceResolvable::getDefaultMessage)
                .toList();

        ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST,
                GlobalExceptionConstants.CHECK_FIELDS_MESSAGE, erros);
        logException("MethodArgumentNotValidException", ex, res);

        return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String field = GlobalExceptionConstants.ATTACHMENT;
        String error = GlobalExceptionConstants.FILE_MAX_SIZE;

        ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error, Arrays.asList(field));
        logException("MaxUploadSizeExceededException", ex, res);
        return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
    }

    @Override
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        Throwable rootCause = ex.getRootCause();
        if (rootCause instanceof InvalidFormatException) {
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
            logException("HttpMessageNotReadableException (ENUM)", ex, res);
            return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
        } else if (rootCause instanceof DateTimeParseException) {
            DateTimeParseException dateTimeParseException = (DateTimeParseException) rootCause;
            String invalidValue = dateTimeParseException.getParsedString();
            String errorMessage = GlobalExceptionConstants.INVALID_DATE_FORMAT_MESSAGE + invalidValue + ". ";

            if (ex.getLocalizedMessage().contains("LocalDate")) {
                errorMessage += GlobalExceptionConstants.EXPECTED_DATE_FORMAT_MESSAGE;
            } else {
                errorMessage += GlobalExceptionConstants.USE_APPROPRIATE_FORMATS_MESSAGE;
            }

            ResponseDto<Object> res = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, errorMessage);
            logException("HttpMessageNotReadableException (DateTime)", ex, res);
            return handleExceptionInternal(ex, res, headers, HttpStatus.BAD_REQUEST, request);
        }

        return handleExceptionInternal(ex, null, headers, HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({BusinessRuleException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ResponseEntity<?> handleBusinessRuleException(BusinessRuleException ex, WebRequest request) {
        String field = ex.getMessage();
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.UNPROCESSABLE_ENTITY, error, Arrays.asList(field));
        logException("BusinessRuleException", ex, obj);
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.UNPROCESSABLE_ENTITY, request);
    }

    @ExceptionHandler({ResourceNotFoundException.class})
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ResponseEntity<?> handleResourceNotFoundException(ResourceNotFoundException ex, WebRequest request) {
        String field = GlobalExceptionConstants.RESOURCE_NOT_FOUND_MESSAGE;
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.NOT_FOUND, error, Arrays.asList(field));
        logException("ResourceNotFoundException", ex, obj);
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler({UnauthorizedException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ResponseEntity<?> handleUnauthorizedException(UnauthorizedException ex, WebRequest request) {
        String field = GlobalExceptionConstants.AUTHENTICATION_ERROR_MESSAGE;
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.UNAUTHORIZED, error, Arrays.asList(field));
        logException("UnauthorizedException", ex, obj);
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler({ForbiddenException.class})
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ResponseEntity<?> handleForbiddenException(ForbiddenException ex, WebRequest request) {
        String field = ex.getMessage();
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.FORBIDDEN, error, Arrays.asList(field));
        logException("ForbiddenException", ex, obj);
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({InvalidQueryParamsException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleInvalidQueryParamsException(InvalidQueryParamsException ex, WebRequest request) {
        String error = ex.getMessage();

        ResponseDto<Object> obj = ResponseDto.fromData(
                null,
                HttpStatus.BAD_REQUEST,
                error,
                Arrays.asList("Invalid QueryParams")
        );
        logException("InvalidQueryParamsException", ex, obj);
        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex, WebRequest request) {
        String error = GlobalExceptionConstants.INVALID_PARAMETER_TYPE_MESSAGE + ex.getName();

        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error);
        logException("MethodArgumentTypeMismatchException", ex, obj);

        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({StripeException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<?> handleStripeException(StripeException ex, WebRequest request) {
        String error = "Error during payment process: " + ex.getMessage();
        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.BAD_REQUEST, error, List.of(ex.getMessage()));
        logException("StripeException", ex, obj);

        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler({OptimisticLockException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ResponseEntity<?> handleOptimisticLockException(OptimisticLockException ex, WebRequest request) {
        String error = "Optimistic concurrency failure";
        ResponseDto<Object> obj = ResponseDto.fromData(null, HttpStatus.CONFLICT, error, List.of(ex.getMessage()));
        logException("OptimisticLockException", ex, obj);

        return handleExceptionInternal(ex, obj, new HttpHeaders(), HttpStatus.CONFLICT, request);
    }

    private void logException(String title, Throwable ex, ResponseDto<?> res) {
        String message = " =============== " + title + " ========================== | response=" + formatResponse(res);
        logger.error(message, ex);
    }

    private String formatResponse(ResponseDto<?> res) {
        if (res == null) {
            return "null";
        }
        try {
            return objectMapper.writeValueAsString(res);
        } catch (Exception ignored) {
            return "status=" + res.getStatus()
                    + ", message=" + res.getMensagem()
                    + ", errors=" + res.getErrors();
        }
    }
}
