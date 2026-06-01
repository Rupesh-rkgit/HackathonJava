package com.atci.quizhub.auth;

public record LoginResponse(String token, String role, String enterpriseId, String name) {}
