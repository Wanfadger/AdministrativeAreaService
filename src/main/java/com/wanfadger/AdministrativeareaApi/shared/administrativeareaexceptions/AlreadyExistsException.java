package com.wanfadger.AdministrativeareaApi.shared.administrativeareaexceptions;

public class AlreadyExistsException extends RuntimeException{
    public AlreadyExistsException(String message) {
        super(message);
    }
}
