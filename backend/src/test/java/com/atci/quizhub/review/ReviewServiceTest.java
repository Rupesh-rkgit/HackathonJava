package com.atci.quizhub.review;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.mcq.dto.*;
import com.atci.quizhub.masterdata.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReviewServiceTest {

    @Autowired ReviewService reviewService;
    @Autowired McqService mcqService;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId;
    final String creator = "gaurav.a.bhola";       // Spring Cloud SME
    final String reviewer = "divya.madhanasekar";  // also Spring Cloud
    final String admin = "birendra.kumar.singh";

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    private Long newReadyMcq() {
        var resp = mcqService.create(new McqRequest("Q?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        return resp.id();
    }

    @Test
    void assignMovesToUnderReview() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        assertEquals(McqStatus.UNDER_REVIEW, mcqService.getEntity(id).getStatus());
    }

    @Test
    void cannotAssignCreatorAsReviewer() {
        Long id = newReadyMcq();
        assertThrows(ForbiddenException.class,
            () -> reviewService.assign(id, creator, admin));
    }

    @Test
    void approveMovesToApproved() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        reviewService.approve(id, reviewer);
        assertEquals(McqStatus.APPROVED, mcqService.getEntity(id).getStatus());
    }

    @Test
    void rejectRequiresComment() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        assertThrows(IllegalArgumentException.class,
            () -> reviewService.reject(id, reviewer, "  "));
    }

    @Test
    void rejectMovesToRejectedAndStoresComment() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        reviewService.reject(id, reviewer, "Option C is wrong");
        Mcq m = mcqService.getEntity(id);
        assertEquals(McqStatus.REJECTED, m.getStatus());
        assertEquals("Option C is wrong", reviewService.latestComments(id));
    }

    @Test
    void nonAssignedReviewerCannotApprove() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        assertThrows(ForbiddenException.class,
            () -> reviewService.approve(id, "swati.avinash.nikam"));
    }

    @Test
    void rejectionCommentSurvivesResubmitAndReassignment() {
        // Multi-cycle: reject with a comment, creator resubmits, admin reassigns.
        // The original rejection feedback must still be retrievable (not hidden by the
        // fresh PENDING assignment that has no comment yet).
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        reviewService.reject(id, reviewer, "Option C is wrong");

        // creator edits and re-sends for review (REJECTED -> READY_FOR_REVIEW)
        mcqService.update(id, new McqRequest("Q fixed?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        // admin assigns again -> a new PENDING assignment with null comments
        reviewService.assign(id, reviewer, admin);

        assertEquals("Option C is wrong", reviewService.latestComments(id),
            "Prior rejection feedback should remain visible after resubmit");
    }
}
