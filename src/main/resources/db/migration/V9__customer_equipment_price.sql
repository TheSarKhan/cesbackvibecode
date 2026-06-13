-- Koordinatorun sifarişçiyə təklif edəcəyi texnika qiyməti (revenue).
-- Mövcud equipment_price field koordinatorun podratçı/investora ödəyəcəyimiz
-- xərc kimi qalır; bu yeni field isə sifarişçiyə təklif olunan xidmət məbləğidir.
ALTER TABLE coordinator_plans
    ADD COLUMN IF NOT EXISTS customer_equipment_price NUMERIC(12, 2);
