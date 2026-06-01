package com.atci.quizhub.review;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.mcq.Mcq;
import com.atci.quizhub.mcq.McqRepository;
import com.atci.quizhub.review.dto.EligibleReviewerResponse;
import com.atci.quizhub.user.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminMcqService {

    private final McqRepository mcqs;
    private final UserSkillRepository userSkills;

    public AdminMcqService(McqRepository mcqs, UserSkillRepository userSkills) {
        this.mcqs = mcqs; this.userSkills = userSkills;
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
}
