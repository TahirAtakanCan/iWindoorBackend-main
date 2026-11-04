-- V4: Proje müşteri bilgileri + seri kanat ölçü sınırları.

ALTER TABLE projects
    ADD COLUMN IF NOT EXISTS customer_name    varchar(200),
    ADD COLUMN IF NOT EXISTS customer_phone   varchar(50),
    ADD COLUMN IF NOT EXISTS customer_email   varchar(200),
    ADD COLUMN IF NOT EXISTS customer_address varchar(500);

ALTER TABLE series
    ADD COLUMN IF NOT EXISTS min_sash_width_mm         double precision NOT NULL DEFAULT 350,
    ADD COLUMN IF NOT EXISTS min_sash_height_mm        double precision NOT NULL DEFAULT 350,
    ADD COLUMN IF NOT EXISTS max_sash_width_mm         double precision NOT NULL DEFAULT 1400,
    ADD COLUMN IF NOT EXISTS max_sash_height_mm        double precision NOT NULL DEFAULT 2400,
    ADD COLUMN IF NOT EXISTS max_slide_sash_width_mm   double precision NOT NULL DEFAULT 2000,
    ADD COLUMN IF NOT EXISTS max_fixed_glass_area_m2   double precision NOT NULL DEFAULT 6.0;
