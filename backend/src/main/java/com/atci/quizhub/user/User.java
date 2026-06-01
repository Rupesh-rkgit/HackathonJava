package com.atci.quizhub.user;

import jakarta.persistence.*;

@Entity
@Table(name = "app_user")
public class User {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, unique = true)
    private String enterpriseId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String passwordHash;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    protected User() {}
    public User(String enterpriseId, String name, String passwordHash, Role role) {
        this.enterpriseId = enterpriseId; this.name = name;
        this.passwordHash = passwordHash; this.role = role;
    }
    public Long getId() { return id; }
    public String getEnterpriseId() { return enterpriseId; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public Role getRole() { return role; }
}
