package com.atci.quizhub.mcq.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Request body carrying a list of MCQ ids for a bulk action. */
public record IdListRequest(@NotEmpty List<Long> ids) {}
