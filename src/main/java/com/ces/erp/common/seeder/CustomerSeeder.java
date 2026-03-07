package com.ces.erp.common.seeder;

import com.ces.erp.customer.entity.Customer;
import com.ces.erp.customer.repository.CustomerRepository;
import com.ces.erp.enums.CustomerStatus;
import com.ces.erp.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class CustomerSeeder implements CommandLineRunner {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (customerRepository.count() > 0) return;
        log.info("Müştəri seed edilir...");

        List<Customer> customers = List.of(

                Customer.builder()
                        .companyName("Azər İnşaat MMC")
                        .voen("1020304050")
                        .address("Bakı, Neftçilər pr. 45")
                        .supplierPerson("Kamran Əliyev")
                        .supplierPhone("+994501234567")
                        .officeContactPerson("Leyla Hüseynova")
                        .officeContactPhone("+994702345678")
                        .paymentTypes(Set.of("TRANSFER"))
                        .status(CustomerStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Uzunmüddətli müştəri, vaxtında ödəyir")
                        .build(),

                Customer.builder()
                        .companyName("Grand Build ASC")
                        .voen("2030405060")
                        .address("Bakı, Hüsü Hacıyev küç. 12")
                        .supplierPerson("Rauf Məmmədov")
                        .supplierPhone("+994553456789")
                        .officeContactPerson("Nərmin Qasımova")
                        .officeContactPhone("+994504567890")
                        .paymentTypes(Set.of("CASH", "TRANSFER"))
                        .status(CustomerStatus.ACTIVE)
                        .riskLevel(RiskLevel.MEDIUM)
                        .notes("Böyük həcmli texnika kirayəsi")
                        .build(),

                Customer.builder()
                        .companyName("SkyLine Tikinti QSC")
                        .voen("3040506070")
                        .address("Sumqayıt, Sənaye rayonu, blok 7")
                        .supplierPerson("Tural İsmayılov")
                        .supplierPhone("+994705678901")
                        .officeContactPerson("Aynur Babayeva")
                        .officeContactPhone("+994506789012")
                        .paymentTypes(Set.of("TRANSFER"))
                        .status(CustomerStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Sumqayıt filialı üzrə aktiv müştəri")
                        .build(),

                Customer.builder()
                        .companyName("Kəpəz Yol Tikintisi MMC")
                        .voen("4050607080")
                        .address("Gəncə, Atatürk pr. 88")
                        .supplierPerson("Elşən Nəsirov")
                        .supplierPhone("+994557890123")
                        .officeContactPerson("Günel Rəhimova")
                        .officeContactPhone("+994508901234")
                        .paymentTypes(Set.of("CASH"))
                        .status(CustomerStatus.VARIABLE)
                        .riskLevel(RiskLevel.MEDIUM)
                        .notes("Mövsümi sifarişlər verir")
                        .build(),

                Customer.builder()
                        .companyName("Caspian Oil Services Ltd")
                        .voen("5060708090")
                        .address("Bakı, Xəzər rayonu, Sahil küç. 3")
                        .supplierPerson("Nicat Əhmədov")
                        .supplierPhone("+994709012345")
                        .officeContactPerson("Zəhra Mustafayeva")
                        .officeContactPhone("+994509012345")
                        .paymentTypes(Set.of("TRANSFER"))
                        .status(CustomerStatus.PASSIVE)
                        .riskLevel(RiskLevel.HIGH)
                        .notes("Ödənişdə gecikməsi var, müvəqqəti passiv")
                        .build()
        );

        customerRepository.saveAll(customers);
        log.info("{} müştəri əlavə edildi.", customers.size());
    }
}
