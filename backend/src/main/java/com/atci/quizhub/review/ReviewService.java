package com.atci.quizhub.review;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ReviewService {

    private final ReviewAssignmentRepository assignments;
    private final McqRepository mcqs;
    private final UserRepository users;
    private final McqLifecycle lifecycle;

    public ReviewService(ReviewAssignmentRepository assignments, McqRepository mcqs,
                         UserRepository users, McqLifecycle lifecycle) {
        this.assignments = assignments; this.mcqs = mcqs;
        this.users = users; this.lifecycle = lifecycle;
    }

    public void assign(Long mcqId, String reviewerEnterpriseId, String adminEnterpriseId) {
        Mcq mcq = mcqs.findById(mcqId).orElseThrow(() -> new NotFoundException("MCQ not found"));
        if (mcq.getCreator().getEnterpriseId().equals(reviewerEnterpriseId)) {
            throw new ForbiddenException("Creator cannot be assigned as reviewer");
        }
        User reviewer = users.findByEnterpriseId(reviewerEnterpriseId)
                .orElseThrow(() -> new NotFoundException("Reviewer not found"));
        User admin = users.findByEnterpriseId(adminEnterpriseId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));
        mcq.setStatus(lifecycle.afterAssign(mcq.getStatus()));
        mcqs.save(mcq);
        assignments.save(new ReviewAssignment(mcq, reviewer, admin));
    }

    public void approve(Long mcqId, String reviewerEnterpriseId) {
        ReviewAssignment a = currentAssignment(mcqId);
        requireAssignedReviewer(a, reviewerEnterpriseId);
        Mcq mcq = a.getMcq();
        mcq.setStatus(lifecycle.afterApprove(mcq.getStatus()));
        mcqs.save(mcq);
        a.setOutcome(ReviewOutcome.APPROVED);
        a.setDecidedAt(Instant.now());
        assignments.save(a);
    }

    public void reject(Long mcqId, String reviewerEnterpriseId, String comments) {
        if (comments == null || comments.isBlank()) {
            throw new IllegalArgumentException("Rejection comment is mandatory");
        }
        ReviewAssignment a = currentAssignment(mcqId);
        requireAssignedReviewer(a, reviewerEnterpriseId);
        Mcq mcq = a.getMcq();
        mcq.setStatus(lifecycle.afterReject(mcq.getStatus()));
        mcqs.save(mcq);
        a.setOutcome(ReviewOutcome.REJECTED);
        a.setComments(comments);
        a.setDecidedAt(Instant.now());
        assignments.save(a);
    }

    public Page<ReviewAssignment> pendingFor(String reviewerEnterpriseId, Pageable pageable) {
        User reviewer = users.findByEnterpriseId(reviewerEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return assignments.findByReviewerIdAndOutcome(reviewer.getId(), ReviewOutcome.PENDING, pageable);
    }

    public String latestComments(Long mcqId) {
        // Return the comment from the most recent *rejection*, not merely the latest
        // assignment — after a resubmit a fresh PENDING row exists with no comment yet.
        return assignments.findFirstByMcqIdAndOutcomeOrderByAssignedAtDesc(mcqId, ReviewOutcome.REJECTED)
                .map(ReviewAssignment::getComments).orElse(null);
    }

    private ReviewAssignment currentAssignment(Long mcqId) {
        return assignments.findFirstByMcqIdAndOutcomeOrderByAssignedAtDesc(mcqId, ReviewOutcome.PENDING)
                .orElseThrow(() -> new NotFoundException("No pending review for MCQ " + mcqId));
    }

    private void requireAssignedReviewer(ReviewAssignment a, String enterpriseId) {
        if (!a.getReviewer().getEnterpriseId().equals(enterpriseId)) {
            throw new ForbiddenException("Only the assigned reviewer may decide this MCQ");
        }
    }
}
