-- V3: Firma profili ve teklif ayarları (şirket bilgileri, KDV, iskonto, para birimi, teklif notu).

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS phone                 varchar(50),
    ADD COLUMN IF NOT EXISTS email                 varchar(200),
    ADD COLUMN IF NOT EXISTS tax_office            varchar(120),
    ADD COLUMN IF NOT EXISTS vat_rate              numeric(5, 2)  NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS default_discount_pct  numeric(5, 2)  NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS currency              varchar(8)     NOT NULL DEFAULT 'TRY',
    ADD COLUMN IF NOT EXISTS quote_note            varchar(2000),
    ADD COLUMN IF NOT EXISTS quote_validity_days   integer        NOT NULL DEFAULT 15;
