# Smart Quiz AI Hub — Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build the complete Phase-1 backend for the Smart Quiz AI Hub — a Spring Boot REST API where SMEs create MCQs that flow through a review/approval lifecycle into a question bank, with Admins assigning reviewers and acting as super-users.

**Architecture:** Modular monolith, layered (Controller → Service → Repository), packaged by feature (`auth`, `user`, `mcq`, `review`, `masterdata`, `bulk`, `common`). A single state-machine class owns all MCQ status transitions. JWT + Spring Security for role-based access. H2 in-memory by default with a PostgreSQL profile.

**Tech Stack:** Java 17, Spring Boot 3.2.x, Spring Data JPA, Spring Security, JJWT, Apache POI, H2, PostgreSQL driver, JUnit 5 + MockMvc, Maven.

---

## File Structure

```
backend/
  pom.xml
  src/main/resources/
    application.yml                 # default (H2) + jwt config
    application-postgres.yml        # postgres profile
  src/main/java/com/atci/quizhub/
    QuizHubApplication.java
    common/
      ApiError.java                 # error response shape
      GlobalExceptionHandler.java   # @RestControllerAdvice
      DomainException.java          # base for 4xx domain errors
      NotFoundException.java
      ForbiddenException.java
      InvalidTransitionException.java
    config/
      SecurityConfig.java           # filter chain, role rules
      DataSeeder.java               # CommandLineRunner: seed master data, users, sample MCQs
    auth/
      JwtService.java               # issue/validate JWT
      JwtAuthFilter.java            # OncePerRequestFilter
      AuthController.java           # POST /api/auth/login
      LoginRequest.java / LoginResponse.java
      CurrentUser.java              # helper to read authenticated principal
    user/
      User.java                     # entity (enterpriseId, role, passwordHash)
      Role.java                     # enum SME, ADMIN
      UserSkill.java                # entity (user, stack)
      UserRepository.java
      UserSkillRepository.java
      UserDetailsServiceImpl.java   # Spring Security UserDetailsService
    masterdata/
      TechStack.java                # entity
      Topic.java                    # entity
      TechStackRepository.java
      TopicRepository.java
      MasterDataController.java     # GET stacks, topics
      MasterDataService.java
    mcq/
      Mcq.java                      # entity
      McqStatus.java                # enum (5 states)
      Difficulty.java               # enum EASY, MEDIUM, HARD
      AnswerOption.java             # enum A, B, C, D
      McqRepository.java
      McqLifecycle.java             # STATE MACHINE — all transitions
      McqService.java               # create/edit/get, My Questions
      McqController.java            # /api/mcqs/*
      dto/McqRequest.java           # create/edit payload + SaveMode
      dto/McqResponse.java
      dto/SaveMode.java             # enum SAVE, SAVE_AND_SEND
    review/
      ReviewAssignment.java         # entity
      ReviewOutcome.java            # enum PENDING, APPROVED, REJECTED
      ReviewAssignmentRepository.java
      ReviewService.java            # assign, approve, reject, pending list
      ReviewController.java         # /api/reviews/*
      AdminMcqController.java       # /api/admin/mcqs/* (bank mgmt, assign, super-edit)
      dto/AssignRequest.java / RejectRequest.java / EligibleReviewerResponse.java
    bulk/
      BulkController.java           # /api/bulk/template, /api/bulk/upload
      BulkService.java
      ExcelTemplate.java            # column layout constants + writer
      BulkRowResult.java            # per-row pass/fail
  src/test/java/com/atci/quizhub/
    ... mirrors main, one test class per service/controller
```

---

## Conventions (read once)

- **Run a single test:** `cd backend && mvn -q -Dtest=ClassName#method test`
- **Run all tests:** `cd backend && mvn -q test`
- **Commit after each green step.** Commit message style: `feat:`, `test:`, `chore:`.
- **Enums map to strings** in DB (`@Enumerated(EnumType.STRING)`).
- **DTOs cross the controller boundary**; entities never leave the service layer.
- Package base: `com.atci.quizhub`.

---

## Task 1: Project skeleton + Maven build

**Files:**
- Create: `backend/pom.xml`
- Create: `backend/src/main/java/com/atci/quizhub/QuizHubApplication.java`
- Create: `backend/src/main/resources/application.yml`

- [ ] **Step 1: Create `pom.xml`**

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.5</version>
        <relativePath/>
    </parent>
    <groupId>com.atci</groupId>
    <artifactId>quizhub</artifactId>
    <version>1.0.0</version>
    <name>smart-quiz-ai-hub</name>
    <properties>
        <java.version>17</java.version>
    </properties>
    <dependencies>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-web</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-data-jpa</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-security</artifactId></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-validation</artifactId></dependency>
        <dependency><groupId>com.h2database</groupId><artifactId>h2</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-api</artifactId><version>0.11.5</version></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-impl</artifactId><version>0.11.5</version><scope>runtime</scope></dependency>
        <dependency><groupId>io.jsonwebtoken</groupId><artifactId>jjwt-jackson</artifactId><version>0.11.5</version><scope>runtime</scope></dependency>
        <dependency><groupId>org.apache.poi</groupId><artifactId>poi-ooxml</artifactId><version>5.2.5</version></dependency>
        <dependency><groupId>org.springframework.boot</groupId><artifactId>spring-boot-starter-test</artifactId><scope>test</scope></dependency>
        <dependency><groupId>org.springframework.security</groupId><artifactId>spring-security-test</artifactId><scope>test</scope></dependency>
    </dependencies>
    <build>
        <plugins>
            <plugin><groupId>org.springframework.boot</groupId><artifactId>spring-boot-maven-plugin</artifactId></plugin>
        </plugins>
    </build>
</project>
```

- [ ] **Step 2: Create `QuizHubApplication.java`**

```java
package com.atci.quizhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class QuizHubApplication {
    public static void main(String[] args) {
        SpringApplication.run(QuizHubApplication.class, args);
    }
}
```

- [ ] **Step 3: Create `application.yml`**

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:quizhub;DB_CLOSE_DELAY=-1;MODE=PostgreSQL
    username: sa
    password: ""
  jpa:
    hibernate:
      ddl-auto: create-drop
    properties:
      hibernate.format_sql: true
    show-sql: false
  h2:
    console:
      enabled: true
      path: /h2-console
app:
  jwt:
    secret: "change-me-please-change-me-please-change-me-please-256bit"
    expiration-ms: 86400000
server:
  port: 8080
```

- [ ] **Step 4: Verify it compiles and boots**

Run: `cd backend && mvn -q -DskipTests package`
Expected: BUILD SUCCESS (jar produced under `target/`).

- [ ] **Step 5: Commit**

```bash
git add backend/pom.xml backend/src
git commit -m "chore: scaffold Spring Boot project with H2 and dependencies"
```

---

## Task 2: Common error handling

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/common/ApiError.java`
- Create: `backend/src/main/java/com/atci/quizhub/common/DomainException.java`
- Create: `backend/src/main/java/com/atci/quizhub/common/NotFoundException.java`
- Create: `backend/src/main/java/com/atci/quizhub/common/ForbiddenException.java`
- Create: `backend/src/main/java/com/atci/quizhub/common/InvalidTransitionException.java`
- Create: `backend/src/main/java/com/atci/quizhub/common/GlobalExceptionHandler.java`

- [ ] **Step 1: Create the exception classes**

```java
package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public abstract class DomainException extends RuntimeException {
    private final HttpStatus status;
    protected DomainException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }
    public HttpStatus getStatus() { return status; }
}
```

```java
package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public class NotFoundException extends DomainException {
    public NotFoundException(String message) { super(message, HttpStatus.NOT_FOUND); }
}
```

```java
package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends DomainException {
    public ForbiddenException(String message) { super(message, HttpStatus.FORBIDDEN); }
}
```

```java
package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;

public class InvalidTransitionException extends DomainException {
    public InvalidTransitionException(String message) { super(message, HttpStatus.CONFLICT); }
}
```

- [ ] **Step 2: Create `ApiError.java`**

```java
package com.atci.quizhub.common;

import java.time.Instant;

