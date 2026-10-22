# Defect report — example

> An example of the level of detail expected for a JIRA defect in this project. The scenario
> illustrates a classic reporting bug that the `DailySalesEtlTest` suite is designed to catch.

| Field | Value |
|---|---|
| **Key** | RX-241 |
| **Summary** | Daily sales summary omits sales made after 8 PM Eastern |
| **Type / Severity / Priority** | Bug / High / P2 |
| **Linked story** | RX-112 Daily sales ETL |
| **Found by** | Automated regression — `DailySalesEtlTest.etlLoadsAccurateAggregates` (nightly run) |
| **Environment** | ci · build #318 · PostgreSQL 16 · app container TZ=America/New_York |
| **Component** | Reporting / ETL |

## Steps to reproduce
1. Start the app with JVM time zone `America/New_York`.
2. Between 20:00 and 23:59 Eastern, pick up any filled prescription (sale is written to `pharmacy_transactions`).
3. `POST /api/reports/daily-sales/run` for today's date.
4. Compare the summary with the source:
   ```sql
   SELECT payment_method, COUNT(*), SUM(amount)
   FROM pharmacy_transactions
   WHERE (created_at AT TIME ZONE 'UTC')::date = CURRENT_DATE
   GROUP BY payment_method;
   ```

## Expected
Target table `daily_sales_summary` equals the source aggregate for every payment method.

## Actual
The CASH row shows 1 fewer transaction and $4.50 less than the source. The sale is assigned to the *next* day because the job bucketed by the session's local date while the report date is in UTC.

## Evidence
- Allure: failed assertion `target table matches source aggregate`, with both result sets attached as SQL attachments.
- Screenshot not applicable (backend).

## Root cause (after investigation with dev)
The ETL query used `created_at::date`, which depends on the database session time zone set by the JVM. The rest of the reporting stack uses UTC.

## Fix & verification
- Fix: bucket by `(created_at AT TIME ZONE 'UTC')::date` in the job and the dashboard query.
- Re-tested with the reproduction steps above: pass.
- Regression: `DailySalesEtlTest` already covers the comparison; added a note in the test strategy (Risks) and pinned all reporting to UTC.
