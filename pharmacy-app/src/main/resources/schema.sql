-- =====================================================================
-- RxCare Pharmacy (demo system under test) - PostgreSQL schema
-- The schema is recreated on every application start so each test run
-- begins from a known, deterministic baseline.
-- =====================================================================

DROP TABLE IF EXISTS daily_sales_summary CASCADE;
DROP TABLE IF EXISTS pharmacy_transactions CASCADE;
DROP TABLE IF EXISTS inventory_transactions CASCADE;
DROP TABLE IF EXISTS prescription_status_history CASCADE;
DROP TABLE IF EXISTS prescriptions CASCADE;
DROP TABLE IF EXISTS drugs CASCADE;
DROP TABLE IF EXISTS patient_allergies CASCADE;
DROP TABLE IF EXISTS patients CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP SEQUENCE IF EXISTS rx_number_seq;

CREATE SEQUENCE rx_number_seq START WITH 1000001;

CREATE TABLE users (
    id            SERIAL PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash CHAR(64)     NOT NULL,
    display_name  VARCHAR(100) NOT NULL,
    role          VARCHAR(20)  NOT NULL CHECK (role IN ('PHARMACIST', 'TECHNICIAN')),
    active        BOOLEAN      NOT NULL DEFAULT TRUE
);

CREATE TABLE patients (
    id             SERIAL PRIMARY KEY,
    first_name     VARCHAR(50)  NOT NULL,
    last_name      VARCHAR(50)  NOT NULL,
    date_of_birth  DATE         NOT NULL CHECK (date_of_birth <= CURRENT_DATE),
    gender         CHAR(1)      NOT NULL CHECK (gender IN ('M', 'F', 'U')),
    phone          VARCHAR(12)  NOT NULL,
    email          VARCHAR(100),
    address        VARCHAR(200),
    insurance_plan VARCHAR(50),
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_patient_identity UNIQUE (first_name, last_name, date_of_birth)
);

CREATE TABLE patient_allergies (
    id         SERIAL PRIMARY KEY,
    patient_id INT         NOT NULL REFERENCES patients (id) ON DELETE CASCADE,
    allergen   VARCHAR(50) NOT NULL,
    CONSTRAINT uq_patient_allergen UNIQUE (patient_id, allergen)
);

CREATE TABLE drugs (
    ndc           VARCHAR(13)    PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    strength      VARCHAR(30)    NOT NULL,
    dosage_form   VARCHAR(30)    NOT NULL,
    drug_class    VARCHAR(50)    NOT NULL,
    schedule      SMALLINT       NOT NULL DEFAULT 0 CHECK (schedule BETWEEN 0 AND 5),
    unit_price    NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    qty_on_hand   INT            NOT NULL CHECK (qty_on_hand >= 0),
    reorder_level INT            NOT NULL CHECK (reorder_level >= 0)
);

CREATE TABLE prescriptions (
    id                SERIAL PRIMARY KEY,
    rx_number         VARCHAR(10)  NOT NULL UNIQUE,
    patient_id        INT          NOT NULL REFERENCES patients (id),
    ndc               VARCHAR(13)  NOT NULL REFERENCES drugs (ndc),
    prescriber        VARCHAR(100) NOT NULL,
    sig               VARCHAR(200) NOT NULL,
    quantity          INT          NOT NULL CHECK (quantity > 0),
    days_supply       INT          NOT NULL CHECK (days_supply BETWEEN 1 AND 90),
    refills           INT          NOT NULL CHECK (refills BETWEEN 0 AND 11),
    refills_remaining INT          NOT NULL,
    status            VARCHAR(20)  NOT NULL
        CHECK (status IN ('NEW', 'VERIFIED', 'FILLED', 'PICKED_UP', 'CANCELLED')),
    fill_count        INT          NOT NULL DEFAULT 0,
    created_by        VARCHAR(50)  NOT NULL,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    verified_by       VARCHAR(50),
    verified_at       TIMESTAMPTZ,
    filled_by         VARCHAR(50),
    filled_at         TIMESTAMPTZ,
    picked_up_at      TIMESTAMPTZ,
    cancel_reason     VARCHAR(200),
    CONSTRAINT ck_refills_remaining CHECK (refills_remaining BETWEEN 0 AND refills)
);

CREATE TABLE prescription_status_history (
    id              SERIAL PRIMARY KEY,
    prescription_id INT         NOT NULL REFERENCES prescriptions (id),
    from_status     VARCHAR(20),
    to_status       VARCHAR(20) NOT NULL,
    changed_by      VARCHAR(50) NOT NULL,
    changed_at      TIMESTAMPTZ NOT NULL DEFAULT CLOCK_TIMESTAMP()
);

CREATE TABLE inventory_transactions (
    id              SERIAL PRIMARY KEY,
    ndc             VARCHAR(13)  NOT NULL REFERENCES drugs (ndc),
    prescription_id INT          REFERENCES prescriptions (id),
    txn_type        VARCHAR(10)  NOT NULL CHECK (txn_type IN ('RECEIVE', 'DISPENSE', 'ADJUST')),
    quantity        INT          NOT NULL CHECK (quantity <> 0),
    reason          VARCHAR(200),
    performed_by    VARCHAR(50)  NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE pharmacy_transactions (
    id              SERIAL PRIMARY KEY,
    prescription_id INT            NOT NULL REFERENCES prescriptions (id),
    amount          NUMERIC(10, 2) NOT NULL CHECK (amount >= 0),
    payment_method  VARCHAR(10)    NOT NULL CHECK (payment_method IN ('CASH', 'CARD', 'INSURANCE')),
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW()
);

-- Target table of the nightly "daily sales" ETL job (source: pharmacy_transactions)
CREATE TABLE daily_sales_summary (
    sales_date        DATE           NOT NULL,
    payment_method    VARCHAR(10)    NOT NULL,
    transaction_count INT            NOT NULL,
    total_amount      NUMERIC(12, 2) NOT NULL,
    refreshed_at      TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    PRIMARY KEY (sales_date, payment_method)
);

CREATE INDEX idx_rx_status ON prescriptions (status);
CREATE INDEX idx_rx_patient ON prescriptions (patient_id);
CREATE INDEX idx_inv_txn_ndc ON inventory_transactions (ndc);
CREATE INDEX idx_hist_rx ON prescription_status_history (prescription_id);
