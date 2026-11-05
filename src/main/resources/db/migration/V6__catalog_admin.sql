-- V6: Aksesuar pasifleştirme (katalog yönetimi).

ALTER TABLE accessories
    ADD COLUMN IF NOT EXISTS is_active boolean NOT NULL DEFAULT true;
