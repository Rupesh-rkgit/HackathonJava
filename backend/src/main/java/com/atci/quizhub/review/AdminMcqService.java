package com.atci.quizhub.review;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.mcq.Mcq;
import com.atci.quizhub.mcq.McqRepository;
import com.atci.quizhub.mcq.dto.BulkActionResult;
import com.atci.quizhub.review.dto.EligibleReviewerResponse;
import com.atci.quizhub.user.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Transactional
public class AdminMcqService {

    private final McqRepository mcqs;
    private final UserSkillRepository userSkills;
    private final ReviewService reviewService;

    public AdminMcqService(McqRepository mcqs, UserSkillRepository userSkills, ReviewService reviewService) {
        this.mcqs = mcqs; this.userSkills = userSkills; this.reviewService = reviewService;
    }

    public List<EligibleReviewerResponse> eligibleReviewers(Long mcqId) {
        Mcq mcq = mcqs.findById(mcqId).orElseThrow(() -> new NotFoundException("MCQ not found"));
        String creatorId = mcq.getCreator().getEnterpriseId();
        return userSkills.findUsersByStackId(mcq.getStack().getId()).stream()
                .filter(u -> !u.getEnterpriseId().equals(creatorId))
                .distinct()
                .map(u -> new EligibleReviewerResponse(u.getEnterpriseId(), u.getName()))
                .toList();
    }

    /**
     * Reviewers eligible for EVERY selected MCQ — the intersection of each MCQ's
     * eligible set. Guarantees a bulk assignment to any returned reviewer is valid
     * for all selected questions (skill-matched to each, creator of none).
     */
    public List<EligibleReviewerResponse> commonEligibleReviewers(List<Long> mcqIds) {
        if (mcqIds == null || mcqIds.isEmpty()) return List.of();
        Map<String, EligibleReviewerResponse> intersection = null;
        for (Long id : mcqIds) {
            Map<String, EligibleReviewerResponse> here = new LinkedHashMap<>();
            for (EligibleReviewerResponse r : eligibleReviewers(id)) {
                here.put(r.enterpriseId(), r);
            }
            if (intersection == null) {
                intersection = here;
            } else {
                intersection.keySet().retainAll(here.keySet());
            }
        }
        return intersection == null ? List.of() : new ArrayList<>(intersection.values());
    }

    /** Assign one reviewer to many MCQs, returning a per-item pass/fail result. */
    public List<BulkActionResult> bulkAssign(List<Long> mcqIds, String reviewerEnterpriseId,
                                             String adminEnterpriseId) {
        List<BulkActionResult> results = new ArrayList<>();
        for (Long id : mcqIds) {
            try {
                reviewService.assign(id, reviewerEnterpriseId, adminEnterpriseId);
                results.add(new BulkActionResult(id, true, "Assigned"));
            } catch (RuntimeException e) {
                results.add(new BulkActionResult(id, false, e.getMessage()));
            }
        }
        return results;
    }
}
