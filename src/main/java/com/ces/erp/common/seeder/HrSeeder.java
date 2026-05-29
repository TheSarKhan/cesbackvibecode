package com.ces.erp.common.seeder;

import com.ces.erp.department.entity.Department;
import com.ces.erp.department.repository.DepartmentRepository;
import com.ces.erp.enums.EmployeeStatus;
import com.ces.erp.enums.Gender;
import com.ces.erp.hr.entity.Employee;
import com.ces.erp.hr.entity.Position;
import com.ces.erp.hr.entity.TaxRateConfig;
import com.ces.erp.hr.repository.EmployeeRepository;
import com.ces.erp.hr.repository.PositionRepository;
import com.ces.erp.hr.repository.TaxRateConfigRepository;
import com.ces.erp.systemmodule.repository.SystemModuleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(10)
@RequiredArgsConstructor
@Slf4j
public class HrSeeder implements CommandLineRunner {

    private final TaxRateConfigRepository taxRepo;
    private final PositionRepository positionRepo;
    private final EmployeeRepository employeeRepo;
    private final DepartmentRepository departmentRepo;
    private final SystemModuleRepository moduleRepo;

    @Override
    @Transactional
    public void run(String... args) {
        seedTaxRates();
        seedPositions();
        ensureHrModule();
        seedEmployees();
    }

    // ─── Vergi tarifləri ─────────────────────────────────────────────────────

    private void seedTaxRates() {
        if (taxRepo.count() > 0) return;
        int year = LocalDate.now().getYear();
        log.info("HR: {} ili üçün default vergi tarifləri seed edilir...", year);

        TaxRateConfig cfg = TaxRateConfig.builder()
                .year(year)
                .active(true)
                .employeePensionThreshold(new BigDecimal("200.0000"))
                .employeePensionRateBelow(new BigDecimal("0.0300"))
                .employeePensionRateAbove(new BigDecimal("0.1000"))
                .employerPensionThreshold(new BigDecimal("200.0000"))
                .employerPensionRateBelow(new BigDecimal("0.2200"))
                .employerPensionRateAbove(new BigDecimal("0.1500"))
                .employeeUnemploymentRate(new BigDecimal("0.0050"))
                .employerUnemploymentRate(new BigDecimal("0.0050"))
                .employeeMedicalThreshold(new BigDecimal("2500.0000"))
                .employeeMedicalRateBelow(new BigDecimal("0.0200"))
                .employeeMedicalRateAbove(new BigDecimal("0.0050"))
                .employerMedicalThreshold(new BigDecimal("2500.0000"))
                .employerMedicalRateBelow(new BigDecimal("0.0200"))
                .employerMedicalRateAbove(new BigDecimal("0.0050"))
                .incomeTaxThreshold(new BigDecimal("8000.0000"))
                .incomeTaxRateBelow(new BigDecimal("0.0000"))
                .incomeTaxRateAbove(new BigDecimal("0.1400"))
                .nonTaxableMinimum(BigDecimal.ZERO)
                .deductSocialFromTaxBase(false)
                .notes("Qeyri-neft-qaz, qeyri-dövlət sektoru üçün default tarif")
                .build();
        taxRepo.save(cfg);
        log.info("Default vergi tarifi yaradıldı.");
    }

    // ─── Vəzifələr ───────────────────────────────────────────────────────────

    private void seedPositions() {
        if (positionRepo.count() > 0) return;
        log.info("HR: default vəzifələr seed edilir...");
        positionRepo.saveAll(List.of(
                pos("Direktor",          "Şirkət direktoru",              new BigDecimal("9000.00")),
                pos("Maliyyə Direktoru", "Maliyyə şöbəsi rəhbəri",        new BigDecimal("6000.00")),
                pos("Baş Mühasib",       "Baş mühasib",                   new BigDecimal("3500.00")),
                pos("Mühasib",           "Mühasib",                       new BigDecimal("2000.00")),
                pos("Xəzinədar",         "Xəzinə işləri",                 new BigDecimal("1500.00")),
                pos("HR Meneceri",       "İnsan resursları meneceri",      new BigDecimal("2500.00")),
                pos("Satış Meneceri",    "Satış şöbəsi meneceri",          new BigDecimal("2200.00")),
                pos("Koordinator",       "Əməliyyat koordinatoru",         new BigDecimal("2000.00")),
                pos("Mühəndis",          "Texniki mühəndis",               new BigDecimal("2500.00")),
                pos("Operator",          "Texnika operatoru",              new BigDecimal("1800.00")),
                pos("Sürücü",            "Avtomobil sürücüsü",             new BigDecimal("1000.00")),
                pos("Köməkçi işçi",      "Köməkçi heyət",                  new BigDecimal("700.00"))
        ));
        log.info("12 default vəzifə əlavə edildi.");
    }

    // ─── İşçilər ─────────────────────────────────────────────────────────────

