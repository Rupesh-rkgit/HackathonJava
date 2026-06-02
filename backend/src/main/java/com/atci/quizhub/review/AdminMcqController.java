package com.atci.quizhub.review;

import com.atci.quizhub.auth.CurrentUser;
import com.atci.quizhub.mcq.McqRepository;
import com.atci.quizhub.mcq.McqService;
import com.atci.quizhub.mcq.dto.BulkActionResult;
import com.atci.quizhub.mcq.dto.IdListRequest;
import com.atci.quizhub.mcq.dto.McqRequest;
import com.atci.quizhub.mcq.dto.McqResponse;
import com.atci.quizhub.review.dto.AssignRequest;
import com.atci.quizhub.review.dto.BulkAssignRequest;
import com.atci.quizhub.review.dto.EligibleReviewerResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/mcqs")
public class AdminMcqController {

    private final McqRepository mcqs;
    private final McqService mcqService;
    private final AdminMcqService adminService;
    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public AdminMcqController(McqRepository mcqs, McqService mcqService, AdminMcqService adminService,
                              ReviewService reviewService, CurrentUser currentUser) {
        this.mcqs = mcqs; this.mcqService = mcqService; this.adminService = adminService;
        this.reviewService = reviewService; this.currentUser = currentUser;
    }

    @GetMapping
    public Page<McqResponse> all(Pageable pageable) {
        return mcqs.findAll(pageable).map(m -> McqResponse.from(m, null));
    }

    @PutMapping("/{id}")
    public McqResponse superEdit(@PathVariable Long id, @Valid @RequestBody McqRequest req) {
        return mcqService.update(id, req, currentUser.get().getEnterpriseId());
    }

    @GetMapping("/{mcqId}/eligible-reviewers")
    public List<EligibleReviewerResponse> eligible(@PathVariable Long mcqId) {
        return adminService.eligibleReviewers(mcqId);
    }

    @PostMapping("/{mcqId}/assign")
    public void assign(@PathVariable Long mcqId, @Valid @RequestBody AssignRequest req) {
        reviewService.assign(mcqId, req.reviewerEnterpriseId(), currentUser.get().getEnterpriseId());
    }

    @PostMapping("/bulk-eligible-reviewers")
    public List<EligibleReviewerResponse> bulkEligible(@Valid @RequestBody IdListRequest req) {
        return adminService.commonEligibleReviewers(req.ids());
    }

    @PostMapping("/bulk-assign")
    public List<BulkActionResult> bulkAssign(@Valid @RequestBody BulkAssignRequest req) {
        return adminService.bulkAssign(req.ids(), req.reviewerEnterpriseId(),
                currentUser.get().getEnterpriseId());
    }
}
