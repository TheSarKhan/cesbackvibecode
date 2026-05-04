# CES ERP — Backend Brief

## Sistem nədir?
**Construction Equipment Services** şirkəti üçün hazırlanmış ERP sistemi.
Tikinti avadanlıqlarının icarəsi, daşınması və texniki xidmətini idarə edir.
Fərqli departamentlər istifadə edir: mühasibat, koordinasiya, anbar, layihələr və s.
**1-2 həftə ərzində production-a çıxacaq.**

---

## Texniki stack
- Java 21, Spring Boot 3.4.3
- PostgreSQL (port 5434, db: ces_erp), Redis (port 6381)
- JWT access/refresh token, Spring Security, `@PreAuthorize` RBAC
- AOP: `@RequiresApproval`, `AuditLog`
- WebSocket bildirişlər, Email bildirişlər (Gmail SMTP, async)
- Flyway migrasyaları (`db/migration/`)
- OpenPDF (Azərbaycan dilli PDF generasiya)

---

## Əsas biznes axını
```
DRAFT sorğu → PENDING → Koordinatora göndər → Təklif → Qəbul → Layihə → Müqavilə → Aktiv → Tamamlandı → Qaimə → Ödəniş
```

---

## Modul strukturu

Hər modul: `controller → service → repository → entity` (package: `com.ces.erp.{modul}/`)

| Modul | Package | Cədvəl(lər) |
|---|---|---|
| Sorğular | `request` | tech_requests, tech_request_params, request_status_logs |
| Koordinator | `coordinator` | coordinator_plans, coordinator_documents |
| Layihələr | `project` | projects, project_expenses, project_revenues, project_payment_entries |
| Mühasibatlıq | `accounting` | invoices, invoice_transports, accounting_transactions, receivables, receivable_payments, payables, payable_payments, recurring_expenses, generated_documents, document_lines |
| Sənəd konfiqurasiyası | `config` (COMPANY_INFO, DOCUMENT_VAT_RATE, COMPANY_BANK_DETAILS kateqoriyaları) | config_items |
| Qaraj | `garage` | equipment, equipment_documents, equipment_images, equipment_inspections, equipment_status_log, equipment_project_history |
| Müştərilər | `customer` | customers, customer_payment_types, customer_documents |
| Podratçılar | `contractor` | contractors |
| İnvestorlar | `investor` | investors |
| Operatorlar | `operator` | operators, operator_documents |
| Servis | `technicalservice` | service_records, service_checklist_items |
| İstifadəçilər | `user` | users, user_approval_departments |
| Rollar | `role` | roles, role_permissions, role_approval_departments |
| Departamentlər | `department` | departments |
| Banklar | `bank` | banks |
| Konfiqurasiya | `config` | config_items |
| Sistem modulları | `systemmodule` | system_modules |
| Approval | `approval` | pending_operations |
| Audit | `common/audit` | audit_logs |

---

## Enums (17 ədəd — `com.ces.erp.enums/`)

| Enum | Dəyərlər |
|---|---|
| RequestStatus | DRAFT, PENDING, SENT_TO_COORDINATOR, OFFER_SENT, ACCEPTED, REJECTED |
| ProjectStatus | PENDING, ACTIVE, COMPLETED |
| ProjectType | DAILY, MONTHLY |
| EquipmentStatus | AVAILABLE, RENTED, IN_TRANSIT, IN_INSPECTION, UNDER_CHECK, IN_REPAIR, DEFECTIVE, OUT_OF_SERVICE |
| OwnershipType | COMPANY, INVESTOR, CONTRACTOR |
| InvoiceStatus | DRAFT, SENT, APPROVED, RETURNED |
| InvoiceType | INCOME, CONTRACTOR_EXPENSE, COMPANY_EXPENSE, INVESTOR_EXPENSE |
| ReceivableStatus | PENDING, PARTIAL, OVERDUE, COMPLETED |
| PayableStatus | PENDING, PARTIAL, OVERDUE, COMPLETED |
| CustomerStatus | ACTIVE, PASSIVE, VARIABLE |
| ContractorStatus | ACTIVE, INACTIVE |
| RiskLevel | LOW, MEDIUM, HIGH |
| OperationType | EDIT, DELETE |
| OperationStatus | PENDING, APPROVED, REJECTED |
| OperatorDocumentType | DRIVING_LICENSE, CRIMINAL_RECORD, HEALTH_CERTIFICATE, CERTIFICATE, ID_CARD, POWER_OF_ATTORNEY |
| DocumentType | HESAB_FAKTURA, TEHVIL_TESLIM_AKTI, ENGLISH_INVOICE |
| RecurrenceFrequency | MONTHLY, QUARTERLY, ANNUAL |

---

## Entity əlaqələri

