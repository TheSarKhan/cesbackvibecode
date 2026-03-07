package com.ces.erp.common.seeder;

import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.RequestStatus;
import com.ces.erp.garage.entity.Equipment;
import com.ces.erp.garage.repository.EquipmentRepository;
import com.ces.erp.request.entity.TechParam;
import com.ces.erp.request.entity.TechRequest;
import com.ces.erp.request.repository.TechRequestRepository;
import com.ces.erp.user.entity.User;
import com.ces.erp.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ces.erp.enums.ProjectType;
import java.time.LocalDate;
import java.util.List;

@Component
@Order(5)
@RequiredArgsConstructor
@Slf4j
public class RequestSeeder implements CommandLineRunner {

    private final TechRequestRepository requestRepository;
    private final CustomerRepository customerRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (requestRepository.count() > 0) return;
        log.info("Sorğu seed edilir...");

        User admin = userRepository.findByEmailAndDeletedFalse("admin@ces.az").orElse(null);

        List<Customer> customers = customerRepository.findAllByDeletedFalse();
        List<Equipment> equipment = equipmentRepository.findAllByDeletedFalse();

        Customer c1 = customers.stream().filter(c -> c.getCompanyName().contains("Azər İnşaat")).findFirst().orElse(customers.isEmpty() ? null : customers.get(0));
        Customer c2 = customers.stream().filter(c -> c.getCompanyName().contains("Grand Build")).findFirst().orElse(customers.size() > 1 ? customers.get(1) : null);
        Customer c3 = customers.stream().filter(c -> c.getCompanyName().contains("SkyLine")).findFirst().orElse(customers.size() > 2 ? customers.get(2) : null);

        Equipment eq1 = equipment.stream().filter(e -> e.getEquipmentCode().equals("EQ-001")).findFirst().orElse(equipment.isEmpty() ? null : equipment.get(0));
        Equipment eq2 = equipment.stream().filter(e -> e.getEquipmentCode().equals("EQ-003")).findFirst().orElse(equipment.size() > 1 ? equipment.get(1) : null);

