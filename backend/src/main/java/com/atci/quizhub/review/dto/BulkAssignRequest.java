package com.atci.quizhub.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Assign one reviewer to many MCQs at once. */
public record BulkAssignRequest(@NotEmpty List<Long> ids, @NotBlank String reviewerEnterpriseId) {}
