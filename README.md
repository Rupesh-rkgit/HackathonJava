# Smart Quiz AI Hub

**Hack-N-Stack: Code the Future — Java Full Stack with AI: Learning Hackathon 2026**

A centralized platform for the ATCI Learning & Talent Transformation (L&TT) team where
Subject-Matter Experts (SMEs) author Multiple-Choice Questions (MCQs) that flow through a
structured **review-and-approval pipeline** into a trusted question bank. Admins oversee the
process, assign skill-matched reviewers, and act as super-users.

Replaces fragmented third-party quiz tools with full content control, a real approval
workflow, and complete visibility into question status.

---

## Architecture

```
┌──────────────────────────┐        REST / JSON (JWT)        ┌───────────────────────────┐
│  Frontend (React + TS)   │  ───────────────────────────▶  │  Backend (Spring Boot 3)  │
│  Vite · Framer Motion    │                                 │  layered modular monolith │
│  editorial design system │  ◀───────────────────────────  │  JPA · Spring Security    │
└──────────────────────────┘                                 └─────────────┬─────────────┘
                                                                            │
                                                              ┌─────────────▼─────────────┐
                                                              │  H2 (default) / PostgreSQL │
                                                              └────────────────────────────┘
```

- **Backend** — Java 17, Spring Boot 3, Spring Data JPA, Spring Security + JWT, Apache POI
  (Excel), H2 by default with a PostgreSQL profile. A single **state-machine** class owns the
  MCQ lifecycle. See [backend/README.md](backend/README.md).
- **Frontend** — React 18 + TypeScript + Vite, Framer Motion, a custom "Editorial Workshop"
  design system (no UI framework). See below.

### MCQ lifecycle (the core)

```
Draft ──Save&Send──▶ Ready for Review ──assign──▶ Under Review ──approve──▶ Approved
  ▲                                                    │
  └──────────────── edit & resubmit ◀──reject (comment)┘  Rejected
```

## Run it

**1 — Backend** (zero setup; seeds demo data on startup)
```
cd backend
mvn spring-boot:run         # http://localhost:8080
```

**2 — Frontend**
```
cd frontend
npm install
npm run dev                 # http://localhost:5173  (proxies /api to :8080)
```

Open http://localhost:5173 and sign in with a demo account.

### Demo logins (password = `password`)
| enterpriseId | role | skills |
|---|---|---|
| birendra.kumar.singh | **ADMIN** | Spring Boot |
| gaurav.a.bhola | SME | Spring Cloud, Spring Core |
| divya.madhanasekar | SME | Spring MVC & REST, Spring Cloud |
| swati.avinash.nikam | SME | Spring Boot |
| indugu.hari.prasad | SME | Spring Cloud |

## Features (maps to the Level-1 problem statement)

- **Login & roles** — SME and Admin, JWT-secured.
- **My Questions** — paginated table with live status; edit Draft/Rejected; reviewer feedback
  shown on rejected items.
- **Add Question** — single via form, or **bulk upload** via `Template_MCQs.xlsx` (download +
  per-row validation report). Save (Draft) vs Save & Send for Review.
- **My Pending Reviews** — view the full question, approve, or reject with a mandatory comment.
- **Question Bank (Admin)** — all MCQs with creator IDs; super-edit any item; **Assign
  Reviewer** to skill-matched SMEs, excluding the creator (no self-review) → moves to Under Review.
- **Master data** — tech stacks, topics, and SME-skill mappings seeded from the problem statement.

## Testing

```
cd backend && mvn test      # 32 tests: lifecycle, services, controllers, auth, bulk import
```
Frontend production build (type-checked): `cd frontend && npm run build`.

## Project layout

```
backend/    Spring Boot API (auth, user, mcq, review, masterdata, bulk, common)
frontend/   React + TS app (pages, components, api client, auth context)
docs/superpowers/
  specs/    design specification
  plans/    bite-sized implementation plan
```

## Packaging for submission

Before zipping (≤ 10 MB): delete `backend/target/` and `frontend/node_modules/`.
```
rm -rf backend/target frontend/node_modules frontend/dist
```

