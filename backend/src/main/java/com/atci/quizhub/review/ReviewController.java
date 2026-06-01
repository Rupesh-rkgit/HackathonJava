package com.atci.quizhub.review;

import com.atci.quizhub.auth.CurrentUser;
import com.atci.quizhub.mcq.Mcq;
import com.atci.quizhub.mcq.dto.McqResponse;
import com.atci.quizhub.review.dto.RejectRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public ReviewController(ReviewService reviewService, CurrentUser currentUser) {
        this.reviewService = reviewService; this.currentUser = currentUser;
    }

    @GetMapping("/pending")
    public Page<McqResponse> pending(Pageable pageable) {
        return reviewService.pendingFor(currentUser.get().getEnterpriseId(), pageable)
                .map(a -> { Mcq m = a.getMcq(); return McqResponse.from(m, null); });
    }

    @PostMapping("/{mcqId}/approve")
    public void approve(@PathVariable Long mcqId) {
        reviewService.approve(mcqId, currentUser.get().getEnterpriseId());
    }

    @PostMapping("/{mcqId}/reject")
    public void reject(@PathVariable Long mcqId, @Valid @RequestBody RejectRequest req) {
        reviewService.reject(mcqId, currentUser.get().getEnterpriseId(), req.comments());
    }
}
