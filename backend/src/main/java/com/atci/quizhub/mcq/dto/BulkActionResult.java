package com.atci.quizhub.mcq.dto;

/** Per-item outcome for a bulk action (send-for-review, assign, etc.). */
public record BulkActionResult(Long mcqId, boolean success, String message) {}
