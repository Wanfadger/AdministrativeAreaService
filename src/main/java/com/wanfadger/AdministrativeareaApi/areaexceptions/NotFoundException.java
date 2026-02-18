package com.wanfadger.AdministrativeareaApi.areaexceptions;

public class NotFoundException extends RuntimeException{
    public NotFoundException(String message) {
        super(message);
    }
}
