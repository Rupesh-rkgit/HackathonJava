package com.atci.quizhub.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {
    Page<ReviewAssignment> findByReviewerIdAndOutcome(Long reviewerId, ReviewOutcome outcome, Pageable pageable);
    Optional<ReviewAssignment> findFirstByMcqIdAndOutcomeOrderByAssignedAtDesc(Long mcqId, ReviewOutcome outcome);
    Optional<ReviewAssignment> findFirstByMcqIdOrderByAssignedAtDesc(Long mcqId);
}
