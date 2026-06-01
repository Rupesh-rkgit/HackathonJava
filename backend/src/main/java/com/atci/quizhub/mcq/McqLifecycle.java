package com.atci.quizhub.mcq;

import com.atci.quizhub.common.InvalidTransitionException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Single owner of all MCQ status transitions. No other class may mutate status directly.
 */
@Component
public class McqLifecycle {

    private static final Set<McqStatus> EDITABLE = Set.of(McqStatus.DRAFT, McqStatus.REJECTED);

    public boolean isEditableByCreator(McqStatus status) {
        return EDITABLE.contains(status);
    }

    public McqStatus afterSendForReview(McqStatus current) {
        if (!EDITABLE.contains(current)) {
            throw new InvalidTransitionException(
                "Only Draft or Rejected MCQs can be sent for review (was " + current + ")");
        }
        return McqStatus.READY_FOR_REVIEW;
    }

    public McqStatus afterAssign(McqStatus current) {
        if (current != McqStatus.READY_FOR_REVIEW) {
            throw new InvalidTransitionException(
                "Only Ready-for-Review MCQs can be assigned (was " + current + ")");
        }
        return McqStatus.UNDER_REVIEW;
    }

    public McqStatus afterApprove(McqStatus current) {
        if (current != McqStatus.UNDER_REVIEW) {
            throw new InvalidTransitionException(
                "Only Under-Review MCQs can be approved (was " + current + ")");
        }
        return McqStatus.APPROVED;
    }

    public McqStatus afterReject(McqStatus current) {
        if (current != McqStatus.UNDER_REVIEW) {
            throw new InvalidTransitionException(
                "Only Under-Review MCQs can be rejected (was " + current + ")");
        }
        return McqStatus.REJECTED;
    }
}
