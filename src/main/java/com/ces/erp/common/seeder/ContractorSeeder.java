package com.ces.erp.common.seeder;

import com.ces.erp.contractor.entity.Contractor;
import com.ces.erp.contractor.repository.ContractorRepository;
import com.ces.erp.enums.ContractorStatus;
import com.ces.erp.enums.RiskLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Mənbə: "Müştəri Bazası.xlsx" → "Podratçı Bazası" vərəqi (real podratçı siyahısı).
 * VÖEN-i olmayan fiziki şəxslər (Contractor.voen NOT NULL/UNIQUE olduğu üçün) seed edilmir.
 * Bu podratçılara bağlı texnika GarageSeeder-də (CONTRACTOR mülkiyyət tipi ilə) əlavə olunur.
 */
@Component
@Order(4)
@RequiredArgsConstructor
@Slf4j
public class ContractorSeeder implements CommandLineRunner {

    private final ContractorRepository contractorRepository;

    @Override
    @Transactional
    public void run(String... args) {
        if (contractorRepository.count() > 0) return;
        log.info("Podratçılar seed edilir...");

        Contractor c1 = contractorRepository.save(Contractor.builder()
                .companyName("MƏHİDOV RÖYAL AKİF")
                .voen("1003951032")
                .contactPerson("Mahir bəy")
                .phone("+994503856515")
                .address("Bakı (Caspian drilling)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c2 = contractorRepository.save(Contractor.builder()
                .companyName("SOVETZADƏ ELŞƏN AKİF")
                .voen("1904392902")
                .contactPerson("Elşən bəy")
                .phone("+994505160726")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c3 = contractorRepository.save(Contractor.builder()
                .companyName("HATEF HOSSEIN ZADEH")
                .voen("1900422682")
                .contactPerson("Hatef bəy")
                .phone("+994556400002")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c4 = contractorRepository.save(Contractor.builder()
                .companyName("BAXŞALIYEV SEVİNDİK CAVANŞİR")
                .voen("2902305592")
                .contactPerson("Sevindik bəy")
                .phone("+994502440001")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c5 = contractorRepository.save(Contractor.builder()
                .companyName("CAFAROV MMC")
                .voen("1906138971")
                .contactPerson("Cavid bəy")
                .phone("+994519081707")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c6 = contractorRepository.save(Contractor.builder()
                .companyName("İSMAYILOV ELÇİN FALƏDDİN")
                .voen("7302100682")
                .contactPerson("Kamil bəy")
                .phone("+994706389090")
                .address("Bakı (Dərnəgül)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c7 = contractorRepository.save(Contractor.builder()
                .companyName("Roman Seyidzadə")
                .voen("1507181862")
                .contactPerson("Roman bəy")
                .phone("+994519466664")
                .address("Bakı (Nərimanov)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c8 = contractorRepository.save(Contractor.builder()
                .companyName("ƏMİROV ELŞƏN ƏHLİMAN")
                .voen("4600467362")
                .contactPerson("Elşən bəy")
                .phone("+994555808200")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c9 = contractorRepository.save(Contractor.builder()
                .companyName("MURADOV VÜQAR ƏNVƏR")
                .voen("4001040062")
                .contactPerson("Xəyyam bəy")
                .phone("+994552834909")
                .address("Qala")
                .paymentType("CASH,TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.MEDIUM)
                .build());

        Contractor c10 = contractorRepository.save(Contractor.builder()
                .companyName("BAKI-TƏCHİZAT MMC")
                .voen("2900238181")
                .contactPerson("Mətləb bəy")
                .phone("+994502233944")
                .address("Bakı (Dərnəgül)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.MEDIUM)
                .build());

        Contractor c11 = contractorRepository.save(Contractor.builder()
                .companyName("RƏHİMLİ FƏRİD CAVANŞİR")
                .voen("3600364802")
                .contactPerson("Arzuman bəy")
                .phone("+994558440844")
                .address("Bakı ( Dərnəgül)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c12 = contractorRepository.save(Contractor.builder()
                .companyName("HEYDƏROV HEYDƏR MƏHƏMMƏD")
                .voen("1003103622")
                .contactPerson("Heydər bəy")
                .phone("+994557353399")
                .address("Bakı (Dərnəgül)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c13 = contractorRepository.save(Contractor.builder()
                .companyName("AZEQUİP MMC")
                .voen("1700696901")
                .contactPerson("Xamis bəy")
                .phone("+994552016551")
                .address("Saray")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c14 = contractorRepository.save(Contractor.builder()
                .companyName("VFS MMC")
                .voen("2008811481")
                .contactPerson("Vüqar bəy")
                .phone("+994506344764")
                .address("Bakı (Xocasən)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c15 = contractorRepository.save(Contractor.builder()
                .companyName("SÜLEYMANOV MİNBƏR QƏNBƏR")
                .voen("3103207172")
                .contactPerson("Sadiq bəy")
                .phone("+994512308090")
                .address("Lökbatan")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c16 = contractorRepository.save(Contractor.builder()
                .companyName("VƏLİYEV MÜZƏFFƏR FƏXRƏDDİN")
                .voen("7000602622")
                .contactPerson("Faiq bəy")
                .phone("+994502851330")
                .address("Bakı (Dərnəgül)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c17 = contractorRepository.save(Contractor.builder()
                .companyName("QULİYEV SADİQ OQTAY")
                .voen("5200256032")
                .contactPerson("Sadiq bəy")
                .phone("+994552192255")
                .address("Sumqayıt")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c18 = contractorRepository.save(Contractor.builder()
                .companyName("SULTANOV TƏBRİZ ƏBÜLFƏZ")
                .voen("3104196822")
                .contactPerson("Samir bəy")
                .phone("+994503208127")
                .address("Sumqayıt")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c19 = contractorRepository.save(Contractor.builder()
                .companyName("MEMO MMC")
                .voen("1501310751")
                .contactPerson("Rəfael bəy")
                .phone("+994553880000")
                .address("Lökbatan (Agrovest)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c20 = contractorRepository.save(Contractor.builder()
                .companyName("VƏLİYEV DİLQAM HÜSEYN")
                .voen("8503640222")
                .contactPerson("Ramal bəy")
                .phone("+994553515737")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c21 = contractorRepository.save(Contractor.builder()
                .companyName("Translift")
                .voen("2006037141")
                .contactPerson("İlqar bəy")
                .address("Lökbatan")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c22 = contractorRepository.save(Contractor.builder()
                .companyName("ERS MMC")
                .voen("2902062861")
                .contactPerson("Hacı bəy")
                .phone("+994502866677")
                .address("Lökbatan")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c23 = contractorRepository.save(Contractor.builder()
                .companyName("MƏMMƏDOV EMİN ƏBDÜLHƏSƏN")
                .voen("1402785182")
                .contactPerson("Emin bəy")
                .phone("+994703488853")
                .address("Bakı ( Nargilə)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c24 = contractorRepository.save(Contractor.builder()
                .companyName("AZLİFT PRO")
                .voen("1503411311")
                .contactPerson("Murad bəy")
                .phone("+994512326623")
                .address("Sumqayıt")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c25 = contractorRepository.save(Contractor.builder()
                .companyName("NƏCƏFOV SƏDRƏDDİN FİZULİ")
                .voen("8401835742")
                .contactPerson("Sədi bəy")
                .phone("+994504599888")
                .address("Şamaxı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c26 = contractorRepository.save(Contractor.builder()
                .companyName("İNTERMAK GROUP MMC")
                .voen("2004257911")
                .contactPerson("Ayxan bəy")
                .phone("+994102305309")
                .address("Salyan şossesi")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c27 = contractorRepository.save(Contractor.builder()
                .companyName("CASPİAN SERVİCES GROUP MMC")
                .voen("2902833601")
                .contactPerson("Yusif Bəy")
                .phone("+994502327720")
                .address("Caspian")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c28 = contractorRepository.save(Contractor.builder()
                .companyName("CƏFƏROVA GÜNEL YUSİF")
                .voen("1407041622")
                .contactPerson("Azər bəy")
                .phone("+994555552495")
                .address("Lökbatan (Agrovest)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c29 = contractorRepository.save(Contractor.builder()
                .companyName("SƏMƏDOVA CEYRAN İGİDALI")
                .voen("1103033172")
                .contactPerson("Tərlan bəy")
                .phone("+994705431037")
                .address("Lökbatan (Agrovest)")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c30 = contractorRepository.save(Contractor.builder()
                .companyName("TURAN VƏFA ELDAR")
                .voen("1203466872")
                .contactPerson("Serdar bey")
                .phone("+994554805216")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.MEDIUM)
                .build());

        Contractor c31 = contractorRepository.save(Contractor.builder()
                .companyName("İSMAYILOV MÜBARİZ YUNİS")
                .voen("2001143862")
                .contactPerson("Fərid bəy")
                .phone("+994552201445")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c32 = contractorRepository.save(Contractor.builder()
                .companyName("ABBASOV SEHRAN ABBAS")
                .voen("5500860212")
                .contactPerson("Röyal bəy")
                .phone("+994502208087")
                .address("Bakı")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c33 = contractorRepository.save(Contractor.builder()
                .companyName("ELŞAD ƏLİYEV ADİL")
                .voen("1302360682")
                .contactPerson("Elşad bəy")
                .phone("+994552940904")
                .address("Lökbatan")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.MEDIUM)
                .build());

        Contractor c34 = contractorRepository.save(Contractor.builder()
                .companyName("İSMAYILOV RASİM ƏVƏZ")
                .voen("5800517572")
                .contactPerson("Qafur bəy")
                .phone("+994703698432")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .notes("Vöen passiv")
                .build());

        Contractor c35 = contractorRepository.save(Contractor.builder()
                .companyName("QURBANOV EMİN TƏYYAR")
                .voen("1006605002")
                .contactPerson("Emin bəy")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c36 = contractorRepository.save(Contractor.builder()
                .companyName("HÜMBƏTOV AMİL MÖVSÜM")
                .voen("1503911972")
                .contactPerson("Amil bəy")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .notes("Vöen passiv")
                .build());

        Contractor c37 = contractorRepository.save(Contractor.builder()
                .companyName("HƏŞİMOV SƏBUHİ ROVEL")
                .voen("6000462502")
                .contactPerson("Yusif Bəy")
                .phone("+994504854414")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c38 = contractorRepository.save(Contractor.builder()
                .companyName("ƏHMƏDOV BAHADUR İBRAHİM")
                .voen("5000561822")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .notes("Vöen passiv")
                .build());

        Contractor c39 = contractorRepository.save(Contractor.builder()
                .companyName("ARCON TECHNIC MMC")
                .voen("2007914761")
                .contactPerson("Fizuli bəy")
                .phone("+994775007377")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c40 = contractorRepository.save(Contractor.builder()
                .companyName("ABDULLAYEV VÜSAL TEHRAN")
                .voen("6001937872")
                .contactPerson("Vüsal bəy")
                .phone("+905432331662")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c41 = contractorRepository.save(Contractor.builder()
                .companyName("BATI-X MMC")
                .voen("1501378541")
                .contactPerson("Tağıyev VİDADİ")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c42 = contractorRepository.save(Contractor.builder()
                .companyName("İSTANBUL ÇARŞI İMPORT EKSPORT LTD")
                .voen("1700066881")
                .contactPerson("Ramid bəy")
                .phone("+994102531060")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c43 = contractorRepository.save(Contractor.builder()
                .companyName("KRAL MAKİNA COMPANY MMC")
                .voen("1001510181")
                .contactPerson("Afiq bəy")
                .phone("+994508023955")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c44 = contractorRepository.save(Contractor.builder()
                .companyName("MƏLİKOV SƏXAVƏT YAŞAR")
                .voen("2907448162")
                .contactPerson("Səxavət bəy")
                .phone("+994997390939")
                .paymentType("TRANSFER")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c45 = contractorRepository.save(Contractor.builder()
                .companyName("QARAYEVA TALİYYƏ İBRAHİM")
                .voen("1803862532")
                .contactPerson("Taliyyə xanım")
                .phone("+994512073973")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c46 = contractorRepository.save(Contractor.builder()
                .companyName("HÜSEYNOV RAUF RAMİZ")
                .voen("1903320892")
                .contactPerson("Rauf bəy")
                .phone("+994709995555")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c47 = contractorRepository.save(Contractor.builder()
                .companyName("ƏLİYEV QEYSƏR")
                .voen("8502871802")
                .contactPerson("Şahin bəy")
                .phone("+994552782020")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c48 = contractorRepository.save(Contractor.builder()
                .companyName("QURBANOVA KƏMALƏ")
                .voen("1008014282")
                .contactPerson("Emin bəy")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c49 = contractorRepository.save(Contractor.builder()
                .companyName("ABBASOV ELXAN")
                .voen("1003165722")
                .contactPerson("Mahir bəy")
                .phone("+994503856515")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c50 = contractorRepository.save(Contractor.builder()
                .companyName("VƏLİYEV FAİQ FƏXRƏDDİN")
                .voen("2003816152")
                .contactPerson("Faiq bəy")
                .phone("+994502851330")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .notes("Vöen passiv")
                .build());

        Contractor c51 = contractorRepository.save(Contractor.builder()
                .companyName("AZADƏLİYEV ELŞƏN VƏLİ")
                .voen("4600249392")
                .contactPerson("Elşən bəy")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c52 = contractorRepository.save(Contractor.builder()
                .companyName("EYVAZOV CABBAR RAMİZ")
                .voen("2004502492")
                .contactPerson("Cabbar bəy")
                .phone("+994775703122")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c53 = contractorRepository.save(Contractor.builder()
                .companyName("AZ.SAFE MMC")
                .voen("1700722461")
                .address("Xırdalan")
                .paymentType("TRANSFER")
                .status(ContractorStatus.INACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        Contractor c54 = contractorRepository.save(Contractor.builder()
                .companyName("EYYUBOV ORXAN MAARİF")
                .voen("7302487122")
                .contactPerson("Orxan bəy")
                .phone("+994557969636")
                .address("Sumqayıt")
                .paymentType("CASH")
                .status(ContractorStatus.ACTIVE)
                .riskLevel(RiskLevel.LOW)
                .build());

        log.info("54 podratçı əlavə edildi.");
    }
}
