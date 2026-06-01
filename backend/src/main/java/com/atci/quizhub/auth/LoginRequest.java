package com.atci.quizhub.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String enterpriseId, @NotBlank String password) {}
