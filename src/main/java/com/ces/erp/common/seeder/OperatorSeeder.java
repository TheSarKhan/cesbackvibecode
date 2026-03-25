package com.ces.erp.common.seeder;

import com.ces.erp.operator.entity.Operator;
import com.ces.erp.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@Order(6)
@RequiredArgsConstructor
@Slf4j
public class OperatorSeeder implements CommandLineRunner {

    private final OperatorRepository operatorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (operatorRepository.count() > 0) return;
        log.info("Operatorlar seed edilir...");

        List<Operator> operators = List.of(

                Operator.builder()
                        .firstName("Rauf")
                        .lastName("Əliyev")
                        .address("Bakı, Binəqədi r., Binəqədi qəs., N.Nərimanov küç. 14")
                        .phone("+994501234567")
                        .email("rauf.aliyev@ces.az")
                        .specialization("Ekskavator operatoru")
                        .notes("7 illik iş təcrübəsi. CAT 320 üzrə beynəlxalq sertifikat. "
                                + "Yanvar-mart 2026 ərzində 3 layihədə iştirak etdi.")
                        .build(),

                Operator.builder()
                        .firstName("Nicat")
                        .lastName("Həsənov")
                        .address("Bakı, Suraxanı r., Hövsan qəs., Muğan küç. 7")
                        .phone("+994552345678")
                        .email("nicat.hasanov@ces.az")
                        .specialization("Kran operatoru")
                        .notes("Ağır texnika üzrə sertifikatlı. 70 ton kran idarəetməsi. "
                                + "Bakı Metro layihəsindədir (mart 2026).")
                        .build(),

                Operator.builder()
                        .firstName("Tural")
                        .lastName("Quliyev")
                        .address("Sumqayıt ş., 9-cu mikrorayon, 48-ci bina")
                        .phone("+994703456789")
                        .email("tural.quliyev@ces.az")
                        .specialization("Buldozer operatoru, Greyder operatoru")
                        .notes("İki ixtisas üzrə lisenziya. Kəpəz layihəsindədir (mart 2026).")
                        .build(),

                Operator.builder()
                        .firstName("Elnur")
                        .lastName("Məmmədov")
                        .address("Bakı, Xətai r., Əhmədli qəs., Xurma küç. 22")
                        .phone("+994514567890")
                        .email("elnur.mammadov@ces.az")
                        .specialization("Yükləyici operatoru, Mini ekskavator operatoru")
                        .notes("Dəqiq iş üslubu. SkyLine layihəsinin icrasında iştirak etdi.")
                        .build(),

                Operator.builder()
                        .firstName("Kamran")
                        .lastName("İsmayılov")
                        .address("Gəncə ş., Kəpəz r., Cavadxan küç. 5")
                        .phone("+994775678901")
                        .email("kamran.ismayilov@ces.az")
                        .specialization("Ekskavator operatoru, Buldozer operatoru")
                        .notes("Gəncə bölgəsi üzrə əsas operator. SOCAR layihəsindən qayıtdı.")
                        .build(),

                Operator.builder()
                        .firstName("Murad")
                        .lastName("Rəhimov")
                        .address("Bakı, Nəsimi r., Əliağa Vahid küç. 38")
                        .phone("+994506789012")
                        .email("murad.rahimov@ces.az")
                        .specialization("Kran operatoru, Forklift operatoru")
                        .notes("Liman və anbar sahəsində 4 il təcrübə. Hazırda boşdur.")
                        .build(),

                Operator.builder()
                        .firstName("Səbuhi")
                        .lastName("Nəsirov")
                        .address("Abşeron r., Balaxanı qəs., Sənaye küç. 3")
                        .phone("+994557890123")
                        .email("sabuhi.nasirov@ces.az")
                        .specialization("Kompressor operatoru, Generator operatoru")
                        .notes("Enerji avadanlıqları üzrə ixtisaslaşıb. AzərGold sahələrindədir.")
                        .build(),

                Operator.builder()
                        .firstName("Vüsal")
                        .lastName("Babayev")
                        .address("Bakı, Pirəkəşkül, Azadlıq küç. 9")
                        .phone("+994508901234")
                        .email("vusal.babayev@ces.az")
                        .specialization("Ekskavator operatoru")
                        .notes("Yeni operator. 2 il təcrübə. Grand Build layihəsindən qayıtdı.")
                        .build()
        );

        operatorRepository.saveAll(operators);
        log.info("{} operator əlavə edildi.", operators.size());
    }
}
