# Smart Quiz AI Hub — Level-1 Design Spec

**Hackathon:** Hack-N-Stack: Code the Future — Java Full Stack with AI: Learning Hackathon 2026
**Date:** 2026-06-02
**Status:** Approved design — ready for implementation planning

---

## 1. Problem Summary

The ATCI Learning & Talent Transformation (L&TT) team lacks a centralized, customizable
platform for creating and managing practice-quiz questions. SMEs currently use third-party
tools that restrict content control, lack internal integration, and provide no unified
review/approval workflow.

**Goal (Level-1):** Build the **Smart Quiz AI Hub** — a centralized web application where
SMEs create Multiple Choice Questions (MCQs) that flow through a structured review-and-approval
workflow into a trusted question bank, with Admins overseeing assignment and acting as
super-users.

This spec covers the **complete non-AI platform** (Phase 1). AI features (generation,
similarity detection) named in the problem statement are **explicitly deferred**; the
architecture leaves a clean seam to add them later.

---

## 2. Scope

### In scope (Phase 1)
- Login + role-based access (roles: **SME**, **ADMIN**)
- **My Questions**: paginated table of the user's own MCQs with status; Edit on Draft/Rejected;
  reviewer comments shown on Rejected
- **Add Question**: single via UI form, and **Bulk Upload** via Excel (`Template_MCQs.xlsx`);
  Save (Draft) vs Save & Send for Review (Ready for Review)
- **My Pending Reviews**: MCQs assigned to the user; view full question; approve/reject with
  **mandatory comment on rejection**
- **Question Bank Management (Admin)**: paginated view of all MCQs with creator enterprise ID;
  Assign Reviewer (skill-matched SMEs, excluding creator); super-user edit of any MCQ
- MCQ lifecycle state machine (5 states, multiple review cycles)
- Master data: tech stacks, topics, SME↔skill mapping (seeded from problem-statement data)
- Demo seed data so the app runs out of the box
- Backend test suite (TDD), heaviest on lifecycle + assignment eligibility

### Out of scope (Phase 1)
- AI question generation and duplicate/similarity detection (deferred; seam preserved)
- Quiz *taking* / leaderboard / participant-facing features (Level-1 is repository + workflow only)
- SSO / external identity providers (local JWT auth with seeded users)

---

## 3. Technology Stack

| Layer | Choice | Rationale |
|---|---|---|
| Backend | Java 17, Spring Boot 3.x, Maven | Current LTS Java; modern Spring Boot; within spec (JDK 8+, LTS Spring) |
| Architecture | Modular monolith, layered, packaged by feature | Spec marks microservices optional ("if any"); monolith is more reliable to demo and judge |
| Persistence | Spring Data JPA / Hibernate | Standard, relational fit |
| Database | H2 in-memory (default) + PostgreSQL profile | Zero-setup demo; production-style option via `postgres` profile |
| Security | Spring Security + JWT, role-based | Two roles, stateless API, clean FE/BE decoupling |
| Excel | Apache POI | Read/write `Template_MCQs.xlsx` |
| Frontend | React 18 + TypeScript + Vite (later phase) | Allowed by spec; best DX and polish |
| Testing | JUnit 5, Spring Boot Test, MockMvc | TDD across services and controllers |

**Build order:** Backend-complete first (all APIs + tests), then frontend.

---

## 4. Architecture

Modular monolith, packaged by feature. Each package has a clear single purpose and communicates
through service interfaces.

```
backend/  (Spring Boot 3, Java 17, Maven)
  auth        login, JWT issuing/validation, Spring Security config, role-based access
  user        SME/Admin accounts, UserSkill (SME↔stack) mapping
  mcq         Mcq entity, lifecycle state machine, CRUD, "My Questions"
  review      ReviewAssignment, assign/approve/reject, "My Pending Reviews"
  masterdata  TechStack, Topic; dropdown data
  bulk        Excel template download + upload parsing/validation (Apache POI)
  common      error handling, pagination, DTO mapping, config, startup seeding

frontend/  (React 18 + TS + Vite) — later phase, consumes REST API
```

**Layering:** Controller → Service → Repository. DTOs at the boundary; entities never leave the
service layer. Role checks enforced both in Spring Security (URL level) and in services
(defense in depth).

