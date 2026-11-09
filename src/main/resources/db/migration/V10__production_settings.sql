-- V10: Üretim ayarları (kesim optimizasyonu ve cam siparişi).

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS saw_kerf_mm           numeric(5, 1)  NOT NULL DEFAULT 4,
    ADD COLUMN IF NOT EXISTS bar_trim_mm           numeric(5, 1)  NOT NULL DEFAULT 10,
    ADD COLUMN IF NOT EXISTS min_offcut_mm         integer        NOT NULL DEFAULT 500,
    ADD COLUMN IF NOT EXISTS glass_supplier_name   varchar(200),
    ADD COLUMN IF NOT EXISTS glass_supplier_phone  varchar(50),
    ADD COLUMN IF NOT EXISTS glass_supplier_email  varchar(200),
    ADD COLUMN IF NOT EXISTS glass_order_note      varchar(1000);
