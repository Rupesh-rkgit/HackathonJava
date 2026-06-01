package com.atci.quizhub.masterdata;

import jakarta.persistence.*;

@Entity
@Table(name = "topic")
public class Topic {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @ManyToOne(optional = false)
    @JoinColumn(name = "stack_id")
    private TechStack stack;

    protected Topic() {}
    public Topic(String name, TechStack stack) { this.name = name; this.stack = stack; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public TechStack getStack() { return stack; }
}