```
User ──M:1──> Department
     ──M:1──> Role ──1:N──> RolePermission ──M:1──> SystemModule
     ◄──1:N── UserApprovalDepartment

TechRequest ──M:1──> Customer
           ──M:1──> Equipment
           ──M:1──> User (createdBy)
           ◄──1:1── CoordinatorPlan
           ◄──1:1── Project

Project ──1:1──> TechRequest
       ◄──1:N── ProjectExpense
       ◄──1:N── ProjectRevenue
       ◄──1:N── ProjectPaymentEntry
       ◄──1:1── Receivable
       ◄──1:1── Payable

Invoice ──M:1──> Project
       ──M:1──> Contractor / Investor / Customer
       ◄──1:N── InvoiceTransport

Equipment ──M:1──> Contractor (ownerContractor)
         ──M:1──> User (responsibleUser)
         ◄──1:N── EquipmentInspection, EquipmentDocument, EquipmentImage
         ◄──1:N── EquipmentProjectHistory, EquipmentStatusLog
         ◄──M:M── ConfigItem (safetyEquipment)

CoordinatorPlan ──1:1──> TechRequest
               ──M:1──> Equipment, Operator
               ◄──M:M── ConfigItem (safetyEquipment)
               ◄──1:N── CoordinatorDocument
```

---

## Status axınları

**Sorğu:**
`DRAFT` → `PENDING` → `SENT_TO_COORDINATOR` → `OFFER_SENT` → `ACCEPTED` / `REJECTED`

**Layihə:**
`PENDING` → `ACTIVE` (müqavilə yüklənəndə, Receivable avtoyaranır) → `COMPLETED` (ən az 1 APPROVED qaimə tələb olunur)

**Qaimə:**
`DRAFT` → `SENT` → `APPROVED` (Payable avtoyaranır) / `RETURNED`

**Texnika:**
`AVAILABLE` → `RENTED` (təklif göndərildikdə) → `IN_TRANSIT` (layihə bitdikdə) → `AVAILABLE` (manual)

---

## Approval mexanizmi
`@RequiresApproval` annotasiyası ilə işarələnmiş əməliyyatlar birbaşa tətbiq edilmir:
1. `PendingOperation` cədvəlinə `oldSnapshot` + `newSnapshot` (JSON) yazılır
2. `hasApproval=true` olan istifadəçilər `/api/approval` səhifəsindən görür
3. Təsdiq edildikdə əməliyyat tətbiq olunur, rədd edildikdə silinir

---

## Mühasibatlıq alt-strukturu

### Daimi xərclər (`RecurringExpense`)
- `name`, `categoryKey`, `categoryLabel`, `sourceKey`, `sourceLabel` — kateqoriya/mənbə `config_items`-dan gəlir
- `amount` (0 = dəyişkən), `frequency` (MONTHLY/QUARTERLY/ANNUAL), `dayOfMonth`, `active`

### Rəsmi sənədlər (`GeneratedDocument` + `DocumentLine`)
- Növ: `HESAB_FAKTURA`, `TEHVIL_TESLIM_AKTI`, `ENGLISH_INVOICE`
- `subtotal`, `vatRate`, `vatAmount`, `grandTotal` — hesablanır
- PDF: OpenPDF ilə generasiya, `COMPANY_INFO` + `COMPANY_BANK_DETAILS` config-dən oxunur
- `GET /api/accounting/documents/{id}/pdf` — PDF blob qaytarır

### Dashboard ödənilməmiş qalıqlar
- `DashboardService` — `ReceivableRepository.sumRemainingByStatuses()` + `PayableRepository.sumRemainingByStatuses()`
- PENDING + PARTIAL + OVERDUE statuslu qalıqların cəmi → `totalUnpaidReceivables`, `totalUnpaidPayables`

---

## Email bildirişlər (`common/email/EmailService`)
`notify(moduleCode, subject, body)` — həm modulun userlərinə, həm `hasApproval=true` adminlərə göndərir:
- Sorğu koordinatora gedəndə → `COORDINATOR` modulu
- Koordinator təklif göndərəndə → `REQUESTS` modulu
- Layihə bitəndə → `ACCOUNTING` modulu

---

## Data seeder (`common/seeder/DataSeeder`)
Startup-da bir dəfə işləyir (DB boşdursa):
1. 16 `SystemModule` yaradır
2. Departamentlər yaradır
3. Baza rolları və icazələri yaradır
4. Admin user (`admin@ces.az`) yaradır

**16 sistem modulu:**
CUSTOMER_MANAGEMENT, CONTRACTOR_MANAGEMENT, ROLE_PERMISSION, EMPLOYEE_MANAGEMENT, GARAGE, REQUESTS, COORDINATOR, PROJECTS, ACCOUNTING, SERVICE_MANAGEMENT, INVESTORS, OPERATORS, OPERATIONS_APPROVAL, TRASH, AUDIT_LOG, CONFIG

---

## Ümumi qaydalar
- **Heç vaxt hard delete yoxdur** — `softDelete()`, bütün sorğular `deleted=false` filtrləyir
- **Bütün endpointlər** `ApiResponse<T>{success, message, data}` qaytarır
- **`BaseEntity`**: id (BIGSERIAL), createdAt, updatedAt, deleted, deletedAt
- **İcazə formatı**: `MODULE_CODE:ACTION` (məs: `ACCOUNTING:POST`)
- **Upload yolu**: `/opt/ces-uploads` (prod), `C:/Users/serxa/ces-uploads` (local)
- **PDF**: OpenPDF + DejaVuSans font (Azərbaycan hərfləri dəstəyi)
- **Bütün UI mətnləri Azərbaycanca**
