-- V2: Şirket bazlı katalog, seri geometrisi (çizim motoru), tasarım belgesi (JSONB) ve iyimser kilit.

-- 1) SERİ: şirket + geometri (mm). Varsayılanlar 70 seri örnek değerleri.
ALTER TABLE series
    ADD COLUMN IF NOT EXISTS company_id               bigint REFERENCES companies (id),
    ADD COLUMN IF NOT EXISTS material                 varchar(16)      NOT NULL DEFAULT 'PVC',
    ADD COLUMN IF NOT EXISTS is_active                boolean          NOT NULL DEFAULT true,
    ADD COLUMN IF NOT EXISTS frame_face_mm            double precision NOT NULL DEFAULT 62,
    ADD COLUMN IF NOT EXISTS sash_face_mm             double precision NOT NULL DEFAULT 78,
    ADD COLUMN IF NOT EXISTS mullion_face_mm          double precision NOT NULL DEFAULT 80,
    ADD COLUMN IF NOT EXISTS bead_face_mm             double precision NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS sash_lap_mm              double precision NOT NULL DEFAULT 8,
    ADD COLUMN IF NOT EXISTS glass_rebate_frame_mm    double precision NOT NULL DEFAULT 14,
    ADD COLUMN IF NOT EXISTS glass_rebate_sash_mm     double precision NOT NULL DEFAULT 14,
    ADD COLUMN IF NOT EXISTS glass_rebate_mullion_mm  double precision NOT NULL DEFAULT 14,
    ADD COLUMN IF NOT EXISTS weld_allowance_mm        double precision NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS mullion_end_allowance_mm double precision NOT NULL DEFAULT 3,
    ADD COLUMN IF NOT EXISTS slide_interlock_mm       double precision NOT NULL DEFAULT 40,
    ADD COLUMN IF NOT EXISTS duplex_width_mm          double precision NOT NULL DEFAULT 18;
CREATE INDEX IF NOT EXISTS ix_series_company ON series (company_id);

-- 2) PROFİL: şirket, boy, çıta-cam kalınlığı aralığı, varsayılan bayrağı
ALTER TABLE profiles
    ADD COLUMN IF NOT EXISTS company_id     bigint REFERENCES companies (id),
    ADD COLUMN IF NOT EXISTS bar_length_mm  integer DEFAULT 6500,
    ADD COLUMN IF NOT EXISTS min_glass_mm   double precision,
    ADD COLUMN IF NOT EXISTS max_glass_mm   double precision,
    ADD COLUMN IF NOT EXISTS is_default     boolean NOT NULL DEFAULT false,
    ADD COLUMN IF NOT EXISTS is_active      boolean NOT NULL DEFAULT true;
CREATE INDEX IF NOT EXISTS ix_profiles_company ON profiles (company_id);
CREATE INDEX IF NOT EXISTS ix_profiles_series ON profiles (series_id);

-- 3) CAM: kod (tasarım belgesi buna bağlanır), görünüm, şirket
ALTER TABLE glasses
    ADD COLUMN IF NOT EXISTS code          varchar(64),
    ADD COLUMN IF NOT EXISTS visual_style  varchar(16) NOT NULL DEFAULT 'clear',
    ADD COLUMN IF NOT EXISTS company_id    bigint REFERENCES companies (id);
UPDATE glasses SET code = 'G' || id WHERE code IS NULL;
CREATE INDEX IF NOT EXISTS ix_glasses_company ON glasses (company_id);

-- 4) AKSESUAR: şirket
ALTER TABLE accessories
    ADD COLUMN IF NOT EXISTS company_id bigint REFERENCES companies (id);
CREATE INDEX IF NOT EXISTS ix_accessories_company ON accessories (company_id);

-- 5) Stok kodları artık ŞİRKET İÇİNDE benzersiz (her şirket şablonun kopyasını aynı kodlarla alır).
--    Hibernate'in ürettiği tek sütunlu UNIQUE(code) kısıtlarını adından bağımsız bul ve kaldır.
DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT con.conname, rel.relname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = ANY (con.conkey)
        WHERE con.contype = 'u'
          AND rel.relname IN ('profiles', 'accessories')
          AND att.attname = 'code'
          AND array_length(con.conkey, 1) = 1
    LOOP
        EXECUTE format('ALTER TABLE %I DROP CONSTRAINT %I', r.relname, r.conname);
    END LOOP;
END $$;
CREATE UNIQUE INDEX IF NOT EXISTS ux_profiles_company_code ON profiles (coalesce(company_id, 0), code);
CREATE UNIQUE INDEX IF NOT EXISTS ux_glasses_company_code ON glasses (coalesce(company_id, 0), code);
CREATE UNIQUE INDEX IF NOT EXISTS ux_accessories_company_code ON accessories (coalesce(company_id, 0), code)
    WHERE code IS NOT NULL;

-- 6) PENCERE: tasarım belgesi, kesim listesi, seri, iyimser kilit
ALTER TABLE window_units
    ADD COLUMN IF NOT EXISTS design          jsonb,
    ADD COLUMN IF NOT EXISTS design_version  integer   NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS bom_snapshot    jsonb,
    ADD COLUMN IF NOT EXISTS series_id       bigint REFERENCES series (id),
    ADD COLUMN IF NOT EXISTS row_version     bigint    NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS updated_at      timestamp(6);
CREATE INDEX IF NOT EXISTS ix_window_units_project ON window_units (project_id);
CREATE INDEX IF NOT EXISTS ix_projects_user ON projects (user_id);
CREATE INDEX IF NOT EXISTS ix_window_nodes_parent ON window_nodes (parent_id);
