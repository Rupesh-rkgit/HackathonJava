package com.atci.quizhub.mcq;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.mcq.dto.McqRequest;
import com.atci.quizhub.mcq.dto.SaveMode;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McqServiceTest {

    @Autowired McqService service;
    @Autowired UserRepository users;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId; String sme;

    @BeforeEach
    void setup() {
        sme = users.findByEnterpriseId("gaurav.a.bhola").orElseThrow().getEnterpriseId();
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    private McqRequest req(SaveMode mode) {
        return new McqRequest("Stem?", "a", "b", "c", "d",
            AnswerOption.B, Difficulty.EASY, stackId, topicId, mode);
    }

    @Test
    void saveCreatesDraft() {
        var resp = service.create(req(SaveMode.SAVE), sme);
        assertEquals(McqStatus.DRAFT, resp.status());
    }

    @Test
    void saveAndSendCreatesReadyForReview() {
        var resp = service.create(req(SaveMode.SAVE_AND_SEND), sme);
        assertEquals(McqStatus.READY_FOR_REVIEW, resp.status());
    }

    @Test
    void editingApprovedMcqIsForbidden() {
        var draft = service.create(req(SaveMode.SAVE), sme);
        // force it to APPROVED via repository to simulate a non-editable state
        Mcq m = service.getEntity(draft.id());
        m.setStatus(McqStatus.APPROVED);
        service.saveEntity(m);
        assertThrows(ForbiddenException.class,
            () -> service.update(draft.id(), req(SaveMode.SAVE), sme));
    }

    @Test
    void nonCreatorCannotEdit() {
        var draft = service.create(req(SaveMode.SAVE), sme);
        assertThrows(ForbiddenException.class,
            () -> service.update(draft.id(), req(SaveMode.SAVE), "divya.madhanasekar"));
    }

    @Test
    void adminCannotEditAnotherUsersDraft() {
        // Spec: a Draft is accessible only to its creator, even for an admin super-user.
        var draft = service.create(req(SaveMode.SAVE), sme);
        assertThrows(ForbiddenException.class,
            () -> service.update(draft.id(), req(SaveMode.SAVE), "birendra.kumar.singh"));
    }

    @Test
    void adminCanEditNonDraftOfAnotherUser() {
        // Admins may super-edit any OTHER state. Send to review (READY_FOR_REVIEW), then admin edits.
        var ready = service.create(req(SaveMode.SAVE_AND_SEND), sme);
        var updated = service.update(ready.id(), req(SaveMode.SAVE), "birendra.kumar.singh");
        assertEquals(McqStatus.READY_FOR_REVIEW, updated.status());
    }

    @Test
    void sendForReviewMovesDraftToReady() {
        var draft = service.create(req(SaveMode.SAVE), sme);
        var resp = service.sendForReview(draft.id(), sme);
        assertEquals(McqStatus.READY_FOR_REVIEW, resp.status());
    }

    @Test
    void sendForReviewRejectedByNonCreator() {
        var draft = service.create(req(SaveMode.SAVE), sme);
        assertThrows(ForbiddenException.class,
            () -> service.sendForReview(draft.id(), "divya.madhanasekar"));
    }

    @Test
    void bulkSendForReviewReturnsPerItemResults() {
        var d1 = service.create(req(SaveMode.SAVE), sme);
        var d2 = service.create(req(SaveMode.SAVE), sme);
        var other = service.create(req(SaveMode.SAVE), "divya.madhanasekar"); // not owned by sme
        var results = service.bulkSendForReview(
            java.util.List.of(d1.id(), d2.id(), other.id()), sme);
        assertEquals(3, results.size());
        assertTrue(results.get(0).success());
        assertTrue(results.get(1).success());
        assertFalse(results.get(2).success()); // sme can't send someone else's draft
        assertEquals(McqStatus.READY_FOR_REVIEW, service.getEntity(d1.id()).getStatus());
    }
}
