# Smart Quiz AI Hub — Demo Video Script (≤ 5 minutes)

A click-by-click, timed walkthrough for recording the Level-1 demo video. Records the
**running application** showing the full SME → review → approval workflow.

> **Recording tool:** Windows Snipping Tool (Win+Shift+S → record) or Snipping Tool video.
> **Before you start:** have both servers running and the browser at `http://localhost:5173`.

## Pre-flight (do this before recording)

```
# Terminal 1 — backend (wait ~7s for "Started QuizHubApplication")
cd backend && mvn spring-boot:run

# Terminal 2 — frontend
cd frontend && npm install && npm run dev
```

Open `http://localhost:5173`, make sure you're signed **out** (start at the login screen).
Close other tabs/notifications. Set browser zoom to 100%.

---

## Timed script

### 0:00 – 0:30 · Intro + the problem (login screen on screen)
> "This is **Smart Quiz AI Hub** — a centralized platform we built for the ATCI Learning &
> Talent Transformation team. Today SMEs create quiz questions in scattered third-party tools
> with no review workflow and no visibility. Our solution replaces that with a single,
> accountable pipeline: every question flows from **Draft → Ready → Review → Approved**, shown
> right here on the login screen."

*Action:* let the login page sit; gesture to the lifecycle rail on the left panel.

### 0:30 – 1:25 · SME authors a question (login as Gaurav)
> "Let me sign in as an SME, Gaurav."

*Action:* click the **Gaurav** demo chip → **Enter the Hub**. Land on **My Questions**.

> "This is My Questions — every MCQ I've authored with its live status. Let me add a new one."

*Action:* click **＋ New Question** (or **Add Question** in the sidebar).
- Keep **Add from UI** selected.
- Type a stem, e.g. *"Which Spring Cloud component provides client-side load balancing?"*
- Fill options A–D (e.g. Eureka / Spring Cloud LoadBalancer / Hibernate / Tomcat).
- Click the **B** letter button to mark it correct (it turns green).
- Pick **Stack** = Spring Cloud, **Topic** = a Spring Cloud topic, **Difficulty** = Medium.

> "I can **Save** as a draft, or **Save & Send for Review**. I'll send it for review."

*Action:* click **Save & Send for Review →**. Toast appears; lands back on My Questions showing
the new question as **Ready for Review**.

### 1:25 – 1:55 · Bulk upload (still as Gaurav)
> "SMEs can also bulk-upload questions from Excel."

*Action:* sidebar → **Add Question** → click **Bulk Upload** tab.
> "Download the template, fill it, drop it back in. Each row is validated and imported as a draft."

*Action:* click **⤓ Download template** (briefly), then drag/drop a prepared `Template_MCQs.xlsx`
(or click to browse). Show the per-row results panel (✓ imported / ✗ rejected with reason).

### 1:55 – 2:55 · Admin assigns a reviewer (switch to Birendra)
*Action:* **Sign out** → log in with the **Birendra** chip (Admin).
> "Now I'm an Admin. Admins get an extra tab — the **Question Bank** — every question across all authors."

*Action:* sidebar → **Question Bank**.
> "Here's the question Gaurav just sent. It's Ready for Review, so I can assign a reviewer."

*Action:* click **Assign Reviewer** on that row.
> "The system only offers reviewers **skilled in this stack — Spring Cloud — and it excludes the
> author**, so no one reviews their own work."

*Action:* pick a reviewer (e.g. Divya) from the dropdown → **Assign → Under Review**. Toast confirms;
the row status flips to **Under Review**.

### 2:55 – 3:55 · Reviewer rejects, author fixes, gets approved
*Action:* **Sign out** → log in as **Divya** (the assigned reviewer). Go to **My Pending Reviews**.
> "Divya sees exactly what's assigned to her. Let's open it."

*Action:* click **View & Review**. The modal shows the full question with the correct answer flagged.
> "She can approve, or reject with mandatory feedback. Let's reject first to show the loop."

*Action:* click **Reject…**, type a comment (e.g. *"Clarify the stem."*), **Confirm Rejection**. Toast.

*Action:* **Sign out** → log back in as **Gaurav** → **My Questions**.
> "Gaurav's question is now **Rejected**, and the reviewer's feedback is shown inline. He edits and resubmits."

*Action:* click **Edit** on the rejected row → tweak the stem → **Save & Send for Review →**.

*Action:* **Sign out** → log in as **Birendra** → **Question Bank** → **Assign Reviewer** → Divya again.
*Action:* **Sign out** → log in as **Divya** → **My Pending Reviews** → **View & Review** → **✓ Approve**.
> "Approved. The question is now part of the trusted question bank."

### 3:55 – 4:40 · Recap the architecture
*Action:* (optional) show the Question Bank as Admin with the approved item, or a slide.
> "Under the hood: a **React + TypeScript** front end, a **Spring Boot 3 / Java 17** REST API
> secured with **JWT** and role-based access, **JPA** persistence on **H2** (with a PostgreSQL
> profile), and Apache POI for Excel. The heart is a single **state-machine** that guarantees no
> question reaches Approved without passing a skill-matched, non-author review — fully covered by
> **35 automated tests**."

### 4:40 – 5:00 · Close
> "That's Smart Quiz AI Hub: one accountable pipeline for question creation, review, and approval —
> giving the L&TT team the control, visibility, and quality gate they were missing. Thank you."

---

## Tips
- Practice the click path once before recording — the flow crosses 4 logins; keep moves crisp.
- If short on time, you can pre-seed a rejected question so you skip the first reject and go
  straight to edit→resubmit→approve.
- Keep narration tight; the pipeline visuals do a lot of the talking.
- The two seeded sample questions (from the problem statement) are already in the bank if you
  want to show data without creating any.
