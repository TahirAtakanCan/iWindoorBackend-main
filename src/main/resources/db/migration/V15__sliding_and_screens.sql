-- V15: Sürme ailesi (sabit/hareketli, kaldır-sür) ve sineklik.

-- 1) accessories.unit: M2
DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE con.contype = 'c'
          AND rel.relname = 'accessories'
          AND pg_get_constraintdef(con.oid) ILIKE '%unit%PIECE%'
    LOOP
        EXECUTE format('ALTER TABLE accessories DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;
ALTER TABLE accessories
    ADD CONSTRAINT accessories_unit_check
        CHECK (unit IN ('PIECE', 'METER', 'SET', 'M2'));

-- 2) hardware_rules.kind: SLIDE_FIXED, LIFT_SLIDE
DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE con.contype = 'c'
          AND rel.relname = 'hardware_rules'
          AND pg_get_constraintdef(con.oid) ILIKE '%kind%TURN%'
    LOOP
        EXECUTE format('ALTER TABLE hardware_rules DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;
ALTER TABLE hardware_rules
    ADD CONSTRAINT hardware_rules_kind_check
        CHECK (kind IN (
            'TURN', 'TILT', 'TILT_TURN', 'SLIDE', 'PASSIVE', 'DOOR', 'DOOR_PASSIVE',
            'SLIDE_FIXED', 'LIFT_SLIDE'
        ));

-- 3) Seri / şirket alanları
ALTER TABLE series
    ADD COLUMN IF NOT EXISTS max_lift_slide_sash_width_mm double precision NOT NULL DEFAULT 3000;

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS screen_min_area_m2 numeric(5, 2) NOT NULL DEFAULT 0.50;

-- 4) Firma aksesuarları (idempotent)
INSERT INTO accessories (code, name, unit, price, currency, company_id, is_active)
SELECT v.code, v.name, v.unit, v.price, 'TRY', c.id, true
FROM companies c
CROSS JOIN (VALUES
    ('HS-SET', 'Kaldır-sür takımı', 'SET', 2400::numeric),
    ('SINEKLIK-PLISE', 'Pileli sineklik', 'M2', 950::numeric),
    ('SINEKLIK-MENTESELI', 'Menteşeli sineklik', 'M2', 700::numeric),
    ('SINEKLIK-SURME', 'Sürme sineklik', 'M2', 600::numeric)
) AS v(code, name, unit, price)
WHERE NOT EXISTS (
    SELECT 1 FROM accessories a WHERE a.company_id = c.id AND a.code = v.code
);

-- 5) Firma donanım kuralları (kind için hiç kural yoksa)
INSERT INTO hardware_rules (company_id, series_id, kind, accessory_code, qty_type, qty, sort_order)
SELECT c.id, NULL, v.kind, v.accessory_code, v.qty_type, v.qty, v.sort_order
FROM companies c
CROSS JOIN (VALUES
    ('SLIDE_FIXED', 'CONTA-EPDM', 'PER_PERIMETER_METER', 1::numeric, 10),
    ('LIFT_SLIDE', 'HS-SET', 'PER_SASH', 1::numeric, 10),
    ('LIFT_SLIDE', 'CONTA-EPDM', 'PER_PERIMETER_METER', 1::numeric, 20)
) AS v(kind, accessory_code, qty_type, qty, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM hardware_rules hr WHERE hr.company_id = c.id AND hr.kind = v.kind
);
