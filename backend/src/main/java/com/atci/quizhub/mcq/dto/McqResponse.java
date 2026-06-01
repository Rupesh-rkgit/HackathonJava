package com.atci.quizhub.mcq.dto;

import com.atci.quizhub.mcq.*;

public record McqResponse(
        Long id, String questionStem,
        String optionA, String optionB, String optionC, String optionD,
        AnswerOption correctAnswer, Difficulty difficulty,
        Long stackId, String stackName, Long topicId, String topicName,
        String creatorEnterpriseId, McqStatus status, String reviewerComments) {

    public static McqResponse from(Mcq m, String reviewerComments) {
        return new McqResponse(
            m.getId(), m.getQuestionStem(),
            m.getOptionA(), m.getOptionB(), m.getOptionC(), m.getOptionD(),
            m.getCorrectAnswer(), m.getDifficulty(),
            m.getStack().getId(), m.getStack().getName(),
            m.getTopic().getId(), m.getTopic().getName(),
            m.getCreator().getEnterpriseId(), m.getStatus(), reviewerComments);
    }
}
