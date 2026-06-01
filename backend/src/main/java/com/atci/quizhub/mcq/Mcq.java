package com.atci.quizhub.mcq;

import com.atci.quizhub.masterdata.TechStack;
import com.atci.quizhub.masterdata.Topic;
import com.atci.quizhub.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "mcq")
public class Mcq {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 2000)
    private String questionStem;
    @Column(nullable = false, length = 1000) private String optionA;
    @Column(nullable = false, length = 1000) private String optionB;
    @Column(nullable = false, length = 1000) private String optionC;
    @Column(nullable = false, length = 1000) private String optionD;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private AnswerOption correctAnswer;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private Difficulty difficulty;
    @ManyToOne(optional = false) @JoinColumn(name = "stack_id")
    private TechStack stack;
    @ManyToOne(optional = false) @JoinColumn(name = "topic_id")
    private Topic topic;
    @ManyToOne(optional = false) @JoinColumn(name = "creator_id")
    private User creator;
    @Enumerated(EnumType.STRING) @Column(nullable = false)
    private McqStatus status;
    private Instant createdAt;
    private Instant updatedAt;

    public Mcq() {}

    public Long getId() { return id; }
    public String getQuestionStem() { return questionStem; }
    public void setQuestionStem(String v) { this.questionStem = v; }
    public String getOptionA() { return optionA; } public void setOptionA(String v) { this.optionA = v; }
    public String getOptionB() { return optionB; } public void setOptionB(String v) { this.optionB = v; }
    public String getOptionC() { return optionC; } public void setOptionC(String v) { this.optionC = v; }
    public String getOptionD() { return optionD; } public void setOptionD(String v) { this.optionD = v; }
    public AnswerOption getCorrectAnswer() { return correctAnswer; } public void setCorrectAnswer(AnswerOption v) { this.correctAnswer = v; }
    public Difficulty getDifficulty() { return difficulty; } public void setDifficulty(Difficulty v) { this.difficulty = v; }
    public TechStack getStack() { return stack; } public void setStack(TechStack v) { this.stack = v; }
    public Topic getTopic() { return topic; } public void setTopic(Topic v) { this.topic = v; }
    public User getCreator() { return creator; } public void setCreator(User v) { this.creator = v; }
    public McqStatus getStatus() { return status; } public void setStatus(McqStatus v) { this.status = v; }
    public Instant getCreatedAt() { return createdAt; } public void setCreatedAt(Instant v) { this.createdAt = v; }
    public Instant getUpdatedAt() { return updatedAt; } public void setUpdatedAt(Instant v) { this.updatedAt = v; }

    @PrePersist void onCreate() { this.createdAt = Instant.now(); this.updatedAt = this.createdAt; }
    @PreUpdate  void onUpdate() { this.updatedAt = Instant.now(); }
}
