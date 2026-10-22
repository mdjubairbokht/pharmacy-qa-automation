# Requirements Traceability Matrix

Each user story maps to its test cases ([test-cases.csv](test-cases.csv)) and to the automated tests that execute them. In the Allure report every test carries its story (`@Story`) and test-case id (`@TmsLink`), so this matrix can be regenerated from any run.

| Story | Acceptance criteria (summary) | Test cases | Automated tests | Layers |
|---|---|---|---|---|
| **RX-101** Secure sign-in & roles | Valid users get a session; invalid/disabled users are refused; all APIs require a token; sign-out ends the session | TC-AUTH-001…005, TC-UI-AUTH-001…004 | `AuthApiTest`, `LoginUiTest` | API, UI |
| **RX-102** Patient registration | Required fields and formats enforced; allergies normalised; no duplicates (name + DOB); edits replace allergy list | TC-PAT-001/002/003/006, TC-UI-PAT-001/002/004 | `PatientApiTest`, `PatientUiTest` | API, UI, DB |
| **RX-103** Patient lookup | Case-insensitive search by name/phone; clear 404 / empty state | TC-PAT-004/005/007, TC-UI-PAT-003 | `PatientApiTest`, `PatientUiTest` | API, UI |
| **RX-104** Prescription intake & workflow | Field limits (qty > 0, days 1–90, refills 0–11, NDC 5-4-2); only valid status transitions; cancel needs a reason; queue filter | TC-RX-005/007/010/011/012, TC-UI-RX-005/006, TC-SYS-002 | `PrescriptionApiTest`, `PrescriptionWorkflowUiTest`, `SystemApiTest` | API, UI, DB |
| **RX-105** Pharmacist verification | Only pharmacists verify; technicians do not see Verify | TC-RX-004, TC-UI-RX-002 | `PrescriptionApiTest`, `PrescriptionWorkflowUiTest` | API, UI, DB |
| **RX-106** Dispensing & inventory | Fill decrements on-hand by quantity and writes one DISPENSE row; cannot fill beyond stock | TC-RX-001/006, TC-UI-RX-001/004 | `PrescriptionApiTest`, `PrescriptionWorkflowUiTest` | API, UI, DB |
| **RX-107** Pickup / point of sale | Sale = quantity × unit price; payment method recorded; amount shown before completing | TC-RX-001, TC-UI-RX-001 | `PrescriptionApiTest`, `PrescriptionWorkflowUiTest` | API, UI, DB |
| **RX-108** Allergy screening (DUR) | Prescription refused when drug class matches a documented allergy | TC-RX-002, TC-UI-RX-003 | `PrescriptionApiTest`, `PrescriptionWorkflowUiTest` | API, UI, DB |
| **RX-109** Controlled substances | Schedule II prescriptions cannot carry refills | TC-RX-003 | `PrescriptionApiTest` | API, DB |
| **RX-110** Refills | Refill re-queues as NEW, decrements refills, needs re-verification; none when 0 remain | TC-RX-008/009 | `PrescriptionApiTest` | API, DB |
| **RX-111** Inventory receiving & low stock | Receipts positive only; never negative; RECEIVE ledger row; low-stock list and filter | TC-INV-001…006, TC-UI-INV-001…003 | `InventoryApiTest`, `InventoryUiTest` | API, UI, DB |
| **RX-112** Daily sales ETL | Summary equals source per day & payment method; idempotent; incremental | TC-ETL-001…004 | `DailySalesEtlTest` | API, DB |
| **RX-113** Audit trail & data integrity | Every status change audited; ledger reconciles; schema rejects invalid data; passwords hashed | TC-DB-001/003 | `DataIntegrityTest`, `DatabaseConstraintTest` | DB |

## Coverage summary

| Stories | With automated tests | Manual-only checks |
|---|---|---|
| 13 | 13 (100%) | UAT walkthrough, keyboard usability, exploratory charters (TC-MAN-001…003) |