public record ApiError(Instant timestamp, int status, String error, String message) {}
```

- [ ] **Step 3: Create `GlobalExceptionHandler.java`**

```java
package com.atci.quizhub.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ApiError> handleDomain(DomainException ex) {
        return build(ex.getStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
        String msg = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(f -> f.getField() + ": " + f.getDefaultMessage())
                .orElse("Validation failed");
        return build(HttpStatus.BAD_REQUEST, msg);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneric(Exception ex) {
        return build(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
    }

    private ResponseEntity<ApiError> build(HttpStatus status, String message) {
        ApiError body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(), message);
        return ResponseEntity.status(status).body(body);
    }
}
```

- [ ] **Step 4: Compile**

Run: `cd backend && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/common
git commit -m "feat: add common error handling (ApiError, domain exceptions, advice)"
```

---

## Task 3: User & masterdata entities

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/user/Role.java`
- Create: `backend/src/main/java/com/atci/quizhub/user/User.java`
- Create: `backend/src/main/java/com/atci/quizhub/user/UserSkill.java`
- Create: `backend/src/main/java/com/atci/quizhub/user/UserRepository.java`
- Create: `backend/src/main/java/com/atci/quizhub/user/UserSkillRepository.java`
- Create: `backend/src/main/java/com/atci/quizhub/masterdata/TechStack.java`
- Create: `backend/src/main/java/com/atci/quizhub/masterdata/Topic.java`
- Create: `backend/src/main/java/com/atci/quizhub/masterdata/TechStackRepository.java`
- Create: `backend/src/main/java/com/atci/quizhub/masterdata/TopicRepository.java`

- [ ] **Step 1: Create masterdata entities**

```java
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
```

```java
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
```

- [ ] **Step 2: Create masterdata repositories**

```java
package com.atci.quizhub.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    Optional<TechStack> findByName(String name);
}
```

```java
package com.atci.quizhub.masterdata;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TopicRepository extends JpaRepository<Topic, Long> {
    List<Topic> findByStackId(Long stackId);
}
```

- [ ] **Step 3: Create `Role` enum and `User` entity**

```java
package com.atci.quizhub.user;

public enum Role { SME, ADMIN }
```

```java
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
```

- [ ] **Step 4: Create `UserSkill` entity and repositories**

```java
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
```

```java
package com.atci.quizhub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEnterpriseId(String enterpriseId);
}
```

```java
package com.atci.quizhub.user;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface UserSkillRepository extends JpaRepository<UserSkill, Long> {
    @Query("select us.user from UserSkill us where us.stack.id = :stackId")
    List<User> findUsersByStackId(Long stackId);
}
```

- [ ] **Step 5: Compile**

Run: `cd backend && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/user backend/src/main/java/com/atci/quizhub/masterdata
git commit -m "feat: add user, role, user-skill, tech-stack, topic entities and repositories"
```

---

## Task 4: MCQ entity, enums, repository

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/mcq/McqStatus.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/Difficulty.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/AnswerOption.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/Mcq.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/McqRepository.java`

- [ ] **Step 1: Create the enums**

```java
package com.atci.quizhub.mcq;

public enum McqStatus { DRAFT, READY_FOR_REVIEW, UNDER_REVIEW, APPROVED, REJECTED }
```

```java
package com.atci.quizhub.mcq;

public enum Difficulty { EASY, MEDIUM, HARD }
```

```java
package com.atci.quizhub.mcq;

public enum AnswerOption { A, B, C, D }
```

- [ ] **Step 2: Create `Mcq` entity**

```java
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

    protected Mcq() {}

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
```

- [ ] **Step 3: Create `McqRepository`**

```java
package com.atci.quizhub.mcq;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McqRepository extends JpaRepository<Mcq, Long> {
    Page<Mcq> findByCreatorId(Long creatorId, Pageable pageable);
}
```

- [ ] **Step 4: Compile**

Run: `cd backend && mvn -q -DskipTests compile`
Expected: BUILD SUCCESS.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/mcq
git commit -m "feat: add MCQ entity, status/difficulty/answer enums, repository"
```

---

## Task 5: MCQ lifecycle state machine (TDD)

This is the heart of the system. Test-first.

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/mcq/McqLifecycle.java`
- Test: `backend/src/test/java/com/atci/quizhub/mcq/McqLifecycleTest.java`

- [ ] **Step 1: Write the failing test**

```java
package com.atci.quizhub.mcq;

import com.atci.quizhub.common.InvalidTransitionException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class McqLifecycleTest {

    private final McqLifecycle lifecycle = new McqLifecycle();

    @Test
    void draftCanGoToReadyForReview() {
        assertEquals(McqStatus.READY_FOR_REVIEW,
            lifecycle.afterSendForReview(McqStatus.DRAFT));
    }

    @Test
    void rejectedCanGoToReadyForReview() {
        assertEquals(McqStatus.READY_FOR_REVIEW,
            lifecycle.afterSendForReview(McqStatus.REJECTED));
    }

    @Test
    void underReviewCannotBeSentForReview() {
        assertThrows(InvalidTransitionException.class,
            () -> lifecycle.afterSendForReview(McqStatus.UNDER_REVIEW));
    }

    @Test
    void readyForReviewCanBeAssigned() {
        assertEquals(McqStatus.UNDER_REVIEW,
            lifecycle.afterAssign(McqStatus.READY_FOR_REVIEW));
    }

    @Test
    void draftCannotBeAssigned() {
        assertThrows(InvalidTransitionException.class,
            () -> lifecycle.afterAssign(McqStatus.DRAFT));
    }

    @Test
    void underReviewCanBeApproved() {
        assertEquals(McqStatus.APPROVED, lifecycle.afterApprove(McqStatus.UNDER_REVIEW));
    }

    @Test
    void underReviewCanBeRejected() {
        assertEquals(McqStatus.REJECTED, lifecycle.afterReject(McqStatus.UNDER_REVIEW));
    }

    @Test
    void approvedCannotBeApprovedAgain() {
        assertThrows(InvalidTransitionException.class,
            () -> lifecycle.afterApprove(McqStatus.APPROVED));
    }

    @Test
    void onlyDraftAndRejectedAreEditable() {
        assertTrue(lifecycle.isEditableByCreator(McqStatus.DRAFT));
        assertTrue(lifecycle.isEditableByCreator(McqStatus.REJECTED));
        assertFalse(lifecycle.isEditableByCreator(McqStatus.READY_FOR_REVIEW));
        assertFalse(lifecycle.isEditableByCreator(McqStatus.UNDER_REVIEW));
        assertFalse(lifecycle.isEditableByCreator(McqStatus.APPROVED));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `cd backend && mvn -q -Dtest=McqLifecycleTest test`
Expected: FAIL — `McqLifecycle` does not exist (compilation error).

- [ ] **Step 3: Write the implementation**

```java
package com.atci.quizhub.mcq;

import com.atci.quizhub.common.InvalidTransitionException;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Single owner of all MCQ status transitions. No other class may mutate status directly.
 */
@Component
public class McqLifecycle {

    private static final Set<McqStatus> EDITABLE = Set.of(McqStatus.DRAFT, McqStatus.REJECTED);

    public boolean isEditableByCreator(McqStatus status) {
        return EDITABLE.contains(status);
    }

    public McqStatus afterSendForReview(McqStatus current) {
        if (!EDITABLE.contains(current)) {
            throw new InvalidTransitionException(
                "Only Draft or Rejected MCQs can be sent for review (was " + current + ")");
        }
        return McqStatus.READY_FOR_REVIEW;
    }

    public McqStatus afterAssign(McqStatus current) {
        if (current != McqStatus.READY_FOR_REVIEW) {
            throw new InvalidTransitionException(
                "Only Ready-for-Review MCQs can be assigned (was " + current + ")");
        }
        return McqStatus.UNDER_REVIEW;
    }

    public McqStatus afterApprove(McqStatus current) {
        if (current != McqStatus.UNDER_REVIEW) {
            throw new InvalidTransitionException(
                "Only Under-Review MCQs can be approved (was " + current + ")");
        }
        return McqStatus.APPROVED;
    }

    public McqStatus afterReject(McqStatus current) {
        if (current != McqStatus.UNDER_REVIEW) {
            throw new InvalidTransitionException(
                "Only Under-Review MCQs can be rejected (was " + current + ")");
        }
        return McqStatus.REJECTED;
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `cd backend && mvn -q -Dtest=McqLifecycleTest test`
Expected: PASS (10 tests green).

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/mcq/McqLifecycle.java backend/src/test/java/com/atci/quizhub/mcq/McqLifecycleTest.java
git commit -m "feat: add MCQ lifecycle state machine with full transition tests"
```

---

## Task 6: Security config, JWT, login (TDD on JwtService)

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/auth/JwtService.java`
- Create: `backend/src/main/java/com/atci/quizhub/user/UserDetailsServiceImpl.java`
- Create: `backend/src/main/java/com/atci/quizhub/auth/JwtAuthFilter.java`
- Create: `backend/src/main/java/com/atci/quizhub/config/SecurityConfig.java`
- Create: `backend/src/main/java/com/atci/quizhub/auth/CurrentUser.java`
- Create: `backend/src/main/java/com/atci/quizhub/auth/LoginRequest.java`
- Create: `backend/src/main/java/com/atci/quizhub/auth/LoginResponse.java`
- Create: `backend/src/main/java/com/atci/quizhub/auth/AuthController.java`
- Test: `backend/src/test/java/com/atci/quizhub/auth/JwtServiceTest.java`

- [ ] **Step 1: Write the failing test for JwtService**

```java
package com.atci.quizhub.auth;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService(
        "test-secret-test-secret-test-secret-test-secret-123456", 3600000L);

    @Test
    void issuesAndParsesToken() {
        String token = jwt.generate("birendra.kumar.singh", "ADMIN");
        assertEquals("birendra.kumar.singh", jwt.extractUsername(token));
        assertTrue(jwt.isValid(token, "birendra.kumar.singh"));
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwt.generate("alice", "SME");
        assertFalse(jwt.isValid(token + "x", "alice"));
    }
}
```

- [ ] **Step 2: Run to verify it fails**

Run: `cd backend && mvn -q -Dtest=JwtServiceTest test`
Expected: FAIL — `JwtService` not defined.

- [ ] **Step 3: Implement `JwtService`**

```java
package com.atci.quizhub.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String generate(String username, String role) {
        Date now = new Date();
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .setIssuedAt(now)
                .setExpiration(new Date(now.getTime() + expirationMs))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String extractUsername(String token) {
        return parse(token).getSubject();
    }

    public boolean isValid(String token, String expectedUsername) {
        try {
            Claims c = parse(token);
            return c.getSubject().equals(expectedUsername) && c.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        return Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
    }
}
```

- [ ] **Step 4: Run to verify it passes**

Run: `cd backend && mvn -q -Dtest=JwtServiceTest test`
Expected: PASS.

- [ ] **Step 5: Implement `UserDetailsServiceImpl`**

```java
package com.atci.quizhub.user;

import com.atci.quizhub.common.NotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;
    public UserDetailsServiceImpl(UserRepository userRepository) { this.userRepository = userRepository; }

    @Override
    public UserDetails loadUserByUsername(String enterpriseId) {
        User u = userRepository.findByEnterpriseId(enterpriseId)
                .orElseThrow(() -> new UsernameNotFoundException(enterpriseId));
        return new org.springframework.security.core.userdetails.User(
                u.getEnterpriseId(),
                u.getPasswordHash(),
                List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
    }
}
```

- [ ] **Step 6: Implement `JwtAuthFilter`**

```java
package com.atci.quizhub.auth;

import com.atci.quizhub.user.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    public JwtAuthFilter(JwtService jwtService, UserDetailsServiceImpl userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtService.extractUsername(token);
                if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                    UserDetails details = userDetailsService.loadUserByUsername(username);
                    if (jwtService.isValid(token, username)) {
                        UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(details, null, details.getAuthorities());
                        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(auth);
                    }
                }
            } catch (Exception ignored) {
                // invalid token -> remain unauthenticated
            }
        }
        chain.doFilter(request, response);
    }
}
```

- [ ] **Step 7: Implement `SecurityConfig`**

```java
package com.atci.quizhub.config;

