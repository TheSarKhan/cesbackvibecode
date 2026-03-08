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
        log.info("Operator seed məlumatları əlavə edilir...");

        List<Operator> operators = List.of(
            Operator.builder()
                .firstName("Rauf").lastName("Əliyev")
                .address("Bakı, Binəqədi r., Binəqədi qəs.")
                .phone("+994 50 123 45 67")
                .email("rauf.aliyev@mail.com")
                .specialization("Ekskavator operatoru")
                .notes("5 illik iş təcrübəsi")
                .build(),
            Operator.builder()
                .firstName("Nicat").lastName("Həsənov")
                .address("Bakı, Suraxanı r., Hövsan qəs.")
                .phone("+994 55 234 56 78")
                .email("nicat.hasanov@mail.com")
                .specialization("Kran operatoru")
                .notes("Ağır texnika üzrə sertifikatlı")
                .build(),
            Operator.builder()
                .firstName("Tural").lastName("Quliyev")
                .address("Sumqayıt ş., 9-cu mikrorayon")
                .phone("+994 70 345 67 89")
                .email("tural.quliyev@mail.com")
                .specialization("Buldozer operatoru")
                .build(),
            Operator.builder()
                .firstName("Elnur").lastName("Məmmədov")
                .address("Bakı, Xətai r., Əhmədli qəs.")
                .phone("+994 51 456 78 90")
                .specialization("Yükleyici operatoru")
                .build(),
            Operator.builder()
                .firstName("Kamran").lastName("İsmayılov")
                .address("Gəncə ş., Kəpəz r.")
                .phone("+994 77 567 89 01")
                .email("kamran.i@mail.com")
                .specialization("Ekskavator operatoru, Yükleyici operatoru")
                .notes("İki ixtisas üzrə sertifikatlı")
                .build()
        );

        operatorRepository.saveAll(operators);
        log.info("{} operator əlavə edildi.", operators.size());
    }
}
