package com.ces.erp.common.seeder;

import com.ces.erp.coordinator.entity.CoordinatorPlan;
import com.ces.erp.coordinator.repository.CoordinatorPlanRepository;
import com.ces.erp.enums.RequestStatus;
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

@Component
@Order(6)
@RequiredArgsConstructor
@Slf4j
public class CoordinatorSeeder implements CommandLineRunner {

    private final TechRequestRepository requestRepository;
    private final CoordinatorPlanRepository planRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (planRepository.count() > 0) return;
        log.info("Koordinator planları seed edilir...");

        List<RequestStatus> statuses = List.of(
                RequestStatus.SENT_TO_COORDINATOR,
                RequestStatus.OFFER_SENT,
                RequestStatus.ACCEPTED,
                RequestStatus.REJECTED
        );

        List<TechRequest> requests = requestRepository.findAllByStatusInAndDeletedFalse(statuses);

        for (TechRequest r : requests) {
            switch (r.getStatus()) {

                case SENT_TO_COORDINATOR -> {
                    // Koordinator planı yarıda — məlumatlar daxil edilib, hələ göndərilməyib
                    CoordinatorPlan plan = CoordinatorPlan.builder()
                            .request(r)
                            .equipmentPrice(new BigDecimal("2100.00"))   // 14 gün × 150 AZN/gün
                            .contractorPayment(new BigDecimal("0.00"))   // şirkətin öz texnikası
                            .transportationPrice(new BigDecimal("1200.00"))
                            .startDate(LocalDate.of(2026, 3, 10))
                            .endDate(LocalDate.of(2026, 3, 24))
                            .notes("Plan hazırlanır, sənədlər gözlənilir")
                            .build();
                    planRepository.save(plan);
                    log.info("SENT_TO_COORDINATOR planı yaradıldı: {}", r.getCompanyName());
                }

                case ACCEPTED -> {
                    // Tam tamamlanmış plan — layihəyə keçirilmiş
                    CoordinatorPlan plan = CoordinatorPlan.builder()
                            .request(r)
                            .equipmentPrice(new BigDecimal("1050.00"))   // 7 gün × 150 AZN/gün
                            .contractorPayment(new BigDecimal("0.00"))
                            .transportationPrice(new BigDecimal("0.00"))
                            .startDate(LocalDate.of(2026, 2, 17))
                            .endDate(LocalDate.of(2026, 2, 24))
                            .notes("Layihə uğurla tamamlandı")
                            .build();
                    planRepository.save(plan);
                    log.info("ACCEPTED planı yaradıldı: {}", r.getCompanyName());
                }

                default -> {
                    // OFFER_SENT, REJECTED — plan tələb olunmur
                }
            }
        }

        log.info("Koordinator seederi tamamlandı.");
    }
}
