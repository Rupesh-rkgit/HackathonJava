package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) { super(message, HttpStatus.FORBIDDEN); }
}
