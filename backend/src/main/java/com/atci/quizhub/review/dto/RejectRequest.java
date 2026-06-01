package com.atci.quizhub.review.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String comments) {}
