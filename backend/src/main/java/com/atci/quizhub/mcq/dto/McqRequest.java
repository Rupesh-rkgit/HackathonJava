package com.atci.quizhub.mcq.dto;

import com.atci.quizhub.mcq.AnswerOption;
import com.atci.quizhub.mcq.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record McqRequest(
        @NotBlank String questionStem,
        @NotBlank String optionA,
        @NotBlank String optionB,
        @NotBlank String optionC,
        @NotBlank String optionD,
        @NotNull AnswerOption correctAnswer,
        @NotNull Difficulty difficulty,
        @NotNull Long stackId,
        @NotNull Long topicId,
        @NotNull SaveMode mode) {}
