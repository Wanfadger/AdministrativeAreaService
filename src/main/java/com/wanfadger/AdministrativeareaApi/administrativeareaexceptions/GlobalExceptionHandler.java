package com.wanfadger.AdministrativeareaApi.administrativeareaexceptions;

import com.wanfadger.AdministrativeareaApi.entity.AdministrativeAreaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;


import java.rmi.ServerException;
import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler  {

    @ExceptionHandler({AlreadyExistsException.class})
    public ProblemDetail handleAlreadyExistsException(AlreadyExistsException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT , exception.getMessage());
    }

    @ExceptionHandler({NotFoundException.class})
    public ProblemDetail handleNotFoundException(NotFoundException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND , exception.getMessage());
    }

    @ExceptionHandler({MissingDataException.class})
    public ProblemDetail handleMissingDataException(MissingDataException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST , exception.getMessage());
    }

    @ExceptionHandler({InvalidException.class})
    public ProblemDetail handleInvalidException(InvalidException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST , exception.getMessage());
    }

    @ExceptionHandler({HttpClientErrorException.Forbidden.class})
    public ProblemDetail handleForbiddenException(HttpClientErrorException.Forbidden exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN , exception.getMessage());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class})
    public ProblemDetail handleForbiddenException(HttpMessageNotReadableException exception) {
        exception.printStackTrace();
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST , "Required request body is missing");
    }





    /** A required query/path param is missing → 400 (not 500). */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingParam(MissingServletRequestParameterException ex) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Missing required parameter '" + ex.getParameterName() + "'");
    }

    /**
     * A param value can't be converted to its target type (e.g. an unknown {@code type}) → 400.
     * Without this, such errors fall through to the catch-all {@code RuntimeException} handler and
     * are wrongly reported as 500.
     */
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

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });

        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST , "Wrong values");

        problemDetail.setProperty("Errors" , errors);

        return problemDetail;
    }

    @ExceptionHandler({RuntimeException.class , ServerException.class})
    public ProblemDetail handleRuntimeException(RuntimeException exception) {
        exception.printStackTrace();

        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR , exception.getMessage());
    }




}
