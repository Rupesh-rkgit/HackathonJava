package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {
    private final HttpStatus status;
    protected DomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
