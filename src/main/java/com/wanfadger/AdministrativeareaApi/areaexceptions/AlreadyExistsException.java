package com.wanfadger.AdministrativeareaApi.areaexceptions;

public class AlreadyExistsException extends RuntimeException{
    public AlreadyExistsException(String message) {
        super(message);
    }
}
