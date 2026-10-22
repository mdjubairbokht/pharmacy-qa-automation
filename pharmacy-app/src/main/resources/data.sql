-- =====================================================================
-- Deterministic seed data. All names, NDCs and prescribers are fictional.
-- =====================================================================

-- Passwords are stored as SHA-256 hex digests (never in plain text).
INSERT INTO users (username, password_hash, display_name, role, active) VALUES
    ('rph.jones',   encode(sha256('Pharm@123'::bytea),  'hex'), 'Dana Jones, RPh',  'PHARMACIST', TRUE),
    ('tech.smith',  encode(sha256('Tech@123'::bytea),   'hex'), 'Chris Smith, CPhT', 'TECHNICIAN', TRUE),
    ('locked.user', encode(sha256('Locked@123'::bytea), 'hex'), 'Locked Account',    'TECHNICIAN', FALSE);

INSERT INTO patients (first_name, last_name, date_of_birth, gender, phone, email, address, insurance_plan) VALUES
    ('John',   'Carter',  DATE '1958-04-12', 'M', '412-555-0101', 'john.carter@example.com', '100 Liberty Ave, Pittsburgh, PA', 'MEDICARE-D'),
    ('Maria',  'Lopez',   DATE '1972-09-30', 'F', '412-555-0102', 'maria.lopez@example.com', '22 Forbes Ave, Pittsburgh, PA',   'BLUE-PPO'),
    ('Robert', 'Chen',    DATE '1985-01-22', 'M', '412-555-0103', NULL,                      '8 Penn Ave, Moon Township, PA',   'AETNA-HMO'),
    ('Aisha',  'Patel',   DATE '1990-06-05', 'F', '412-555-0104', 'aisha.patel@example.com', '415 Smithfield St, Pittsburgh, PA', NULL),
    ('Emily',  'Johnson', DATE '2001-11-17', 'F', '412-555-0105', NULL,                      '77 Grant St, Pittsburgh, PA',     'CASH');

INSERT INTO patient_allergies (patient_id, allergen) VALUES
    (1, 'PENICILLIN'),
    (2, 'SULFONAMIDE'),
    (4, 'OPIOID');

INSERT INTO drugs (ndc, name, strength, dosage_form, drug_class, schedule, unit_price, qty_on_hand, reorder_level) VALUES
    ('50000-0101-01', 'Amoxicillin',                   '500 mg',     'Capsule', 'PENICILLIN',    0,  0.25, 5000, 500),
    ('50000-0202-01', 'Lisinopril',                    '10 mg',      'Tablet',  'ACE_INHIBITOR', 0,  0.15, 5000, 500),
    ('50000-0303-01', 'Atorvastatin',                  '20 mg',      'Tablet',  'STATIN',        0,  0.30, 5000, 500),
    ('50000-0404-01', 'Metformin',                     '500 mg',     'Tablet',  'BIGUANIDE',     0,  0.10, 5000, 500),
    ('50000-0505-01', 'Sertraline',                    '50 mg',      'Tablet',  'SSRI',          0,  0.35, 3000, 300),
    ('50000-0606-01', 'Sulfamethoxazole/Trimethoprim', '800/160 mg', 'Tablet',  'SULFONAMIDE',   0,  0.40, 2000, 200),
    ('50000-0707-01', 'Oxycodone HCl',                 '5 mg',       'Tablet',  'OPIOID',        2,  1.20, 1000, 100),
    ('50000-0808-01', 'Albuterol HFA',                 '90 mcg',     'Inhaler', 'BETA_AGONIST',  0, 45.00,    8,  20),
    ('50000-0909-01', 'Omeprazole',                    '20 mg',      'Capsule', 'PPI',           0,  0.20, 4000, 400);

-- Opening balances are recorded as RECEIVE transactions so that
-- qty_on_hand always equals SUM(inventory_transactions.quantity).
INSERT INTO inventory_transactions (ndc, txn_type, quantity, reason, performed_by)
SELECT ndc, 'RECEIVE', qty_on_hand, 'Opening balance', 'system' FROM drugs;

INSERT INTO prescriptions (rx_number, patient_id, ndc, prescriber, sig, quantity, days_supply, refills, refills_remaining, status, created_by) VALUES
    ('RX' || nextval('rx_number_seq'), 3, '50000-0202-01', 'Dr. Alan Grant', 'Take 1 tablet by mouth once daily',         30, 30, 5, 5, 'NEW', 'tech.smith'),
    ('RX' || nextval('rx_number_seq'), 1, '50000-0303-01', 'Dr. Ellie Sattler', 'Take 1 tablet by mouth at bedtime',       90, 90, 3, 3, 'NEW', 'tech.smith');

INSERT INTO prescriptions (rx_number, patient_id, ndc, prescriber, sig, quantity, days_supply, refills, refills_remaining, status, created_by, verified_by, verified_at) VALUES
    ('RX' || nextval('rx_number_seq'), 5, '50000-0505-01', 'Dr. Ian Malcolm', 'Take 1 tablet by mouth every morning', 30, 30, 2, 2, 'VERIFIED', 'tech.smith', 'rph.jones', NOW());

INSERT INTO prescription_status_history (prescription_id, from_status, to_status, changed_by) VALUES
    (1, NULL,  'NEW',      'tech.smith'),
    (2, NULL,  'NEW',      'tech.smith'),
    (3, NULL,  'NEW',      'tech.smith'),
    (3, 'NEW', 'VERIFIED', 'rph.jones');
