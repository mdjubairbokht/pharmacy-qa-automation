# RxCare Pharmacy — QA Automation Framework

[![CI](https://github.com/mdjubairbokht/pharmacy-qa-automation/actions/workflows/ci.yml/badge.svg)](https://github.com/mdjubairbokht/pharmacy-qa-automation/actions/workflows/ci.yml)
![Java](https://img.shields.io/badge/Java-17-ED8B00?logo=openjdk&logoColor=white)
![Selenium](https://img.shields.io/badge/Selenium-4-43B02A?logo=selenium&logoColor=white)
![TestNG](https://img.shields.io/badge/TestNG-7-FF7F00)
![REST Assured](https://img.shields.io/badge/REST%20Assured-5-2E7D32)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Allure](https://img.shields.io/badge/Report-Allure-FF6B6B)
![License](https://img.shields.io/badge/License-MIT-blue)

> **At a glance:** 108 automated test executions across UI, API and database layers · all passing in CI · full pipeline (build → parallel API / DB / UI suites → Postman → Allure report) in about **2 minutes** on every push.

| | |
|---|---|
| **UI automation** | Selenium WebDriver 4, TestNG, Page Object Model, parallel execution, cross-browser (Chrome / Firefox / Edge / Grid) |
| **API automation** | REST Assured, JSON Schema validation, data-driven negative & boundary tests, Postman/Newman |
| **Database testing** | SQL integrity checks, schema-constraint tests, ETL source-to-target reconciliation |
| **CI/CD** | GitHub Actions (parallel matrix), Jenkins pipeline, Azure DevOps pipeline, Docker Compose |
| **QA process** | Test strategy, 59 test cases, requirements traceability, defect reporting, exploratory charters |

### Results

| Layer | Test executions | Result |
|---|---|---|
| API (REST Assured) | 59 | ✅ all passing |
| Database (SQL / JDBC / ETL) | 29 | ✅ all passing |
| UI (Selenium + TestNG) | 20 | ✅ all passing |

### Screenshots

**Prescription work queue:** the app under test, driven by the Selenium suite
![Prescription work queue](docs/images/prescriptions.png)

<table>
<tr>
<td width="50%"><b>Patient-safety rule:</b> allergy screening blocks an opioid for an allergic patient (covered by UI, API and DB tests)<br><img src="docs/images/allergy-check.png" alt="Allergy check"></td>
<td width="50%"><b>Point of sale:</b> amount due verified against quantity × unit price in the database<br><img src="docs/images/pickup.png" alt="Pickup dialog"></td>
</tr>
<tr>
<td width="50%"><b>Inventory:</b> low-stock flags and receiving, reconciled against the inventory ledger<br><img src="docs/images/inventory.png" alt="Inventory"></td>
<td width="50%"><b>Sign-in:</b> role-based access (pharmacist vs technician)<br><img src="docs/images/login.png" alt="Sign in"></td>
</tr>
</table>

---

## About

End-to-end quality engineering project for a **retail pharmacy management system**: a Java test automation framework covering **UI (Selenium WebDriver + TestNG + Page Object Model)**, **REST API (REST Assured)** and **database (SQL / JDBC, including ETL validation)**, wired into **GitHub Actions, Jenkins and Azure DevOps**, with the manual QA artefacts that go with it (test strategy, test cases, traceability, defect reporting, exploratory charters).

The repository includes its own system under test, **RxCare Pharmacy**: a Spring Boot + PostgreSQL app with patient management, prescription processing (intake → pharmacist verification → fill → pickup → refill), allergy screening, controlled-substance rules, inventory and a daily-sales ETL job. Everything runs locally with one `docker compose` command.

> Demo project. RxCare and all data in it are fictional. Not affiliated with, or derived from, any vendor's pharmacy software.

---

## Architecture

```mermaid
flowchart LR
    subgraph QA["qa-automation (Java 17 · TestNG)"]
        UI["UI tests<br/>Selenium WebDriver + POM"]
        API["API tests<br/>REST Assured + JSON Schema"]
        DB["DB tests<br/>JDBC + SQL · ETL checks"]
    end
    subgraph SUT["pharmacy-app (Spring Boot)"]
        WEB["Web UI<br/>(HTML/JS)"]
        REST["REST API<br/>/api/**"]
    end
    PG[("PostgreSQL 16")]
    UI --> WEB --> REST
    API --> REST
    REST --> PG
    DB --> PG
    CI["GitHub Actions · Jenkins · Azure DevOps"] -. runs .-> QA
    QA -. results .-> ALLURE["Allure report"]
```

## How this maps to the role

| Responsibility | Where it lives |
|---|---|
| Requirement analysis & test planning | [`docs/test-strategy.md`](docs/test-strategy.md), [`docs/traceability-matrix.md`](docs/traceability-matrix.md) |
| Test case design (SpiraTeam/JIRA import-ready) | [`docs/test-cases.csv`](docs/test-cases.csv) — 56 automated + 3 manual cases, each linked to a story |
| Selenium WebDriver + TestNG + POM framework | [`qa-automation/src/main/java/com/rxcare/qa/pages`](qa-automation/src/main/java/com/rxcare/qa/pages), [`driver`](qa-automation/src/main/java/com/rxcare/qa/driver) |
| Data-driven testing | TestNG `@DataProvider` fed by CSV/JSON in [`src/test/resources/testdata`](qa-automation/src/test/resources/testdata) |
| REST API testing (REST Assured, Postman) | [`tests/api`](qa-automation/src/test/java/com/rxcare/qa/tests/api), [`postman/`](postman) |
| SQL backend validation, data integrity, ETL testing | [`tests/db`](qa-automation/src/test/java/com/rxcare/qa/tests/db), [`PharmacyQueries.java`](qa-automation/src/main/java/com/rxcare/qa/db/PharmacyQueries.java) |
| Pharmacy workflows (Rx processing, patients, dispensing, inventory, POS) | [`PrescriptionWorkflowUiTest`](qa-automation/src/test/java/com/rxcare/qa/tests/ui/PrescriptionWorkflowUiTest.java), [`PrescriptionApiTest`](qa-automation/src/test/java/com/rxcare/qa/tests/api/PrescriptionApiTest.java) |
| CI/CD: Jenkins, Azure DevOps, GitHub | [`Jenkinsfile`](Jenkinsfile), [`azure-pipelines.yml`](azure-pipelines.yml), [`.github/workflows/ci.yml`](.github/workflows/ci.yml) |
| Regression optimisation | Parallel TestNG execution (ThreadLocal drivers), API-based test setup, suite split into parallel CI jobs, smoke gate |
| Defect management & RCA | [`docs/defect-report-example.md`](docs/defect-report-example.md), [issue template](.github/ISSUE_TEMPLATE/defect.md) |
| Exploratory testing | [`docs/exploratory-charters.md`](docs/exploratory-charters.md) |
| Smoke / sanity / regression / release validation | `smoke`, `api`, `db`, `ui`, `regression` suites in [`suites/`](qa-automation/src/test/resources/suites) |

## Tech stack

Java 17 · Maven · Selenium 4 · TestNG 7 · REST Assured 5 · JSON Schema Validator · PostgreSQL 16 · JDBC · Allure 2 · SLF4J · Spring Boot 3.5 (SUT) · Docker Compose · Postman/Newman · GitHub Actions · Jenkins · Azure DevOps

## Project structure

```
pharmacy-qa-automation/
├── pharmacy-app/                     System under test (Spring Boot + PostgreSQL)
│   └── src/main/resources/
│       ├── schema.sql, data.sql      Schema with constraints + deterministic seed data
│       └── static/                   Web UI (login + pharmacy workstation), data-testid hooks
├── qa-automation/                    Test framework
│   ├── src/main/java/com/rxcare/qa/
│   │   ├── config/                   Layered config (properties → env vars → -D flags)
│   │   ├── driver/                   DriverFactory (chrome/firefox/edge, local or Grid), ThreadLocal manager
│   │   ├── pages/ (+ components/)    Page Object Model
│   │   ├── api/ (+ models/)          REST Assured clients, request/response models, workflow helpers
│   │   ├── db/                       JDBC client + catalogue of validation SQL
│   │   ├── data/                     Test data factory, CSV/JSON readers, seed references
│   │   └── listeners/                Health gate, logging, retry (UI only), failure screenshots
│   └── src/test/
│       ├── java/.../tests/{api,db,ui}  Test classes
│       └── resources/                TestNG suites, test data, JSON schemas, Allure config
├── postman/                          Postman collection + environment (run by Newman in CI)
├── docs/                             Test strategy, test cases, RTM, defect example, charters
├── .github/workflows/ci.yml          GitHub Actions
├── Jenkinsfile                       Jenkins declarative pipeline
├── azure-pipelines.yml               Azure DevOps pipeline
└── docker-compose.yml                PostgreSQL + app (+ Selenium Grid profile)
```

## Run locally

**Prerequisites:** JDK 17+, Maven 3.9+, Docker, Google Chrome.

```bash
# 1. Start PostgreSQL + the app (http://localhost:8080)
docker compose up -d --build

#    ...or run the app from source against the containerised database
docker compose up -d db
mvn -f pharmacy-app/pom.xml spring-boot:run

# 2. Run tests
mvn -f qa-automation/pom.xml test -Dsuite=smoke          # ~10 critical checks
mvn -f qa-automation/pom.xml test -Dsuite=api
mvn -f qa-automation/pom.xml test -Dsuite=db
mvn -f qa-automation/pom.xml test -Dsuite=ui -Dheadless=true
mvn -f qa-automation/pom.xml test                        # full regression

# 3. Open the Allure report
mvn -f qa-automation/pom.xml allure:serve
```

Sign in to the app with `rph.jones / Pharm@123` (pharmacist) or `tech.smith / Tech@123` (technician).

The app recreates its schema and seed data on every start, so restart it (`docker compose restart app`) to return to a clean baseline.

### Useful switches

| Flag | Values | Default |
|---|---|---|
| `-Dsuite` | `smoke`, `api`, `db`, `ui`, `regression` | `regression` |
| `-Denv` | `local`, `ci`, `docker` (see `src/main/resources/config/`) | `local` |
| `-Dbrowser` | `chrome`, `firefox`, `edge` | `chrome` |
| `-Dheadless` | `true` / `false` | per env |
| `-Dgrid.url` | Selenium Grid URL, e.g. `http://localhost:4444` | local browser |
| `-Dgroups` | TestNG groups, e.g. `smoke`, `e2e`, `etl` | all in suite |

Every config key can also come from an environment variable (`db.url` → `DB_URL`, `app.baseUrl` → `APP_BASE_URL`), which is how CI injects secrets.

### Run against Selenium Grid in Docker

```bash
docker compose --profile grid up -d --build
mvn -f qa-automation/pom.xml test -Dsuite=ui -Denv=docker
# watch the browser at http://localhost:7900 (password: secret)
```

## CI/CD

| Pipeline | Trigger | What it does |
|---|---|---|
| **GitHub Actions** (`ci.yml`) | push, PR, weekday nightly, manual (pick suite + browser) | Build & unit-test app → API, DB and UI suites as **parallel jobs**, each with its own PostgreSQL service → Newman run of the Postman collection → merged **Allure report** artifact (optional publish to GitHub Pages: set repo variable `PUBLISH_ALLURE=true`) |
| **Jenkins** (`Jenkinsfile`) | SCM, weekday nightly, manual with `SUITE` parameter | Build → `docker compose` environment incl. Selenium Grid → smoke gate → regression → JUnit + Allure publishing, failure screenshots archived |
| **Azure DevOps** (`azure-pipelines.yml`) | push, PR, weekday nightly | Build stage → test stage with an API / DB / UI matrix on PostgreSQL service containers → results in the *Tests* tab, Allure results as artifacts |

## Framework design highlights

- **Page Object Model with components** — pages expose business actions (`verify(rx)`, `completeSale("CARD")`), never raw locators. All locators use `data-testid`.
- **No sleeps, no implicit waits** — explicit waits plus a `data-state="loading|ready"` contract on tables, and helpers that clear stale state before each action.
- **Parallel-safe** — `ThreadLocal` WebDriver, unique generated test data, reserved NDCs per test class for exact inventory deltas.
- **Arrange via API, assert via UI + DB** — `PrescriptionWorkflow` puts a prescription into any state in milliseconds so UI tests only spend browser time on the behaviour under test.
- **Database as an oracle** — integrity rules written as SQL that returns *violating rows*; a failure message lists the exact offending records.
- **ETL validation** — source-vs-target reconciliation, duplicate-key and idempotency checks for the daily sales job.
- **Fail fast** — a suite listener waits for `/api/health` and stops with a clear message if the app is not up.
- **Flakiness control** — retry only for UI tests and only in CI; API/DB failures are never retried.
- **Evidence** — Allure captures REST requests/responses, every SQL query with parameters, and screenshot + page source on UI failure.

## Test coverage

| Suite | Classes | Test methods | Focus |
|---|---|---|---|
| API | `AuthApiTest`, `PatientApiTest`, `PrescriptionApiTest`, `InventoryApiTest`, `SystemApiTest` | 32 | Contracts, business rules, negative & boundary data |
| DB | `DataIntegrityTest`, `DatabaseConstraintTest`, `DailySalesEtlTest` | 7 (29 executions with data providers) | Integrity, constraints, ETL |
| UI | `LoginUiTest`, `PatientUiTest`, `PrescriptionWorkflowUiTest`, `InventoryUiTest` | 17 | Critical journeys, role-based UI, E2E |

## License

MIT — use it freely for learning and interviews.
