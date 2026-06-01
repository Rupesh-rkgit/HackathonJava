package com.atci.quizhub.review;

import com.atci.quizhub.mcq.*;
import com.atci.quizhub.mcq.dto.*;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.review.dto.EligibleReviewerResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminMcqServiceTest {

    @Autowired AdminMcqService adminService;
    @Autowired McqService mcqService;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId;
    final String creator = "gaurav.a.bhola"; // Spring Cloud

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    @Test
    void eligibleReviewersMatchStackAndExcludeCreator() {
        var resp = mcqService.create(new McqRequest("Q?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        List<EligibleReviewerResponse> eligible = adminService.eligibleReviewers(resp.id());
        List<String> ids = eligible.stream().map(EligibleReviewerResponse::enterpriseId).toList();
        // Spring Cloud SMEs are gaurav (creator, excluded), divya, indugu
        assertTrue(ids.contains("divya.madhanasekar"));
        assertTrue(ids.contains("indugu.hari.prasad"));
        assertFalse(ids.contains("gaurav.a.bhola"));
    }
}
