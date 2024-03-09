package com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.HttpClientErrorException;


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

    @ExceptionHandler({ExpiredJwtException.class})
    public ProblemDetail handleExpiredJwtException(ExpiredJwtException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED , exception.getMessage());
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
