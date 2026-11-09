-- V8: Orta kayıtsız çift kanat (adaptör), kapı kanadı ve eşik desteği.

-- 1) profiles.type CHECK: DOOR_SASH, THRESHOLD
DO $$
DECLARE r record;
BEGIN
    FOR r IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE con.contype = 'c'
          AND rel.relname = 'profiles'
          AND pg_get_constraintdef(con.oid) ILIKE '%type%FRAME%'
    LOOP
        EXECUTE format('ALTER TABLE profiles DROP CONSTRAINT %I', r.conname);
    END LOOP;
END $$;
ALTER TABLE profiles
    ADD CONSTRAINT profiles_type_check
        CHECK (type IN ('FRAME', 'SASH', 'MULLION', 'BEAD', 'ADAPTER', 'EXTENSION', 'DOOR_SASH', 'THRESHOLD'));

-- 2) hardware_rules.kind CHECK: PASSIVE, DOOR, DOOR_PASSIVE
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
        CHECK (kind IN ('TURN', 'TILT', 'TILT_TURN', 'SLIDE', 'PASSIVE', 'DOOR', 'DOOR_PASSIVE'));

-- 3) Seri geometri / kapı sınırları
ALTER TABLE series
    ADD COLUMN IF NOT EXISTS adapter_face_mm            double precision NOT NULL DEFAULT 40,
    ADD COLUMN IF NOT EXISTS door_sash_face_mm          double precision NOT NULL DEFAULT 110,
    ADD COLUMN IF NOT EXISTS threshold_face_mm          double precision NOT NULL DEFAULT 20,
    ADD COLUMN IF NOT EXISTS threshold_end_allowance_mm double precision NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS min_door_sash_width_mm     double precision NOT NULL DEFAULT 600,
    ADD COLUMN IF NOT EXISTS max_door_sash_width_mm     double precision NOT NULL DEFAULT 1100,
    ADD COLUMN IF NOT EXISTS min_door_sash_height_mm    double precision NOT NULL DEFAULT 1800,
    ADD COLUMN IF NOT EXISTS max_door_sash_height_mm    double precision NOT NULL DEFAULT 2500;

-- 4) Mevcut şirket serilerine eksik profiller (tek seferlik, idempotent)
INSERT INTO profiles (code, name, type, width_mm, price_per_meter, currency, company_id, bar_length_mm,
                      is_default, is_active, series_id)
SELECT 'KN-KAPI-' || s.id, 'Kapı Kanadı', 'DOOR_SASH', 110, 260, 'TRY', s.company_id, 6500, true, true, s.id
FROM series s
WHERE s.company_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.series_id = s.id AND p.type = 'DOOR_SASH');

INSERT INTO profiles (code, name, type, width_mm, price_per_meter, currency, company_id, bar_length_mm,
                      is_default, is_active, series_id)
SELECT 'ESIK-' || s.id, 'Alüminyum Eşik', 'THRESHOLD', 20, 310, 'TRY', s.company_id, 6500, true, true, s.id
FROM series s
WHERE s.company_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.series_id = s.id AND p.type = 'THRESHOLD');

INSERT INTO profiles (code, name, type, width_mm, price_per_meter, currency, company_id, bar_length_mm,
                      is_default, is_active, series_id)
SELECT 'AD-' || s.id, 'Çift Kanat Adaptörü', 'ADAPTER', 40, 120, 'TRY', s.company_id, 6500, true, true, s.id
FROM series s
WHERE s.company_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.series_id = s.id AND p.type = 'ADAPTER');

-- 5) Firma aksesuarları
INSERT INTO accessories (code, name, unit, price, currency, company_id, is_active)
SELECT v.code, v.name, v.unit, v.price, 'TRY', c.id, true
FROM companies c
CROSS JOIN (VALUES
    ('PASIF-KANAT-SET', 'Pasif kanat takımı', 'SET', 320::numeric),
    ('KAPI-KILIT-SET', 'Kapı kilit takımı', 'SET', 950::numeric),
    ('KAPI-KOLU', 'Kapı kolu', 'PIECE', 380::numeric),
    ('KAPI-MENTESE', 'Kapı menteşesi', 'PIECE', 140::numeric)
) AS v(code, name, unit, price)
WHERE NOT EXISTS (
    SELECT 1 FROM accessories a WHERE a.company_id = c.id AND a.code = v.code
);

-- 6) Firma donanım kuralları (kind için hiç kural yoksa)
INSERT INTO hardware_rules (company_id, series_id, kind, accessory_code, qty_type, qty, sort_order)
SELECT c.id, NULL, v.kind, v.accessory_code, v.qty_type, v.qty, v.sort_order
FROM companies c
CROSS JOIN (VALUES
    ('PASSIVE', 'PASIF-KANAT-SET', 'PER_SASH', 1::numeric, 10),
    ('PASSIVE', 'MENTESE-SET', 'PER_SASH', 1::numeric, 20),
    ('PASSIVE', 'CONTA-EPDM', 'PER_PERIMETER_METER', 1::numeric, 30),
    ('DOOR', 'KAPI-KILIT-SET', 'PER_SASH', 1::numeric, 10),
    ('DOOR', 'KAPI-KOLU', 'PER_SASH', 1::numeric, 20),
    ('DOOR', 'KAPI-MENTESE', 'PER_SASH', 3::numeric, 30),
    ('DOOR', 'CONTA-EPDM', 'PER_PERIMETER_METER', 1::numeric, 40),
    ('DOOR_PASSIVE', 'PASIF-KANAT-SET', 'PER_SASH', 1::numeric, 10),
    ('DOOR_PASSIVE', 'KAPI-MENTESE', 'PER_SASH', 3::numeric, 20),
    ('DOOR_PASSIVE', 'CONTA-EPDM', 'PER_PERIMETER_METER', 1::numeric, 30)
) AS v(kind, accessory_code, qty_type, qty, sort_order)
WHERE NOT EXISTS (
    SELECT 1 FROM hardware_rules hr WHERE hr.company_id = c.id AND hr.kind = v.kind
);
