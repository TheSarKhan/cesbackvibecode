-- 2026 rəsmi formata uyğun olaraq tibbi sığorta thresholdunu 8000 → 2500 yeniləyirik.
-- Qeyri-neft-qaz, qeyri-dövlət sektoru üçün rəsmi 2026 cədvəlinə əsasən.

UPDATE hr_tax_rate_configs
SET employee_medical_threshold = 2500.0000,
    employer_medical_threshold = 2500.0000
WHERE employee_medical_threshold = 8000.0000
   OR employer_medical_threshold = 8000.0000;