        List<TechRequest> requests = List.of(

                // 1. DRAFT — Yeni yaradılmış
                TechRequest.builder()
                        .status(RequestStatus.DRAFT)
                        .customer(c1)
                        .companyName(c1 != null ? c1.getCompanyName() : "Azər İnşaat MMC")
                        .contactPerson(c1 != null ? c1.getOfficeContactPerson() : "Leyla Hüseynova")
                        .contactPhone(c1 != null ? c1.getOfficeContactPhone() : "+994702345678")
                        .projectName("Binəqədi yol tikintisi")
                        .region("Bakı, Binəqədi")
                        .requestDate(LocalDate.of(2026, 3, 5))
                        .projectType(ProjectType.DAILY)
                        .dayCount(30)
                        .transportationRequired(false)
                        .params(List.of(
                                new TechParam("Texnika növü", "Ekskavator"),
                                new TechParam("Minimum yük gücü", "20 ton"),
                                new TechParam("İş müddəti", "30 gün")
                        ))
                        .createdBy(admin)
                        .notes("Torpaq qazıma işləri üçün ekskavator lazımdır")
                        .build(),

                // 2. PENDING — Alım-Satım komandasında
                TechRequest.builder()
                        .status(RequestStatus.PENDING)
                        .customer(c2)
                        .companyName(c2 != null ? c2.getCompanyName() : "Grand Build ASC")
                        .contactPerson(c2 != null ? c2.getOfficeContactPerson() : "Nərmin Qasımova")
                        .contactPhone(c2 != null ? c2.getOfficeContactPhone() : "+994504567890")
                        .projectName("Mehdiabad yarımstansiyası")
                        .region("Bakı, Sabunçu")
                        .requestDate(LocalDate.of(2026, 3, 1))
                        .projectType(ProjectType.MONTHLY)
                        .dayCount(1)
                        .transportationRequired(true)
                        .params(List.of(
                                new TechParam("Texnika növü", "Kran"),
                                new TechParam("Qaldırma gücü", "Min 50 ton"),
                                new TechParam("Bom uzunluğu", "Min 30 metr"),
                                new TechParam("İş müddəti", "14 gün")
                        ))
                        .createdBy(admin)
                        .notes("Ağır avadanlıq qaldırma üçün mobil kran tələb olunur")
                        .build(),

                // 3. PENDING — Texnika seçilib, kordinatora göndərilməyi gözləyir
                TechRequest.builder()
                        .status(RequestStatus.PENDING)
                        .customer(c3)
                        .companyName(c3 != null ? c3.getCompanyName() : "SkyLine Tikinti QSC")
                        .contactPerson(c3 != null ? c3.getOfficeContactPerson() : "Aynur Babayeva")
                        .contactPhone(c3 != null ? c3.getOfficeContactPhone() : "+994506789012")
                        .projectName("Sumqayıt çimərlik kompleksi")
                        .region("Sumqayıt, şimal sahili")
                        .requestDate(LocalDate.of(2026, 2, 25))
                        .projectType(ProjectType.DAILY)
                        .dayCount(20)
                        .transportationRequired(false)
                        .params(List.of(
                                new TechParam("Texnika növü", "Hidravlik Ekskavator"),
                                new TechParam("Çəkisi", "20-30 ton"),
                                new TechParam("Kovş həcmi", "Min 1.2 m³")
                        ))
                        .selectedEquipment(eq1)
                        .createdBy(admin)
                        .notes("")
                        .build(),

                // 4. SENT_TO_COORDINATOR — Kordinatorda
                TechRequest.builder()
                        .status(RequestStatus.SENT_TO_COORDINATOR)
                        .companyName("Caspian Road Builders LLC")
                        .contactPerson("Həsən Rəcəbov")
                        .contactPhone("+994551234567")
                        .projectName("M1 magistral yolu bərpası")
                        .region("Abşeron, Balaxanı")
                        .requestDate(LocalDate.of(2026, 2, 18))
                        .projectType(ProjectType.DAILY)
                        .dayCount(14)
                        .transportationRequired(true)
                        .params(List.of(
                                new TechParam("Texnika növü", "Yük Kranı"),
                                new TechParam("Qaldırma gücü", "70 ton"),
                                new TechParam("İş saatları", "Gündüz shift, 8-18")
                        ))
                        .selectedEquipment(eq2)
                        .createdBy(admin)
                        .notes("Körpü bərpa işləri üçün ağır kran")
                        .build(),

                // 5. ACCEPTED — Qəbul edilmiş
                TechRequest.builder()
                        .status(RequestStatus.ACCEPTED)
                        .companyName("NeftQaz Servis ASC")
                        .contactPerson("Elnur Babaxanov")
                        .contactPhone("+994705559900")
                        .projectName("Pirəkəşkül anbarlıq sahəsi")
                        .region("Abşeron, Pirəkəşkül")
                        .requestDate(LocalDate.of(2026, 2, 10))
                        .projectType(ProjectType.DAILY)
                        .dayCount(7)
                        .transportationRequired(false)
                        .params(List.of(
                                new TechParam("Texnika növü", "Buldozer"),
                                new TechParam("İş müddəti", "7 gün"),
                                new TechParam("Torpaq növü", "Gil-qum qarışığı")
                        ))
                        .createdBy(admin)
                        .notes("Qəbul edilib, layihəyə keçirildi")
                        .build(),

                // 6. REJECTED — Rədd edilmiş
                TechRequest.builder()
                        .status(RequestStatus.REJECTED)
                        .companyName("Flex Construction Ltd")
                        .contactPerson("Orxan Süleymanov")
                        .contactPhone("+994502223344")
                        .projectName("Novxanı anbar kompleksi")
                        .region("Novxanı")
                        .requestDate(LocalDate.of(2026, 1, 28))
                        .projectType(ProjectType.MONTHLY)
                        .dayCount(2)
                        .transportationRequired(true)
                        .params(List.of(
                                new TechParam("Texnika növü", "Kompaktor"),
                                new TechParam("Çəki", "Min 15 ton")
                        ))
                        .createdBy(admin)
                        .notes("Texniki tələblər qarşılanmadı")
                        .build()
        );

        requestRepository.saveAll(requests);
        log.info("{} sorğu əlavə edildi.", requests.size());
    }
}
