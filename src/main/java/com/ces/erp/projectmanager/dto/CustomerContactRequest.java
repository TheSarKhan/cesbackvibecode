package com.ces.erp.projectmanager.dto;

import lombok.Data;

/**
 * LM addımı 1.3 — Sifarişçi ofisindəki əlaqə şəxsi.
 * PM sorğunu qəbul etdikdən sonra sifarişçi ilə əlaqə yaradır və
 * kimə müraciət ediləcəyini bu endpoint ilə qeyd edir.
 */
@Data
public class CustomerContactRequest {
    private String customerOfficeContact;
    private String customerOfficePhone;
}
