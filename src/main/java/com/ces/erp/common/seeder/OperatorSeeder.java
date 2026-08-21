package com.ces.erp.common.seeder;

import com.ces.erp.enums.OperatorStatus;
import com.ces.erp.operator.entity.Operator;
import com.ces.erp.operator.repository.OperatorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Mənbə: "Operatorlar.xlsx" → "Operator bazası" vərəqi (real operator siyahısı).
 * Excel-də soyad sütunu yoxdur — tam ad firstName-də saxlanılır, lastName boş qalır.
 */
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
                        .firstName("Dəyanət")
                        .lastName("")
                        .address("Kürdəmir")
                        .phone("+994517304515")
                        .specialization("Teleskop")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Bahadur")
                        .lastName("")
                        .address("Siyəzən")
                        .phone("+994705334353")
                        .specialization("Forklift,Kon")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Elvin")
                        .lastName("")
                        .address("Bayıl")
                        .phone("+994514467664")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Cəmil")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994557990858")
                        .specialization("Forklift")
                        .status(OperatorStatus.EXCELLENT)
                        .notes("Rezerv")
                        .build(),

                Operator.builder()
                        .firstName("Ramil")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994504561472")
                        .specialization("Forklift")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Natiq")
                        .lastName("")
                        .address("Sumqayıt")
                        .phone("+994706766878")
                        .specialization("Forklift")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Polad")
                        .lastName("")
                        .address("Salyan")
                        .phone("+994555557029")
                        .specialization("Teleskop")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Asuman")
                        .lastName("")
                        .address("Sumqayıt")
                        .phone("+994558345312")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Şahin")
                        .lastName("")
                        .address("Binə")
                        .phone("+994559958514")
                        .specialization("Teleskop")
                        .status(OperatorStatus.BAD)
                        .build(),

                Operator.builder()
                        .firstName("Mürşüd")
                        .lastName("")
                        .address("Ələt")
                        .phone("+994553839694")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Abdul")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994555893326")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Afiq")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994553626767")
                        .specialization("Teleskop")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Rüfət")
                        .lastName("")
                        .address("Ələt")
                        .phone("+994775441310")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Elnur")
                        .lastName("")
                        .address("Hacıqabul")
                        .phone("+994503662823")
                        .specialization("Forklift")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Cahangir")
                        .lastName("")
                        .address("Koroğlu")
                        .phone("+994557128580")
                        .specialization("Forklift")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Maarif")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994773232200")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Milhac")
                        .lastName("")
                        .address("Sumqayıt")
                        .phone("+994775386138")
                        .specialization("Teleskop")
                        .status(OperatorStatus.EXCELLENT)
                        .build(),

                Operator.builder()
                        .firstName("Əmrah")
                        .lastName("")
                        .address("Hövsan")
                        .phone("+99455391109")
                        .specialization("Forklift")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Əsəd")
                        .lastName("")
                        .address("Kürdəmir")
                        .phone("+994518005150")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Rahil")
                        .lastName("")
                        .address("Kürdəmir")
                        .phone("+994506727918")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Ceyhun")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994708000709")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Sakit")
                        .lastName("")
                        .address("Cəbrayıl")
                        .phone("+994512327294")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Cəfər")
                        .lastName("")
                        .address("Gəncə")
                        .phone("+994516704745")
                        .specialization("Ekskovator")
                        .status(OperatorStatus.EXCELLENT)
                        .build(),

                Operator.builder()
                        .firstName("Pərviz")
                        .lastName("")
                        .address("Mingəçevir")
                        .phone("+994773920057")
                        .specialization("Ekskovator")
                        .notes("Hələki heç bir layihədə iş icra etməyib")
                        .build(),

                Operator.builder()
                        .firstName("Aydın")
                        .lastName("")
                        .address("Sangaçal")
                        .phone("+994707425303")
                        .specialization("Teleskop (forklift)")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Elşən")
                        .lastName("")
                        .address("Sangaçal")
                        .phone("+994519954544")
                        .specialization("Forklift")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Hikmət")
                        .lastName("")
                        .address("Kürdəmir")
                        .phone("+994509689936")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Hüseyn")
                        .lastName("")
                        .address("Gəncə")
                        .phone("+994553056195")
                        .specialization("Qayçı səbət")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Ramil")
                        .lastName("")
                        .address("Gəncə")
                        .phone("+994507306441")
                        .specialization("Bakaloder")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Paşa")
                        .lastName("")
                        .address("Sangaçal")
                        .phone("+994553056195")
                        .specialization("Teleskop (forklift)")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Sərxan")
                        .lastName("")
                        .address("Yevlax")
                        .phone("+994993999619")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Fərman")
                        .lastName("")
                        .address("Şirvan")
                        .phone("+994516184845")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Afiq")
                        .lastName("")
                        .address("Suraxanı")
                        .phone("+994508825239")
                        .specialization("Teleskop")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Cabir")
                        .lastName("")
                        .address("Rayon")
                        .phone("+994506327357")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Bəxtiyar")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994704314515")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Coşqun")
                        .lastName("")
                        .phone("+994559883988")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Elşən")
                        .lastName("")
                        .address("Şirvan")
                        .phone("+994505866153")
                        .specialization("Teleskop")
                        .status(OperatorStatus.EXCELLENT)
                        .build(),

                Operator.builder()
                        .firstName("Elmir")
                        .lastName("")
                        .phone("+994504169984")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Elnur")
                        .lastName("")
                        .address("Ağdaş")
                        .phone("+994503662823")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Famil")
                        .lastName("")
                        .address("Sangaçal")
                        .phone("+994708340747")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Heybət")
                        .lastName("")
                        .address("Bakı")
                        .phone("+994512155636")
                        .specialization("Teleskop")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Elmir")
                        .lastName("")
                        .address("Şirvan")
                        .phone("+994509823339")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Allahverdi")
                        .lastName("")
                        .address("Ağdaş")
                        .phone("+994507483451")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Elvin")
                        .lastName("")
                        .address("Mingəçevir")
                        .phone("+994516534693")
                        .build(),

                Operator.builder()
                        .firstName("Elariz")
                        .lastName("")
                        .address("Cəbrayıl")
                        .phone("+994516534693")
                        .specialization("Teleskop")
                        .status(OperatorStatus.NORMAL)
                        .build(),

                Operator.builder()
                        .firstName("Rəvan")
                        .lastName("")
                        .address("Xırdalan")
                        .phone("+994559770709")
                        .specialization("Forklift")
                        .status(OperatorStatus.EXCELLENT)
                        .build(),

                Operator.builder()
                        .firstName("Adıgözəl")
                        .lastName("")
                        .address("Sangaçal")
                        .phone("+994507783931")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Elmir")
                        .lastName("")
                        .phone("+994509823339")
                        .specialization("Forklift")
                        .build(),

                Operator.builder()
                        .firstName("Rəvan")
                        .lastName("")
                        .phone("+994558151607")
                        .specialization("Teleskop")
                        .build(),

                Operator.builder()
                        .firstName("Səxavət")
                        .lastName("")
                        .phone("+994997390939")
                        .specialization("Teleskop")
                        .build(),

                Operator.builder()
                        .firstName("Tofiq")
                        .lastName("")
                        .phone("+994558025051")
                        .specialization("Teleskop")
                        .build(),

                Operator.builder()
                        .firstName("Araz")
                        .lastName("")
                        .phone("+994506689114")
                        .build(),

                Operator.builder()
                        .firstName("Əmrah")
                        .lastName("")
                        .phone("+994705655033")
                        .specialization("Kran")
                        .build(),

                Operator.builder()
                        .firstName("Osman")
                        .lastName("")
                        .phone("+994558810801")
                        .build(),

                Operator.builder()
                        .firstName("Yaşar")
                        .lastName("")
                        .phone("+994507307064")
                        .specialization("Bakaloder")
                        .build(),

                Operator.builder()
                        .firstName("Mehrac")
                        .lastName("")
                        .address("Sahil qəsəbəsi")
                        .phone("+994555978212")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Polad")
                        .lastName("")
                        .phone("+994998098208")
                        .specialization("Forklift")
                        .notes("Rezerv")
                        .build(),

                Operator.builder()
                        .firstName("Rafet")
                        .lastName("")
                        .specialization("Forklift")
                        .status(OperatorStatus.GOOD)
                        .build(),

                Operator.builder()
                        .firstName("Zahid")
                        .lastName("")
                        .address("Ağdam")
                        .phone("+994702590209")
                        .specialization("Teleskop")
                        .notes("Rezerv")
                        .build()
        );

        operatorRepository.saveAll(operators);
        log.info("{} operator əlavə edildi.", operators.size());
    }
}
