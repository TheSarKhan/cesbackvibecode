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
        log.info("Müştərilər seed edilir...");

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
                        .notes("3 ay ərzində 4 sorğu. Vaxtında ödəyir. VIP müştəri.")
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
                        .notes("Böyük həcmli sifarişlər. Bir dəfə ödəniş gecikib.")
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
                        .notes("Sumqayıt filialı üzrə daimi müştəri. Aylıq müqavilə.")
                        .build(),

                Customer.builder()
                        .companyName("Kəpəz Yol Tikintisi MMC")
                        .voen("4050607080")
                        .address("Gəncə, Atatürk pr. 88")
                        .supplierPerson("Elşən Nəsirov")
                        .supplierPhone("+994557890123")
                        .officeContactPerson("Günel Rəhimova")
                        .officeContactPhone("+994508901234")
                        .paymentTypes(Set.of("CASH", "TRANSFER"))
                        .status(CustomerStatus.VARIABLE)
                        .riskLevel(RiskLevel.MEDIUM)
                        .notes("Gəncə bölgəsində yol tikintisi. Mövsümi aktivlik.")
                        .build(),

                Customer.builder()
                        .companyName("SOCAR Tikinti ASC")
                        .voen("5060708091")
                        .address("Bakı, Hüsü Hacıyev küç. 40, SOCAR MK")
                        .supplierPerson("Fuad Babaxanov")
                        .supplierPhone("+994503334455")
                        .officeContactPerson("Şəhriyar Əhmədov")
                        .officeContactPhone("+994703334455")
                        .paymentTypes(Set.of("TRANSFER"))
                        .status(CustomerStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Neft-qaz sektoru. Xüsusi texniki tələblər. Çox etibarlı.")
                        .build(),

                Customer.builder()
                        .companyName("AzərGold QSC")
                        .voen("6070809091")
                        .address("Bakı, Üzeyir Hacıbəyov küç. 33")
                        .supplierPerson("Rəşad Abbasov")
                        .supplierPhone("+994554445566")
                        .officeContactPerson("Vüsal Məmmədov")
                        .officeContactPhone("+994704445566")
                        .paymentTypes(Set.of("TRANSFER"))
                        .status(CustomerStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Mədən sektoru. Ağır texnikaya tələbat yüksəkdir.")
                        .build(),

                Customer.builder()
                        .companyName("Bakı Metro MMC")
                        .voen("7080901012")
                        .address("Bakı, Hüsü Hacıyev küç. 33, Metro İdarəsi")
                        .supplierPerson("Elnur Cəfərov")
                        .supplierPhone("+994505556677")
                        .officeContactPerson("Lalə Mustafayeva")
                        .officeContactPhone("+994705556677")
                        .paymentTypes(Set.of("TRANSFER"))
                        .status(CustomerStatus.ACTIVE)
                        .riskLevel(RiskLevel.LOW)
                        .notes("Dövlət müəssisəsi. Böyük büdcə. Ödənişlər 30 gün müddətli.")
                        .build(),

                Customer.builder()
                        .companyName("Delta İnşaat Ltd")
                        .voen("8091011121")
                        .address("Bakı, Binəqədi r., sənaye zonaası")
                        .supplierPerson("Orxan Kərimov")
                        .supplierPhone("+994556667788")
                        .officeContactPerson("Sevinc Qasımova")
                        .officeContactPhone("+994506667788")
                        .paymentTypes(Set.of("CASH"))
                        .status(CustomerStatus.PASSIVE)
                        .riskLevel(RiskLevel.HIGH)
                        .notes("Ödənişdə sistemli gecikmə. Yanvar sifarişi ləğv edildi.")
                        .build()
        );

        customerRepository.saveAll(customers);
        log.info("{} müştəri əlavə edildi.", customers.size());
    }
}
