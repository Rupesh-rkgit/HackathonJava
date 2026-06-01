# Smart Quiz AI Hub — Backend

Spring Boot 3 REST API for the centralized MCQ creation/review/approval platform.

## Run (H2, zero setup)
```
cd backend
mvn spring-boot:run
```
- App: http://localhost:8080
- H2 console: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:quizhub`, user `sa`, empty password)

Data is seeded automatically on startup (tech stacks, topics, demo users, SME-skill map, sample MCQs).

## Run against PostgreSQL
```
mvn spring-boot:run -Dspring-boot.run.profiles=postgres
```
Set `DB_USER` / `DB_PASSWORD` env vars as needed (defaults: `quizhub`/`quizhub`, db `quizhub`).

## Demo logins (password = `password` for all)
| enterpriseId | role |
|---|---|
| birendra.kumar.singh | ADMIN |
| gaurav.a.bhola | SME |
| divya.madhanasekar | SME |
| swati.avinash.nikam | SME |
| indugu.hari.prasad | SME |

## Test
```
mvn test
```

## Key endpoints
| Method | Path | Purpose |
|---|---|---|
| POST | /api/auth/login | Login, returns JWT + role |
| GET | /api/mcqs/mine | My Questions (paged) |
| POST | /api/mcqs | Add single MCQ (Save / Save & Send) |
| PUT | /api/mcqs/{id} | Edit MCQ |
| GET | /api/mcqs/{id} | Full MCQ (+reviewer comments if rejected) |
| GET | /api/bulk/template | Download Template_MCQs.xlsx |
| POST | /api/bulk/upload | Bulk import MCQs from Excel |
| GET | /api/reviews/pending | My Pending Reviews (paged) |
| POST | /api/reviews/{id}/approve | Approve (assigned reviewer) |
| POST | /api/reviews/{id}/reject | Reject (mandatory comment) |
| GET | /api/admin/mcqs | Question Bank (all, paged) — ADMIN |
| PUT | /api/admin/mcqs/{id} | Super-edit any MCQ — ADMIN |
| GET | /api/admin/mcqs/{id}/eligible-reviewers | Skill-matched reviewers — ADMIN |
| POST | /api/admin/mcqs/{id}/assign | Assign reviewer → Under Review — ADMIN |
| GET | /api/masterdata/stacks | Tech stacks |
| GET | /api/masterdata/topics?stackId= | Topics for stack |

Send the JWT as `Authorization: Bearer <token>` on all endpoints except login.
