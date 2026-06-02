package com.atci.quizhub.mcq;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.dto.*;
import com.atci.quizhub.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class McqService {

    private final McqRepository mcqs;
    private final TechStackRepository stacks;
    private final TopicRepository topics;
    private final UserRepository users;
    private final McqLifecycle lifecycle;

    public McqService(McqRepository mcqs, TechStackRepository stacks, TopicRepository topics,
                      UserRepository users, McqLifecycle lifecycle) {
        this.mcqs = mcqs; this.stacks = stacks; this.topics = topics;
        this.users = users; this.lifecycle = lifecycle;
    }

    public McqResponse create(McqRequest req, String creatorEnterpriseId) {
        User creator = users.findByEnterpriseId(creatorEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        Mcq m = new Mcq();
        applyFields(m, req);
        m.setCreator(creator);
        m.setStatus(req.mode() == SaveMode.SAVE_AND_SEND
                ? lifecycle.afterSendForReview(McqStatus.DRAFT)
                : McqStatus.DRAFT);
        return McqResponse.from(mcqs.save(m), null);
    }

    public McqResponse update(Long id, McqRequest req, String editorEnterpriseId) {
        Mcq m = getEntity(id);
        User editor = users.findByEnterpriseId(editorEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        boolean isCreator = m.getCreator().getEnterpriseId().equals(editorEnterpriseId);
        boolean isAdmin = editor.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new ForbiddenException("Only the creator or an admin may edit this MCQ");
        }
        // Spec: a Draft is accessible ONLY to its creator — even an admin cannot edit
        // someone else's Draft. Admins may edit any OTHER state as super-users.
        if (m.getStatus() == McqStatus.DRAFT && !isCreator) {
            throw new ForbiddenException("Draft MCQs are accessible only to their creator");
        }
        // For non-admins, only Draft/Rejected are editable at all.
        if (!lifecycle.isEditableByCreator(m.getStatus()) && !isAdmin) {
            throw new ForbiddenException("MCQ in status " + m.getStatus() + " is not editable");
        }
        applyFields(m, req);
        if (req.mode() == SaveMode.SAVE_AND_SEND) {
            m.setStatus(lifecycle.afterSendForReview(m.getStatus()));
        }
        return McqResponse.from(mcqs.save(m), null);
    }

    /**
     * Transition a single MCQ to Ready-for-Review WITHOUT editing its fields.
     * Used by the SME "Send for Review" action (single and bulk).
     */
    public McqResponse sendForReview(Long id, String editorEnterpriseId) {
        Mcq m = getEntity(id);
        User editor = users.findByEnterpriseId(editorEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        boolean isCreator = m.getCreator().getEnterpriseId().equals(editorEnterpriseId);
        boolean isAdmin = editor.getRole() == Role.ADMIN;
        if (!isCreator && !isAdmin) {
            throw new ForbiddenException("Only the creator or an admin may send this MCQ for review");
        }
        m.setStatus(lifecycle.afterSendForReview(m.getStatus())); // throws if not Draft/Rejected
        return McqResponse.from(mcqs.save(m), null);
    }

    /** Bulk variant: send many MCQs for review, returning a per-item pass/fail result. */
    public java.util.List<com.atci.quizhub.mcq.dto.BulkActionResult> bulkSendForReview(
            java.util.List<Long> ids, String editorEnterpriseId) {
        java.util.List<com.atci.quizhub.mcq.dto.BulkActionResult> results = new java.util.ArrayList<>();
        for (Long id : ids) {
            try {
                sendForReview(id, editorEnterpriseId);
                results.add(new com.atci.quizhub.mcq.dto.BulkActionResult(id, true, "Sent for review"));
            } catch (RuntimeException e) {
                results.add(new com.atci.quizhub.mcq.dto.BulkActionResult(id, false, e.getMessage()));
            }
        }
        return results;
    }

    public Page<McqResponse> myQuestions(String creatorEnterpriseId, Pageable pageable) {
        User creator = users.findByEnterpriseId(creatorEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return mcqs.findByCreatorId(creator.getId(), pageable)
                .map(m -> McqResponse.from(m, null));
    }

    public Mcq getEntity(Long id) {
        return mcqs.findById(id).orElseThrow(() -> new NotFoundException("MCQ not found: " + id));
    }

    public Mcq saveEntity(Mcq m) { return mcqs.save(m); }

    private void applyFields(Mcq m, McqRequest req) {
        TechStack stack = stacks.findById(req.stackId())
                .orElseThrow(() -> new NotFoundException("Stack not found: " + req.stackId()));
        Topic topic = topics.findById(req.topicId())
                .orElseThrow(() -> new NotFoundException("Topic not found: " + req.topicId()));
        m.setQuestionStem(req.questionStem());
        m.setOptionA(req.optionA()); m.setOptionB(req.optionB());
        m.setOptionC(req.optionC()); m.setOptionD(req.optionD());
        m.setCorrectAnswer(req.correctAnswer());
        m.setDifficulty(req.difficulty());
        m.setStack(stack); m.setTopic(topic);
    }
}
