package com.atci.quizhub.mcq;

import com.atci.quizhub.common.InvalidTransitionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McqLifecycleTest {

    private final McqLifecycle lifecycle = new McqLifecycle();

    @Test
    void draftCanGoToReadyForReview() {
        assertEquals(McqStatus.READY_FOR_REVIEW,
            lifecycle.afterSendForReview(McqStatus.DRAFT));
    }

    @Test
    void rejectedCanGoToReadyForReview() {
        assertEquals(McqStatus.READY_FOR_REVIEW,
            lifecycle.afterSendForReview(McqStatus.REJECTED));
    }

    @Test
    void underReviewCannotBeSentForReview() {
        assertThrows(InvalidTransitionException.class,
            () -> lifecycle.afterSendForReview(McqStatus.UNDER_REVIEW));
    }

    @Test
    void readyForReviewCanBeAssigned() {
        assertEquals(McqStatus.UNDER_REVIEW,
            lifecycle.afterAssign(McqStatus.READY_FOR_REVIEW));
    }

    @Test
    void draftCannotBeAssigned() {
        assertThrows(InvalidTransitionException.class,
            () -> lifecycle.afterAssign(McqStatus.DRAFT));
    }

    @Test
    void underReviewCanBeApproved() {
        assertEquals(McqStatus.APPROVED, lifecycle.afterApprove(McqStatus.UNDER_REVIEW));
    }

    @Test
    void underReviewCanBeRejected() {
        assertEquals(McqStatus.REJECTED, lifecycle.afterReject(McqStatus.UNDER_REVIEW));
    }

    @Test
    void approvedCannotBeApprovedAgain() {
        assertThrows(InvalidTransitionException.class,
            () -> lifecycle.afterApprove(McqStatus.APPROVED));
    }

    @Test
    void onlyDraftAndRejectedAreEditable() {
        assertTrue(lifecycle.isEditableByCreator(McqStatus.DRAFT));
        assertTrue(lifecycle.isEditableByCreator(McqStatus.REJECTED));
        assertFalse(lifecycle.isEditableByCreator(McqStatus.READY_FOR_REVIEW));
        assertFalse(lifecycle.isEditableByCreator(McqStatus.UNDER_REVIEW));
        assertFalse(lifecycle.isEditableByCreator(McqStatus.APPROVED));
    }
}
