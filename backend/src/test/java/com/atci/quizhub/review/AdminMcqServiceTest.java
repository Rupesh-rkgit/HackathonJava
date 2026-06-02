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

    @Test
    void commonEligibleReviewersIsIntersectionAcrossSelection() {
        // Two Spring Cloud questions by gaurav -> common eligible = divya, indugu (gaurav excluded)
        var q1 = mcqService.create(new McqRequest("Q1?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        var q2 = mcqService.create(new McqRequest("Q2?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        var common = adminService.commonEligibleReviewers(List.of(q1.id(), q2.id()));
        var ids = common.stream().map(EligibleReviewerResponse::enterpriseId).toList();
        assertTrue(ids.contains("divya.madhanasekar"));
        assertTrue(ids.contains("indugu.hari.prasad"));
        assertFalse(ids.contains("gaurav.a.bhola"));
    }

    @Test
    void commonEligibleReviewersEmptyWhenNoReviewerFitsAll() {
        // Mix a Spring Cloud question (creator gaurav) with a Spring Boot question whose
        // only non-creator SME is swati. divya/indugu don't have Spring Boot; swati lacks
        // Spring Cloud -> no single reviewer is eligible for BOTH -> empty intersection.
        Long bootStack = stacks.findByName("Spring Boot").orElseThrow().getId();
        Long bootTopic = topics.findByStackId(bootStack).get(0).getId();
        var cloudQ = mcqService.create(new McqRequest("Cloud?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        // Spring Boot question created by swati so its eligible reviewer is birendra(admin, Spring Boot) only
        var bootQ = mcqService.create(new McqRequest("Boot?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, bootStack, bootTopic, SaveMode.SAVE_AND_SEND), "swati.avinash.nikam");
        var common = adminService.commonEligibleReviewers(List.of(cloudQ.id(), bootQ.id()));
        assertTrue(common.isEmpty(), "no single reviewer is skilled in both stacks");
    }
}
