package com.ces.erp.bank.entity;

import com.ces.erp.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Bank extends BaseEntity {

    @Column(nullable = false, length = 200)
    private String bankName;

    @Column(length = 100)
    private String bankCode;

    @Column(length = 50)
    private String swift;

    @Column(length = 100)
    private String iban;

    @Column(name = "correspondent_account", length = 100)
    private String correspondentAccount;

    @Column(name = "settlement_account", length = 100)
    private String settlementAccount;
}