    private void seedEmployees() {
        if (employeeRepo.count() > 0) return;

        Map<String, Position> pm = positionRepo.findAll().stream()
                .collect(Collectors.toMap(Position::getName, Function.identity(), (a, b) -> a));
        Map<String, Department> dm = departmentRepo.findAllByDeletedFalse().stream()
                .collect(Collectors.toMap(Department::getName, Function.identity(), (a, b) -> a));

        if (pm.isEmpty() || dm.isEmpty()) {
            log.warn("HR: vəzifə və ya şöbə tapılmadı, işçilər seed edilmir.");
            return;
        }

        log.info("HR: işçilər seed edilir...");

        record E(String first, String last, String father, String fin, Gender gender,
                 String born, String hired, String pos, String dept, String salary, String phone) {}

        List<E> data = List.of(
            new E("Fuad",   "Quliyev",    "Rauf",   "5AA1234", Gender.MALE,   "1978-03-15", "2015-01-05",
                  "Direktor",          "Rəhbərlik",             "9000.00", "+994501000010"),
            new E("Xədicə", "Babayeva",   "Əli",    "6BB2345", Gender.FEMALE, "1985-07-22", "2016-03-01",
                  "Baş Mühasib",       "Maliyyə şöbəsi",        "3500.00", "+994502000011"),
            new E("Nigar",  "Əhmədova",   "Fərid",  "7CC3456", Gender.FEMALE, "1990-11-08", "2017-06-15",
                  "Satış Meneceri",    "Satış şöbəsi",          "2200.00", "+994503000012"),
            new E("Bəhruz", "Hüseynov",   "Kamil",  "8DD4567", Gender.MALE,   "1988-04-30", "2018-02-10",
                  "Koordinator",       "Koordinasiya şöbəsi",   "2000.00", "+994504000013"),
            new E("Günay",  "Məmmədova",  "Tofiq",  "9EE5678", Gender.FEMALE, "1993-09-14", "2019-08-20",
                  "Mühasib",           "Maliyyə şöbəsi",        "2000.00", "+994505000014"),
            new E("Əli",    "Rəsulov",    "Əkbər",  "1FF6789", Gender.MALE,   "1982-12-01", "2016-09-01",
                  "Mühəndis",          "Texniki Xidmət şöbəsi", "2500.00", "+994506000015"),
            new E("Sevinc", "İsmayılova", "Vahid",  "2GG7890", Gender.FEMALE, "1995-05-25", "2020-04-01",
                  "HR Meneceri",       "Rəhbərlik",             "2500.00", "+994507000016"),
            new E("Tural",  "Abbasov",    "Murad",  "3HH8901", Gender.MALE,   "1991-08-18", "2019-01-15",
                  "Operator",          "Texniki Xidmət şöbəsi", "1800.00", "+994508000017"),
            new E("Rəşad",  "Nəcəfov",    "İlham",  "4II9012", Gender.MALE,   "1986-02-07", "2017-11-01",
                  "Koordinator",       "Koordinasiya şöbəsi",   "2000.00", "+994509000018"),
            new E("Aytən",  "Həsənova",   "Samir",  "5JJ0123", Gender.FEMALE, "1994-06-12", "2021-03-01",
                  "Mühasib",           "Maliyyə şöbəsi",        "2000.00", "+994501000019"),
            new E("Kamran", "Əliyev",     "Elnur",  "6KK1234", Gender.MALE,   "1989-10-03", "2018-07-15",
                  "Sürücü",            "Koordinasiya şöbəsi",   "1000.00", "+994502000020"),
            new E("Lalə",   "Qasımova",   "Rauf",   "7LL2345", Gender.FEMALE, "1997-01-28", "2022-05-01",
                  "Xəzinədar",         "Maliyyə şöbəsi",        "1500.00", "+994503000021")
        );

        int counter = 1;
        for (E e : data) {
            Employee emp = Employee.builder()
                    .employeeCode(String.format("EMP-2026-%04d", counter++))
                    .firstName(e.first())
                    .lastName(e.last())
                    .fatherName(e.father())
                    .fin(e.fin())
                    .gender(e.gender())
                    .birthDate(LocalDate.parse(e.born()))
                    .hireDate(LocalDate.parse(e.hired()))
                    .position(pm.get(e.pos()))
                    .department(dm.get(e.dept()))
                    .grossSalary(new BigDecimal(e.salary()))
                    .phone(e.phone())
                    .status(EmployeeStatus.ACTIVE)
                    .annualLeaveDays(21)
                    .build();
            employeeRepo.save(emp);
        }
        log.info("{} işçi əlavə edildi.", data.size());
    }

    // ─── HR modulunun mövcudluğunu təmin et ───────────────────────────────────
    // (Super Admin HR-ə ümumi icazə qrantları ilə çıxış əldə edir — ayrıca grant lazım deyil.)

    private void ensureHrModule() {
        if (!moduleRepo.existsByCode("HR_MANAGEMENT")) {
            moduleRepo.save(com.ces.erp.systemmodule.entity.SystemModule.builder()
                    .code("HR_MANAGEMENT")
                    .nameAz("İnsan Resursları Modulu")
                    .nameEn("HR Management")
                    .orderIndex(17)
                    .build());
            log.info("HR_MANAGEMENT modulu yaradıldı.");
        }
    }

    // ─── Köməkçilər ──────────────────────────────────────────────────────────

    private Position pos(String name, String desc, BigDecimal salary) {
        return Position.builder()
                .name(name)
                .description(desc)
                .defaultSalary(salary)
                .active(true)
                .build();
    }
}
