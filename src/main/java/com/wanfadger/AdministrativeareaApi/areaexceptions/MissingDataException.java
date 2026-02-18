package com.wanfadger.AdministrativeareaApi.areaexceptions;

public class MissingDataException extends RuntimeException{
    public MissingDataException(String message) {
        super(message);
    }
}