**Design patterns:**
- **State-machine pattern** for the MCQ lifecycle — one class owns all legal transitions; no
  scattered status mutation.
- **DTO mapping** at controller boundary.
- **Strategy-style validation** for bulk-upload row checks.

---

## 5. Data Model

```
User
  id, enterpriseId (unique, e.g. birendra.kumar.singh), name,
  passwordHash, role (SME | ADMIN)

TechStack
  id, name                                  -- seeded (Spring Cloud, Spring Boot, ...)

Topic
  id, stackId (FK -> TechStack), name        -- seeded

UserSkill                                     -- SME↔skill (many-to-many)
  userId (FK -> User), stackId (FK -> TechStack)

Mcq
  id, questionStem,
  optionA, optionB, optionC, optionD,
  correctAnswer (A | B | C | D),
  difficulty (EASY | MEDIUM | HARD),
  stackId (FK -> TechStack), topicId (FK -> Topic),
  creatorId (FK -> User),
  status (DRAFT | READY_FOR_REVIEW | UNDER_REVIEW | APPROVED | REJECTED),
  createdAt, updatedAt

ReviewAssignment                              -- one row per review cycle (full history)
  id, mcqId (FK -> Mcq), reviewerId (FK -> User), assignedById (FK -> User),
  outcome (PENDING | APPROVED | REJECTED), comments,
  assignedAt, decidedAt
```

**Notes:**
- `Mcq.status` is the single source of truth for the current state.
- `ReviewAssignment` is separate so review history is preserved across multiple cycles
  (spec: MCQs may loop before Approved). The "current" assignment is the latest PENDING one.

---

## 6. MCQ Lifecycle State Machine

Exactly one state at any time. One class owns all transitions; illegal transitions throw a
domain error → clean 4xx response.

| From | Action | To | Guard |
|---|---|---|---|
| — | create (Save) | DRAFT | — |
| — | create (Save & Send for Review) | READY_FOR_REVIEW | — |
| DRAFT / REJECTED | edit + Save | *(unchanged)* | creator or Admin only |
| DRAFT / REJECTED | Save & Send for Review | READY_FOR_REVIEW | creator or Admin only |
| READY_FOR_REVIEW | assign reviewer | UNDER_REVIEW | Admin only; reviewer ≠ creator; reviewer skill-matched |
| UNDER_REVIEW | approve | APPROVED | acting user = assigned reviewer |
| UNDER_REVIEW | reject | REJECTED | acting user = assigned reviewer; comment mandatory |

**Admin super-user rule:** Admins may edit any MCQ at any stage **except DRAFT** (Draft is
accessible only to its creator). Approving/rejecting still requires being the assigned reviewer.

---

## 7. REST API

All under `/api`. JWT bearer auth; stateless. `/api/admin/**` requires ADMIN role.

```
POST /api/auth/login                                  -> { token, role, enterpriseId }

GET  /api/mcqs/mine            (paged)                 -> My Questions (creator = current user)
POST /api/mcqs                  body: McqRequest       -> add single (draft | ready-for-review)
PUT  /api/mcqs/{id}             body: McqRequest+mode  -> edit (Save | Save & Send), Draft/Rejected only
GET  /api/mcqs/{id}                                    -> full MCQ (+ latest reviewer comments if Rejected)

GET  /api/bulk/template                                -> download Template_MCQs.xlsx
POST /api/bulk/upload           multipart xlsx         -> validate rows; import valid as DRAFT; per-row report

GET  /api/reviews/pending      (paged)                 -> My Pending Reviews (assigned, Under Review)
POST /api/reviews/{mcqId}/approve                       -> approve (assigned reviewer)
POST /api/reviews/{mcqId}/reject  body: { comments }   -> reject; comment required

GET  /api/admin/mcqs           (paged)                 -> Question Bank Management (all MCQs, +creator id)
PUT  /api/admin/mcqs/{id}                              -> super-user edit (any status except Draft)
GET  /api/admin/mcqs/{mcqId}/eligible-reviewers        -> SMEs matching MCQ stack, excluding creator
POST /api/admin/mcqs/{mcqId}/assign  body:{ reviewerId}-> assign reviewer -> Under Review

GET  /api/masterdata/stacks                            -> tech stacks (dropdown)
GET  /api/masterdata/topics?stackId=                   -> topics for stack (dropdown)
```

