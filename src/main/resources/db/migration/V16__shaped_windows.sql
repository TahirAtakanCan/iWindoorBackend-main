-- V16: Şekilli pencereler — büküm ve şekilli cam aksesuarları.

INSERT INTO accessories (code, name, unit, price, currency, company_id, is_active)
SELECT v.code, v.name, v.unit, v.price, 'TRY', c.id, true
FROM companies c
CROSS JOIN (VALUES
    ('BUKUM', 'Profil büküm (kemer)', 'METER', 350::numeric),
    ('SEKILLI-CAM', 'Şekilli cam farkı', 'PIECE', 250::numeric)
) AS v(code, name, unit, price)
WHERE NOT EXISTS (
    SELECT 1 FROM accessories a WHERE a.company_id = c.id AND a.code = v.code
);
