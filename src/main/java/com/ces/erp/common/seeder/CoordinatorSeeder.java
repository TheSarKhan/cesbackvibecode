package com.ces.erp.common.seeder;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.operator.entity.Operator;
import com.ces.erp.operator.repository.OperatorRepository;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
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
@Order(8)
@RequiredArgsConstructor
@Slf4j
public class CoordinatorSeeder implements CommandLineRunner {

    private final TechRequestRepository requestRepository;
    private final CoordinatorPlanRepository planRepository;
    private final OperatorRepository operatorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() > 0) return;
        log.info("Koordinator planları seed edilir...");

        List<Operator> ops = operatorRepository.findAll();
        Map<String, Operator> opMap = ops.stream()
                .collect(Collectors.toMap(o -> o.getFirstName() + " " + o.getLastName(),
                        Function.identity(), (a, b) -> a));

        Operator opRauf   = opMap.get("Rauf Əliyev");
        Operator opNicat  = opMap.get("Nicat Həsənov");
        Operator opTural  = opMap.get("Tural Quliyev");
        Operator opElnur  = opMap.get("Elnur Məmmədov");
        Operator opKamran = opMap.get("Kamran İsmayılov");
        Operator opMurad  = opMap.get("Murad Rəhimov");
        Operator opSabuhi = opMap.get("Səbuhi Nəsirov");
        Operator opVusal  = opMap.get("Vüsal Babayev");

        // ACCEPTED və SENT_TO_COORDINATOR, OFFER_SENT statuslu sorğular
        List<RequestStatus> targetStatuses = List.of(
                RequestStatus.ACCEPTED,
                RequestStatus.SENT_TO_COORDINATOR,
                RequestStatus.OFFER_SENT
        );
        List<TechRequest> requests = requestRepository
                .findAllByStatusInAndDeletedFalse(targetStatuses);

        for (TechRequest r : requests) {
            String co = r.getCompanyName();
            String proj = r.getProjectName();

            switch (r.getStatus()) {

                // ── ACCEPTED — tamamlanmış və ya aktiv layihə planları ─────────

                case ACCEPTED -> {
                    CoordinatorPlan plan = buildAcceptedPlan(r, co, proj, opRauf, opNicat,
                            opTural, opElnur, opKamran, opMurad, opSabuhi, opVusal);
                    if (plan != null) planRepository.save(plan);
                }

                // ── SENT_TO_COORDINATOR — planlar hazırlanır ──────────────────

                case SENT_TO_COORDINATOR -> {
                    CoordinatorPlan plan = buildCoordPlan(r, co);
                    if (plan != null) planRepository.save(plan);
                }

                // ── OFFER_SENT — planlar göndərilmiş, cavab gözlənilir ────────

                case OFFER_SENT -> {
                    CoordinatorPlan plan = buildOfferPlan(r, co);
                    if (plan != null) planRepository.save(plan);
                }
            }
        }

        log.info("{} koordinator planı əlavə edildi.", planRepository.count());
    }

    // ── ACCEPTED planları ─────────────────────────────────────────────────────

    private CoordinatorPlan buildAcceptedPlan(TechRequest r, String co, String proj,
            Operator rauf, Operator nicat, Operator tural, Operator elnur,
            Operator kamran, Operator murad, Operator sabuhi, Operator vusal) {

        // R-001: Azər İnşaat — Binəqədi torpaq qazıma (yanvar 5-20)
        if (co.contains("Azər İnşaat") && proj.contains("Binəqədi")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(rauf)
                    .dayCount(15)
                    .equipmentPrice(new BigDecimal("3000.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("750.00"))
                    .transportationPrice(BigDecimal.ZERO)
                    .startDate(LocalDate.of(2026, 1, 5))
                    .endDate(LocalDate.of(2026, 1, 20))
                    .notes("200 AZN/gün. Operator Rauf Əliyev. Uğurla tamamlandı.")
                    .build();
        }

        // R-002: Grand Build — Sabunçu bina kran (yanvar 10-22)
        if (co.contains("Grand Build") && proj.contains("Sabunçu")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(nicat)
                    .dayCount(12)
                    .equipmentPrice(new BigDecimal("4800.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("720.00"))
                    .transportationPrice(new BigDecimal("600.00"))
                    .startDate(LocalDate.of(2026, 1, 10))
                    .endDate(LocalDate.of(2026, 1, 22))
                    .notes("400 AZN/gün kran + daşınma. Nicat Həsənov. Uğurla tamamlandı.")
                    .build();
        }

        // R-003: SOCAR — Balaxanı buldozer (yanvar 16 - fevral 3)
        if (co.contains("SOCAR") && proj.contains("Balaxanı")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(kamran)
                    .dayCount(18)
                    .equipmentPrice(new BigDecimal("3240.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("900.00"))
                    .transportationPrice(new BigDecimal("800.00"))
                    .startDate(LocalDate.of(2026, 1, 16))
                    .endDate(LocalDate.of(2026, 2, 3))
                    .notes("180 AZN/gün + operator + daşınma. ATEX sahəsi. Tamamlandı.")
                    .build();
        }

        // R-004: SkyLine — Sumqayıt çimərlik (yanvar 22 - fevral 1)
        if (co.contains("SkyLine") && proj.contains("çimərlik")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(vusal)
                    .dayCount(10)
                    .equipmentPrice(new BigDecimal("1600.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("400.00"))
                    .transportationPrice(BigDecimal.ZERO)
                    .startDate(LocalDate.of(2026, 1, 22))
                    .endDate(LocalDate.of(2026, 2, 1))
                    .notes("Podratçı texnikası, JCB 3CX. Vüsal Babayev. Tamamlandı.")
                    .build();
        }

        // R-005: AzərGold — Daşkəsən teleskopik (fevral 4-18)
        if (co.contains("AzərGold") && proj.contains("yüklənmə")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(sabuhi)
                    .dayCount(14)
                    .equipmentPrice(new BigDecimal("2800.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("700.00"))
                    .transportationPrice(new BigDecimal("900.00"))
                    .startDate(LocalDate.of(2026, 2, 4))
                    .endDate(LocalDate.of(2026, 2, 18))
                    .notes("İnvestor texnikası. Daşkəsən-Gəncə daşınma. Tamamlandı.")
                    .build();
        }

        // R-006: Bakı Metro — Kran aylıq (fevral 15 - mart 15)
        if (co.contains("Bakı Metro") && proj.contains("8 Noyabr")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(nicat)
                    .dayCount(28)
                    .equipmentPrice(new BigDecimal("11200.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("2800.00"))
                    .transportationPrice(new BigDecimal("1200.00"))
                    .startDate(LocalDate.of(2026, 2, 15))
                    .endDate(LocalDate.of(2026, 3, 15))
                    .notes("400 AZN/gün. Gecə-gündüz 2 növbə. Nicat Həsənov. Aktiv.")
                    .build();
        }

        // R-007: Kəpəz — Gəncə-Samux yol buldozeri (fevral 24 - mart 26)
        if (co.contains("Kəpəz") && proj.contains("magistral")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(tural)
                    .dayCount(30)
                    .equipmentPrice(new BigDecimal("5400.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("1500.00"))
                    .transportationPrice(new BigDecimal("500.00"))
                    .startDate(LocalDate.of(2026, 2, 24))
                    .endDate(LocalDate.of(2026, 3, 26))
                    .notes("180 AZN/gün. Tural Quliyev. Yol bərpası, Gəncə. Aktiv.")
                    .build();
        }

        // R-008: Azər İnşaat — Maştağa ekskavator (mart 5-25)
        if (co.contains("Azər İnşaat") && proj.contains("Maştağa")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(rauf)
                    .dayCount(20)
                    .equipmentPrice(new BigDecimal("4000.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("1000.00"))
                    .transportationPrice(BigDecimal.ZERO)
                    .startDate(LocalDate.of(2026, 3, 5))
                    .endDate(LocalDate.of(2026, 3, 25))
                    .notes("200 AZN/gün. Rauf Əliyev. Yaşayış kompleksi. Aktiv.")
                    .build();
        }

        // R-009: Grand Build — Xəzər mini ekskavator (mart 15 - aprel 9)
        if (co.contains("Grand Build") && proj.contains("villa")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .operator(elnur)
                    .dayCount(25)
                    .equipmentPrice(new BigDecimal("3250.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("1000.00"))
                    .transportationPrice(BigDecimal.ZERO)
                    .startDate(LocalDate.of(2026, 3, 15))
                    .endDate(LocalDate.of(2026, 4, 9))
                    .notes("130 AZN/gün. Elnur Məmmədov. Müqavilə gözlənilir.")
                    .build();
        }

        return null;
    }

    // ── SENT_TO_COORDINATOR planları — hazırlanır ─────────────────────────────

    private CoordinatorPlan buildCoordPlan(TechRequest r, String co) {

        // R-010: SOCAR — Lökbatan yük maşını
        if (co.contains("SOCAR") && r.getProjectName().contains("Lökbatan")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .dayCount(22)
                    .equipmentPrice(new BigDecimal("3960.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("880.00"))
                    .transportationPrice(new BigDecimal("700.00"))
                    .startDate(LocalDate.of(2026, 3, 25))
                    .endDate(LocalDate.of(2026, 4, 16))
                    .notes("Plan hazır, sənədlər tamamlanır. SOCAR icazəsi gözlənilir.")
                    .build();
        }

        // R-011: AzərGold — Daşkəsən asfalt silindir
        if (co.contains("AzərGold") && r.getProjectName().contains("bərpa")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .dayCount(10)
                    .equipmentPrice(new BigDecimal("1500.00"))
                    .contractorPayment(new BigDecimal("900.00"))
                    .operatorPayment(BigDecimal.ZERO)
                    .transportationPrice(new BigDecimal("600.00"))
                    .startDate(LocalDate.of(2026, 3, 22))
                    .endDate(LocalDate.of(2026, 4, 1))
                    .notes("Podratçı texnikası (EQ-011). Operator podratçıdan. Plan yoxlanılır.")
                    .build();
        }

        // R-012: SkyLine — Sumqayıt sənaye parkı greyder
        if (co.contains("SkyLine") && r.getProjectName().contains("sənaye parkı")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .dayCount(18)
                    .equipmentPrice(new BigDecimal("3240.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("900.00"))
                    .transportationPrice(new BigDecimal("500.00"))
                    .startDate(LocalDate.of(2026, 4, 1))
                    .endDate(LocalDate.of(2026, 4, 19))
                    .notes("Plan hazırlanır. Greyder EQ-007 aprel 1-dən boş olacaq.")
                    .build();
        }

        return null;
    }

    // ── OFFER_SENT planları — göndərilmiş, cavab gözlənilir ──────────────────

    private CoordinatorPlan buildOfferPlan(TechRequest r, String co) {

        // R-013: Bakı Metro — Dərnəgül beton mikser
        if (co.contains("Bakı Metro") && r.getProjectName().contains("Dərnəgül")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .dayCount(20)
                    .equipmentPrice(new BigDecimal("3000.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("800.00"))
                    .transportationPrice(BigDecimal.ZERO)
                    .startDate(LocalDate.of(2026, 3, 28))
                    .endDate(LocalDate.of(2026, 4, 17))
                    .notes("Teklif 150 AZN/gün + operator ilə göndərildi. Cavab gözlənilir.")
                    .build();
        }

        // R-014: Caspian Roads — M5 yolu greyder
        if (co.contains("Caspian") && r.getProjectName().contains("M5")) {
            return CoordinatorPlan.builder()
                    .request(r)
                    .selectedEquipment(r.getSelectedEquipment())
                    .dayCount(12)
                    .equipmentPrice(new BigDecimal("2160.00"))
                    .contractorPayment(BigDecimal.ZERO)
                    .operatorPayment(new BigDecimal("600.00"))
                    .transportationPrice(new BigDecimal("400.00"))
                    .startDate(LocalDate.of(2026, 3, 20))
                    .endDate(LocalDate.of(2026, 4, 1))
                    .notes("180 AZN/gün + daşınma. Teklif göndərildi. Müştəri cavabı gözlənilir.")
                    .build();
        }

        return null;
    }
}