---

## 8. Cross-Cutting Concerns

- **Error handling:** `@RestControllerAdvice` produces consistent
  `{ timestamp, status, error, message }`. Bean Validation on request DTOs.
- **Pagination:** Spring `Pageable`; responses include content + page metadata.
- **Bulk upload:** Apache POI parses xlsx. Per-row validation — required fields present
  (stack, topic, difficulty, stem, four options, correct answer); stack/topic exist;
  correctAnswer ∈ {A,B,C,D}; difficulty valid. Valid rows imported as DRAFT under the current
  user; response is a per-row pass/fail report. Template columns mirror problem-statement slide 14.
- **Seeding (startup):** tech stacks + topics (slide 15), SME↔skill mapping (slide 16),
  demo users with enterprise IDs from slide 16 (mix of SME and ADMIN), and the two sample MCQs
  (slide 14). Default passwords documented for demo login.
- **Security:** JWT filter; `/api/auth/login` public; `/api/admin/**` ADMIN-only; everything
  else authenticated. Service-layer ownership/role guards in addition to URL rules.

---

## 9. Testing Strategy

TDD throughout. Priorities:
1. **Lifecycle state machine** — every legal transition and every illegal-transition rejection.
2. **Assign-reviewer eligibility** — skill match, creator exclusion, status guard, status→Under Review.
3. **Review approve/reject** — assigned-reviewer guard, mandatory rejection comment, status reflection
   on creator's My Questions.
4. **Bulk upload** — valid import, per-row validation failures, malformed file handling.
5. **Auth/role** — login, JWT validation, admin-only endpoint protection.

Tooling: JUnit 5, Spring Boot Test, MockMvc for controller slices, service unit tests with
in-memory H2.

---

## 10. Mapping to Problem Statement (traceability)

| Requirement (slide) | Where covered |
|---|---|
| SME create MCQ via UI (8) | `POST /api/mcqs` + Add form |
| SME bulk upload Excel, download template (9) | `bulk` package, `/api/bulk/*` |
| My Questions, paginated, status, edit Draft/Rejected, show comments (7) | `GET /api/mcqs/mine`, `GET /api/mcqs/{id}` |
| Save vs Save & Send transitions (7,8) | state machine, `PUT /api/mcqs/{id}` mode |
| Review assigned MCQs, approve/reject, mandatory reject comment (10) | `review` package, `/api/reviews/*` |
| 5-state lifecycle, one state at a time, multiple cycles (5) | state machine + ReviewAssignment history |
| Admin = SME powers + super-user edit + assign reviewers (4,11,12) | `/api/admin/*`, role rules |
| Question Bank Mgmt, paginated, creator id (12) | `GET /api/admin/mcqs` |
| Assign Reviewer: skill-matched, exclude creator, ->Under Review (12) | `/api/admin/mcqs/{id}/eligible-reviewers` + `/assign` |
| Two roles, login (13) | `auth` package, JWT |
| Master lists: stacks, topics, SME-skill map (13,15,16) | `masterdata` + seeding |
| Stack: Java + Spring + SQL + React (6) | full stack choice (§3) |

---

## 11. Deliverables (per submission template)

- Source code (zip, < 10 MB; `target/` and `node_modules/` excluded before zipping)
- Completed submission PPT (team, technical architecture, software architecture, solution
  description, workflow/deployment screenshots)
- Demo video (≤ 5 min) showing the running application

---

## 12. What Makes This Solution Stand Out

- **Enforced quality gate**: a real state machine guarantees no MCQ reaches "Approved" without
  passing review by a *skill-matched, non-author* reviewer — directly fixing the "no control /
  no review workflow" pain of the current third-party tools.
- **Full review history** via `ReviewAssignment` rows, so multi-cycle review is auditable.
- **Zero-setup demo** (H2 + seed) with a production-style PostgreSQL switch.
- **Clean AI seam** ready for the named Spring AI generation/similarity features in a later phase.
```