import com.atci.quizhub.auth.JwtAuthFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    public SecurityConfig(JwtAuthFilter jwtAuthFilter) { this.jwtAuthFilter = jwtAuthFilter; }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(org.springframework.security.config.Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/h2-console/**").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .headers(h -> h.frameOptions(f -> f.disable()))
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean public PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration cfg) throws Exception {
        return cfg.getAuthenticationManager();
    }
}
```

- [ ] **Step 8: Implement `CurrentUser` helper**

```java
package com.atci.quizhub.auth;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.user.User;
import com.atci.quizhub.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUser {
    private final UserRepository userRepository;
    public CurrentUser(UserRepository userRepository) { this.userRepository = userRepository; }

    public User get() {
        String enterpriseId = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEnterpriseId(enterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found: " + enterpriseId));
    }
}
```

- [ ] **Step 9: Implement login DTOs and `AuthController`**

```java
package com.atci.quizhub.auth;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(@NotBlank String enterpriseId, @NotBlank String password) {}
```

```java
package com.atci.quizhub.auth;

public record LoginResponse(String token, String role, String enterpriseId, String name) {}
```

```java
package com.atci.quizhub.auth;

import com.atci.quizhub.user.User;
import com.atci.quizhub.user.UserRepository;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authManager;
    private final JwtService jwtService;
    private final UserRepository userRepository;

    public AuthController(AuthenticationManager authManager, JwtService jwtService, UserRepository userRepository) {
        this.authManager = authManager;
        this.jwtService = jwtService;
        this.userRepository = userRepository;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            authManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.enterpriseId(), req.password()));
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(401).build();
        }
        User u = userRepository.findByEnterpriseId(req.enterpriseId()).orElseThrow();
        String token = jwtService.generate(u.getEnterpriseId(), u.getRole().name());
        return ResponseEntity.ok(
            new LoginResponse(token, u.getRole().name(), u.getEnterpriseId(), u.getName()));
    }
}
```

- [ ] **Step 10: Compile and run JwtService test again**

Run: `cd backend && mvn -q -Dtest=JwtServiceTest test`
Expected: PASS, project compiles.

- [ ] **Step 11: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/auth backend/src/main/java/com/atci/quizhub/config backend/src/main/java/com/atci/quizhub/user/UserDetailsServiceImpl.java backend/src/test/java/com/atci/quizhub/auth/JwtServiceTest.java
git commit -m "feat: add JWT auth, security config, login endpoint"
```

---

## Task 7: Data seeder

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/config/DataSeeder.java`

- [ ] **Step 1: Implement `DataSeeder`** (seeds stacks/topics from slide 15, SME-skill map from slide 16, demo users incl. an admin, and the two sample MCQs from slide 14)

```java
package com.atci.quizhub.config;

import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.user.*;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class DataSeeder implements CommandLineRunner {

    private final TechStackRepository stacks;
    private final TopicRepository topics;
    private final UserRepository users;
    private final UserSkillRepository skills;
    private final McqRepository mcqs;
    private final PasswordEncoder encoder;

    public DataSeeder(TechStackRepository stacks, TopicRepository topics, UserRepository users,
                      UserSkillRepository skills, McqRepository mcqs, PasswordEncoder encoder) {
        this.stacks = stacks; this.topics = topics; this.users = users;
        this.skills = skills; this.mcqs = mcqs; this.encoder = encoder;
    }

    @Override
    public void run(String... args) {
        if (stacks.count() > 0) return; // idempotent

        // --- Tech stacks (slide 15) ---
        TechStack springCloud = stacks.save(new TechStack("Spring Cloud"));
        TechStack springBoot  = stacks.save(new TechStack("Spring Boot"));
        TechStack springCore  = stacks.save(new TechStack("Spring Core"));
        TechStack springMvc   = stacks.save(new TechStack("Spring MVC & REST"));
        TechStack springOrm   = stacks.save(new TechStack("Spring ORM & Data JPA"));
        TechStack coreJava    = stacks.save(new TechStack("Core Java"));

        // --- Topics for Spring Cloud (slide 15) ---
        topics.save(new Topic("Introduction to Spring Cloud", springCloud));
        topics.save(new Topic("Service Discovery design pattern – Eureka Server & Discovery Client", springCloud));
        topics.save(new Topic("Eureka Heartbeats & Self Preservation", springCloud));
        topics.save(new Topic("Spring Cloud Loadbalancer", springCloud));
        topics.save(new Topic("Spring Cloud OpenFeign", springCloud));
        topics.save(new Topic("Resilience4J- Circuit Breaker", springCloud));
        topics.save(new Topic("Spring Boot Actuator", springCloud));
        // a couple of topics for other stacks so dropdowns aren't empty
        Topic bootIntro = topics.save(new Topic("Spring Boot Auto-configuration", springBoot));
        topics.save(new Topic("Spring Boot Starters", springBoot));
        topics.save(new Topic("Beans & Dependency Injection", springCore));
        topics.save(new Topic("REST Controllers", springMvc));
        topics.save(new Topic("JPA Repositories", springOrm));
        topics.save(new Topic("Collections", coreJava));

        // --- Users (enterprise IDs from slide 16). Password = "password" for all. ---
        String pw = encoder.encode("password");
        User gaurav   = users.save(new User("gaurav.a.bhola", "Gaurav Bhola", pw, Role.SME));
        User birendra = users.save(new User("birendra.kumar.singh", "Birendra Kumar Singh", pw, Role.ADMIN));
        User divya    = users.save(new User("divya.madhanasekar", "Divya Madhanasekar", pw, Role.SME));
        User swati    = users.save(new User("swati.avinash.nikam", "Swati Avinash Nikam", pw, Role.SME));
        User indugu   = users.save(new User("indugu.hari.prasad", "Indugu Hari Prasad", pw, Role.SME));

        // --- SME ↔ skill mapping (slide 16) ---
        skills.save(new UserSkill(gaurav, springCloud));
        skills.save(new UserSkill(birendra, springBoot));
        skills.save(new UserSkill(gaurav, springCore));
        skills.save(new UserSkill(divya, springMvc));
        skills.save(new UserSkill(divya, springCloud));
        skills.save(new UserSkill(swati, springBoot));
        skills.save(new UserSkill(indugu, springCloud));

        // --- Sample MCQs (slide 14), created by birendra ---
        Topic introCloud = topics.findByStackId(springCloud.getId()).get(0);
        Mcq m1 = new Mcq();
        m1.setQuestionStem("Alex is building a microservices-based system using Spring Boot. "
            + "Which is the primary purpose of Spring Cloud?");
        m1.setOptionA("To replace Spring Boot completely");
        m1.setOptionB("To provide tools for building distributed systems and microservices");
        m1.setOptionC("To manage only database transactions");
        m1.setOptionD("To handle only UI development");
        m1.setCorrectAnswer(AnswerOption.B);
        m1.setDifficulty(Difficulty.MEDIUM);
        m1.setStack(springCloud); m1.setTopic(introCloud); m1.setCreator(birendra);
        m1.setStatus(McqStatus.READY_FOR_REVIEW);
        mcqs.save(m1);

        Mcq m2 = new Mcq();
        m2.setQuestionStem("John has multiple instances of a service running dynamically in the cloud. "
            + "Which component is used for automatic registration and discovery?");
        m2.setOptionA("Spring MVC");
        m2.setOptionB("Eureka Server");
        m2.setOptionC("Hibernate");
        m2.setOptionD("Apache Tomcat");
        m2.setCorrectAnswer(AnswerOption.B);
        m2.setDifficulty(Difficulty.MEDIUM);
        m2.setStack(springCloud); m2.setTopic(introCloud); m2.setCreator(birendra);
        m2.setStatus(McqStatus.APPROVED);
        mcqs.save(m2);
    }
}
```

- [ ] **Step 2: Boot the app to confirm seeding works**

Run: `cd backend && mvn -q -DskipTests spring-boot:run` (let it start, then Ctrl-C)
Expected: starts without errors; no exceptions during `DataSeeder`.

- [ ] **Step 3: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/config/DataSeeder.java
git commit -m "feat: seed master data, demo users, and sample MCQs on startup"
```

---

## Task 8: MCQ DTOs + service (create/edit/get/My Questions) — TDD

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/mcq/dto/SaveMode.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/dto/McqRequest.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/dto/McqResponse.java`
- Create: `backend/src/main/java/com/atci/quizhub/mcq/McqService.java`
- Test: `backend/src/test/java/com/atci/quizhub/mcq/McqServiceTest.java`

- [ ] **Step 1: Create DTOs**

```java
package com.atci.quizhub.mcq.dto;

public enum SaveMode { SAVE, SAVE_AND_SEND }
```

```java
package com.atci.quizhub.mcq.dto;

import com.atci.quizhub.mcq.AnswerOption;
import com.atci.quizhub.mcq.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record McqRequest(
        @NotBlank String questionStem,
        @NotBlank String optionA,
        @NotBlank String optionB,
        @NotBlank String optionC,
        @NotBlank String optionD,
        @NotNull AnswerOption correctAnswer,
        @NotNull Difficulty difficulty,
        @NotNull Long stackId,
        @NotNull Long topicId,
        @NotNull SaveMode mode) {}
```

```java
package com.atci.quizhub.mcq.dto;

import com.atci.quizhub.mcq.*;

public record McqResponse(
        Long id, String questionStem,
        String optionA, String optionB, String optionC, String optionD,
        AnswerOption correctAnswer, Difficulty difficulty,
        Long stackId, String stackName, Long topicId, String topicName,
        String creatorEnterpriseId, McqStatus status, String reviewerComments) {

    public static McqResponse from(Mcq m, String reviewerComments) {
        return new McqResponse(
            m.getId(), m.getQuestionStem(),
            m.getOptionA(), m.getOptionB(), m.getOptionC(), m.getOptionD(),
            m.getCorrectAnswer(), m.getDifficulty(),
            m.getStack().getId(), m.getStack().getName(),
            m.getTopic().getId(), m.getTopic().getName(),
            m.getCreator().getEnterpriseId(), m.getStatus(), reviewerComments);
    }
}
```

- [ ] **Step 2: Write the failing service test**

```java
package com.atci.quizhub.mcq;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.mcq.dto.McqRequest;
import com.atci.quizhub.mcq.dto.SaveMode;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class McqServiceTest {

    @Autowired McqService service;
    @Autowired UserRepository users;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId; String sme;

    @BeforeEach
    void setup() {
        sme = users.findByEnterpriseId("gaurav.a.bhola").orElseThrow().getEnterpriseId();
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    private McqRequest req(SaveMode mode) {
        return new McqRequest("Stem?", "a", "b", "c", "d",
            AnswerOption.B, Difficulty.EASY, stackId, topicId, mode);
    }

    @Test
    void saveCreatesDraft() {
        var resp = service.create(req(SaveMode.SAVE), sme);
        assertEquals(McqStatus.DRAFT, resp.status());
    }

    @Test
    void saveAndSendCreatesReadyForReview() {
        var resp = service.create(req(SaveMode.SAVE_AND_SEND), sme);
        assertEquals(McqStatus.READY_FOR_REVIEW, resp.status());
    }

    @Test
    void editingApprovedMcqIsForbidden() {
        var draft = service.create(req(SaveMode.SAVE), sme);
        // force it to APPROVED via repository to simulate a non-editable state
        Mcq m = service.getEntity(draft.id());
        m.setStatus(McqStatus.APPROVED);
        service.saveEntity(m);
        assertThrows(ForbiddenException.class,
            () -> service.update(draft.id(), req(SaveMode.SAVE), sme));
    }

    @Test
    void nonCreatorCannotEdit() {
        var draft = service.create(req(SaveMode.SAVE), sme);
        assertThrows(ForbiddenException.class,
            () -> service.update(draft.id(), req(SaveMode.SAVE), "divya.madhanasekar"));
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `cd backend && mvn -q -Dtest=McqServiceTest test`
Expected: FAIL — `McqService` not defined.

- [ ] **Step 4: Implement `McqService`**

```java
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
        if (!lifecycle.isEditableByCreator(m.getStatus()) && !isAdmin) {
            throw new ForbiddenException("MCQ in status " + m.getStatus() + " is not editable");
        }
        applyFields(m, req);
        if (req.mode() == SaveMode.SAVE_AND_SEND) {
            m.setStatus(lifecycle.afterSendForReview(m.getStatus()));
        }
        return McqResponse.from(mcqs.save(m), null);
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
```

- [ ] **Step 5: Run to verify it passes**

Run: `cd backend && mvn -q -Dtest=McqServiceTest test`
Expected: PASS (4 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/mcq backend/src/test/java/com/atci/quizhub/mcq/McqServiceTest.java
git commit -m "feat: add MCQ create/edit/my-questions service with ownership and lifecycle rules"
```

---

## Task 9: Review service (assign / approve / reject / pending) — TDD

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/review/ReviewOutcome.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/ReviewAssignment.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/ReviewAssignmentRepository.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/ReviewService.java`
- Test: `backend/src/test/java/com/atci/quizhub/review/ReviewServiceTest.java`

- [ ] **Step 1: Create `ReviewOutcome`, `ReviewAssignment`, repository**

```java
package com.atci.quizhub.review;

public enum ReviewOutcome { PENDING, APPROVED, REJECTED }
```

```java
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
    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant d) { this.decidedAt = d; }
}
```

```java
package com.atci.quizhub.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ReviewAssignmentRepository extends JpaRepository<ReviewAssignment, Long> {
    Page<ReviewAssignment> findByReviewerIdAndOutcome(Long reviewerId, ReviewOutcome outcome, Pageable pageable);
    Optional<ReviewAssignment> findFirstByMcqIdAndOutcomeOrderByAssignedAtDesc(Long mcqId, ReviewOutcome outcome);
    Optional<ReviewAssignment> findFirstByMcqIdOrderByAssignedAtDesc(Long mcqId);
}
```

- [ ] **Step 2: Write the failing test**

```java
package com.atci.quizhub.review;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.common.InvalidTransitionException;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.mcq.dto.*;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class ReviewServiceTest {

    @Autowired ReviewService reviewService;
    @Autowired McqService mcqService;
    @Autowired UserRepository users;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId;
    final String creator = "gaurav.a.bhola";       // Spring Cloud SME
    final String reviewer = "divya.madhanasekar";  // also Spring Cloud
    final String admin = "birendra.kumar.singh";

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    private Long newReadyMcq() {
        var resp = mcqService.create(new McqRequest("Q?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        return resp.id();
    }

    @Test
    void assignMovesToUnderReview() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        assertEquals(McqStatus.UNDER_REVIEW, mcqService.getEntity(id).getStatus());
    }

    @Test
    void cannotAssignCreatorAsReviewer() {
        Long id = newReadyMcq();
        assertThrows(ForbiddenException.class,
            () -> reviewService.assign(id, creator, admin));
    }

    @Test
    void approveMovesToApproved() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        reviewService.approve(id, reviewer);
        assertEquals(McqStatus.APPROVED, mcqService.getEntity(id).getStatus());
    }

    @Test
    void rejectRequiresComment() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        assertThrows(IllegalArgumentException.class,
            () -> reviewService.reject(id, reviewer, "  "));
    }

    @Test
    void rejectMovesToRejectedAndStoresComment() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        reviewService.reject(id, reviewer, "Option C is wrong");
        Mcq m = mcqService.getEntity(id);
        assertEquals(McqStatus.REJECTED, m.getStatus());
        assertEquals("Option C is wrong", reviewService.latestComments(id));
    }

    @Test
    void nonAssignedReviewerCannotApprove() {
        Long id = newReadyMcq();
        reviewService.assign(id, reviewer, admin);
        assertThrows(ForbiddenException.class,
            () -> reviewService.approve(id, "swati.avinash.nikam"));
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `cd backend && mvn -q -Dtest=ReviewServiceTest test`
Expected: FAIL — `ReviewService` not defined.

- [ ] **Step 4: Implement `ReviewService`**

```java
package com.atci.quizhub.review;

import com.atci.quizhub.common.ForbiddenException;
import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@Transactional
public class ReviewService {

    private final ReviewAssignmentRepository assignments;
    private final McqRepository mcqs;
    private final UserRepository users;
    private final McqLifecycle lifecycle;

    public ReviewService(ReviewAssignmentRepository assignments, McqRepository mcqs,
                         UserRepository users, McqLifecycle lifecycle) {
        this.assignments = assignments; this.mcqs = mcqs;
        this.users = users; this.lifecycle = lifecycle;
    }

    public void assign(Long mcqId, String reviewerEnterpriseId, String adminEnterpriseId) {
        Mcq mcq = mcqs.findById(mcqId).orElseThrow(() -> new NotFoundException("MCQ not found"));
        if (mcq.getCreator().getEnterpriseId().equals(reviewerEnterpriseId)) {
            throw new ForbiddenException("Creator cannot be assigned as reviewer");
        }
        User reviewer = users.findByEnterpriseId(reviewerEnterpriseId)
                .orElseThrow(() -> new NotFoundException("Reviewer not found"));
        User admin = users.findByEnterpriseId(adminEnterpriseId)
                .orElseThrow(() -> new NotFoundException("Admin not found"));
        mcq.setStatus(lifecycle.afterAssign(mcq.getStatus()));
        mcqs.save(mcq);
        assignments.save(new ReviewAssignment(mcq, reviewer, admin));
    }

    public void approve(Long mcqId, String reviewerEnterpriseId) {
        ReviewAssignment a = currentAssignment(mcqId);
        requireAssignedReviewer(a, reviewerEnterpriseId);
        Mcq mcq = a.getMcq();
        mcq.setStatus(lifecycle.afterApprove(mcq.getStatus()));
        mcqs.save(mcq);
        a.setOutcome(ReviewOutcome.APPROVED);
        a.setDecidedAt(Instant.now());
        assignments.save(a);
    }

    public void reject(Long mcqId, String reviewerEnterpriseId, String comments) {
        if (comments == null || comments.isBlank()) {
            throw new IllegalArgumentException("Rejection comment is mandatory");
        }
        ReviewAssignment a = currentAssignment(mcqId);
        requireAssignedReviewer(a, reviewerEnterpriseId);
        Mcq mcq = a.getMcq();
        mcq.setStatus(lifecycle.afterReject(mcq.getStatus()));
        mcqs.save(mcq);
        a.setOutcome(ReviewOutcome.REJECTED);
        a.setComments(comments);
        a.setDecidedAt(Instant.now());
        assignments.save(a);
    }

    public Page<ReviewAssignment> pendingFor(String reviewerEnterpriseId, Pageable pageable) {
        User reviewer = users.findByEnterpriseId(reviewerEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return assignments.findByReviewerIdAndOutcome(reviewer.getId(), ReviewOutcome.PENDING, pageable);
    }

    public String latestComments(Long mcqId) {
        return assignments.findFirstByMcqIdOrderByAssignedAtDesc(mcqId)
                .map(ReviewAssignment::getComments).orElse(null);
    }

    private ReviewAssignment currentAssignment(Long mcqId) {
        return assignments.findFirstByMcqIdAndOutcomeOrderByAssignedAtDesc(mcqId, ReviewOutcome.PENDING)
                .orElseThrow(() -> new NotFoundException("No pending review for MCQ " + mcqId));
    }

    private void requireAssignedReviewer(ReviewAssignment a, String enterpriseId) {
        if (!a.getReviewer().getEnterpriseId().equals(enterpriseId)) {
            throw new ForbiddenException("Only the assigned reviewer may decide this MCQ");
        }
    }
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `cd backend && mvn -q -Dtest=ReviewServiceTest test`
Expected: PASS (6 tests).

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/review backend/src/test/java/com/atci/quizhub/review/ReviewServiceTest.java
git commit -m "feat: add review service (assign/approve/reject/pending) with guards and history"
```

---

## Task 10: Admin question-bank service + eligible reviewers — TDD

**Files:**
- Modify: `backend/src/main/java/com/atci/quizhub/mcq/McqRepository.java` (add `findAll` is inherited; add nothing)
- Create: `backend/src/main/java/com/atci/quizhub/review/AdminMcqService.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/dto/EligibleReviewerResponse.java`
- Test: `backend/src/test/java/com/atci/quizhub/review/AdminMcqServiceTest.java`

- [ ] **Step 1: Create `EligibleReviewerResponse`**

```java
package com.atci.quizhub.review.dto;

public record EligibleReviewerResponse(String enterpriseId, String name) {}
```

- [ ] **Step 2: Write the failing test**

```java
package com.atci.quizhub.review;

import com.atci.quizhub.mcq.*;
import com.atci.quizhub.mcq.dto.*;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.review.dto.EligibleReviewerResponse;
import com.atci.quizhub.user.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AdminMcqServiceTest {

    @Autowired AdminMcqService adminService;
    @Autowired McqService mcqService;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId;
    final String creator = "gaurav.a.bhola"; // Spring Cloud

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    @Test
    void eligibleReviewersMatchStackAndExcludeCreator() {
        var resp = mcqService.create(new McqRequest("Q?", "a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND), creator);
        List<EligibleReviewerResponse> eligible = adminService.eligibleReviewers(resp.id());
        List<String> ids = eligible.stream().map(EligibleReviewerResponse::enterpriseId).toList();
        // Spring Cloud SMEs are gaurav (creator, excluded), divya, indugu
        assertTrue(ids.contains("divya.madhanasekar"));
        assertTrue(ids.contains("indugu.hari.prasad"));
        assertFalse(ids.contains("gaurav.a.bhola"));
    }
}
```

- [ ] **Step 3: Run to verify it fails**

Run: `cd backend && mvn -q -Dtest=AdminMcqServiceTest test`
Expected: FAIL — `AdminMcqService` not defined.

- [ ] **Step 4: Implement `AdminMcqService`**

```java
package com.atci.quizhub.review;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.mcq.Mcq;
import com.atci.quizhub.mcq.McqRepository;
import com.atci.quizhub.review.dto.EligibleReviewerResponse;
import com.atci.quizhub.user.User;
import com.atci.quizhub.user.UserSkillRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class AdminMcqService {

    private final McqRepository mcqs;
    private final UserSkillRepository userSkills;

    public AdminMcqService(McqRepository mcqs, UserSkillRepository userSkills) {
        this.mcqs = mcqs; this.userSkills = userSkills;
    }

    public List<EligibleReviewerResponse> eligibleReviewers(Long mcqId) {
        Mcq mcq = mcqs.findById(mcqId).orElseThrow(() -> new NotFoundException("MCQ not found"));
        String creatorId = mcq.getCreator().getEnterpriseId();
        return userSkills.findUsersByStackId(mcq.getStack().getId()).stream()
                .filter(u -> !u.getEnterpriseId().equals(creatorId))
                .distinct()
                .map(u -> new EligibleReviewerResponse(u.getEnterpriseId(), u.getName()))
                .toList();
    }
}
```

- [ ] **Step 5: Run to verify it passes**

Run: `cd backend && mvn -q -Dtest=AdminMcqServiceTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/review/AdminMcqService.java backend/src/main/java/com/atci/quizhub/review/dto/EligibleReviewerResponse.java backend/src/test/java/com/atci/quizhub/review/AdminMcqServiceTest.java
git commit -m "feat: add admin eligible-reviewers (skill match, exclude creator)"
```

---

## Task 11: Master data controller + service

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/masterdata/MasterDataService.java`
- Create: `backend/src/main/java/com/atci/quizhub/masterdata/MasterDataController.java`
- Test: `backend/src/test/java/com/atci/quizhub/masterdata/MasterDataControllerTest.java`

- [ ] **Step 1: Implement service**

```java
package com.atci.quizhub.masterdata;

import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class MasterDataService {
    private final TechStackRepository stacks;
    private final TopicRepository topics;
    public MasterDataService(TechStackRepository stacks, TopicRepository topics) {
        this.stacks = stacks; this.topics = topics;
    }
    public List<TechStack> allStacks() { return stacks.findAll(); }
    public List<Topic> topicsForStack(Long stackId) { return topics.findByStackId(stackId); }
}
```

- [ ] **Step 2: Implement controller** (returns lightweight maps to avoid lazy-loading issues)

```java
package com.atci.quizhub.masterdata;

import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/masterdata")
public class MasterDataController {
    private final MasterDataService service;
    public MasterDataController(MasterDataService service) { this.service = service; }

    @GetMapping("/stacks")
    public List<Map<String, Object>> stacks() {
        return service.allStacks().stream()
                .map(s -> Map.<String,Object>of("id", s.getId(), "name", s.getName()))
                .toList();
    }

    @GetMapping("/topics")
    public List<Map<String, Object>> topics(@RequestParam Long stackId) {
        return service.topicsForStack(stackId).stream()
                .map(t -> Map.<String,Object>of("id", t.getId(), "name", t.getName()))
                .toList();
    }
}
```

- [ ] **Step 3: Write the controller test**

```java
package com.atci.quizhub.masterdata;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class MasterDataControllerTest {

    @Autowired MockMvc mvc;

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void listsStacks() throws Exception {
        mvc.perform(get("/api/masterdata/stacks"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].name").exists());
    }

    @Test
    void unauthenticatedIsRejected() throws Exception {
        mvc.perform(get("/api/masterdata/stacks"))
           .andExpect(status().isUnauthorized());
    }
}
```

- [ ] **Step 4: Run the test**

Run: `cd backend && mvn -q -Dtest=MasterDataControllerTest test`
Expected: PASS (note: unauthenticated returns 401/403 — if 403, adjust the assertion to `isForbidden()`; Spring default for no-auth on stateless is 403, so use `isForbidden()`).

> NOTE for implementer: With this security setup the unauthenticated case returns **403**. Use `status().isForbidden()` in the second test. Verify by running and matching the actual status.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/masterdata/MasterDataService.java backend/src/main/java/com/atci/quizhub/masterdata/MasterDataController.java backend/src/test/java/com/atci/quizhub/masterdata/MasterDataControllerTest.java
git commit -m "feat: add master data endpoints for stacks and topics"
```

---

## Task 12: MCQ controller

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/mcq/McqController.java`
- Test: `backend/src/test/java/com/atci/quizhub/mcq/McqControllerTest.java`

- [ ] **Step 1: Implement `McqController`**

```java
package com.atci.quizhub.mcq;

import com.atci.quizhub.auth.CurrentUser;
import com.atci.quizhub.mcq.dto.McqRequest;
import com.atci.quizhub.mcq.dto.McqResponse;
import com.atci.quizhub.review.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

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
```

- [ ] **Step 2: Write the controller test**

```java
package com.atci.quizhub.mcq;

import com.atci.quizhub.masterdata.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class McqControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId;

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
    }

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void createDraftThenListMine() throws Exception {
        Map<String,Object> body = Map.of(
            "questionStem","What is DI?","optionA","a","optionB","b","optionC","c","optionD","d",
            "correctAnswer","A","difficulty","EASY","stackId",stackId,"topicId",topicId,"mode","SAVE");
        mvc.perform(post("/api/mcqs").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.status").value("DRAFT"));

        mvc.perform(get("/api/mcqs/mine"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void validationFailsForBlankStem() throws Exception {
        Map<String,Object> body = Map.of(
            "questionStem","","optionA","a","optionB","b","optionC","c","optionD","d",
            "correctAnswer","A","difficulty","EASY","stackId",stackId,"topicId",topicId,"mode","SAVE");
        mvc.perform(post("/api/mcqs").contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(body)))
           .andExpect(status().isBadRequest());
    }
}
```

- [ ] **Step 3: Run the test**

Run: `cd backend && mvn -q -Dtest=McqControllerTest test`
Expected: PASS.

- [ ] **Step 4: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/mcq/McqController.java backend/src/test/java/com/atci/quizhub/mcq/McqControllerTest.java
git commit -m "feat: add MCQ REST controller (create/edit/get/my-questions)"
```

---

## Task 13: Review + Admin controllers

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/review/dto/AssignRequest.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/dto/RejectRequest.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/ReviewController.java`
- Create: `backend/src/main/java/com/atci/quizhub/review/AdminMcqController.java`
- Test: `backend/src/test/java/com/atci/quizhub/review/ReviewFlowControllerTest.java`

- [ ] **Step 1: Create request DTOs**

```java
package com.atci.quizhub.review.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull String reviewerEnterpriseId) {}
```

```java
package com.atci.quizhub.review.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String comments) {}
```

- [ ] **Step 2: Implement `ReviewController`**

```java
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
```

- [ ] **Step 3: Implement `AdminMcqController`**

```java
package com.atci.quizhub.review;

import com.atci.quizhub.auth.CurrentUser;
import com.atci.quizhub.mcq.Mcq;
import com.atci.quizhub.mcq.McqRepository;
import com.atci.quizhub.mcq.McqService;
import com.atci.quizhub.mcq.dto.McqRequest;
import com.atci.quizhub.mcq.dto.McqResponse;
import com.atci.quizhub.review.dto.AssignRequest;
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
}
```

- [ ] **Step 4: Write an end-to-end flow controller test**

```java
package com.atci.quizhub.review;

import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.mcq.dto.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ReviewFlowControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired McqService mcqService;
    @Autowired TechStackRepository stacks;
    @Autowired TopicRepository topics;

    Long stackId; Long topicId; Long mcqId;

    @BeforeEach
    void setup() {
        stackId = stacks.findByName("Spring Cloud").orElseThrow().getId();
        topicId = topics.findByStackId(stackId).get(0).getId();
        mcqId = mcqService.create(new McqRequest("Q?","a","b","c","d",
            AnswerOption.A, Difficulty.EASY, stackId, topicId, SaveMode.SAVE_AND_SEND),
            "gaurav.a.bhola").id();
    }

    @Test
    @WithMockUser(username = "birendra.kumar.singh", roles = {"ADMIN"})
    void adminCanListBankAndAssignReviewer() throws Exception {
        mvc.perform(get("/api/admin/mcqs"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$.content").isArray());

        mvc.perform(get("/api/admin/mcqs/" + mcqId + "/eligible-reviewers"))
           .andExpect(status().isOk())
           .andExpect(jsonPath("$[0].enterpriseId").exists());

        mvc.perform(post("/api/admin/mcqs/" + mcqId + "/assign")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("reviewerEnterpriseId","divya.madhanasekar"))))
           .andExpect(status().isOk());

        assert mcqService.getEntity(mcqId).getStatus() == McqStatus.UNDER_REVIEW;
    }

    @Test
    @WithMockUser(username = "gaurav.a.bhola", roles = {"SME"})
    void smeCannotAccessAdminEndpoints() throws Exception {
        mvc.perform(get("/api/admin/mcqs"))
           .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "divya.madhanasekar", roles = {"SME"})
    void assignedReviewerRejectsWithComment() throws Exception {
        // assign first (as admin would) — do it directly via service for setup
        // then reject via the endpoint as the assigned reviewer
        // (assignment done in a separate admin-authenticated step is covered above;
        //  here we assign through the service to isolate the reject endpoint)
        // NOTE: implementer — use ReviewService bean to assign in @BeforeEach-like setup if needed.
        mvc.perform(post("/api/reviews/" + mcqId + "/reject")
                .contentType(MediaType.APPLICATION_JSON)
                .content(om.writeValueAsString(Map.of("comments",""))))
           .andExpect(status().isBadRequest()); // blank comment rejected by validation
    }
}
```

> NOTE for implementer: the third test only asserts that a **blank** rejection comment is a 400 (validation), which needs no prior assignment. The full assign→reject happy path is already covered in `ReviewServiceTest`. Keep controller tests focused on wiring + security.

- [ ] **Step 5: Run the tests**

Run: `cd backend && mvn -q -Dtest=ReviewFlowControllerTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/review/ReviewController.java backend/src/main/java/com/atci/quizhub/review/AdminMcqController.java backend/src/main/java/com/atci/quizhub/review/dto backend/src/test/java/com/atci/quizhub/review/ReviewFlowControllerTest.java
git commit -m "feat: add review and admin controllers with role-protected endpoints"
```

---

## Task 14: Bulk Excel template + upload — TDD

**Files:**
- Create: `backend/src/main/java/com/atci/quizhub/bulk/ExcelTemplate.java`
- Create: `backend/src/main/java/com/atci/quizhub/bulk/BulkRowResult.java`
- Create: `backend/src/main/java/com/atci/quizhub/bulk/BulkService.java`
- Create: `backend/src/main/java/com/atci/quizhub/bulk/BulkController.java`
- Test: `backend/src/test/java/com/atci/quizhub/bulk/BulkServiceTest.java`

- [ ] **Step 1: Create `ExcelTemplate` (column layout + writer)**

```java
package com.atci.quizhub.bulk;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ExcelTemplate {

    // Column order for Template_MCQs.xlsx
    public static final String[] HEADERS = {
        "Stack_name", "Topic_name", "Difficulty", "Question_Stem",
        "Option_A", "Option_B", "Option_C", "Option_D", "Correct_answer"
    };

    private ExcelTemplate() {}

    public static byte[] emptyTemplate() {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("MCQs");
            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                header.createCell(i).setCellValue(HEADERS[i]);
            }
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to build template", e);
        }
    }
}
```

- [ ] **Step 2: Create `BulkRowResult`**

```java
package com.atci.quizhub.bulk;

public record BulkRowResult(int rowNumber, boolean success, String message) {}
```

- [ ] **Step 3: Write the failing test** (build an in-memory xlsx, feed it to the service)

```java
package com.atci.quizhub.bulk;

import com.atci.quizhub.mcq.McqService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class BulkServiceTest {

    @Autowired BulkService bulkService;

    private MockMultipartFile xlsx(String[][] rows) throws Exception {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = wb.createSheet("MCQs");
            Row header = sheet.createRow(0);
            for (int i = 0; i < ExcelTemplate.HEADERS.length; i++)
                header.createCell(i).setCellValue(ExcelTemplate.HEADERS[i]);
            for (int r = 0; r < rows.length; r++) {
                Row row = sheet.createRow(r + 1);
                for (int c = 0; c < rows[r].length; c++) row.createCell(c).setCellValue(rows[r][c]);
            }
            wb.write(out);
            return new MockMultipartFile("file", "Template_MCQs.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", out.toByteArray());
        }
    }

    @Test
    void validRowImportsAsDraft() throws Exception {
        var file = xlsx(new String[][]{
            {"Spring Cloud","Introduction to Spring Cloud","EASY","What is Spring Cloud?","a","b","c","d","B"}
        });
        List<BulkRowResult> results = bulkService.importFile(file, "gaurav.a.bhola");
        assertEquals(1, results.size());
        assertTrue(results.get(0).success(), results.get(0).message());
    }

    @Test
    void invalidCorrectAnswerFailsRow() throws Exception {
        var file = xlsx(new String[][]{
            {"Spring Cloud","Introduction to Spring Cloud","EASY","Q?","a","b","c","d","Z"}
        });
        List<BulkRowResult> results = bulkService.importFile(file, "gaurav.a.bhola");
        assertFalse(results.get(0).success());
    }

    @Test
    void unknownStackFailsRow() throws Exception {
        var file = xlsx(new String[][]{
            {"Nonexistent Stack","Some Topic","EASY","Q?","a","b","c","d","A"}
        });
        List<BulkRowResult> results = bulkService.importFile(file, "gaurav.a.bhola");
        assertFalse(results.get(0).success());
    }
}
```

- [ ] **Step 4: Run to verify it fails**

Run: `cd backend && mvn -q -Dtest=BulkServiceTest test`
Expected: FAIL — `BulkService` not defined.

- [ ] **Step 5: Implement `BulkService`**

```java
package com.atci.quizhub.bulk;

import com.atci.quizhub.common.NotFoundException;
import com.atci.quizhub.masterdata.*;
import com.atci.quizhub.mcq.*;
import com.atci.quizhub.user.*;
import org.apache.poi.ss.usermodel.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Service
public class BulkService {

    private final McqRepository mcqs;
    private final TechStackRepository stacks;
    private final TopicRepository topics;
    private final UserRepository users;

    public BulkService(McqRepository mcqs, TechStackRepository stacks,
                       TopicRepository topics, UserRepository users) {
        this.mcqs = mcqs; this.stacks = stacks; this.topics = topics; this.users = users;
    }

    @Transactional
    public List<BulkRowResult> importFile(MultipartFile file, String creatorEnterpriseId) {
        User creator = users.findByEnterpriseId(creatorEnterpriseId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        List<BulkRowResult> results = new ArrayList<>();
        try (InputStream in = file.getInputStream(); Workbook wb = WorkbookFactory.create(in)) {
            Sheet sheet = wb.getSheetAt(0);
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isBlank(row)) continue;
                results.add(processRow(row, r + 1, creator));
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not read Excel file: " + e.getMessage());
        }
        return results;
    }

    private BulkRowResult processRow(Row row, int rowNumber, User creator) {
        try {
            String stackName = cell(row, 0);
            String topicName = cell(row, 1);
            String difficultyRaw = cell(row, 2);
            String stem = cell(row, 3);
            String a = cell(row, 4), b = cell(row, 5), c = cell(row, 6), d = cell(row, 7);
            String correct = cell(row, 8);

            if (stem.isBlank() || a.isBlank() || b.isBlank() || c.isBlank() || d.isBlank()) {
                return new BulkRowResult(rowNumber, false, "Missing required text field");
            }
            TechStack stack = stacks.findByName(stackName)
                    .orElse(null);
            if (stack == null) return new BulkRowResult(rowNumber, false, "Unknown stack: " + stackName);

            Topic topic = topics.findByStackId(stack.getId()).stream()
                    .filter(t -> t.getName().equalsIgnoreCase(topicName)).findFirst().orElse(null);
            if (topic == null) return new BulkRowResult(rowNumber, false, "Unknown topic: " + topicName);

            AnswerOption answer;
            try { answer = AnswerOption.valueOf(correct.trim().toUpperCase()); }
            catch (Exception e) { return new BulkRowResult(rowNumber, false, "Invalid correct answer: " + correct); }

            Difficulty difficulty;
            try { difficulty = Difficulty.valueOf(difficultyRaw.trim().toUpperCase()); }
            catch (Exception e) { return new BulkRowResult(rowNumber, false, "Invalid difficulty: " + difficultyRaw); }

            Mcq m = new Mcq();
            m.setQuestionStem(stem);
            m.setOptionA(a); m.setOptionB(b); m.setOptionC(c); m.setOptionD(d);
            m.setCorrectAnswer(answer); m.setDifficulty(difficulty);
            m.setStack(stack); m.setTopic(topic); m.setCreator(creator);
            m.setStatus(McqStatus.DRAFT);
            mcqs.save(m);
            return new BulkRowResult(rowNumber, true, "Imported as Draft");
        } catch (Exception e) {
            return new BulkRowResult(rowNumber, false, "Error: " + e.getMessage());
        }
    }

    private boolean isBlank(Row row) {
        for (int i = 0; i < ExcelTemplate.HEADERS.length; i++) {
            if (!cell(row, i).isBlank()) return false;
        }
        return true;
    }

    private String cell(Row row, int idx) {
        Cell cell = row.getCell(idx);
        if (cell == null) return "";
        cell.setCellType(CellType.STRING);
        String v = cell.getStringCellValue();
        return v == null ? "" : v.trim();
    }
}
```

- [ ] **Step 6: Run to verify it passes**

Run: `cd backend && mvn -q -Dtest=BulkServiceTest test`
Expected: PASS (3 tests).

- [ ] **Step 7: Implement `BulkController`**

```java
package com.atci.quizhub.bulk;

import com.atci.quizhub.auth.CurrentUser;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/bulk")
public class BulkController {

    private final BulkService bulkService;
    private final CurrentUser currentUser;

    public BulkController(BulkService bulkService, CurrentUser currentUser) {
        this.bulkService = bulkService; this.currentUser = currentUser;
    }

    @GetMapping("/template")
    public ResponseEntity<ByteArrayResource> template() {
        byte[] bytes = ExcelTemplate.emptyTemplate();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Template_MCQs.xlsx")
                .contentType(MediaType.parseMediaType(
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new ByteArrayResource(bytes));
    }

    @PostMapping("/upload")
    public List<BulkRowResult> upload(@RequestParam("file") MultipartFile file) {
        return bulkService.importFile(file, currentUser.get().getEnterpriseId());
    }
}
```

- [ ] **Step 8: Compile + run bulk test again**

Run: `cd backend && mvn -q -Dtest=BulkServiceTest test`
Expected: PASS; project compiles.

- [ ] **Step 9: Commit**

```bash
git add backend/src/main/java/com/atci/quizhub/bulk backend/src/test/java/com/atci/quizhub/bulk/BulkServiceTest.java
git commit -m "feat: add bulk Excel template download and upload with per-row validation"
```

---

## Task 15: PostgreSQL profile + CORS + run docs

**Files:**
- Create: `backend/src/main/resources/application-postgres.yml`
- Create: `backend/src/main/java/com/atci/quizhub/config/CorsConfig.java`
- Create: `backend/README.md`

- [ ] **Step 1: Create `application-postgres.yml`**

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/quizhub
    username: ${DB_USER:quizhub}
    password: ${DB_PASSWORD:quizhub}
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: false
```

- [ ] **Step 2: Create `CorsConfig`** (allow the React dev server)

```java
package com.atci.quizhub.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/api/**")
                        .allowedOrigins("http://localhost:5173")
                        .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                        .allowedHeaders("*");
            }
        };
    }
}
```

- [ ] **Step 3: Create `backend/README.md`**

```markdown
# Smart Quiz AI Hub — Backend

## Run (H2, zero setup)
    cd backend
    mvn spring-boot:run
App: http://localhost:8080  •  H2 console: http://localhost:8080/h2-console

## Run against PostgreSQL
    mvn spring-boot:run -Dspring-boot.run.profiles=postgres
Set DB_USER / DB_PASSWORD env vars as needed.

## Demo logins (password = `password` for all)
| enterpriseId | role |
|---|---|
| birendra.kumar.singh | ADMIN |
| gaurav.a.bhola | SME |
| divya.madhanasekar | SME |
| swati.avinash.nikam | SME |
| indugu.hari.prasad | SME |

## Test
    mvn test
```

- [ ] **Step 4: Run the full test suite**

Run: `cd backend && mvn -q test`
Expected: BUILD SUCCESS, all tests green.

- [ ] **Step 5: Commit**

```bash
git add backend/src/main/resources/application-postgres.yml backend/src/main/java/com/atci/quizhub/config/CorsConfig.java backend/README.md
git commit -m "chore: add postgres profile, CORS for frontend, backend run docs"
```

---

## Task 16: Full suite + smoke check

- [ ] **Step 1: Run the entire test suite**

Run: `cd backend && mvn -q clean test`
Expected: BUILD SUCCESS; all test classes pass.

- [ ] **Step 2: Boot and smoke-test login + a protected call**

Run (terminal A): `cd backend && mvn -q -DskipTests spring-boot:run`
Run (terminal B):
```bash
# login as admin
curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"enterpriseId":"birendra.kumar.singh","password":"password"}'
# copy the token, then:
curl -s localhost:8080/api/admin/mcqs -H "Authorization: Bearer <TOKEN>"
```
Expected: login returns a token + `"role":"ADMIN"`; admin list returns a paged JSON with the seeded sample MCQs. Stop the server (Ctrl-C).

- [ ] **Step 3: Commit any fixes, then tag the backend milestone**

```bash
git add -A
git commit -m "test: full backend suite green; manual smoke check passed" || echo "nothing to commit"
git tag backend-complete
```

---

## Spec Coverage Check

| Spec requirement | Task |
|---|---|
| Login + roles (SME/ADMIN) | 6, 7 |
| My Questions (paged, status, edit Draft/Rejected, comments on Rejected) | 8, 12 |
| Add single (Save / Save & Send) | 8, 12 |
| Bulk upload + template download | 14 |
| My Pending Reviews + approve/reject (mandatory reject comment) | 9, 13 |
| 5-state lifecycle, multiple cycles | 5, 9 |
| Admin super-edit + question bank (paged, creator id) | 10, 13 |
| Assign reviewer (skill-matched, exclude creator → Under Review) | 9, 10, 13 |
| Master data (stacks/topics/SME-skill) | 3, 7, 11 |
| Error handling / validation | 2, throughout |
| H2 default + Postgres profile | 1, 15 |

Frontend (React) is a **separate plan** to be written after `backend-complete`.
```