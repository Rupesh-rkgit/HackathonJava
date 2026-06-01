package com.atci.quizhub.common;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {}
