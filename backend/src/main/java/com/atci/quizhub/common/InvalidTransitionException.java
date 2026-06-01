package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public class InvalidTransitionException extends DomainException {
    public InvalidTransitionException(String message) { super(message, HttpStatus.CONFLICT); }
}
