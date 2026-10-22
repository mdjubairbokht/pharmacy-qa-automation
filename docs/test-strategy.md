# Test Strategy & Plan — RxCare Pharmacy

| | |
|---|---|
| **Product** | RxCare Pharmacy (demo retail pharmacy management system) |
| **Release** | 1.0 — Sprints 1–3 |
| **Owner** | QA (Sr. Associate SQA Analyst) |
| **Related** | [Test cases](test-cases.csv) · [Traceability matrix](traceability-matrix.md) · [Defect example](defect-report-example.md) · [Exploratory charters](exploratory-charters.md) |

## 1. Objectives

1. Confirm every user story (RX-101 … RX-113) meets its acceptance criteria before sprint review.
2. Protect patient safety rules — allergy screening, controlled-substance limits, pharmacist-only verification — with automated, always-on regression.
3. Prove backend data integrity (inventory ledger, audit trail, sales) independently of the UI and API.
4. Keep regression feedback fast enough to run on every pull request.

## 2. Scope

**In scope:** sign-in & roles, patient registration/search, prescription intake → verification → fill → pickup → refill/cancel, inventory receiving & low stock, daily-sales ETL, REST API contracts and error handling, database constraints and integrity.

**Out of scope (this release):** third-party insurance adjudication, e-prescribing (NCPDP SCRIPT) interfaces, performance/load testing, accessibility audit (tracked as tech debt).

## 3. Approach — test pyramid

| Layer | Tooling | What it proves | Automated test methods |
|---|---|---|---|
| Unit | JUnit 5 (dev-owned, `pharmacy-app`) | State-machine rules | 2 (11 parameterised cases) |
| API | REST Assured, JSON Schema, data-driven TestNG | Contracts, status codes, business rules, negative/boundary cases | 32 |
| Database | JDBC + SQL | Integrity invariants, schema constraints, ETL source-to-target reconciliation | 7 (13 integrity rules + 11 constraint cases data-driven) |
| UI / E2E | Selenium WebDriver, TestNG, Page Object Model | Critical user journeys, role-based UI, error messaging | 17 |
| Manual / exploratory | Charters, Postman | Usability, real pharmacist workflows, unexpected behaviour | per sprint |

Design principles:

- **Arrange via API, assert via UI/DB.** UI tests create their data through the API so they only spend browser time on what the user actually does.
- **Independent tests.** Every test creates uniquely named data; a few seed NDCs are reserved per class so quantity deltas stay exact when classes run in parallel.
- **Database as an oracle.** Business rules are re-checked with SQL that returns violating rows (expected result: no rows), so a failure lists the exact records.
- **No sleeps.** Explicit waits only; the app exposes `data-state="ready"` on tables so tests wait for rendering, not for time.

## 4. Test types mapped to the release

| Type | When | How |
|---|---|---|
| Smoke | Every push, every deploy | `-Dsuite=smoke` — 10 critical checks across API, DB and UI |
| Sanity | After a hotfix | Targeted TestNG groups or single classes |
| Functional / integration | During the sprint, per story | API + DB suites |
| Regression | Every PR and nightly | `api`, `db`, `ui` suites as parallel CI jobs |
| End-to-end | Every PR | `PrescriptionWorkflowUiTest` — UI + API + DB in one flow |
| ETL | Every PR | `DailySalesEtlTest` — completeness, accuracy, duplicates, idempotency |
| Exploratory | Each sprint, 2 × 60 min sessions | [Charters](exploratory-charters.md) |
| UAT support | Before release | Demo environment + walkthrough with pharmacy SMEs |

## 5. Environments & data

| Env | Purpose | Config |
|---|---|---|
| local | Developer/QA workstation | `-Denv=local` — app on :8080, PostgreSQL via `docker compose up -d db` |
| ci | GitHub Actions / Azure DevOps | `-Denv=ci` — PostgreSQL service container, headless Chrome, 1 UI retry |
| docker | Jenkins agent | `-Denv=docker` — full compose stack incl. Selenium standalone Chrome |

The application recreates its schema and seed data on start-up, so every pipeline run begins from the same baseline. All patient data is fictional.

## 6. Entry / exit criteria

**Entry (story ready for test):** acceptance criteria agreed in refinement · build green · API available in the test environment.

**Exit (story done):** all linked test cases executed · no open Critical/High defects · automation added for regression-worthy scenarios · traceability updated.

**Release exit:** smoke + full regression green on the release candidate · open defects reviewed with the PO · Allure report attached to the release ticket.

## 7. Defect management (JIRA)

Workflow: `Open → In Progress → Ready for QA → Verified → Closed` (or `Reopened`).

| Severity | Definition | Example |
|---|---|---|
| Critical | Patient safety, data loss, no workaround | Allergy check bypassed; inventory goes negative |
| High | Core workflow blocked, workaround exists | Fill fails for a valid prescription |
| Medium | Incorrect behaviour, low impact | Wrong total on the dashboard card |
| Low | Cosmetic | Misaligned badge |

Every defect carries steps to reproduce, expected vs actual, environment/build, evidence (screenshot, API response, SQL) and a linked story. Fixed defects are re-tested and a regression test is added where the gap was in automation coverage. See the [defect example](defect-report-example.md).

## 8. Risks & mitigations

| Risk | Mitigation |
|---|---|
| Flaky UI tests erode trust | Explicit waits, `data-testid` locators, one retry in CI for UI only (API/DB never retried) |
| Shared test data collisions in parallel runs | Unique generated data, reserved NDCs per class, separate DB per CI job |
| Timezone edge cases in daily reporting | Sales are bucketed by UTC date in both app and tests |
| Requirement ambiguity | Three-amigos review of acceptance criteria during refinement |

## 9. Reporting

- **Per run:** Allure report (steps, request/response, SQL, screenshots), JUnit results in the CI "Tests" tab.
- **Per sprint review:** pass rate trend, automated vs manual coverage per story, defects found/fixed by severity, regression duration trend.
