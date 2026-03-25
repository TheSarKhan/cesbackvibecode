package com.ces.erp.common.seeder;

import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.ProjectType;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Order(7)
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
        log.info("Sorğular seed edilir...");

        User admin = userRepository.findByEmailAndDeletedFalse("admin@ces.az").orElse(null);
        User nigar = userRepository.findByEmailAndDeletedFalse("nigar@ces.az").orElse(admin);

        // Müştərilər
        Map<String, Customer> cust = customerRepository.findAllByDeletedFalse()
                .stream().collect(Collectors.toMap(Customer::getCompanyName, Function.identity()));
        Customer cAzer   = cust.get("Azər İnşaat MMC");
        Customer cGrand  = cust.get("Grand Build ASC");
        Customer cSky    = cust.get("SkyLine Tikinti QSC");
        Customer cKepez  = cust.get("Kəpəz Yol Tikintisi MMC");
        Customer cSocar  = cust.get("SOCAR Tikinti ASC");
        Customer cGold   = cust.get("AzərGold QSC");
        Customer cMetro  = cust.get("Bakı Metro MMC");
        Customer cDelta  = cust.get("Delta İnşaat Ltd");

        // Texnikalar
        Map<String, Equipment> eq = equipmentRepository.findAllByDeletedFalse()
                .stream().collect(Collectors.toMap(Equipment::getEquipmentCode, Function.identity()));
        Equipment eq1  = eq.get("EQ-001"); // Hidravlik Ekskavator
        Equipment eq2  = eq.get("EQ-002"); // Buldozer
        Equipment eq3  = eq.get("EQ-003"); // Mobil Yük Kranı
        Equipment eq4  = eq.get("EQ-004"); // Beton Mikser
        Equipment eq6  = eq.get("EQ-006"); // Yük Maşını
        Equipment eq7  = eq.get("EQ-007"); // Greyder
        Equipment eq9  = eq.get("EQ-009"); // Mini Ekskavator
        Equipment eq10 = eq.get("EQ-010"); // Teleskopik Yükləyici
        Equipment eq11 = eq.get("EQ-011"); // Asfalt Silindir
        Equipment eq12 = eq.get("EQ-012"); // Ekskavator-Yükləyici

        // ═══════════════════════════════════════════════════════════════════════
        //  YANVAR 2026 — tamamlanmış layihələrə çevrilmiş sorğular (ACCEPTED)
        // ═══════════════════════════════════════════════════════════════════════

        // R-001: Azər İnşaat — Ekskavator kirayəsi (15 gün) → COMPLETED project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cAzer)
                .companyName("Azər İnşaat MMC")
                .contactPerson("Leyla Hüseynova")
                .contactPhone("+994702345678")
                .projectName("Binəqədi torpaq qazıma işləri")
                .region("Bakı, Binəqədi")
                .requestDate(LocalDate.of(2026, 1, 3))
                .projectType(ProjectType.DAILY)
                .dayCount(15)
                .transportationRequired(false)
                .selectedEquipment(eq1)
                .params(List.of(
                        new TechParam("Texnika növü", "Hidravlik Ekskavator"),
                        new TechParam("Çəki (ton)", "20-22"),
                        new TechParam("İş müddəti (gün)", "15"),
                        new TechParam("Torpaq növü", "Gil-qum qarışığı")
                ))
                .createdBy(nigar)
                .notes("Qapalı məkanda qazıma. Dar sahə. Texnika kiçik olmalıdır.")
                .build());

        // R-002: Grand Build — Kran kirayəsi (12 gün) → COMPLETED project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cGrand)
                .companyName("Grand Build ASC")
                .contactPerson("Nərmin Qasımova")
                .contactPhone("+994504567890")
                .projectName("Sabunçu rayon bina tikintisi")
                .region("Bakı, Sabunçu")
                .requestDate(LocalDate.of(2026, 1, 7))
                .projectType(ProjectType.DAILY)
                .dayCount(12)
                .transportationRequired(true)
                .selectedEquipment(eq3)
                .params(List.of(
                        new TechParam("Texnika növü", "Mobil Kran"),
                        new TechParam("Yük qaldırma (ton)", "Min 50"),
                        new TechParam("Boom uzunluğu (m)", "Min 35"),
                        new TechParam("İş müddəti (gün)", "12")
                ))
                .createdBy(nigar)
                .notes("Prefabrik beton panel qaldırma. Operator tələb olunur.")
                .build());

        // R-003: SOCAR — Buldozer kirayəsi (18 gün) → COMPLETED project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cSocar)
                .companyName("SOCAR Tikinti ASC")
                .contactPerson("Şəhriyar Əhmədov")
                .contactPhone("+994703334455")
                .projectName("Balaxanı emal sahəsi torpaq işləri")
                .region("Bakı, Balaxanı")
                .requestDate(LocalDate.of(2026, 1, 14))
                .projectType(ProjectType.DAILY)
                .dayCount(18)
                .transportationRequired(true)
                .selectedEquipment(eq2)
                .params(List.of(
                        new TechParam("Texnika növü", "Buldozer"),
                        new TechParam("Güc (HP)", "Min 230"),
                        new TechParam("İş müddəti (gün)", "18"),
                        new TechParam("İş saatları", "07:00 - 19:00")
                ))
                .createdBy(admin)
                .notes("ATEX zolağında iş. Yanğın təhlükəsizliyi tələbləri vacibdir.")
                .build());

        // R-004: SkyLine — Ekskavator-Yükləyici (10 gün) → COMPLETED project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cSky)
                .companyName("SkyLine Tikinti QSC")
                .contactPerson("Aynur Babayeva")
                .contactPhone("+994506789012")
                .projectName("Sumqayıt çimərlik kompleksi — bünövrə")
                .region("Sumqayıt, şimal sahili")
                .requestDate(LocalDate.of(2026, 1, 20))
                .projectType(ProjectType.DAILY)
                .dayCount(10)
                .transportationRequired(false)
                .selectedEquipment(eq12)
                .params(List.of(
                        new TechParam("Texnika növü", "Ekskavator-Yükləyici"),
                        new TechParam("Kovş həcmi (m³)", "Min 0.8"),
                        new TechParam("İş müddəti (gün)", "10")
                ))
                .createdBy(nigar)
                .notes("Sahil boyu bünövrə qazıma. Duzlu torpaq şəraiti.")
                .build());

        // R-005: AzərGold — Teleskopik Yükləyici (14 gün) → COMPLETED project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cGold)
                .companyName("AzərGold QSC")
                .contactPerson("Vüsal Məmmədov")
                .contactPhone("+994704445566")
                .projectName("Daşkəsən mədən sahəsi yüklənmə")
                .region("Gəncə")
                .requestDate(LocalDate.of(2026, 2, 1))
                .projectType(ProjectType.DAILY)
                .dayCount(14)
                .transportationRequired(true)
                .selectedEquipment(eq10)
                .params(List.of(
                        new TechParam("Texnika növü", "Teleskopik Yükləyici"),
                        new TechParam("Yük qaldırma (ton)", "Min 5"),
                        new TechParam("Hündürlük (m)", "Min 14"),
                        new TechParam("İş müddəti (gün)", "14")
                ))
                .createdBy(nigar)
                .notes("Mədən sahəsi — engebeli arazi. Operatorla birlikdə.")
                .build());

        // ═══════════════════════════════════════════════════════════════════════
        //  FEVRAL 2026 — aktiv layihələrə çevrilmiş sorğular (ACCEPTED)
        // ═══════════════════════════════════════════════════════════════════════

        // R-006: Bakı Metro — Kran (aylıq) → ACTIVE project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cMetro)
                .companyName("Bakı Metro MMC")
                .contactPerson("Lalə Mustafayeva")
                .contactPhone("+994705556677")
                .projectName("8 Noyabr stansiyası yerüstü konstruksiya")
                .region("Bakı, Xətai")
                .requestDate(LocalDate.of(2026, 2, 8))
                .projectType(ProjectType.MONTHLY)
                .dayCount(1)
                .transportationRequired(true)
                .selectedEquipment(eq3)
                .params(List.of(
                        new TechParam("Texnika növü", "Mobil Kran"),
                        new TechParam("Yük qaldırma (ton)", "70+"),
                        new TechParam("Boom uzunluğu (m)", "Min 40"),
                        new TechParam("İş saatları", "06:00 - 22:00")
                ))
                .createdBy(nigar)
                .notes("Metro tikintisi. Gündüz və gecə növbə. Sürətli mobilizasiya tələb olunur.")
                .build());

        // R-007: Kəpəz — Buldozer (30 gün) → ACTIVE project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cKepez)
                .companyName("Kəpəz Yol Tikintisi MMC")
                .contactPerson("Günel Rəhimova")
                .contactPhone("+994508901234")
                .projectName("Gəncə-Samux magistral yol bərpası")
                .region("Gəncə")
                .requestDate(LocalDate.of(2026, 2, 15))
                .projectType(ProjectType.DAILY)
                .dayCount(30)
                .transportationRequired(false)
                .selectedEquipment(eq2)
                .params(List.of(
                        new TechParam("Texnika növü", "Buldozer"),
                        new TechParam("Güc (HP)", "Min 250"),
                        new TechParam("İş müddəti (gün)", "30"),
                        new TechParam("Torpaq növü", "Bərk-çınqıllı")
                ))
                .createdBy(nigar)
                .notes("Yol hamarlanması + bəndin gücləndirilməsi. Operator Tural Quliyev.")
                .build());

        // R-008: Azər İnşaat — Ekskavator (20 gün) → ACTIVE project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cAzer)
                .companyName("Azər İnşaat MMC")
                .contactPerson("Leyla Hüseynova")
                .contactPhone("+994702345678")
                .projectName("Maştağa yaşayış kompleksi — torpaq işləri")
                .region("Bakı, Abşeron")
                .requestDate(LocalDate.of(2026, 3, 1))
                .projectType(ProjectType.DAILY)
                .dayCount(20)
                .transportationRequired(false)
                .selectedEquipment(eq1)
                .params(List.of(
                        new TechParam("Texnika növü", "Hidravlik Ekskavator"),
                        new TechParam("Çəki (ton)", "20"),
                        new TechParam("İş müddəti (gün)", "20"),
                        new TechParam("Torpaq növü", "Qumlu-gil")
                ))
                .createdBy(nigar)
                .notes("Yaşayış kompleksi bünövrəsi. Operator Rauf Əliyev tələb olunur.")
                .build());

        // R-009: Grand Build — Mini Ekskavator (25 gün) → PENDING project
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.ACCEPTED)
                .customer(cGrand)
                .companyName("Grand Build ASC")
                .contactPerson("Nərmin Qasımova")
                .contactPhone("+994504567890")
                .projectName("Xəzər r. villa kompleksi bünövrə qazıması")
                .region("Bakı, Xəzər")
                .requestDate(LocalDate.of(2026, 3, 10))
                .projectType(ProjectType.DAILY)
                .dayCount(25)
                .transportationRequired(false)
                .selectedEquipment(eq9)
                .params(List.of(
                        new TechParam("Texnika növü", "Mini Ekskavator"),
                        new TechParam("Kovş həcmi (m³)", "0.2-0.3"),
                        new TechParam("İş müddəti (gün)", "25")
                ))
                .createdBy(nigar)
                .notes("Dar küçələrə uyğun mini texnika. Müqavilə hazırlanır.")
                .build());

        // ═══════════════════════════════════════════════════════════════════════
        //  MART 2026 — koordinatorda / cavab gözlənilir
        // ═══════════════════════════════════════════════════════════════════════

        // R-010: SOCAR — Yük Maşını → SENT_TO_COORDINATOR
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.SENT_TO_COORDINATOR)
                .customer(cSocar)
                .companyName("SOCAR Tikinti ASC")
                .contactPerson("Şəhriyar Əhmədov")
                .contactPhone("+994703334455")
                .projectName("Lökbatan neft bazası genişləndirilməsi")
                .region("Bakı, Lökbatan")
                .requestDate(LocalDate.of(2026, 3, 5))
                .projectType(ProjectType.DAILY)
                .dayCount(22)
                .transportationRequired(true)
                .selectedEquipment(eq6)
                .params(List.of(
                        new TechParam("Texnika növü", "Yük maşını"),
                        new TechParam("Yük tutumu (ton)", "Min 25"),
                        new TechParam("İş müddəti (gün)", "22")
                ))
                .createdBy(admin)
                .notes("Məhdud daxil olma. Neft bazası zolağı — xüsusi icazə tələb olunur.")
                .build());

        // R-011: AzərGold — Asfalt Silindir → SENT_TO_COORDINATOR
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.SENT_TO_COORDINATOR)
                .customer(cGold)
                .companyName("AzərGold QSC")
                .contactPerson("Vüsal Məmmədov")
                .contactPhone("+994704445566")
                .projectName("Daşkəsən mədən daxili yolların bərpası")
                .region("Gəncə")
                .requestDate(LocalDate.of(2026, 3, 8))
                .projectType(ProjectType.DAILY)
                .dayCount(10)
                .transportationRequired(false)
                .selectedEquipment(eq11)
                .params(List.of(
                        new TechParam("Texnika növü", "Asfalt Silindir"),
                        new TechParam("Çəki (ton)", "Min 10"),
                        new TechParam("İş müddəti (gün)", "10")
                ))
                .createdBy(nigar)
                .notes("Mədən daxili bağlı yollarda sıxlaşdırma işləri.")
                .build());

        // R-012: SkyLine — Greyder → SENT_TO_COORDINATOR
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.SENT_TO_COORDINATOR)
                .customer(cSky)
                .companyName("SkyLine Tikinti QSC")
                .contactPerson("Aynur Babayeva")
                .contactPhone("+994506789012")
                .projectName("Sumqayıt sənaye parkı — yol infrastrukturu")
                .region("Sumqayıt")
                .requestDate(LocalDate.of(2026, 3, 12))
                .projectType(ProjectType.DAILY)
                .dayCount(18)
                .transportationRequired(true)
                .selectedEquipment(eq7)
                .params(List.of(
                        new TechParam("Texnika növü", "Greyder"),
                        new TechParam("Güc (HP)", "Min 180"),
                        new TechParam("İş müddəti (gün)", "18")
                ))
                .createdBy(nigar)
                .notes("Sənaye parkı daxili yolları. Əsas yoldan giriş məhdudiyyəti var.")
                .build());

        // R-013: Bakı Metro — Beton Mikser → OFFER_SENT
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.OFFER_SENT)
                .customer(cMetro)
                .companyName("Bakı Metro MMC")
                .contactPerson("Lalə Mustafayeva")
                .contactPhone("+994705556677")
                .projectName("Dərnəgül stansiyası tünnel beton işləri")
                .region("Bakı, Qaradağ")
                .requestDate(LocalDate.of(2026, 2, 28))
                .projectType(ProjectType.DAILY)
                .dayCount(20)
                .transportationRequired(false)
                .selectedEquipment(eq4)
                .params(List.of(
                        new TechParam("Texnika növü", "Beton Mikser"),
                        new TechParam("Həcm (m³)", "Min 9"),
                        new TechParam("İş müddəti (gün)", "20"),
                        new TechParam("İş saatları", "Gecə-gündüz")
                ))
                .createdBy(admin)
                .notes("Koordinator teklif hazırlayıb. Müştərinin cavabı gözlənilir.")
                .build());

        // R-014: Kəpəz — Greyder (ikinci sahə) → OFFER_SENT
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.OFFER_SENT)
                .companyName("Caspian Roads LLC")
                .contactPerson("Həsən Rəcəbov")
                .contactPhone("+994551234567")
                .projectName("M5 Bakı-Şamaxı yolu hamarlanması")
                .region("Bakı, Abşeron")
                .requestDate(LocalDate.of(2026, 3, 3))
                .projectType(ProjectType.DAILY)
                .dayCount(12)
                .transportationRequired(true)
                .selectedEquipment(eq7)
                .params(List.of(
                        new TechParam("Texnika növü", "Greyder"),
                        new TechParam("İş müddəti (gün)", "12"),
                        new TechParam("Torpaq növü", "Çınqıllı asfalt altı qat")
                ))
                .createdBy(admin)
                .notes("Yol Agentliyi ilə koordinasiya lazımdır. Teklif göndərildi.")
                .build());

        // ═══════════════════════════════════════════════════════════════════════
        //  MART 2026 — icmal/baxışda olan yeni sorğular
        // ═══════════════════════════════════════════════════════════════════════

        // R-015: AzərGold — yeni sorğu → PENDING
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.PENDING)
                .customer(cGold)
                .companyName("AzərGold QSC")
                .contactPerson("Vüsal Məmmədov")
                .contactPhone("+994704445566")
                .projectName("Qazax mədən sahəsi — ekskavator")
                .region("Gəncə")
                .requestDate(LocalDate.of(2026, 3, 15))
                .projectType(ProjectType.DAILY)
                .dayCount(20)
                .transportationRequired(true)
                .params(List.of(
                        new TechParam("Texnika növü", "Hidravlik Ekskavator"),
                        new TechParam("Çəki (ton)", "22-25"),
                        new TechParam("İş müddəti (gün)", "20")
                ))
                .createdBy(nigar)
                .notes("Gəncə bölgəsi üçün. EQ-001 mart 25-dən boş olacaq.")
                .build());

        // R-016: SOCAR — ikinci sorğu → PENDING
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.PENDING)
                .customer(cSocar)
                .companyName("SOCAR Tikinti ASC")
                .contactPerson("Fuad Babaxanov")
                .contactPhone("+994503334455")
                .projectName("Sabunçu kompressor stansiyası — kran")
                .region("Bakı, Sabunçu")
                .requestDate(LocalDate.of(2026, 3, 18))
                .projectType(ProjectType.DAILY)
                .dayCount(8)
                .transportationRequired(false)
                .params(List.of(
                        new TechParam("Texnika növü", "Mobil Kran"),
                        new TechParam("Yük qaldırma (ton)", "Min 60"),
                        new TechParam("İş müddəti (gün)", "8")
                ))
                .createdBy(nigar)
                .notes("Kompressor bloku qaldırma. EQ-003 mart 15-dən boş olacaq.")
                .build());

        // ═══════════════════════════════════════════════════════════════════════
        //  MART 2026 — yeni daxil olmuş sorğular (DRAFT)
        // ═══════════════════════════════════════════════════════════════════════

        // R-017: Yeni müştəri — DRAFT
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.DRAFT)
                .companyName("Azər Tikinti Holdinq ASC")
                .contactPerson("Zaur Nəsirov")
                .contactPhone("+994702998877")
                .projectName("Abşeron şossesi yenidən qurulması")
                .region("Bakı, Abşeron")
                .requestDate(LocalDate.of(2026, 3, 20))
                .projectType(ProjectType.DAILY)
                .dayCount(45)
                .transportationRequired(true)
                .params(List.of(
                        new TechParam("Texnika növü", "Buldozer"),
                        new TechParam("Güc (HP)", "Min 300"),
                        new TechParam("İş müddəti (gün)", "45")
                ))
                .createdBy(nigar)
                .notes("Yeni müştəri. Zəng edib, formu doldurulur. VÖEN yoxlanılır.")
                .build());

        // R-018: Kəpəz — DRAFT
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.DRAFT)
                .customer(cKepez)
                .companyName("Kəpəz Yol Tikintisi MMC")
                .contactPerson("Elşən Nəsirov")
                .contactPhone("+994557890123")
                .projectName("Gəncə şəhəryanı yol çiyni düzəldilməsi")
                .region("Gəncə")
                .requestDate(LocalDate.of(2026, 3, 21))
                .projectType(ProjectType.DAILY)
                .dayCount(12)
                .transportationRequired(false)
                .params(List.of(
                        new TechParam("Texnika növü", "Greyder"),
                        new TechParam("İş müddəti (gün)", "12")
                ))
                .createdBy(nigar)
                .notes("Cari layihənin davamı. Martın sonundan başlaya bilər.")
                .build());

        // ═══════════════════════════════════════════════════════════════════════
        //  YANVAR-FEVRAL 2026 — rədd edilmiş sorğular
        // ═══════════════════════════════════════════════════════════════════════

        // R-019: Delta İnşaat — rədd edildi (qiymət razılaşmadı)
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.REJECTED)
                .customer(cDelta)
                .companyName("Delta İnşaat Ltd")
                .contactPerson("Orxan Kərimov")
                .contactPhone("+994556667788")
                .projectName("Binəqədi anbar kompleksi — bünövrə")
                .region("Bakı, Binəqədi")
                .requestDate(LocalDate.of(2026, 1, 25))
                .projectType(ProjectType.DAILY)
                .dayCount(20)
                .transportationRequired(false)
                .params(List.of(
                        new TechParam("Texnika növü", "Ekskavator-Yükləyici"),
                        new TechParam("İş müddəti (gün)", "20")
                ))
                .createdBy(nigar)
                .notes("Müştəri qiymətlə razılaşmadı. Ödəniş gecikməsi tarixi var.")
                .build());

        // R-020: Naməlum şirkət — rədd edildi (texniki tələblər qarşılanmadı)
        requestRepository.save(TechRequest.builder()
                .status(RequestStatus.REJECTED)
                .companyName("Flex Construction Ltd")
                .contactPerson("Orxan Süleymanov")
                .contactPhone("+994502223344")
                .projectName("Novxanı anbar kompleksi inşaatı")
                .region("Bakı, Abşeron")
                .requestDate(LocalDate.of(2026, 2, 12))
                .projectType(ProjectType.MONTHLY)
                .dayCount(2)
                .transportationRequired(true)
                .params(List.of(
                        new TechParam("Texnika növü", "Kompaktor"),
                        new TechParam("Çəki (ton)", "Min 15"),
                        new TechParam("Güc (HP)", "Min 200")
                ))
                .createdBy(nigar)
                .notes("EQ-005 təmirdədir. Tələb olunan texnika mövcud deyil. Rədd edildi.")
                .build());

        log.info("{} sorğu əlavə edildi.", requestRepository.count());
    }
}
