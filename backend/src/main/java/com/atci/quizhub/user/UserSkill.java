package com.atci.quizhub.user;

import com.atci.quizhub.masterdata.TechStack;
import jakarta.persistence.*;

@Entity
@Table(name = "user_skill")
public class UserSkill {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(optional = false) @JoinColumn(name = "user_id")
    private User user;
    @ManyToOne(optional = false) @JoinColumn(name = "stack_id")
    private TechStack stack;

    protected UserSkill() {}
    public UserSkill(User user, TechStack stack) { this.user = user; this.stack = stack; }
    public Long getId() { return id; }
    public User getUser() { return user; }
    public TechStack getStack() { return stack; }
}
