package com.wanfadger.AdministrativeareaApi.administrativeareaexceptions;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * RFC-7807 error responses.
 *
 * <p>Clients — including the Angular console — render {@code detail} verbatim, so every handler here
 * must populate it with something a human can act on. Golden files pin the exact strings.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ------------------------------------------------------------------ domain

    @ExceptionHandler(AlreadyExistsException.class)
    public ProblemDetail handleAlreadyExists(AlreadyExistsException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail handleNotFound(NotFoundException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(MissingDataException.class)
    public ProblemDetail handleMissingData(MissingDataException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidException.class)
    public ProblemDetail handleInvalid(InvalidException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // ------------------------------------------------------------------ request binding

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Missing required parameter '" + ex.getParameterName() + "'");
    }

    /** A param that won't convert (e.g. an unknown {@code type}). Without this it would be a 500. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail;
        if (ex.getRequiredType() == AdministrativeAreaType.class) {
            detail = "Invalid value '" + ex.getValue() + "' for '" + ex.getName()
                    + "'. Allowed types (case-insensitive): REGION, SUBREGION, LOCALGOVERNMENT, "
                    + "COUNTY, SUBCOUNTY, PARISH.";
        } else {
            detail = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "'";
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody(HttpMessageNotReadableException ex) {
        log.debug("Unreadable request body", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Required request body is missing");
    }

    // ------------------------------------------------------------------ validation

    /**
     * Bean-validation failure on a single-object body (the PUT).
     *
     * <p>{@code detail} used to be the literal string "Wrong values", which told the user nothing —
     * the actual field errors were buried in a non-standard {@code Errors} extension property that
     * no client reads. Now the field errors ARE the detail; the map is kept as an extension for
     * machine consumers.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fe -> errors.putIfAbsent(fe.getField(), fe.getDefaultMessage()));

        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .sorted()
                .collect(Collectors.joining("; "));

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isBlank() ? "Validation failed" : detail);
        problem.setProperty("errors", errors);
        return problem;
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException ex) {
        String detail = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .sorted()
                .collect(Collectors.joining("; "));
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                detail.isBlank() ? "Validation failed" : detail);
    }

    /**
     * An unknown sort/filter property. Spring Data throws this at query time, and it used to reach
     * the catch-all and surface as a 500.
     */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownProperty(PropertyReferenceException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Unknown field '" + ex.getPropertyName() + "'");
    }

    /**
     * A database constraint the application-level check failed to anticipate — e.g. a duplicate name
     * under the same parent, racing two concurrent creates. A conflict, not a server error.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException ex) {
        log.warn("Database constraint violated", ex);
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "An administrative area with that name or code already exists");
    }

    // ------------------------------------------------------------------ catch-all

    /**
     * Spring's own exceptions ({@code ResponseStatusException},
     * {@code HandlerMethodValidationException}, …) all extend {@link RuntimeException}, and this
     * advice runs BEFORE {@code DefaultHandlerExceptionResolver} — so without this handler they
     * would be swallowed by the catch-all below and reported as 500s with a leaked message. Their
     * bodies are already correct; just pass them through.
     */
    @ExceptionHandler(ErrorResponseException.class)
    public ProblemDetail handleErrorResponse(ErrorResponseException ex) {
        return ex.getBody();
    }

    /**
     * Anything unforeseen. Returns a correlation reference instead of {@code ex.getMessage()}: the
     * raw message can carry SQL fragments, table names and other internals, and it goes straight to
     * the client. The stack trace goes to the log (it was being printed to stdout via
     * {@code printStackTrace()}, bypassing logging entirely).
     */
    @ExceptionHandler(RuntimeException.class)
    public ProblemDetail handleRuntime(RuntimeException ex) {
        String reference = UUID.randomUUID().toString();
        log.error("Unhandled exception [{}]", reference, ex);

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Reference: " + reference);
        problem.setProperty("reference", reference);
        return problem;
    }
}
