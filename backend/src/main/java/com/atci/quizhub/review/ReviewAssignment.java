package com.atci.quizhub.review;

import com.atci.quizhub.mcq.Mcq;
import com.atci.quizhub.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "review_assignment")
public class ReviewAssignment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "mcq_id")
    private Mcq mcq;
    @ManyToOne(optional = false) @JoinColumn(name = "reviewer_id")
    private User reviewer;
    @ManyToOne(optional = false) @JoinColumn(name = "assigned_by_id")
    private User assignedBy;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private ReviewOutcome outcome;
    @Column(length = 2000)
    private String comments;
    private Instant assignedAt;
    private Instant decidedAt;

    protected ReviewAssignment() {}
    public ReviewAssignment(Mcq mcq, User reviewer, User assignedBy) {
        this.mcq = mcq; this.reviewer = reviewer; this.assignedBy = assignedBy;
        this.outcome = ReviewOutcome.PENDING; this.assignedAt = Instant.now();
    }
    public Long getId() { return id; }
    public Mcq getMcq() { return mcq; }
    public User getReviewer() { return reviewer; }
    public User getAssignedBy() { return assignedBy; }
    public ReviewOutcome getOutcome() { return outcome; }
    public void setOutcome(ReviewOutcome o) { this.outcome = o; }
    public String getComments() { return comments; }
    public void setComments(String c) { this.comments = c; }
    public Instant getAssignedAt() { return assignedAt; }
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant d) { this.decidedAt = d; }
}
