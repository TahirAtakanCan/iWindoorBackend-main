-- V17: Panjur ve ek profiller (denizlik, pervaz, kasa genişletme).

ALTER TABLE series
    ADD COLUMN IF NOT EXISTS shutter_box_height_mm double precision NOT NULL DEFAULT 180,
    ADD COLUMN IF NOT EXISTS sill_overhang_mm      double precision NOT NULL DEFAULT 30,
    ADD COLUMN IF NOT EXISTS trim_width_mm         double precision NOT NULL DEFAULT 70;

ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS shutter_min_area_m2 numeric(5, 2) NOT NULL DEFAULT 1.00;

-- Firma aksesuarları (idempotent)
INSERT INTO accessories (code, name, unit, price, currency, company_id, is_active)
SELECT v.code, v.name, v.unit, v.price, 'TRY', c.id, true
FROM companies c
CROSS JOIN (VALUES
    ('PANJUR-LAMEL-AL', 'Alüminyum panjur lameli', 'M2', 1100::numeric),
    ('PANJUR-LAMEL-PVC', 'PVC panjur lameli', 'M2', 700::numeric),
    ('PANJUR-KUTU-MONO', 'Monoblok panjur kutusu', 'METER', 650::numeric),
    ('PANJUR-KUTU-DIS', 'Dıştan panjur kutusu', 'METER', 550::numeric),
    ('PANJUR-RAY', 'Panjur yan rayı', 'METER', 120::numeric),
    ('PANJUR-MOTOR', 'Panjur motoru', 'PIECE', 2200::numeric),
    ('PANJUR-KAYIS', 'Panjur kayış/makara takımı', 'SET', 450::numeric),
    ('DENIZLIK-DIS', 'Dış denizlik (alüminyum)', 'METER', 280::numeric),
    ('DENIZLIK-IC', 'İç denizlik (PVC)', 'METER', 180::numeric),
    ('PERVAZ', 'İç pervaz', 'METER', 60::numeric)
) AS v(code, name, unit, price)
WHERE NOT EXISTS (
    SELECT 1 FROM accessories a WHERE a.company_id = c.id AND a.code = v.code
);

-- Seriye EXTENSION profili yoksa ekle
INSERT INTO profiles (code, name, type, width_mm, price_per_meter, currency, company_id, bar_length_mm,
                      is_default, is_active, series_id)
SELECT 'GN-' || s.id, 'Kasa Genişletme', 'EXTENSION', 30, 95, 'TRY', s.company_id, 6500, true, true, s.id
FROM series s
WHERE s.company_id IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM profiles p WHERE p.series_id = s.id AND p.type = 'EXTENSION');
