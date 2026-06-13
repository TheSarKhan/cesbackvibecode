package com.ces.erp.hr.service;

import com.ces.erp.enums.DeductionAppliesTo;
import com.ces.erp.enums.DeductionParty;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Generic tutulma hesablama motoru — JPA-dan asılı deyil (xalis, test oluna bilən).
 *
 * <p>Bir dilimə (bracket) düşən baza üçün düstur:
 * <pre>nəticə = sabit_mebleg + (baza − alt_hedd) × faiz − güzəşt</pre>
 *
 * <p>Sərhəd məntiqi: <b>alt_hedd &lt; baza ≤ ust_hedd</b> (ust_hedd null = sonsuz).
 * Mövcud hardcoded kodla birə-bir uyğundur: tam 2500 → 3% dilimi, tam 8000 → 10% dilimi.
 */
@Component
public class DeductionCalculator {

    private static final int SCALE = 2;
    private static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    /**
     * Bir maaş aralığı (dilim).
     *
     * <p>güzəşt (exemption) — şemada kolon YOXDUR (YAGNI, 2026 dəyərləri üçün lazımsız);
     * motor düsturu yenə də dəstəkləyir (default 0). Gələcəkdə kolon əlavə edilərsə bura ötürülür.
     */
    public record BracketDef(BigDecimal lower, BigDecimal upper, BigDecimal fixedAmount,
                             BigDecimal rate, BigDecimal exemption) {
        public BracketDef(BigDecimal lower, BigDecimal upper, BigDecimal fixedAmount, BigDecimal rate) {
            this(lower, upper, fixedAmount, rate, BigDecimal.ZERO);
        }
    }

    /** Bir tutulma növü + onun işçi/işəgötürən aralıqları. */
    public record DeductionDef(String code, String name, DeductionAppliesTo appliesTo, boolean deductedFromNet,
                               List<BracketDef> isciBrackets, List<BracketDef> isegoturenBrackets) {}

    /** Bir tutulma növü üzrə hesablanmış nəticə (xam, yuvarlaqlaşdırılmamış). */
    public record DeductionLine(String code, String name, boolean deductedFromNet,
                                BigDecimal employeeAmount, BigDecimal employerAmount) {}

    /** Tam hesablama nəticəsi. Bütün məbləğlər yuvarlaqlaşdırılmış (2 onluq). */
    public record Result(BigDecimal base, List<DeductionLine> lines,
                         BigDecimal totalEmployeeDeductions, BigDecimal totalEmployerContributions,
                         BigDecimal netPay) {

        /** Verilən kodun işçi məbləği (yoxdursa 0). */
        public BigDecimal employee(String code) {
            return lines.stream().filter(l -> l.code().equalsIgnoreCase(code))
                    .map(DeductionLine::employeeAmount).findFirst().orElse(BigDecimal.ZERO);
        }

        /** Verilən kodun işəgötürən məbləği (yoxdursa 0). */
        public BigDecimal employer(String code) {
            return lines.stream().filter(l -> l.code().equalsIgnoreCase(code))
                    .map(DeductionLine::employerAmount).findFirst().orElse(BigDecimal.ZERO);
        }
    }

    /**
     * Verilən baza üçün bütün tutulmaları hesablayır.
     *
     * @param base hesablanmış əməkhaqqı (vergi tutulan baza)
     * @param defs aktiv tutulma növləri və aralıqları
     */
    public Result compute(BigDecimal base, List<DeductionDef> defs) {
        BigDecimal baza = base == null || base.signum() < 0 ? BigDecimal.ZERO : base;
        List<DeductionLine> lines = new ArrayList<>();

        BigDecimal totalEmployee = BigDecimal.ZERO;  // net-dən çıxılan işçi tutulmaları
        BigDecimal totalEmployer = BigDecimal.ZERO;

        for (DeductionDef d : defs) {
            BigDecimal emp = BigDecimal.ZERO;
            BigDecimal employer = BigDecimal.ZERO;

            if (d.appliesTo() != null && d.appliesTo().allows(DeductionParty.ISCI)) {
                emp = evaluate(baza, d.isciBrackets());
            }
            if (d.appliesTo() != null && d.appliesTo().allows(DeductionParty.ISEGOTUREN)) {
                employer = evaluate(baza, d.isegoturenBrackets());
            }

            lines.add(new DeductionLine(d.code(), d.name(), d.deductedFromNet(), round(emp), round(employer)));

            if (d.deductedFromNet()) {
                totalEmployee = totalEmployee.add(emp);  // xam toplanır, sonda yuvarlaqlaşdırılır
            }
            totalEmployer = totalEmployer.add(employer);
        }

        BigDecimal net = baza.subtract(totalEmployee);
        if (net.signum() < 0) net = BigDecimal.ZERO;

        return new Result(round(baza), lines, round(totalEmployee), round(totalEmployer), round(net));
    }

    /**
     * Tək bir baza dəyəri üçün uyğun dilimi tapıb düsturu tətbiq edir.
     * Dilim seçimi: alt_hedd &lt; baza ≤ ust_hedd (ust null = sonsuz).
     */
    public BigDecimal evaluate(BigDecimal baza, List<BracketDef> brackets) {
        if (baza == null || baza.signum() <= 0 || brackets == null || brackets.isEmpty()) {
            return BigDecimal.ZERO;
        }
        for (BracketDef b : brackets) {
            BigDecimal lower = nz(b.lower());
            boolean lowerOk = baza.compareTo(lower) > 0;
            boolean upperOk = b.upper() == null || baza.compareTo(b.upper()) <= 0;
            if (lowerOk && upperOk) {
                BigDecimal res = nz(b.fixedAmount())
                        .add(baza.subtract(lower).multiply(nz(b.rate())))
                        .subtract(nz(b.exemption()));
                return res.signum() < 0 ? BigDecimal.ZERO : res;
            }
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal round(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(SCALE, ROUNDING);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
