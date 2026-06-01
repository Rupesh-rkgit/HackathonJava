package com.atci.quizhub.masterdata;

import jakarta.persistence.*;

@Entity
@Table(name = "tech_stack")
public class TechStack {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String name;

    protected TechStack() {}
    public TechStack(String name) { this.name = name; }
    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
}
