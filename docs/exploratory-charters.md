# Exploratory testing charters

Time-boxed sessions (60 min) run each sprint, simulating real pharmacist and technician work. Findings become defects, new automated tests, or questions for refinement.

| ID | Charter | Persona | Focus / heuristics |
|---|---|---|---|
| EXP-01 | Explore **prescription intake** with messy real-world input to discover validation gaps | Technician at a busy counter | Pasted values with spaces, very long SIGs, unusual prescriber names, switching patient mid-entry, double-clicking Create |
| EXP-02 | Explore **verification and fill** under interruption to discover state and concurrency problems | Pharmacist + technician on two browsers | Two users acting on the same Rx, stale screens, back button, refreshing mid-action |
| EXP-03 | Explore **inventory at shift start** to discover gaps between what the screen says and what is on the shelf | Lead technician | Low-stock filter, receiving partial deliveries, correcting a wrong receipt with ADJUST |
| EXP-04 | Explore **patient search** with ambiguous data to discover findability issues | Technician answering the phone | Same last name, hyphenated names, apostrophes, phone-number search |
| EXP-05 | Explore **session and role boundaries** to discover security weaknesses | Curious technician | Deep links, expired session, calling pharmacist-only APIs from devtools |

## Session notes template

```
Charter:     EXP-0x
Tester/date: 
Build:       
Duration:    60 min (setup 5 / test 45 / notes 10)

Areas covered:
Bugs found (JIRA keys):
Questions for PO / dev:
New automation candidates:
```
