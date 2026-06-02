package com.atci.quizhub.mcq;

import com.atci.quizhub.auth.CurrentUser;
import com.atci.quizhub.mcq.dto.BulkActionResult;
import com.atci.quizhub.mcq.dto.IdListRequest;
import com.atci.quizhub.mcq.dto.McqRequest;
import com.atci.quizhub.mcq.dto.McqResponse;
import com.atci.quizhub.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mcqs")
public class McqController {

    private final McqService service;
    private final ReviewService reviewService;
    private final CurrentUser currentUser;

    public McqController(McqService service, ReviewService reviewService, CurrentUser currentUser) {
        this.service = service; this.reviewService = reviewService; this.currentUser = currentUser;
    }

    @GetMapping("/mine")
    public Page<McqResponse> mine(Pageable pageable) {
        return service.myQuestions(currentUser.get().getEnterpriseId(), pageable);
    }

    @PostMapping
    public McqResponse create(@Valid @RequestBody McqRequest req) {
        return service.create(req, currentUser.get().getEnterpriseId());
    }

    @PostMapping("/bulk-send-for-review")
    public List<BulkActionResult> bulkSend(@Valid @RequestBody IdListRequest req) {
        return service.bulkSendForReview(req.ids(), currentUser.get().getEnterpriseId());
    }

    @PutMapping("/{id}")
    public McqResponse update(@PathVariable Long id, @Valid @RequestBody McqRequest req) {
        return service.update(id, req, currentUser.get().getEnterpriseId());
    }

    @GetMapping("/{id}")
    public McqResponse get(@PathVariable Long id) {
        Mcq m = service.getEntity(id);
        String comments = m.getStatus() == McqStatus.REJECTED
                ? reviewService.latestComments(id) : null;
        return McqResponse.from(m, comments);
    }
}
