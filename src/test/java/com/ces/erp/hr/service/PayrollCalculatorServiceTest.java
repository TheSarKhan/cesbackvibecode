package com.ces.erp.hr.service;

import com.ces.erp.enums.DeductionAppliesTo;
import com.ces.erp.hr.entity.PayrollEntry;
import com.ces.erp.hr.service.DeductionCalculator.BracketDef;
import com.ces.erp.hr.service.DeductionCalculator.DeductionDef;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 2026 (qeyri-neft-qaz / özəl sektor) əməkhaqqı hesablamasının doğrulanması.
 *
 * <p>Test konfiqurasiyası {@code V10__hr_deduction_config.sql} seed-i ilə birə-bir eynidir.
 * Hesab motoru ({@link DeductionCalculator}) Spring kontekstinə ehtiyac duymur.
 */
class PayrollCalculatorServiceTest {

    private final PayrollCalculatorService service = new PayrollCalculatorService(new DeductionCalculator());

    private static BracketDef br(String lower, String upper, String fixed, String rate) {
        return new BracketDef(new BigDecimal(lower), upper == null ? null : new BigDecimal(upper),
                new BigDecimal(fixed), new BigDecimal(rate));
    }

    /** Seed (2026) ilə eyni konfiqurasiya. */
    private static ResolvedDeductionConfig config2026() {
        DeductionDef incomeTax = new DeductionDef("GELIR_VERGISI", "Gəlir vergisi",
                DeductionAppliesTo.ISCI, true,
                List.of(br("0", "200", "0", "0"),
                        br("200", "2500", "0", "0.03"),
                        br("2500", "8000", "75", "0.10"),
                        br("8000", null, "625", "0.14")),
                List.of());

        DeductionDef dsmf = new DeductionDef("DSMF", "DSMF / Pensiya Fondu",
                DeductionAppliesTo.HER_IKISI, true,
                List.of(br("0", "200", "0", "0.03"),
                        br("200", null, "6", "0.10")),
                List.of(br("0", "200", "0", "0.22"),
                        br("200", "8000", "44", "0.15"),
                        br("8000", null, "1214", "0.11")));

        DeductionDef ish = new DeductionDef("ISH", "İşsizlikdən sığorta",
                DeductionAppliesTo.HER_IKISI, true,
                List.of(br("0", null, "0", "0.005")),
                List.of(br("0", null, "0", "0.005")));

        DeductionDef its = new DeductionDef("ITS", "İcbari tibbi sığorta",
                DeductionAppliesTo.HER_IKISI, true,
                List.of(br("0", "2500", "0", "0.02"),
                        br("2500", null, "50", "0.005")),
                List.of(br("0", "2500", "0", "0.02"),
                        br("2500", null, "50", "0.005")));

        return new ResolvedDeductionConfig(1L, 1, LocalDate.of(2026, 1, 1),
                List.of(incomeTax, dsmf, ish, its));
    }

    private static PayrollEntry entryWithBase(String base) {
        return PayrollEntry.builder()
                .employeeFullName("Test")
                .baseSalary(new BigDecimal(base))
                .workingDaysInMonth(22)
                .actualDaysWorked(22)   // proration yox → gross = baseSalary
                .build();
    }

    /** baza | gelirV | dsmfİşçi | İSHişçi | İTSişçi | dsmfİşəg | İSHişəg | İTSişəg | net */
    static Stream<Arguments> scenarios() {
        return Stream.of(
                // ── Mənbə cədvəlin 5 ssenarisi ──
                Arguments.of("9000", "765", "886", "45",   "82.5", "1324", "45",   "82.5", "7221.5"),
                Arguments.of("7500", "575", "736", "37.5", "75",   "1139", "37.5", "75",   "6076.5"),
                Arguments.of("2000", "54",  "186", "10",   "40",   "314",  "10",   "40",   "1710"),
                Arguments.of("1000", "24",  "86",  "5",    "20",   "164",  "5",    "20",   "865"),
                Arguments.of("500",  "9",   "36",  "2.5",  "10",   "89",   "2.5",  "10",   "442.5"),
                // ── Sərhəd dəyərləri (tam 2500 və tam 8000) ──
                Arguments.of("2500", "69",  "236", "12.5", "50",   "389",  "12.5", "50",   "2132.5"),
                Arguments.of("8000", "625", "786", "40",   "77.5", "1214", "40",   "77.5", "6471.5")
        );
    }

    @ParameterizedTest(name = "baza={0} → net={8}")
    @MethodSource("scenarios")
    @DisplayName("2026 əməkhaqqı tutulmaları düzgün hesablanır")
    void calculatesDeductions(String base, String incomeTax, String empPension, String empUnemployment,
                              String empMedical, String erPension, String erUnemployment, String erMedical,
                              String net) {
        PayrollEntry e = entryWithBase(base);

        service.recalculate(e, config2026());

        assertThat(e.getGrossTotal()).isEqualByComparingTo(base);
        assertThat(e.getIncomeTax()).isEqualByComparingTo(incomeTax);
        assertThat(e.getEmployeePension()).isEqualByComparingTo(empPension);
        assertThat(e.getEmployeeUnemployment()).isEqualByComparingTo(empUnemployment);
        assertThat(e.getEmployeeMedical()).isEqualByComparingTo(empMedical);
        assertThat(e.getEmployerPension()).isEqualByComparingTo(erPension);
        assertThat(e.getEmployerUnemployment()).isEqualByComparingTo(erUnemployment);
        assertThat(e.getEmployerMedical()).isEqualByComparingTo(erMedical);
        assertThat(e.getNetPay()).isEqualByComparingTo(net);

        // totalDeductions = işçi tutulmaları cəmi; net = gross − totalDeductions
        BigDecimal expectedTotalDed = new BigDecimal(incomeTax).add(new BigDecimal(empPension))
                .add(new BigDecimal(empUnemployment)).add(new BigDecimal(empMedical));
        assertThat(e.getTotalDeductions()).isEqualByComparingTo(expectedTotalDed);
        assertThat(e.getGrossTotal().subtract(e.getTotalDeductions())).isEqualByComparingTo(net);

        // şirkət xərci = gross + işəgötürən töhfələri
        BigDecimal expectedEmployer = new BigDecimal(erPension).add(new BigDecimal(erUnemployment))
                .add(new BigDecimal(erMedical));
        assertThat(e.getTotalEmployerContributions()).isEqualByComparingTo(expectedEmployer);
        assertThat(e.getTotalCompanyCost()).isEqualByComparingTo(new BigDecimal(base).add(expectedEmployer));
    }
}
