-- Debitor (Receivable) müştəri FK-sini könüllü et.
-- Səbəb: layihə yalnız companyName (mətn) ilə yarana bilər, müştəri FK (TechRequest.customer) boş ola bilər.
-- Əvvəl customer_id NOT NULL olduğu üçün belə layihələrdə debitor ümumiyyətlə yaranmırdı (görünmürdü).
ALTER TABLE receivables ALTER COLUMN customer_id DROP NOT NULL;
